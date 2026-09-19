/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.menu.DosTerminalMenu;
import dev.jstech.computers.menu.LinuxTtyMenu;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.OpenBootMenuPayload;
import dev.jstech.computers.operation.payload.OpenComputerUiPayload;
import dev.jstech.computers.operation.payload.OpenInstallDonePayload;
import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.operation.payload.OpenKvmPayload;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.computers.operation.payload.OpenSystemBootPayload;
import dev.jstech.computers.operation.payload.OsInstallProgressPayload;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.os.ConsoleIdentity;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.boot.BootController;
import dev.jstech.computers.os.boot.BootIdentity;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootSplash;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.IPeripheralConnectable;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Monitor: a peripheral that displays the interface of the computer it is linked to (over a Peripheral Cable, ≤ 16 blocks).
 *
 * <p>Implements {@link IEraChassisBlock} so era-specific subclasses ({@link VintageMonitorBlock},
 * {@link LegacyMonitorBlock}) each wear their own era's textures and the {@code LIT} blockstate
 * texture resolves to the correct on-screen OS style.
 */
public class MonitorBlock extends HorizontalDirectionalBlock implements EntityBlock, IPeripheralConnectable, IEraChassisBlock {

    public static final MapCodec<MonitorBlock> CODEC = simpleCodec(MonitorBlock::new);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MonitorBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends MonitorBlock> codec() {
        return CODEC;
    }

    /** The hardware era this monitor chassis belongs to. Overridden by era-specific subclasses. */
    public HardwareEra era() {
        return HardwareEra.STANDARD;
    }

    @Override
    public HardwareEra chassisEra() {
        return era();
    }

