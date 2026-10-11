/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

/**
 * A desktop program that shows the player's real inventory inside its window: a framed band of the 36
 * container slots, laid out by the desktop over the app's content each frame. The app paints the slot
 * backgrounds where it says the cells are; the desktop draws the items and drives the cursor, drag and
 * shift-click through the vanilla container, so what the player carries is the same stack everywhere.
 *
 * <p>Every place is asked for the content area's size this frame, as the window will hand it to the program to
 * draw: a size the program kept from the frame before lags a window being resized by a frame, and for that frame the
 * items stood where their cells had been.
 */
public interface IInventoryBandApp extends IDesktopApp {

    /** Claims this window as the live instance its server replies are routed to; called when it comes to the front. */
    void markActive();

    /**
     * The content-local x of inventory column {@code col}'s cell, where the item is drawn, for a content area that
     * size.
     */
    int invCellContentX(int col, int contentWidth, int contentHeight);

    /** The content-local y of inventory row {@code row}'s cell, for a content area that size. */
    int invCellContentY(int row, int contentWidth, int contentHeight);

    /** The content-local y just past the hotbar row, for a content area that size. */
    int invBandBottom(int contentWidth, int contentHeight);

    /**
     * The content-local y where the band's slots begin, for a content area that size. A row placed above it is
     * folded away (the band can show fewer rows than the inventory has) and goes inert.
     */
    default int invBandTop(final int contentWidth, final int contentHeight) {
        return invCellContentY(0, contentWidth, contentHeight);
    }
}
