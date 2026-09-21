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
 * Where a trash window puts things, in each of the three looks it takes, measured from the top left of the window's
 * content for content {@code w} by {@code h}.
 *
 * <p>Frames shows the bin as its explorer did: a pane of tasks and details down the left and a list beside it with
 * each file's name, the place it came from and its size. The Linux desktops show it as their file managers do: a bar
 * over the view with the button that empties it, the places down the left, and the files as icons. CDE shows its
 * Trash Can: a menu bar, the objects as icons in a well, and a line under the well counting them.
 *
 * <p>The drawing and the click handling both ask here, so what a player sees and what they hit are the same.
 */
public final class TrashLayout {

    /** The window's first size and the least it can be made, frame and title included. */
    public static final int DEFAULT_W = 360;
    public static final int DEFAULT_H = 180;
    public static final int MIN_W = 330;
    public static final int MIN_H = 150;

    /** The frame and title bar a window adds around its content. */
    public static final int FRAME_W = 8;
    public static final int FRAME_H = 22;

    /** A line of a list, and the picture at its left, which is a file icon's width. */
    public static final int ROW_H = 11;
    public static final int ICON_W = 12;

    /* Frames */
    public static final int PANE_W = 116;
    public static final int BOX_HEAD_H = 12;
    public static final int LINK_H = 10;
    public static final int HEADER_H = 12;
    public static final int SIZE_COL_W = 40;

    /* The Linux desktops */
    public static final int BAR_H = 16;
    public static final int PLACES_W = 76;
    public static final int PLACE_H = 13;

    /* CDE */
    public static final int MENU_H = 13;
    public static final int STATUS_H = 11;

    /** One object of an icon view: its picture drawn twice over, and its name under it. */
    public static final int CELL_W = 56;
    public static final int CELL_H = 36;

    /** How many places the Linux sidebar lists: Home, Desktop and the Trash. */
    public static final int PLACES = 3;

    /** How many menus CDE's menu bar has: File, Selected and View. */
    public static final int MENUS = 3;

    private static final int PAD = 4;
    private static final int BOX_EDGE = 3;
    private static final int BUTTON_W = 70;
    private static final int BUTTON_H = 12;
    private static final int[] MENU_X = {4, 32, 84};
    private static final int[] MENU_W = {26, 50, 30};

    private TrashLayout() {
    }

    // Frames

    /** The box of the bin's tasks: its heading and the two things it can do. */
    public static Rect tasksBox() {
        return new Rect(PAD, PAD, PANE_W - PAD * 2, BOX_HEAD_H + LINK_H * 2 + BOX_EDGE);
    }

    /** Task {@code index} of the tasks box: nought empties the bin, one restores what is selected. */
    public static Rect task(final int index) {
        final Rect box = tasksBox();
        return new Rect(box.x() + PAD, box.y() + BOX_HEAD_H + index * LINK_H, box.w() - PAD * 2, LINK_H);
    }

    /** The box of details under the tasks, which counts what is in the bin. */
    public static Rect detailsBox() {
        final Rect tasks = tasksBox();
        return new Rect(PAD, tasks.y() + tasks.h() + PAD, PANE_W - PAD * 2, BOX_HEAD_H + LINK_H + BOX_EDGE);
    }

    /** The headings of the list's three columns. */
    public static Rect header(final int w) {
        return new Rect(PANE_W, 0, w - PANE_W, HEADER_H);
    }

    /** Where the name of a row is written, past its icon. */
    public static int nameX() {
        return PANE_W + 3 + ICON_W + 3;
    }

    /** Where the column of old places starts. */
    public static int placeX(final int w) {
        return PANE_W + (w - PANE_W - SIZE_COL_W) / 2;
    }

    /** The right edge sizes are written against. */
    public static int sizeRight(final int w) {
        return w - PAD;
    }

