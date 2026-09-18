/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.firmware;

import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.IBootMenuScreenOpener;
import dev.jstech.computers.block.IInstallDoneScreenOpener;
import dev.jstech.computers.block.IInstallProgressScreenOpener;
import dev.jstech.computers.block.IPostScreenOpener;
import dev.jstech.computers.block.ISystemBootScreenOpener;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.BootSequenceScreen;
import dev.jstech.computers.client.FirmwareScreen;
import dev.jstech.computers.client.InstallerScreen;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.FirmwareActionPayload;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.OpenInstallDonePayload;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.computers.operation.payload.OpenBootMenuPayload;
import dev.jstech.computers.operation.payload.OpenSystemBootPayload;
import dev.jstech.computers.operation.payload.OsInstallProgressPayload;
import dev.jstech.computers.operation.payload.PostCompletePayload;
import dev.jstech.computers.operation.payload.RequestFirmwarePayload;
import dev.jstech.computers.operation.payload.RequestFirmwareStatePayload;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.InstallMode;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.core.tier.HardwareEra;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The firmware's payloads: the state and actions of the setup, the power-on self-test, and installing a system from
 * a media reader.
 */
public final class FirmwarePayloads {

    /** What an install asks for when it does not care which drive the medium is in. */
    public static final long ANY_READER = -1L;

