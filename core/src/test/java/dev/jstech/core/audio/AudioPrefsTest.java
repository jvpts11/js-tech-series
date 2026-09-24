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

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AudioPrefsTest {

    @Test
    void volume_isFullUntilSaidAndHeldBetweenNothingAndAll() {
        final AudioPrefs prefs = new AudioPrefs();
        assertEquals(1.0F, prefs.volume("jscore:machines"));
        prefs.setVolume("jscore:machines", 0.25F);
        assertEquals(0.25F, prefs.volume("jscore:machines"));
        prefs.setVolume("jscore:alerts", 3.0F);
        assertEquals(1.0F, prefs.volume("jscore:alerts"));
        prefs.setVolume("jscore:alerts", -1.0F);
        assertEquals(0.0F, prefs.volume("jscore:alerts"));
        prefs.setVolume("jscore:music", Float.NaN);
        assertEquals(1.0F, prefs.volume("jscore:music"));
    }

    @Test
    void volumes_keepsOnlyTheChannelsTurnedDown() {
        final AudioPrefs prefs = new AudioPrefs();
        prefs.setVolume("jscore:machines", 0.5F);
        prefs.setVolume("jscore:alerts", 1.0F);
        assertEquals(Map.of("jscore:machines", 0.5F), prefs.volumes());
    }

    @Test
    void setMuted_turnsAnySoundOffAndOnAgain() {
        final AudioPrefs prefs = new AudioPrefs();
        prefs.setMuted("minecraft:block.note_block.bell", true);
        prefs.setMuted("jsc:computer/fan", true);
        assertTrue(prefs.isMuted("minecraft:block.note_block.bell"));
        assertEquals(Set.of("jsc:computer/fan", "minecraft:block.note_block.bell"), prefs.mutedSounds());
        prefs.setMuted("jsc:computer/fan", false);
        assertFalse(prefs.isMuted("jsc:computer/fan"));
    }

    @Test
    void flags_startWithWallsMufflingAlertsLoweringTheRestAndNoVisualCues() {
        final AudioPrefs prefs = new AudioPrefs();
        assertTrue(prefs.occlusion());
        assertTrue(prefs.ducking());
        assertFalse(prefs.visualCues());
        prefs.setDucking(false);
        assertFalse(prefs.ducking());
    }
}
