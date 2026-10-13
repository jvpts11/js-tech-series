/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

/** The one rectangle hit test the terminal's tabs and popups share, so the boundary rule lives in one place. */
final class TerminalHit {

    private TerminalHit() {
    }

    /** Whether the point is inside the rectangle: its left and top edges count, its right and bottom do not. */
    static boolean inRect(final double mx, final double my, final int x, final int y, final int w, final int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
