/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

/**
 * Room kept on each side of a box, in pixels: its padding inside, or its margin outside.
 *
 * @param top    above
 * @param right  to the right
 * @param bottom below
 * @param left   to the left
 */
public record Insets(int top, int right, int bottom, int left) {

    /** No room on any side. */
    public static final Insets NONE = new Insets(0, 0, 0, 0);

    public Insets {
        if (top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException("room around a box cannot be negative: " + top + ", " + right + ", "
                    + bottom + ", " + left);
        }
    }

    /** The same room on every side. */
    public static Insets of(final int all) {
        return new Insets(all, all, all, all);
    }

    /** {@code vertical} above and below, {@code horizontal} left and right. */
    public static Insets of(final int vertical, final int horizontal) {
        return new Insets(vertical, horizontal, vertical, horizontal);
    }

    /** The room left and right together. */
    public int horizontal() {
        return left + right;
    }

    /** The room above and below together. */
    public int vertical() {
        return top + bottom;
    }
}
