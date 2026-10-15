/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config;

import dev.jstech.core.text.TextKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A named set of values for some of a file's settings ("gentle consequences", "full realism"), which a mod ships with
 * its file and a server owner picks, on the settings screen or with {@code /jstech config preset}. Picking one changes
 * only the settings it names; the others keep their values.
 *
 * <pre>{@code
 * ConfigPreset GENTLE = ConfigPreset.of("gentle", GENTLE_TITLE)
 *         .with(RADIATION_DAMAGE, false)
 *         .with(POLLUTION_RATE, 0.25);
 * }</pre>
 *
 * @param name   its name in commands and in the file of presets: one word
 * @param title  what a settings screen calls it, translated
 * @param values each setting it names, by dotted path, written as a file writes it
 */
public record ConfigPreset(String name, TextKey title, Map<String, Object> values) {

    public ConfigPreset {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(title, "title");
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** A preset that names no setting yet. */
    public static ConfigPreset of(final String name, final TextKey title) {
        return new ConfigPreset(name, title, Map.of());
    }

    /** The same preset, setting {@code key} to {@code value} too. */
    public <T> ConfigPreset with(final ConfigKey<T> key, final T value) {
        final Map<String, Object> more = new LinkedHashMap<>(this.values);
        more.put(key.dottedPath(), key.plain(value));
        return new ConfigPreset(this.name, this.title, more);
    }

    /** Its values as a file holds them, each at its place in the sections. */
    public Map<String, Object> plain() {
        final Map<String, Object> out = new LinkedHashMap<>();
        this.values.forEach((path, value) -> ConfigTree.put(out, ConfigTree.path(path), value));
        return out;
    }
}
