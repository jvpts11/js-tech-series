/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where CDE stands the icons of the windows that were put away: from the top left of the workspace, in a row
 * that runs to the right and starts again underneath when it is full.
 *
 * <p>The top left is theirs alone. What a player keeps on the workspace is laid out from the right edge on CDE,
 * so the two never meet until a workspace is very full. The drawing and the click handling both ask here, so the
 * icon a player sees and the one they hit are the same rectangle.
 */
public final class CdeWindowIconLayout {

    /** The raised square the program's picture stands on. */
    public static final int TILE = 22;

    /** The strip under the tile where the window's name is written. */
    public static final int NAME_H = 10;

    /** Wide enough for a name like "Style Manager" in the small text, which is most of what a window is called. */
    private static final int CELL_W = 64;
    private static final int CELL_H = 40;
    private static final int NAME_GAP = 2;
    private static final int LEFT = 8;
    private static final int TOP = 8;

    private CdeWindowIconLayout() {
    }

    /** How many icons one row holds on a workspace that wide. */
    public static int perRow(final int width) {
        return Math.max(1, (width - LEFT * 2) / CELL_W);
    }

    /** Everything that belongs to icon {@code index}: its tile and the name under it. */
    public static Rect cell(final int index, final int width, final int top) {
        final int perRow = perRow(width);
        return new Rect(LEFT + (index % perRow) * CELL_W, top + TOP + (index / perRow) * CELL_H, CELL_W,
                TILE + NAME_GAP + NAME_H);
    }

    /** The raised square of icon {@code index}, centred over its name. */
    public static Rect tile(final int index, final int width, final int top) {
        final Rect cell = cell(index, width, top);
        return new Rect(cell.x() + (CELL_W - TILE) / 2, cell.y(), TILE, TILE);
    }

    /** The strip of icon {@code index} its name is written on. */
    public static Rect name(final int index, final int width, final int top) {
        final Rect cell = cell(index, width, top);
        return new Rect(cell.x(), cell.y() + TILE + NAME_GAP, CELL_W, NAME_H);
    }

    /** The icon under that point out of {@code count}, or -1 when the point is on none of them. */
    public static int indexAt(final double px, final double py, final int count, final int width, final int top) {
        if (px < LEFT || py < top + TOP) {
            return -1;
        }
        final int perRow = perRow(width);
        final int col = (int) ((px - LEFT) / CELL_W);
        final int row = (int) ((py - top - TOP) / CELL_H);
        final int index = row * perRow + col;
        return col < perRow && index < count && cell(index, width, top).holds(px, py) ? index : -1;
    }

    /** That many icons as solids that may not overlap, on a workspace that size. */
    public static GuiLayout layout(final int count, final int width, final int height) {
        final GuiLayout l = new GuiLayout(width, height);
        for (int i = 0; i < count; i++) {
            final Rect r = cell(i, width, 0);
            l.box("window_icon_" + i, r.x(), r.y(), r.w(), r.h());
        }
        return l;
    }
}
