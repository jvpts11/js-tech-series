/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * A part of a machine's front a click works, such as a power button or a port: its edges in sixty-fourths of the
 * block's front, counted from its top left as the front is seen, where the machine's model draws it.
 *
 * <p>Free of Minecraft types, so where a click lands is testable without the game.
 *
 * @param left   its left edge
 * @param top    its top edge
 * @param right  its right edge
 * @param bottom its bottom edge
 */
public record FrontArea(int left, int top, int right, int bottom) {

    /*
     * The room round a part a click still reaches: half a pixel, as round an eject button. The parts are a few pixels
     * across, and the server measures the player's aim a moment after it was taken.
     */
    private static final int ROOM = 2;

    /** Whether a point of the front, in sixty-fourths from its top left as seen, lands on the part. */
    public boolean contains(final double x, final double y) {
        return x >= left - ROOM && x <= right + ROOM && y >= top - ROOM && y <= bottom + ROOM;
    }

    /** The middle of the part, across, where a click on it is aimed. */
    public double middleX() {
        return (left + right) / 2.0;
    }

    /** The middle of the part, down. */
    public double middleY() {
        return (top + bottom) / 2.0;
    }
}
