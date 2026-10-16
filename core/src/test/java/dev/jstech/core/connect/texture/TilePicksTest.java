/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TilePicksTest {

    private static final int DOWN = 0;
    private static final int UP = 1;
    private static final int NORTH = 2;
    private static final int TRIES = 10_000;

    @Test
    void seed_isTheSameForAPlaceEveryTime() {
        assertEquals(TilePicks.seed(4, 70, -9, NORTH, TilePicks.EACH_FACE, 0),
                TilePicks.seed(4, 70, -9, NORTH, TilePicks.EACH_FACE, 0));
        assertNotEquals(TilePicks.seed(4, 70, -9, NORTH, TilePicks.EACH_FACE, 0),
                TilePicks.seed(5, 70, -9, NORTH, TilePicks.EACH_FACE, 0), "the place beside it picks for itself");
    }

    @Test
    void seed_pairsTheFacesAsTheSymmetrySays() {
        assertNotEquals(TilePicks.seed(1, 2, 3, DOWN, TilePicks.EACH_FACE, 0),
                TilePicks.seed(1, 2, 3, UP, TilePicks.EACH_FACE, 0));
        assertEquals(TilePicks.seed(1, 2, 3, DOWN, TilePicks.OPPOSITE_FACES, 0),
                TilePicks.seed(1, 2, 3, UP, TilePicks.OPPOSITE_FACES, 0), "down and up are across from each other");
        assertEquals(TilePicks.seed(1, 2, 3, DOWN, TilePicks.ALL_FACES, 0),
                TilePicks.seed(1, 2, 3, NORTH, TilePicks.ALL_FACES, 0));
        assertNotEquals(TilePicks.seed(1, 2, 3, DOWN, TilePicks.EACH_FACE, 0),
                TilePicks.seed(1, 2, 3, DOWN, TilePicks.EACH_FACE, 2), "more loops mix it again");
    }

    @Test
    void weighted_picksEachTileAsOftenAsItsWeight() {
        final int[] counts = new int[3];
        for (int i = 0; i < TRIES; i++) {
            counts[TilePicks.weighted(TilePicks.seed(i, 0, i * 7, UP, TilePicks.EACH_FACE, 0),
                    new int[] {1, 2, 7})]++;
        }
        assertTrue(Math.abs(counts[0] - TRIES / 10) < TRIES / 40, "about a tenth: " + counts[0]);
        assertTrue(Math.abs(counts[2] - TRIES * 7 / 10) < TRIES / 20, "about seven tenths: " + counts[2]);
        assertEquals(0, TilePicks.weighted(42L, new int[] {0, 0}), "no weight at all: the first");
    }

    @Test
    void any_staysWithinItsTiles() {
        for (int i = 0; i < TRIES; i++) {
            final int tile = TilePicks.any(TilePicks.seed(i, i, i, UP, TilePicks.EACH_FACE, 0), 5);
            assertTrue(tile >= 0 && tile < 5);
        }
        assertEquals(0, TilePicks.any(9L, 0));
    }

    @Test
    void pattern_laysItsTilesOverTheWorld() {
        assertEquals(0, TilePicks.pattern(0, 0, 3, 2, 0, 0));
        assertEquals(2, TilePicks.pattern(2, 0, 3, 2, 0, 0));
        assertEquals(3, TilePicks.pattern(0, 1, 3, 2, 0, 0), "the second row");
        assertEquals(0, TilePicks.pattern(3, 2, 3, 2, 0, 0), "round again");
        assertEquals(5, TilePicks.pattern(-1, -1, 3, 2, 0, 0), "below and left of the start too");
        assertEquals(1, TilePicks.pattern(0, 0, 3, 2, 1, 0), "moved across by one");
    }
}
