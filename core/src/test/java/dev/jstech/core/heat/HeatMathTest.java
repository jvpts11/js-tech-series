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

    /* What a mole of water takes to boil, in joules: 2.257 MJ a kilogram times 18.015 grams. */
    private static final double WATER_MOLAR_ENTHALPY = 2_257_000.0 * 0.018_015;

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

    @Test
    void boilingAt_givesWatersBoilingPointInTheOverworldsAir() {
        assertEquals(373.15, HeatMath.boilingAt(373.15, WATER_MOLAR_ENTHALPY, HeatMath.SEA_LEVEL_KPA), 1e-9);
    }

    @Test
    void boilingAt_isLowerInThinAirAndHigherInAPressureVessel() {
        assertEquals(344.2, HeatMath.boilingAt(373.15, WATER_MOLAR_ENTHALPY, 33.7), 0.5, "on top of Everest");
        assertEquals(268.2, HeatMath.boilingAt(373.15, WATER_MOLAR_ENTHALPY, 0.6), 0.5, "on Mars");
        assertEquals(452.1, HeatMath.boilingAt(373.15, WATER_MOLAR_ENTHALPY, 1_000.0), 0.5, "at ten bar");
    }

    @Test
    void boilingAt_ofAVacuumIsColdButNotNothing() {
        final double kelvin = HeatMath.boilingAt(373.15, WATER_MOLAR_ENTHALPY, 0.0);
        assertTrue(kelvin > 0.0 && kelvin < 200.0, "boils very cold: " + kelvin);
    }

    @Test
    void gasLitres_countsAMoleAs22Litres() {
        assertEquals(22.414, HeatMath.gasLitres(0.028_014, 0.028_014), 1e-9);
        assertEquals(1_244.2, HeatMath.gasLitres(1.0, 0.018_015), 0.1, "a kilogram of steam");
        assertEquals(0.0, HeatMath.gasLitres(1.0, 0.0), 1e-9);
    }
}
