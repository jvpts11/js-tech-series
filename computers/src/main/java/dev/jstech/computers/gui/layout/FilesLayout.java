/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * The geometry of the Files explorer window, in desktop units, kept out of the screen so the pieces
 * that must never overlap (the toolbar's buttons, the address, the search box, the drive tree, the
 * column header, the list and the status bar) are checked by a test instead of by a screenshot.
 *
 * <p>Top to bottom: a toolbar (back, forward, up, the address trail, a search box, the view toggle),
 * then the drive tree on the left beside a column header and the file list, then the status bar.
 */
public final class FilesLayout {

    public static final int DEFAULT_W = 300;
    public static final int DEFAULT_H = 190;
    public static final int MIN_W = 250;
    public static final int MIN_H = 120;

    /** The toolbar row. */
    public static final int TOOL_H = 14;
    /** One navigation button (back, forward, up, view), square-ish, and the gap between them. */
    public static final int NAV_W = 12;
    public static final int NAV_H = 11;
    public static final int NAV_GAP = 1;
    /** The search box on the toolbar's right. */
    public static final int SEARCH_W = 60;
    /** The side of the throbber square. */
    public static final int THROBBER = 12;
    /** The drive tree: wide enough for "Local Disk (C:)" at the desktop font. */
    public static final int TREE_W = 84;
    /** The column header over the list. */
    public static final int COLS_H = 10;
    public static final int STATUS_H = 11;
    /** A row holds a 16-pixel icon with a pixel to spare, the way a details view of small icons always has. */
    public static final int ROW_H = 17;
    public static final int ICON_W = 16;
    /** How far down a row its words start, so they sit in the middle of it beside the icon. */
    public static final int TEXT_DY = 5;
    /** The Type and Size columns, right-aligned in the list. */
    public static final int TYPE_COL_W = 54;
    public static final int SIZE_COL_W = 40;
    /** The context menu. */
    public static final int CTX_W = 96;
    public static final int CTX_ITEM_H = 11;
    /** The properties dialog: a title, five rows of ten and a button row. */
    public static final int PROPS_W = 150;
    public static final int PROPS_H = 82;
    /** Frames 7's command bar under the address row, its rule included, and a button on it. */
    public static final int COMMAND_H = 14;
    public static final int COMMAND_BUTTON_H = 11;
    /** Frames 10's ribbon: its tabs above the address row, and the body a tab drops down over the window. */
    public static final int RIBBON_TABS_H = 12;
    public static final int RIBBON_BODY_H = 34;
    /** A command on the ribbon's body, two stacked in a column, and the caption under its group. */
    public static final int RIBBON_BUTTON_H = 10;
    public static final int RIBBON_CAPTION_H = 8;

    /**
     * The bar a file manager wears besides its toolbar: none, Frames 7's command bar under it, or Frames 10's ribbon
     * tabs above it. What it adds above the toolbar and under it moves everything below.
     */
    public enum Bar {
        NONE(0, 0),
        COMMAND(0, COMMAND_H),
        RIBBON(RIBBON_TABS_H, 0);

        private final int above;
        private final int below;

        Bar(final int above, final int below) {
            this.above = above;
            this.below = below;
        }

        public int above() {
            return above;
        }

        public int below() {
            return below;
        }
    }

    private FilesLayout() {
    }

    /** The toolbar's top: under the ribbon's tabs on the file manager that has them. */
    public static int toolY(final Bar bar) {
        return bar.above();
    }

    /** The command bar's buttons' top, under the toolbar's rule. */
    public static int commandY(final Bar bar) {
        return toolY(bar) + TOOL_H + 1 + (COMMAND_H - 1 - COMMAND_BUTTON_H) / 2;
    }

    /** The ribbon's body, dropped down under its tabs over the toolbar and whatever is below. */
    public static int ribbonBodyY() {
        return RIBBON_TABS_H;
    }

    /** The x of navigation button {@code i}: 0 back, 1 forward, 2 up. */
    public static int navX(final int i) {
        return 2 + i * (NAV_W + NAV_GAP);
    }

    public static int navY() {
        return navY(Bar.NONE);
    }

    public static int navY(final Bar bar) {
        return toolY(bar) + (TOOL_H - NAV_H) / 2;
    }

    /** The view toggle sits at the right edge of the toolbar. */
    public static int viewX(final int width) {
        return viewX(width, false);
    }

    /** The view toggle, left of the throbber on a file manager that has one at the toolbar's right end. */
    public static int viewX(final int width, final boolean throbber) {
        return (throbber ? throbberX(width) - 2 : width - 2) - NAV_W;
    }

    /** The throbber at the right end of the toolbar, on the file managers of the period that had one. */
    public static int throbberX(final int width) {
        return width - 2 - THROBBER;
    }

    public static int throbberY() {
        return (TOOL_H - THROBBER) / 2;
    }

    public static int searchX(final int width) {
        return searchX(width, false);
    }

    public static int searchX(final int width, final boolean throbber) {
        return viewX(width, throbber) - 2 - SEARCH_W;
    }

