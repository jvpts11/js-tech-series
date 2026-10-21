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
 * How a HUD element dragged in the HUD editor settles: an edge near another element's edge goes against it, a little
 * apart, an edge near the screen's side keeps the corners' margin, and otherwise an edge near a line of the editor's
 * grid goes on the line; a size changed by a corner goes in quarter steps.
 *
 * <p>Pure: no game types, so the arithmetic is checked without the game.
 */
public final class HudSnap {

    /** The editor's grid, in the GUI's pixels. */
    public static final int GRID = 16;
    /** How near an edge has to come to be caught. */
    public static final int REACH = 4;
    /** The steps a size goes in. */
    public static final float SCALE_STEP = 0.25F;

    private HudSnap() {
    }

    /**
     * Where a box {@code size} dragged to {@code x}, {@code y} settles among {@code others} on a screen
     * {@code screenWidth} by {@code screenHeight}, kept on the screen.
     */
    public static HudStack.Point snap(final int x, final int y, final HudStack.Size size, final List<Box> others,
                                      final int screenWidth, final int screenHeight) {
        final int left = Math.max(0, Math.min(screenWidth - size.width(), x));
        final int top = Math.max(0, Math.min(screenHeight - size.height(), y));
        return new HudStack.Point(along(left, size.width(), screenWidth, others, true),
                along(top, size.height(), screenHeight, others, false));
    }

    /** {@code scale} in the steps a size goes in, within the sizes an element may have. */
    public static float scale(final float scale) {
        final float stepped = Math.round(scale / SCALE_STEP) * SCALE_STEP;
        return Math.max(HudPlacement.LEAST_SCALE, Math.min(HudPlacement.MOST_SCALE, stepped));
    }

    /* Where a box's near edge at {@code start}, {@code length} long, settles along one axis. */
    private static int along(final int start, final int length, final int screen, final List<Box> others,
                             final boolean across) {
        int best = start;
        int nearest = REACH + 1;
        // The others' edges first: against one, a gap apart, or flush with it.
        for (final Box other : others) {
            final int from = across ? other.x() : other.y();
            final int to = from + (across ? other.width() : other.height());
            for (final int candidate : new int[] {to + HudStack.GAP, from - HudStack.GAP - length, from,
                to - length}) {
                final int distance = Math.abs(candidate - start);
                if (distance < nearest) {
                    nearest = distance;
                    best = candidate;
                }
            }
        }
        // The screen's sides, the corners' margin from them.
        for (final int candidate : new int[] {HudStack.MARGIN, screen - HudStack.MARGIN - length}) {
            final int distance = Math.abs(candidate - start);
            if (distance < nearest) {
                nearest = distance;
                best = candidate;
            }
        }
        if (nearest <= REACH) {
            return Math.max(0, Math.min(screen - length, best));
        }
        // Then the grid, the near edge or the far one on a line.
        final int nearLine = Math.round(start / (float) GRID) * GRID;
        final int farLine = Math.round((start + length) / (float) GRID) * GRID - length;
        final int toNear = Math.abs(nearLine - start);
        final int toFar = Math.abs(farLine - start);
        if (Math.min(toNear, toFar) <= REACH) {
            return Math.max(0, Math.min(screen - length, toNear <= toFar ? nearLine : farLine));
        }
        return start;
    }

    /**
     * A box on the screen, in the GUI's pixels.
     *
     * @param x      its left
     * @param y      its top
     * @param width  how wide
     * @param height how tall
     */
    public record Box(int x, int y, int width, int height) {

        /** Whether the point lies in the box. */
        public boolean contains(final double px, final double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
