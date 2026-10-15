/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.config.format.PlainValues;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a player changed on a settings screen and has not kept yet: each change held here, the file left as it was,
 * until {@link #apply()} hands them all to the file at once, and the file writes itself once. Leaving the screen
 * without keeping them drops them.
 *
 * <p>Every change is held to what its setting is held to, as the file holds it: a number pulled into its range, a
 * word kept to its list. Pure: no game types, so the screen's arithmetic is checked without the game.
 */
public final class ConfigDraft implements IConfigValues {

    private final ConfigFile file;
    /* The changes, by the setting's dotted path, in the order they were made. */
    private final Map<String, Object> pending = new LinkedHashMap<>();
    /* What the changes were before each step, for undo, and what an undo took back, for redo. */
    private final Deque<Map<String, Object>> undone = new ArrayDeque<>();
    private final Deque<Map<String, Object>> redone = new ArrayDeque<>();
    /* The setting a drag is moving, whose moves are one step; none between drags. */
    private String sliding;

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
        /** A number with no range: typed, or stepped up and down. */
        NUMBER,
        /** A number held to a range: slid along it, or typed beside it. */
        SLIDER,
        /** One of a list of words: picked from a drop-down list. */
        CHOICE,
        /** A piece of text: typed. */
        TEXT,
        /** Picked on a chooser of its own, which shows what each value means. */
        CHOOSE,
        /** A colour: picked on a colour picker, or typed as {@code #RRGGBB}. */
        COLOUR,
        /** One id of the game's items, blocks or fluids, picked from them. */
        ID,
        /** A list of ids of the game's items, blocks or fluids, each picked from them. */
        IDS,
        /** A list of plain values: entries added and taken away. */
        LIST,
        /** A map of names to plain values: entries added, changed and taken away. */
        MAP,
        /** Anything else, a record of its own: shown, and changed in the file. */
        FIXED
    }

    public ConfigFile file() {
        return file;
    }

    /** How {@code key} is changed on a screen. */
    public static Control controlOf(final ConfigKey<?> key) {
        if (key.chooser().isPresent()) {
            return Control.CHOOSE;
        }
        switch (key.traits().control()) {
            case COLOUR -> {
                return Control.COLOUR;
            }
            case ITEM, BLOCK, FLUID -> {
                return Control.ID;
            }
            case ITEMS, BLOCKS, FLUIDS -> {
                return Control.IDS;
            }
            case AUTO -> {
            }
        }
        final Object kind = key.defaultValue();
        if (kind instanceof Boolean) {
            return Control.TOGGLE;
        }
        if (kind instanceof Integer || kind instanceof Long || kind instanceof Double) {
            return key.range().isPresent() ? Control.SLIDER : Control.NUMBER;
        }
        if (kind instanceof String) {
            return key.allowed().isPresent() ? Control.CHOICE : Control.TEXT;
        }
        final Object plain = plainDefault(key);
        if (plain instanceof List<?> list && list.stream().allMatch(ConfigDraft::scalar)) {
            return Control.LIST;
        }
        if (plain instanceof Map<?, ?> map && map.values().stream().allMatch(ConfigDraft::scalar)) {
            return Control.MAP;
        }
        return Control.FIXED;
    }

    /**
     * What a player typed as one plain value: {@code true} or {@code false}, a whole number, a number with a fraction,
     * or else the text itself.
     */
    public static Object plainScalar(final String text) {
        final String trimmed = text.trim();
        if (trimmed.equals(String.valueOf(true)) || trimmed.equals(String.valueOf(false))) {
            return Boolean.parseBoolean(trimmed);
        }
        try {
            final long whole = Long.parseLong(trimmed);
            return whole == (int) whole ? (Object) (int) whole : (Object) whole;
        } catch (final NumberFormatException notWhole) {
            try {
                final double fraction = Double.parseDouble(trimmed);
                return Double.isFinite(fraction) ? fraction : text;
            } catch (final NumberFormatException notANumber) {
                return text;
            }
        }
    }

    /** What {@code key} holds as the screen shows it: the change made to it, or the file's value. */
    @SuppressWarnings("unchecked")
    public <T> T value(final ConfigKey<T> key) {
        final Object changed = pending.get(key.dottedPath());
        return changed != null ? (T) changed : file.get(key);
    }

    @Override
    public <T> T get(final ConfigKey<T> key) {
        return value(key);
    }

    /** Whether {@code key} is active with the changes in hand. */
    public boolean isActive(final ConfigKey<?> key) {
        return file.isActive(key, this);
    }

    /** Whether a settings screen shows {@code key} with the changes in hand. */
    public boolean isShown(final ConfigKey<?> key) {
        return file.isShown(key, this);
    }

    /** The rules between the file's settings the changes in hand break. */
    public List<ConfigRule> broken() {
        return file.broken(this);
    }

    /** How many of the changes not kept yet take hold only on a restart. */
    public int pendingOnRestart() {
        int count = 0;
        for (final String path : pending.keySet()) {
            if (file.key(path).map(key -> key.traits().applies() == ConfigApply.RESTART).orElse(false)) {
                count++;
            }
        }
        return count;
    }

    /** What {@code key} holds as the screen shows it, written as a file writes it. */
    public Object plainValue(final ConfigKey<?> key) {
        return plainNow(key);
    }

    /**
     * Takes {@code plain}, written as a file writes it, as {@code key}'s value, held to what it is held to, as one
     * step.
     *
     * @return whether it could be read as the setting's value; a change that cannot is not made
     */
    public <T> boolean putPlain(final ConfigKey<T> key, final Object plain) {
        final Optional<T> read = key.read(plain).result();
        read.ifPresent(value -> put(key, value));
        return read.isPresent();
    }

    /** The entries of a list setting as the screen shows them, written as a file writes them. */
    public List<Object> entries(final ConfigKey<?> key) {
        return plainNow(key) instanceof List<?> list ? List.copyOf(list) : List.of();
    }

    /**
     * Adds what a player typed or picked to the end of a list setting: read as a plain value first, and as the text
     * itself when the list does not take that.
     *
     * @return whether the list took it
     */
    public boolean addEntry(final ConfigKey<?> key, final String typed) {
        final List<Object> more = new ArrayList<>(entries(key));
        more.add(plainScalar(typed));
        if (putPlain(key, more)) {
            return true;
        }
        more.set(more.size() - 1, typed);
        return putPlain(key, more);
    }

    /** Takes the entry at {@code index} out of a list setting. */
    public void removeEntry(final ConfigKey<?> key, final int index) {
        final List<Object> fewer = new ArrayList<>(entries(key));
        if (index >= 0 && index < fewer.size()) {
            fewer.remove(index);
            putPlain(key, fewer);
        }
    }

    /** The entries of a map setting as the screen shows them, by name, written as a file writes them. */
    public Map<String, Object> mapEntries(final ConfigKey<?> key) {
        final Map<String, Object> out = new LinkedHashMap<>();
        if (plainNow(key) instanceof Map<?, ?> map) {
            map.forEach((name, value) -> out.put(String.valueOf(name), value));
        }
        return out;
    }

    /**
     * Sets the entry {@code name} of a map setting to what a player typed: read as a plain value first, and as the text
     * itself when the map does not take that.
     *
     * @return whether the map took it
     */
    public boolean putMapEntry(final ConfigKey<?> key, final String name, final String typed) {
        if (name.isBlank()) {
            return false;
        }
        final Map<String, Object> changed = mapEntries(key);
        changed.put(name.trim(), plainScalar(typed));
        if (putPlain(key, changed)) {
            return true;
        }
        changed.put(name.trim(), typed);
        return putPlain(key, changed);
    }

    /** Takes the entry {@code name} out of a map setting. */
    public void removeMapEntry(final ConfigKey<?> key, final String name) {
        final Map<String, Object> changed = mapEntries(key);
        if (changed.remove(name) != null) {
            putPlain(key, changed);
        }
    }

    /** The colour a colour setting shows, {@code 0xRRGGBB}; its default's when what it holds cannot be read. */
    public int colourOf(final ConfigKey<String> key) {
        return ConfigColours.parse(value(key)).orElseGet(() -> ConfigColours.parse(key.defaultValue()).orElse(0));
    }

    /** Sets a colour setting to {@code rgb}, {@code 0xRRGGBB}. */
    public void setColour(final ConfigKey<String> key, final int rgb) {
        put(key, ConfigColours.write(rgb));
    }

    /** Whether there is a step to take back. */
    public boolean canUndo() {
        return !undone.isEmpty();
    }

    /** Whether there is a step taken back to take again. */
    public boolean canRedo() {
        return !redone.isEmpty();
    }

    /** Takes the last step back. */
    public void undo() {
        if (!undone.isEmpty()) {
            redone.push(new LinkedHashMap<>(pending));
            restore(undone.pop());
        }
        sliding = null;
    }

    /** Takes the last step taken back again. */
    public void redo() {
        if (!redone.isEmpty()) {
            undone.push(new LinkedHashMap<>(pending));
            restore(redone.pop());
        }
        sliding = null;
    }

    /**
     * Takes every setting a file's plain values hold (a preset, a text imported), each held to what it is held to, as
     * one step; what they do not hold is left as it is. What cannot be read as its setting is left out.
     *
     * @return how many settings it took
     */
    public int takePlain(final Map<String, Object> plain) {
        step();
        int taken = 0;
        for (final ConfigKey<?> key : file.keys()) {
            final Object raw = ConfigTree.get(PlainValues.map(plain), key.path());
            if (raw != null && takeOne(key, raw)) {
                taken++;
            }
        }
        return taken;
    }

    /** The values the screen shows, changes and all, as the file would hold them: what an export writes. */
    public Map<String, Object> toPlain() {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put(ConfigFile.VERSION_KEY, file.version());
        for (final ConfigKey<?> key : file.keys()) {
            ConfigTree.put(out, key.path(), plainNow(key));
        }
        return out;
    }

    /** Whether {@code key} holds a change not kept yet. */
    public boolean isChanged(final ConfigKey<?> key) {
        return pending.containsKey(key.dottedPath());
    }

    /** The settings changed and not kept yet, by dotted path. */
    public Set<String> changed() {
        return Set.copyOf(pending.keySet());
    }

    /** Takes the value a chooser picked for {@code key}, held to what the setting is held to. */
    public <T> void choose(final ConfigKey<T> key, final T value) {
        put(key, value);
    }

    /**
     * Takes a value a slider or a picker gives as it is dragged: the moves of one drag of one setting are one step for
     * undo, however many values it passed through.
     */
    public <T> void slide(final ConfigKey<T> key, final T value) {
        if (!key.dottedPath().equals(sliding)) {
            step();
        }
        hold(key, value);
        sliding = key.dottedPath();
    }

    /** Ends a drag, so the next move of the same setting is a step of its own. */
    public void endSlide() {
        sliding = null;
    }

    /** The changes not kept yet, each by its dotted path, written as a file writes it: what is sent to a server. */
    public Map<String, Object> pendingPlain() {
        final Map<String, Object> out = new LinkedHashMap<>();
        for (final String path : pending.keySet()) {
            file.key(path).ifPresent(key -> out.put(path, plainNow(key)));
        }
        return out;
    }

    /** Forgets every change not kept yet, and what undo and redo held. */
    public void discard() {
        pending.clear();
        undone.clear();
        redone.clear();
        sliding = null;
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
                // A word outside its list, or one its codec does not take (a colour not written #RRGGBB), is refused.
                if (key.allowed().isPresent() && !key.allowed().get().contains(text)
                        || key.read(text).result().isEmpty()) {
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

    /** Puts every setting back to its default, as changes to keep or drop like any other, one step for undo. */
    public void defaults() {
        defaults(List.copyOf(file.keys()));
    }

    /** Puts the given settings back to their defaults, a section's, as changes to keep or drop, one step for undo. */
    public void defaults(final List<ConfigKey<?>> keys) {
        step();
        for (final ConfigKey<?> key : keys) {
            holdDefault(key);
        }
    }

    /** Puts one setting back to its default, as a change to keep or drop like any other. */
    public void toDefault(final ConfigKey<?> key) {
        step();
        holdDefault(key);
    }

    /** Whether {@code key} shows a value other than its default, kept or not. */
    public boolean isOffDefault(final ConfigKey<?> key) {
        return !Objects.equals(value(key), key.defaultValue());
    }

    /** How many changes are held and not kept yet. */
    public int changeCount() {
        return pending.size();
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
        discard();
    }

    /* Holds the change as a step of its own, which undo takes back. */
    private <T> void put(final ConfigKey<T> key, final T value) {
        step();
        hold(key, value);
    }

    /* Keeps what the changes are now, before a step, and forgets what was taken back. */
    private void step() {
        undone.push(new LinkedHashMap<>(pending));
        redone.clear();
        sliding = null;
    }

    private void restore(final Map<String, Object> changes) {
        pending.clear();
        pending.putAll(changes);
    }

    private <T> boolean takeOne(final ConfigKey<T> key, final Object raw) {
        final Optional<T> read = key.read(raw).result();
        read.ifPresent(value -> hold(key, value));
        return read.isPresent();
    }

    private <T> Object plainNow(final ConfigKey<T> key) {
        return key.plain(value(key));
    }

    private static <T> Object plainDefault(final ConfigKey<T> key) {
        return key.plain(key.defaultValue());
    }

    /* Whether a plain value is one a single field shows: a word, a number or a switch. */
    private static boolean scalar(final Object plain) {
        return plain instanceof String || plain instanceof Number || plain instanceof Boolean;
    }

    /* Holds the change, held to what the setting is held to; a change back to the file's value is no change. */
    private <T> void hold(final ConfigKey<T> key, final T value) {
        final T held = file.validated(key, value);
        if (Objects.equals(held, file.get(key))) {
            pending.remove(key.dottedPath());
        } else {
            pending.put(key.dottedPath(), held);
        }
    }

    private <T> void holdDefault(final ConfigKey<T> key) {
        hold(key, key.defaultValue());
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
