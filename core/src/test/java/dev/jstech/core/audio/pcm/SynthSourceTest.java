/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SynthSourceTest {

    private static final int RATE = PcmFormat.SYNTH.sampleRate();

    @Test
    void read_makesAsManySamplesAsTheNotesLast() {
        final SynthSource source = new SynthSource(List.of(Tone.beep(440, 100), Tone.rest(50)));
        final int expected = RATE / 10 + RATE / 20;
        assertEquals(expected, source.totalSamples());
        assertEquals(expected, readAll(source).length);
        assertEquals(-1, source.read(new short[16], 0, 16));
    }

    @Test
    void read_isSilentInARest() {
        for (final short sample : readAll(new SynthSource(List.of(Tone.rest(20))))) {
            assertEquals(0, sample);
        }
    }

    @Test
    void read_risesAndFallsAtTheEdgesOfANote() {
        final short[] samples = readAll(new SynthSource(List.of(Tone.beep(440, 100))));
        assertEquals(0, samples[0]);
        assertEquals(0, samples[samples.length - 1]);
        int loudest = 0;
        for (final short sample : samples) {
            loudest = Math.max(loudest, Math.abs(sample));
        }
        assertEquals(Short.MAX_VALUE / 2, loudest, 2);
    }

    @Test
    void read_squareChangesSignAtItsPitch() {
        final short[] samples = readAll(new SynthSource(List.of(new Tone(Waveform.SQUARE, 441, 1000, 1.0F))));
        int changes = 0;
        for (int i = 1; i < samples.length; i++) {
            if (samples[i - 1] > 0 && samples[i] < 0 || samples[i - 1] < 0 && samples[i] > 0) {
                changes++;
            }
        }
        assertEquals(2 * 441, changes, 3);
    }

    @Test
    void read_noiseSoundsTheSameEveryTime() {
        final List<Tone> tones = List.of(new Tone(Waveform.NOISE, 1000, 30, 0.8F));
        assertArrayEquals(readAll(new SynthSource(tones)), readAll(new SynthSource(tones)));
    }

    @Test
    void read_givesTheWholeSequenceHoweverItIsAskedFor() {
        final List<Tone> tones = List.of(Tone.beep(220, 40), new Tone(Waveform.TRIANGLE, 330, 30, 0.7F));
        final SynthSource source = new SynthSource(tones);
        final short[] whole = new short[(int) source.totalSamples()];
        assertEquals(whole.length, source.read(whole, 0, whole.length));
        assertArrayEquals(whole, readAll(new SynthSource(tones)));
    }

    @Test
    void constructor_refusesNoNotes() {
        assertThrows(IllegalArgumentException.class, () -> new SynthSource(List.of()));
    }

    private static short[] readAll(final SynthSource source) {
        final short[] out = new short[(int) source.totalSamples()];
        final short[] chunk = new short[97];
        int at = 0;
        int read = source.read(chunk, 0, chunk.length);
        while (read > 0) {
            System.arraycopy(chunk, 0, out, at, read);
            at += read;
            read = source.read(chunk, 0, chunk.length);
        }
        assertEquals(out.length, at);
        return out;
    }
}
