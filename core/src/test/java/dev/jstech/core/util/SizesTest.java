/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SizesTest {

    @Test
    void times_isPlainMultiplicationWhereItFits() {
        assertEquals(6L, Sizes.times(2L, 3L));
        assertEquals(0L, Sizes.times(0L, Long.MAX_VALUE));
        assertEquals(-6L, Sizes.times(-2L, 3L));
    }

    /*
     * The case the whole class exists for. Written plainly this comes back negative, and a negative amount
     * is not a large number badly written: everything downstream reads it as less than nothing.
     */
    @Test
    void times_stopsAtTheLargestRatherThanGoingRound() {
        assertEquals(Long.MAX_VALUE, Sizes.times(Long.MAX_VALUE, 1000L));
        assertTrue(Long.MAX_VALUE * 1000L < 0L, "which is what it would have done");
    }

    @Test
    void times_stopsAtTheSmallestWhereTheSignsDiffer() {
        assertEquals(Long.MIN_VALUE, Sizes.times(Long.MAX_VALUE, -1000L));
        assertEquals(Long.MIN_VALUE, Sizes.times(-1000L, Long.MAX_VALUE));
    }

    @Test
    void plus_isPlainAdditionWhereItFits() {
        assertEquals(5L, Sizes.plus(2L, 3L));
        assertEquals(-1L, Sizes.plus(2L, -3L));
    }

    @Test
    void plus_stopsAtTheEndsRatherThanGoingRound() {
        assertEquals(Long.MAX_VALUE, Sizes.plus(Long.MAX_VALUE, 1L));
        assertEquals(Long.MIN_VALUE, Sizes.plus(Long.MIN_VALUE, -1L));
    }

    @Test
    void minus_isPlainSubtractionWhereItFits() {
        assertEquals(-1L, Sizes.minus(2L, 3L));
        assertEquals(5L, Sizes.minus(2L, -3L));
    }

    @Test
    void minus_stopsAtTheEndsRatherThanGoingRound() {
        assertEquals(Long.MAX_VALUE, Sizes.minus(Long.MAX_VALUE, -1L));
        assertEquals(Long.MIN_VALUE, Sizes.minus(Long.MIN_VALUE, 1L));
    }

    @Test
    void ceilDiv_roundsUp() {
        assertEquals(1L, Sizes.ceilDiv(1L, 4L));
        assertEquals(1L, Sizes.ceilDiv(4L, 4L));
        assertEquals(2L, Sizes.ceilDiv(5L, 4L));
        assertEquals(3L, Sizes.ceilDiv(9L, 4L));
    }

    @Test
    void ceilDiv_readsNothingAndNonsenseAsNothing() {
        assertEquals(0L, Sizes.ceilDiv(0L, 4L));
        assertEquals(0L, Sizes.ceilDiv(-8L, 4L));
        assertEquals(0L, Sizes.ceilDiv(8L, 0L));
        assertEquals(0L, Sizes.ceilDiv(8L, -4L));
    }

    /*
     * Rounding up is usually written as an addition first, and that addition is itself an amount that can run
     * past the end: the largest size there is would round up to a negative one, and something enormous would
     * then cost nothing at all to store.
     */
    @Test
    void ceilDiv_roundsUpTheLargestSizeThereIs() {
        assertTrue(Sizes.ceilDiv(Long.MAX_VALUE, 4L) > 0L);
        assertEquals(Long.MAX_VALUE / 4L + 1L, Sizes.ceilDiv(Long.MAX_VALUE, 4L));
        assertTrue((Long.MAX_VALUE + 4L - 1L) / 4L < 0L, "which is what the plain form would have done");
    }

    @Test
    void remaining_neverGoesBelowNothing() {
        assertEquals(4L, Sizes.remaining(10L, 6L));
        assertEquals(0L, Sizes.remaining(6L, 10L));
        assertEquals(0L, Sizes.remaining(Long.MIN_VALUE, 1L));
    }
}
