/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

/**
 * How an unsaved query is folded onto one line of the window's saved state and read back: a backslash
 * becomes two and a line break becomes a backslash and the letter n.
 */
final class IsmsQueryEscaping {

    private IsmsQueryEscaping() {
    }

    /** Folds the text onto one line. */
    static String escape(final String text) {
        return text.replace("\\", "\\\\").replace("\n", "\\n");
    }

    /**
     * Reads back what {@link #escape} wrote, in one left-to-right pass: a doubled backslash is one backslash
     * and a backslash with an n is a line break. Replacing one before the other would turn a real backslash
     * followed by the letter n into a line break.
     */
    static String unescape(final String saved) {
        final StringBuilder out = new StringBuilder(saved.length());
        for (int i = 0; i < saved.length(); i++) {
            final char c = saved.charAt(i);
            if (c == '\\' && i + 1 < saved.length()) {
                final char next = saved.charAt(i + 1);
                if (next == '\\') {
                    out.append('\\');
                    i++;
                    continue;
                }
                if (next == 'n') {
                    out.append('\n');
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
