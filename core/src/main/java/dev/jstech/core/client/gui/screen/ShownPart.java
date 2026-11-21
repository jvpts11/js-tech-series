/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.screen;

import java.util.Objects;

/**
 * A part a screen draws itself ({@link IShownParts}), in the screen's own pixels: its box, and the point within it a
 * click on it lands at, its middle unless the screen says another, as where it shows over what is drawn in front of it.
 *
 * @param name   what it is called while the screen is open, the same for the same thing
 * @param words  what a player reads on it, or of it
 * @param x      its box's left
 * @param y      its box's top
 * @param width  its box's width
 * @param height its box's height
 * @param pointX where a click on it lands, across
 * @param pointY where a click on it lands, down
 * @param usable whether it takes a click now
 */
public record ShownPart(String name, String words, int x, int y, int width, int height, int pointX, int pointY,
                        boolean usable) {

    public ShownPart {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(words, "words");
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException(name + " has a box " + width + " by " + height);
        }
    }

    /** A part clicked at its box's middle. */
    public static ShownPart of(final String name, final String words, final int x, final int y, final int width,
                               final int height, final boolean usable) {
        return new ShownPart(name, words, x, y, width, height, x + width / 2, y + height / 2, usable);
    }
}
