/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.disk.DiskLayout;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.disk.PartitionRole;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Where an installer puts the system on its disk, and what it makes there (computers/docs/design/storage-devices.md):
 * the table the disk is left with, the partition the system goes on, and the partitions made or formatted on the way.
 * Each installer formats with its own file system; under UEFI it makes a small EFI system partition when the disk has
 * none; on Linux, UNIX and the BSDs it makes a swap partition as big as the memory when there is room for one; on the
 * Master Boot Record the system's partition becomes the one that starts.
 *
 * <p>What the player chose is followed: the whole disk erased, a partition or a free space picked, the system beside
 * another whose partition is shrunk. Left to itself, an installer takes a disk that holds nothing whole, puts a system
 * already there back where it is, then uses the largest free space, then (Ubuntu and Linux Mint) shrinks the system
 * beside it, and last installs beside the files of a partition it can live on.
 *
 * <p>Pure: the machine writes what this answers.
 */
@TextHolder
public final class InstallPlan {

    /** The EFI system partition the installers make under UEFI, in megabytes. */
    public static final long EFI_MB = 512L;

    private static final TextKey NO_DISK = TextKey.of("jsc.install.plan.no_disk", "No disk is chosen.");
    private static final TextKey NO_ROOM = TextKey.of("jsc.install.plan.no_room", "There is not room for %s here.");
    private static final TextKey TABLE_FULL = TextKey.of("jsc.install.plan.table_full",
            "The disk is MBR and already has its four partitions.");
    private static final TextKey TOO_BIG = TextKey.of("jsc.install.plan.too_big",
            "That file system cannot be made on a partition this big.");
    private static final TextKey NEEDS_FORMAT = TextKey.of("jsc.install.plan.needs_format",
            "%s cannot be installed on this partition as it is: format it first.");
    private static final TextKey NOT_FOR_A_SYSTEM = TextKey.of("jsc.install.plan.not_for_a_system",
            "No system is installed on this partition.");
    private static final TextKey CANNOT_SHRINK = TextKey.of("jsc.install.plan.cannot_shrink",
            "That partition cannot be shrunk to this size.");

    private InstallPlan() {
    }

    /** Where the installer puts the system as things stand, on the disk it has chosen. */
    public static Plan of(final InstallerFlow flow) {
        final InstallerFlow.Disk disk = flow.target();
        return disk == null ? Plan.refused(InstallerFlow.NO_DISK, Problem.NO_DISK) : on(flow, disk);
    }

    /**
     * Where the installer would put the system on {@code disk}: the player's choices when it is the chosen one, and
     * the installer's own otherwise.
     */
    public static Plan on(final InstallerFlow flow, final InstallerFlow.Disk disk) {
        if (flow.eraseSlot() == disk.slot()) {
            return whole(flow, disk);
        }
        final Placement placement = flow.placement();
        if (disk.slot() != flow.targetSlot()) {
            return chosenForYou(flow, disk);
        }
        if (placement.shrinkToMb() > 0L) {
            return beside(flow, disk, placement.shrinkToMb());
        }
        if (placement.startMb() != Placement.ANYWHERE) {
            final DiskSpot spot = disk.spotAt(placement.startMb());
            if (spot == null) {
                return Plan.refused(disk.slot(), Problem.NO_ROOM);
            }
            return spot.isFree() ? inStretch(flow, disk, disk.layout(), spot.startMb(), spot.sizeMb())
                    : onPartition(flow, disk, spot, placement.fileSystem());
        }
        return chosenForYou(flow, disk);
    }

    /** Where the installer would put the system on {@code disk} erased whole, as a page asks before it erases. */
    public static Plan erasing(final InstallerFlow flow, final InstallerFlow.Disk disk) {
        return whole(flow, disk);
    }

    /**
     * The second disk of a ZFS mirror, erased and laid out as the first: none when no mirror is asked for, or when the
     * system is not going on ZFS.
     */
    @Nullable
    public static Plan mirror(final InstallerFlow flow) {
        final InstallerFlow.Disk second = flow.diskAt(flow.placement().mirrorSlot());
        if (second == null || second.slot() == flow.targetSlot() || formatWith(flow) != FileSystemType.ZFS) {
            return null;
        }
        return whole(flow, second);
    }

