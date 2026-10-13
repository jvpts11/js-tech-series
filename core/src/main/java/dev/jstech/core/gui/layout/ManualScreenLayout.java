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
 * at the right edge, the buttons on the cover's top edge (the contents, back and forward at its left, the search at
 * its right), the page-turn arrows on its sides, and the search field on the left page. The screen draws from these,
 * and a test checks nothing collides for the sizes a style can ask for.
 *
 * <p>Every x and y is from the top left of the binder, which the screen centres.
 */
public final class ManualScreenLayout {

    /** The vinyl round the pages, at the sides and the foot. */
    public static final int COVER = 7;
    /** The vinyl over the pages, deeper than the rest so the buttons stand on it. */
    public static final int TOP = 10;
    /** The spine between two pages, the rings through it. */
    public static final int GUTTER = 8;
    /** How far the chapter tabs stand out past the cover's right edge. */
    public static final int TAB_OUT = 10;
    /**
     * A tab as it stands when its chapter is not shown; the shown chapter's reaches two pixels further over the page
     * and one further out, the one under the pointer a pixel over the page. Twelve pixels tall, two apart, thirteen
     * fit above the arrow, a chapter of the Guide each.
     */
    public static final int TAB_WIDTH = 16;
    public static final int TAB_HEIGHT = 12;
    public static final int TAB_PITCH = 14;
    public static final int TAB_TOP = 12;
    /** How far into the cover the tabs start: from the page's edge. */
    public static final int TAB_UNDER = COVER;
    public static final int BUTTON_WIDTH = 14;
    public static final int BUTTON_HEIGHT = 9;
    public static final int BUTTON_Y = 0;
    public static final int BUTTON_INSET = 2;
    /** The room between two buttons side by side. */
    public static final int BUTTON_GAP = 2;
    public static final int ARROW_WIDTH = 7;
    public static final int ARROW_HEIGHT = 13;
    /** How far above the binder's foot the arrows stand. */
    public static final int ARROW_RISE = 26;
    public static final int SEARCH_HEIGHT = 14;
    /** Where the search field stands on the left page, under the page's head. */
    public static final int SEARCH_Y = 22;
    /** The closed manual: as wide as one page and its covers, whatever its pages are. */
    public static final int COVER_WIDTH = 180;
    /** How far a closed binder's tabs stand out past its edge. */
    public static final int COVER_TAB_OUT = 11;
    /** A printed thumb index: the tallest a block is, how far from the page's top and foot, and its widths. */
    public static final int THUMB_HEIGHT = 14;
    public static final int THUMB_TOP = 10;
    public static final int THUMB_WIDTH = 6;
    public static final int THUMB_OPEN = 9;
    /** Paper flags: how far from the top the first stands, how far apart, how tall, and how far under the page. */
    public static final int FLAG_TOP = 18;
    public static final int FLAG_PITCH = 22;
    public static final int FLAG_HEIGHT = 16;
    public static final int FLAG_UNDER = 5;

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
        final Rect back = new Rect(contents.x() + BUTTON_WIDTH + BUTTON_GAP, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        final Rect forward = new Rect(back.x() + BUTTON_WIDTH + BUTTON_GAP, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        final Rect search = new Rect(coverRight - COVER - BUTTON_INSET - BUTTON_WIDTH, BUTTON_Y, BUTTON_WIDTH,
                BUTTON_HEIGHT);
        final Rect previous = new Rect(0, height - ARROW_RISE, ARROW_WIDTH, ARROW_HEIGHT);
        final Rect next = new Rect(coverRight - ARROW_WIDTH, height - ARROW_RISE, ARROW_WIDTH, ARROW_HEIGHT);
        final int tabsX = coverRight - TAB_UNDER;
        final int tabs = Math.max(0, (next.y() - TAB_TOP + (TAB_PITCH - TAB_HEIGHT)) / TAB_PITCH);
        return new Geometry(width, height, coverRight, left, right, contents, back, forward, search, previous, next,
                tabsX, tabs);
    }

    /** A chapter's tab, counted from the top. */
    public static Rect tab(final Geometry geometry, final int index) {
        return new Rect(geometry.tabsX(), TAB_TOP + index * TAB_PITCH, TAB_WIDTH, TAB_HEIGHT);
    }

    /**
     * A chapter's block of a thumb index printed on the right page's outer edge, counted from the top: as tall as
     * {@code count} blocks share the page's height, at most {@link #THUMB_HEIGHT}; the open chapter's the widest.
     */
    public static Rect thumb(final Geometry geometry, final int index, final int count, final boolean open) {
        final Rect page = geometry.right() != null ? geometry.right() : geometry.left();
        final int height = Math.min(THUMB_HEIGHT, (page.height() - 2 * THUMB_TOP) / Math.max(1, count));
        final int width = open ? THUMB_OPEN : THUMB_WIDTH;
        return new Rect(page.x() + page.width() - width, page.y() + THUMB_TOP + index * height, width, height - 1);
    }

    /**
     * A chapter's paper flag stuck on the pages' outer edge, counted from the top: standing out past the cover to where
     * a tab would, the open chapter's reaching two pixels further onto the page.
     */
    public static Rect flag(final Geometry geometry, final int index, final boolean open) {
        final int x = geometry.coverRight() - COVER - FLAG_UNDER - (open ? 2 : 0);
        return new Rect(x, FLAG_TOP + index * FLAG_PITCH, geometry.width() - x - (open ? 0 : 1), FLAG_HEIGHT);
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
        box(layout, "back", geometry.back());
        box(layout, "forward", geometry.forward());
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
     * @param contents   the button that turns to the contents
     * @param back       the button that goes back to where the reader came from
     * @param forward    the button that goes forward again after going back
     * @param search     the button that opens the search
     * @param previous   the arrow that turns back a page
     * @param next       the arrow that turns on a page
     * @param tabsX      where the tabs start, under the cover's edge
     * @param tabs       how many tabs fit above the arrow
     */
    public record Geometry(int width, int height, int coverRight, Rect left, Rect right, Rect contents, Rect back,
                           Rect forward, Rect search, Rect previous, Rect next, int tabsX, int tabs) {
    }
}
