/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BlockSquareTest {

    private static final String ONE = "trc1q8f3k2w9xv7m4d0s6l2c8hz5r3n7h4x";
    private static final String TWO = "trc1qk2nx8f3k2w9xv7m4d0s6l2c88s0a";

    @Test
    void of_drawsTheSameAddressTheSameWay() {
        assertArrayEquals(BlockSquare.of(ONE), BlockSquare.of(ONE));
    }

    @Test
    void of_drawsAnotherAddressAnotherWay() {
        assertFalse(Arrays.deepEquals(BlockSquare.of(ONE), BlockSquare.of(TWO)));
    }

    @Test
    void of_marksThreeCornersWithRings() {
        final boolean[][] square = BlockSquare.of(ONE);
        final int last = BlockSquare.SIDE - 1;

        assertTrue(square[0][0] && square[0][last] && square[last][0]);
        assertFalse(square[1][1]);
        assertTrue(square[3][3]);
        assertFalse(square[7][7]);
    }
}
