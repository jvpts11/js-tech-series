/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

/**
 * The button on a device's front that opens and closes its disc tray, where the device's model draws it: its edges in
 * sixty-fourths of the block's front, counted from the front's top left as the front is seen, and how far behind the
 * front face its own face stands, also in sixty-fourths. A button set back in the front is pressed where the player
 * sees it, not where the look first crosses the face of the block.
 *
 * <p>Free of Minecraft types, so where a click lands is testable without the game.
 *
 * @param left   its left edge
 * @param top    its top edge
 * @param right  its right edge
 * @param bottom its bottom edge
 * @param depth  how far behind the front face its own face stands
 */
public record EjectButton(int left, int top, int right, int bottom, int depth) {

    /** The front is measured this many units across and down. */
    public static final int FRONT = 64;
    /*
     * The room round the button a click still presses: half a pixel. The button is a sliver a pixel or two across, and
     * the server measures the player's aim a moment after it was taken, never quite where it was.
     */
    private static final int ROOM = 2;

    /** Whether a point of the front, in sixty-fourths from its top left as seen, presses the button. */
    public boolean pressedAt(final double x, final double y) {
        return x >= left - ROOM && x <= right + ROOM && y >= top - ROOM && y <= bottom + ROOM;
    }
}
