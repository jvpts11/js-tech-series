/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.machine.FileService;
import dev.jstech.computers.machine.InstallService;
import dev.jstech.computers.machine.IqlService;
import dev.jstech.computers.machine.MachineConfigService;
import dev.jstech.computers.machine.MainframeStatsService;
import dev.jstech.computers.machine.NetworkPathResolver;
import dev.jstech.computers.machine.NetworkReadService;
import dev.jstech.computers.machine.OperationsService;
import dev.jstech.computers.machine.PackageService;
import dev.jstech.computers.machine.PortsService;
import dev.jstech.computers.machine.ProgramService;
import dev.jstech.computers.machine.RemoteComputerService;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.UnixTree;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The machine a shell runs on: what it is, what system it runs, the services a command reaches it through, and who
 * is typing. The first layer of {@link ServerCliComputer}, which the files, the network and the software are each
 * laid over in a layer of their own.
 */
@TextHolder
abstract class ServerCliShell implements ICliComputer {

    private static final TextKey WORLD_TIME = TextKey.of("jsc.cli.machine.world_time", "Day %s, %s");
    /* A system with no desktop says what it has instead, which is its own terminal. */
    private static final TextKey NO_DESKTOP = TextKey.of("jsc.cli.machine.no_desktop", "none (%s)");

    protected final IComputerTerminalHost host;
    protected final BlockEntity hostBlock;
    protected final ServerLevel level;
    /**
     * Who is typing, when somebody is.
     *
     * <p>Most of what a machine does is the machine's own and has no one in front of it: a program, a service,
     * a schedule. A few words are about the person at the keyboard, though, and those are exactly the words
     * that cannot work from anywhere else: taking items into your own hands, or handing over what you are
     * holding. A shell built with nobody typing simply has no one to hand anything to, which is what a session
     * opened on another machine is, and it says so rather than reaching across the world for a player.
     */
    @Nullable
    protected final ServerPlayer typist;
    /** The terminal window's shell session this prompt speaks for, or 0 for the machine's own prompt. */
    protected int session;
    // Set by the reboot verb during a command run; the payload handler reads it once the shell returns.
    private boolean firmwareReboot;
    private boolean reboot;

    protected ServerCliShell(final IComputerTerminalHost host, final ServerLevel level,
                             @Nullable final ServerPlayer typist) {
        this.host = host;
        this.hostBlock = (BlockEntity) host;
        this.level = level;
        this.typist = typist;
    }

    /**
     * Says which terminal window's shell this is.
     *
     * <p>A window that has moved with {@code cd} is where it went, whatever the other windows and the
     * full-screen prompt are doing; one that has not is wherever the machine's prompt is.
     */
    public void useSession(final int session) {
        this.session = session;
    }

    @Override
    public void report(final String event, final String detail) {
        if (typist != null) {
            JscEvents.award(typist, event, detail);
        } else {
            JscEvents.awardOperator(hostBlock, event, detail);
        }
    }

    @Override
    public String name() {
        // The Mainframe has no custom name; its kind is shown by type() instead.
        if (hostBlock instanceof IOsHost computer) {
            return computer.customName();
        }
        return "";
    }

    @Override
    public String type() {
        return RemoteComputerService.typeOf(hostBlock).english();
    }

    @Override
    public Object hostBlock() {
        return hostBlock;
    }

    /** The block this shell runs on. */
    public BlockEntity machine() {
        return hostBlock;
    }

    @Override
    public String nodeId() {
        final NodeUuid node;
        if (hostBlock instanceof IOsHost computer) {
            node = computer.nodeUuid();
        } else if (hostBlock instanceof MainframeBlockEntity mainframe) {
            node = mainframe.nodeUuid();
        } else {
            node = null;
        }
        return node == null ? "------" : ShortId.of(node.asString());
    }

    @Override
    public boolean running() {
        return host.computerRunning();
    }

    @Override
    public long cpuCapacity() {
        return host.orchestrationCapacity();
    }

    @Override
    public long ramBuffer() {
        return host.computerRamBuffer();
    }

    @Override
    public String hostname() {
        // The host resolves its own name so the shell, the provenance rows and the remote host list agree.
        return host.hostname();
    }

    @Override
    public ShellFamily shellFamily() {
        return ServerCliComputer.shellFamilyOf(hostBlock);
    }

    @Override
    public Platform platform() {
        final OsDef os = hostBlock instanceof IOsHost computer ? computer.installedOs() : null;
        return os == null ? Platform.LINUX : os.platform();
    }

    @Override
    public UnixTree tree() {
        return UnixTree.of(platform());
    }

    @Override
    public int osEdition() {
        return hostBlock instanceof IOsHost computer ? OsRegistry.osVersionRank(computer.installedOsId()) : 0;
    }

    @Override
    public boolean hostIs(final HostScope scope) {
        return switch (scope) {
            case ANY -> true;
            case MAINFRAME -> hostBlock instanceof MainframeBlockEntity;
            case CRAFTING_COMPUTER -> hostBlock instanceof CraftingComputerBlockEntity;
            /* A rack answers as the server mounted in it, which is what a server-only command belongs to. */
            case SERVER -> hostBlock instanceof ServerRackBlockEntity;
            case CLUSTER_MANAGEMENT_COMPUTER -> hostBlock instanceof ClusterManagementComputerBlockEntity;
        };
    }

    @Override
    public boolean hasFiles() {
        return hostBlock instanceof IOsHost computer
                && FileAccess.filesystemKindOf(computer) != FilesystemKind.NONE;
    }

