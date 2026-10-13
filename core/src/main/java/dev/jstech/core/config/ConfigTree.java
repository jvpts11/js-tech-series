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

/**
 * Reaching into a settings file's plain values by path: {@code ["boot", "show_boot_menu"]} is the value named
 * {@code show_boot_menu} in the section {@code boot}. An upgrade step works on a file this way.
 */
public final class ConfigTree {

    private ConfigTree() {
    }

    /** The value at {@code path}, or null when there is none. */
    public static Object get(final Map<String, Object> values, final List<String> path) {
        Object at = values;
        for (final String segment : path) {
            if (!(at instanceof Map<?, ?> map)) {
                return null;
            }
            at = map.get(segment);
        }
        return at;
    }

    /** Puts a value at {@code path}, making the sections on the way; a value where a section goes is replaced. */
    @SuppressWarnings("unchecked")
    public static void put(final Map<String, Object> values, final List<String> path, final Object value) {
        Map<String, Object> at = values;
        for (int i = 0; i < path.size() - 1; i++) {
            final Object next = at.get(path.get(i));
            if (next instanceof Map<?, ?> map) {
                at = (Map<String, Object>) map;
            } else {
                final Map<String, Object> made = new LinkedHashMap<>();
                at.put(path.get(i), made);
                at = made;
            }
        }
        at.put(path.get(path.size() - 1), value);
    }

    /** Takes the value at {@code path} out and gives it back, or gives null when there was none. */
    @SuppressWarnings("unchecked")
    public static Object remove(final Map<String, Object> values, final List<String> path) {
        Map<String, Object> at = values;
        for (int i = 0; i < path.size() - 1; i++) {
            if (!(at.get(path.get(i)) instanceof Map<?, ?> map)) {
                return null;
            }
            at = (Map<String, Object>) map;
        }
        return at.remove(path.get(path.size() - 1));
    }

    /** A dotted path, {@code "boot.show_boot_menu"}, as its parts. */
    public static List<String> path(final String dotted) {
        return List.of(dotted.split("\\.", -1));
    }
}
