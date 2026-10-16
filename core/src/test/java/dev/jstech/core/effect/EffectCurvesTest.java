/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EffectCurvesTest {

    private static final double CLOSE = 1e-9;
    private static final int OPAQUE_WHITE = 0xFF << 24 | 0xFFFFFF;
    private static final int CLEAR_BLACK = 0;

    @Test
    void at_oneKeyHoldsAllItsLife() {
        assertEquals(0.4, EffectCurves.at(List.of(0.4), 0.7), CLOSE);
    }

    @Test
    void at_twoKeysGoFromTheFirstToTheSecond() {
        assertEquals(0.4, EffectCurves.at(List.of(0.4, 2.0), 0), CLOSE);
        assertEquals(1.2, EffectCurves.at(List.of(0.4, 2.0), 0.5), CLOSE);
        assertEquals(2.0, EffectCurves.at(List.of(0.4, 2.0), 1), CLOSE);
    }

    @Test
    void at_keysAreSpreadEvenly() {
        final List<Double> keys = List.of(0.0, 10.0, 0.0);
        assertEquals(10, EffectCurves.at(keys, 0.5), CLOSE);
        assertEquals(5, EffectCurves.at(keys, 0.25), CLOSE);
        assertEquals(5, EffectCurves.at(keys, 0.75), CLOSE);
    }

    @Test
    void at_holdsBeyondItsLife() {
        assertEquals(2.0, EffectCurves.at(List.of(0.4, 2.0), 3), CLOSE);
        assertEquals(0.4, EffectCurves.at(List.of(0.4, 2.0), -1), CLOSE);
    }

    @Test
    void colourAt_blendsEveryChannelWithItsAlpha() {
        final int half = EffectCurves.colourAt(List.of(OPAQUE_WHITE, CLEAR_BLACK), 0.5);
        assertEquals(128, half >>> 24);
        assertEquals(128, half >> 16 & 0xFF);
        assertEquals(128, half & 0xFF);
    }
}
