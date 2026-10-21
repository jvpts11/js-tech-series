/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the studio puts its parts on a screen of any size: the bar along the top with the way to the thing chosen, the
 * search and the count of checks; the tree of things on the left; the bench in the middle, with the buttons of a
 * machine's states, and the checks across its parts under it; what the thing is made of on the right; and the foot,
 * with what is on and the buttons. The side columns keep their room and the middle takes the rest. Pure, so a unit
 * test checks that nothing overlaps at the smallest screen and the largest.
 */
public final class StudioLayout {

    /** The room round the screen's edge and between the columns. */
    public static final int PAD = 6;
    public static final int GAP = 6;
    /** The bar along the top and the foot. */
    public static final int BAR = 22;
    public static final int FOOT = 26;
    /** How tall a row of the tree is, a row of what it is made of and a row of the checks. */
    public static final int TREE_ROW = 12;
    public static final int PART_ROW = 25;
    public static final int CHECK_ROW = 24;
    /** The heads of the panels: how far down their first row starts. */
    public static final int HEAD = 16;
    /** The search and the count of checks in the bar. */
    public static final int SEARCH_WIDTH = 160;
    public static final int CHECKS_WIDTH = 58;
    public static final int FIELD_HEIGHT = 14;
    /** The buttons of a machine's states on the bench. */
    public static final int STATES = 3;
    public static final int STATE_WIDTH = 44;
    public static final int STATE_HEIGHT = 13;
    public static final int STATE_GAP = 3;
    /** The bench's label, small: "On the bench" is 66 pixels of the game's font, drawn at three quarters. */
    public static final int BENCH_LABEL = 52;
    public static final int BENCH_LABEL_HEIGHT = 7;
    /** A check's Open. */
    public static final int OPEN_WIDTH = 40;
    public static final int OPEN_HEIGHT = 15;
    /** The foot's buttons, from the right: Done, Record a test, Run its tests, Reload all. */
    public static final int DONE_WIDTH = 50;
    public static final int RECORD_WIDTH = 84;
    public static final int RUN_WIDTH = 80;
    public static final int RELOAD_WIDTH = 64;
    public static final int BUTTON_HEIGHT = 18;
    /** A tool's tab: the room each side of its label, between two tabs, and its height. */
    public static final int TAB_PAD = 5;
    public static final int TAB_GAP = 2;
    public static final int TAB_HEIGHT = 12;
    /**
     * The smallest screen the studio is laid out for, a window of 1280 by 720 at the GUI scale of 3, and the
     * narrowest and widest its side columns stand.
     */
    public static final int MIN_WIDTH = 426;
    public static final int MIN_HEIGHT = 240;
    public static final int TREE_MIN = 112;
    public static final int TREE_MAX = 170;
    public static final int PARTS_MIN = 140;
    public static final int PARTS_MAX = 210;
    /** The least and the most the checks take of the middle's height. */
    public static final int ACROSS_MIN = 64;
    public static final int ACROSS_MAX = 110;

    private static final double TREE_SHARE = 0.25;
    private static final double PARTS_SHARE = 0.285;
    private static final double ACROSS_SHARE = 0.32;

    private StudioLayout() {
    }

