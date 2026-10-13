/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The plain values every settings format reads into and writes from: maps with text keys in their order, lists,
 * text, whole numbers as {@link Integer} or {@link Long}, fractions as {@link Double}, and booleans.
 */
public final class PlainValues {

    private PlainValues() {
    }

    /** A copy of {@code map} in plain values: its keys as text, its maps and lists copied as well. */
    public static Map<String, Object> map(final Map<?, ?> map) {
        final Map<String, Object> out = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final Object value = value(entry.getValue());
            if (value != null) {
                out.put(String.valueOf(entry.getKey()), value);
            }
        }
        return out;
    }

    /**
     * {@code value} as a plain value, or null when it has none: smaller whole numbers widen to an {@link Integer} and
     * a {@link Float} to a {@link Double}, and anything else a library hands back (a date, say) becomes its text.
     */
    public static Object value(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return map(map);
        }
        if (value instanceof Collection<?> collection) {
            final List<Object> out = new ArrayList<>(collection.size());
            for (final Object element : collection) {
                final Object plain = value(element);
                if (plain != null) {
                    out.add(plain);
                }
            }
            return out;
        }
        if (value instanceof Byte || value instanceof Short) {
            return ((Number) value).intValue();
        }
        if (value instanceof Float number) {
            return number.doubleValue();
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Double || value instanceof Boolean
                || value instanceof String) {
            return value;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return String.valueOf(value);
    }

    /**
     * {@code raw} read in the shape of {@code shape}, the value a setting holds when nobody has changed it: a number
     * where a boolean belongs is that boolean, since a format with no booleans of its own (NBT) keeps them as 0 and 1.
     * Maps are matched key by key and a list by its first element; anything else is left as it is, for the setting's
     * own reading to take or refuse.
     */
    public static Object shapedLike(final Object raw, final Object shape) {
        if (shape instanceof Boolean && raw instanceof Number number) {
            return number.longValue() != 0;
        }
        if (shape instanceof Map<?, ?> shapeMap && raw instanceof Map<?, ?> rawMap) {
            final Map<String, Object> out = new LinkedHashMap<>();
            for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
                final Object inShape = shapeMap.get(entry.getKey());
                out.put(String.valueOf(entry.getKey()),
                        inShape == null ? entry.getValue() : shapedLike(entry.getValue(), inShape));
            }
            return out;
        }
        if (shape instanceof List<?> shapeList && !shapeList.isEmpty() && raw instanceof List<?> rawList) {
            final List<Object> out = new ArrayList<>(rawList.size());
            for (final Object element : rawList) {
                out.add(shapedLike(element, shapeList.get(0)));
            }
            return out;
        }
        return raw;
    }
}
