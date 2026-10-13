/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.state;

import java.util.Map;

/**
 * What a state that keeps one value for each key does with its file, whatever the key is: a player, a team. A value
 * equal to the default is never kept, so the file holds only what differs.
 */
final class KeyedValues {

    private KeyedValues() {
    }

    /** The value kept under {@code key}, or {@code defaultValue} when none is. */
    static <K, T> T get(final StateSave<Map<K, T>> file, final K key, final T defaultValue) {
        return file.value().getOrDefault(key, defaultValue);
    }

    /**
     * Keeps {@code value} under {@code key}, and marks the file changed.
     *
     * @return false when the value kept was already equal to it, and nothing changed
     */
    static <K, T> boolean set(final StateSave<Map<K, T>> file, final K key, final T value, final T defaultValue) {
        if (value.equals(get(file, key, defaultValue))) {
            return false;
        }
        if (value.equals(defaultValue)) {
            file.value().remove(key);
        } else {
            file.value().put(key, value);
        }
        file.changed();
        return true;
    }

    /** Every value kept that is not the default, by key. */
    static <K, T> Map<K, T> all(final StateSave<Map<K, T>> file) {
        return Map.copyOf(file.value());
    }
}