    /**
     * The studio's parts on a screen {@code width} by {@code height}, held at least to the smallest it is made for,
     * its tools' tabs as wide as {@code tabs} says each one's label is, in a row under the bar, or in more rows when
     * one does not hold them.
     */
    public static Regions of(final int screenWidth, final int screenHeight, final List<Integer> tabs) {
        final int width = Math.max(MIN_WIDTH, screenWidth);
        final int height = Math.max(MIN_HEIGHT, screenHeight);
        final List<Rect> tabRects = new ArrayList<>();
        int tabX = PAD;
        int tabY = BAR + 2;
        for (final int label : tabs) {
            final int tabWidth = label + 2 * TAB_PAD;
            if (tabX + tabWidth > width - PAD && tabX > PAD) {
                tabX = PAD;
                tabY += TAB_HEIGHT;
            }
            tabRects.add(new Rect(tabX, tabY, tabWidth, TAB_HEIGHT));
            tabX += tabWidth + TAB_GAP;
        }
        final Rect strip = new Rect(0, BAR + 2, width, tabY + TAB_HEIGHT - (BAR + 2));
        final int tree = clamp((int) Math.round(width * TREE_SHARE), TREE_MIN, TREE_MAX);
        final int parts = clamp((int) Math.round(width * PARTS_SHARE), PARTS_MIN, PARTS_MAX);
        final int middle = width - 2 * PAD - 2 * GAP - tree - parts;
        final int top = strip.bottom() + 4;
        final int bottom = height - FOOT - 4;
        final int body = bottom - top;
        final int across = clamp((int) Math.round(body * ACROSS_SHARE), ACROSS_MIN, ACROSS_MAX);
        final Rect bench = new Rect(PAD + tree + GAP, top, middle, body - across - GAP);
        final Rect label = new Rect(bench.x() + 4, bench.y() + 4, BENCH_LABEL, BENCH_LABEL_HEIGHT);
        // The states stand at the bench's top right, beside its label, or under it when the bench is too narrow.
        final int statesWidth = STATES * STATE_WIDTH + (STATES - 1) * STATE_GAP;
        final int statesY = bench.right() - 4 - statesWidth >= label.right() + GAP ? bench.y() + 4
                : label.bottom() + 2;
        final List<Rect> states = new ArrayList<>();
        for (int i = 0; i < STATES; i++) {
            states.add(new Rect(bench.right() - 4 - (STATES - i) * STATE_WIDTH - (STATES - 1 - i) * STATE_GAP,
                    statesY, STATE_WIDTH, STATE_HEIGHT));
        }
        final int buttonY = height - FOOT + (FOOT - BUTTON_HEIGHT) / 2;
        final Rect done = new Rect(width - PAD - DONE_WIDTH, buttonY, DONE_WIDTH, BUTTON_HEIGHT);
        final Rect record = new Rect(done.x() - GAP - RECORD_WIDTH, buttonY, RECORD_WIDTH, BUTTON_HEIGHT);
        final Rect run = new Rect(record.x() - GAP - RUN_WIDTH, buttonY, RUN_WIDTH, BUTTON_HEIGHT);
        final Rect reload = new Rect(run.x() - GAP - RELOAD_WIDTH, buttonY, RELOAD_WIDTH, BUTTON_HEIGHT);
        final Rect checks = new Rect(width - PAD - CHECKS_WIDTH, (BAR - FIELD_HEIGHT - 2) / 2, CHECKS_WIDTH,
                FIELD_HEIGHT + 2);
        final Rect search = new Rect(checks.x() - GAP - SEARCH_WIDTH, (BAR - FIELD_HEIGHT) / 2, SEARCH_WIDTH,
                FIELD_HEIGHT);
        return new Regions(width, height, new Rect(0, 0, width, BAR), search, checks, strip, List.copyOf(tabRects),
                new Rect(PAD, top, width - 2 * PAD, body), new Rect(PAD, top, tree, body), bench, label,
                List.copyOf(states), new Rect(bench.x(), bench.bottom() + GAP, middle, across),
                new Rect(bench.right() + GAP, top, parts, body), new Rect(0, height - FOOT, width, FOOT),
                reload, run, record, done);
    }

    /**
     * The body split across into columns, {@code shares} of its width each but the last, which takes the rest, with
     * the gap between them: how a tool's page lays out its panels.
     */
    public static List<Rect> columns(final Rect body, final double... shares) {
        final List<Rect> out = new ArrayList<>();
        int x = body.x();
        final int room = body.width() - shares.length * GAP;
        for (final double share : shares) {
            final int wide = (int) Math.round(room * share);
            out.add(new Rect(x, body.y(), wide, body.height()));
            x += wide + GAP;
        }
        out.add(new Rect(x, body.y(), body.right() - x, body.height()));
        return List.copyOf(out);
    }