    /**
     * The partition holding another system that an installer going beside it shrinks: the one with the most free room,
     * among those whose file system shrinks; null when there is none.
     */
    @Nullable
    public static DiskSpot besideOf(final InstallerFlow flow, final InstallerFlow.Disk disk) {
        DiskSpot best = null;
        for (final DiskSpot spot : disk.spots()) {
            final FileSystemType fs = spot.fileSystem();
            if (spot.holdsData() && spot.hasSystem() && !spot.holds().equals(flow.systemName()) && fs != null
                    && fs.resizes(false, false) && (best == null || spot.freeMb() > best.freeMb())) {
                best = spot;
            }
        }
        return best;
    }

    /**
     * How big the installer leaves the partition of the system beside it when the player has not said: it keeps what
     * it holds and half of what is free, and the new system gets the rest, at least all it needs. Zero when it cannot.
     */
    public static long shrinkFor(final InstallerFlow flow, final InstallerFlow.Disk disk) {
        final DiskSpot other = besideOf(flow, disk);
        if (other == null) {
            return 0L;
        }
        final long need = neededIn(flow, disk.layout());
        final long give = Math.max(need, other.freeMb() / 2L);
        return give > other.freeMb() ? 0L : other.sizeMb() - give;
    }

    /** What the system and what comes with it need in free space on that table: the EFI partition and the swap too. */
    public static long neededIn(final InstallerFlow flow, final DiskLayout layout) {
        final long efi = makesEfi(flow, layout) ? EFI_MB : 0L;
        final long swap = hasSwap(layout) ? 0L : flow.facts().swapMb();
        return flow.footprintMb() + efi + swap;
    }

    /** The file system the system is formatted with: the one the player chose, or the installer's own. */
    public static FileSystemType formatWith(final InstallerFlow flow) {
        final String chosen = flow.placement().fileSystem();
        return chosen.isEmpty() || Placement.KEEP.equals(chosen) ? flow.facts().fileSystem()
                : FileSystemType.byName(chosen);
    }

    /** Whether the table has an EFI system partition. */
    public static boolean hasEfi(final DiskLayout layout) {
        for (final PartitionEntry partition : layout.partitions()) {
            if (partition.role() == PartitionRole.EFI) {
                return true;
            }
        }
        return false;
    }

    /** What a refusal says, in the player's language. */
    public static Text reason(final Problem problem, final String systemName) {
        return switch (problem) {
            case NO_DISK -> NO_DISK.text();
            case NO_ROOM -> NO_ROOM.with(systemName);
            case TABLE_FULL -> TABLE_FULL.text();
            case TOO_BIG -> TOO_BIG.text();
            case NEEDS_FORMAT -> NEEDS_FORMAT.with(systemName);
            case NOT_FOR_A_SYSTEM -> NOT_FOR_A_SYSTEM.text();
            case CANNOT_SHRINK -> CANNOT_SHRINK.text();
        };
    }

    /* The whole disk, erased, under a new table of the machine's own scheme. */
    private static Plan whole(final InstallerFlow flow, final InstallerFlow.Disk disk) {
        final DiskLayout fresh = DiskLayout.empty(flow.facts().scheme(), disk.sizeMb());
        final Plan plan = inStretch(flow, disk, fresh, 0L, fresh.reachMb());
        return plan.ok() ? plan.erasingTheDisk() : plan;
    }

