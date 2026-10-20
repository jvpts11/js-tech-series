/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Whether a structure that lost blocks is still all one, and if not, its pieces: from each block beside one lost, the
 * blocks joined to it face to face are found, all of them a step at a time by turns, and two that meet are one. The
 * search ends as soon as only one is still growing: that one is the structure as it goes on, and each other, all
 * found, is a piece to part from it. So the cost is that of the small pieces, never of the whole of a great ship that
 * lost a corner. Pure: the blocks are asked of {@link IFilled}, places as the game packs them.
 */
public final class SplitSearch {

    /* The six steps from a block to those beside it. */
    private static final int[][] STEPS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    /* The packing of places, as the game's own: 26 bits east, 26 south, 12 up. */
    private static final int BITS_XZ = 26;
    private static final int BITS_Y = 12;
    private static final int SHIFT_Z = BITS_Y;
    private static final int SHIFT_X = BITS_Y + BITS_XZ;
    private static final long MASK_XZ = (1L << BITS_XZ) - 1;
    private static final long MASK_Y = (1L << BITS_Y) - 1;

    private SplitSearch() {
    }

    /**
     * The pieces to part from a structure whose blocks {@code filled} says, the blocks beside those it lost being
     * {@code seeds}: none when all are still one. Each piece is every block of it, never the one that goes on.
     */
    public static List<Set<Long>> pieces(final Set<Long> seeds, final IFilled filled) {
        final List<Long> starts = new ArrayList<>();
        for (final long seed : seeds) {
            if (filled.filled(seed)) {
                starts.add(seed);
            }
        }
        if (starts.size() < 2) {
            return List.of();
        }
        final Map<Long, Integer> found = new HashMap<>();
        final int[] parent = new int[starts.size()];
        final List<ArrayDeque<Long>> fronts = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            parent[i] = i;
            fronts.add(new ArrayDeque<>());
        }
        for (int i = 0; i < starts.size(); i++) {
            final long start = starts.get(i);
            final Integer had = found.get(start);
            if (had != null) {
                union(parent, fronts, had, i);
                continue;
            }
            found.put(start, i);
            fronts.get(i).add(start);
        }
        int growing = growingRoots(parent, fronts).size();
        while (growing > 1) {
            for (final int root : growingRoots(parent, fronts)) {
                if (find(parent, root) != root) {
                    continue;
                }
                final ArrayDeque<Long> front = fronts.get(root);
                final Long at = front.poll();
                if (at == null) {
                    continue;
                }
                for (final int[] step : STEPS) {
                    final long next = offset(at, step[0], step[1], step[2]);
                    if (!filled.filled(next)) {
                        continue;
                    }
                    final Integer other = found.get(next);
                    if (other == null) {
                        found.put(next, root);
                        front.add(next);
                    } else if (find(parent, other) != find(parent, root)) {
                        union(parent, fronts, find(parent, other), find(parent, root));
                    }
                }
            }
            growing = growingRoots(parent, fronts).size();
        }
        // Every one all found is a piece; of those, when none is still growing, the largest goes on.
        final Map<Integer, Set<Long>> byRoot = new HashMap<>();
        for (final Map.Entry<Long, Integer> each : found.entrySet()) {
            byRoot.computeIfAbsent(find(parent, each.getValue()), any -> new LinkedHashSet<>()).add(each.getKey());
        }
        final Set<Integer> stillGrowing = new HashSet<>(growingRoots(parent, fronts));
        final List<Set<Long>> pieces = new ArrayList<>();
        Set<Long> largest = null;
        for (final Map.Entry<Integer, Set<Long>> each : byRoot.entrySet()) {
            if (stillGrowing.contains(each.getKey())) {
                continue;
            }
            pieces.add(each.getValue());
            if (largest == null || each.getValue().size() > largest.size()) {
                largest = each.getValue();
            }
        }
        if (stillGrowing.isEmpty() && largest != null) {
            pieces.remove(largest);
        }
        return pieces;
    }

    /** The packed place {@code (dx, dy, dz)} from {@code pos}, packed as the game packs a block's place. */
    public static long offset(final long pos, final int dx, final int dy, final int dz) {
        final int x = (int) (pos >> SHIFT_X);
        final int y = (int) (pos << (Long.SIZE - BITS_Y) >> (Long.SIZE - BITS_Y));
        final int z = (int) (pos << (Long.SIZE - SHIFT_Z - BITS_XZ) >> (Long.SIZE - BITS_XZ));
        return pack(x + dx, y + dy, z + dz);
    }

    /** The packed place of {@code (x, y, z)}, as the game packs a block's place. */
    public static long pack(final int x, final int y, final int z) {
        return ((x & MASK_XZ) << SHIFT_X) | ((z & MASK_XZ) << SHIFT_Z) | (y & MASK_Y);
    }

    /* The roots whose search still has blocks to look beside. */
    private static List<Integer> growingRoots(final int[] parent, final List<ArrayDeque<Long>> fronts) {
        final List<Integer> roots = new ArrayList<>();
        for (int i = 0; i < parent.length; i++) {
            if (find(parent, i) == i && !fronts.get(i).isEmpty()) {
                roots.add(i);
            }
        }
        return roots;
    }

    private static int find(final int[] parent, final int of) {
        int at = of;
        while (parent[at] != at) {
            parent[at] = parent[parent[at]];
            at = parent[at];
        }
        return at;
    }

    /* Joins two searches into one: the second's blocks still to look beside go to the first. */
    private static void union(final int[] parent, final List<ArrayDeque<Long>> fronts, final int first,
                              final int second) {
        final int a = find(parent, first);
        final int b = find(parent, second);
        if (a == b) {
            return;
        }
        parent[b] = a;
        fronts.get(a).addAll(fronts.get(b));
        fronts.get(b).clear();
    }

    /** Which blocks are a structure's, by their packed place. */
    public interface IFilled {

        /** Whether a block of the structure is at {@code pos}. */
        boolean filled(long pos);
    }
}
