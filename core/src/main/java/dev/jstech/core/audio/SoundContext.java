/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;

/**
 * What a sound is chosen by, besides what happened: the era of the machine it comes from, the device that plays it,
 * the family of the system running on it. A cue picks its sound from these ({@link SoundSet}), so one event (a
 * computer powering on) sounds as its machine would.
 *
 * <p>The dimensions are open: {@link #ERA}, {@link #DEVICE} and {@link #FAMILY} are the ones the series uses, and a
 * mod can add its own. Names and values are short, since a context travels with every cue the server sends.
 *
 * @param values each dimension's value, by dimension
 */
public record SoundContext(Map<String, String> values) {

    /** A context that says nothing, which picks each cue's fallback. */
    public static final SoundContext EMPTY = new SoundContext(Map.of());
    /** The hardware era of the machine a sound comes from, by its serialized name ({@code vintage}). */
    public static final String ERA = "era";
    /** The audio device that plays it, by its id ({@code jsc:pc_speaker}). */
    public static final String DEVICE = "device";
    /** The family of the system running on the machine ({@code unix}). */
    public static final String FAMILY = "family";
    /** The most dimensions one context has. */
    public static final int MAX_DIMENSIONS = 16;
    /** The longest a dimension's name or value is. */
    public static final int MAX_LENGTH = 64;

    public SoundContext {
        if (values.size() > MAX_DIMENSIONS) {
            throw new IllegalArgumentException("at most " + MAX_DIMENSIONS + " dimensions: " + values.size());
        }
        for (final Map.Entry<String, String> one : values.entrySet()) {
            if (one.getKey().isEmpty() || one.getKey().length() > MAX_LENGTH || one.getValue().length() > MAX_LENGTH) {
                throw new IllegalArgumentException("a dimension and its value are 1 to " + MAX_LENGTH
                        + " characters: " + one.getKey() + "=" + one.getValue());
            }
        }
        values = Map.copyOf(values);
    }

    /** The context with one dimension set, or replaced. */
    public SoundContext with(final String dimension, final String value) {
        final Map<String, String> out = new TreeMap<>(values);
        out.put(dimension, value);
        return new SoundContext(out);
    }

    /** This context with every dimension of {@code over} laid on top, {@code over} winning where both say. */
    public SoundContext and(final SoundContext over) {
        final Map<String, String> out = new TreeMap<>(values);
        out.putAll(over.values);
        return new SoundContext(out);
    }

    /** The value of that dimension, or null when the context does not say. */
    @Nullable
    public String get(final String dimension) {
        return values.get(dimension);
    }
}
