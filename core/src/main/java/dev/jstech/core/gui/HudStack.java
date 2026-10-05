/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the HUD elements of one corner of the screen go: stacked from the corner inwards, one under the other in the
 * top corners and one above the other in the bottom ones, each flush with its corner's side, so that any number of
 * them from any number of mods share the corner without one drawing over another.
 *
 * <p>Pure: no game types, so the arithmetic is checked without the game.
 */
public final class HudStack {

    /** From the screen's edges to the first element. */
    public static final int MARGIN = 4;
    /** Between one element and the next. */
    public static final int GAP = 2;

    private HudStack() {
    }

    /** A corner of the screen. */
    public enum Corner {
        TOP_LEFT(false, false),
        TOP_RIGHT(true, false),
        BOTTOM_LEFT(false, true),
        BOTTOM_RIGHT(true, true);

        private final boolean right;
        private final boolean bottom;

        Corner(final boolean right, final boolean bottom) {
            this.right = right;
            this.bottom = bottom;
        }

        public boolean right() {
            return right;
        }

        public boolean bottom() {
            return bottom;
        }
    }

    /**
     * An element's size, in the GUI's pixels.
     *
     * @param width  how wide
     * @param height how tall
     */
    public record Size(int width, int height) {

        public Size {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("a HUD element is no smaller than nothing: " + width + "x" + height);
            }
        }
    }

    /**
     * An element's top-left corner.
     *
     * @param x across from the screen's left edge
     * @param y down from its top edge
     */
    public record Point(int x, int y) {
    }

    /**
     * Where each of {@code sizes} goes in {@code corner} of a screen {@code screenWidth} by {@code screenHeight}, in
     * the order given: the first nearest the corner.
     */
    public static List<Point> place(final Corner corner, final int screenWidth, final int screenHeight,
                                    final List<Size> sizes) {
        final List<Point> points = new ArrayList<>(sizes.size());
        int along = MARGIN;
        for (final Size size : sizes) {
            final int x = corner.right() ? screenWidth - MARGIN - size.width() : MARGIN;
            final int y = corner.bottom() ? screenHeight - along - size.height() : along;
            points.add(new Point(x, y));
            along += size.height() + GAP;
        }
        return points;
    }
}
