/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

/**
 * Where the player put one HUD element in the HUD editor, and how big and whether it shows: held from the corner of
 * the screen nearest it, so it stays by that corner whatever the size of the window.
 *
 * <p>Pure: no game types, so the arithmetic is checked without the game.
 *
 * @param corner the corner it is held from
 * @param x      from that corner's side to the element's nearest side, inwards
 * @param y      from that corner's top or bottom to the element's nearest edge, inwards
 * @param scale  how big, against its own size
 * @param hidden whether the player hid it
 */
public record HudPlacement(HudStack.Corner corner, int x, int y, float scale, boolean hidden) {

    /** The smallest and the largest an element is drawn, against its own size. */
    public static final float LEAST_SCALE = 0.5F;
    public static final float MOST_SCALE = 3.0F;

    public HudPlacement {
        scale = Math.max(LEAST_SCALE, Math.min(MOST_SCALE, scale));
        x = Math.max(0, x);
        y = Math.max(0, y);
    }

    /**
     * The placement of an element whose top left the player left at {@code left}, {@code top}, drawn {@code size}
     * on a screen {@code screenWidth} by {@code screenHeight}: held from the corner nearest its middle, kept on the
     * screen.
     */
    public static HudPlacement at(final int left, final int top, final HudStack.Size size, final int screenWidth,
                                  final int screenHeight, final float scale, final boolean hidden) {
        final int x = Math.max(0, Math.min(screenWidth - size.width(), left));
        final int y = Math.max(0, Math.min(screenHeight - size.height(), top));
        final boolean right = x + size.width() / 2 > screenWidth / 2;
        final boolean bottom = y + size.height() / 2 > screenHeight / 2;
        final HudStack.Corner corner = right ? bottom ? HudStack.Corner.BOTTOM_RIGHT : HudStack.Corner.TOP_RIGHT
                : bottom ? HudStack.Corner.BOTTOM_LEFT : HudStack.Corner.TOP_LEFT;
        return new HudPlacement(corner, right ? screenWidth - x - size.width() : x,
                bottom ? screenHeight - y - size.height() : y, scale, hidden);
    }

    /** Where the element's top left is on a screen {@code screenWidth} by {@code screenHeight}, drawn {@code size}. */
    public HudStack.Point topLeft(final int screenWidth, final int screenHeight, final HudStack.Size size) {
        final int left = corner.right() ? screenWidth - x - size.width() : x;
        final int top = corner.bottom() ? screenHeight - y - size.height() : y;
        return new HudStack.Point(Math.max(0, Math.min(screenWidth - size.width(), left)),
                Math.max(0, Math.min(screenHeight - size.height(), top)));
    }

    /** The element's own {@code size} at this placement's scale. */
    public HudStack.Size scaled(final HudStack.Size size) {
        return new HudStack.Size(Math.round(size.width() * scale), Math.round(size.height() * scale));
    }

    /** The same element shown or hidden. */
    public HudPlacement withHidden(final boolean value) {
        return new HudPlacement(corner, x, y, scale, value);
    }
}
