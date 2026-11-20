/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Where an airlock's boarding stair stands let down, as the approved airlock draws it: from its hinge under the outer
 * door's sill, three flights a little over three quarters of a block each fall at 55 degrees out from the hull and
 * down to the land, each with two treads, the floor 2.35 over the land. Its treads, in the airlock's model's space
 * (across from the middle line, up from the hull's foot, along from the airlock's front), are what a player climbs; its
 * rails, each side of the treads, between their ends and the door's sides, keep the climber to the middle of the
 * flights, as the drawn rails do: the outer door's opening, a block wide, takes a player only within a fifth of a block
 * of its middle, and a climber up the stair's edge stopped against its frame. The blocks of the stair hold them, each
 * the treads and rails within it. The Side Door hold away from the wings has the same stair turned across the middle
 * line onto its right flank, under its door's front, its floor as high: its treads and rails are the mirrored ones, in
 * the hold's model's space. Pure, so it is tested without the game.
 */
public final class ShipStairGeometry {

    /** The blocks of the airlock's model the stair stands in, let down, by their places: its treads' first. */
    public static final List<int[]> CELLS;
    /** The blocks of the Side Door hold's model its stair stands in, let down: the airlock's, mirrored. */
    public static final List<int[]> MIRRORED_CELLS;
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
    /* Along the airlock, the outer door's sides, the treads a little within them, the rails between. */
    private static final double DOOR_Z0 = 1.45;
    private static final double DOOR_Z1 = 2.55;
    private static final double WITHIN = 0.06;
    /* How high over each tread the rails beside it stand: a hand's height. */
    private static final double RAIL_HEIGHT = 0.9;
    /* Each tread, its lowest corner then its highest, in the model's space; the same turned across the middle line. */
    private static final List<double[]> STEPS;
    private static final List<double[]> MIRRORED;
    /* The rails beside each tread, the same way; and turned across the middle line. */
    private static final List<double[]> RAILS;
    private static final List<double[]> MIRRORED_RAILS;
    /* The treads within each block of the stair, in the block's own space, by its place; the same, mirrored. */
    private static final TreadCells BY_CELL;
    private static final TreadCells MIRRORED_BY_CELL;
    /* The rails within each block of the stair, the same way; and mirrored. */
    private static final TreadCells RAILS_BY_CELL;
    private static final TreadCells MIRRORED_RAILS_BY_CELL;
    private static final int AXES = 3;

    static {
        final List<double[]> steps = new ArrayList<>();
        final List<double[]> rails = new ArrayList<>();
        final double angle = Math.toRadians(-(180 - SLOPE));
        final double dx = FLIGHT * Math.cos(angle);
        final double dy = FLIGHT * Math.sin(angle);
        for (int k = 0; k < FLIGHTS; k++) {
            for (final double share : TREADS) {
                final double x = HINGE_X + dx * (k + share);
                final double y = HINGE_Y + dy * (k + share);
                steps.add(new double[] {x - TREAD_HALF, y - TREAD_UNDER, DOOR_Z0 + WITHIN, x + TREAD_HALF,
                    y + TREAD_OVER, DOOR_Z1 - WITHIN});
                rails.add(new double[] {x - TREAD_HALF, y - TREAD_UNDER, DOOR_Z0, x + TREAD_HALF,
                    y + TREAD_OVER + RAIL_HEIGHT, DOOR_Z0 + WITHIN});
                rails.add(new double[] {x - TREAD_HALF, y - TREAD_UNDER, DOOR_Z1 - WITHIN, x + TREAD_HALF,
                    y + TREAD_OVER + RAIL_HEIGHT, DOOR_Z1});
            }
        }
        STEPS = List.copyOf(steps);
        RAILS = List.copyOf(rails);
        BY_CELL = TreadCells.of(STEPS);
        RAILS_BY_CELL = TreadCells.of(RAILS);
        CELLS = cells(BY_CELL, RAILS_BY_CELL);
        MIRRORED = STEPS.stream().map(ShipStairGeometry::mirrored).toList();
        MIRRORED_RAILS = RAILS.stream().map(ShipStairGeometry::mirrored).toList();
        MIRRORED_BY_CELL = TreadCells.of(MIRRORED);
        MIRRORED_RAILS_BY_CELL = TreadCells.of(MIRRORED_RAILS);
        MIRRORED_CELLS = cells(MIRRORED_BY_CELL, MIRRORED_RAILS_BY_CELL);
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

    /** Every tread of the Side Door hold's stair, its lowest corner then its highest, in the hold's model's space. */
    public static List<double[]> mirroredSteps() {
        return MIRRORED.stream().map(double[]::clone).toList();
    }

    /**
     * The treads of the Side Door hold's stair within the block {@code (x, y, z)} of the hold's model, each its lowest
     * corner then its highest in the block's own space; none for a block the stair does not stand in.
     */
    public static List<double[]> mirroredTreads(final int x, final int y, final int z) {
        return MIRRORED_BY_CELL.within(x, y, z);
    }

    /** Every rail beside the treads, its lowest corner then its highest, in the airlock's model's space. */
    public static List<double[]> rails() {
        return RAILS.stream().map(double[]::clone).toList();
    }

    /**
     * The rails within the block {@code (x, y, z)} of the airlock's model, each its lowest corner then its highest in
     * the block's own space, from 0 to 1; none for a block they do not stand in.
     */
    public static List<double[]> rails(final int x, final int y, final int z) {
        return RAILS_BY_CELL.within(x, y, z);
    }

    /** The rails of the Side Door hold's stair within the block {@code (x, y, z)} of the hold's model, as above. */
    public static List<double[]> mirroredRails(final int x, final int y, final int z) {
        return MIRRORED_RAILS_BY_CELL.within(x, y, z);
    }

    /* {@code box} turned across the middle line, its lowest corner then its highest still. */
    private static double[] mirrored(final double[] box) {
        return new double[] {-box[AXES], box[1], box[2], -box[0], box[AXES + 1], box[AXES + 2]};
    }

    /* The blocks {@code treads} stand in, then those only {@code rails} reach into. */
    private static List<int[]> cells(final TreadCells treads, final TreadCells rails) {
        final List<int[]> all = new ArrayList<>(treads.cells());
        for (final int[] cell : rails.cells()) {
            if (all.stream().noneMatch(each -> Arrays.equals(each, cell))) {
                all.add(cell);
            }
        }
        return List.copyOf(all);
    }
}