    /** The top of list row {@code index}, counted from the first row shown. */
    public static int rowY(final int index) {
        return HEADER_H + 1 + index * ROW_H;
    }

    /** How many rows the list shows at once. */
    public static int rowsShown(final int h) {
        return Math.max(1, (h - HEADER_H - 1) / ROW_H);
    }

    // The Linux desktops

    /** The button over the view that empties the trash. */
    public static Rect emptyButton() {
        return new Rect(PAD, (BAR_H - BUTTON_H) / 2, BUTTON_W, BUTTON_H);
    }

    /** Where the count of what is in the trash is written, beside the button. */
    public static int summaryX() {
        return PAD + BUTTON_W + PAD * 2;
    }

    /** Place {@code index} of the sidebar. */
    public static Rect place(final int index) {
        return new Rect(0, BAR_H + 2 + index * PLACE_H, PLACES_W, PLACE_H);
    }

    /** The view the trashed files are shown in as icons. */
    public static Rect linuxView(final int w, final int h) {
        return new Rect(PLACES_W, BAR_H, w - PLACES_W, h - BAR_H);
    }

    // CDE

    /** The title of menu {@code index} on the menu bar: File, Selected, View. */
    public static Rect menu(final int index) {
        return new Rect(MENU_X[index], 0, MENU_W[index], MENU_H - 1);
    }

    /** The sunken well the objects stand in. */
    public static Rect cdeWell(final int w, final int h) {
        return new Rect(BOX_EDGE, MENU_H + 1, w - BOX_EDGE * 2, h - MENU_H - 1 - STATUS_H - 1);
    }

    /** The top of the line under the well that counts the objects. */
    public static int statusY(final int h) {
        return h - STATUS_H + 2;
    }

    // Icon views

    /** How many objects a row of an icon view holds. */
    public static int columns(final Rect view) {
        return Math.max(1, (view.w() - PAD) / CELL_W);
    }

    /** How many rows of objects an icon view shows at once. */
    public static int rowsShown(final Rect view) {
        return Math.max(1, (view.h() - PAD) / CELL_H);
    }

    /** Object {@code index} of an icon view scrolled down {@code scrollRows} rows. */
    public static Rect cell(final Rect view, final int index, final int scrollRows) {
        final int cols = columns(view);
        return new Rect(view.x() + PAD / 2 + (index % cols) * CELL_W,
                view.y() + PAD / 2 + (index / cols - scrollRows) * CELL_H, CELL_W, CELL_H);
    }

    // What a test checks

    /** Frames' bin as solids that may not overlap, for content that size. */
    public static GuiLayout framesLayout(final int w, final int h) {
        final GuiLayout l = new GuiLayout(w, h);
        add(l, "tasks", tasksBox());
        add(l, "details", detailsBox());
        add(l, "header", header(w));
        l.box("list", PANE_W, HEADER_H + 1, w - PANE_W, rowsShown(h) * ROW_H);
        return l;
    }

    /** The Linux desktops' trash as solids that may not overlap, for content that size. */
    public static GuiLayout linuxLayout(final int w, final int h) {
        final GuiLayout l = new GuiLayout(w, h);
        add(l, "empty", emptyButton());
        for (int i = 0; i < PLACES; i++) {
            add(l, "place_" + i, place(i));
        }
        add(l, "view", linuxView(w, h));
        return l;
    }

    /** CDE's Trash Can as solids that may not overlap, for content that size. */
    public static GuiLayout cdeLayout(final int w, final int h) {
        final GuiLayout l = new GuiLayout(w, h);
        for (int i = 0; i < MENUS; i++) {
            add(l, "menu_" + i, menu(i));
        }
        add(l, "well", cdeWell(w, h));
        l.box("status", PAD, statusY(h), w - PAD * 2, STATUS_H - 2);
        return l;
    }

    private static void add(final GuiLayout l, final String name, final Rect r) {
        l.box(name, r.x(), r.y(), r.w(), r.h());
    }
}
