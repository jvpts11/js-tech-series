/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import java.util.Map;
import java.util.Objects;

/**
 * How a light's brightness moves in time, written as letters: {@code m} is its brightness as declared, {@code a} dark,
 * each letter a twelfth more or less, so {@code z} is about twice as bright. The pattern steps {@code rate} letters a
 * second and goes round again, or ends on its last letter, as a fluorescent tube that stutters as it starts and then
 * holds. Each light starts the pattern at its own point, so a row of lamps does not flicker as one. Pure.
 *
 * @param pattern the letters, {@code a} to {@code z}
 * @param rate    how many letters a second
 * @param loops   whether it goes round again, else it holds its last letter
 */
public record Flicker(String pattern, double rate, boolean loops) {

    /** Steady: always as bright as declared. */
    public static final Flicker STEADY = new Flicker("m", 1, true);
    /** A flame: a gentle wavering. */
    public static final Flicker CANDLE = new Flicker("mlmnmmlkmnmlmmnolmmkmlnm", 10, true);
    /** A fluorescent tube starting: a few stutters, then steady. */
    public static final Flicker STARTING = new Flicker("aaamaaaamaamaaammmamm", 10, false);
    /** A lamp about to fail: now and then it drops out. */
    public static final Flicker FAILING = new Flicker("mmmmmmmmmmmmmmmmmmmmmmamammmmmmmmmmmmmmmmmmmmmmmmmmmmaammam", 10,
            true);
    /** A slow beat, as a warning light breathes. */
    public static final Flicker PULSE = new Flicker("mnopqrstuvwxyzyxwvutsrqponmlkjihgfedcbabcdefghijkl", 20, true);
    /** A strobe: on and off. */
    public static final Flicker STROBE = new Flicker("ma", 8, true);
    /** The flickers a file names by a word. */
    public static final Map<String, Flicker> NAMED = Map.of("steady", STEADY, "candle", CANDLE, "starting",
            STARTING, "failing", FAILING, "pulse", PULSE, "strobe", STROBE);

    private static final char DARK = 'a';
    private static final double DECLARED = 12.0;

    public Flicker {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty() || !pattern.chars().allMatch(c -> c >= 'a' && c <= 'z')) {
            throw new IllegalArgumentException("a flicker is written in the letters a to z, not " + pattern);
        }
        if (!(rate > 0)) {
            throw new IllegalArgumentException("a flicker steps at a rate above 0, not " + rate);
        }
    }

    /** Its brightness at {@code seconds}, a share of the declared one, for the light {@code seed} starts it at. */
    public double brightness(final double seconds, final long seed) {
        final int length = this.pattern.length();
        final long step = (long) Math.floor(seconds * this.rate);
        final int at;
        if (this.loops) {
            at = (int) Math.floorMod(step + Math.floorMod(seed, length), length);
        } else {
            at = (int) Math.clamp(step, 0, length - 1);
        }
        return (this.pattern.charAt(at) - DARK) / DECLARED;
    }

    /** Whether it never moves, so nothing need follow it. */
    public boolean steady() {
        return this.pattern.chars().distinct().count() == 1;
    }
}
