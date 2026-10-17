/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Which chunks a Space Persistor's area covers, without the game: its shape round its own chunk, as wide as its radius,
 * the chunks clicked on its map added, those clicked away taken off. Its own chunk is always in it, since a Persistor
 * keeps the rest only while it is loaded itself. A chunk is an offset from the Persistor's own, packed into one number
 * as the game packs chunks: x in the low half, z in the high.
 */
public final class PersistorShapes {

    private PersistorShapes() {
    }

    /** The offset {@code dx, dz} as one number. */
    public static long pack(final int dx, final int dz) {
        return Integer.toUnsignedLong(dx) | (long) dz << Integer.SIZE;
    }

    /** The x of a packed offset. */
    public static int dx(final long packed) {
        return (int) packed;
    }

    /** The z of a packed offset. */
    public static int dz(final long packed) {
        return (int) (packed >> Integer.SIZE);
    }

    /**
     * The offsets the shape covers alone, before any clicks: a square; a circle, the chunks whose middles lie within
     * the radius, a little rounded out (so a radius of one is the plus round the middle, and two takes in the corners
     * beside it but not the far ones); a line across the Persistor's facing; or its own chunk.
     *
     * @param alongX whether a line runs along x; along z otherwise
     */
    public static Set<Long> base(final PersistorShape shape, final int radius, final boolean alongX) {
        final int r = Math.max(0, radius);
        final Set<Long> covered = new LinkedHashSet<>();
        covered.add(pack(0, 0));
        switch (shape) {
            case SQUARE -> {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        covered.add(pack(dx, dz));
                    }
                }
            }
            case CIRCLE -> {
                final double reach = r * (r + 0.5);
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        if (dx * dx + dz * dz <= reach) {
                            covered.add(pack(dx, dz));
                        }
                    }
                }
            }
            case LINE -> {
                for (int step = -r; step <= r; step++) {
                    covered.add(alongX ? pack(step, 0) : pack(0, step));
                }
            }
            case FREE -> {
                // Its own chunk alone: the rest is clicked.
            }
        }
        return covered;
    }

    /** The whole area: the shape's offsets and those added, less those taken off, and its own chunk always. */
    public static Set<Long> area(final PersistorShape shape, final int radius, final boolean alongX,
                                 final Set<Long> added, final Set<Long> removed) {
        final Set<Long> covered = base(shape, radius, alongX);
        covered.addAll(added);
        covered.removeAll(removed);
        covered.add(pack(0, 0));
        return covered;
    }

    /**
     * What clicking the offset {@code dx, dz} on the map makes of the clicks so far: a chunk in the area is taken off,
     * one outside it added; clicking it again undoes it. Its own chunk is never taken off.
     */
    public static Clicks click(final PersistorShape shape, final int radius, final boolean alongX,
                               final Set<Long> added, final Set<Long> removed, final int dx, final int dz) {
        final long at = pack(dx, dz);
        final Set<Long> nowAdded = new LinkedHashSet<>(added);
        final Set<Long> nowRemoved = new LinkedHashSet<>(removed);
        if (at == pack(0, 0)) {
            return new Clicks(nowAdded, nowRemoved);
        }
        final boolean inShape = base(shape, radius, alongX).contains(at);
        final boolean covered = area(shape, radius, alongX, added, removed).contains(at);
        if (covered) {
            nowAdded.remove(at);
            if (inShape) {
                nowRemoved.add(at);
            }
        } else {
            nowRemoved.remove(at);
            if (!inShape) {
                nowAdded.add(at);
            }
        }
        return new Clicks(nowAdded, nowRemoved);
    }

    /**
     * The clicks of an area after one more.
     *
     * @param added   the offsets added outside the shape
     * @param removed the offsets of the shape taken off
     */
    public record Clicks(Set<Long> added, Set<Long> removed) {

        public Clicks {
            added = Set.copyOf(added);
            removed = Set.copyOf(removed);
        }
    }
}
