/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CompressionMathTest {

    private static final double ROOM = 293.15;

    @Test
    void work_isTheGasLawsWorkOverTheEfficiency() {
        // A cubic metre of gas at the open air's pressure, 44.6 moles, pressed to 200 bar.
        assertEquals(823_081.0, CompressionMath.work(1_000.0, 100.0, 20_000.0, ROOM, 0.7), 50.0);
    }

    @Test
    void work_growsWithTheRatioOfThePressuresNotTheirDifference() {
        final double toTwo = CompressionMath.work(1_000.0, 100.0, 200.0, ROOM, 1.0);
        final double toTwoHundred = CompressionMath.work(1_000.0, 100.0, 20_000.0, ROOM, 1.0);
        assertEquals(Math.log(200.0) / Math.log(2.0), toTwoHundred / toTwo, 1e-9);
        assertEquals(toTwo, CompressionMath.work(1_000.0, 1_000.0, 2_000.0, ROOM, 1.0), 1e-6);
    }

    @Test
    void work_costsNothingWithoutGasOrARise() {
        assertEquals(0.0, CompressionMath.work(0.0, 100.0, 200.0, ROOM, 0.8));
        assertEquals(0.0, CompressionMath.work(1_000.0, 200.0, 100.0, ROOM, 0.8));
    }

    @Test
    void work_refusesAnEfficiencyPastTheWhole() {
        assertThrows(IllegalArgumentException.class, () -> CompressionMath.work(1.0, 100.0, 200.0, ROOM, 1.5));
        assertThrows(IllegalArgumentException.class, () -> CompressionMath.work(1.0, 0.0, 200.0, ROOM, 0.5));
    }

    @Test
    void held_isTheVolumeTimesThePressure() {
        assertEquals(20_000.0, CompressionMath.held(1_000.0, 20 * 101.325), 1e-6);
    }
}
