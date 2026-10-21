/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.live;

/**
 * The part of a block's face a screen covers, in shares of the face as it is seen from in front (0 to 1 across from
 * its left and down from its top), and how many pixels the screen's picture has across and down.
 *
 * @param left   where it starts across
 * @param top    where it starts down
 * @param right  where it ends across
 * @param bottom where it ends down
 * @param width  the picture's pixels across
 * @param height the picture's pixels down
 */
public record TouchArea(double left, double top, double right, double bottom, int width, int height) {

    public TouchArea {
        if (right <= left || bottom <= top || width < 1 || height < 1) {
            throw new IllegalArgumentException("A screen covers some of a face and has a pixel, not " + left + ", "
                    + top + " to " + right + ", " + bottom + " at " + width + " by " + height);
        }
    }

    /** A screen on the whole face, {@code width} by {@code height} pixels. */
    public static TouchArea whole(final int width, final int height) {
        return new TouchArea(0, 0, 1, 1, width, height);
    }
}