    public static int addressX() {
        return navX(3) + 1;
    }

    public static int addressW(final int width) {
        return addressW(width, false);
    }

    public static int addressW(final int width, final boolean throbber) {
        return Math.max(20, searchX(width, throbber) - 2 - addressX());
    }

    public static int treeY() {
        return treeY(Bar.NONE);
    }

    public static int treeY(final Bar bar) {
        return bar.above() + TOOL_H + 1 + bar.below();
    }

    public static int treeH(final int height) {
        return treeH(height, Bar.NONE);
    }

    public static int treeH(final int height, final Bar bar) {
        return height - treeY(bar) - STATUS_H - 1;
    }

    public static int listX() {
        return TREE_W + 1;
    }

    public static int listW(final int width) {
        return width - listX();
    }

    public static int colsY() {
        return colsY(Bar.NONE);
    }

    public static int colsY(final Bar bar) {
        return treeY(bar);
    }

    public static int listY() {
        return listY(Bar.NONE);
    }

    public static int listY(final Bar bar) {
        return colsY(bar) + COLS_H;
    }

    public static int listH(final int height) {
        return listH(height, Bar.NONE);
    }

    public static int listH(final int height, final Bar bar) {
        return height - listY(bar) - STATUS_H - 1;
    }

    public static int statusY(final int height) {
        return height - STATUS_H;
    }

    /** How many rows the list shows at {@code height}. */
    public static int visibleRows(final int height) {
        return visibleRows(height, Bar.NONE);
    }

    public static int visibleRows(final int height, final Bar bar) {
        return Math.max(1, (listH(height, bar) - 2) / ROW_H);
    }

    /** The x where the Type column begins, measured from the window's left edge. */
    public static int typeColX(final int width) {
        return typeColX(width, TYPE_COL_W, SIZE_COL_W);
    }

    public static int sizeColX(final int width) {
        return sizeColX(width, SIZE_COL_W);
    }

    /** The same, with the widths the player dragged the columns to. */
    public static int typeColX(final int width, final int typeW, final int sizeW) {
        return width - sizeW - typeW;
    }

    public static int sizeColX(final int width, final int sizeW) {
        return width - sizeW;
    }

    public static int nameMaxW(final int width, final int typeW, final int sizeW) {
        return typeColX(width, typeW, sizeW) - (listX() + 4 + ICON_W + 3) - 3;
    }

    /** The least a column may be dragged down to, so its heading still reads. */
    public static final int MIN_COL_W = 26;

    /** The widest a name may be drawn before it runs into the Type column. */
    public static int nameMaxW(final int width) {
        return typeColX(width) - (listX() + 4 + ICON_W + 3) - 3;
    }

    /** The whole window as solid boxes, so a test proves nothing overlaps at a given size. */
    public static GuiLayout layout(final int width, final int height) {
        return layout(width, height, false);
    }

    /** The same, for a file manager with a throbber at the toolbar's right end or without one. */
    public static GuiLayout layout(final int width, final int height, final boolean throbber) {
        return layout(width, height, throbber, Bar.NONE);
    }

    /** The same, with the bar the file manager wears besides its toolbar. */
    public static GuiLayout layout(final int width, final int height, final boolean throbber, final Bar bar) {
        final GuiLayout l = new GuiLayout(width, height);
        if (bar == Bar.RIBBON) {
            l.box("ribbon-tabs", 0, 0, width, RIBBON_TABS_H);
        }
        for (int i = 0; i < 3; i++) {
            l.box("nav" + i, navX(i), navY(bar), NAV_W, NAV_H);
        }
        l.box("address", addressX(), navY(bar), addressW(width, throbber), NAV_H);
        l.box("search", searchX(width, throbber), navY(bar), SEARCH_W, NAV_H);
        l.box("view", viewX(width, throbber), navY(bar), NAV_W, NAV_H);
        if (throbber) {
            l.box("throbber", throbberX(width), toolY(bar) + throbberY(), THROBBER, THROBBER);
        }
        if (bar == Bar.COMMAND) {
            l.box("command", 0, commandY(bar), width, COMMAND_BUTTON_H);
        }
        l.box("tree", 0, treeY(bar), TREE_W, treeH(height, bar));
        l.box("columns", listX(), colsY(bar), listW(width), COLS_H);
        l.box("list", listX(), listY(bar), listW(width), listH(height, bar));
        l.box("status", 0, statusY(height), width, STATUS_H);
        return l;
    }

    /** One list row as its three columns, so a test proves the name never runs into the Type column. */
    public static GuiLayout columns(final int width) {
        final GuiLayout l = new GuiLayout(width, ROW_H);
        l.box("name-col", listX() + 4, 0, nameMaxW(width) + ICON_W + 3, ROW_H);
        l.box("type-col", typeColX(width), 0, TYPE_COL_W - 2, ROW_H);
        l.box("size-col", sizeColX(width), 0, SIZE_COL_W - 2, ROW_H);
        return l;
    }
}
