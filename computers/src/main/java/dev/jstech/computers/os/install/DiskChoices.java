/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.FileSystemType;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What an installer's disk step offers to choose from (computers/docs/design/storage-devices.md): every partition and
 * free space for Frames XP's text phase and the table of Frames 7 to 11, the file systems of Frames XP's format page,
 * and Ubuntu's three ways to install. Pure: the screen draws them, the machine is told the one chosen.
 */
public final class DiskChoices {

    private DiskChoices() {
    }

    /** Every partition and free space of every disk, in the order they lie and the disks are fitted. */
    public static List<Pick> picks(final InstallerFlow flow) {
        final List<Pick> picks = new ArrayList<>();
        for (int d = 0; d < flow.disks().size(); d++) {
            final InstallerFlow.Disk disk = flow.disks().get(d);
            for (final DiskSpot spot : disk.spots()) {
                picks.add(new Pick(disk, spot, d));
            }
        }
        return picks;
    }

    /**
     * Where the installer's own choice sits among the picks: the partition or free space chosen, else the one its plan
     * puts the system on, else the first of the chosen disk, else the first of all.
     */
    public static int indexOfChosen(final InstallerFlow flow, final List<Pick> picks) {
        final InstallerFlow.Disk target = flow.target();
        if (target == null) {
            return 0;
        }
        long start = flow.placement().startMb();
        if (start == Placement.ANYWHERE) {
            final InstallPlan.Plan plan = flow.plan();
            start = plan.ok() && plan.system() != null && !plan.wholeDisk() ? plan.system().startMb() : -1L;
        }
        int first = -1;
        for (int i = 0; i < picks.size(); i++) {
            final Pick pick = picks.get(i);
            if (pick.disk().slot() != target.slot()) {
                continue;
            }
            if (first < 0) {
                first = i;
            }
            if (start >= 0L && pick.spot().startMb() <= start && start < pick.spot().endMb()) {
                return i;
            }
        }
        return Math.max(0, first);
    }

    /**
     * The file systems Frames XP's text phase offers for the partition or free space chosen, in its order: NTFS and FAT
     * quick, then NTFS and FAT in full, and leaving the file system as it is only for a partition the system lives on.
     * FAT is FAT16 on a partition of 2 GB or less and FAT32 above.
     */
    public static List<Format> xpFormats(final InstallerFlow flow) {
        final DiskSpot spot = chosenSpot(flow);
        final long sizeMb = spot == null ? 0L : spot.sizeMb();
        final String fat = (sizeMb <= FileSystemType.FAT16.mostPartitionMb() ? FileSystemType.FAT16
                : FileSystemType.FAT32).serializedName();
        final String ntfs = FileSystemType.NTFS.serializedName();
        final List<Format> formats = new ArrayList<>(List.of(new Format(ntfs, true), new Format(fat, true),
                new Format(ntfs, false), new Format(fat, false)));
        if (spot != null && !spot.isFree() && spot.fileSystem() != null
                && flow.facts().carriers().contains(spot.fileSystem())) {
            formats.add(new Format(Placement.KEEP, false));
        }
        return formats;
    }

    /** The partition or free space the system is going on, or null when none is chosen. */
    @Nullable
    public static DiskSpot chosenSpot(final InstallerFlow flow) {
        final InstallerFlow.Disk target = flow.target();
        if (target == null) {
            return null;
        }
        final List<Pick> picks = picks(flow);
        return picks.isEmpty() ? null : picks.get(indexOfChosen(flow, picks)).spot();
    }

    /**
     * Ubuntu's ways to install, in its order: beside the system on a disk when one can be shrunk, an entire disk for
     * every disk, and the custom layout, which puts the system in the largest free space.
     */
    public static List<Option> ubuntuOptions(final InstallerFlow flow) {
        final List<Option> options = new ArrayList<>();
        for (final InstallerFlow.Disk disk : flow.disks()) {
            if (InstallPlan.besideOf(flow, disk) != null) {
                options.add(new Option(Option.Kind.ALONGSIDE, disk));
                break;
            }
        }
        for (final InstallerFlow.Disk disk : flow.disks()) {
            options.add(new Option(Option.Kind.ENTIRE, disk));
        }
        final InstallerFlow.Disk target = flow.target();
        options.add(new Option(Option.Kind.CUSTOM, target != null ? target
                : flow.disks().isEmpty() ? null : flow.disks().get(0)));
        return options;
    }

    /** Which of Ubuntu's ways the installer is set on, by what it holds. */
    public static Option.Kind ubuntuChosen(final InstallerFlow flow) {
        final InstallerFlow.Disk target = flow.target();
        if (target != null && flow.eraseSlot() == target.slot()) {
            return Option.Kind.ENTIRE;
        }
        if (flow.placement().startMb() != Placement.ANYWHERE) {
            return Option.Kind.CUSTOM;
        }
        return target != null && InstallPlan.besideOf(flow, target) != null ? Option.Kind.ALONGSIDE
                : Option.Kind.CUSTOM;
    }

    /** The largest free space on that disk, where the custom layout puts the system, or null when it has none. */
    @Nullable
    public static DiskSpot largestFree(final InstallerFlow.Disk disk) {
        DiskSpot largest = null;
        for (final DiskSpot spot : disk.spots()) {
            if (spot.isFree() && (largest == null || spot.sizeMb() > largest.sizeMb())) {
                largest = spot;
            }
        }
        return largest;
    }

    /**
     * One partition or free space the disk step lists.
     *
     * @param disk      the disk it is on
     * @param spot      the partition or free space
     * @param diskIndex the disk's place among the machine's disks, from 0, as the installers number them
     */
    public record Pick(InstallerFlow.Disk disk, DiskSpot spot, int diskIndex) {
    }

    /**
     * One file system Frames XP's text phase offers.
     *
     * @param fileSystem its serialized name, or {@link Placement#KEEP}
     * @param quick      whether it is the quick format
     */
    public record Format(String fileSystem, boolean quick) {
    }

    /**
     * One of Ubuntu's ways to install.
     *
     * @param kind which way
     * @param disk the disk it is about, or null when the machine has none
     */
    public record Option(Kind kind, @Nullable InstallerFlow.Disk disk) {

        /** The three ways. */
        public enum Kind {
            ALONGSIDE, ENTIRE, CUSTOM
        }
    }
}
