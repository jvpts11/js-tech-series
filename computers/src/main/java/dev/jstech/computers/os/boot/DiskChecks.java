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
import dev.jstech.computers.os.InstallMode;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.storage.DiskUsage;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.world.item.ItemStack;

/**
 * The disks a system had in use when its machine went dark suddenly, and the check it makes of them the next time it
 * comes up (computers/docs/design/power.md). The mark rides on each disk, as a real file system's dirty bit does, so a
 * disk moved to another machine is checked there; a clean shutdown leaves none.
 *
 * <p>A disk carries no partitions yet, so every disk of the machine is taken to be formatted the way its system's
 * installer formats its own ({@link FileSystemType#installedBy}), and checked as one.
 */
public final class DiskChecks {

    private DiskChecks() {
    }

    /** Marks the disks of {@code machine}'s system as left unclean: it went dark with them in use. */
    public static void markUnclean(final IOsHost machine) {
        if (machine.installedOs() == null) {
            return;
        }
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (DriveVolumes.isDisk(disk)) {
                disk.set(ComputingComponents.DISK_UNCLEAN.get(), true);
            }
        }
    }

    /** Whether {@code disk} was left unclean by a machine that went dark with it in use. */
    public static boolean unclean(final ItemStack disk) {
        return Boolean.TRUE.equals(disk.get(ComputingComponents.DISK_UNCLEAN.get()));
    }

    /**
     * The check {@code machine}'s system makes as it comes up: the disks left unclean, read as long as their file
     * system takes; {@link DiskCheck#NONE} when none was, or the system checks nothing.
     */
    public static DiskCheck atBoot(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return DiskCheck.NONE;
        }
        final DiskCheck.Look look = DiskCheck.Look.of(system.platform(), system.familyRank());
        if (look == DiskCheck.Look.NONE) {
            return DiskCheck.NONE;
        }
        boolean any = false;
        long items = 0L;
        long usedMb = 0L;
        long capacityMb = 0L;
        int kinds = 0;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (!unclean(disk) || !(HardwareRegistry.disk(disk) instanceof DiskSpec spec)) {
                continue;
            }
            any = true;
            final DiskUsage usage = DriveVolumes.usage(disk);
            final long held = usage.usedWeight() / StorageKey.MB_EQ_PER_ITEM;
            items += held;
            usedMb += held * EraStorage.mbPerItem(spec.era());
            capacityMb += spec.capacityMb();
            kinds += usage.types();
        }
        if (!any) {
            return DiskCheck.NONE;
        }
        final FileSystemType type = FileSystemType.installedBy(system.platform(), system.familyRank(),
                system.installMode() != InstallMode.GUIDED, system.id().getPath(), machine.installedEra());
        return new DiskCheck(look, type, usedMb, capacityMb, kinds, type.recovery().ticksFor(items));
    }

    /**
     * {@code machine}'s system came up: what it checked is clean again, and a copy-on-write system had nothing to
     * check. A system that checks nothing (MC-DOS, MC-NET) leaves the marks where they are, for one that does.
     *
     * @return whether any disk was made clean, which the machine then has to save
     */
    public static boolean cameUp(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        if (system == null || DiskCheck.Look.of(system.platform(), system.familyRank()) == DiskCheck.Look.NONE) {
            return false;
        }
        boolean cleaned = false;
        for (int slot = 0; slot < machine.diskSlots(); slot++) {
            final ItemStack disk = machine.diskInSlot(slot);
            if (unclean(disk)) {
                disk.remove(ComputingComponents.DISK_UNCLEAN.get());
                cleaned = true;
            }
        }
        return cleaned;
    }
}
