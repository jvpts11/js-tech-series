/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TintGridTest {

    private static final int RED = 0xFF0000;
    private static final int BLUE = 0x0000FF;

    @Test
    void at_takesThePlaceInFrontOfAFaceAtItsMiddle() {
        final int[] colours = whites();
        colours[TintGrid.index(0, 1, 0)] = RED;
        final TintGrid grid = new TintGrid(colours, allLit());

        assertEquals(RED, grid.at(0.5, 1.5, 0.5));
    }

    @Test
    void at_blendsHalfWayBetweenTwoPlaces() {
        final int[] colours = whites();
        colours[TintGrid.index(0, 1, 0)] = RED;
        colours[TintGrid.index(1, 1, 0)] = BLUE;
        final TintGrid grid = new TintGrid(colours, allLit());

        assertEquals(0x800080, grid.at(1.0, 1.5, 0.5));
    }

    @Test
    void at_leavesOutPlacesNoLightReaches() {
        final int[] colours = whites();
        colours[TintGrid.index(0, 1, 0)] = RED;
        final boolean[] lit = allLit();
        lit[TintGrid.index(1, 1, 0)] = false;
        final TintGrid grid = new TintGrid(colours, lit);

        assertEquals(RED, grid.at(1.0, 1.5, 0.5));
    }

    @Test
    void at_isWhiteWhereNothingIsLit() {
        final int[] colours = whites();
        Arrays.fill(colours, RED);
        final boolean[] lit = new boolean[TintGrid.SIZE];
        final TintGrid grid = new TintGrid(colours, lit);

        assertEquals(LightMath.WHITE, grid.at(0.5, 0.5, 0.5));
    }

    @Test
    void at_holdsPointsBeyondTheGridAtItsEdge() {
        final int[] colours = whites();
        colours[TintGrid.index(1, 1, 1)] = BLUE;
        final TintGrid grid = new TintGrid(colours, allLit());

        assertEquals(BLUE, grid.at(4.0, 4.0, 4.0));
    }

    @Test
    void plain_isTrueOnlyWithoutALitColour() {
        final int[] colours = whites();
        assertTrue(new TintGrid(colours, allLit()).plain());
        colours[TintGrid.index(-1, 0, 0)] = RED;
        assertFalse(new TintGrid(colours, allLit()).plain());
        assertTrue(new TintGrid(colours, new boolean[TintGrid.SIZE]).plain());
    }

    @Test
    void index_coversEveryPlaceOnce() {
        final boolean[] seen = new boolean[TintGrid.SIZE];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    seen[TintGrid.index(dx, dy, dz)] = true;
                }
            }
        }
        for (final boolean each : seen) {
            assertTrue(each);
        }
    }

    @Test
    void constructor_refusesAGridOfAnotherSize() {
        assertThrows(IllegalArgumentException.class, () -> new TintGrid(new int[3], new boolean[3]));
    }

    private static int[] whites() {
        final int[] colours = new int[TintGrid.SIZE];
        Arrays.fill(colours, LightMath.WHITE);
        return colours;
    }

    private static boolean[] allLit() {
        final boolean[] lit = new boolean[TintGrid.SIZE];
        Arrays.fill(lit, true);
        return lit;
    }
}
