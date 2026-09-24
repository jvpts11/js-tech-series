/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

/**
 * The colour of the shadow under a piece of text, worked out from the letter and from what it is written on.
 *
 * <p>The game shadows its text with a darker copy of the letter, which works on the one kind of ground the
 * game has. The screens of this series are written on grounds of every colour, light ones included, and a
 * dark copy of a dark letter on a cream panel is a smear and not a shadow. So the shadow is the letter's own
 * colour most of the way to the ground's: it is never the letter's colour, never the ground's, and never a
 * colour that is foreign to the letter, because it is made of nothing but the two. On a dark glass it comes out
 * a dimmer tone of the letter; on a light panel it comes out a pale one.
 *
 * <p>Pure colour arithmetic over ARGB ints, so a palette can be held to it in a test.
 */
public final class TextShadow {

    /**
     * How far from the letter towards the ground the shadow sits. Far enough that it cannot be mistaken for a
     * second copy of the letter, and short enough of the ground that it is still there to be seen.
     */
    private static final double TOWARDS_THE_GROUND = 0.66;

    private TextShadow() {
    }

    /**
     * The shadow for text of that colour on a ground of that one.
     *
     * @param text   the letter's colour, whose alpha the shadow keeps
     * @param ground the colour of what the text is written on; its alpha is ignored
     */
    public static int of(final int text, final int ground) {
        final int r = between((text >> 16) & 0xFF, (ground >> 16) & 0xFF);
        final int g = between((text >> 8) & 0xFF, (ground >> 8) & 0xFF);
        final int b = between(text & 0xFF, ground & 0xFF);
        return text >>> 24 << 24 | (r << 16) | (g << 8) | b;
    }

    private static int between(final int letter, final int ground) {
        return (int) Math.round(letter + (ground - letter) * TOWARDS_THE_GROUND);
    }
}
