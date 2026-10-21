/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

/**
 * Whether a file, a sound or a setting is named after a thing: its name holds the thing's name whole, between the
 * name's ends or the marks a path is cut by ({@code /}, {@code .}, {@code _}, {@code -}). So the effect
 * {@code compressor_steam} and the sound {@code machine/compressor.running} are the compressor's, and the settings
 * {@code compressors} and {@code decompressor} are not.
 */
public final class ThingNames {

    private ThingNames() {
    }

    /** Whether {@code name} is named after the thing whose id's path is {@code path}, by its last part. */
    public static boolean names(final String name, final String path) {
        final String thing = path.substring(path.lastIndexOf('/') + 1);
        if (thing.isEmpty()) {
            return false;
        }
        int from = name.indexOf(thing);
        while (from >= 0) {
            final int end = from + thing.length();
            if ((from == 0 || cut(name.charAt(from - 1))) && (end == name.length() || cut(name.charAt(end)))) {
                return true;
            }
            from = name.indexOf(thing, from + 1);
        }
        return false;
    }

    private static boolean cut(final char c) {
        return c == '/' || c == '.' || c == '_' || c == '-';
    }
}
