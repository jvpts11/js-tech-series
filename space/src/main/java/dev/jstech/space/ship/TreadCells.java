/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The treads of a way down shared among the blocks they stand in: each tread, a box in a model's space, cut at the
 * blocks' faces, each block holding the parts within it in its own space, from 0 to 1, for the block of the way to
 * stand as ({@link ShipStairBlock}). Pure, so it is tested without the game.
 */
public final class TreadCells {

    /* The parts of the treads within each block, in the block's own space, by its place. */
    private final Map<List<Integer>, List<double[]>> byCell;
    /* The blocks the treads stand in, by their places, in the order first met. */
    private final List<int[]> cells;

    private static final int AXES = 3;
    /* How far short of a block's face a tread ending on it is taken to end, so it takes nothing of the next block. */
    private static final double SHORT = 1.0e-9;

    private TreadCells(final Map<List<Integer>, List<double[]>> byCell) {
        this.byCell = byCell;
        final List<int[]> found = new ArrayList<>();
        byCell.keySet().forEach(cell -> found.add(new int[] {cell.get(0), cell.get(1), cell.get(2)}));
        this.cells = List.copyOf(found);
    }

    /** The treads {@code boxes}, each its lowest corner then its highest, six numbers, shared among their blocks. */
    public static TreadCells of(final List<double[]> boxes) {
        final Map<List<Integer>, List<double[]>> byCell = new LinkedHashMap<>();
        for (final double[] box : boxes) {
            for (int x = (int) Math.floor(box[0]); x <= (int) Math.floor(box[AXES] - SHORT); x++) {
                for (int y = (int) Math.floor(box[1]); y <= (int) Math.floor(box[AXES + 1] - SHORT); y++) {
                    for (int z = (int) Math.floor(box[2]); z <= (int) Math.floor(box[AXES + 2] - SHORT); z++) {
                        final double[] within = {Math.max(box[0], x) - x, Math.max(box[1], y) - y,
                            Math.max(box[2], z) - z, Math.min(box[AXES], x + 1) - x,
                            Math.min(box[AXES + 1], y + 1) - y, Math.min(box[AXES + 2], z + 1) - z};
                        if (within[AXES] > within[0] && within[AXES + 1] > within[1] && within[AXES + 2] > within[2]) {
                            byCell.computeIfAbsent(List.of(x, y, z), cell -> new ArrayList<>()).add(within);
                        }
                    }
                }
            }
        }
        return new TreadCells(byCell);
    }

    /** The blocks the treads stand in, each its three numbers in the model's space. */
    public List<int[]> cells() {
        return this.cells.stream().map(int[]::clone).toList();
    }

    /**
     * The parts of the treads within the block {@code (x, y, z)}, each its lowest corner then its highest in the
     * block's own space, from 0 to 1; none for a block no tread stands in.
     */
    public List<double[]> within(final int x, final int y, final int z) {
        return this.byCell.getOrDefault(List.of(x, y, z), List.of()).stream().map(double[]::clone).toList();
    }
}
