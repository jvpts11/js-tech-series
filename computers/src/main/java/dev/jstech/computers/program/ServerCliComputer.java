/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.NetworkPathResolver;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlOperation;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Backs the Command Prompt's {@link ICliComputer} facade with a real computer and its network. Every command the shell
 * runs ultimately calls one of these methods on the server; effecting verbs route through the same Mainframe operation
 * dispatch the graphical terminal uses, so the CLI is a true alternative interface, not a parallel code path.
 */
public final class ServerCliComputer implements ICliComputer {

    private final IComputerTerminalHost host;
    private final BlockEntity hostBlock;
    private final ServerLevel level;

    public ServerCliComputer(final IComputerTerminalHost host, final ServerLevel level) {
        this.host = host;
        this.hostBlock = (BlockEntity) host;
        this.level = level;
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
        if (hostBlock instanceof MainframeBlockEntity) {
            return "Mainframe";
        }
        if (hostBlock instanceof CraftingComputerBlockEntity) {
            return "Crafting Computer";
        }
        if (hostBlock instanceof PersonalComputerBlockEntity) {
            return "Personal Computer";
        }
        if (hostBlock instanceof dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity) {
            return "Cluster Management Computer";
        }
        return "Computer";
    }

    @Override
    public Object hostBlock() {
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
    public boolean onNetwork() {
        return networkReads().online();
    }

    @Override
    public String networkId() {
        return networkReads().id();
    }

    @Override
    public boolean isMainframe() {
        return networkReads().isMainframe();
    }

    // Remote shells

    /**
     * Every machine on this network a remote shell can reach, keyed by host name: the Mainframe, the
     * personal computers, and the servers in their racks. The local machine is left out, since you cannot
     * ssh into the terminal you are already sitting at.
     */
    /** The reachable machines by host name, for callers outside the CLI (Remote Control's list). */
    public Map<String, BlockEntity> remoteMachines() {
        return remotes().machines();
    }

    @Override
    public List<RemoteHost> reachableHosts() {
        return remotes().hosts();
    }

    @Override
    public OpResult sshConnect(final String hostname) {
        return remotes().connect(hostname);
    }

    @Override
    public OpResult sshDisconnect() {
        return remotes().disconnect();
    }

    @Override
    public String sshSession() {
        return remotes().session();
    }

    @Override
    public NetSummary network() {
        return networkReads().summary();
    }

    @Override
    public List<StoredItem> query(final dev.jstech.computers.program.iql.IIqlCondition where,
                                  final String server, final int limit) {
        return networkReads().query(where, server, limit);
    }

    @Override
    public List<StoredItem> queryObject(final String object,
                                        final dev.jstech.computers.program.iql.IIqlCondition where,
                                        final String server, final int limit) {
        return networkReads().queryObject(object, where, server, limit);
    }

    @Override
    public List<ServerUse> servers() {
        return networkReads().servers();
    }

    @Override
    public ServerUse networkUse() {
        return networkReads().use();
    }

    @Override
    public List<Holding> find(final String item) {
        return networkReads().find(item);
    }

    @Override
    public OpResult select(final String item, final long quantity) {
        return select(item, quantity, MoveLabels.SHELL);
    }

    @Override
    public OpResult select(final String item, final long quantity, final String origin) {
        return operations().select(item, quantity, origin);
    }

    @Override
    public List<OperationStat> operationStats() {
        return mainframeStats().work();
    }

    @Override
    public int peakOperationsToday() {
        return mainframeStats().peakToday();
    }

    @Override
    public OpResult repriorityOperation(final String id, final String priority) {
        return operations().reprioritise(id, priority);
    }

    public OpResult cancelOperation(final String id) {
        return operations().cancel(id);
    }

    @Override
    public OpResult insert(final String item, final long quantity) {
        return insert(item, quantity, dev.jstech.core.operation.OperationPriority.DEFAULT, MoveLabels.SHELL);
    }

    @Override
    public OpResult insert(final String item, final long quantity, final String origin) {
        return insert(item, quantity, dev.jstech.core.operation.OperationPriority.DEFAULT, origin);
    }

    private OpResult insert(final String item, final long quantity,
                            final dev.jstech.core.operation.OperationPriority priority,
                            final String origin) {
        return operations().insert(item, quantity, priority, origin);
    }

    @Override
    public OpResult craft(final String item, final long quantity) {
        return craft(item, quantity, dev.jstech.core.operation.OperationPriority.DEFAULT, MoveLabels.SHELL);
    }

    @Override
    public OpResult craft(final String item, final long quantity, final String origin) {
        return craft(item, quantity, dev.jstech.core.operation.OperationPriority.DEFAULT, origin);
    }

    private OpResult craft(final String item, final long quantity,
                           final dev.jstech.core.operation.OperationPriority priority,
                           final String origin) {
        return operations().craft(item, quantity, priority, origin);
    }

    @Override
    public OpResult lock(final String item, final long quantity) {
        return operations().lock(item, quantity);
    }

    @Override
    public OpResult unlock(final String item) {
        return operations().unlock(item);
    }

    @Override
    public List<StoredItem> locks() {
        return operations().locks();
    }

    @Override
    public List<ActiveOp> activeOps() {
        return operations().list();
    }

    @Override
    public OpResult maintenance(final String action) {
        return operations().maintenance(action);
    }

    @Override
    public List<String> peripherals() {
        if (hostBlock instanceof dev.jstech.core.peripheral.IPeripheralOwnerSupport owner) {
            final List<String> rows = new ArrayList<>();
            for (final long endpoint : owner.peripheralEndpoints()) {
                final BlockPos pos = BlockPos.of(endpoint);
                rows.add(level.getBlockState(pos).getBlock().getName().getString()
                        + " @ " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
            }
            return rows;
        }
        return List.of();
    }

    @Override
    public List<ProgramInfo> programs() {
        return installs().programs();
    }

    @Override
    public OpResult install(final String programId) {
        return installs().install(programId);
    }

    /** The machine as the thing that installs programs, whichever of the two handles this prompt holds. */
    @org.jetbrains.annotations.Nullable
    private dev.jstech.computers.os.IOsHost osHost() {
        if (host instanceof dev.jstech.computers.os.IOsHost fromHost) {
            return fromHost;
        }
        return hostBlock instanceof dev.jstech.computers.os.IOsHost fromBlock ? fromBlock : null;
    }

    @Override
    public OpResult engineControl(final String action) {
        return iql().control(action);
    }

    @Override
    public java.util.List<ServiceStatus> services() {
        return packages().services();
    }

    @Override
    public boolean iqlEngineInstalled() {
        return iql().installed();
    }

    @Override
    public OpResult execute(final IqlOperation op) {
        return iql().execute(op);
    }

    // helpers

    private MainframeBlockEntity mainframe(final NetworkUuid net) {
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(level).mainframePositionOf(net)
                .map(pos -> level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }

    /** The storage key an item name stands for, as the shell reads names, or null for one it does not know. */
    public static StorageKey itemKey(final String name) {
        return resolveKey(name);
    }

    private static StorageKey resolveKey(final String name) {
        final Item item = resolveItem(name);
        return item == null ? null : StorageKey.of(item);
    }

    private static Item resolveItem(final String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        final String id = name.contains(":") ? name : "minecraft:" + name;
        final ResourceLocation location = ResourceLocation.tryParse(id.toLowerCase(java.util.Locale.ROOT));
        if (location == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(location).orElse(null);
    }


    /** The shell family of the OS installed on {@code host} (DOS when it has no OS or is not a computer). */
    public static dev.jstech.computers.os.ShellFamily shellFamilyOf(final Object host) {
        if (host instanceof IOsHost computer) {
            final OsDef os = computer.installedOs();
            final KernelDef kernel = os == null ? null : OsRegistry.getKernel(os.kernelId());
            if (kernel != null) {
                return kernel.shellFamily();
            }
        }
        return dev.jstech.computers.os.ShellFamily.DOS;
    }

    @Override
    public dev.jstech.computers.os.ShellFamily shellFamily() {
        return shellFamilyOf(hostBlock);
    }

    // Set by the reboot verb during a command run; the payload handler reads it once the shell returns.
    private boolean firmwareReboot;
    private boolean reboot;

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

    // packages: the Linux package managers over the network's Mirror service

    @Override
    public dev.jstech.computers.os.PackageManagerKind packageManager() {
        return packages().manager();
    }

    @Override
    public boolean mirrorReachable() {
        return packages().reachable();
    }

    @Override
    public java.util.List<String> drainBuildNotices() {
        return packages().notices();
    }

    @Override
    public java.util.List<PackageInfo> packagesAvailable() {
        return packages().available();
    }

    @Override
    public OpResult publishPackage(final String path) {
        return packages().publish(path);
    }

    @Override
    public OpResult unpublishPackage(final String name) {
        return packages().unpublish(name);
    }


    @Override
    public OpResult packageInstall(final String name) {
        return packages().install(name);
    }

    /** The build every package the Mirror serves is currently at: the mod's own version. */
    public static String modVersion() {
        return dev.jstech.computers.machine.PackageService.modVersion();
    }

    @Override
    public OpResult packageUpdate() {
        return packages().update();
    }

    @Override
    public OpResult packageRemove(final String name) {
        return packages().remove(name);
    }

    @Override
    public OpResult formatDrive(final char letterRaw) {
        return files().formatDrive(letterRaw);
    }

    @Override
    public boolean hasProgram(final net.minecraft.resources.ResourceLocation id) {
        return installs().has(id);
    }

    @Override
    public SystemInfo systemInfo() {
        if (!(hostBlock instanceof IOsHost computer) || computer.installedOs() == null) {
            return null;
        }
        final dev.jstech.computers.os.OsDef os = computer.installedOs();
        final net.minecraft.resources.ResourceLocation desktopId = computer.installedDesktopId();
        final dev.jstech.computers.os.DesktopEnvironmentDef chrome =
                desktopId == null ? null : OsRegistry.getDesktop(desktopId);
        final ItemStack systemDisk = computer.systemDisk();
        final long totalMb = systemDisk.getItem()
                instanceof dev.jstech.computers.item.DiskItem disk
                ? disk.spec().capacityItems() * StorageKey.MB_EQ_PER_ITEM : 0L;
        final long freeMb = computer.systemDiskFreeMb();
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        return new SystemInfo(
                os.id().getPath(),
                os.displayName(),
                shellFamily() == dev.jstech.computers.os.ShellFamily.POSIX
                        ? "Linux 6.8-jsc x86_64" : "JSC " + os.id().getPath(),
                hostname(),
                os.shellId(),
                chrome != null ? chrome.displayName() : "none (tty1)",
                computer.maxCpuMhz() + " MHz",
                (int) Math.min(Integer.MAX_VALUE, computer.ramBuffer()),
                Math.max(0L, totalMb - freeMb),
                totalMb,
                console == null ? 0 : console.installed().size(),
                level.getGameTime());
    }

    @Override
    public java.util.Map<String, Long> buildsRemaining() {
        return packages().buildsRemaining();
    }

    @Override
    public OpResult mirrorControl(final String action) {
        return packages().control(action);
    }

    @Override
    public String hostname() {
        // The host resolves its own name so the shell, the provenance rows and the remote host list agree.
        return host.hostname();
    }

    @Override
    public dev.jstech.computers.program.install.LiveInstallState liveInstall() {
        return installs().live();
    }

    @Override
    public OpResult liveRun(final String line) {
        // The live medium's reboot is a real one: the shell closes, the POST replays, the new system boots.
        return installs().liveRun(line, this::requestReboot);
    }

    @Override
    public String prompt() {
        final dev.jstech.computers.program.install.LiveInstallState live = liveInstall();
        if (live != null) {
            return live.prompt();
        }
        if (shellFamily() != dev.jstech.computers.os.ShellFamily.POSIX) {
            return currentLocation().dosPath() + ">";
        }
        final String cwd = dev.jstech.computers.program.cli.PosixPath.renderForPrompt(currentLocation());
        final OsDef os = hostBlock instanceof IOsHost c ? c.installedOs() : null;
        final boolean zsh = os != null && os.shellId().equals("zsh");
        return zsh ? "player@" + hostname() + " " + cwd + " %" : "player@" + hostname() + ":" + cwd + "$";
    }

    @Override
    public java.util.List<MountInfo> mounts() {
        return files().mounts();
    }

    /** The terminal window's shell session this prompt speaks for, or 0 for the machine's own prompt. */
    private int session;

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
    public dev.jstech.computers.program.cli.DosPath.Location currentLocation() {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return dev.jstech.computers.program.cli.DosPath.Location.root('C');
        }
        final dev.jstech.computers.program.ComputerConsoleState.ShellSpot spot =
                session == 0 ? null : console.sessionLocation(session);
        if (spot != null) {
            final java.util.List<String> parts = spot.dir().isEmpty()
                    ? java.util.List.of() : java.util.List.of(spot.dir().split("/"));
            return new dev.jstech.computers.program.cli.DosPath.Location(spot.drive(), parts);
        }
        /*
         * A fresh POSIX session starts in the home directory (a DOS one at the drive root); once the player
         * has changed directory the stored location wins, so "cd /" really lands on the root.
         */
        if (!console.hasTerminalLocation() && console.terminalDrive() == 'C'
                && shellFamily() == dev.jstech.computers.os.ShellFamily.POSIX) {
            return dev.jstech.computers.program.cli.PosixPath.home();
        }
        final String dir = console.terminalDir();
        final java.util.List<String> segments = dir.isEmpty()
                ? java.util.List.of() : java.util.List.of(dir.split("/"));
        return new dev.jstech.computers.program.cli.DosPath.Location(console.terminalDrive(), segments);
    }

    @Override
    public void setCurrentLocation(final dev.jstech.computers.program.cli.DosPath.Location location) {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        if (console == null) {
            return;
        }
        if (session != 0) {
            console.setSessionLocation(session, location.drive(), location.storagePath());
        } else {
            console.setTerminalLocation(location.drive(), location.storagePath());
        }
    }

    @Override
    public FsResult listDisk(final String dir) {
        return files().listDisk(dir);
    }

    @Override
    public FsResult readFile(final String path) {
        return files().readFile(path);
    }

    @Override
    public FsResult deleteFile(final String path) {
        return files().deleteFile(path);
    }

    @Override
    public FsResult runScript(final String path) {
        return iql().runFile(path);
    }

    @Override
    public FsResult writeFile(final String path, final String content) {
        return files().writeFile(path, content);
    }

    @Override
    public FsResult appendFile(final String path, final String content) {
        return files().appendFile(path, content);
    }

    @Override
    public FsResult changeDir(final String input) {
        return files().changeDir(input, this::setCurrentLocation);
    }

    @Override
    public FsResult changeDrive(final char drive) {
        return files().changeDrive(drive, letter -> {
            final dev.jstech.computers.program.ComputerConsoleState console = host.console();
            if (console != null) {
                console.setTerminalDrive(letter);
            }
        });
    }

    @Override
    public FsResult makeDir(final String path) {
        return files().makeDir(path);
    }

    @Override
    public FsResult removeDir(final String path) {
        return files().removeDir(path);
    }

    @Override
    public FsResult copyPath(final String src, final String dest) {
        return files().copyPath(src, dest);
    }

    @Override
    public FsResult movePath(final String src, final String destDir) {
        return files().movePath(src, destDir);
    }

    @Override
    public FsResult renamePath(final String src, final String newName) {
        return files().renamePath(src, newName);
    }

    @Override
    public java.util.List<String> configSummary() {
        return config().summary();
    }

    @Override
    public OpResult setConfig(final String key, final String value) {
        return config().set(key, value);
    }

    @Override
    public List<ShareInfo> shares() {
        return config().shares();
    }

    @Override
    public List<NetworkShare> networkShares() {
        return remotes().networkShares();
    }

    /** Where a path on another machine of the network leads, followed on that machine's own shell. */
    private NetworkPathResolver networkPaths() {
        return remotes().paths();
    }

    /** What this machine keeps about itself, as this shell reads and changes it. */
    private dev.jstech.computers.machine.MachineConfigService config() {
        return new dev.jstech.computers.machine.MachineConfigService(host, level, files());
    }

    /** The other computers of this machine's network, as this shell reaches them. */
    private dev.jstech.computers.machine.RemoteComputerService remotes() {
        return new dev.jstech.computers.machine.RemoteComputerService(host, level);
    }

    /** What is installed on this machine, and the installing itself, as this shell reaches it. */
    private dev.jstech.computers.machine.InstallService installs() {
        return new dev.jstech.computers.machine.InstallService(host, level, packages(), iql());
    }

    /** The packages this machine installs over its network's Mirror, as this shell reaches them. */
    private dev.jstech.computers.machine.PackageService packages() {
        return new dev.jstech.computers.machine.PackageService(host, level, files(), iql());
    }

    /** The network's own language, as this shell speaks it. */
    private dev.jstech.computers.machine.IqlService iql() {
        return new dev.jstech.computers.machine.IqlService(host, level, files(), operations(), networkReads());
    }

    /** What the Mainframe of this machine's network keeps about the work it has done. */
    private dev.jstech.computers.machine.MainframeStatsService mainframeStats() {
        return new dev.jstech.computers.machine.MainframeStatsService(host, level);
    }

    /** The work this machine asks of its network, as this shell asks for it. */
    private dev.jstech.computers.machine.OperationsService operations() {
        return new dev.jstech.computers.machine.OperationsService(host, level, this);
    }

    /** The data network this machine is on, as this shell reads it. */
    private dev.jstech.computers.machine.NetworkReadService networkReads() {
        return new dev.jstech.computers.machine.NetworkReadService(host, level, this, operations());
    }

    /** This machine's drives as this shell reaches them, from where this shell's window stands. */
    private dev.jstech.computers.machine.FileService files() {
        return new dev.jstech.computers.machine.FileService(hostBlock, level, networkPaths(), this::currentLocation);
    }

    /** The machine on this network that {@code name} picks out, as its own shell; null when none or several. */
    @org.jetbrains.annotations.Nullable
    public ServerCliComputer remoteShell(final String name) {
        return remotes().find(name);
    }


    /** The block this shell runs on. */
    public BlockEntity machine() {
        return hostBlock;
    }

    /** The Mainframe of the network this machine is on, or null off any network. */
    @org.jetbrains.annotations.Nullable
    public MainframeBlockEntity mainframe() {
        return mainframe(host.networkUuid());
    }

    /** Whether this machine takes programs and commands from the other computers on its network. */
    public boolean remoteAllowed() {
        final dev.jstech.computers.program.ComputerConsoleState console = host.console();
        return console != null && console.settings().remoteAllowed();
    }



    // Script processes: the Σ# programs this machine is running.

    /**
     * The machine's programs when one of them has the terminal, or null when the prompt is free.
     *
     * <p>A machine has one prompt, so it has at most one program in front of it; whoever is at the
     * keyboard is typing at that program until it returns.
     */
    @org.jetbrains.annotations.Nullable
    public MachinePrograms foreground() {
        final dev.jstech.computers.machine.ProgramService running = sigma();
        return running == null ? null : running.foreground();
    }

    @Override
    public OpResult startSigma(final String path, final int heapMb) {
        return this.startSigma(path, heapMb, List.of());
    }

    @Override
    public OpResult startSigma(final String path, final int heapMb, final List<String> arguments) {
        final dev.jstech.computers.machine.ProgramService running = sigma();
        return running == null ? OpResult.fail("sigma: this machine cannot run programs")
                : running.startAtTerminal(path, heapMb, arguments);
    }

    @Override
    public OpResult stopSigma(final int id) {
        final dev.jstech.computers.machine.ProgramService running = sigma();
        return running == null ? OpResult.fail("sigma: this machine cannot run programs") : running.stop(id);
    }

    @Override
    public List<SigmaProcess> sigmaProcesses() {
        final dev.jstech.computers.machine.ProgramService running = sigma();
        return running == null ? List.of() : running.processes();
    }

    /**
     * The programs running on this machine, as this shell reaches them; null when the machine runs none at all.
     *
     * <p>Named for the language rather than for programs, since this shell already answers {@code programs()} with
     * what is installed, which is another thing entirely.
     */
    @org.jetbrains.annotations.Nullable
    private dev.jstech.computers.machine.ProgramService sigma() {
        return hostBlock instanceof AbstractComputerBlockEntity computer
                ? new dev.jstech.computers.machine.ProgramService(computer, host, level, files(), remotes()) : null;
    }
}