    /*
     * The installer's own choice on a disk the player left to it: a disk that holds nothing taken whole, then the
     * system already there put back, then the largest free space, then the system beside shrunk (by the installers that
     * do), and last the files of a partition the system lives on.
     */
    private static Plan chosenForYou(final InstallerFlow flow, final InstallerFlow.Disk disk) {
        if (disk.holdsNothing()) {
            return whole(flow, disk);
        }
        for (final DiskSpot spot : disk.spots()) {
            if (!spot.isFree() && spot.holds().equals(flow.systemName())) {
                return onPartition(flow, disk, spot, Placement.KEEP);
            }
        }
        DiskSpot largest = null;
        for (final DiskSpot spot : disk.spots()) {
            if (spot.isFree() && (largest == null || spot.sizeMb() > largest.sizeMb())) {
                largest = spot;
            }
        }
        if (largest != null) {
            final Plan plan = inStretch(flow, disk, disk.layout(), largest.startMb(), largest.sizeMb());
            if (plan.ok()) {
                return plan;
            }
        }
        if (flow.style().shrinksBeside()) {
            final long to = shrinkFor(flow, disk);
            if (to > 0L) {
                final Plan plan = beside(flow, disk, to);
                if (plan.ok()) {
                    return plan;
                }
            }
        }
        for (final DiskSpot spot : disk.spots()) {
            if (spot.holdsData() && !spot.hasSystem() && carries(flow, spot) && spot.freeMb() >= flow.footprintMb()) {
                return onPartition(flow, disk, spot, Placement.KEEP);
            }
        }
        return Plan.refused(disk.slot(), Problem.NO_ROOM);
    }

    /* Beside the system on the disk, its partition shrunk to {@code toMb} and the room it frees used. */
    private static Plan beside(final InstallerFlow flow, final InstallerFlow.Disk disk, final long toMb) {
        final DiskSpot other = besideOf(flow, disk);
        if (other == null) {
            return Plan.refused(disk.slot(), Problem.NO_ROOM);
        }
        final DiskLayout.Change shrunk = disk.layout().resize(other.number(), toMb, false, other.usedMb());
        if (!shrunk.ok()) {
            return Plan.refused(disk.slot(), problemOf(shrunk.refusal()));
        }
        final long start = other.startMb() + toMb;
        for (final DiskLayout.FreeSpace gap : shrunk.layout().free()) {
            if (gap.startMb() == start) {
                return inStretch(flow, disk, shrunk.layout(), start, gap.sizeMb()).shrinking(other.number());
            }
        }
        return Plan.refused(disk.slot(), Problem.NO_ROOM);
    }

    /*
     * In a free stretch: the EFI partition first under UEFI when the disk has none, then the system, then its swap
     * after it when there is room for both and the table has room for both.
     */
    private static Plan inStretch(final InstallerFlow flow, final InstallerFlow.Disk disk, final DiskLayout layout,
                                  final long startMb, final long sizeMb) {
        final long footprint = flow.footprintMb();
        final List<Integer> made = new ArrayList<>();
        DiskLayout next = layout;
        long at = startMb;
        long room = sizeMb;
        if (makesEfi(flow, next)) {
            if (room < EFI_MB + footprint) {
                return Plan.refused(disk.slot(), Problem.NO_ROOM);
            }
            final DiskLayout.Change efi = next.create(at, EFI_MB, FileSystemType.FAT32, PartitionRole.EFI);
            if (!efi.ok()) {
                return Plan.refused(disk.slot(), problemOf(efi.refusal()));
            }
            next = efi.layout();
            made.add(efi.number());
            at += EFI_MB;
            room -= EFI_MB;
        }
        final FileSystemType fs = formatWith(flow);
        final long swapMb = swapIn(flow, next, room);
        final long systemMb = Math.min(room - swapMb, fs.mostPartitionMb());
        if (systemMb < footprint || systemMb <= 0L) {
            return Plan.refused(disk.slot(), Problem.NO_ROOM);
        }
        final DiskLayout.Change system = next.create(at, systemMb, fs, PartitionRole.DATA);
        if (!system.ok()) {
            return Plan.refused(disk.slot(), problemOf(system.refusal()));
        }
        next = system.layout();
        made.add(system.number());
        if (swapMb > 0L) {
            final DiskLayout.Change swap = next.create(at + systemMb, swapMb, FileSystemType.SWAP, PartitionRole.DATA);
            if (swap.ok()) {
                next = swap.layout();
                made.add(swap.number());
            }
        }
        return Plan.made(disk.slot(), startsFrom(next, system.number()), system.number(), made);
    }

