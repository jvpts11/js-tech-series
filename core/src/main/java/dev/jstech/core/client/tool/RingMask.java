/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import dev.jstech.core.gui.layout.ToolHudLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ring of modes as runs of pixels, row by row, worked out once for each number of modes: which segment each run
 * is in and whether it is the segment's edge, and the disc in the middle with its edge. Drawing it is a fill a run.
 */
final class RingMask {

    private final List<List<Run>> rows;
    private final List<List<Run>> disc;

    /* The disc in the middle is this much smaller than the ring's inner circle. */
    private static final int DISC_INSET = 3;
    private static final Map<Integer, RingMask> MADE = new ConcurrentHashMap<>();

    private RingMask(final int count) {
        this.rows = new ArrayList<>();
        this.disc = new ArrayList<>();
        final int radius = ToolHudLayout.INNER - DISC_INSET;
        for (int dy = -ToolHudLayout.OUTER; dy <= ToolHudLayout.OUTER; dy++) {
            final List<Run> row = new ArrayList<>();
            final List<Run> discRow = new ArrayList<>();
            for (int dx = -ToolHudLayout.OUTER; dx <= ToolHudLayout.OUTER; dx++) {
                final int segment = ToolHudLayout.segmentAt(dx, dy, count);
                if (segment >= 0) {
                    add(row, dx, segment, ToolHudLayout.onEdge(dx, dy, count));
                }
                if (inDisc(dx, dy, radius)) {
                    add(discRow, dx, 0, !inDisc(dx + 1, dy, radius) || !inDisc(dx - 1, dy, radius)
                            || !inDisc(dx, dy + 1, radius) || !inDisc(dx, dy - 1, radius));
                }
            }
            this.rows.add(row);
            this.disc.add(discRow);
        }
    }

    /** The mask of a ring of {@code count} modes. */
    static RingMask of(final int count) {
        return MADE.computeIfAbsent(count, RingMask::new);
    }

    /** The runs of the ring, row by row from {@code -OUTER} to {@code OUTER} below the centre. */
    List<List<Run>> rows() {
        return this.rows;
    }

    /** The runs of the disc in the middle, row by row as the ring's. */
    List<List<Run>> disc() {
        return this.disc;
    }

    private static boolean inDisc(final int dx, final int dy, final int radius) {
        return dx * dx + dy * dy <= radius * radius;
    }

    /* Grows the row's last run by the pixel at {@code dx}, or starts another when it differs. */
    private static void add(final List<Run> row, final int dx, final int segment, final boolean edge) {
        if (!row.isEmpty()) {
            final Run last = row.get(row.size() - 1);
            if (last.to() == dx && last.segment() == segment && last.edge() == edge) {
                row.set(row.size() - 1, new Run(last.from(), dx + 1, segment, edge));
                return;
            }
        }
        row.add(new Run(dx, dx + 1, segment, edge));
    }

    /**
     * Pixels side by side in one row, from {@code from} up to {@code to}, left of the centre below nothing.
     *
     * @param from    the first, from the centre
     * @param to      the one after the last
     * @param segment the segment they are in
     * @param edge    whether they are its edge
     */
    record Run(int from, int to, int segment, boolean edge) {
    }
}
