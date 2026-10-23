/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChipFormulasTest {

    @Test
    void diesPerWafer_giveAboutFortyTimesOnThreeHundredMillimetresAsOnFifty() {
        final double ratio = (double) ChipFormulas.diesPerWafer(300.0, 10.0) / ChipFormulas.diesPerWafer(50.0, 10.0);
        assertTrue(ratio > 36.0 && ratio < 46.0, "ratio " + ratio);
    }

    @Test
    void diesPerWafer_giveNoneOfADieLargerThanTheWafer() {
        assertEquals(0L, ChipFormulas.diesPerWafer(10.0, 1_000.0));
        assertEquals(0L, ChipFormulas.diesPerWafer(0.0, 10.0));
    }

    @Test
    void goodShare_fallsAsTheDefectsRise() {
        assertEquals(1.0, ChipFormulas.goodShare(100.0, 0.0));
        assertTrue(ChipFormulas.goodShare(100.0, 0.01) > ChipFormulas.goodShare(100.0, 0.02));
    }

    @Test
    void particlesAllowed_areTheIsoClassesLimits() {
        assertEquals(3_520.0, ChipFormulas.particlesAllowed(5), 1e-6);
        assertEquals(3_520_000.0, ChipFormulas.particlesAllowed(8), 1e-3);
    }
}
