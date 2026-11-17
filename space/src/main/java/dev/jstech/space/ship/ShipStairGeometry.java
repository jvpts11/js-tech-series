/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import java.util.ArrayList;
import java.util.List;

/**
 * Where an airlock's boarding stair stands let down, as the approved airlock draws it: from its hinge under the outer
 * door's sill, three flights a little over three quarters of a block each fall at 55 degrees out from the hull and
 * down to the land, each with two treads, the floor 2.35 over the land. Its treads, in the airlock's model's space
 * (across from the middle line, up from the hull's foot, along from the airlock's front), are what a player climbs; the
 * blocks of the stair hold them, each the treads within it. Pure, so it is tested without the game.
 */
public final class ShipStairGeometry {

    /** The blocks of the airlock's model the stair stands in, let down, by their places. */
    public static final List<int[]> CELLS;
    /* Where the stair's hinge stands, under the door's sill, and how far over the hull's foot. */
    private static final double HINGE_X = -2.05;
    private static final double HINGE_Y = 0.02;
    private static final double FLIGHT = 0.81;
    private static final double SLOPE = 55.0;
    private static final int FLIGHTS = 3;
    /* Each flight's treads, at those shares along it; a tread's half width, how far under and over its line. */
    private static final double[] TREADS = {0.3, 0.8};
    private static final double TREAD_HALF = 0.17;
    private static final double TREAD_UNDER = 0.03;
    private static final double TREAD_OVER = 0.02;
    /* Along the airlock, the outer door's sides, the treads a little within them. */
    private static final double DOOR_Z0 = 1.45;
    private static final double DOOR_Z1 = 2.55;
    private static final double WITHIN = 0.06;
    /* Each tread, its lowest corner then its highest, in the model's space. */
    private static final List<double[]> STEPS;
    /* The treads within each block of the stair, in the block's own space, by its place. */
    private static final TreadCells BY_CELL;

    static {
        final List<double[]> steps = new ArrayList<>();
        final double angle = Math.toRadians(-(180 - SLOPE));
        final double dx = FLIGHT * Math.cos(angle);
        final double dy = FLIGHT * Math.sin(angle);
        for (int k = 0; k < FLIGHTS; k++) {
            for (final double share : TREADS) {
                final double x = HINGE_X + dx * (k + share);
                final double y = HINGE_Y + dy * (k + share);
                steps.add(new double[] {x - TREAD_HALF, y - TREAD_UNDER, DOOR_Z0 + WITHIN, x + TREAD_HALF,
                    y + TREAD_OVER, DOOR_Z1 - WITHIN});
            }
        }
        STEPS = List.copyOf(steps);
        BY_CELL = TreadCells.of(STEPS);
        CELLS = BY_CELL.cells();
    }

    private ShipStairGeometry() {
    }

    /** Every tread, its lowest corner then its highest, six numbers, in the airlock's model's space. */
    public static List<double[]> steps() {
        return STEPS.stream().map(double[]::clone).toList();
    }

    /**
     * The treads within the block {@code (x, y, z)} of the airlock's model, each its lowest corner then its highest
     * in the block's own space, from 0 to 1; none for a block the stair does not stand in.
     */
    public static List<double[]> treads(final int x, final int y, final int z) {
        return BY_CELL.within(x, y, z);
    }
}
