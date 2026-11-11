/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * Where a Mining Computer's risers stand (computers/docs/design/teracoin.md), in sixteenths of a block, as its model
 * draws them facing north: x from the origin block's west side eastward (one block, or two: a wide rig's second block
 * stands east of its origin, on the left of a player facing its front), y up from the floor, z from the front, the
 * north face, to the back. The risers lie across the rails at even steps about the rig's middle, and a card stands on
 * each, its bracket to the front. A player aiming at a riser's place seats a card there. Pure.
 *
 * @param risers how many risers it carries
 * @param width  how wide it is: 16, or 32 for a rig two blocks wide
 * @param pitch  the step from one riser to the next
 * @param left   the left of the run of risers
 * @param right  the right of the run of risers
 * @param railY  the top of the rails the risers lie on
 * @param front  where the risers' places begin, from the front
 */
public record RigGeometry(int risers, int width, double pitch, double left, double right, double railY,
                          double front) {

    /** The Transition's rig: four risers in one block. */
    public static final RigGeometry TRANSITION = new RigGeometry(4, 16, 3.6, 2.0, 14.0, 9.2, 1.2);
    /** The Standard's rig: six risers two blocks wide. */
    public static final RigGeometry STANDARD = new RigGeometry(6, 32, 4.6, 1.5, 30.5, 8.4, 1.0);
    /** The Advanced's rig: eight risers two blocks wide. */
    public static final RigGeometry ADVANCED = new RigGeometry(8, 32, 3.55, 1.5, 30.5, 8.4, 1.0);
    /* How far a riser's place reaches under the rail, over it (a card standing on it), and from its front back. */
    private static final double BELOW = 0.5;
    private static final double ABOVE = 5.0;
    private static final double DEPTH = 9.5;
    /* A place's sides stand this far inside half a step, so two neighbours never touch. */
    private static final double GAP = 0.1;
    private static final double BLOCK = 16.0;
    private static final double MIDDLE = 0.5;
    private static final int TURNS = 4;

    /** The rig of {@code era}: the Transition's in one block, the Standard's and the Advanced's two blocks wide. */
    public static RigGeometry of(final Era era) {
        if (era.isAtMost(HardwareEra.TRANSITION)) {
            return TRANSITION;
        }
        return era.isAtMost(HardwareEra.STANDARD) ? STANDARD : ADVANCED;
    }

    /** The middle of riser {@code index} across the rig. */
    public double riserX(final int index) {
        return (left + right) / 2.0 - (risers - 1) * pitch / 2.0 + index * pitch;
    }

    /** The place of riser {@code index}, facing north: {x0, y0, z0, x1, y1, z1}, in sixteenths. */
    public double[] place(final int index) {
        final double x = riserX(index);
        final double half = pitch / 2.0 - GAP;
        return new double[] {x - half, railY - BELOW, front, x + half, railY + ABOVE, front + DEPTH};
    }

    /**
     * The place of riser {@code index} of a rig turned {@code turns} quarter turns clockwise seen from above (0 facing
     * north, 1 east, 2 south, 3 west), in blocks from the origin block's corner: {x0, y0, z0, x1, y1, z1}. The rig
     * turns about its origin block's middle, as its footprint does.
     */
    public double[] placeTurned(final int index, final int turns) {
        final double[] at = place(index);
        final double[] a = turned(at[0] / BLOCK, at[2] / BLOCK, turns);
        final double[] b = turned(at[3] / BLOCK, at[5] / BLOCK, turns);
        return new double[] {Math.min(a[0], b[0]), at[1] / BLOCK, Math.min(a[1], b[1]), Math.max(a[0], b[0]),
                at[4] / BLOCK, Math.max(a[1], b[1])};
    }

    /* The point (x, z) of a rig facing north, in blocks, where it stands once turned {@code turns} quarter turns. */
    private static double[] turned(final double x, final double z, final int turns) {
        final double cx = x - MIDDLE;
        final double cz = z - MIDDLE;
        return switch (Math.floorMod(turns, TURNS)) {
            case 1 -> new double[] {-cz + MIDDLE, cx + MIDDLE};
            case 2 -> new double[] {-cx + MIDDLE, -cz + MIDDLE};
            case 3 -> new double[] {cz + MIDDLE, -cx + MIDDLE};
            default -> new double[] {x, z};
        };
    }
}
