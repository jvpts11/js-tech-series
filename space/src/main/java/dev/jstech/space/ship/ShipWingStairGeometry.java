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
import java.util.Optional;

/**
 * Where a wing's stair stands let down, as the approved storyboard draws it: folded flat under the wing, its hinge
 * where the trailing edge leaves the hull's side, it swings down behind the wing to the land, its flights falling at
 * 54 degrees, each with two treads, so a crew member out of the Side Door hold walks along the wing and down to the
 * land. A player standing on its highest tread just past the trailing edge is a step under the wing's top. Its treads
 * are in the wing's model's space (across from the middle line, up from the hull's foot, along from the wing's root),
 * the stair under the right wing; the blocks of the stair hold them, each the treads within it. Pure, so it is tested
 * without the game.
 */
public final class ShipWingStairGeometry {

    /* Its model's name, the wing's top over the stair, its hinge's height and place along, and its flights. */
    private final String model;
    private final double top;
    private final double hingeY;
    private final double hingeZ;
    private final int flights;
    /* Each tread, its lowest corner then its highest, in the model's space. */
    private final List<double[]> steps;
    /* The treads within each block of the stair, in the block's own space, by its place. */
    private final TreadCells byCell;

    /** Across the wing from the middle line, where the stair stands, in blocks: a block beside the hull's side. */
    public static final double ACROSS_FROM = 2.0;
    public static final double ACROSS_TO = 3.0;
    /* How steeply its flights fall; the land under the hull's foot. */
    private static final double SLOPE = 54.0;
    private static final double LAND = -2.0;
    /* Each flight's treads, at those shares along it; a tread's half depth, how far under and over its line. */
    private static final double[] TREADS = {0.2, 0.7};
    private static final double TREAD_HALF = 0.17;
    private static final double TREAD_UNDER = 0.03;
    private static final double TREAD_OVER = 0.02;
    /* The treads a little within the stair's sides. */
    private static final double WITHIN = 0.06;
    /*
     * The three wings that leave the hull low enough to walk out onto: each its top over the stair, its hinge under
     * its underside and at its trailing edge where it leaves the stair's inner side, as the wings' models draw them.
     */
    /** The swept wings, their roots a block over the deck. */
    public static final ShipWingStairGeometry SWEPT = new ShipWingStairGeometry("small_swept_wings", 1.368, 0.994,
            4.79, 5);
    /** The stub wings, their roots a block over the deck. */
    public static final ShipWingStairGeometry STUB = new ShipWingStairGeometry("small_stub_wings", 1.65, 1.205, 5.04,
            5);
    /** The blended delta, level with the deck. */
    public static final ShipWingStairGeometry DELTA = new ShipWingStairGeometry("small_blended_delta", 0.419, -0.024,
            7.78, 3);
    /** Every wing with a stair. */
    public static final List<ShipWingStairGeometry> ALL = List.of(SWEPT, STUB, DELTA);

    private ShipWingStairGeometry(final String model, final double top, final double hingeY, final double hingeZ,
                                  final int flights) {
        this.model = model;
        this.top = top;
        this.hingeY = hingeY;
        this.hingeZ = hingeZ;
        this.flights = flights;
        // Its flights as long as they must be to reach the land from the hinge at that slope.
        final double angle = Math.toRadians(SLOPE);
        final double flight = (hingeY - LAND) / (flights * Math.sin(angle));
        final List<double[]> made = new ArrayList<>();
        for (int k = 0; k < flights; k++) {
            for (final double share : TREADS) {
                final double along = flight * (k + share);
                final double y = hingeY - along * Math.sin(angle);
                final double z = hingeZ + along * Math.cos(angle);
                made.add(new double[] {ACROSS_FROM + WITHIN, y - TREAD_UNDER, z - TREAD_HALF, ACROSS_TO - WITHIN,
                    y + TREAD_OVER, z + TREAD_HALF});
            }
        }
        this.steps = List.copyOf(made);
        this.byCell = TreadCells.of(this.steps);
    }

    /** The wing whose model is called {@code name} (its path's last part), if it has a stair. */
    public static Optional<ShipWingStairGeometry> of(final String name) {
        return ALL.stream().filter(wing -> wing.model.equals(name)).findFirst();
    }

    /** Its model's name, its path's last part. */
    public String model() {
        return this.model;
    }

    /** The wing's top over the stair, up from the hull's foot. */
    public double top() {
        return this.top;
    }

    /** How high its hinge stands under the wing, up from the hull's foot. */
    public double hingeY() {
        return this.hingeY;
    }

    /** Where its hinge stands along the wing from its root: the trailing edge over the stair's inner side. */
    public double hingeZ() {
        return this.hingeZ;
    }

    /** How many flights it has. */
    public int flights() {
        return this.flights;
    }

    /** Every tread, its lowest corner then its highest, six numbers, in the wing's model's space. */
    public List<double[]> steps() {
        return this.steps.stream().map(double[]::clone).toList();
    }

    /** The blocks of the wing's model the stair stands in let down, each its three numbers. */
    public List<int[]> cells() {
        return this.byCell.cells();
    }

    /**
     * The treads within the block {@code (x, y, z)} of the wing's model, each its lowest corner then its highest in
     * the block's own space, from 0 to 1; none for a block the stair does not stand in.
     */
    public List<double[]> treads(final int x, final int y, final int z) {
        return this.byCell.within(x, y, z);
    }
}
