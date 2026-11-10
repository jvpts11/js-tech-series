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
 * Where everything of Linux Mint's Software Manager sits (computers/docs/design/operating-systems.md): the bar with
 * the search across the top, and under it the front page (the featured program, the categories and the newest
 * arrivals), a program's own page, or a list of programs, a category's or a search's.
 *
 * <p>Positions are measured from the window's content corner. This class is pure and carries no Minecraft
 * dependency.
 */
public final class SoftwareManagerLayout {

    public static final int WIDTH = 340;
    public static final int HEIGHT = 212;
    /** How tall a line of the game's font is, which every text below is measured with. */
    public static final int LINE = 9;

    /* The bar: back, the search field and the menu. */
    public static final int BAR_H = 17;
    public static final Rect BACK = new Rect(3, 3, 11, 11);
    public static final Rect SEARCH = new Rect(100, 3, 140, 11);
    public static final Rect MENU = new Rect(324, 3, 11, 11);

    /*
     * The front page: the featured program across it, its icon drawn at twice its size, and beside it its name, its
     * house and what it does, each measured from the banner's corner.
     */
    public static final Rect FEATURED = new Rect(7, 22, 326, 42);
    public static final int FEATURED_ICON = 32;
    public static final int FEATURED_ICON_X = 6;
    public static final int FEATURED_ICON_Y = 5;
    public static final int FEATURED_TEXT_X = 46;
    public static final int FEATURED_NAME_Y = 6;
    public static final int FEATURED_HOUSE_Y = 17;
    public static final int FEATURED_LINE_Y = 28;
    public static final int CATEGORIES_LABEL_Y = 70;
    /* The categories, five to a row. */
    public static final int CATEGORY_TOP = 81;
    public static final int CATEGORY_COLUMNS = 5;
    public static final int CATEGORY_W = 63;
    public static final int CATEGORY_H = 14;
    public static final int CATEGORY_GAP = 2;
    public static final int CATEGORY_PITCH = 17;
    /* The newest arrivals, three tiles. */
    public static final int NEWEST_LABEL_Y = 118;
    public static final int NEWEST_TOP = 129;
    public static final int NEWEST_COUNT = 3;
    public static final int NEWEST_W = 106;
    public static final int NEWEST_H = 56;
    public static final int NEWEST_GAP = 4;
    public static final int FOOTER_Y = 199;

    /* A program's page: its icon, its name and house, the button, the facts and what it does. */
    public static final int PAGE_ICON = 32;
    public static final Rect PAGE_ICON_AT = new Rect(10, 24, PAGE_ICON, PAGE_ICON);
    public static final int PAGE_NAME_X = 50;
    public static final int PAGE_NAME_Y = 28;
    public static final int PAGE_HOUSE_Y = 40;
    public static final Rect ACTION = new Rect(270, 30, 60, 14);
    public static final int PAGE_RULE_Y = 62;
    public static final int FACTS_TOP = 68;
    public static final int FACT_PITCH = 11;
    public static final int FACT_COUNT = 5;
    public static final int FACT_LABEL_X = 10;
    public static final int FACT_VALUE_X = 72;
    public static final int ABOUT_TOP = FACTS_TOP + FACT_COUNT * FACT_PITCH + 6;

    /* A list of programs, a category's or a search's: its heading, then a row each. */
    public static final int LIST_HEADING_Y = 22;
    public static final int LIST_TOP = 34;
    public static final int ROW_H = 18;
    public static final int ROWS = 9;
    public static final int ROW_ICON = 16;
    public static final int ROW_TEXT_X = 28;

    private SoftwareManagerLayout() {
    }

    /** The category tile at that place among them, left to right and row by row. */
    public static Rect category(final int index) {
        final int column = index % CATEGORY_COLUMNS;
        final int row = index / CATEGORY_COLUMNS;
        return new Rect(FEATURED.x() + column * (CATEGORY_W + CATEGORY_GAP), CATEGORY_TOP + row * CATEGORY_PITCH,
                CATEGORY_W, CATEGORY_H);
    }

