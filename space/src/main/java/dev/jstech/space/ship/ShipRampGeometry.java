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
 * Where the Ramp hold's ramp lies let down, as the approved Ramp hold draws it (space/docs/design/starships.md): from
 * its hinge at the deck's tail edge it falls straight out from the tail to the land, its toe unfolded beyond it, the
 * deck 2.35 over the land. Its walking face, in the hold's model's space (across from the middle line, up from the
 * hull's foot, along from the hold's front), is what a player climbs, a step every quarter block out along it, each a
 * step down from the one before; the blocks of the ramp hold them, each the steps within it. Pure, so it is tested
 * without the game.
 */
public final class ShipRampGeometry {

    /** The blocks the ramp stands in let down, each its three numbers in the hold's model's space. */
    public static final List<int[]> CELLS;
    /* Its hinge: the deck's height and the tail, along the ship; its half width; its length with its toe out. */
    private static final double DECK = 0.35;
    private static final double TAIL = 5.0;
    private static final double HALF = 0.97;
    private static final double LONG = 2.65 + 1.5;
    /* The land under the hull's foot. */
    private static final double LAND = -2.0;
    /* How far out along the ship each step reaches, and how thick it is under its top. */
    private static final double RUN = 0.25;
    private static final double THICK = 0.1;
    private static final int AXES = 3;
    private static final List<double[]> STEPS;
    /* The steps within each block of the ramp, in the block's own space, by its place. */
    private static final Map<List<Integer>, List<double[]>> BY_CELL = new LinkedHashMap<>();

    static {
        final double slope = Math.asin((DECK - LAND) / LONG);
        final double out = LONG * Math.cos(slope);
        final int count = (int) Math.ceil(out / RUN);
        final List<double[]> steps = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            final double z0 = TAIL + k * RUN;
            final double z1 = TAIL + Math.min(out, (k + 1) * RUN);
            // Its top where the ramp's face stands over the step's middle.
            final double top = DECK - ((z0 + z1) / 2 - TAIL) * Math.tan(slope);
            steps.add(new double[] {-HALF, top - THICK, z0, HALF, top, z1});
        }
        STEPS = List.copyOf(steps);
        for (final double[] step : STEPS) {
            for (int x = (int) Math.floor(step[0]); x <= (int) Math.floor(step[AXES] - 1.0e-9); x++) {
                for (int y = (int) Math.floor(step[1]); y <= (int) Math.floor(step[AXES + 1] - 1.0e-9); y++) {
                    for (int z = (int) Math.floor(step[2]); z <= (int) Math.floor(step[AXES + 2] - 1.0e-9); z++) {
                        final double[] within = {Math.max(step[0], x) - x, Math.max(step[1], y) - y,
                            Math.max(step[2], z) - z, Math.min(step[AXES], x + 1) - x,
                            Math.min(step[AXES + 1], y + 1) - y, Math.min(step[AXES + 2], z + 1) - z};
                        if (within[AXES] > within[0] && within[AXES + 1] > within[1] && within[AXES + 2] > within[2]) {
                            BY_CELL.computeIfAbsent(List.of(x, y, z), cell -> new ArrayList<>()).add(within);
                        }
                    }
                }
            }
        }
        final List<int[]> cells = new ArrayList<>();
        BY_CELL.keySet().forEach(cell -> cells.add(new int[] {cell.get(0), cell.get(1), cell.get(2)}));
        CELLS = List.copyOf(cells);
    }

    private ShipRampGeometry() {
    }

    /** Every step, its lowest corner then its highest, six numbers, in the hold's model's space, from the tail out. */
    public static List<double[]> steps() {
        return STEPS.stream().map(double[]::clone).toList();
    }

    /**
     * The steps within the block {@code (x, y, z)} of the hold's model, each its lowest corner then its highest in
     * the block's own space, from 0 to 1; none for a block the ramp does not stand in.
     */
    public static List<double[]> treads(final int x, final int y, final int z) {
        return BY_CELL.getOrDefault(List.of(x, y, z), List.of()).stream().map(double[]::clone).toList();
    }
}