    /*
     * On a partition already there: kept as it is, files and all, when the system lives on its file system and was
     * not told to format it, or formatted when it was told to or the partition holds nothing. A partition holding
     * files or another system in a file system the system does not live on is formatted only when the player says so.
     */
    private static Plan onPartition(final InstallerFlow flow, final InstallerFlow.Disk disk, final DiskSpot spot,
                                    final String fileSystem) {
        if (!spot.holdsData()) {
            return Plan.refused(disk.slot(), Problem.NOT_FOR_A_SYSTEM);
        }
        final boolean again = spot.holds().equals(flow.systemName());
        final boolean keep = Placement.KEEP.equals(fileSystem)
                || fileSystem.isEmpty() && carries(flow, spot) && (again || !spot.hasSystem());
        final List<Integer> made = new ArrayList<>();
        DiskLayout next = disk.layout();
        if (keep) {
            if (!carries(flow, spot)) {
                return Plan.refused(disk.slot(), Problem.NEEDS_FORMAT);
            }
            if (!again && spot.freeMb() < flow.footprintMb()) {
                return Plan.refused(disk.slot(), Problem.NO_ROOM);
            }
        } else {
            if (fileSystem.isEmpty() && (spot.hasSystem() || spot.usedMb() > 0L)) {
                return Plan.refused(disk.slot(), Problem.NEEDS_FORMAT);
            }
            final DiskLayout.Change formatted = next.format(spot.number(), formatWith(flow), PartitionRole.DATA);
            if (!formatted.ok()) {
                return Plan.refused(disk.slot(), problemOf(formatted.refusal()));
            }
            if (spot.sizeMb() < flow.footprintMb()) {
                return Plan.refused(disk.slot(), Problem.NO_ROOM);
            }
            next = formatted.layout();
            made.add(spot.number());
        }
        final long swapMb = flow.facts().swapMb();
        if (swapMb > 0L && !hasSwap(next) && next.partitions().size() < next.scheme().mostPartitions()) {
            for (final DiskLayout.FreeSpace gap : next.free()) {
                if (gap.sizeMb() >= swapMb) {
                    final DiskLayout.Change swap = next.create(gap.startMb(), swapMb, FileSystemType.SWAP,
                            PartitionRole.DATA);
                    if (swap.ok()) {
                        next = swap.layout();
                        made.add(swap.number());
                    }
                    break;
                }
            }
        }
        return Plan.made(disk.slot(), startsFrom(next, spot.number()), spot.number(), made);
    }

    /*
     * The swap made beside a system in {@code room}: as big as the memory, when the system still fits and so does it.
     */
    private static long swapIn(final InstallerFlow flow, final DiskLayout layout, final long room) {
        final long swapMb = flow.facts().swapMb();
        final boolean tableRoom = layout.partitions().size() + 2 <= layout.scheme().mostPartitions();
        return swapMb > 0L && !hasSwap(layout) && tableRoom && room - swapMb >= flow.footprintMb() ? swapMb : 0L;
    }

    /* Whether an EFI system partition is made on that table: a machine that starts by UEFI, a GPT disk without one. */
    private static boolean makesEfi(final InstallerFlow flow, final DiskLayout layout) {
        return flow.facts().scheme() == PartitionScheme.GPT && layout.scheme() == PartitionScheme.GPT
                && !hasEfi(layout);
    }

    /* On the Master Boot Record, the system's partition becomes the one that starts. */
    private static DiskLayout startsFrom(final DiskLayout layout, final int number) {
        if (layout.scheme() != PartitionScheme.MBR) {
            return layout;
        }
        final DiskLayout.Change active = layout.activate(number);
        return active.ok() ? active.layout() : layout;
    }

    private static boolean carries(final InstallerFlow flow, final DiskSpot spot) {
        return spot.fileSystem() != null && flow.facts().carriers().contains(spot.fileSystem());
    }

