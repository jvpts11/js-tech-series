/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlastMathTest {

    @Test
    void tnt_isTheEnergyOverAKilogramsWorth() {
        assertEquals(1.0, BlastMath.tnt(4.184e6), 1.0e-12);
        assertEquals(1_000.0, BlastMath.tnt(4.184e9), 1.0e-9);
    }

    @Test
    void burstEnergy_isWhatTheGasHeldAboveTheAirAround() {
        // A cubic metre at two hundred atmospheres: 199 x 101 325 Pa / 0.4, about fifty megajoules, twelve kilograms.
        assertEquals(50.41e6, BlastMath.burstEnergy(200, 1), 0.01e6);
        assertEquals(12.05, BlastMath.tnt(BlastMath.burstEnergy(200, 1)), 0.01);
        assertEquals(2 * BlastMath.burstEnergy(51, 1), BlastMath.burstEnergy(101, 1), 1.0e-6);
        assertEquals(0.0, BlastMath.burstEnergy(1, 1));
        assertEquals(0.0, BlastMath.burstEnergy(0.5, 1));
        assertEquals(0.0, BlastMath.burstEnergy(200, 0));
    }

    @Test
    void overpressure_followsTheKinneyGrahamFit() {
        // A kilogram of TNT: about a thousand kilopascals at a metre, ten at ten metres.
        assertEquals(1_008, BlastMath.overpressure(1, 1), 10);
        assertEquals(10.0, BlastMath.overpressure(1, 10), 0.5);
    }

    @Test
    void overpressure_scalesWithTheCubeRootOfTheEnergy() {
        // A thousand times the TNT gives the same pressure ten times as far.
        assertEquals(BlastMath.overpressure(1, 3), BlastMath.overpressure(1_000, 30), 1.0e-9);
    }

    @Test
    void overpressure_fallsWithDistance() {
        double before = Double.MAX_VALUE;
        for (int metres = 1; metres <= 200; metres++) {
            final double now = BlastMath.overpressure(100, metres);
            assertTrue(now < before, "falls at " + metres);
            before = now;
        }
    }

    @Test
    void reach_isWhereThePressureFallsTo() {
        final double reach = BlastMath.reach(1, 10);
        assertEquals(10.0, reach, 0.3);
        assertEquals(10, BlastMath.overpressure(1, reach), 0.05);
        assertEquals(0, BlastMath.reach(1, 1.0e9), 1.0e-12);
    }

    @Test
    void arrival_isTheDistanceOverTheSpeedOfSound() {
        assertEquals(20, BlastMath.arrival(343, BlastMath.SOUND_IN_AIR));
        assertEquals(0, BlastMath.arrival(10, BlastMath.SOUND_IN_AIR));
        assertEquals(1, BlastMath.arrival(100, BlastMath.SOUND_IN_WATER));
    }

    @Test
    void harm_startsPastTheHarmlessAndThrowsLimited() {
        assertEquals(0, BlastMath.harm(BlastMath.HARMLESS), 1.0e-12);
        assertEquals(18, BlastMath.harm(100), 1.0e-9);
        assertEquals(0, BlastMath.thrown(5), 1.0e-12);
        assertEquals(3, BlastMath.thrown(10_000), 1.0e-12);
    }

    @Test
    void standsFromResistance_ordersFlowersPlanksStone() {
        assertEquals(BlastMath.LEAST_STANDING, BlastMath.standsFromResistance(0), 1.0e-12);
        assertTrue(BlastMath.standsFromResistance(3) > 50 && BlastMath.standsFromResistance(3) < 120);
        assertTrue(BlastMath.standsFromResistance(6) > 150 && BlastMath.standsFromResistance(6) < 250);
        assertTrue(BlastMath.standsFromResistance(1_200) > 10_000);
    }

    @Test
    void fireball_growsSlowerThanTheCharge() {
        assertEquals(1.93, BlastMath.fireball(1), 1.0e-9);
        assertTrue(BlastMath.fireball(1_000) < 20 && BlastMath.fireball(1_000) > 15);
        assertEquals(0, BlastMath.fireball(0), 1.0e-12);
    }

    @Test
    void fragmentSpeed_followsGurney() {
        // A casing as heavy as its charge flies at 2440 / sqrt(1.5), about two kilometres a second.
        assertEquals(1_992, BlastMath.fragmentSpeed(1, 1), 1);
        assertTrue(BlastMath.fragmentSpeed(10, 1) < BlastMath.fragmentSpeed(1, 1));
        assertEquals(0, BlastMath.fragmentSpeed(1, 0), 1.0e-12);
    }

    @Test
    void slowing_isFarthestForDenseFragmentsAndNeverInAVacuum() {
        final double steel = BlastMath.slowing(0.002, 7_850, 1.2);
        final double aluminium = BlastMath.slowing(0.002, 2_700, 1.2);
        assertEquals(83, steel, 2);
        assertTrue(aluminium < steel);
        assertTrue(BlastMath.slowing(0.002, 7_850, 1_000) < 1);
        assertEquals(Double.POSITIVE_INFINITY, BlastMath.slowing(0.002, 7_850, 0));
    }

    @Test
    void fragmentReach_isWhereTheEnergyFallsToAHalfHeart() {
        final double slowing = BlastMath.slowing(0.002, 7_850, 1.2);
        final double reach = BlastMath.fragmentReach(0.002, 1_500, slowing);
        final double left = 1_500 * Math.exp(-reach / slowing);
        assertEquals(BlastMath.JOULES_PER_HALF_HEART, BlastMath.kinetic(0.002, left), 1.0e-6);
        assertEquals(0, BlastMath.fragmentReach(0.002, 100, slowing), 1.0e-12);
        assertEquals(1, BlastMath.fragmentHarm(BlastMath.JOULES_PER_HALF_HEART), 1.0e-12);
    }
}
