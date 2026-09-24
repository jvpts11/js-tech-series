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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.audio.pcm.Waveform;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AudioDeviceTest {

    private static final TextKey NAME = new TextKey("test.device", "Test device");
    private static final List<Tone> TUNE = List.of(new Tone(Waveform.TRIANGLE, 440, 100, 0.7F), Tone.rest(50),
            new Tone(Waveform.NOISE, 1000, 30, 0.4F), Tone.beep(880, 100));

    @Test
    void adapt_playsEveryShapeItCannotMakeAsTheFirstItCan() {
        final AudioDevice speaker = new AudioDevice("x:speaker", NAME, Set.of(Waveform.SQUARE), false, 1);
        final List<Tone> played = speaker.adapt(TUNE);
        assertEquals(TUNE.size(), played.size());
        for (int i = 0; i < TUNE.size(); i++) {
            assertEquals(Waveform.SQUARE, played.get(i).wave());
            assertEquals(TUNE.get(i).frequency(), played.get(i).frequency());
            assertEquals(TUNE.get(i).millis(), played.get(i).millis());
            assertEquals(TUNE.get(i).volume(), played.get(i).volume());
        }
    }

    @Test
    void adapt_keepsTheShapesItCanMake() {
        final AudioDevice card = new AudioDevice("x:card", NAME, Set.of(Waveform.values()), true, 9);
        assertEquals(TUNE, card.adapt(TUNE));
    }

    @Test
    void adapt_playsNothingOnADeviceThatSynthesisesNothing() {
        assertTrue(new AudioDevice("x:dac", NAME, Set.of(), true, 2).adapt(TUNE).isEmpty());
        assertTrue(new AudioDevice("x:mute", NAME, Set.of(Waveform.SQUARE), false, 0).adapt(TUNE).isEmpty());
    }

    @Test
    void audible_needsAVoiceAndSomethingToPlay() {
        assertTrue(new AudioDevice("x:speaker", NAME, Set.of(Waveform.SQUARE), false, 1).audible());
        assertTrue(new AudioDevice("x:dac", NAME, Set.of(), true, 1).audible());
        assertFalse(new AudioDevice("x:none", NAME, Set.of(), false, 1).audible());
        assertFalse(new AudioDevice("x:mute", NAME, Set.of(Waveform.SQUARE), true, 0).audible());
    }

    @Test
    void constructor_refusesFewerThanNoVoices() {
        assertThrows(IllegalArgumentException.class, () -> new AudioDevice("x:y", NAME, Set.of(), false, -1));
    }
}
