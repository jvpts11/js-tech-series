/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a mod tells an agent of one of its things ({@link AgentReadings}): named values, numbers, words and flags, and
 * named parts holding more of them, in the order they are told. A name is told once: told again, it takes the newer
 * value.
 */
public final class AgentReport {

    private final Map<String, Object> values = new LinkedHashMap<>();

    /** Tells a number by its name, in the unit the name says, such as {@code speed_mps}. */
    public AgentReport number(final String name, final double value) {
        return put(name, value);
    }

    /** Tells a whole number by its name. */
    public AgentReport whole(final String name, final long value) {
        return put(name, value);
    }

    /** Tells some words by their name, as a mode's name or a world's. */
    public AgentReport words(final String name, final String value) {
        return put(name, value);
    }

    /** Tells whether something is so, by its name. */
    public AgentReport flag(final String name, final boolean value) {
        return put(name, value);
    }

    /** The part named {@code name}, to tell more in, made the first time it is asked for. */
    public AgentReport part(final String name) {
        if (this.values.get(named(name)) instanceof AgentReport part) {
            return part;
        }
        final AgentReport part = new AgentReport();
        this.values.put(name, part);
        return part;
    }

    /** Whether it tells nothing, its parts telling nothing either. */
    public boolean isEmpty() {
        return this.values.values().stream().allMatch(value -> value instanceof AgentReport part && part.isEmpty());
    }

    /**
     * What it tells, by name, in the order it was told: each a {@link Double}, a {@link Long}, a {@link String}, a
     * {@link Boolean} or an {@code AgentReport}.
     */
    public Map<String, Object> values() {
        return Collections.unmodifiableMap(this.values);
    }

    /* Takes in what {@code other} tells, its parts into its own parts of the same names. */
    void merge(final AgentReport other) {
        other.values.forEach((name, value) -> {
            if (value instanceof AgentReport part) {
                part(name).merge(part);
            } else {
                this.values.put(name, value);
            }
        });
    }

    private AgentReport put(final String name, final Object value) {
        this.values.put(named(name), value);
        return this;
    }

    private static String named(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a reading is named, not blank: " + name);
        }
        return name;
    }
}
