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
 * Where CDE's Application Manager puts things inside its window: a strip across the head that says which group
 * is open, and under it a well of icons, each a picture with its name below, filled row by row.
 *
 * <p>Everything is measured from the top left of the window's content. The drawing and the click handling both
 * ask here, so the icon a player sees and the one they hit are the same rectangle.
 */
public final class CdeAppManagerLayout {

    /** The strip that names the open group; the view of the groups themselves has none. */
    public static final int HEAD_H = 13;

    public static final int ICON = 24;
    public static final int NAME_H = 9;

    private static final int CELL_W = 66;
    private static final int CELL_H = 42;
    private static final int PAD = 4;
    private static final int HEAD_GAP = 3;

    private CdeAppManagerLayout() {
    }

    /** The strip across the head of a content area that wide. */
    public static Rect head(final int width) {
        return new Rect(0, 0, width, HEAD_H);
    }

    /** The well the icons stand in, under the strip when there is one. */
    public static Rect well(final boolean headed, final int width, final int height) {
        final int top = headed ? HEAD_H + HEAD_GAP : 0;
        return new Rect(0, top, width, Math.max(0, height - top));
    }

    /** How many icons one row of the well holds. */
    public static int perRow(final int width) {
        return Math.max(1, (width - PAD * 2) / CELL_W);
    }

    /** Everything that belongs to icon {@code index}: its picture and the name under it. */
    public static Rect cell(final int index, final boolean headed, final int width, final int height) {
        final Rect well = well(headed, width, height);
        final int perRow = perRow(width);
        return new Rect(well.x() + PAD + (index % perRow) * CELL_W, well.y() + PAD + (index / perRow) * CELL_H,
                CELL_W, ICON + 2 + NAME_H);
    }

    /** The picture of icon {@code index}, centred over its name. */
    public static Rect picture(final int index, final boolean headed, final int width, final int height) {
        final Rect cell = cell(index, headed, width, height);
        return new Rect(cell.x() + (CELL_W - ICON) / 2, cell.y(), ICON, ICON);
    }

    /** The icon under that point out of {@code count}, or -1 when the point is on none of them. */
    public static int indexAt(final double px, final double py, final int count, final boolean headed,
                              final int width, final int height) {
        for (int i = 0; i < count; i++) {
            if (cell(i, headed, width, height).holds(px, py)) {
                return i;
            }
        }
        return -1;
    }

    /** The window's content with that many icons, as solids that may not overlap. */
    public static GuiLayout layout(final int count, final boolean headed, final int width, final int height) {
        final GuiLayout l = new GuiLayout(width, height);
        if (headed) {
            final Rect head = head(width);
            l.box("head", head.x(), head.y(), head.w(), head.h());
        }
        for (int i = 0; i < count; i++) {
            final Rect r = cell(i, headed, width, height);
            l.box("icon_" + i, r.x(), r.y(), r.w(), r.h());
        }
        return l;
    }
}
