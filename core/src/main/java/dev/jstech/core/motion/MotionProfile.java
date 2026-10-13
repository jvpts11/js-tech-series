/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.config.format.IConfigComments;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * How one system moves: for each kind of thing it moves ({@link MotionKinds}), how ({@link MotionSpec}). A kind the
 * profile does not name does not move.
 *
 * <p>Kept as a file a resource pack can replace, one object per kind:
 *
 * <pre>
 * {
 *   "window_open": { "style": "scale", "duration": 150, "easing": "ease-out-expo", "group": "animations",
 *                    "from": 0.8 },
 *   "menu_show":   { "style": "slide", "duration": 200, "easing": "ease-out-cubic", "distance": 0.2 }
 * }
 * </pre>
 *
 * Every field but the style may be left out: the duration and the delay are then 0, the curve linear, and no switch
 * turns the motion off. Any other number is one the style reads. A pack that wants a system still writes
 * {@code "style": "none"}, or leaves the kind out.
 */
public record MotionProfile(Map<String, MotionSpec> kinds) {

    /** A system where nothing moves. */
    public static final MotionProfile STILL = new MotionProfile(Map.of());

    private static final String STYLE = "style";
    private static final String DURATION = "duration";
    private static final String DELAY = "delay";
    private static final String EASING = "easing";
    private static final String GROUP = "group";

    public MotionProfile {
        kinds = Map.copyOf(kinds);
    }

    /** Starts a profile, one kind at a time. */
    public static Builder builder() {
        return new Builder();
    }

    /** How that kind of thing moves on this system; {@link MotionSpec#NONE} for a kind it does not name. */
    public MotionSpec spec(final String kind) {
        return kinds.getOrDefault(kind, MotionSpec.NONE);
    }

    /**
     * The profile a motion file holds, as its bytes.
     *
     * @throws IllegalArgumentException when the file is not a JSON object, or a kind's entry is not an object, has
     *                                  no style, or names no curve
     */
    public static MotionProfile read(final byte[] file) {
        try {
            return read(ConfigFormats.JSON.read(file));
        } catch (final ConfigFormatException notJson) {
            throw new IllegalArgumentException("a motion file is a JSON object: " + notJson.getMessage(), notJson);
        }
    }

    /**
     * The profile a motion file holds, as the plain values its JSON reads into: an object of kinds, each an object of
     * fields.
     *
     * @throws IllegalArgumentException when a kind's entry is not an object, has no style, or names no curve
     */
    public static MotionProfile read(final Map<String, Object> file) {
        final Map<String, MotionSpec> kinds = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> kind : file.entrySet()) {
            if (!(kind.getValue() instanceof Map<?, ?> entry)) {
                throw new IllegalArgumentException("the motion of " + kind.getKey() + " is not an object");
            }
            kinds.put(kind.getKey(), readSpec(kind.getKey(), entry));
        }
        return new MotionProfile(kinds);
    }

    /** This profile as the plain values of a motion file, its kinds and their numbers in a stable order. */
    public Map<String, Object> write() {
        final Map<String, Object> file = new LinkedHashMap<>();
        for (final Map.Entry<String, MotionSpec> kind : new TreeMap<>(kinds).entrySet()) {
            final MotionSpec spec = kind.getValue();
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put(STYLE, spec.style());
            entry.put(DURATION, spec.duration());
            if (spec.delay() > 0) {
                entry.put(DELAY, spec.delay());
            }
            entry.put(EASING, IEasing.nameOf(spec.easing()));
            if (!spec.group().isEmpty()) {
                entry.put(GROUP, spec.group());
            }
            entry.putAll(spec.params());
            file.put(kind.getKey(), entry);
        }
        return file;
    }

    /** This profile as the bytes of a motion file. */
    public byte[] json() {
        return ConfigFormats.JSON.write(write(), IConfigComments.NONE);
    }

    private static MotionSpec readSpec(final String kind, final Map<?, ?> entry) {
        if (!(entry.get(STYLE) instanceof String style)) {
            throw new IllegalArgumentException("the motion of " + kind + " does not say its style");
        }
        final Map<String, Double> params = new TreeMap<>();
        for (final Map.Entry<?, ?> field : entry.entrySet()) {
            final String name = String.valueOf(field.getKey());
            if (!MotionSpec.RESERVED.contains(name) && field.getValue() instanceof Number number) {
                params.put(name, number.doubleValue());
            }
        }
        final String easing = text(kind, entry, EASING);
        return new MotionSpec(style, whole(kind, entry, DURATION), whole(kind, entry, DELAY),
                easing == null ? IEasing.LINEAR : IEasing.named(easing), text(kind, entry, GROUP, ""), params);
    }

    /** A whole number of milliseconds, none where the file gives none, from 0 up and saturating, never wrapping. */
    private static int whole(final String kind, final Map<?, ?> entry, final String field) {
        final Object value = entry.get(field);
        if (value == null) {
            return 0;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("the " + field + " of the motion of " + kind + " is not a number");
        }
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, Math.round(number.doubleValue())));
    }

    /** A text field, or null where the file gives none. */
    private static String text(final String kind, final Map<?, ?> entry, final String field) {
        final Object value = entry.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("the " + field + " of the motion of " + kind + " is not a text");
        }
        return text;
    }

    private static String text(final String kind, final Map<?, ?> entry, final String field, final String fallback) {
        final String text = text(kind, entry, field);
        return text == null ? fallback : text;
    }

    /** A profile being put together, one kind at a time, in the order the kinds are given. */
    public static final class Builder {

        private final Map<String, MotionSpec> kinds = new LinkedHashMap<>();

        private Builder() {
        }

        /** How that kind of thing moves. */
        public Builder kind(final String kind, final MotionSpec spec) {
            kinds.put(kind, spec);
            return this;
        }

        public MotionProfile build() {
            return new MotionProfile(kinds);
        }
    }
}
