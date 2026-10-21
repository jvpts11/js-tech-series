/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextSizesTest {

    private static final float EXACT = 1e-6f;

    @Test
    void scale_isTheScreensOwnAtTheNormalSize() {
        assertEquals(1.0f, TextSizes.scale(2, 0, 1.0f), EXACT);
        assertEquals(0.75f, TextSizes.scale(2, 0, 0.75f), EXACT);
    }

    @Test
    void scale_addsAScreenPixelToAFontPixelEachStep() {
        assertEquals(1.5f, TextSizes.scale(2, 1, 1.0f), EXACT, "three screen pixels to a font pixel at GUI scale 2");
        assertEquals(2.0f, TextSizes.scale(2, 2, 1.0f), EXACT);
        assertEquals(1.25f, TextSizes.scale(4, 1, 1.0f), EXACT, "five to one at GUI scale 4");
    }

    @Test
    void scale_keepsTheSmallFontsLettersWhole() {
        // Three quarters of three screen pixels is two and a quarter: two, which at GUI scale 2 is the normal font.
        assertEquals(1.0f, TextSizes.scale(2, 1, 0.75f), EXACT);
        assertEquals(1.5f, TextSizes.scale(2, 2, 0.75f), EXACT);
    }

    @Test
    void scale_isNeverSmallerThanAtTheNormalSize() {
        // Four screen pixels times 0.7 is 2.8, cut to two: two thirds of GUI scale 3, less than 0.7, so 0.7 stays.
        assertEquals(0.7f, TextSizes.scale(3, 1, 0.7f), EXACT);
        assertEquals(1.0f, TextSizes.scale(1, 1, 0.75f), EXACT, "two screen pixels times three quarters is one");
    }

    @Test
    void stepsOf_readsTheWordsAndFallsBackToTheNormal() {
        assertEquals(0, TextSizes.stepsOf(TextSizes.NORMAL));
        assertEquals(1, TextSizes.stepsOf(TextSizes.LARGER));
        assertEquals(2, TextSizes.stepsOf(TextSizes.LARGEST));
        assertEquals(0, TextSizes.stepsOf("huge"));
    }

    @Test
    void grown_roundsToWholePixels() {
        assertEquals(9, TextSizes.grown(9, 1.0f));
        assertEquals(14, TextSizes.grown(9, 1.5f));
    }
}
