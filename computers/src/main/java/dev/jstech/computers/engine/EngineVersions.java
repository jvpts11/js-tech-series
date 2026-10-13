/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

/**
 * The order of an engine's versions, as a house numbers them: a dotted version and a year both read as numbers, so
 * {@code 4.2} comes before {@code 2000}, and {@code 2008} before {@code 2012}.
 */
public final class EngineVersions {

    private EngineVersions() {
    }

    /**
     * Compares two versions part by part, each part as a number where both are numbers and as text otherwise; a
     * missing part counts as nothing. Negative when {@code a} is the older.
     */
    public static int compare(final String a, final String b) {
        final String[] left = a.split("\\.");
        final String[] right = b.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            final String l = i < left.length ? left[i] : "0";
            final String r = i < right.length ? right[i] : "0";
            final int order = isNumber(l) && isNumber(r)
                    ? Long.compare(Long.parseLong(l), Long.parseLong(r)) : l.compareTo(r);
            if (order != 0) {
                return order;
            }
        }
        return 0;
    }

    /** Whether {@code have} is {@code min} or newer. */
    public static boolean atLeast(final String have, final String min) {
        return compare(have, min) >= 0;
    }

    private static boolean isNumber(final String part) {
        if (part.isEmpty() || part.length() > 18) {
            return false;
        }
        for (int i = 0; i < part.length(); i++) {
            if (!Character.isDigit(part.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