    private FirmwarePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        /*
         * The firmware boot manager: state request/reply, boot/install/boot-order actions, restart into setup.
         * The setup, the installer and the self-test are plain screens, so they answer to the screen gate.
         */
        ComputerAccess.accept(registrar, RequestFirmwareStatePayload.TYPE, RequestFirmwareStatePayload.STREAM_CODEC,
                ComputerAccess.screen(RequestFirmwareStatePayload::hostPos),
                FirmwarePayloads::handleRequestFirmwareState);
        registrar.playToClient(FirmwareStatePayload.TYPE, FirmwareStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> {
                    /*
                     * One answer, three readers: the setup page that lists the parts, the self-test that reads
                     * them out as it finds them, and the installer that checks them before it will begin.
                     */
                    FirmwareScreen.accept(payload);
                    BootSequenceScreen.accept(payload);
                    InstallerScreen.accept(payload);
                }));
        ComputerAccess.accept(registrar, FirmwareActionPayload.TYPE, FirmwareActionPayload.STREAM_CODEC,
                ComputerAccess.screen(FirmwareActionPayload::hostPos), FirmwarePayloads::handleFirmwareAction);
        // Setup is asked for from a running system's settings as well as from the installer.
        ComputerAccess.accept(registrar, RequestFirmwarePayload.TYPE, RequestFirmwarePayload.STREAM_CODEC,
                ComputerAccess.anyOf(ComputerAccess.machine(RequestFirmwarePayload::hostPos),
                        ComputerAccess.screen(RequestFirmwarePayload::hostPos)),
                FirmwarePayloads::handleRequestFirmware);
        /*
         * The power-on self-test: the server asks the monitor to play what is left of it. The machine ends it
         * itself and boots whoever is watching; the client only speaks up when DEL asks for the setup.
         */
        registrar.playToClient(OpenPostPayload.TYPE, OpenPostPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        IPostScreenOpener.Holder.open(
                                payload.host(), payload.monitorPos(),
                                FirmwareKind.byId(payload.firmwareKind()),
                                payload.name(), payload.remainingTicks(), payload.halted())));
        // A finished installer still waiting for its reboot: the monitor comes back to that prompt.
        registrar.playToClient(OpenInstallDonePayload.TYPE, OpenInstallDonePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        IInstallDoneScreenOpener.Holder.open(
                                payload.host(), payload.monitorPos(),
                                FirmwareKind.byId(payload.firmwareKind()),
                                payload.osName(), payload.targetLabel(), payload.targetSlot(), payload.failure())));
        // The boot manager: the monitor joins the machine where it stands, with what is left of its wait.
        registrar.playToClient(OpenBootMenuPayload.TYPE, OpenBootMenuPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        IBootMenuScreenOpener.Holder.open(
                                payload.hostPos(), payload.monitorPos(), payload.menu(),
                                payload.remainingTicks())));
        // The system coming up: the monitor joins it where the machine has got to.
        registrar.playToClient(OpenSystemBootPayload.TYPE, OpenSystemBootPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        ISystemBootScreenOpener.Holder.open(
                                payload.hostPos(), payload.monitorPos(), payload.sequence(),
                                payload.remainingTicks(), payload.totalTicks(), payload.endsDark(),
                                payload.splash(), payload.desktopId(), payload.systemName())));
        // A copy already under way: the monitor shows where the machine has got to, not a fresh one.
        registrar.playToClient(OsInstallProgressPayload.TYPE, OsInstallProgressPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        IInstallProgressScreenOpener.Holder.open(
                                payload.hostPos(), payload.monitorPos(),
                                FirmwareKind.byId(payload.firmwareKind()),
                                payload.osName(), payload.targetLabel(), payload.ticksLeft(),
                                payload.ticksTotal())));
        ComputerAccess.accept(registrar, PostCompletePayload.TYPE, PostCompletePayload.STREAM_CODEC,
                ComputerAccess.screen(PostCompletePayload::hostPos), FirmwarePayloads::handlePostComplete);
    }

    private static void handleRequestFirmwareState(final RequestFirmwareStatePayload payload, final ServerPlayer player,
                                                   final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            PacketDistributor.sendToPlayer(player, buildFirmwareState(level, computer, payload.hostPos()));
        }
    }

    /**
     * Everything the boot manager lists for {@code computer}: disks, linked media, boot order, hardware.
     *
     * <p>Public because it is the one answer to "what is in this machine", and three screens draw from it: the
     * self-test reading its parts out, the setup listing them, and the installer checking them. Anything that
     * wants to know what a machine would say about itself asks here rather than working it out again.
     */
    public static FirmwareStatePayload buildFirmwareState(final ServerLevel level,
                                                  final IOsHost computer, final BlockPos pos) {
        final HardwareEra era = computer.displayEra() != null ? computer.displayEra() : HardwareEra.STANDARD;
        final List<FirmwareStatePayload.Entry> entries = new ArrayList<>();
        for (int i = 0; i < computer.diskSlots(); i++) {
            final ItemStack disk = computer.diskInSlot(i);
            if (!(disk.getItem() instanceof DiskItem)) {
                continue;
            }
            final ResourceLocation osId =
                    OsDisks.systemOn(disk);
            final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
            final DiskSpec spec = ((DiskItem) disk.getItem()).spec();
            entries.add(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_DISK, i,
                    os == null ? "" : os.id().toString(),
                    os == null ? "no system" : os.displayName(),
                    disk.getHoverName().getString(),
                    spec == null ? "" : DiskSpec.sizeLabel(spec.capacityMb()),
                    "", os != null, -1));
        }
        for (final long endpoint : computer.linkedEndpoints()) {
            if (!(level.getBlockEntity(BlockPos.of(endpoint)) instanceof MediaReaderBlockEntity reader)) {
                continue;
            }
            final String drive = reader.driveType().driveName();
            final ItemStack media = reader.mediaSlot().getStackInSlot(0);
            if (media.isEmpty()) {
                entries.add(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, endpoint, "",
                        "empty", drive, "", "", false, -1));
                continue;
            }
            final OsDef os = reader.insertedKind() == MediaKind.OS_INSTALL && reader.insertedPayload() != null
                    ? OsRegistry.getOs(reader.insertedPayload()) : null;
            if (os == null) {
                entries.add(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, endpoint, "",
                        media.getHoverName().getString(), drive, "", "", false, -1));
                continue;
            }
            final boolean eraOk = OsGating.canInstall(os.minEra(), era);
            entries.add(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, endpoint,
                    os.id().toString(),
                    os.displayName() + (os.installMode() == InstallMode.GUIDED
                            ? " installer" : " (live)"),
                    drive, "", eraOk ? "" : eraName(os.minEra()) + " era or newer", eraOk,
                    os.installMode().id()));
        }
        return new FirmwareStatePayload(pos, era.id(), machineOf(level, computer, pos), computer.bootDiskSlot(),
                computer.defaultInstallSlot(), entries, raidInfoOf(level, computer));
    }

    /**
     * What the firmware found when it powered the machine on, read off the parts rather than off the block.
     *
     * <p>A self-test reads out what is in the machine, so it has to be asked of the machine: the processor by its
     * own model with its cores and its architecture, the memory counted over the modules, the board, the video
     * card, and the monitors that are really linked rather than a "connected" that was always true.
     */
    private static FirmwareStatePayload.Machine machineOf(final ServerLevel level, final IOsHost computer,
                                                          final BlockPos pos) {
        if (!(computer instanceof AbstractComputerBlockEntity machine)) {
            return FirmwareStatePayload.Machine.NONE;
        }
        final HardwareEra era = computer.displayEra();
        final ComputerBuild build = machine.currentBuild();
        final CpuSpec cpu = build == null || build.cpus().isEmpty() ? null : build.cpus().getFirst();
        String cpuName = "";
        String boardName = "";
        String gpuName = "";
        String ramName = "";
        final ItemStackHandler hardware = machine.getHardware();
        for (int i = 0; i < hardware.getSlots(); i++) {
            final ItemStack part = hardware.getStackInSlot(i);
            if (part.isEmpty()) {
                continue;
            }
            if (part.getItem() instanceof CpuItem && cpuName.isEmpty()) {
                cpuName = part.getHoverName().getString();
            } else if (part.getItem() instanceof MotherboardItem) {
                boardName = part.getHoverName().getString();
            } else if (part.getItem() instanceof GpuItem && gpuName.isEmpty()) {
                gpuName = part.getHoverName().getString();
            } else if (part.getItem() instanceof RamItem && ramName.isEmpty()) {
                ramName = part.getHoverName().getString();
            }
        }
        int monitors = 0;
        for (final long endpoint : computer.linkedEndpoints()) {
            if (level.getBlockEntity(BlockPos.of(endpoint))
                    instanceof MonitorBlockEntity) {
                monitors++;
            }
        }
        final String name = computer.customName().isEmpty()
                ? level.getBlockState(pos).getBlock().getName().getString() : computer.customName();
        return new FirmwareStatePayload.Machine(name, cpuName, cpu == null ? 0 : cpu.cores(),
                cpu == null ? 0 : cpu.freqMhz(),
                cpu == null ? "" : cpu.architecture().name(), cpu == null ? 0 : cpu.architecture().bits(),
                boardName, (int) Math.min(Integer.MAX_VALUE, computer.ramTotalMb()),
                build == null ? 0 : build.rams().size(), machine.boardRamSlots(), ramName, gpuName, monitors,
                machine.maxEndpoints(), era == null ? "" : HardwareTooltip.label(era));
    }

    /**
     * What the firmware's storage page shows: the controller in this machine's bay, the array it
     * runs, and what each mode would give. Only a rack server has one, since a desk computer's firmware
     * simply has no storage page.
     */
    private static FirmwareStatePayload.RaidInfo raidInfoOf(
            final ServerLevel level, final IOsHost computer) {
        if (!(computer instanceof ServerRackBlockEntity rack)) {
            return FirmwareStatePayload.RaidInfo.ABSENT;
        }
        final int slot = rack.soleComputerSlot();
        if (slot < 0 || rack.raidControllerSlot(slot) < 0) {
            return FirmwareStatePayload.RaidInfo.ABSENT;
        }
        final List<Long> sizes = new ArrayList<>();
        for (final ItemStack drive : rack.claimedDriveStacks(slot)) {
            if (drive.getItem() instanceof DiskItem disk) {
                sizes.add(disk.spec().capacityItems());
            }
        }
        final List<Long> capacities = new ArrayList<>(RaidMode.values().length);
        for (final var mode : RaidMode.values()) {
            // NONE presents the drives as they are; the others present the array they would form.
            capacities.add(mode == RaidMode.NONE
                    ? sizes.stream().mapToLong(Long::longValue).sum()
                    : mode.usableCapacity(sizes));
        }
        return new FirmwareStatePayload.RaidInfo(true, rack.raidModeOf(slot).id(),
                rack.raidMemberCount(slot), sizes.size(), capacities);
    }

    private static void handleFirmwareAction(final FirmwareActionPayload payload, final ServerPlayer player,
                                             final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        switch (payload.action()) {
            case FirmwareActionPayload.ACTION_BOOT_DISK -> {
                computer.setBootDiskSlot((int) payload.ref());
                computer.setPendingInstallSlot(IOsHost.NO_PENDING_INSTALL); // the reboot the installer asked for
                if (computer.hasOs()) {
                    /*
                     * Booting a disk from the firmware is a restart, so it replays POST like any
                     * other. Handing straight over to the system skipped the self-test the machine
                     * has to run, and left the session fixed on whatever it was before.
                     */
                    computer.setNeedsPost(true);
                    MonitorBlock.openPost(
                            player, level, payload.monitorPos(), payload.hostPos());
                    return;
                }
            }
            case FirmwareActionPayload.ACTION_HOLD_BOOT_MENU -> {
                if (computer instanceof AbstractComputerBlockEntity machine) {
                    machine.holdBootMenu();
                }
                return;
            }
            case FirmwareActionPayload.ACTION_OPEN_SETUP -> {
                // Leaving the menu for the setup: the machine is no longer on its way anywhere until it is told.
                if (computer instanceof AbstractComputerBlockEntity machine) {
                    machine.holdBootMenu();
                }
                MonitorBlock.openFirmware(
                        player, level, payload.monitorPos(), payload.hostPos());
                return;
            }
            case FirmwareActionPayload.ACTION_BOOT_ONCE -> {
                /*
                 * The one-time menu: boot that disk now and leave the saved order where it is. The machine has
                 * already tested itself, so this hands straight over rather than starting again.
                 */
                computer.setPendingInstallSlot(IOsHost.NO_PENDING_INSTALL);
                if (computer instanceof AbstractComputerBlockEntity machine) {
                    /*
                     * The choice names where it sat in the manager's list, not the disk, because a disk carries
                     * several systems and two entries can share one. The list is built again here from the same
                     * machine the player was looking at, so the place in it means the same thing on both sides.
                     */
                    final BootMenu list = BootLines.menuFor(machine, 0);
                    final int at = (int) payload.ref();
                    if (at < 0 || at >= list.entries().size()) {
                        return;
                    }
                    final BootMenu.Entry chosen = list.entries().get(at);
                    machine.setBootOnce(chosen.slot(),
                            ResourceLocation.tryParse(chosen.osId()));
                    /*
                     * Chosen at the boot manager, the system still has to come up: the machine leaves the menu and
                     * starts loading, and puts the player in front of that. Chosen anywhere else there is nothing
                     * left to load, so it hands straight over.
                     */
                    if (machine.atBootMenu()) {
                        machine.leaveBootMenu();
                        return;
                    }
                }
                MonitorBlock.openBootTarget(
                        player, level, payload.monitorPos(), payload.hostPos());
                return;
            }
            case FirmwareActionPayload.ACTION_SET_BOOT -> computer.setBootDiskSlot((int) payload.ref());
            case FirmwareActionPayload.ACTION_RAID_MODE -> {
                final var mode = RaidMode.find((int) payload.ref());
                if (computer instanceof ServerRackBlockEntity rack && mode != null) {
                    rack.setRaidMode(rack.soleComputerSlot(), mode);
                }
            }
            case FirmwareActionPayload.ACTION_FORMAT -> computer.formatDisk((int) payload.ref());
            case FirmwareActionPayload.ACTION_INSTALL -> {
                /*
                 * The setup does not run an installation; it restarts the machine into one. A computer put
                 * a system on a disk by booting the medium that carries it, so the firmware's part is to say
                 * what this machine starts from next and then start it: the self-test runs again, and what
                 * comes up after it is the installer rather than the setup the player pressed the button on.
                 *
                 * The order matters. Asking for the self-test first clears whatever the machine was holding,
                 * which is what a restart does; the installation is set up after that, so it survives into
                 * the machine the self-test hands over to.
                 */
                computer.setNeedsPost(true);
                final String failure = beginInstall(level, computer, payload.ref(), payload.target());
                if (failure != null) {
                    // Refused before a minute of copying: say so instead of restarting into nothing.
                    final HardwareEra era = computer.displayEra();
                    final int kind = FirmwareKind
                            .forEra(era != null ? era : HardwareEra.STANDARD).id();
                    final int slot = payload.target();
                    computer.setNeedsPost(false);
                    PacketDistributor.sendToPlayer(player, new OpenInstallDonePayload(payload.hostPos(),
                            payload.monitorPos(), kind, "",
                            slot < 0 ? "the default disk" : "Disk " + slot, slot, failure));
                    MonitorBlock.openSession(player, level, payload.monitorPos(), payload.hostPos(), computer,
                            MonitorSessionMenu.Phase.INSTALL_PROGRESS);
                    return;
                }
                MonitorBlock.openSession(player, level, payload.monitorPos(), payload.hostPos());
            }
            case FirmwareActionPayload.ACTION_BOOT_MEDIA -> {
                if (level.getBlockEntity(BlockPos.of(payload.ref())) instanceof MediaReaderBlockEntity reader
                        && reader.insertedKind() == MediaKind.OS_INSTALL && reader.insertedPayload() != null) {
                    final OsDef os = OsRegistry.getOs(reader.insertedPayload());
                    if (os != null && os.installMode()
                            != InstallMode.GUIDED) {
                        // A live medium: boot its shell and let the player install the system by hand.
                        computer.console().startLiveInstall(os.id().getPath().equals("arch")
                                ? LiveInstallState.Distro.ARCH
                                : LiveInstallState.Distro.GENTOO);
                        computer.setChanged();
                        MonitorBlock.openBootTarget(
                                player, level, payload.monitorPos(), payload.hostPos());
                        return;
                    } else {
                        final int target = payload.target() >= 0 ? payload.target() : computer.defaultInstallSlot();
                        if (installOsFromReader(level, computer, payload.ref(), target)) {
                            computer.setBootDiskSlot(target);
                            MonitorBlock.openBootTarget(
                                    player, level, payload.monitorPos(), payload.hostPos());
                            return;
                        }
                    }
                }
            }
            default -> {
            }
        }
        PacketDistributor.sendToPlayer(player, buildFirmwareState(level, computer, payload.hostPos()));
    }

    /**
     * Installs the OS from the medium in the reader at {@code readerPos} (or from any linked installer medium
     * when {@code -1}) onto disk slot {@code targetSlot} ({@code -1} = the default target). Unlike the legacy
     * no-OS path this allows a second system beside an installed one (dual boot). Returns whether it installed.
     */
    public static boolean installOsFromReader(final ServerLevel level, final IOsHost computer,
                                              final long readerPos, final int targetSlot) {
        return beginInstall(level, computer, readerPos, targetSlot) == null;
    }


    /**
     * Starts putting the system in a linked drive onto a disk, telling why it did not start: {@code null} once
     * the machine is copying, otherwise a sentence for the player.
     *
     * <p>Everything a machine can refuse for is asked here, before a minute of copying: a system newer than the
     * machine's era, a live medium that installs by hand, no room on the disk. What happens after that belongs
     * to the machine, and {@link OsInstallRunner} carries it.
     */
    @Nullable
    public static String beginInstall(final ServerLevel level, final IOsHost computer,
                                      final long readerPos, final int targetSlot) {
        final HardwareEra hostEra = computer.installedEra() != null ? computer.installedEra() : HardwareEra.STANDARD;
        String failure = null;
        for (final long endpoint : computer.linkedEndpoints()) {
            /*
             * -1 is the word for "any drive with an installer in it"; anything else names one drive by its packed
             * position. Asking whether that position is positive is not the same question: a drive west or north
             * of the world's origin packs into a negative long, so on those bases naming a drive was read as
             * naming none of them, and the machine installed from whichever it found first.
             */
            if (readerPos != ANY_READER && endpoint != readerPos) {
                continue;
            }
            if (!(level.getBlockEntity(BlockPos.of(endpoint)) instanceof MediaReaderBlockEntity reader)
                    || reader.insertedKind() != MediaKind.OS_INSTALL || reader.insertedPayload() == null) {
                continue;
            }
            final OsDef def = OsRegistry.getOs(reader.insertedPayload());
            if (def == null) {
                failure = "The system on the medium is not known to this machine.";
                continue;
            }
            if (!OsGating.canInstall(def.minEra(), hostEra)) {
                failure = def.displayName() + " needs " + eraName(def.minEra()) + " era hardware or newer; this machine is "
                        + eraName(hostEra) + " era.";
                continue;
            }
            /*
             * A live/source medium (Arch, Gentoo) never one-click installs: it must be BOOTED and the
             * system put on the disk by hand through its shell. Only guided installers land here.
             */
            if (def.installMode() != InstallMode.GUIDED) {
                failure = def.displayName() + " is put on the disk by hand from its own shell: boot the medium instead.";
                continue;
            }
            final String noRoom = computer.defaultInstallSlot() < 0
                    ? "No disk is installed to put " + def.displayName() + " on."
                    : "The target disk has no room for " + def.displayName() + " ("
                            + def.footprintMb() + " MB needed).";
            /*
             * A machine that holds a copy of its own takes as long over it as the system is big and the medium
             * is slow, and goes on with it whether or not anybody is watching. A host with nowhere to keep the
             * work writes the system there and then, the way every machine used to.
             */
            if (!computer.keepsInstalls()) {
                if (computer.installOs(def.id(), targetSlot)) {
                    computer.setPendingInstallSlot(targetSlot);
                    return null;
                }
                return noRoom;
            }
            final IOsHost machine = computer;
            /*
             * Room is no longer asked once and for all here: the installer offers the disks and the player
             * chooses one, and a disk that is full can be erased on the way. What still stops it before it
             * starts is a machine with no disk at all, which has nothing to offer.
             */
            /*
             * How fast this machine copies, from the parts it is made of rather than from its generation: the
             * medium it reads and the processor that unpacks it here, and the disk it writes to added by the
             * installer once the player has chosen one. A machine of an age used to take exactly as long as
             * every other machine of that age, so nothing a player put in it ever showed.
             */
            final double baseRate = SetupTiming.installRate(reader.insertedFormat(), 1,
                    machine.cpuCores(), machine.maxCpuMhz());
            final InstallerFlow flow =
                    Installers.beginning(machine, level, def, baseRate);
            if (flow.disks().isEmpty()) {
                return noRoom;
            }
            flow.select(targetSlot);
            machine.setInstaller(flow);
            machine.setInstalling(new OsInstallJob(def.id().toString(),
                    flow.targetSlot(), endpoint, flow.ticksTotal(), flow.ticksTotal()));
            return null;
        }
        return failure != null ? failure : "No installation medium is in a drive linked to this machine.";
    }

    /** The era as the firmware names it to the player: "Vintage", "Legacy", "Standard" ... */
    private static String eraName(final HardwareEra era) {
        final String lower = era.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void handleRequestFirmware(final RequestFirmwarePayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost) {
            // Leave whatever screen the request came from (the desktop or the terminal) and enter setup.
            player.closeContainer();
            MonitorBlock.openFirmware(
                    player, level, payload.monitorPos(), payload.hostPos());
        }
    }

    private static void handlePostComplete(final PostCompletePayload payload, final ServerPlayer player,
                                           final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer)) {
            return;
        }
        if (!computer.isRunning()) {
            return; // powered off mid-POST: the screen just stays dark
        }
        computer.setNeedsPost(false);
        /*
         * A key at a failed self-test is what ends the standing about. Until it is pressed the machine is
         * still at its failure and every monitor opened on it shows that; after it, the machine goes on to
         * whatever it can still be asked for, which with nothing on any disk is its setup.
         */
        computer.resumeFromHalt();
        /*
         * POST is the moment the machine decides what it is running. Fixing it here is what makes a
         * freshly installed (or removed) desktop package wait for a restart instead of appearing the
         * next time the monitor is opened.
         */
        computer.setBootedDesktopId(computer.installedDesktopId());
        if (payload.enterSetup()) {
            MonitorBlock.openFirmware(
                    player, level, payload.monitorPos(), payload.hostPos());
        } else {
            MonitorBlock.openBootTarget(
                    player, level, payload.monitorPos(), payload.hostPos());
        }
    }

    /**
     * Scans the computer's linked peripheral endpoints for a {@link MediaReaderBlockEntity}
     * holding an OS installer medium. Takes the first match whose OS passes the era gate and
     * whose footprint fits the computer's free storage, then calls
     * {@link IOsHost#installOs(ResourceLocation)}.
     *
     * <p>The reader must be linked to the computer over the COMPUTING peripheral cable system
     * (same way a monitor links). Only readers that are already auto-linked endpoints are
     * considered; a reader placed in the world but not yet linked on the peripheral system
     * will not be found here.
     *
     * <p>All gating conditions must be satisfied in order:
     * <ol>
     *   <li>The computer block entity must be an {@link IOsHost} with no OS yet.</li>
     *   <li>A linked endpoint must resolve to a {@link MediaReaderBlockEntity} holding a medium
     *       of kind {@link MediaKind#OS_INSTALL} whose payload names a registered {@link OsDef}.</li>
     *   <li>{@link OsGating#canInstall} must accept the OS on the computer's hardware era.</li>
     * </ol>
     * A silent no-op is the correct outcome when any condition is unmet; the firmware screen will
     * remain open and the player can fix the configuration before trying again.
     *
     * @param level       the server level the computer lives in
     * @param computerPos the position of the computer to install the OS onto
     */
    public static void installOsFromLinkedReader(final ServerLevel level, final BlockPos computerPos) {
        if (!(level.getBlockEntity(computerPos) instanceof IOsHost computer)) {
            return;
        }
        if (computer.hasOs()) {
            return; // already installed; nothing to do
        }
        final HardwareEra hostEra = computer.installedEra() != null
                ? computer.installedEra()
                : HardwareEra.STANDARD;

        // Walk every linked peripheral endpoint and look for a media reader with an OS installer.
        for (final long endpointLong : computer.linkedEndpoints()) {
            final BlockPos endpointPos = BlockPos.of(endpointLong);
            if (!(level.getBlockEntity(endpointPos) instanceof MediaReaderBlockEntity reader)) {
                continue;
            }
            if (reader.insertedKind() != MediaKind.OS_INSTALL) {
                continue;
            }
            final ResourceLocation osId = reader.insertedPayload();
            if (osId == null) {
                continue;
            }
            final OsDef def = OsRegistry.getOs(osId);
            if (def == null) {
                continue;
            }
            if (!OsGating.canInstall(def.minEra(), hostEra)) {
                continue;
            }
            // Live/source media (Arch, Gentoo) install only by hand through their booted shell.
            if (def.installMode() != InstallMode.GUIDED) {
                continue;
            }
            // installOs checks the footprint against free storage; false means it did not fit.
            computer.installOs(osId);
            return; // first valid linked reader wins
        }
    }
}
