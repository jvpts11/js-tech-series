/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.DiskUsage;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.tier.Era;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The volumes a system had in use when its machine went dark suddenly, and the check it makes of them the next time it
 * comes up (computers/docs/design/power.md). The mark rides on each volume, as a real file system's dirty bit does, so
 * a disk moved to another machine is checked there; a clean shutdown leaves none.
 *
 * <p>A system has in use the volumes it mounts, those in a file system it reads (computers/docs/design/
 * storage-devices.md), and each gets back on its feet the way its own file system does: a long check without a
 * journal, two seconds with one, nothing on copy-on-write. The check reads them one after the other, and shows the
 * file system of the one that takes longest.
 */
public final class DiskChecks {

    private DiskChecks() {
    }

    /** Marks the volumes {@code machine}'s system had in use as left unclean: it went dark with them mounted. */
    public static void markUnclean(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return;
        }
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!DriveVolumes.isDisk(disk)) {
                continue;
            }
            for (final DiskPartitions.Volume volume : DiskPartitions.volumes(disk)) {
                if (mounts(system, machine.installedEra(), volume)) {
                    volume.stack().set(ComputingComponents.DISK_UNCLEAN.get(), true);
                    volume.commit();
                }
            }
        }
    }

    /** Whether {@code volume} was left unclean by a machine that went dark with it in use. */
    public static boolean unclean(final ItemStack volume) {
        return Boolean.TRUE.equals(volume.get(ComputingComponents.DISK_UNCLEAN.get()));
    }

    /**
     * The check {@code machine}'s system makes as it comes up: the volumes it mounts left unclean, each read as long as
     * its file system takes; {@link DiskCheck#NONE} when none was, or the system checks nothing.
     */
    public static DiskCheck atBoot(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return DiskCheck.NONE;
        }
        final DiskCheck.Look family = DiskCheck.Look.of(system.platform(), system.familyRank());
        if (family == DiskCheck.Look.NONE) {
            return DiskCheck.NONE;
        }
        // OpenBSD names its root disk by the bus it hangs on: wd on the IDE of the older machines, sd on SATA.
        final DiskCheck.Look look = family == DiskCheck.Look.OPENBSD && machine.installedEra() != null
                && !Installers.ideDisk(machine.installedEra()) ? DiskCheck.Look.OPENBSD_SD : family;
        FileSystemType slowest = null;
        int slowestTicks = -1;
        int ticks = 0;
        long usedMb = 0L;
        long capacityMb = 0L;
        int kinds = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!(HardwareRegistry.disk(disk) instanceof DiskSpec spec)) {
                continue;
            }
            for (final DiskPartitions.Volume volume : DiskPartitions.volumes(disk)) {
                if (!mounts(system, machine.installedEra(), volume) || !unclean(volume.stack())) {
                    continue;
                }
                final FileSystemType fs = volume.partition().fs();
                final DiskUsage usage = DriveVolumes.usage(volume.stack());
                final long held = usage.usedWeight() / StorageKey.MB_EQ_PER_ITEM;
                final int took = fs.recovery().ticksFor(held);
                ticks += took;
                usedMb += held * EraStorage.mbPerItem(spec.era());
                capacityMb += volume.partition().sizeMb();
                kinds += usage.types();
                if (took > slowestTicks) {
                    slowest = fs;
                    slowestTicks = took;
                }
            }
        }
        if (slowest == null) {
            return DiskCheck.NONE;
        }
        return new DiskCheck(look, slowest, usedMb, capacityMb, kinds, ticks);
    }

    /**
     * {@code machine}'s system came up: what it checked is clean again, and a copy-on-write system had nothing to
     * check. A system that checks nothing (MC-DOS, MC-NET) leaves the marks where they are, for one that does, as does
     * every system for a volume it does not mount.
     *
     * @return whether any volume was made clean, which the machine then has to save
     */
    public static boolean cameUp(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        if (system == null || DiskCheck.Look.of(system.platform(), system.familyRank()) == DiskCheck.Look.NONE) {
            return false;
        }
        boolean cleaned = false;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!DriveVolumes.isDisk(disk)) {
                continue;
            }
            for (final DiskPartitions.Volume volume : DiskPartitions.volumes(disk)) {
                if (mounts(system, machine.installedEra(), volume) && unclean(volume.stack())) {
                    volume.stack().remove(ComputingComponents.DISK_UNCLEAN.get());
                    volume.commit();
                    cleaned = true;
                }
            }
        }
        return cleaned;
    }

    /* Whether the system mounts that volume: it holds a file system the system reads. */
    private static boolean mounts(final OsDef system, @Nullable final Era era, final DiskPartitions.Volume volume) {
        final FileSystemType fs = volume.partition() == null ? null : volume.partition().fs();
        return fs != null && fs.readBy(system.platform(), era);
    }
}
