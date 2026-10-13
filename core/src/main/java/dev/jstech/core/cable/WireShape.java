/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

/**
 * How a wire that keeps a shape picks the faces it joins in a block, out of the faces it could join: one that runs only
 * straight keeps to one axis, and one that joins at most so many faces stops there.
 *
 * <p>A wire keeps the faces it already joins while it still can, so laying a cable beside a run never pulls the run
 * apart: a branch laid against a straight fibre or against a line already joined at both ends is the one left out.
 * Where nothing was joined yet, the axis with the most faces wins, and a tie goes up and down first, then north and
 * south, then west and east.
 *
 * <p>Faces are bits by the game's numbering of them: down 0, up 1, north 2, south 3, west 4, east 5. Pure.
 */
public final class WireShape {

    /* The faces of each axis, in the order a tie is settled in: up and down, north and south, west and east. */
    private static final int[] AXES = {0b000011, 0b001100, 0b110000};
    private static final int FACES = 6;

    private WireShape() {
    }

    /** Whether a wire joining {@code faces} keeps its shape. */
    public static boolean allows(final boolean straight, final int most, final int faces) {
        if (straight && axisOf(faces) < 0 && faces != 0) {
            return false;
        }
        return most <= 0 || Integer.bitCount(faces) <= most;
    }

    /**
     * The faces a wire joins out of {@code candidates}, having joined {@code before}: the shape kept, what it already
     * joins kept first.
     */
    public static int select(final boolean straight, final int most, final int candidates, final int before) {
        int chosen = candidates;
        if (straight) {
            chosen &= AXES[axisFor(candidates, before & candidates)];
        }
        if (most > 0 && Integer.bitCount(chosen) > most) {
            chosen = keepAtMost(chosen, before & chosen, most);
        }
        return chosen;
    }

    /* The axis a straight wire keeps: the one it already joins along, or else the one with the most faces. */
    private static int axisFor(final int candidates, final int kept) {
        final int already = axisOf(kept);
        if (already >= 0 && kept != 0) {
            return already;
        }
        int best = 0;
        for (int axis = 1; axis < AXES.length; axis++) {
            if (Integer.bitCount(candidates & AXES[axis]) > Integer.bitCount(candidates & AXES[best])) {
                best = axis;
            }
        }
        return best;
    }

    /* The one axis every face of {@code faces} lies on, or -1 when they lie on more than one, or there are none. */
    private static int axisOf(final int faces) {
        for (int axis = 0; axis < AXES.length; axis++) {
            if (faces != 0 && (faces & ~AXES[axis]) == 0) {
                return axis;
            }
        }
        return -1;
    }

    /* At most {@code most} of {@code faces}: those already joined first, then the rest in the order of the faces. */
    private static int keepAtMost(final int faces, final int kept, final int most) {
        int chosen = 0;
        for (int face = 0; face < FACES && Integer.bitCount(chosen) < most; face++) {
            if ((kept & 1 << face) != 0) {
                chosen |= 1 << face;
            }
        }
        for (int face = 0; face < FACES && Integer.bitCount(chosen) < most; face++) {
            if ((faces & 1 << face) != 0) {
                chosen |= 1 << face;
            }
        }
        return chosen;
    }
}
