/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import java.util.Locale;

/**
 * DOS's wildcards for file names: a star for any run of characters, a question mark for any one, matched without
 * regard to case, and {@code *.*} for every name there is, one with no dot in it included, as DOS read it.
 */
public final class Wildcards {

    private static final String EVERY_NAME = "*.*";

    private Wildcards() {
    }

    /** Whether {@code name} is like {@code pattern}. */
    public static boolean matches(final String pattern, final String name) {
        final String p = pattern.toUpperCase(Locale.ROOT);
        if (EVERY_NAME.equals(p)) {
            return true;
        }
        return matchFrom(p, 0, name.toUpperCase(Locale.ROOT), 0);
    }

    private static boolean matchFrom(final String p, final int pi, final String n, final int ni) {
        if (pi == p.length()) {
            return ni == n.length();
        }
        final char c = p.charAt(pi);
        if (c == '*') {
            for (int k = ni; k <= n.length(); k++) {
                if (matchFrom(p, pi + 1, n, k)) {
                    return true;
                }
            }
            return false;
        }
        return ni < n.length() && (c == '?' || c == n.charAt(ni)) && matchFrom(p, pi + 1, n, ni + 1);
    }
}
