/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeterScaleTest {

    @Test
    void upTo_printsTheFirstRoundNumberAtOrAboveTheValue() {
        assertEquals(new MeterScale(5_000L, "k", "5", "2.5"), MeterScale.upTo(5_000L));
        assertEquals(new MeterScale(100_000L, "k", "100", "50"), MeterScale.upTo(100_000L));
        assertEquals(new MeterScale(500_000L, "k", "500", "250"), MeterScale.upTo(400_000L));
        assertEquals(new MeterScale(20_000_000_000L, "G", "20", "10"), MeterScale.upTo(12_000_000_000L));
        assertEquals(new MeterScale(2_000_000_000L, "G", "2", "1"), MeterScale.upTo(1_500_000_000L));
    }

    @Test
    void upTo_movesToTheNextPrefixAtAThousand() {
        assertEquals(new MeterScale(1_000L, "k", "1", "0.5"), MeterScale.upTo(600L));
        assertEquals(new MeterScale(1_000_000_000L, "G", "1", "0.5"), MeterScale.upTo(600_000_000L));
    }

    @Test
    void upTo_showsAtLeastOneForNothing() {
        assertEquals(new MeterScale(1L, "", "1", "0.5"), MeterScale.upTo(0L));
        assertEquals(new MeterScale(1L, "", "1", "0.5"), MeterScale.upTo(-3L));
        assertEquals(new MeterScale(2L, "", "2", "1"), MeterScale.upTo(2L));
    }

    @Test
    void fraction_standsBetweenTheFootAndTheFullScale() {
        final MeterScale scale = MeterScale.upTo(100L);

        assertEquals(0.5, scale.fraction(50L), 1e-9);
        assertEquals(0.0, scale.fraction(-10L), 1e-9);
        assertEquals(1.0, scale.fraction(250L), 1e-9);
    }
}
