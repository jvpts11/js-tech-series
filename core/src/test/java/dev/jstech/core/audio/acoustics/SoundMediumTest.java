/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoundMediumTest {

    @Test
    void of_isAVacuumUnderTheLeastPressure() {
        assertEquals(SoundMedium.Kind.VACUUM, SoundMedium.of(0.01, 20, false).kind());
        assertEquals(SoundMedium.Kind.AIR, SoundMedium.of(0.6, -60, false).kind());
        assertEquals(SoundMedium.Kind.WATER, SoundMedium.of(0, 20, true).kind());
    }

    @Test
    void vacuum_carriesOnlyWhatIsTouched() {
        final SoundMedium vacuum = SoundMedium.of(0, 20, false);
        assertFalse(vacuum.carries());
        assertEquals(0, vacuum.gain(8));
        assertTrue(vacuum.gain(1) > 0 && vacuum.gain(1) < 1, "felt through the body, softly");
        assertTrue(vacuum.highs(1) < 0.5, "and dull");
        assertEquals(0, vacuum.speed());
    }

    @Test
    void thinAir_carriesAsLoudAsTheSquareRootOfItsPressure() {
        assertEquals(1, SoundMedium.SEA_LEVEL.gain(30), 1e-9);
        final SoundMedium mars = SoundMedium.of(0.6, -60, false);
        assertEquals(Math.sqrt(0.6 / 101.325), mars.gain(30), 1e-9);
        assertEquals(1, SoundMedium.of(300, 20, false).gain(30), "thick air is no louder than the sea level's");
    }

    @Test
    void water_mufflesAndCarriesFaster() {
        final SoundMedium water = SoundMedium.of(101.325, 15, true);
        assertTrue(water.highs(10) < 0.2);
        assertEquals(1481, water.speed());
    }

    @Test
    void speed_followsTheAirsTemperature() {
        assertEquals(331.3, SoundMedium.of(101.325, 0, false).speed(), 1e-9);
        assertEquals(340.3, SoundMedium.SEA_LEVEL.speed(), 0.1);
        assertTrue(SoundMedium.of(0.6, -60, false).speed() < 300, "cold Martian air is slow");
    }
}