    @Override
    public PeripheralCableType peripheralType() {
        return PeripheralCableType.COMPUTING;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        // A Monitor is meant to be looked AT, so the screen faces the player who places it.
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(LIT, false);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        final BlockPos owner = monitor.ownerPos();
        if (owner == null) {
            /*
             * Explain WHY the screen is dark instead of a generic "not linked", so a missing GPU
             * (the most common cause) or a full host is obvious rather than silent.
             */
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(diagnoseUnlinked(level, pos), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        /*
         * The monitor mirrors the linked computer's OS: the screen depends on the installed OS, not on
         * the monitor. The desktop, terminal and network GUI are all server-opened container menus; the
         * firmware setup is a client-only screen the server requests via OpenComputerUiPayload; with no
         * OS the screen stays dark with a hint.
         */
        if (level.isClientSide()) {
            // The server (which alone knows the installed OS) decides and opens the right screen.
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            /*
             * Using the monitor directly always means "show me MY machine": any remote session this
             * screen was holding ends here.
             */
            monitor.setRemoteSession(null);
            bootOrPost(serverPlayer, level, pos, owner);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The monitor-use entry point: a powered-off computer shows nothing (no boot, no firmware, the screen
     * has no signal); a freshly powered-on or rebooting one plays the power-on self-test first (DEL during
     * it enters the firmware setup); otherwise the boot target opens directly.
     */
    public static void bootOrPost(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                  final BlockPos owner) {
        /*
         * A rack holding several machines has to be told which one the monitor means: that is what
         * the KVM Switch is for. Without one the screen cannot address a bay at all.
         */
        if (level.getBlockEntity(owner)
                instanceof ServerRackBlockEntity rack) {
            final List<Integer> computers = rack.computerSlots();
            if (computers.isEmpty()) {
                player.displayClientMessage(
                        Component.translatable("block.jsc.monitor.rack_empty"), true);
                return;
            }
            if (computers.size() > 1) {
                if (!rack.hasKvmSwitch()) {
                    player.displayClientMessage(
                            Component.translatable("block.jsc.monitor.needs_kvm"), true);
                    return;
                }
                openKvmChannels(player, rack, monitorPos);
                return;
            }
        }
        openSession(player, level, monitorPos, owner);
    }

    /**
     * Starts the session of the machine the KVM switch is currently showing. Called after the player
     * picks a channel, so it skips the channel bar it just came from.
     */
    public static void openSelectedChannel(final ServerPlayer player, final Level level,
                                           final BlockPos monitorPos, final BlockPos rackPos) {
        openSession(player, level, monitorPos, rackPos);
    }

    /** What the monitor shows when a session on this machine is opened. */
    public enum Entry {
        /** The machine is off: the screen has no signal. */
        NO_POWER,
        /** A fresh power-up or a restart: the power-on self-test first. */
        POST,
        /** The machine is copying a system onto a disk: the screen joins it where it has got to. */
        INSTALLING,
        /** The machine is standing at its boot manager, waiting to be told what to start. */
        BOOT_MENU,
        /** The self-test is done and the system is coming up: the screen joins that instead. */
        BOOTING,
        /** A guided installer has written the system and still waits for the reboot that boots it. */
        INSTALLER,
        /** The system is closing down before the machine starts over: the screen joins that. */
        GOING_DOWN,
        /** Hand over to whatever the boot target is (firmware, shell, desktop). */
        BOOT
    }

    /**
     * Decides the entry for a session on {@code computer}. A finished installer keeps the machine
     * until it restarts, exactly like a real one: closing the monitor is not a reboot. If the system
     * it installed is no longer on that disk (formatted, pulled), there is nothing to reboot into and
     * the pending install is dropped.
     */
    public static Entry entryFor(final IOsHost computer) {
        if (!computer.isRunning()) {
            return Entry.NO_POWER;
        }
        /*
         * Both of these are the self-test's screen: one is a machine in the middle of it, the other is a
         * machine standing at the end of one that found nothing to boot. The second used to be invisible to
         * anybody who was not already watching, so opening the monitor on such a machine fell through to its
         * setup with no word about why it had not booted.
         */
        if (computer.needsPost() || computer.haltedAtPost()) {
            return Entry.POST;
        }
        /*
         * A machine closing down is doing exactly one thing, and it comes before everything below: without
         * this, looking away during a restart and looking back put the player on the desktop of a system that
         * was in the middle of being closed.
         */
        if (computer.goingDown()) {
            return Entry.GOING_DOWN;
        }
        if (computer.installing() != null) {
            return Entry.INSTALLING;
        }
        /*
         * An installer holds the machine from its first page to the restart that ends it, so the last page
         * still counts as being in one even with nothing left to copy. It only holds it while what it wrote
         * is still there, though: a disk formatted or pulled between the install and the restart leaves
         * nothing to restart into, and the machine goes back to whatever it would boot.
         */
        final InstallerFlow installer = computer.installer();
        if (installer != null) {
            final int wrote = installer.targetSlot();
            final boolean systemStillThere = wrote < 0 ? computer.hasOs()
                    : OsDisks.hasSystem(computer.diskInSlot(wrote));
            if (systemStillThere) {
                return Entry.INSTALLING;
            }
            computer.setInstaller(null);
        }
        if (computer.atBootMenu()) {
            return Entry.BOOT_MENU;
        }
        if (computer.booting()) {
            return Entry.BOOTING;
        }
        final int slot = computer.pendingInstallSlot();
        if (slot != IOsHost.NO_PENDING_INSTALL) {
            final boolean systemStillThere = slot < 0 ? computer.hasOs()
                    : OsDisks.hasSystem(computer.diskInSlot(slot));
            if (systemStillThere) {
                return Entry.INSTALLER;
            }
            computer.setPendingInstallSlot(IOsHost.NO_PENDING_INSTALL);
        }
        return Entry.BOOT;
    }

    public static void openSession(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                    final BlockPos owner) {
        if (level.getBlockEntity(owner) instanceof IOsHost computer) {
            switch (entryFor(computer)) {
                case NO_POWER -> {
                    player.displayClientMessage(Component.translatable("block.jsc.monitor.no_power"), true);
                    return;
                }
                case POST -> {
                    openPost(player, level, monitorPos, owner);
                    return;
                }
                case INSTALLING -> {
                    openInstallProgress(player, level, monitorPos, owner, computer);
                    return;
                }
                case BOOT_MENU -> {
                    openBootMenu(player, level, monitorPos, owner, computer);
                    return;
                }
                case BOOTING -> {
                    openSystemBoot(player, level, monitorPos, owner, computer);
                    return;
                }
                case GOING_DOWN -> {
                    openSystemDown(player, level, monitorPos, owner, computer);
                    return;
                }
                case INSTALLER -> {
                    openInstallerPrompt(player, level, monitorPos, owner, computer);
                    return;
                }
                default -> {
                }
            }
        }
        openBootTarget(player, level, monitorPos, owner);
    }

    /** Sends the client the boot manager this machine is standing at, with what is left of its wait. */
    public static void openBootMenu(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                    final BlockPos owner, final IOsHost computer) {
        PacketDistributor.sendToPlayer(player, new OpenBootMenuPayload(
                owner, monitorPos,
                BootLines.menuFor(computer, computer.menuRemaining()),
                computer.menuRemaining()));
        openSession(player, level, monitorPos, owner, computer, MonitorSessionMenu.Phase.BOOT_MENU);
    }

    /**
     * Sends the client the system this machine is bringing up, at the point the machine has reached.
     *
     * <p>What is on the glass goes first and the session that shows it second, in that order, so the screen
     * is built with the machine's own lines already in hand rather than blank for a tick.
     */
    public static void openSystemBoot(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                      final BlockPos owner, final IOsHost computer) {
        PacketDistributor.sendToPlayer(player, new OpenSystemBootPayload(
                owner, monitorPos, computer.bootRemaining(), computer.bootTotal(),
                computer.bootSequence(), false, splashOf(computer), identityOf(computer)));
        openSession(player, level, monitorPos, owner, computer, MonitorSessionMenu.Phase.SYSTEM_BOOT);
    }

    /**
     * Who is coming up: the desktop, the system by the name it prints of itself, and the machine's host name,
     * which a desktop's own loading screen may name.
     *
     * <p>The desktop is what the machine booted with rather than what is installed on it: a desktop added a
     * moment ago waits for a restart, so the screen that shows a desktop coming up has to show the one that
     * really is.
     */
    private static BootIdentity identityOf(final IOsHost computer) {
        final ResourceLocation desktop = computer.bootedDesktopId();
        final OsDef system = computer.installedOs();
        return new BootIdentity(desktop == null ? "" : desktop.getPath(),
                system == null ? "" : system.displayName(), Installers.hostName(computer),
                computer.console() == null ? "" : computer.console().cdeStyle().encoded());
    }

    /**
     * Sends the client the system this machine is closing down, at the point it has reached.
     *
     * <p>The same screen as a system coming up and for the same reason: the machine is the one keeping the
     * time, so a monitor opened half way through a restart joins the goodbye where it is rather than starting
     * it over or missing it. Nothing dark follows it, because what follows it is the self-test.
     */
    public static void openSystemDown(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                      final BlockPos owner, final IOsHost computer) {
        PacketDistributor.sendToPlayer(player, new OpenSystemBootPayload(
                owner, monitorPos, computer.downRemaining(), computer.downTotal(),
                BootLines.shutdownFor(computer, true), false, splashOf(computer),
                identityOf(computer).goingDown()));
        openSession(player, level, monitorPos, owner, computer, MonitorSessionMenu.Phase.SYSTEM_BOOT);
    }

    /** The picture that machine's system comes up behind, or the plain one when it has no system. */
    private static BootSplash splashOf(final IOsHost computer) {
        final OsDef system = computer.installedOs();
        return system == null ? BootSplash.PLAIN : BootSplash.of(system.platform(), system.familyRank());
    }

    /**
     * Opens one of a monitor's sessions: the self-test, the boot manager, a system coming up, the firmware
     * setup, an installer, or a rack's channel switch.
     *
     * <p>All of them are one menu with a phase, so the server is told when a player closes one and stops
     * counting them as watching. That is what stopped an installer's next page turning up in front of
     * somebody who had walked away from the machine.
     *
     * <p>A player already in that session on that machine is left where they are. Opening a menu tears the
     * screen down and builds it again, so doing it for every page of an installer threw away the page that had
     * just arrived and put the one before it back: what a player saw was a Next that did nothing.
     */
    public static void openSession(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                    final BlockPos owner, final IOsHost computer,
                                    final MonitorSessionMenu.Phase phase) {
        if (MonitorSessionMenu.isShowing(player, owner, phase)) {
            return;
        }
        final HardwareEra era = computer.displayEra();
        // The machine's own name: these screens write their own headings, so nothing draws this one.
        final Component title = level.getBlockState(owner).getBlock().getName();
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new MonitorSessionMenu(id, inv, monitorPos, owner, era, phase), title),
                buf -> MonitorSessionMenu.writeOpenBuffer(buf, monitorPos, owner, era, phase));
    }

    /** Sends the client the copy this machine is in the middle of, at the point the machine has reached. */
    private static void openInstallProgress(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                            final BlockPos owner, final IOsHost computer) {
        final InstallerFlow flow = computer.installer();
        final OsInstallJob job = computer.installing();
        if (flow != null) {
            /*
             * The installer is the machine's, so a monitor opened halfway through is put on the page the
             * machine has reached, with the work it has already done behind it.
             */
            final int done = job == null ? flow.ticksTotal() : job.ticksTotal() - job.ticksLeft();
            PacketDistributor.sendToPlayer(player, OpenInstallerPayload
                    .of(owner, monitorPos, flow, done));
            openSession(player, level, monitorPos, owner, computer, MonitorSessionMenu.Phase.INSTALLER);
            return;
        }
        if (job == null) {
            return;
        }
        final HardwareEra era = computer.displayEra();
        final FirmwareKind kind = FirmwareKind.forEra(era != null ? era : HardwareEra.STANDARD);
        final OsDef os = OsRegistry
                .getOs(ResourceLocation.tryParse(job.osId()));
        PacketDistributor.sendToPlayer(player, new OsInstallProgressPayload(
                owner, monitorPos, kind.id(), os != null ? os.displayName() : job.osId(),
                job.targetSlot() < 0 ? "the default disk" : "Disk " + job.targetSlot(),
                job.ticksLeft(), job.ticksTotal()));
        openSession(player, level, monitorPos, owner, computer, MonitorSessionMenu.Phase.INSTALL_PROGRESS);
    }

    /** Sends the client the finished installer's reboot prompt for the system it just put on the disk. */
    private static void openInstallerPrompt(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                            final BlockPos owner, final IOsHost computer) {
        final int slot = computer.pendingInstallSlot();
        final HardwareEra era = computer.displayEra();
        final FirmwareKind kind = FirmwareKind.forEra(era != null ? era : HardwareEra.STANDARD);
        final ResourceLocation osId = slot < 0 ? computer.installedOsId()
                : OsDisks.systemOn(computer.diskInSlot(slot));
        final OsDef os = osId == null ? null
                : OsRegistry.getOs(osId);
        final String osName = os != null ? os.displayName() : "";
        final String targetLabel = slot < 0 ? "the default disk" : "Disk " + slot;
        PacketDistributor.sendToPlayer(player, new OpenInstallDonePayload(
                owner, monitorPos, kind.id(), osName, targetLabel, slot, ""));
        openSession(player, level, monitorPos, owner, computer, MonitorSessionMenu.Phase.INSTALL_PROGRESS);
    }

    /** Sends the client the switch's channel bar: every machine this rack can put on the monitor. */
    private static void openKvmChannels(
            final ServerPlayer player,
            final ServerRackBlockEntity rack,
            final BlockPos monitorPos) {
        final List<OpenKvmPayload.Channel> channels = new ArrayList<>();
        for (final int slot : rack.computerSlots()) {
            final ItemStack stack = rack.getServers().getStackInSlot(slot);
            final String custom = ServerItem.customName(stack);
            channels.add(new OpenKvmPayload.Channel(slot,
                    custom.isEmpty() ? "bay " + (slot + 1) + "U" : custom,
                    rack.bayPowerOn(slot)
                            && ServerItem.build(stack) != null));
        }
        PacketDistributor.sendToPlayer(player,
                new OpenKvmPayload(
                        rack.getBlockPos(), monitorPos, rack.activeChannel(), channels));
        openSession(player, rack.getLevel(), monitorPos, rack.getBlockPos(), rack,
                MonitorSessionMenu.Phase.KVM);
    }

    /** Sends the client the era-styled power-on self-test for the host computer on this monitor. */
    public static void openPost(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                final BlockPos owner) {
        final BlockEntity ownerBe = level.getBlockEntity(owner);
        final String name = level.getBlockState(owner).getBlock().getName().getString();
        final HardwareEra era = ownerBe instanceof IOsHost c ? c.displayEra() : null;
        final FirmwareKind kind = FirmwareKind.forEra(era != null ? era : HardwareEra.STANDARD);
        final int remaining = ownerBe instanceof IOsHost machine ? machine.postRemaining() : 0;
        // A machine standing at a failed self-test opens at the end of it, not at the start of another one.
        final boolean halted = ownerBe instanceof IOsHost machine && machine.haltedAtPost();
        PacketDistributor.sendToPlayer(player,
                new OpenPostPayload(owner, monitorPos, kind.id(), name, remaining, halted));
        if (ownerBe instanceof IOsHost machine) {
            openSession(player, level, monitorPos, owner, machine, MonitorSessionMenu.Phase.POST);
        }
    }

    /**
     * Boots the computer at {@code owner} on the monitor at {@code monitorPos}: opens the UI its boot target
     * calls for (firmware when there is no system, else the installed OS's desktop / terminal / network GUI).
     * Also used by the firmware boot manager after the player picks a boot entry.
     */
    public static void openBootTarget(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                      final BlockPos owner) {
        final BlockEntity ownerBe = level.getBlockEntity(owner);
        /*
         * A powered-off computer opens nothing, whichever path asked for the boot (monitor use, a firmware
         * action, a reboot): the screen simply has no signal until the machine is switched on.
         */
        if (ownerBe instanceof IOsHost gate) {
            if (!gate.isRunning()) {
                player.displayClientMessage(Component.translatable("block.jsc.monitor.no_power"), true);
                return;
            }
            /*
             * Drop a live-install session whose medium was pulled out, so the boot below falls back to
             * the firmware (or the disk system) instead of resurrecting the installer shell.
             */
            gate.validateOsSession();
        }
        final BootController.BootTarget target = BootController.targetForComputer(ownerBe);
        switch (target) {
            case FIRMWARE -> openFirmwareUi(player, level, monitorPos, owner, ownerBe);
            case FULL_DESKTOP -> openDesktopUi(player, level, monitorPos, owner, ownerBe);
            case TERMINAL_ONLY -> openCommandPrompt(player, level, monitorPos, owner);
            case NETWORK_GUI -> openTerminal(player, level, monitorPos, owner);
        }
        /*
         * The first time an installed system comes up in front of somebody, and only then. The mark lives on the
         * disk with the system, so erasing it and installing again is a first boot again; before the mark existed
         * this fired on every single boot, which made "first boot" mean nothing at all.
         */
        if (target != BootController.BootTarget.FIRMWARE
                && ownerBe instanceof IOsHost c && c.installedOsId() != null
                && (c.console() == null || c.console().liveInstall() == null)
                && !c.systemWelcome().seen()) {
            c.setSystemWelcome(c.systemWelcome().met());
            ComputingModule.OS_FIRST_BOOT.get()
                    .trigger(player, c.installedOsId());
        }
    }

    /** Opens the firmware setup on the monitor regardless of an installed OS (a restart into setup). */
    public static void openFirmware(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                    final BlockPos owner) {
        openFirmwareUi(player, level, monitorPos, owner, level.getBlockEntity(owner));
    }


    /** Sends the client the era-correct firmware setup screen for the host computer. */
    private static void openFirmwareUi(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                       final BlockPos owner, final BlockEntity ownerBe) {
        final String name = level.getBlockState(owner).getBlock().getName().getString();
        final HardwareEra era = ownerBe instanceof IOsHost c ? c.displayEra() : null;
        final FirmwareKind kind = FirmwareKind.forEra(era != null ? era : HardwareEra.STANDARD);
        PacketDistributor.sendToPlayer(player, new OpenComputerUiPayload(owner, monitorPos, kind.id(), name));
        if (ownerBe instanceof IOsHost machine) {
            openSession(player, level, monitorPos, owner, machine, MonitorSessionMenu.Phase.FIRMWARE);
        }
    }

    /** Opens the desktop shell (a real container menu) for the host's installed FULL_DESKTOP OS. */
    private static void openDesktopUi(final ServerPlayer player, final Level level, final BlockPos monitorPos,
                                      final BlockPos owner, final BlockEntity ownerBe) {
        if (ownerBe instanceof IOsHost c && c.installedOsId() != null) {
            final String name = level.getBlockState(owner).getBlock().getName().getString();
            final ResourceLocation osId = c.installedOsId();
            final Component title = level.getBlockState(owner).getBlock().getName();
            /*
             * The machine's RAM and what its system, desktop and services already hold: the desktop weighs
             * the windows it opens against the rest.
             */
            final int ramTotalMb = c.ramTotalMb();
            final int ramReservedMb = c.ramReservedMb();
            /*
             * The desktop environment: the OS's bundled one (Frames) or the Linux package installed.
             * The desktop this session booted, not whatever is on disk right now: a package installed
             * since the machine came up belongs to the next boot.
             */
            final ResourceLocation desktopId =
                    c.bootedDesktopId() != null ? c.bootedDesktopId() : osId;
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new DesktopMenu(
                            id, inv, monitorPos, owner, osId, desktopId, name, ramTotalMb, ramReservedMb), title),
                    buf -> DesktopMenu.writeOpenBuffer(
                            buf, monitorPos, owner, osId, desktopId, name, ramTotalMb, ramReservedMb));
        }
    }

