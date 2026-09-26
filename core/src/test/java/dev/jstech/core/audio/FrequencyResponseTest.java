/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequencyResponseTest {

    @Test
    void full_takesNothingAway() {
        assertTrue(FrequencyResponse.FULL.full());
    }

    @Test
    void full_anyLimitIsNotFull() {
        assertFalse(new FrequencyResponse(22_050, 0, 0, 0).full());
        assertFalse(new FrequencyResponse(0, 8, 0, 0).full());
        assertFalse(new FrequencyResponse(0, 0, 150, 0).full());
        assertFalse(new FrequencyResponse(0, 0, 0, 7_000).full());
    }

    @Test
    void constructor_refusesANegativeFrequency() {
        assertThrows(IllegalArgumentException.class, () -> new FrequencyResponse(0, 0, -1, 0));
    }

    @Test
    void constructor_refusesMoreBitsThanASampleHas() {
        assertThrows(IllegalArgumentException.class, () -> new FrequencyResponse(0, 17, 0, 0));
    }

    @Test
    void constructor_refusesALowCutAboveTheHighCut() {
        assertThrows(IllegalArgumentException.class, () -> new FrequencyResponse(0, 0, 8_000, 7_000));
    }

    @Test
    void through_keepsTheTighterOfEachLimit() {
        final FrequencyResponse card = new FrequencyResponse(22_050, 8, 0, 0);
        final FrequencyResponse speaker = new FrequencyResponse(44_100, 0, 150, 7_000);
        assertEquals(new FrequencyResponse(22_050, 8, 150, 7_000), card.through(speaker));
    }

    @Test
    void through_fullHardwareChangesNothing() {
        final FrequencyResponse speaker = new FrequencyResponse(22_050, 0, 150, 7_000);
        assertEquals(speaker, FrequencyResponse.FULL.through(speaker));
        assertEquals(speaker, speaker.through(FrequencyResponse.FULL));
    }

    @Test
    void through_dropsALowCutThatWouldReachTheHighCut() {
        final FrequencyResponse bassless = new FrequencyResponse(0, 0, 900, 0);
        final FrequencyResponse muffled = new FrequencyResponse(0, 0, 0, 800);
        assertEquals(new FrequencyResponse(0, 0, 0, 800), bassless.through(muffled));
    }

    @Test
    void stereoSide_numberNoSideHasReadsAsBoth() {
        assertEquals(StereoSide.BOTH, StereoSide.byId(99));
        assertEquals(StereoSide.RIGHT, StereoSide.byId(StereoSide.RIGHT.id()));
    }
}