    @Override
    public boolean hasPorts() {
        return hostBlock instanceof IPeripheralOwner owner && owner.maxEndpoints() > 0;
    }

    @Override
    public int processorBits() {
        return hostBlock instanceof IOsHost computer ? computer.processorBits() : 64;
    }

    @Override
    public void requestFirmwareReboot() {
        this.firmwareReboot = true;
    }

    @Override
    public boolean firmwareRebootRequested() {
        return firmwareReboot;
    }

    @Override
    public void requestReboot() {
        this.reboot = true;
    }

    @Override
    public boolean rebootRequested() {
        return reboot;
    }

    @Override
    public SystemInfo systemInfo() {
        if (!(hostBlock instanceof IOsHost computer) || computer.installedOs() == null) {
            return null;
        }
        final OsDef os = computer.installedOs();
        final ResourceLocation desktopId = computer.installedDesktopId();
        final DesktopEnvironmentDef chrome = desktopId == null ? null : OsRegistry.getDesktop(desktopId);
        final ItemStack systemDisk = computer.systemDisk();
        final long totalMb = systemDisk.getItem() instanceof DiskItem disk
                ? disk.spec().capacityItems() * StorageKey.MB_EQ_PER_ITEM : 0L;
        final long freeMb = computer.systemDiskFreeMb();
        final ComputerConsoleState console = host.console();
        return new SystemInfo(
                os.id().getPath(),
                os.displayName(),
                shellFamily() == ShellFamily.POSIX
                        ? KernelNames.kernel(os.platform(), computer.processorBits())
                        : os.platform() == Platform.FRAMES ? KernelNames.frames(os.familyRank())
                        : "JSC " + os.id().getPath(),
                hostname(),
                os.shell().serializedName(),
                chrome != null ? chrome.displayName()
                        : NO_DESKTOP.with(KernelNames.terminal(os.platform())).english(),
                computer.maxCpuMhz() + " MHz",
                (int) Math.min(Integer.MAX_VALUE, computer.ramBuffer()),
                Math.max(0L, totalMb - freeMb),
                totalMb,
                console == null ? 0 : console.installed().size(),
                level.getGameTime());
    }

    @Override
    public Text worldTime() {
        /*
         * The world's own clock, read the way the game reads it: day one is the first day, and the hours run
         * from six in the morning, which is when a day starts here.
         */
        final long time = level.getDayTime();
        final long day = time / 24000L + 1L;
        final long minutes = (time % 24000L) * 60L / 1000L + 6L * 60L;
        return WORLD_TIME.with(day, String.format(Locale.ROOT, "%02d:%02d", minutes / 60L % 24L, minutes % 60L));
    }

    @Override
    public MemoryUse memory() {
        if (!(hostBlock instanceof IOsHost computer)) {
            return new MemoryUse(0, 0, 0L);
        }
        final RamLedger ledger = computer.ramLedger();
        return new MemoryUse(ledger.totalMb(), ledger.usedMb(), ledger.heldBytes());
    }

    @Override
    public boolean listsEverything() {
        return ComputersServerConfig.listCommands();
    }

    /** The Mainframe of the network this machine is on, or null off any network. */
    @Nullable
    public MainframeBlockEntity mainframe() {
        final NetworkUuid net = host.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(level).mainframePositionOf(net)
                .map(pos -> level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }

    /** Where a path on another machine of the network leads, followed on that machine's own shell. */
    protected NetworkPathResolver networkPaths() {
        return remotes().paths();
    }

    /** What this machine keeps about itself, as this shell reads and changes it. */
    protected MachineConfigService config() {
        return new MachineConfigService(host, level, files());
    }

    /** The other computers of this machine's network, as this shell reaches them. */
    protected RemoteComputerService remotes() {
        return new RemoteComputerService(host, level);
    }

    /** What is installed on this machine, and the installing itself, as this shell reaches it. */
    protected InstallService installs() {
        return new InstallService(host, level, packages(), iql());
    }

    /** The packages this machine installs over its network's Mirror, as this shell reaches them. */
    protected PackageService packages() {
        return new PackageService(host, level, files(), iql());
    }

    /** The ports tree on this machine and the ports built from it, as this shell reaches them. */
    protected PortsService ports() {
        return new PortsService(host, level, packages());
    }

    /** The network's own language, as this shell speaks it. */
    protected IqlService iql() {
        return new IqlService(host, level, files(), operations(), networkReads());
    }

    /** What the Mainframe of this machine's network keeps about the work it has done. */
    protected MainframeStatsService mainframeStats() {
        return new MainframeStatsService(host, level);
    }

    /** The work this machine asks of its network, as this shell asks for it. */
    protected OperationsService operations() {
        return new OperationsService(host, level, this);
    }

    /** The data network this machine is on, as this shell reads it. */
    protected NetworkReadService networkReads() {
        return new NetworkReadService(host, level, this, operations());
    }

    /** This machine's drives as this shell reaches them, from where this shell's window stands. */
    protected FileService files() {
        return new FileService(hostBlock, level, networkPaths(), this::currentLocation);
    }

    /**
     * The programs running on this machine, as this shell reaches them; null when the machine runs none at all.
     *
     * <p>Named for the language rather than for programs, since this shell already answers {@code programs()} with
     * what is installed, which is another thing entirely.
     */
    @Nullable
    protected ProgramService sigma() {
        return hostBlock instanceof AbstractComputerBlockEntity computer
                ? new ProgramService(computer, host, level, files(), remotes()) : null;
    }
}