    /** Opens the Command Prompt (the sole shell of a terminal-only OS) on this monitor for its host. */
    private static void openCommandPrompt(final ServerPlayer player, final Level level,
                                          final BlockPos monitorPos, final BlockPos owner) {
        if (level.getBlockEntity(owner) instanceof IOsHost host) {
            final HardwareEra era = host.displayEra();
            final Component title = level.getBlockState(owner).getBlock().getName();
            /*
             * A system met at a Unix prompt gets a login banner and a prompt in its shell's shape: tell the
             * client which shell, host name and system it is, and what that system runs on, so it can draw
             * them before the first command round-trip.
             */
            final OsDef os = host.installedOs();
            final LiveInstallState live =
                    host.console() == null ? null : host.console().liveInstall();
            final boolean posix = ServerCliComputer.shellFamilyOf(host)
                    == ShellFamily.POSIX;
            final ConsoleIdentity console;
            if (live != null) {
                // A booted live medium: a root shell on the installer, named after the medium.
                console = new ConsoleIdentity(ConsoleIdentity.LIVE, live.hostname(),
                        (live.distro() == LiveInstallState.Distro.ARCH ? "Arch Linux" : "Gentoo") + " live",
                        Platform.LINUX, host.processorBits());
            } else {
                final String hostname = posix
                        && host instanceof IComputerTerminalHost terminalHost
                        && level instanceof ServerLevel serverLevel
                        ? new ServerCliComputer(terminalHost, serverLevel)
                                .hostname()
                        : "";
                console = new ConsoleIdentity(posix && os != null ? os.shellId() : "", hostname,
                        os == null ? "" : os.displayName(), os == null ? null : os.platform(),
                        host.processorBits());
            }
            /*
             * Each platform gets its own console screen: the Linux TTY (installed distributions and live
             * media), the MC-DOS terminal, and the MC-NET Command Prompt window, never each other's.
             */
            final boolean tty = posix || live != null;
            final boolean dos = !tty && os != null
                    && os.platform() == Platform.MC_DOS;
            // Which run of the machine this terminal belongs to, so it does not come up showing another's lines.
            final long session = host.console() == null ? 0L : host.console().session();
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> tty
                            ? new LinuxTtyMenu(id, inv, monitorPos, owner, era, console, session)
                            : dos
                                    ? new DosTerminalMenu(id, inv, monitorPos, owner, era, console, session)
                                    : new CommandPromptMenu(id, inv, monitorPos, owner, era, console, session),
                    title),
                    buf -> CommandPromptMenu.writeOpenBuffer(buf, monitorPos, owner, era, console, session));
        }
    }

    private static void openTerminal(final ServerPlayer player, final Level level,
                                     final BlockPos monitorPos, final BlockPos owner) {
        if (level.getBlockEntity(owner) instanceof IComputerTerminalHost host) {
            final Component title = level.getBlockState(owner).getBlock().getName();
            // Reopen on the tab the player last used here (persisted on the Monitor).
            final int initialTab =
                    level.getBlockEntity(monitorPos) instanceof MonitorBlockEntity monitor
                            ? monitor.lastTab() : ComputerTerminalMenu.TAB_NETWORK;
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ComputerTerminalMenu(id, inv, host, owner, monitorPos, initialTab), title),
                    buf -> {
                        buf.writeBlockPos(monitorPos);
                        buf.writeBlockPos(owner);
                        buf.writeVarInt(initialTab);
                    });
        }
    }

    private static Component diagnoseUnlinked(final Level level, final BlockPos monitorPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Component.translatable("block.jsc.monitor.unlinked");
        }
        final OptionalLong host =
                PeripheralLinks.discoverOwner(serverLevel, monitorPos.asLong());
        if (host.isEmpty()) {
            return Component.translatable("block.jsc.monitor.no_computer");
        }
        if (serverLevel.getBlockEntity(BlockPos.of(host.getAsLong())) instanceof IPeripheralOwner owner) {
            if (owner.maxEndpoints() <= 0) {
                return Component.translatable("block.jsc.monitor.no_gpu");
            }
            if (owner.linkedEndpoints().size() >= owner.maxEndpoints()) {
                return Component.translatable("block.jsc.monitor.at_capacity");
            }
        }
        return Component.translatable("block.jsc.monitor.unlinked");
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof MonitorBlockEntity monitor) {
            monitor.unlink(serverLevel); // free the computer's endpoint slot
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new MonitorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            final Level level, final BlockState state, final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, ComputingModule.MONITOR_BE.get(), MonitorBlockEntity::serverTick);
    }

}
