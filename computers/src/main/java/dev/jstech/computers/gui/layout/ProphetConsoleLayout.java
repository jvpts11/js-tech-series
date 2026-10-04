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
 * Where everything sits in the Prophet Reactive Console: the tabs along the top (States, Subscriptions, Reactions,
 * Settings); on the States tab the list of states with its column names, then the selected state's detail (its graph
 * at the left, the Operations it set going at the right); the YourIQL prompt with Check and Apply under them; the
 * orange status bar at the bottom. Every position is measured from the top left of the window's content.
 *
 * <p>Pure: no game types.
 */
public final class ProphetConsoleLayout {

    /** The window at the size the desktop opens it, and at its smallest. */
    public static final int DEFAULT_W = 400;
    public static final int DEFAULT_H = 260;
    public static final int MIN_W = 320;
    public static final int MIN_H = 220;

    public static final int PAD = 4;
    public static final int TABS_Y = 2;
    public static final int TABS_H = 14;
    public static final int LIST_Y = TABS_Y + TABS_H + 3;
    public static final int COLUMNS_H = 11;
    public static final int ROW_H = 12;
    /** The detail under the list: the selected state's statement, its graph and its Operations. */
    public static final int DETAIL_H = 74;
    public static final int GRAPH_W_SHARE = 55;
    /** The prompt line: the prompt, the statement, Check and Apply. */
    public static final int EDITOR_H = 13;
    public static final int BUTTON_W = 40;
    public static final int BUTTON_GAP = 3;
    public static final int PROMPT_W = 46;
    public static final int STATUS_H = 11;

    /** The share of the list's width each column takes, in hundredths: state, chip, level, work, last reaction. */
    public static final int[] COLUMN_SHARES = {36, 14, 22, 12, 16};

    private static final String TABS = "tabs";
    private static final String LIST = "list";
    private static final String GRAPH = "graph";
    private static final String OPERATIONS = "operations";
    private static final String PROMPT = "prompt";
    private static final String STATEMENT = "statement";
    private static final String CHECK = "check";
    private static final String APPLY = "apply";
    private static final String STATUS = "status";

    private ProphetConsoleLayout() {
    }

    /** The States tab at the size the window opens at. */
    public static GuiLayout layout() {
        return layout(DEFAULT_W, DEFAULT_H);
    }

    /** The States tab in a window whose content is {@code w} by {@code h}. */
    public static GuiLayout layout(final int w, final int h) {
        final GuiLayout l = new GuiLayout(w, h);
        l.box(TABS, PAD, TABS_Y, w - 2 * PAD, TABS_H);
        l.box(LIST, PAD, LIST_Y, w - 2 * PAD, listH(h));
        l.box(GRAPH, PAD, detailY(h) + 12, graphW(w), DETAIL_H - 14);
        l.box(OPERATIONS, PAD + graphW(w) + PAD, detailY(h) + 12, w - 3 * PAD - graphW(w), DETAIL_H - 14);
        l.box(PROMPT, PAD, editorY(h), PROMPT_W, EDITOR_H);
        l.box(STATEMENT, statementX(), editorY(h), statementW(w), EDITOR_H);
        l.box(CHECK, checkX(w), editorY(h), BUTTON_W, EDITOR_H);
        l.box(APPLY, applyX(w), editorY(h), BUTTON_W, EDITOR_H);
        l.box(STATUS, 0, statusY(h), w, STATUS_H);
        return l;
    }

    /** Where the status bar is. */
    public static int statusY(final int h) {
        return h - STATUS_H;
    }

    /** Where the prompt line is. */
    public static int editorY(final int h) {
        return statusY(h) - PAD - EDITOR_H;
    }

    /** Where the detail of the selected state begins. */
    public static int detailY(final int h) {
        return editorY(h) - PAD - DETAIL_H;
    }

    /** How tall the list is, with its column names. */
    public static int listH(final int h) {
        return detailY(h) - PAD - LIST_Y;
    }

    /** How tall a tab other than States is, down to the prompt line. */
    public static int pageH(final int h) {
        return editorY(h) - PAD - LIST_Y;
    }

    /** How wide the graph is. */
    public static int graphW(final int w) {
        return (w - 3 * PAD) * GRAPH_W_SHARE / 100;
    }

    public static int statementX() {
        return PAD + PROMPT_W + 2;
    }

    public static int statementW(final int w) {
        return checkX(w) - BUTTON_GAP - statementX();
    }

    public static int checkX(final int w) {
        return applyX(w) - BUTTON_GAP - BUTTON_W;
    }

    public static int applyX(final int w) {
        return w - PAD - BUTTON_W;
    }

    /** Where each column of the list begins, across a list {@code width} wide starting at {@code x}. */
    public static int[] columns(final int x, final int width) {
        final int[] out = new int[COLUMN_SHARES.length];
        int at = x;
        for (int i = 0; i < COLUMN_SHARES.length; i++) {
            out[i] = at;
            at += width * COLUMN_SHARES[i] / 100;
        }
        return out;
    }

    /**
     * Where a level falls on a graph {@code height} tall whose top stands for {@code top}: the y counted down from the
     * graph's top, a level above the top drawn at the top.
     */
    public static int levelY(final long level, final long top, final int height) {
        if (top <= 0) {
            return height;
        }
        final long clamped = Math.max(0L, Math.min(level, top));
        return height - (int) (clamped * height / top);
    }

    /** What the top of a graph stands for: a little above the highest of the band and the levels shown. */
    public static long graphTop(final long lower, final long upper, final long highest) {
        final long ceiling = upper == Long.MAX_VALUE ? lower + lower / 2 : upper;
        return Math.max(1L, Math.max(ceiling, highest) * 11 / 10);
    }
}
