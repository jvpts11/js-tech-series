/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.firmware;

import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.FirmwareActionPayload;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.OpenInstallDonePayload;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.computers.operation.payload.PostCompletePayload;
import dev.jstech.computers.operation.payload.RequestFirmwarePayload;
import dev.jstech.computers.operation.payload.RequestFirmwareStatePayload;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsGating;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
                    // The same hardware state feeds the setup screen and the POST's device-detection lines.
                    dev.jstech.computers.client.FirmwareScreen.accept(payload);
                    dev.jstech.computers.client.BootSequenceScreen.accept(payload);
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
                        dev.jstech.computers.block.IPostScreenOpener.Holder.open(
                                payload.host(), payload.monitorPos(),
                                dev.jstech.computers.os.FirmwareKind.byId(payload.firmwareKind()),
                                payload.name(), payload.remainingTicks())));
        // A finished installer still waiting for its reboot: the monitor comes back to that prompt.
        registrar.playToClient(OpenInstallDonePayload.TYPE, OpenInstallDonePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        dev.jstech.computers.block.IInstallDoneScreenOpener.Holder.open(
                                payload.host(), payload.monitorPos(),
                                dev.jstech.computers.os.FirmwareKind.byId(payload.firmwareKind()),
                                payload.osName(), payload.targetLabel(), payload.targetSlot(), payload.failure())));
        ComputerAccess.accept(registrar, PostCompletePayload.TYPE, PostCompletePayload.STREAM_CODEC,
                ComputerAccess.screen(PostCompletePayload::hostPos), FirmwarePayloads::handlePostComplete);
    }

    private static void handleRequestFirmwareState(final RequestFirmwareStatePayload payload, final ServerPlayer player,
                                                   final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost computer) {
            PacketDistributor.sendToPlayer(player, buildFirmwareState(level, computer, payload.hostPos()));
        }
    }

    /** Everything the boot manager lists for {@code computer}: disks, linked media, boot order, hardware. */
    static FirmwareStatePayload buildFirmwareState(final ServerLevel level,
                                                  final IOsHost computer, final BlockPos pos) {
        final HardwareEra era = computer.displayEra() != null ? computer.displayEra() : HardwareEra.STANDARD;
        final List<FirmwareStatePayload.Entry> entries = new ArrayList<>();
        for (int i = 0; i < computer.diskSlots(); i++) {
            final net.minecraft.world.item.ItemStack disk = computer.diskInSlot(i);
            if (!(disk.getItem() instanceof dev.jstech.computers.item.DiskItem)) {
                continue;
            }
            final net.minecraft.resources.ResourceLocation osId =
                    disk.get(dev.jstech.computers.ComputingModule.SYSTEM_OS.get());
            final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
            entries.add(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_DISK, i,
                    os == null ? "" : os.id().toString(),
                    os == null ? "(no system)" : os.displayName(),
                    "Disk " + i + ": " + disk.getHoverName().getString(),
                    os != null, -1));
        }
        for (final long endpoint : computer.linkedEndpoints()) {
            if (!(level.getBlockEntity(BlockPos.of(endpoint)) instanceof MediaReaderBlockEntity reader)) {
                continue;
            }
            final String drive = reader.driveType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
            final net.minecraft.world.item.ItemStack media = reader.mediaSlot().getStackInSlot(0);
            if (media.isEmpty()) {
                entries.add(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, endpoint, "",
                        "(no medium)", drive, false, -1));
                continue;
            }
            final OsDef os = reader.insertedKind() == MediaKind.OS_INSTALL && reader.insertedPayload() != null
                    ? OsRegistry.getOs(reader.insertedPayload()) : null;
            if (os == null) {
                entries.add(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, endpoint, "",
                        media.getHoverName().getString(), drive, false, -1));
                continue;
            }
            final boolean eraOk = OsGating.canInstall(os.minEra(), era);
            entries.add(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, endpoint,
                    os.id().toString(),
                    os.displayName() + (os.installMode() == dev.jstech.computers.os.InstallMode.GUIDED
                            ? " installer" : " (live)"),
                    drive + (eraOk ? "" : " - " + eraName(os.minEra()) + " era or newer"), eraOk,
                    os.installMode().id()));
        }
        final int cpuMhz = computer.maxCpuMhz();
        final String cpuLabel = cpuMhz > 0 ? cpuMhz + " MHz" : "not detected";
        final int ramMb = (int) Math.min(Integer.MAX_VALUE, computer.ramBuffer());
        return new FirmwareStatePayload(pos, era.id(), cpuLabel, cpuMhz, ramMb, computer.bootDiskSlot(),
                computer.defaultInstallSlot(), entries, raidInfoOf(level, computer));
    }

    /**
     * What the firmware's storage page shows: the controller in this machine's bay, the array it
     * runs, and what each mode would give. Only a rack server has one, since a desk computer's firmware
     * simply has no storage page.
     */
    private static FirmwareStatePayload.RaidInfo raidInfoOf(
            final ServerLevel level, final dev.jstech.computers.os.IOsHost computer) {
        if (!(computer instanceof dev.jstech.computers.blockentity
                .ServerRackBlockEntity rack)) {
            return FirmwareStatePayload.RaidInfo.ABSENT;
        }
        final int slot = rack.soleComputerSlot();
        if (slot < 0 || rack.raidControllerSlot(slot) < 0) {
            return FirmwareStatePayload.RaidInfo.ABSENT;
        }
        final List<Long> sizes = new ArrayList<>();
        for (final ItemStack drive : rack.claimedDriveStacks(slot)) {
            if (drive.getItem() instanceof dev.jstech.computers.item.DiskItem disk) {
                sizes.add(disk.spec().capacityItems());
            }
        }
        final List<Long> capacities = new ArrayList<>(dev.jstech.computers.rack.RaidMode.values().length);
        for (final var mode : dev.jstech.computers.rack.RaidMode.values()) {
            // NONE presents the drives as they are; the others present the array they would form.
            capacities.add(mode == dev.jstech.computers.rack.RaidMode.NONE
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
                    dev.jstech.computers.block.MonitorBlock.openPost(
                            player, level, payload.monitorPos(), payload.hostPos());
                    return;
                }
            }
            case FirmwareActionPayload.ACTION_SET_BOOT -> computer.setBootDiskSlot((int) payload.ref());
            case FirmwareActionPayload.ACTION_RAID_MODE -> {
                final var mode = dev.jstech.computers.rack.RaidMode.find((int) payload.ref());
                if (computer instanceof dev.jstech.computers.blockentity
                        .ServerRackBlockEntity rack && mode != null) {
                    rack.setRaidMode(rack.soleComputerSlot(), mode);
                }
            }
            case FirmwareActionPayload.ACTION_FORMAT -> computer.formatDisk((int) payload.ref());
            case FirmwareActionPayload.ACTION_INSTALL -> {
                final String failure = installFailure(level, computer, payload.ref(), payload.target());
                if (failure != null) {
                    /*
                     * The client's installer has just played its progress to the end: end it on the
                     * refusal, not on a "complete" the disk never saw.
                     */
                    final HardwareEra era = computer.displayEra();
                    final int slot = payload.target();
                    ScreenSessions.opened(player, payload.monitorPos(), payload.hostPos());
                    PacketDistributor.sendToPlayer(player, new OpenInstallDonePayload(payload.hostPos(),
                            payload.monitorPos(),
                            dev.jstech.computers.os.FirmwareKind
                                    .forEra(era != null ? era : HardwareEra.STANDARD).id(),
                            "", slot < 0 ? "the default disk" : "Disk " + slot, slot, failure));
                }
            }
            case FirmwareActionPayload.ACTION_BOOT_MEDIA -> {
                if (level.getBlockEntity(BlockPos.of(payload.ref())) instanceof MediaReaderBlockEntity reader
                        && reader.insertedKind() == MediaKind.OS_INSTALL && reader.insertedPayload() != null) {
                    final OsDef os = OsRegistry.getOs(reader.insertedPayload());
                    if (os != null && os.installMode()
                            != dev.jstech.computers.os.InstallMode.GUIDED) {
                        // A live medium: boot its shell and let the player install the system by hand.
                        computer.console().startLiveInstall(os.id().getPath().equals("arch")
                                ? dev.jstech.computers.program.install.LiveInstallState.Distro.ARCH
                                : dev.jstech.computers.program.install.LiveInstallState.Distro.GENTOO);
                        computer.setChanged();
                        dev.jstech.computers.block.MonitorBlock.openBootTarget(
                                player, level, payload.monitorPos(), payload.hostPos());
                        return;
                    } else {
                        final int target = payload.target() >= 0 ? payload.target() : computer.defaultInstallSlot();
                        if (installOsFromReader(level, computer, payload.ref(), target)) {
                            computer.setBootDiskSlot(target);
                            dev.jstech.computers.block.MonitorBlock.openBootTarget(
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
        return installFailure(level, computer, readerPos, targetSlot) == null;
    }

    /**
     * The install behind {@link #installOsFromReader}, telling why it did not happen: {@code null} once the
     * system is on the disk, otherwise a sentence for the player. The installer screen plays its progress
     * on the client before the write, so without this a refused install (a system newer than the machine's
     * era, a live medium, no room on the disk) looked exactly like a finished one.
     */
    @Nullable
    public static String installFailure(final ServerLevel level, final IOsHost computer,
                                        final long readerPos, final int targetSlot) {
        final HardwareEra hostEra = computer.installedEra() != null ? computer.installedEra() : HardwareEra.STANDARD;
        String failure = null;
        for (final long endpoint : computer.linkedEndpoints()) {
            if (readerPos >= 0 && endpoint != readerPos) {
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
            if (def.installMode() != dev.jstech.computers.os.InstallMode.GUIDED) {
                failure = def.displayName() + " is put on the disk by hand from its own shell: boot the medium instead.";
                continue;
            }
            if (computer.installOs(def.id(), targetSlot)) {
                /*
                 * The files are on the disk, but the machine is still running the installer until it
                 * restarts: remember that, so the monitor comes back to the reboot prompt, not the system.
                 */
                computer.setPendingInstallSlot(targetSlot);
                return null;
            }
            return computer.defaultInstallSlot() < 0
                    ? "No disk is installed to put " + def.displayName() + " on."
                    : "The target disk has no room for " + def.displayName() + " ("
                            + def.footprintMb() + " MB needed).";
        }
        return failure != null ? failure : "No installation medium is in a drive linked to this machine.";
    }

    /** The era as the firmware names it to the player: "Vintage", "Legacy", "Standard" ... */
    private static String eraName(final HardwareEra era) {
        final String lower = era.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void handleRequestFirmware(final RequestFirmwarePayload payload, final ServerPlayer player,
                                              final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IOsHost) {
            // Leave whatever screen the request came from (the desktop or the terminal) and enter setup.
            player.closeContainer();
            dev.jstech.computers.block.MonitorBlock.openFirmware(
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
         * POST is the moment the machine decides what it is running. Fixing it here is what makes a
         * freshly installed (or removed) desktop package wait for a restart instead of appearing the
         * next time the monitor is opened.
         */
        computer.setBootedDesktopId(computer.installedDesktopId());
        if (payload.enterSetup()) {
            dev.jstech.computers.block.MonitorBlock.openFirmware(
                    player, level, payload.monitorPos(), payload.hostPos());
        } else {
            dev.jstech.computers.block.MonitorBlock.openBootTarget(
                    player, level, payload.monitorPos(), payload.hostPos());
        }
    }

    /**
     * Scans the computer's linked peripheral endpoints for a {@link MediaReaderBlockEntity}
     * holding an OS installer medium. Takes the first match whose OS passes the era gate and
     * whose footprint fits the computer's free storage, then calls
     * {@link IOsHost#installOs(net.minecraft.resources.ResourceLocation)}.
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
            final net.minecraft.resources.ResourceLocation osId = reader.insertedPayload();
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
            if (def.installMode() != dev.jstech.computers.os.InstallMode.GUIDED) {
                continue;
            }
            // installOs checks the footprint against free storage; false means it did not fit.
            computer.installOs(osId);
            return; // first valid linked reader wins
        }
    }
}
