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
 * <p>{@link #FIXED_6X10} is the X Window System's "fixed" font at six by ten, the small font of xterm and of the
 * Unix consoles of its time: one cell for every character, nearly sixteen hundred characters across Latin, Greek,
 * Cyrillic, Hebrew, runes, Braille, the symbols and the box and block characters, put in the public domain by its
 * makers. It is what a terminal-like view of any mod draws in.
 */
public final class CoreFonts {

    /** Misc Fixed 6x10: a cell six pixels wide and ten tall, eight of them above the baseline. */
    public static final CellFont FIXED_6X10 = CoreItems.CONTENT.font("fixed_6x10")
            .bdf("font/source/fixed_6x10.bdf")
            .cell(6, 10)
            .baseline(8)
            .licence("Public domain")
            .credit("Misc Fixed 6x10, from Markus Kuhn's ucs-fonts as the X.Org Foundation ships it in font-misc-misc "
                    + "(https://gitlab.freedesktop.org/xorg/font/misc-misc)")
            .register();

    private CoreFonts() {
    }

    public static void declare() {
        // Loading the class declares them.
    }
}
