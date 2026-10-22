/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChunkLoadingBalanceTest {

    @BeforeEach
    void defaults() {
        ChunkLoadingBalance.setKeepWhileAway(true);
        ChunkLoadingBalance.setAwayHours(0);
    }

    @Test
    void letsGoAfter_neverByDefault() {
        assertFalse(ChunkLoadingBalance.letsGoAfter(0));
        assertFalse(ChunkLoadingBalance.letsGoAfter(Long.MAX_VALUE / 2));
    }

    @Test
    void letsGoAfter_atOnceWhenNothingIsKeptWhileAway() {
        ChunkLoadingBalance.setKeepWhileAway(false);
        ChunkLoadingBalance.setAwayHours(5);
        assertTrue(ChunkLoadingBalance.letsGoAfter(0));
    }

    @Test
    void letsGoAfter_onceTheHoursAllowedHavePassed() {
        ChunkLoadingBalance.setAwayHours(2);
        assertFalse(ChunkLoadingBalance.letsGoAfter(2 * ChunkLoadingBalance.TICKS_AN_HOUR - 1));
        assertTrue(ChunkLoadingBalance.letsGoAfter(2 * ChunkLoadingBalance.TICKS_AN_HOUR));
    }

    @Test
    void setAwayHours_takesANegativeNumberAsNoEnd() {
        ChunkLoadingBalance.setAwayHours(-3);
        assertFalse(ChunkLoadingBalance.letsGoAfter(Long.MAX_VALUE / 2));
    }
}
