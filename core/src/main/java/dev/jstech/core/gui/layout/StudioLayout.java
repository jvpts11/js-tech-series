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
    public static final int STATE_WIDTH = 44;
    public static final int STATE_HEIGHT = 13;
    public static final int STATE_GAP = 3;
    /** A check's Open. */
    public static final int OPEN_WIDTH = 40;
    public static final int OPEN_HEIGHT = 15;
    /** The foot's buttons, from the right: Done, Run its tests, Reload all. */
    public static final int DONE_WIDTH = 60;
    public static final int RUN_WIDTH = 84;
    public static final int RELOAD_WIDTH = 68;
    public static final int BUTTON_HEIGHT = 18;
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

    /** The studio's parts on a screen {@code width} by {@code height}, held at least to the smallest it is made for. */
    public static Regions of(final int screenWidth, final int screenHeight) {
        final int width = Math.max(MIN_WIDTH, screenWidth);
        final int height = Math.max(MIN_HEIGHT, screenHeight);
        final int tree = clamp((int) Math.round(width * TREE_SHARE), TREE_MIN, TREE_MAX);
        final int parts = clamp((int) Math.round(width * PARTS_SHARE), PARTS_MIN, PARTS_MAX);
        final int middle = width - 2 * PAD - 2 * GAP - tree - parts;
        final int top = BAR + 4;
        final int bottom = height - FOOT - 4;
        final int body = bottom - top;
        final int across = clamp((int) Math.round(body * ACROSS_SHARE), ACROSS_MIN, ACROSS_MAX);
        final Rect bench = new Rect(PAD + tree + GAP, top, middle, body - across - GAP);
        final List<Rect> states = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            states.add(new Rect(bench.right() - 4 - (3 - i) * STATE_WIDTH - (2 - i) * STATE_GAP, bench.y() + 4,
                    STATE_WIDTH, STATE_HEIGHT));
        }
        final int buttonY = height - FOOT + (FOOT - BUTTON_HEIGHT) / 2;
        final Rect done = new Rect(width - PAD - DONE_WIDTH, buttonY, DONE_WIDTH, BUTTON_HEIGHT);
        final Rect run = new Rect(done.x() - GAP - RUN_WIDTH, buttonY, RUN_WIDTH, BUTTON_HEIGHT);
        final Rect reload = new Rect(run.x() - GAP - RELOAD_WIDTH, buttonY, RELOAD_WIDTH, BUTTON_HEIGHT);
        final Rect checks = new Rect(width - PAD - CHECKS_WIDTH, (BAR - FIELD_HEIGHT - 2) / 2, CHECKS_WIDTH,
                FIELD_HEIGHT + 2);
        final Rect search = new Rect(checks.x() - GAP - SEARCH_WIDTH, (BAR - FIELD_HEIGHT) / 2, SEARCH_WIDTH,
                FIELD_HEIGHT);
        return new Regions(width, height, new Rect(0, 0, width, BAR), search, checks,
                new Rect(PAD, top, tree, body), bench, List.copyOf(states),
                new Rect(bench.x(), bench.bottom() + GAP, middle, across),
                new Rect(bench.right() + GAP, top, parts, body), new Rect(0, height - FOOT, width, FOOT),
                reload, run, done);
    }

    /** Where a check's Open stands, on the row that starts {@code rowY} down the screen. */
    public static Rect open(final Regions regions, final int rowY) {
        return new Rect(regions.across().right() - 4 - OPEN_WIDTH, rowY + (CHECK_ROW - OPEN_HEIGHT) / 2 - 1,
                OPEN_WIDTH, OPEN_HEIGHT);
    }

    /** The parts as the overlap check reads them, the bar's and the foot's words among them. */
    public static GuiLayout layout(final int screenWidth, final int screenHeight) {
        final Regions regions = of(screenWidth, screenHeight);
        final GuiLayout layout = new GuiLayout(regions.width(), regions.height());
        layout.text("title", PAD, (BAR - 8) / 2, 6, 1f);
        box(layout, "search", regions.search());
        box(layout, "checks", regions.checks());
        box(layout, "tree", regions.tree());
        box(layout, "bench", regions.bench());
        box(layout, "across", regions.across());
        box(layout, "madeOf", regions.madeOf());
        box(layout, "reload", regions.reload());
        box(layout, "runTests", regions.runTests());
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
     * @param tree     the tree of things
     * @param bench    the bench
     * @param states   the buttons of a machine's states on it: working, idle, jammed
     * @param across   the checks across its parts
     * @param madeOf   what it is made of
     * @param foot     the foot
     * @param reload   Reload all
     * @param runTests Run its tests
     * @param done     Done
     */
    public record Regions(int width, int height, Rect bar, Rect search, Rect checks, Rect tree, Rect bench,
                          List<Rect> states, Rect across, Rect madeOf, Rect foot, Rect reload, Rect runTests,
                          Rect done) {
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
