/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.audio.pcm.Tone;
import dev.jstech.core.audio.pcm.Waveform;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The tunes a program writes for its computer to play, read into notes: one item after another, each a note by its
 * name ({@code C4}, {@code F#3}, {@code Bb5}), a pitch in hertz ({@code 440}) or a rest ({@code R}), several played
 * together joined by {@code +} ({@code C4+E4+G4}), and how long it lasts in milliseconds after a colon
 * ({@code C4:250}); an item that does not say lasts as long as the one before, the first a quarter of a second.
 *
 * <p>Notes played together each take a voice of their own, so a tune is read into as many parts as its widest chord
 * has notes, each part a line of notes and rests that plays alongside the others.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class ToneNotation {

    /** How long an item lasts when none before it said. */
    public static final int DEFAULT_MILLIS = 250;
    /** The most items one tune holds. */
    public static final int MAX_ITEMS = 1024;
    /** The most notes played together. */
    public static final int MAX_CHORD = 16;
    /** How loud each note plays, before the machine's own volume. */
    private static final float VOLUME = 0.5F;
    private static final String NOTES = "C D EF G A B";

    private ToneNotation() {
    }

    /**
     * The tune's parts, each played by one voice, its notes in that shape.
     *
     * @throws IllegalArgumentException when an item is not one, naming it
     */
    public static List<List<Tone>> parts(final String tune, final Waveform wave) {
        final List<List<Double>> items = new ArrayList<>();
        final List<Integer> lengths = new ArrayList<>();
        int millis = DEFAULT_MILLIS;
        for (final String item : tune.trim().split("[\\s,]+")) {
            if (item.isEmpty()) {
                continue;
            }
            if (items.size() >= MAX_ITEMS) {
                throw new IllegalArgumentException(tune.length() > 40 ? tune.substring(0, 40) : tune);
            }
            final int colon = item.indexOf(':');
            if (colon >= 0) {
                millis = lengthOf(item.substring(colon + 1), item);
            }
            final String[] pitches = (colon >= 0 ? item.substring(0, colon) : item).split("\\+");
            if (pitches.length > MAX_CHORD) {
                throw new IllegalArgumentException(item);
            }
            final List<Double> chord = new ArrayList<>(pitches.length);
            for (final String pitch : pitches) {
                chord.add(frequencyOf(pitch, item));
            }
            items.add(chord);
            lengths.add(millis);
        }
        int width = 0;
        for (final List<Double> chord : items) {
            width = Math.max(width, chord.size());
        }
        final List<List<Tone>> parts = new ArrayList<>(width);
        for (int voice = 0; voice < width; voice++) {
            final List<Tone> part = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                final List<Double> chord = items.get(i);
                final double frequency = voice < chord.size() ? chord.get(voice) : 0.0;
                part.add(frequency <= 0.0 ? Tone.rest(lengths.get(i))
                        : new Tone(wave, frequency, lengths.get(i), VOLUME));
            }
            parts.add(part);
        }
        return parts;
    }

    /** How long a tune lasts, in milliseconds, from its parts. */
    public static long millisOf(final List<List<Tone>> parts) {
        long longest = 0L;
        for (final List<Tone> part : parts) {
            long total = 0L;
            for (final Tone tone : part) {
                total += tone.millis();
            }
            longest = Math.max(longest, total);
        }
        return longest;
    }

    /* A pitch in hertz, or 0 for a rest. */
    private static double frequencyOf(final String pitch, final String item) {
        final String written = pitch.trim().toUpperCase(Locale.ROOT);
        if (written.equals("R")) {
            return 0.0;
        }
        if (!written.isEmpty() && Character.isDigit(written.charAt(0))) {
            try {
                final double hertz = Double.parseDouble(written);
                if (hertz > 0.0 && hertz <= 20_000.0) {
                    return hertz;
                }
            } catch (final NumberFormatException notANumber) {
                // Told below, with the item it came in.
            }
            throw new IllegalArgumentException(item);
        }
        final int step = written.isEmpty() || written.charAt(0) == ' ' ? -1 : NOTES.indexOf(written.charAt(0));
        if (step < 0) {
            throw new IllegalArgumentException(item);
        }
        int at = 1;
        int semitone = step;
        // A sharp raises it a semitone and a flat, written b, lowers it; upper-cased, a flat reads as a B.
        final char accidental = at < written.length() ? written.charAt(at) : ' ';
        if (accidental == '#') {
            semitone++;
            at++;
        } else if (accidental == 'B') {
            semitone--;
            at++;
        }
        if (at >= written.length()) {
            throw new IllegalArgumentException(item);
        }
        final int octave;
        try {
            octave = Integer.parseInt(written.substring(at));
        } catch (final NumberFormatException notAnOctave) {
            throw new IllegalArgumentException(item);
        }
        if (octave < 0 || octave > 8) {
            throw new IllegalArgumentException(item);
        }
        // A4 is 440 Hz, and every semitone a twelfth of an octave from it.
        final int midi = 12 * (octave + 1) + semitone;
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    private static int lengthOf(final String written, final String item) {
        try {
            final int millis = Integer.parseInt(written.trim());
            if (millis >= 1 && millis <= 60_000) {
                return millis;
            }
        } catch (final NumberFormatException notANumber) {
            // Told below, with the item it came in.
        }
        throw new IllegalArgumentException(item);
    }
}
