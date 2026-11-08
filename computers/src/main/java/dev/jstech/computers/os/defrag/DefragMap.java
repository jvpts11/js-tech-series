/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import java.util.Random;
import org.jetbrains.annotations.Nullable;

/**
 * The map of a disk's clusters that DEFRAG and Frames 95's Disk Defragmenter draw (computers/docs/design/storage.md):
 * as many cells used, free, corrupted and bad as the disk's shares say, scattered the same way every time for the same
 * disk, the part a pass has gone over already drawn as done, and the few cells it is reading at its head.
 *
 * <p>Pure: it says what each cell is, and each screen paints the cells in its own colours.
 */
public final class DefragMap {

    /** What a cell is. */
    public enum Cell { USED, FREE, CORRUPTED, BAD, DONE, READING, UNMOVABLE }

    /* The first cells a system keeps for itself, which no pass moves. */
    private static final int UNMOVABLE_CELLS = 4;
    /* How many cells the head of a pass reads at once. */
    private static final int HEAD_CELLS = 5;

    private DefragMap() {
    }

    /** The {@code count} cells of the map of {@code disk}, with {@code running}'s progress on it when it runs there. */
    public static Cell[] cells(final DefragState.Disk disk, @Nullable final DefragState.Run running, final int count) {
        final Cell[] cells = new Cell[count];
        final Random scatter = new Random(31L * disk.index() + disk.capacityMb());
        final double used = disk.used() / 1000.0;
        final double corrupted = used * disk.damage() / 1000.0;
        final double bad = disk.bad() / 1000.0;
        final int done = running == null || running.disk() != disk.index() ? -1
                : (int) ((long) count * running.percent() / 100L);
        for (int i = 0; i < count; i++) {
            final double roll = scatter.nextDouble();
            final Cell kind;
            if (i < UNMOVABLE_CELLS) {
                kind = Cell.UNMOVABLE;
            } else if (roll < bad) {
                kind = Cell.BAD;
            } else if (roll < bad + corrupted) {
                kind = Cell.CORRUPTED;
            } else if (roll < bad + used) {
                kind = Cell.USED;
            } else {
                kind = Cell.FREE;
            }
            if (kind == Cell.UNMOVABLE || kind == Cell.BAD || done < 0) {
                cells[i] = kind;
            } else if (i < done) {
                cells[i] = kind == Cell.FREE ? Cell.FREE : Cell.DONE;
            } else if (i < done + HEAD_CELLS && running != null && !running.paused()) {
                cells[i] = Cell.READING;
            } else {
                cells[i] = kind;
            }
        }
        return cells;
    }
}
