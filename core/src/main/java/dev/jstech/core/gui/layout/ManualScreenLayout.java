/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where everything sits on an open manual: the binder's covers round one page or two, the chapter tabs standing out
 * at the right edge, the contents and search buttons on the cover's top edge, the page-turn arrows on its sides, and
 * the search field on the left page. The screen draws from these, and a test checks nothing collides for the sizes a
 * style can ask for.
 *
 * <p>Every x and y is from the top left of the binder, which the screen centres.
 */
public final class ManualScreenLayout {

    /** The vinyl round the pages, at the sides and the foot. */
    public static final int COVER = 7;
    /** The vinyl over the pages, deeper than the rest so the two buttons stand on it. */
    public static final int TOP = 10;
    /** The spine between two pages, the rings through it. */
    public static final int GUTTER = 8;
    /** How far the chapter tabs stand out past the cover's right edge. */
    public static final int TAB_OUT = 10;
    public static final int TAB_WIDTH = 17;
    public static final int TAB_HEIGHT = 22;
    public static final int TAB_PITCH = 26;
    public static final int TAB_TOP = 22;
    public static final int BUTTON_WIDTH = 14;
    public static final int BUTTON_HEIGHT = 8;
    public static final int BUTTON_Y = 1;
    public static final int BUTTON_INSET = 2;
    public static final int ARROW_WIDTH = 5;
    public static final int ARROW_HEIGHT = 9;
    /** How far above the binder's foot the arrows stand. */
    public static final int ARROW_RISE = 22;
    public static final int SEARCH_HEIGHT = 14;
    /** Where the search field stands on the left page, under the page's head. */
    public static final int SEARCH_Y = 22;
    /** The closed manual: as wide as one page and its covers, whatever its pages are. */
    public static final int COVER_WIDTH = 180;
    /** How far a closed binder's tabs stand out past its edge. */
    public static final int COVER_TAB_OUT = 11;

    private ManualScreenLayout() {
    }

    /** The binder for pages of that size, opened as two pages or one, with as many tabs as fit. */
    public static Geometry of(final int pageWidth, final int pageHeight, final boolean spread) {
        final int coverRight = COVER + pageWidth + (spread ? GUTTER + pageWidth : 0) + COVER;
        final int width = coverRight + TAB_OUT;
        final int height = TOP + pageHeight + COVER;
        final Rect left = new Rect(COVER, TOP, pageWidth, pageHeight);
        final Rect right = spread ? new Rect(COVER + pageWidth + GUTTER, TOP, pageWidth, pageHeight) : null;
        final Rect contents = new Rect(COVER + BUTTON_INSET, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        final Rect search = new Rect(coverRight - COVER - BUTTON_INSET - BUTTON_WIDTH, BUTTON_Y, BUTTON_WIDTH,
                BUTTON_HEIGHT);
        final Rect previous = new Rect(1, height - ARROW_RISE, ARROW_WIDTH, ARROW_HEIGHT);
        final Rect next = new Rect(coverRight - ARROW_WIDTH - 1, height - ARROW_RISE, ARROW_WIDTH, ARROW_HEIGHT);
        final int tabsX = coverRight - COVER;
        final int tabs = Math.max(0, (next.y() - 2 - TAB_TOP + (TAB_PITCH - TAB_HEIGHT)) / TAB_PITCH);
        return new Geometry(width, height, coverRight, left, right, contents, search, previous, next, tabsX, tabs);
    }

    /** A chapter's tab, counted from the top. */
    public static Rect tab(final Geometry geometry, final int index) {
        return new Rect(geometry.tabsX(), TAB_TOP + index * TAB_PITCH, TAB_WIDTH, TAB_HEIGHT);
    }

    /** The closed manual, in the middle of where the open one stands, its tabs beside it. */
    public static Rect cover(final Geometry geometry) {
        return new Rect((geometry.coverRight() - COVER_WIDTH - COVER_TAB_OUT) / 2, 0, COVER_WIDTH,
                geometry.height());
    }

    /** A chapter's tab on the closed binder, standing out past its edge. */
    public static Rect coverTab(final Geometry geometry, final int index) {
        final Rect cover = cover(geometry);
        return new Rect(cover.x() + cover.width(), TAB_TOP + index * TAB_PITCH, COVER_TAB_OUT, TAB_HEIGHT);
    }

    /** The search field on the left page, inside its margins. */
    public static Rect searchField(final Geometry geometry, final int margin) {
        final Rect page = geometry.left();
        return new Rect(page.x() + margin, page.y() + SEARCH_Y, page.width() - 2 * margin, SEARCH_HEIGHT);
    }

    /**
     * The binder's solid parts as a layout a test checks: the pages, the buttons, the arrows and every tab, each as
     * drawn. The tabs start at the page's right edge, so they are checked against each other and the arrows as well
     * as the pages.
     */
    public static GuiLayout layout(final Geometry geometry, final int margin) {
        final GuiLayout layout = new GuiLayout(geometry.width(), geometry.height());
        box(layout, "left", geometry.left());
        if (geometry.right() != null) {
            box(layout, "right", geometry.right());
        }
        box(layout, "contents", geometry.contents());
        box(layout, "search", geometry.search());
        box(layout, "previous", geometry.previous());
        box(layout, "next", geometry.next());
        for (int i = 0; i < geometry.tabs(); i++) {
            box(layout, "tab" + i, tab(geometry, i));
        }
        final Rect field = searchField(geometry, margin);
        layout.text("search field", field.x(), field.y(), (field.width() - 4) / 6, 1.0F);
        return layout;
    }

    private static void box(final GuiLayout layout, final String name, final Rect rect) {
        layout.box(name, rect.x(), rect.y(), rect.width(), rect.height());
    }

    /** A rectangle of the binder. */
    public record Rect(int x, int y, int width, int height) {

        /** Whether a point is inside it. */
        public boolean contains(final double px, final double py) {
            return px >= this.x && py >= this.y && px < this.x + this.width && py < this.y + this.height;
        }
    }

    /**
     * The binder laid out.
     *
     * @param width      the whole binder, its tabs included
     * @param height     the whole binder
     * @param coverRight where the cover's right edge is, past which the tabs stand out
     * @param left       the left page, or the only page
     * @param right      the right page, or null for a style of one page
     * @param tabsX      where the tabs start, under the cover's edge
     * @param tabs       how many tabs fit above the arrow
     */
    public record Geometry(int width, int height, int coverRight, Rect left, Rect right, Rect contents, Rect search,
                           Rect previous, Rect next, int tabsX, int tabs) {
    }
}
