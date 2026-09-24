/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PcmFormatTest {

    @Test
    void synth_isMonoAtTwentyTwoKilohertz() {
        assertEquals(22_050, PcmFormat.SYNTH.sampleRate());
        assertEquals(1, PcmFormat.SYNTH.channels());
    }

    @Test
    void constructor_refusesWhatTheSpeakersCannotTake() {
        assertThrows(IllegalArgumentException.class, () -> new PcmFormat(44_100, 6));
        assertThrows(IllegalArgumentException.class, () -> new PcmFormat(44_100, 0));
        assertThrows(IllegalArgumentException.class, () -> new PcmFormat(500, 1));
        assertThrows(IllegalArgumentException.class, () -> new PcmFormat(400_000, 2));
    }
}
