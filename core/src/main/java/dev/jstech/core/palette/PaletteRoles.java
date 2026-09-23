/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * A palette as the roles it is made of: a record whose every component is one colour, named for what it colours.
 *
 * <p>The record's own component names are the roles, so a palette is declared once, as the record it already is,
 * and nothing keeps a second list of its names in step. A colour is written {@code #AARRGGBB}, and read that way or
 * as {@code #RRGGBB}, which is opaque.
 *
 * <p>Pure: no game types, so the rules of reading and writing a palette hold without a game running.
 */
public final class PaletteRoles {

    private PaletteRoles() {
    }

    /**
     * The roles of a palette record, in the order it declares them.
     *
     * @throws IllegalArgumentException when a component is not a colour, which a palette has nothing else of
     */
    public static List<String> roles(final Class<? extends Record> shape) {
        final List<String> out = new ArrayList<>();
        for (final RecordComponent component : shape.getRecordComponents()) {
            if (component.getType() != int.class) {
                throw new IllegalArgumentException("a palette holds colours only, and " + shape.getSimpleName()
                        + "." + component.getName() + " is a " + component.getType().getSimpleName());
            }
            out.add(component.getName());
        }
        return out;
    }

    /** Every role of the palette with its colour, in the order the record declares them. */
    public static Map<String, Integer> read(final Record palette) {
        final Map<String, Integer> out = new LinkedHashMap<>();
        for (final RecordComponent component : palette.getClass().getRecordComponents()) {
            try {
                final Method accessor = component.getAccessor();
                accessor.setAccessible(true);
                out.put(component.getName(), (Integer) accessor.invoke(palette));
            } catch (final IllegalAccessException | InvocationTargetException unreadable) {
                throw new IllegalStateException("the colour " + component.getName() + " could not be read",
                        unreadable);
            }
        }
        return out;
    }

    /**
     * The palette {@code base} with the colours {@code overrides} names put in its place. A role the overrides do
     * not name keeps the base's colour, which is what lets a resource pack change one role and no other.
     */
    public static <P extends Record> P with(final P base, final Map<String, Integer> overrides) {
        final Map<String, Integer> colours = read(base);
        overrides.forEach((role, colour) -> {
            if (colours.containsKey(role)) {
                colours.put(role, colour);
            }
        });
        return build(shapeOf(base), colours);
    }

    /** Writes a colour the way a palette file holds it: {@code #AARRGGBB}, in capitals. */
    public static String format(final int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    /** Reads a colour as a palette file holds it, {@code #AARRGGBB} or {@code #RRGGBB}; null when it is neither. */
    @Nullable
    public static Integer parse(@Nullable final String text) {
        if (text == null || !text.startsWith("#")) {
            return null;
        }
        final String hex = text.substring(1);
        if (hex.length() != 6 && hex.length() != 8) {
            return null;
        }
        try {
            final long value = Long.parseLong(hex, 16);
            return (int) (hex.length() == 6 ? 0xFF000000L | value : value);
        } catch (final NumberFormatException notHex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <P extends Record> Class<P> shapeOf(final P palette) {
        return (Class<P>) palette.getClass();
    }

    private static <P extends Record> P build(final Class<P> shape, final Map<String, Integer> colours) {
        final RecordComponent[] components = shape.getRecordComponents();
        final Class<?>[] types = new Class<?>[components.length];
        final Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = int.class;
            values[i] = colours.get(components[i].getName());
        }
        try {
            final Constructor<P> canonical = shape.getDeclaredConstructor(types);
            canonical.setAccessible(true);
            return canonical.newInstance(values);
        } catch (final ReflectiveOperationException unbuildable) {
            throw new IllegalStateException("the palette " + shape.getSimpleName() + " could not be built",
                    unbuildable);
        }
    }
}
