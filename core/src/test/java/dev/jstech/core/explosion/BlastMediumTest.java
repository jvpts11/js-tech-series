/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlastMediumTest {

    @Test
    void air_atSeaLevelIsTheFitItself() {
        final BlastMedium air = BlastMedium.air(BlastMath.SEA_LEVEL, 15);
        assertEquals(340, air.speed(), 1);
        assertEquals(1.225, air.density(), 1.0e-3);
        assertEquals(BlastMath.overpressure(1, 5), air.overpressure(1, 5), 1.0e-9);
        assertEquals(BlastMath.reach(1, 10), air.reach(1, 10), 1.0e-9);
    }

    @Test
    void air_thinnerThanTheThinnestIsAVacuum() {
        assertSame(BlastMedium.VACUUM, BlastMedium.air(0.5, 20));
        assertFalse(BlastMedium.VACUUM.carriesWave());
        assertEquals(0, BlastMedium.VACUUM.overpressure(1_000, 1), 1.0e-12);
        assertEquals(0, BlastMedium.VACUUM.reach(1_000, 1), 1.0e-12);
        assertEquals(Integer.MAX_VALUE, BlastMedium.VACUUM.arrival(10));
    }

    @Test
    void air_thinCarriesAWeakerWave() {
        final BlastMedium thin = BlastMedium.air(BlastMath.SEA_LEVEL / 10, 15);
        final BlastMedium sea = BlastMedium.air(BlastMath.SEA_LEVEL, 15);
        assertTrue(thin.overpressure(1, 5) < sea.overpressure(1, 5));
        assertTrue(thin.reach(1, BlastMath.HARMLESS) < sea.reach(1, BlastMath.HARMLESS));
        assertEquals(sea.speed(), thin.speed(), 1.0e-9);
    }

    @Test
    void air_colderCarriesSoundSlower() {
        final double cold = BlastMedium.air(BlastMath.SEA_LEVEL, -40).speed();
        assertTrue(cold < BlastMedium.air(BlastMath.SEA_LEVEL, 40).speed());
    }

    @Test
    void water_carriesTheWaveFartherAndFaster() {
        final BlastMedium sea = BlastMedium.air(BlastMath.SEA_LEVEL, 15);
        assertEquals(BlastMedium.WATER_CARRY * sea.reach(1, 50) / sea.carry(), BlastMedium.WATER.reach(1, 50), 1.0e-6);
        assertTrue(BlastMedium.WATER.arrival(100) < sea.arrival(100));
    }

    @Test
    void paced_keepsTheWaveButNotAVacuumsLack() {
        final BlastMedium slow = BlastMedium.air(BlastMath.SEA_LEVEL, 15).paced(2);
        assertEquals(2, slow.speed(), 1.0e-12);
        assertEquals(40, slow.arrival(4));
        assertSame(BlastMedium.VACUUM, BlastMedium.VACUUM.paced(2));
    }

    @Test
    void constructor_refusesWhatIsNoMedium() {
        assertThrows(IllegalArgumentException.class, () -> new BlastMedium(-1, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new BlastMedium(1, 1, 0, 1));
    }
}
