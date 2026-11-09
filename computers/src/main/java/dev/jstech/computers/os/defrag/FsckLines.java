/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What a Unix's fsck prints as it repairs a corrupted disk, in each system's own words, which no fsck ever printed in
 * anything but its own (computers/docs/design/storage.md): e2fsck's five passes on Linux, the five phases of UFS's on
 * FreeBSD and of System V's, fsck_hfs's checks under Disk Utility's First Aid on Quince's systems, and at the end what
 * it found and gave back, and the bad sectors before and after.
 *
 * <p>Pure: no game types.
 */
public final class FsckLines {

    /* The blocks each counts in: ext's and UFS's fragments of 4 KiB, System V's of 512 bytes. */
    private static final long EXT_BLOCKS_A_MB = 256L;
    private static final long SYSTEM_V_BLOCKS_A_MB = 2_048L;
    private static final int UFS_FRAGS_A_BLOCK = 8;
    private static final int EXT_BLOCKS_AN_INODE = 4;
    /* The files a file system keeps for itself besides what the disk stores. */
    private static final int EXT_RESERVED_INODES = 11;
    private static final int UFS_SYSTEM_FILES = 3;
    private static final int SYSTEM_V_SYSTEM_FILES = 30;
    /* What Quince's systems call the disk they start from, and any other. */
    private static final String QUINCE_SYSTEM_VOLUME = "Quince HD";
    private static final String QUINCE_OTHER_VOLUME = "Untitled";

    private FsckLines() {
    }

    /** What the command prints as it starts on disk {@code index}. */
    public static List<String> opening(final DefragLook look, final int index) {
        return switch (look) {
            case LINUX -> List.of("fsck from util-linux 2.38.1", "e2fsck 1.47.0 (5-Feb-2023)");
            case FREEBSD -> List.of("** " + look.device(index));
            case QUINCE -> List.of("** /dev/r" + look.drive(index), "** Checking HFS Plus volume.");
            default -> List.of("", look.device(index), "File System: " + (index == 0 ? "root" : "usr") + " Volume: 1",
                    "");
        };
    }

    /** The five passes, or phases, printed one by one as the repair goes. */
    public static List<String> phases(final DefragLook look) {
        return switch (look) {
            case LINUX -> List.of("Pass 1: Checking inodes, blocks, and sizes", "Pass 2: Checking directory structure",
                    "Pass 3: Checking directory connectivity", "Pass 4: Checking reference counts",
                    "Pass 5: Checking group summary information");
            case FREEBSD -> List.of("** Phase 1 - Check Blocks and Sizes", "** Phase 2 - Check Pathnames",
                    "** Phase 3 - Check Connectivity", "** Phase 4 - Check Reference Counts",
                    "** Phase 5 - Check Cyl groups");
            case QUINCE -> List.of("** Checking Extents Overflow file.", "** Checking Catalog file.",
                    "** Checking Catalog hierarchy.", "** Checking volume bitmap.", "** Checking volume information.");
            default -> List.of("** Phase 1 - Check Blocks and Sizes", "** Phase 2 - Check Pathnames",
                    "** Phase 3 - Check Connectivity", "** Phase 4 - Check Reference Counts",
                    "** Phase 5 - Check Free List");
        };
    }

    /** How many of the phases a repair {@code percent} through has printed: the first at once, the rest as it goes. */
    public static int phasesBy(final int percent) {
        return Math.min(5, 1 + Math.max(0, percent) / 20);
    }

    /** What it prints once the repair of {@code disk} ended as {@code result}. */
    public static List<String> summary(final DefragLook look, @Nullable final DefragState.Disk disk,
                                       final DefragState.Result result) {
        final List<String> out = new ArrayList<>();
        final String drive = look.drive(result.disk());
        final boolean linux = look == DefragLook.LINUX;
        final boolean modified = result.found() > 0L || result.badBefore() != result.badAfter();
        if (result.found() > 0L) {
            out.add(linux ? drive + ": " + result.found() + " corrupted items: " + result.recovered() + " recovered, "
                    + result.lost() + " lost"
                    : result.found() + " CORRUPTED ITEMS: " + result.recovered() + " RECOVERED, " + result.lost()
                            + " LOST");
        }
        if (result.badBefore() > 0) {
            out.add(linux ? drive + ": bad sectors " + result.badBefore() + " -> " + result.badAfter()
                    : "BAD SECTORS " + result.badBefore() + " -> " + result.badAfter());
        }
        final long capacityMb = disk == null ? 0L : disk.capacityMb();
        final long freeMb = disk == null ? 0L : Math.min(capacityMb, disk.freeMb());
        final long usedMb = capacityMb - freeMb;
        final int types = disk == null ? 0 : disk.types();
        switch (look) {
            case QUINCE -> out.add("** The volume " + (result.disk() == 0 ? QUINCE_SYSTEM_VOLUME : QUINCE_OTHER_VOLUME)
                    + (modified ? " was repaired successfully." : " appears to be OK."));
            case LINUX -> {
                if (modified) {
                    out.add("");
                    out.add(drive + ": ***** FILE SYSTEM WAS MODIFIED *****");
                }
                final long blocks = capacityMb * EXT_BLOCKS_A_MB;
                final long files = types + EXT_RESERVED_INODES;
                out.add(drive + ": " + files + "/" + Math.max(files, blocks / EXT_BLOCKS_AN_INODE)
                        + " files (0.0% non-contiguous), " + usedMb * EXT_BLOCKS_A_MB + "/" + blocks + " blocks");
            }
            case FREEBSD -> {
                final long free = freeMb * EXT_BLOCKS_A_MB;
                out.add((types + UFS_SYSTEM_FILES) + " files, " + usedMb * EXT_BLOCKS_A_MB + " used, " + free
                        + " free (" + free % UFS_FRAGS_A_BLOCK + " frags, " + free / UFS_FRAGS_A_BLOCK
                        + " blocks, 0.0% fragmentation)");
                if (modified) {
                    out.add("");
                    out.add("***** FILE SYSTEM WAS MODIFIED *****");
                }
            }
            default -> {
                out.add((types + SYSTEM_V_SYSTEM_FILES) + " files " + usedMb * SYSTEM_V_BLOCKS_A_MB + " blocks "
                        + freeMb * SYSTEM_V_BLOCKS_A_MB + " free");
                if (modified) {
                    out.add("");
                    out.add("*** FILE SYSTEM WAS MODIFIED ***");
                }
            }
        }
        return out;
    }
}
