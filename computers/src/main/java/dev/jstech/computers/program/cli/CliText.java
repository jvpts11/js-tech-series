/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.Locale;

/**
 * The little pieces of text every family of commands writes the same way, so a number printed by the network reads
 * like a number printed by a drive.
 */
public final class CliText {

    private CliText() {
    }

    /** A number with its thousands apart, as every listing writes one: {@code 18,742}. */
    public static String group(final long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /**
     * Text in a column of that many cells, padded on the right, and cut with a tilde when it is too long.
     *
     * <p>A terminal is a grid, so a column is a column: anything that runs past its width would push
     * everything after it out of line, and a listing that does not line up is harder to read than a name that
     * is a letter short.
     */
    public static String pad(final String text, final int width) {
        if (width <= 0) {
            return "";
        }
        if (text.length() > width) {
            return width == 1 ? "~" : text.substring(0, width - 2) + "~ ";
        }
        return text + " ".repeat(width - text.length());
    }

    /** The same in a column padded on the left, which is where a number belongs. */
    public static String padLeft(final String text, final int width) {
        if (width <= 0) {
            return "";
        }
        if (text.length() > width) {
            return width == 1 ? "~" : "~" + text.substring(text.length() - width + 1);
        }
        return " ".repeat(width - text.length()) + text;
    }
}
