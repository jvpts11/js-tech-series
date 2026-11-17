/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import java.util.ArrayList;
import java.util.List;

/**
 * The Rocket Hangar's bay door as it moves, as the approved hangar draws it, the Vehicle Assembly Building's: its lower
 * door's two halves slide apart into the walls at its sides, each half shrinking toward its outer edge, and the slot's
 * seven leaves fold away, each shrinking toward its foot; every block of a half or a leaf goes with it. Places are the
 * hangar's pattern's, a block's from its lowest corner: across the door, then up. Pure, so it is tested without the
 * game.
 */
public final class HangarDoorMotion {

    /** How long the door takes to open or shut, in ticks: a few seconds. */
    public static final int TICKS = 100;
    /** Across the lower door, its outer edges, and the line its halves part along, its middle block's middle. */
    public static final double LOWER_FROM = -HangarStructure.LOWER_HALF;
    public static final double LOWER_TO = HangarStructure.LOWER_HALF + 1;
    public static final double MIDDLE = 0.5;
    /** Up the slot, from the lower door's top to the slot's, and how many leaves it is. */
    public static final double SLOT_FROM = HangarStructure.LOWER_TOP + 1;
    public static final double SLOT_TO = HangarStructure.SLOT_TOP + 1;
    public static final int LEAVES = 7;

    private HangarDoorMotion() {
    }

    /**
     * How far open the door stands, from nought (shut) to one, {@code ticks} after it began to open ({@code opening})
     * or to shut.
     */
    public static double share(final boolean opening, final double ticks) {
        final double moved = Math.clamp(ticks / TICKS, 0, 1);
        return opening ? moved : 1 - moved;
    }

    /**
     * Where the door's block at ({@code x}, {@code y}) of the pattern is drawn, the door {@code open} of the way open:
     * the boxes, each {from across, from up, to across, to up}, its parts are drawn squeezed into; none once open.
     */
    public static List<double[]> pieces(final int x, final int y, final double open) {
        final double shut = 1 - Math.clamp(open, 0, 1);
        final List<double[]> out = new ArrayList<>();
        if (shut <= 0) {
            return out;
        }
        if (y < SLOT_FROM) {
            // The lower door: each half shrinks toward its outer edge.
            final double[][] halves = {{LOWER_FROM, MIDDLE}, {MIDDLE, LOWER_TO}};
            for (final double[] half : halves) {
                final double from = Math.max(x, half[0]);
                final double to = Math.min(x + 1, half[1]);
                if (to > from) {
                    final boolean left = half[0] == LOWER_FROM;
                    final double edge = left ? LOWER_FROM : LOWER_TO;
                    out.add(new double[] {edge + (from - edge) * shut, y, edge + (to - edge) * shut, y + 1});
                }
            }
            return out;
        }
        // The slot: each leaf shrinks toward its foot.
        final double leaf = (SLOT_TO - SLOT_FROM) / LEAVES;
        for (int k = 0; k < LEAVES; k++) {
            final double foot = SLOT_FROM + k * leaf;
            final double from = Math.max(y, foot);
            final double to = Math.min(y + 1, foot + leaf);
            if (to > from) {
                out.add(new double[] {x, foot + (from - foot) * shut, x + 1, foot + (to - foot) * shut});
            }
        }
        return out;
    }
}
