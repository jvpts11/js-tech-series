/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LastSoundToggleTest {

    private AudioPrefs prefs;
    private LastSoundToggle toggle;

    @BeforeEach
    void setUp() {
        prefs = new AudioPrefs();
        toggle = new LastSoundToggle(5_000L);
    }

    @Test
    void press_turnsOffTheLastSoundHeard() {
        final LastSoundToggle.Outcome outcome = toggle.press("jsc:computer/fan", 1_000L, prefs);
        assertEquals(new LastSoundToggle.Outcome(LastSoundToggle.Kind.TURNED_OFF, "jsc:computer/fan"), outcome);
        assertTrue(prefs.isMuted("jsc:computer/fan"));
    }

    @Test
    void press_againSoonAfterBringsItBack() {
        toggle.press("jsc:computer/fan", 1_000L, prefs);
        final LastSoundToggle.Outcome outcome = toggle.press("minecraft:entity.zombie.ambient", 4_000L, prefs);
        assertEquals(new LastSoundToggle.Outcome(LastSoundToggle.Kind.BACK_ON, "jsc:computer/fan"), outcome);
        assertFalse(prefs.isMuted("jsc:computer/fan"));
        assertFalse(prefs.isMuted("minecraft:entity.zombie.ambient"));
    }

    @Test
    void press_againLaterTurnsOffTheNextOne() {
        toggle.press("jsc:computer/fan", 1_000L, prefs);
        final LastSoundToggle.Outcome outcome = toggle.press("jsc:drive/seek", 7_000L, prefs);
        assertEquals(LastSoundToggle.Kind.TURNED_OFF, outcome.kind());
        assertTrue(prefs.isMuted("jsc:computer/fan"));
        assertTrue(prefs.isMuted("jsc:drive/seek"));
    }

    @Test
    void press_bringsBackOnlyOnce() {
        toggle.press("jsc:computer/fan", 1_000L, prefs);
        toggle.press("jsc:computer/fan", 2_000L, prefs);
        final LastSoundToggle.Outcome third = toggle.press("jsc:computer/fan", 3_000L, prefs);
        assertEquals(LastSoundToggle.Kind.TURNED_OFF, third.kind());
        assertTrue(prefs.isMuted("jsc:computer/fan"));
    }

    @Test
    void press_withNothingHeardDoesNothing() {
        final LastSoundToggle.Outcome outcome = toggle.press(null, 1_000L, prefs);
        assertEquals(new LastSoundToggle.Outcome(LastSoundToggle.Kind.NOTHING, null), outcome);
        assertTrue(prefs.mutedSounds().isEmpty());
    }
}
