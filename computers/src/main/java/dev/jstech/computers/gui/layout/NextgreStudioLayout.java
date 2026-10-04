/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Where everything sits in the Nextgre Planner Studio: the tabs along the top (Explain, Statistics, Rules, History);
 * on the Explain tab the statement with its two buttons, the plan's tree on the left and the plans weighed on the
 * right; the blue status bar at the bottom. Every position is measured from the top left of the window's content.
 *
 * <p>The plan's tree is laid out here too, as arithmetic: each step a box, its parts in a row under it, a box over the
 * middle of the boxes under it, and no two boxes on one row closer than {@link #BOX_GAP}. So whether two boxes would
 * overlap is a question a test answers, not a player.
 *
 * <p>Pure: no game types.
 */
public final class NextgreStudioLayout {

    /** The window at the size the desktop opens it, and at its smallest. */
    public static final int DEFAULT_W = 400;
    public static final int DEFAULT_H = 250;
    public static final int MIN_W = 300;
    public static final int MIN_H = 200;

    public static final int PAD = 4;
    public static final int TABS_Y = 2;
    public static final int TABS_H = 14;
    /** The statement's box on the Explain tab, under the tabs, three lines tall. */
    public static final int QUERY_Y = TABS_Y + TABS_H + 3;
    public static final int QUERY_H = 32;
    public static final int BUTTON_H = 12;
    public static final int BUTTON_W = 66;
    public static final int BUTTON_GAP = 3;
    /** Where the tree and the list beside it begin. */
    public static final int BODY_Y = QUERY_Y + QUERY_H + PAD;
    /** The list of the plans weighed, at the right of the tree. */
    public static final int SIDE_W = 118;
    public static final int STATUS_H = 11;
    public static final int ROW_H = 11;
    /** A tab's list (Statistics, Rules, History) begins under its line of column names. */
    public static final int LIST_Y = TABS_Y + TABS_H + 3;
    public static final int COLUMNS_H = 11;
    /** A rule's row: its name with a box to tick, and what it does under it. */
    public static final int RULE_H = 21;

    /** A step's box in the tree: its name, where, the two bars, the times; a hint's chip under them. */
    public static final int BOX_W = 108;
    public static final int BOX_H = 42;
    public static final int BOX_GAP = 8;
    public static final int LEVEL_GAP = 14;
    public static final int TREE_PAD = 6;

    private static final String TABS = "tabs";
    private static final String QUERY = "query";
    private static final String EXPLAIN = "explain";
    private static final String ANALYZE = "analyze";
    private static final String TREE = "tree";
    private static final String SIDE = "side";
    private static final String STATUS = "status";

    private NextgreStudioLayout() {
    }

    /** The Explain tab at the size the window opens at. */
    public static GuiLayout layout() {
        return layout(DEFAULT_W, DEFAULT_H);
    }

    /** The Explain tab in a window whose content is {@code w} by {@code h}. */
    public static GuiLayout layout(final int w, final int h) {
        final GuiLayout l = new GuiLayout(w, h);
        l.box(TABS, PAD, TABS_Y, w - 2 * PAD, TABS_H);
        l.box(QUERY, PAD, QUERY_Y, queryW(w), QUERY_H);
        l.box(EXPLAIN, buttonsX(w), QUERY_Y, BUTTON_W, BUTTON_H);
        l.box(ANALYZE, buttonsX(w), QUERY_Y + BUTTON_H + BUTTON_GAP, BUTTON_W, BUTTON_H);
        l.box(TREE, PAD, BODY_Y, treeW(w), bodyH(h));
        l.box(SIDE, sideX(w), BODY_Y, SIDE_W, bodyH(h));
        l.box(STATUS, 0, statusY(h), w, STATUS_H);
        return l;
    }

    /** The width of the statement's box, leaving room for the buttons at its right. */
    public static int queryW(final int w) {
        return w - 2 * PAD - BUTTON_W - PAD;
    }

    /** Where the two buttons stand. */
    public static int buttonsX(final int w) {
        return w - PAD - BUTTON_W;
    }

    /** The tree's width, leaving room for the list beside it. */
    public static int treeW(final int w) {
        return w - 3 * PAD - SIDE_W;
    }

    /** Where the list of the plans weighed begins. */
    public static int sideX(final int w) {
        return w - PAD - SIDE_W;
    }

    /** How tall the tree and the list are. */
    public static int bodyH(final int h) {
        return statusY(h) - PAD - BODY_Y;
    }

    /** Where the status bar is. */
    public static int statusY(final int h) {
        return h - STATUS_H;
    }

    /** How tall the list of a tab other than Explain is. */
    public static int listH(final int h) {
        return statusY(h) - PAD - LIST_Y - COLUMNS_H;
    }

    /**
     * Where each box of a tree goes, inside the tree's own area: its left and its top, in the order of
     * {@code parents}, where each entry is the place of the box's parent in that list, or -1 for the top one, and a
     * parent always comes before its children.
     */
    public static List<int[]> tree(final List<Integer> parents) {
        final int n = parents.size();
        final List<List<Integer>> children = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            children.add(new ArrayList<>());
        }
        final int[] depth = new int[n];
        for (int i = 0; i < n; i++) {
            final int parent = parents.get(i);
            if (parent >= 0 && parent < i) {
                children.get(parent).add(i);
                depth[i] = depth[parent] + 1;
            }
        }
        final int[] x = new int[n];
        final int[] next = {TREE_PAD};
        for (int i = 0; i < n; i++) {
            final int parent = parents.get(i);
            if (parent < 0 || parent >= i) {
                place(i, children, x, next);
            }
        }
        final List<int[]> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new int[] {x[i], TREE_PAD + depth[i] * (BOX_H + LEVEL_GAP)});
        }
        return out;
    }

    /** How wide and tall a tree laid out by {@link #tree} is, with its margins. */
    public static int[] treeSize(final List<int[]> boxes) {
        int right = 0;
        int bottom = 0;
        for (final int[] box : boxes) {
            right = Math.max(right, box[0] + BOX_W);
            bottom = Math.max(bottom, box[1] + BOX_H);
        }
        return new int[] {right + TREE_PAD, bottom + TREE_PAD};
    }

    /* Lays out the subtree of {@code node}: its leaves left to right from {@code next}, each parent over its own. */
    private static void place(final int node, final List<List<Integer>> children, final int[] x, final int[] next) {
        final List<Integer> under = children.get(node);
        if (under.isEmpty()) {
            x[node] = next[0];
            next[0] += BOX_W + BOX_GAP;
            return;
        }
        for (final int child : under) {
            place(child, children, x, next);
        }
        final int first = x[under.get(0)];
        final int last = x[under.get(under.size() - 1)];
        x[node] = (first + last) / 2;
    }
}
