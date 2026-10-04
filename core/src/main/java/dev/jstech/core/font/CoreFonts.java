/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import dev.jstech.core.registry.CoreItems;

/**
 * The fonts the Core carries for every mod built on it.
 *
 * <p>They are the X Window System's "fixed" font in three sizes, the fonts of xterm and of the Unix consoles of its
 * time, put in the public domain by their makers: {@link #FIXED_6X10}, the small one, with nearly sixteen hundred
 * characters across Latin, Greek, Cyrillic, Hebrew, runes, Braille, the symbols and the box and block characters, and
 * {@link #FIXED_9X15} and {@link #FIXED_10X20}, the same design larger, with several thousand. One cell for every
 * character. A terminal-like view picks the size that fits it best on the screen it is on.
 */
public final class CoreFonts {

    /** The licence the three come under, as their makers name it. */
    private static final String LICENCE = "Public domain";
    /** Who made them and where they come from, after the size's own name. */
    private static final String MAKERS = ", from Markus Kuhn's ucs-fonts as the X.Org Foundation ships it in "
            + "font-misc-misc (https://gitlab.freedesktop.org/xorg/font/misc-misc)";

    /** Misc Fixed 6x10: a cell six pixels wide and ten tall, eight of them above the baseline. */
    public static final CellFont FIXED_6X10 = misc("6x10", 6, 10, 8);

    /** Misc Fixed 9x15, the same design a size up: a cell nine pixels wide and fifteen tall, twelve above the line. */
    public static final CellFont FIXED_9X15 = misc("9x15", 9, 15, 12);

    /** Misc Fixed 10x20, the largest of the three: a cell ten pixels wide and twenty tall, sixteen above the line. */
    public static final CellFont FIXED_10X20 = misc("10x20", 10, 20, 16);

    private CoreFonts() {
    }

    public static void declare() {
        // Loading the class declares them.
    }

    /** One size of Misc Fixed, read from its BDF source under the Core's {@code font/source/}. */
    private static CellFont misc(final String size, final int width, final int height, final int baseline) {
        return CoreItems.CONTENT.font("fixed_" + size)
                .bdf("font/source/fixed_" + size + ".bdf")
                .cell(width, height)
                .baseline(baseline)
                .licence(LICENCE)
                .credit("Misc Fixed " + size + MAKERS)
                .register();
    }
}