    /** {@code rect} with its top {@code down} pixels taken off, for a row of buttons over a page's panels. */
    public static Rect below(final Rect rect, final int down) {
        return new Rect(rect.x(), rect.y() + down, rect.width(), rect.height() - down);
    }

    /** Where a check's Open stands, on the row that starts {@code rowY} down the screen. */
    public static Rect open(final Regions regions, final int rowY) {
        return new Rect(regions.across().right() - 4 - OPEN_WIDTH, rowY + (CHECK_ROW - OPEN_HEIGHT) / 2 - 1,
                OPEN_WIDTH, OPEN_HEIGHT);
    }

    /** The parts as the overlap check reads them, the bar's and the foot's words among them. */
    public static GuiLayout layout(final int screenWidth, final int screenHeight, final List<Integer> tabs) {
        final Regions regions = of(screenWidth, screenHeight, tabs);
        final GuiLayout layout = new GuiLayout(regions.width(), regions.height());
        layout.text("title", PAD, (BAR - 8) / 2, 6, 1f);
        box(layout, "search", regions.search());
        box(layout, "checks", regions.checks());
        for (int i = 0; i < regions.tabs().size(); i++) {
            box(layout, "tab" + i, regions.tabs().get(i));
        }
        box(layout, "tree", regions.tree());
        box(layout, "bench", regions.bench());
        box(layout, "across", regions.across());
        box(layout, "madeOf", regions.madeOf());
        box(layout, "reload", regions.reload());
        box(layout, "runTests", regions.runTests());
        box(layout, "record", regions.record());
        box(layout, "done", regions.done());
        layout.text("status", PAD, regions.foot().y() + (FOOT - 6) / 2, 24, 0.75f);
        return layout;
    }

    private static void box(final GuiLayout layout, final String name, final Rect rect) {
        layout.box(name, rect.x(), rect.y(), rect.width(), rect.height());
    }

    private static int clamp(final int value, final int least, final int most) {
        return Math.max(least, Math.min(most, value));
    }

    /**
     * The studio's parts on one screen.
     *
     * @param width    the screen's width the studio is laid out on
     * @param height   its height
     * @param bar      the bar along the top
     * @param search   the search in it
     * @param checks   the count of checks in it
     * @param strip    the strip of the tools' tabs under the bar, in as many rows as they take
     * @param tabs     each tool's tab on it, in their order
     * @param body     what a tool's page has under the strip, down to the foot
     * @param tree     the tree of things
     * @param bench    the bench
     * @param label    the bench's label, at its top left, as wide as its longest language writes it
     * @param states   the buttons of a machine's states on it: working, idle, jammed
     * @param across   the checks across its parts
     * @param madeOf   what it is made of
     * @param foot     the foot
     * @param reload   Reload all
     * @param runTests Run its tests
     * @param record   Record a test
     * @param done     Done
     */
    public record Regions(int width, int height, Rect bar, Rect search, Rect checks, Rect strip, List<Rect> tabs,
                          Rect body, Rect tree, Rect bench, Rect label, List<Rect> states, Rect across, Rect madeOf,
                          Rect foot, Rect reload, Rect runTests, Rect record, Rect done) {
    }

    /** A part's place: its top-left corner, its width and its height. */
    public record Rect(int x, int y, int width, int height) {

        /** Where it ends across. */
        public int right() {
            return this.x + this.width;
        }

        /** Where it ends down. */
        public int bottom() {
            return this.y + this.height;
        }

        /** Whether a point is inside it. */
        public boolean contains(final double px, final double py) {
            return px >= this.x && py >= this.y && px < this.right() && py < this.bottom();
        }

        /** Whether it lies wholly inside {@code other}. */
        public boolean within(final Rect other) {
            return this.x >= other.x && this.y >= other.y && this.right() <= other.right()
                    && this.bottom() <= other.bottom();
        }

        /** Whether it and {@code other} share a pixel. */
        public boolean meets(final Rect other) {
            return this.x < other.right() && other.x < this.right() && this.y < other.bottom()
                    && other.y < this.bottom();
        }
    }
}
