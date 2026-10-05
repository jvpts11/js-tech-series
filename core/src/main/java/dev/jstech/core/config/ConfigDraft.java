/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What a player changed on a settings screen and has not kept yet: each change held here, the file left as it was,
 * until {@link #apply()} hands them all to the file at once, and the file writes itself once. Leaving the screen
 * without keeping them drops them.
 *
 * <p>Every change is held to what its setting is held to, as the file holds it: a number pulled into its range, a
 * word kept to its list. Pure: no game types, so the screen's arithmetic is checked without the game.
 */
public final class ConfigDraft {

    private final ConfigFile file;
    /* The changes, by the setting's dotted path, in the order they were made. */
    private final Map<String, Object> pending = new LinkedHashMap<>();

    /** A step of a whole number, and of one with a fraction; a big step is ten of them. */
    private static final long WHOLE_STEP = 1L;
    private static final double FRACTION_STEP = 0.1;
    private static final int BIG = 10;

    public ConfigDraft(final ConfigFile file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /** How a setting is changed on a screen, by what it holds. */
    public enum Control {
        /** On or off: a switch. */
        TOGGLE,
        /** A number: typed, or stepped up and down. */
        NUMBER,
        /** One of a list of words: gone through one after the other. */
        CHOICE,
        /** A piece of text: typed. */
        TEXT,
        /** Anything else, a list or a map: shown, and changed in the file. */
        FIXED
    }

    public ConfigFile file() {
        return file;
    }

    /** How {@code key} is changed on a screen. */
    public static Control controlOf(final ConfigKey<?> key) {
        final Object kind = key.defaultValue();
        if (kind instanceof Boolean) {
            return Control.TOGGLE;
        }
        if (kind instanceof Integer || kind instanceof Long || kind instanceof Double) {
            return Control.NUMBER;
        }
        if (kind instanceof String) {
            return key.allowed().isPresent() ? Control.CHOICE : Control.TEXT;
        }
        return Control.FIXED;
    }

    /** What {@code key} holds as the screen shows it: the change made to it, or the file's value. */
    @SuppressWarnings("unchecked")
    public <T> T value(final ConfigKey<T> key) {
        final Object changed = pending.get(key.dottedPath());
        return changed != null ? (T) changed : file.get(key);
    }

    /** Whether {@code key} holds a change not kept yet. */
    public boolean isChanged(final ConfigKey<?> key) {
        return pending.containsKey(key.dottedPath());
    }

    /** The settings changed and not kept yet, by dotted path. */
    public Set<String> changed() {
        return Set.copyOf(pending.keySet());
    }

    /** Turns a setting that is on or off the other way. */
    public void toggle(final ConfigKey<Boolean> key) {
        put(key, !value(key));
    }

    /**
     * Steps a number up ({@code direction} 1) or down (-1), by one, or by ten when {@code big}; a number with a
     * fraction steps by a tenth. It stops at the ends of its range.
     */
    @SuppressWarnings("unchecked")
    public void step(final ConfigKey<?> key, final int direction, final boolean big) {
        final Object now = value(key);
        final int times = Integer.signum(direction) * (big ? BIG : 1);
        if (now instanceof Integer whole) {
            put((ConfigKey<Integer>) key, (int) clampWhole(key, Math.max(Integer.MIN_VALUE,
                    Math.min(Integer.MAX_VALUE, (long) whole + WHOLE_STEP * times))));
        } else if (now instanceof Long whole) {
            put((ConfigKey<Long>) key, clampWhole(key, saturatedAdd(whole, WHOLE_STEP * times)));
        } else if (now instanceof Double fraction) {
            put((ConfigKey<Double>) key, clampFraction(key,
                    Math.round((fraction + FRACTION_STEP * times) * 1000.0) / 1000.0));
        }
    }

    /** Goes to the next word of a setting's list ({@code direction} 1) or the one before (-1), round the ends. */
    public void cycle(final ConfigKey<String> key, final int direction) {
        final List<String> words = key.allowed().orElse(List.of());
        if (words.isEmpty()) {
            return;
        }
        final int at = Math.max(0, words.indexOf(value(key)));
        put(key, words.get(Math.floorMod(at + Integer.signum(direction), words.size())));
    }

    /**
     * Takes what a player typed for {@code key}: a number read as its kind and held to its range, a word as it is.
     *
     * @return whether it could be read as the setting's value; a change that cannot is not made
     */
    @SuppressWarnings("unchecked")
    public boolean typed(final ConfigKey<?> key, final String text) {
        final Object kind = key.defaultValue();
        final String trimmed = text.trim();
        try {
            if (kind instanceof Integer) {
                put((ConfigKey<Integer>) key, (int) clampWhole(key, Integer.parseInt(trimmed)));
            } else if (kind instanceof Long) {
                put((ConfigKey<Long>) key, clampWhole(key, Long.parseLong(trimmed)));
            } else if (kind instanceof Double) {
                final double read = Double.parseDouble(trimmed);
                if (!Double.isFinite(read)) {
                    return false;
                }
                put((ConfigKey<Double>) key, clampFraction(key, read));
            } else if (kind instanceof String) {
                if (key.allowed().isPresent() && !key.allowed().get().contains(text)) {
                    return false;
                }
                put((ConfigKey<String>) key, text);
            } else {
                return false;
            }
            return true;
        } catch (final NumberFormatException notANumber) {
            return false;
        }
    }

    /** Puts every setting back to its default, as changes to keep or drop like any other. */
    public void defaults() {
        for (final ConfigKey<?> key : file.keys()) {
            putDefault(key);
        }
    }

    /** Hands every change to the file, which writes itself once, and forgets them. */
    public void apply() {
        if (pending.isEmpty()) {
            return;
        }
        for (final ConfigKey<?> key : file.keys()) {
            stage(key);
        }
        file.save();
        pending.clear();
    }

    /* Holds the change, held to what the setting is held to; a change back to the file's value is no change. */
    private <T> void put(final ConfigKey<T> key, final T value) {
        final T held = file.validated(key, value);
        if (Objects.equals(held, file.get(key))) {
            pending.remove(key.dottedPath());
        } else {
            pending.put(key.dottedPath(), held);
        }
    }

    private <T> void putDefault(final ConfigKey<T> key) {
        put(key, key.defaultValue());
    }

    @SuppressWarnings("unchecked")
    private <T> void stage(final ConfigKey<T> key) {
        final Object changed = pending.get(key.dottedPath());
        if (changed != null) {
            file.stage(key, (T) changed);
        }
    }

    /* A whole number pulled into its setting's range, so a step past an end stops at it without a word in the log. */
    private static long clampWhole(final ConfigKey<?> key, final long value) {
        return key.range().map(range -> Math.max(range.min().longValue(), Math.min(range.max().longValue(), value)))
                .orElse(value);
    }

    private static double clampFraction(final ConfigKey<?> key, final double value) {
        return key.range().map(range -> Math.max(range.min().doubleValue(),
                Math.min(range.max().doubleValue(), value))).orElse(value);
    }

    private static long saturatedAdd(final long a, final long b) {
        final long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? (a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE) : sum;
    }
}
