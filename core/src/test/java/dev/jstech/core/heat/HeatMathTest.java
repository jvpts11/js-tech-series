/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HeatMathTest {

    @Test
    void conductance_ofTwoOfTheSameIsTheirConductivity() {
        assertEquals(400.0, HeatMath.conductance(400.0, 400.0), 1e-9);
        assertEquals(0.0, HeatMath.conductance(400.0, 0.0), 1e-9);
    }

    @Test
    void conductance_isHeldBackByThePoorerConductor() {
        final double copperToStone = HeatMath.conductance(400.0, 2.0);
        assertTrue(copperToStone < 4.0 && copperToStone > 3.9, "about twice the stone's: " + copperToStone);
    }

    @Test
    void convection_followsTheAirsThicknessAndStopsInAVacuum() {
        assertEquals(100.0, HeatMath.convection(310.0, 300.0, HeatMath.SEA_LEVEL_KPA, 1.0), 1e-9);
        assertEquals(50.0, HeatMath.convection(310.0, 300.0, HeatMath.SEA_LEVEL_KPA / 2, 1.0), 1e-9);
        assertEquals(0.0, HeatMath.convection(1_000.0, 3.0, 0.0, 6.0), 1e-9);
    }

    @Test
    void radiation_ofABlackBodyAtAThousandKelvin() {
        final double watts = HeatMath.radiation(1_000.0, 0.0, 1.0, 1.0);
        assertEquals(56_703.74, watts, 0.01);
        assertTrue(HeatMath.radiation(300.0, 400.0, 0.9, 1.0) < 0.0, "a cold body takes heat in");
    }

    @Test
    void exchange_neverCarriesPastTheShareTemperature() {
        final double flow = HeatMath.exchange(1_000.0, 100.0, 300.0, 100.0, 1e9, 1.0);
        assertEquals(35_000.0, flow, 1e-6);
        assertEquals(-35_000.0, HeatMath.exchange(300.0, 100.0, 1_000.0, 100.0, 1e9, 1.0), 1e-6);
    }

    @Test
    void exchange_isConductanceTimesDifferenceTimesTime() {
        assertEquals(20.0, HeatMath.exchange(310.0, 1e6, 300.0, 1e6, 40.0, 0.05), 1e-9);
    }

    @Test
    void loss_stopsAtTheAmbient() {
        assertEquals(50.0, HeatMath.loss(310.0, 1e6, 300.0, 1_000.0, 0.05), 1e-9);
        assertEquals(100.0, HeatMath.loss(301.0, 100.0, 300.0, 1e9, 1.0), 1e-9);
        assertEquals(-100.0, HeatMath.loss(299.0, 100.0, 300.0, -1e9, 1.0), 1e-9);
    }

    @Test
    void kelvin_andCelsiusAreTheSameScaleShifted() {
        assertEquals(288.15, HeatMath.kelvin(15.0), 1e-9);
        assertEquals(-273.15, HeatMath.celsius(0.0), 1e-9);
    }
}
