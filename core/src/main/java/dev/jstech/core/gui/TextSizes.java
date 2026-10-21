/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import java.util.List;

/**
 * The size of the text on the series' screens apart from the GUI scale, in whole steps: at the normal size the text is
 * drawn as the game draws it, as many screen pixels to a font pixel as the GUI scale; each step larger is one screen
 * pixel more to a font pixel, so its letters stay sharp, never a pixel and a quarter. The icons, frames and the game's
 * own widgets keep the GUI scale; what holds text grows with it. Pure, so a unit test checks the arithmetic.
 */
public final class TextSizes {

    /** The sizes a player picks from, by the words of their setting, the normal one first. */
    public static final String NORMAL = "normal";
    public static final String LARGER = "larger";
    public static final String LARGEST = "largest";
    public static final List<String> WORDS = List.of(NORMAL, LARGER, LARGEST);

    private TextSizes() {
    }

    /** How many steps larger than the normal the size {@code word} is; the normal for a word it does not know. */
    public static int stepsOf(final String word) {
        return Math.max(0, WORDS.indexOf(word));
    }

    /**
     * The scale to draw a text at that the screen would draw at {@code scale} of the GUI scale {@code guiScale},
     * {@code steps} steps larger: its screen pixels to a font pixel one more each step, cut to a whole number so the
     * letters stay sharp, and never smaller than at the normal size. A text of the small font, three quarters of the
     * normal, is two screen pixels to a font pixel one step larger at GUI scale 2, not two and a quarter.
     */
    public static float scale(final int guiScale, final int steps, final float scale) {
        if (steps <= 0 || guiScale <= 0) {
            return scale;
        }
        final double pixels = Math.max(1.0, Math.floor((guiScale + steps) * (double) scale + 1e-6));
        return Math.max(scale, (float) (pixels / guiScale));
    }

    /** How many pixels of the GUI a length of {@code length} at the normal size takes at {@code scale}, whole. */
    public static int grown(final int length, final float scale) {
        return Math.round(length * scale);
    }
}
