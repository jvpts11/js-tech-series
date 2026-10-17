/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.acoustics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoundTravelTest {

    @Test
    void delayTicks_isHowLongTheSoundTakesToArrive() {
        assertEquals(20, SoundTravel.delayTicks(343, 343));
        assertEquals(0, SoundTravel.delayTicks(16, 343), "too near to be heard as late");
        assertEquals(SoundTravel.LEAST_DELAY, SoundTravel.delayTicks(34.3, 343));
    }

    @Test
    void delayTicks_isNothingWhereNothingCarriesTheSound() {
        assertEquals(0, SoundTravel.delayTicks(500, 0));
        assertEquals(0, SoundTravel.delayTicks(0, 343));
    }

    @Test
    void delayTicks_isHeldToItsLongest() {
        assertEquals(SoundTravel.MOST_DELAY, SoundTravel.delayTicks(100_000, 343));
    }

    @Test
    void velocity_isTheMoveOverTheTime() {
        final SoundTravel.Motion motion = SoundTravel.velocity(1, 0, 0, 0.05);
        assertEquals(20, motion.x(), 1e-9);
        assertEquals(20, motion.speed(), 1e-9);
    }

    @Test
    void velocity_ofAJumpIsStandingStill() {
        assertEquals(SoundTravel.Motion.STILL, SoundTravel.velocity(50, 0, 0, 0.05));
        assertEquals(SoundTravel.Motion.STILL, SoundTravel.velocity(1, 0, 0, 0));
    }

    @Test
    void doppler_isHigherComingAndLowerGoing() {
        assertTrue(SoundTravel.doppler(30, 0, 343) > 1);
        assertTrue(SoundTravel.doppler(-30, 0, 343) < 1);
        assertEquals(1, SoundTravel.doppler(0, 0, 343), 1e-9);
        assertEquals(1, SoundTravel.doppler(30, 0, 0), "no medium, no change");
        assertTrue(Double.isFinite(SoundTravel.doppler(1_000, 0, 343)), "nothing outruns its own sound");
    }
}
