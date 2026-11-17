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
 * the right wing's as they are, the left wing's turned across the middle line; the blocks of the stair hold them, each
 * the treads within it. Whether a side's stair is down is kept by a block of that side's wing by its root. Pure, so it
 * is tested without the game.
 */
public final class ShipWingStairGeometry {

    /*
     * Its model's name; where its root's chord runs along the hull's side, from the root's front; the wing's top over
     * the stair, its hinge's height and place along, its flights; and the layer of the block keeping its state.
     */
    private final String model;
    private final double lead;
    private final double trail;
    private final double top;
    private final double hingeY;
    private final double hingeZ;
    private final int flights;
    private final int stateY;
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
    /* The right wing's block keeping its stair's state: across, its root's first; along, a block past its front. */
    private static final int STATE_X = 2;
    private static final int STATE_Z = 1;
    private static final int AXES = 3;
    /*
     * The three wings that leave the hull low enough to walk out onto, as their models draw them: each its root's
     * chord along the hull's side, its top over the stair, its hinge under its underside and at its trailing edge
     * where it leaves the stair's inner side, its flights, and the layer of its root's blocks.
     */
    /** The swept wings, their roots a block over the deck. */
    public static final ShipWingStairGeometry SWEPT = new ShipWingStairGeometry("small_swept_wings", 0.2, 4.8,
            1.368, 0.994, 4.79, 5, 1);
    /** The stub wings, their roots a block over the deck. */
    public static final ShipWingStairGeometry STUB = new ShipWingStairGeometry("small_stub_wings", 0.26, 5.08, 1.65,
            1.205, 5.04, 5, 1);
    /** The blended delta, level with the deck. */
    public static final ShipWingStairGeometry DELTA = new ShipWingStairGeometry("small_blended_delta", -0.4, 7.8,
            0.419, -0.024, 7.78, 3, 0);
    /** Every wing with a stair. */
    public static final List<ShipWingStairGeometry> ALL = List.of(SWEPT, STUB, DELTA);

    private ShipWingStairGeometry(final String model, final double lead, final double trail, final double top,
                                  final double hingeY, final double hingeZ, final int flights, final int stateY) {
        this.model = model;
        this.lead = lead;
        this.trail = trail;
        this.top = top;
        this.hingeY = hingeY;
        this.hingeZ = hingeZ;
        this.flights = flights;
        this.stateY = stateY;
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

    /** Where its root's chord begins along the hull's side, from the front of the section it hangs from. */
    public double lead() {
        return this.lead;
    }

    /** Where its root's chord ends along the hull's side, its trailing edge there, from the section's front. */
    public double trail() {
        return this.trail;
    }

    /** The block of the wing's model keeping whether the stair under its left wing, or its right, is down. */
    public int[] stateCell(final boolean left) {
        return new int[] {left ? -1 - STATE_X : STATE_X, this.stateY, STATE_Z};
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

    /** The blocks of the wing's model the stair under its left wing, or its right, stands in let down. */
    public List<int[]> cells(final boolean left) {
        final List<int[]> right = this.byCell.cells();
        return left ? right.stream().map(cell -> new int[] {-1 - cell[0], cell[1], cell[2]}).toList() : right;
    }

    /**
     * The treads within the block {@code (x, y, z)} of the wing's model, each its lowest corner then its highest in
     * the block's own space, from 0 to 1: the left wing's stair's for a block left of the middle line, turned across
     * it; none for a block the stair does not stand in.
     */
    public List<double[]> treads(final int x, final int y, final int z) {
        if (x >= 0) {
            return this.byCell.within(x, y, z);
        }
        return this.byCell.within(-1 - x, y, z).stream().map(tread -> new double[] {1 - tread[AXES], tread[1],
            tread[2], 1 - tread[0], tread[AXES + 1], tread[AXES + 2]}).toList();
    }
}
