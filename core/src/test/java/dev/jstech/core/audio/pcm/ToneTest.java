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

class ToneTest {

    @Test
    void constructor_keepsTheVolumeBetweenNothingAndFull() {
        assertEquals(1.0F, new Tone(Waveform.SINE, 440, 10, 3.0F).volume());
        assertEquals(0.0F, new Tone(Waveform.SINE, 440, 10, -1.0F).volume());
    }

    @Test
    void constructor_refusesWhatNoSpeakerPlays() {
        assertThrows(IllegalArgumentException.class, () -> new Tone(Waveform.SINE, -1, 10, 1.0F));
        assertThrows(IllegalArgumentException.class, () -> new Tone(Waveform.SINE, 30_000, 10, 1.0F));
        assertThrows(IllegalArgumentException.class, () -> new Tone(Waveform.SINE, 440, 0, 1.0F));
        assertThrows(IllegalArgumentException.class, () -> new Tone(Waveform.SINE, 440, 60_001, 1.0F));
    }

    @Test
    void beep_isASquareAtHalfVolume() {
        final Tone beep = Tone.beep(750, 200);
        assertEquals(Waveform.SQUARE, beep.wave());
        assertEquals(750, beep.frequency());
        assertEquals(200, beep.millis());
        assertEquals(0.5F, beep.volume());
    }

    @Test
    void rest_isSilent() {
        final Tone rest = Tone.rest(80);
        assertEquals(0, rest.frequency());
        assertEquals(0.0F, rest.volume());
        assertEquals(80, rest.millis());
    }
}
