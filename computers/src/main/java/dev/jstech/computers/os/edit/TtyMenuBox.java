/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

/**
 * Where a terminal editor's menu box sits over the text, and how many of its rows show at once.
 *
 * <p>The box starts at a column measured in the terminal's own cell width, one text row down from the top
 * of what it is drawn over, and never wider or taller than that area. Its items may still be more than the
 * box has room for, so a window of them is kept, slid just far enough that the chosen one always shows.
 *
 * @param x           the box's left edge, from the left edge of the area it is drawn over
 * @param y           the box's top edge, from the top of that area
 * @param width       the box's width
 * @param height      the box's height
 * @param visibleRows how many of the menu's item rows fit inside it
 * @param topRow      the first item row shown
 */
public record TtyMenuBox(int x, int y, int width, int height, int visibleRows, int topRow) {

    /** How many columns of the terminal's own cell width the box starts at. */
    private static final int COLUMN = 24;

    /** The title and the blank row under it, which a real menu draws before its first item. */
    private static final int HEADER_ROWS = 2;

    /**
     * Works out the box for a menu with {@code itemCount} rows, the widest of them (or the title, whichever
     * needs more) being {@code innerWidth} pixels.
     *
     * @param areaWidth  how wide the area under it is
     * @param areaHeight how tall that area is
     * @param cellWidth  the width of one column of the terminal's font, in pixels
     * @param textRow    the height of one row of the file's text, one of which the box sits below
     * @param pad        the padding this glass draws everything with
     * @param itemRow    how tall one row of the menu itself is
     * @param innerWidth the widest the title or an item needs, in pixels
     * @param itemCount  how many items the menu has
     * @param selected   which of them is chosen right now
     */
    public static TtyMenuBox of(final int areaWidth, final int areaHeight, final int cellWidth, final int textRow,
                                final int pad, final int itemRow, final int innerWidth, final int itemCount,
                                final int selected) {
        final int width = Math.max(0, Math.min(areaWidth - 2 * pad, innerWidth + 4 * pad));
        final int height = Math.max(0, Math.min(areaHeight - textRow, (itemCount + HEADER_ROWS + 1) * itemRow + pad));
        final int x = Math.max(0, Math.min(COLUMN * cellWidth, areaWidth - width));
        final int y = textRow;
        final int visibleRows = Math.max(1, (height - pad) / itemRow - HEADER_ROWS);
        final int scrolledToSelected = Math.max(0, selected - visibleRows + 1);
        final int topRow = Math.min(scrolledToSelected, Math.max(0, itemCount - visibleRows));
        return new TtyMenuBox(x, y, width, height, visibleRows, topRow);
    }
}