    private static boolean hasSwap(final DiskLayout layout) {
        for (final PartitionEntry partition : layout.partitions()) {
            if (partition.swap()) {
                return true;
            }
        }
        return false;
    }

    private static Problem problemOf(@Nullable final DiskLayout.Refusal refusal) {
        if (refusal == null) {
            return Problem.NO_ROOM;
        }
        return switch (refusal) {
            case TABLE_FULL -> Problem.TABLE_FULL;
            case TOO_BIG_FOR_ITS_FILE_SYSTEM -> Problem.TOO_BIG;
            case BELOW_WHAT_IT_HOLDS, GROWS_ONLY_UNMOUNTED, SHRINKS_ONLY_UNMOUNTED, NEVER_SHRINKS ->
                    Problem.CANNOT_SHRINK;
            default -> Problem.NO_ROOM;
        };
    }

    /**
     * What the installer knows of the machine beyond its pages.
     *
     * @param fileSystem the file system its installer formats with
     * @param carriers   the file systems the system installs onto as they stand, keeping what they hold
     * @param swapMb     the swap partition it makes, as big as the memory; 0 for a system that keeps none
     * @param scheme     the table it writes on a disk it takes whole: MBR under a BIOS, GPT under UEFI
     */
    public record Facts(FileSystemType fileSystem, List<FileSystemType> carriers, int swapMb, PartitionScheme scheme) {

        /** What a plain installer knows: FAT, no swap, the Master Boot Record. */
        public static final Facts PLAIN = new Facts(FileSystemType.FAT32,
                List.of(FileSystemType.FAT16, FileSystemType.FAT32), 0, PartitionScheme.MBR);

        public Facts {
            carriers = List.copyOf(carriers);
            swapMb = Math.max(0, swapMb);
        }
    }

    /** Why the installer cannot put the system where it was asked. */
    public enum Problem {
        NO_DISK, NO_ROOM, TABLE_FULL, TOO_BIG, NEEDS_FORMAT, NOT_FOR_A_SYSTEM, CANNOT_SHRINK
    }

    /**
     * Where the system goes.
     *
     * @param slot            the disk it goes on
     * @param layout          the table the disk is left with, or null when refused
     * @param systemPartition the partition the system goes on
     * @param made            the partitions made or formatted for it, emptied when written
     * @param wholeDisk       whether the whole disk is erased first
     * @param shrunk          the partition of the system beside it that is shrunk, or 0
     * @param problem         why it cannot go there, or null
     */
    public record Plan(int slot, @Nullable DiskLayout layout, int systemPartition, List<Integer> made,
                       boolean wholeDisk, int shrunk, @Nullable Problem problem) {

        public Plan {
            made = List.copyOf(made);
        }

        /** Whether the system can go there. */
        public boolean ok() {
            return this.problem == null;
        }

        /** The system's partition as the table leaves it, or null when refused. */
        @Nullable
        public PartitionEntry system() {
            return this.layout == null ? null : this.layout.find(this.systemPartition);
        }

        /** The swap partition made for it, or null when none is. */
        @Nullable
        public PartitionEntry swap() {
            if (this.layout == null) {
                return null;
            }
            for (final int number : this.made) {
                final PartitionEntry partition = this.layout.find(number);
                if (partition != null && partition.swap()) {
                    return partition;
                }
            }
            return null;
        }

        static Plan made(final int slot, final DiskLayout layout, final int systemPartition, final List<Integer> made) {
            return new Plan(slot, layout, systemPartition, made, false, 0, null);
        }

        static Plan refused(final int slot, final Problem problem) {
            return new Plan(slot, null, 0, List.of(), false, 0, problem);
        }

        Plan erasingTheDisk() {
            return new Plan(this.slot, this.layout, this.systemPartition, this.made, true, this.shrunk, this.problem);
        }

        Plan shrinking(final int number) {
            return new Plan(this.slot, this.layout, this.systemPartition, this.made, this.wholeDisk, number,
                    this.problem);
        }
    }
}
