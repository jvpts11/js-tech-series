/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundOutputTest {

    @Test
    void both_playsOutOfEverythingThereIs() {
        assertTrue(SoundOutput.BOTH.monitorsPlay(true, true));
        assertTrue(SoundOutput.BOTH.speakersPlay(true, true));
    }

    @Test
    void monitor_leavesTheSpeakersOutWhileThereIsAMonitor() {
        assertTrue(SoundOutput.MONITOR.monitorsPlay(true, true));
        assertFalse(SoundOutput.MONITOR.speakersPlay(true, true));
    }

    @Test
    void speakers_leaveTheMonitorsOutWhileThereIsASpeaker() {
        assertFalse(SoundOutput.SPEAKERS.monitorsPlay(true, true));
        assertTrue(SoundOutput.SPEAKERS.speakersPlay(true, true));
    }

    @Test
    void choiceWithNothingToPlayIt_givesWayToWhatIsLinked() {
        assertTrue(SoundOutput.SPEAKERS.monitorsPlay(true, false), "no speakers: the monitor plays");
        assertTrue(SoundOutput.MONITOR.speakersPlay(false, true), "no monitor: the speakers play");
    }

    @Test
    void nothingLinked_playsNothing() {
        for (final SoundOutput output : SoundOutput.values()) {
            assertFalse(output.monitorsPlay(false, false));
            assertFalse(output.speakersPlay(false, false));
        }
    }

    @Test
    void byId_readsTheNameWhateverItsCaseAndRefusesOthers() {
        assertEquals(SoundOutput.SPEAKERS, SoundOutput.byId(" Speakers "));
        assertEquals(SoundOutput.MONITOR, SoundOutput.byId("monitor"));
        assertNull(SoundOutput.byId("headphones"));
        assertNull(SoundOutput.byId(null));
    }
}
