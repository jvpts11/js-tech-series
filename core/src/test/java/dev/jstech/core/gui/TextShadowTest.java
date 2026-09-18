/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A shadow is never the letter's colour, never the ground's, and never a colour foreign to the letter. */
class TextShadowTest {

    /** Letters and grounds the series really writes with: a terminal's, a dark window's, a cream panel's. */
    private static final int[][] WRITTEN = {
        {0xFFCDD6E2, 0xFF000000}, {0xFF39D6C4, 0xFF000000}, {0xFFEF6A5A, 0xFF000000}, {0xFF5A8FD6, 0xFF000000},
        {0xFFD5DAE4, 0xFF1E212A}, {0xFF1B2437, 0xFFFFFFFF}, {0xFF1B2437, 0xFFF2EAD3}, {0xFFFFFFFF, 0xFF1F3A6E},
        {0xFF000000, 0xFFD9D9D9}};

    private static int channel(final int argb, final int shift) {
        return (argb >> shift) & 0xFF;
    }

    @Test
    void of_isNeitherTheLettersColourNorTheGrounds() {
        for (final int[] pair : WRITTEN) {
            final int shadow = TextShadow.of(pair[0], pair[1]);
            assertNotEquals(pair[0], shadow, "the same as the letter");
            assertNotEquals(pair[1] | 0xFF000000, shadow | 0xFF000000, "the same as the ground");
        }
    }

    /** Every channel lies between the letter's and the ground's, so it is made of nothing but the two. */
    @Test
    void of_liesBetweenTheLetterAndTheGroundInEveryChannel() {
        for (final int[] pair : WRITTEN) {
            final int shadow = TextShadow.of(pair[0], pair[1]);
            for (final int shift : new int[] {16, 8, 0}) {
                final int lo = Math.min(channel(pair[0], shift), channel(pair[1], shift));
                final int hi = Math.max(channel(pair[0], shift), channel(pair[1], shift));
                assertTrue(channel(shadow, shift) >= lo && channel(shadow, shift) <= hi,
                        Integer.toHexString(shadow) + " leaves the two it is made of");
            }
        }
    }

    /** It must not pass for a second copy of the letter, and it must still be there to be seen. */
    @Test
    void of_standsApartFromTheLetterAndStillShowsOnTheGround() {
        for (final int[] pair : WRITTEN) {
            final int shadow = TextShadow.of(pair[0], pair[1]);
            final double letterOnGround = ColorContrast.ratio(pair[0], pair[1]);
            final double shadowOnGround = ColorContrast.ratio(shadow, pair[1]);
            assertTrue(shadowOnGround > 1.15, Integer.toHexString(shadow) + " is lost on its ground");
            assertTrue(shadowOnGround < letterOnGround, "the shadow reads louder than the letter it is under");
        }
    }

    @Test
    void of_onADarkGlassIsADimmerToneOfTheLetter_andOnALightPanelAPalerOne() {
        assertTrue(ColorContrast.luminance(TextShadow.of(0xFF39D6C4, 0xFF000000))
                < ColorContrast.luminance(0xFF39D6C4));
        assertTrue(ColorContrast.luminance(TextShadow.of(0xFF1B2437, 0xFFFFFFFF))
                > ColorContrast.luminance(0xFF1B2437));
    }

    @Test
    void of_keepsTheLettersAlpha() {
        assertEquals(0x80, TextShadow.of(0x80FFFFFF, 0xFF000000) >>> 24);
    }
}