    /** The tile of the newest arrival at that place among them. */
    public static Rect newest(final int index) {
        return new Rect(FEATURED.x() + index * (NEWEST_W + NEWEST_GAP), NEWEST_TOP, NEWEST_W, NEWEST_H);
    }

    /** The row of a program list at that place on the page. */
    public static Rect row(final int index) {
        return new Rect(FEATURED.x(), LIST_TOP + index * ROW_H, FEATURED.w(), ROW_H - 1);
    }

    /** Which row of a program list a point is on, or -1. */
    public static int rowAt(final double px, final double py) {
        for (int i = 0; i < ROWS; i++) {
            if (row(i).contains(px, py)) {
                return i;
            }
        }
        return -1;
    }

    /** Every piece of the front page with ten categories, for a test to see none overlaps another. */
    public static GuiLayout front() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        bar(layout);
        layout.box("featured", FEATURED.x(), FEATURED.y(), FEATURED.w(), FEATURED.h());
        layout.textBox("categories_label", FEATURED.x(), CATEGORIES_LABEL_Y, FEATURED.w(), LINE);
        for (int i = 0; i < 2 * CATEGORY_COLUMNS; i++) {
            final Rect r = category(i);
            layout.box("category_" + i, r.x(), r.y(), r.w(), r.h());
        }
        layout.textBox("newest_label", FEATURED.x(), NEWEST_LABEL_Y, FEATURED.w(), LINE);
        for (int i = 0; i < NEWEST_COUNT; i++) {
            final Rect r = newest(i);
            layout.box("newest_" + i, r.x(), r.y(), r.w(), r.h());
        }
        layout.textBox("footer", FEATURED.x(), FOOTER_Y, FEATURED.w(), LINE);
        return layout;
    }

    /** Every piece of a program's page. */
    public static GuiLayout page() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        bar(layout);
        layout.box("icon", PAGE_ICON_AT.x(), PAGE_ICON_AT.y(), PAGE_ICON_AT.w(), PAGE_ICON_AT.h());
        layout.textBox("name", PAGE_NAME_X, PAGE_NAME_Y, ACTION.x() - PAGE_NAME_X - 6, LINE);
        layout.textBox("house", PAGE_NAME_X, PAGE_HOUSE_Y, ACTION.x() - PAGE_NAME_X - 6, LINE);
        layout.box("action", ACTION.x(), ACTION.y(), ACTION.w(), ACTION.h());
        layout.box("rule", FEATURED.x(), PAGE_RULE_Y, FEATURED.w(), 1);
        for (int i = 0; i < FACT_COUNT; i++) {
            layout.textBox("fact_" + i, FACT_LABEL_X, FACTS_TOP + i * FACT_PITCH, WIDTH - 2 * FACT_LABEL_X, LINE);
        }
        layout.textBox("about", FACT_LABEL_X, ABOUT_TOP, WIDTH - 2 * FACT_LABEL_X, 3 * (LINE + 1));
        return layout;
    }

    /** Every piece of a page that lists programs, every row full. */
    public static GuiLayout list() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        bar(layout);
        layout.textBox("heading", FEATURED.x(), LIST_HEADING_Y, FEATURED.w(), LINE);
        for (int i = 0; i < ROWS; i++) {
            final Rect r = row(i);
            layout.box("row_" + i, r.x(), r.y(), r.w(), r.h());
        }
        return layout;
    }

    private static void bar(final GuiLayout layout) {
        layout.box("back", BACK.x(), BACK.y(), BACK.w(), BACK.h());
        layout.box("search", SEARCH.x(), SEARCH.y(), SEARCH.w(), SEARCH.h());
        layout.box("menu", MENU.x(), MENU.y(), MENU.w(), MENU.h());
    }

    /** A rectangle within the window, measured from its content corner. */
    public record Rect(int x, int y, int w, int h) {

        /** Whether a point measured from the content corner falls inside it. */
        public boolean contains(final double px, final double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        public int right() {
            return x + w;
        }

        public int bottom() {
            return y + h;
        }
    }
}
