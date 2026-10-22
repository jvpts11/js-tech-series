/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.font.CellFont;

/**
 * A font of the test mod made from a TrueType file, declared the way a mod declares one: the X11 fixed 6 by 10 font
 * with each of its pixels drawn as a square, so drawn at 10 pixels it gives back the Core's own fixed font, pixel for
 * pixel.
 */
public final class TestFonts {

    /** The font made from {@code assets/jstests/font/source/test_squares.ttf}, drawn 10 pixels tall. */
    public static final CellFont SQUARES = TestSounds.CONTENT.font("test_squares")
            .outline("font/source/test_squares.ttf", 10)
            .cell(6, 10)
            .baseline(8)
            .licence("Public domain")
            .credit("The X11 fixed 6x10 font, each pixel drawn as a square by the series' own script")
            .register();

    private TestFonts() {
    }

    /** Declares the font, from the test mod's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }
}
