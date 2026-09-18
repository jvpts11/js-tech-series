/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.TtyScript;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the disk tools say, and when: the filesystem makers, and the two that list what the machine has.
 *
 * <p>Making a filesystem is not instant and the real tool does not pretend it is. It says what it is about to
 * make at once, then counts its way through the block groups twice, waits on the journal, and counts through
 * them a third time, each count running on the line it started on. How long all that takes is the disk's
 * business, so it is told how long it has and shares the time out the way the real one spends it.
 */
public final class DiskVoices {

    private static final String MKE2FS_VERSION = "mke2fs 1.47.1 (20-May-2024)";
    private static final String MKFS_FAT_VERSION = "mkfs.fat 4.2 (2021-01-31)";

    /** How many of the spare superblocks fit on one line of the list, which is where the real tool breaks it. */
    private static final int BACKUPS_PER_LINE = 8;

    private DiskVoices() {
    }

    /**
     * Making an ext4 filesystem.
     *
     * @param sizeMb how big the device is, which every figure printed is worked out from
     * @param seed   what the filesystem's identifier is worked out from
     * @param ticks  how long this disk takes over it
     * @param made   what making it means to the machine, done when the tool has finished and not before
     */
    public static TtyScript mke2fs(final String device, final long sizeMb, final long seed, final int ticks,
                                   final Runnable made) {
        final Ext4Figures figures = Ext4Figures.of(sizeMb);
        final int groups = (int) Math.min(Integer.MAX_VALUE, figures.groups());
        final TtyScript.Builder script = TtyScript.script()
                .say(MKE2FS_VERSION)
                .pause(share(ticks, 6))
                .say("Creating filesystem with " + figures.blocks() + " 4k blocks and " + figures.inodes() + " inodes")
                .say("Filesystem UUID: " + Ext4Figures.uuid(device, seed));
        if (!figures.backups().isEmpty()) {
            script.say("Superblock backups stored on blocks: ");
            script.sayAll(backupLines(figures.backups()));
        }
        script.say("")
                .redraw(share(ticks, 10), p -> Bars.counting("Allocating group tables: ", p, groups, "done"))
                .redraw(share(ticks, 40), p -> Bars.counting("Writing inode tables: ", p, groups, "done"));
        if (figures.journal() > 0) {
            /* The journal is the one step with no count to watch: the line stands there, and then it is done. */
            final String label = "Creating journal (" + figures.journal() + " blocks): ";
            script.redraw(share(ticks, 22), p -> CliLine.plain(p >= 1.0 ? label + "done" : label));
        }
        return script
                .redraw(share(ticks, 22), p -> Bars.counting(
                        "Writing superblocks and filesystem accounting information: ", p, groups, "done"))
                .say("")
                .effect(made)
                .done();
    }

    /** Making a FAT filesystem, which says its name and version, takes a moment, and says nothing else. */
    public static TtyScript mkfsFat(final int ticks, final Runnable made) {
        return TtyScript.script().pause(ticks).say(MKFS_FAT_VERSION).effect(made).done();
    }

    /** The header of a listing of block devices: the seven columns, in the real tool's order. */
    public static CliLine lsblkHeader() {
        return CliLine.plain(String.format(Locale.ROOT, "%-9s %-7s %2s %6s %2s %-4s %s",
                "NAME", "MAJ:MIN", "RM", "SIZE", "RO", "TYPE", "MOUNTPOINTS"));
    }

    /**
     * One device in that listing.
     *
     * @param name      what it is called, already carrying the branch drawing if it hangs off a disk
     * @param removable whether it can be taken out, which a medium can and a disk cannot
     * @param readOnly  whether it can only be read, which is what a live medium's image is
     */
    public static CliLine lsblkRow(final String name, final int major, final int minor, final boolean removable,
                                   final long sizeMb, final boolean readOnly, final String type,
                                   final String mountPoint) {
        return CliLine.plain(String.format(Locale.ROOT, "%-9s %-7s %2d %6s %2d %-4s %s",
                name, major + ":" + minor, removable ? 1 : 0, size(sizeMb), readOnly ? 1 : 0, type, mountPoint)
                .stripTrailing());
    }

    /** One filesystem as the identifier lister names it. */
    public static CliLine blkid(final String device, final String uuid, final boolean fat, final String partUuid) {
        return CliLine.plain("/dev/" + device + ": UUID=\"" + uuid + "\" BLOCK_SIZE=\"" + (fat ? "512" : "4096")
                + "\" TYPE=\"" + (fat ? "vfat" : "ext4") + "\" PARTUUID=\"" + partUuid + "\"");
    }

    /** A size the way a device listing writes one: the largest unit that fits, a decimal only when it says something. */
    public static String size(final long sizeMb) {
        if (sizeMb >= 1024L * 1024) {
            return trimmed(sizeMb / (1024.0 * 1024.0)) + "T";
        }
        if (sizeMb >= 1024) {
            return trimmed(sizeMb / 1024.0) + "G";
        }
        return sizeMb + "M";
    }

    /** A share of the time a tool has, in hundredths, and never none of it. */
    private static int share(final int ticks, final int hundredths) {
        return Math.max(1, ticks * hundredths / 100);
    }

    /** The spare superblocks, a tab in and so many to a line, a comma after every one but the last. */
    private static List<CliLine> backupLines(final List<Long> backups) {
        final List<CliLine> out = new ArrayList<>();
        final StringBuilder row = new StringBuilder("\t");
        for (int i = 0; i < backups.size(); i++) {
            row.append(backups.get(i));
            final boolean last = i == backups.size() - 1;
            if (!last) {
                row.append(", ");
            }
            if (last || (i + 1) % BACKUPS_PER_LINE == 0) {
                out.add(CliLine.plain(row.toString().stripTrailing()));
                row.setLength(0);
                row.append('\t');
            }
        }
        return out;
    }

    private static String trimmed(final double value) {
        final double rounded = Math.round(value * 10.0) / 10.0;
        return rounded == Math.floor(rounded)
                ? String.valueOf((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
    }
}
