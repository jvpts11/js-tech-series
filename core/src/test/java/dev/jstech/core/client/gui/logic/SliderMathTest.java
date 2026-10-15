/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SliderMathTest {

    @Test
    void fraction_fallsWhereThePointerIsWithinTheTrack() {
        assertEquals(0.0, SliderMath.fraction(0, 10, 100, 8));
        assertEquals(1.0, SliderMath.fraction(500, 10, 100, 8));
        assertEquals(0.5, SliderMath.fraction(60, 10, 100, 8), 1e-9);
    }

    @Test
    void snap_putsAFractionOnTheNearestStep() {
        assertEquals(7 / 15.0, SliderMath.snap(0.47, 16), 1e-9);
        assertEquals(0.47, SliderMath.snap(0.47, 0), 1e-9);
        assertEquals(7, SliderMath.index(7 / 15.0, 16));
    }

    @Test
    void step_stopsAtTheEnds() {
        assertEquals(1.0, SliderMath.step(14 / 15.0, 16, 1), 1e-9);
        assertEquals(0.0, SliderMath.step(0.0, 16, -1), 1e-9);
    }
}
