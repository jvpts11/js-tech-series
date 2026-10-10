/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * How one kind of thing moves on one system: the way it moves ({@link MotionStyles}), for how long, after how long a
 * wait, along which curve, which of the system's switches turns it off, and the numbers the way of moving takes (how
 * small a window grows from, how far a menu slides).
 *
 * @param style    how it moves, one of {@link MotionStyles}
 * @param duration how long it takes, in milliseconds
 * @param delay    how long it waits before it starts, in milliseconds
 * @param easing   the curve it follows
 * @param group    the switches that turn it off on the system's own settings page, separated by spaces, empty for
 *                 none: any one of them switched off keeps it still, so a motion can answer to its own box and to a
 *                 box over a whole group of effects at once
 * @param params   the numbers the style reads, by name
 */
public record MotionSpec(String style, int duration, int delay, IEasing easing, String group,
                         Map<String, Double> params) {

    /** Nothing moves: what happens happens at once. */
    public static final MotionSpec NONE = new MotionSpec(MotionStyles.NONE, 0, 0, IEasing.LINEAR, "", Map.of());

    /** The names a motion file keeps for its own fields, which a style's number cannot take. */
    static final Set<String> RESERVED = Set.of("style", "duration", "delay", "easing", "group");

    public MotionSpec {
        for (final String name : params.keySet()) {
            if (RESERVED.contains(name)) {
                throw new IllegalArgumentException("'" + name + "' is a field of a motion, not a number of its style");
            }
        }
        if (duration < 0 || delay < 0) {
            throw new IllegalArgumentException("a motion cannot last " + duration + " ms after " + delay + " ms");
        }
        params = Map.copyOf(new TreeMap<>(params));
    }

    /** A motion of that style, that long, along that curve, under that switch, with no numbers of its own. */
    public static MotionSpec of(final String style, final int duration, final IEasing easing, final String group) {
        return new MotionSpec(style, duration, 0, easing, group, Map.of());
    }

    /** The same motion with one more number for its style. */
    public MotionSpec with(final String param, final double value) {
        final Map<String, Double> more = new TreeMap<>(params);
        more.put(param, value);
        return new MotionSpec(style, duration, delay, easing, group, more);
    }

    /** The same motion after that wait. */
    public MotionSpec after(final int waitMs) {
        return new MotionSpec(style, duration, waitMs, easing, group, params);
    }

    /**
     * The same motion taking {@code percent} of its own time, its wait as well: 100 leaves it as it is, 200 takes
     * twice as long, and 0 makes it happen at once, which is what a system's speed slider set to its end does.
     */
    public MotionSpec slowed(final int percent) {
        final int at = Math.max(0, percent);
        return new MotionSpec(style, (int) ((long) duration * at / 100), (int) ((long) delay * at / 100), easing,
                group, params);
    }

    /** The switches that turn it off, in the order they are written. */
    public List<String> groups() {
        final List<String> out = new ArrayList<>();
        for (final String name : group.trim().split("\\s+")) {
            if (!name.isEmpty()) {
                out.add(name);
            }
        }
        return out;
    }

    /** One of the style's numbers, or {@code fallback} when this motion does not give it. */
    public double param(final String name, final double fallback) {
        final Double value = params.get(name);
        return value == null ? fallback : value;
    }

    /** Whether anything moves at all. */
    public boolean moves() {
        return !MotionStyles.NONE.equals(style) && duration > 0;
    }
}
