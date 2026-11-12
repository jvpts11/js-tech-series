/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.os.defrag.DefragLook;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything of the Frames' Defrag windows stands, by edition: Frames 95's map of blocks with its bar and four
 * buttons, XP's volumes, two bars, five buttons and legend, 7's schedule and list of disks, and the Optimize Drives of
 * 10 and 11. Pure, so the window draws from it and a test holds it to account.
 */
public final class DefragLayout {

    /** How wide every edition's window is, and how tall each one is. */
    public static final int W = 300;
    public static final int H_95 = 225;
    public static final int H_XP = 228;
    public static final int H_7 = 194;
    public static final int H_10 = 206;
    /** The step from one line of text to the next, and the height of a button. */
    public static final int ROW = 10;
    public static final int BUTTON_H = 14;
    /** A list's rows: how far apart, and how many show. */
    public static final int LIST_ROW = 9;
    public static final int LIST_ROWS = 3;

    /* Frames 95: the map of blocks, the line and bar under it, and the four buttons in two rows beside them. */
    public static final int MAP_X = 5;
    public static final int MAP_Y = 5;
    public static final int MAP_COLS = 50;
    public static final int MAP_ROWS = 30;
    public static final int MAP_CELL = 5;
    public static final int F95_LINE_Y = 172;
    public static final int F95_BAR_Y = 183;
    public static final int F95_BAR_W = 140;
    public static final int F95_PERCENT_Y = 197;
    public static final int F95_COUNTS_Y = 211;
    public static final int F95_BUTTON_W = 60;
    public static final int[] F95_BUTTON_X = {152, 216, 152, 216};
    public static final int[] F95_BUTTON_Y = {171, 171, 189, 189};

    /* Frames XP: the menu line, the volumes, the two bars, the buttons, the legend, the report and the status bar. */
    public static final int XP_MENU_Y = 3;
    public static final int XP_LIST_Y = 14;
    public static final int XP_LIST_H = 45;
    public static final int[] XP_COLUMNS = {5, 46, 130, 190, 245};
    public static final int XP_BEFORE_Y = 63;
    public static final int XP_BEFORE_BAR_Y = 73;
    public static final int XP_AFTER_Y = 89;
    public static final int XP_AFTER_BAR_Y = 99;
    public static final int XP_BAR_H = 12;
    public static final int XP_BUTTONS_Y = 115;
    public static final int[] XP_BUTTON_X = {2, 56, 122, 170, 214};
    public static final int[] XP_BUTTON_W = {50, 62, 44, 40, 66};
    public static final int XP_LEGEND_Y = 136;
    public static final int[] XP_LEGEND_X = {2, 100, 198};
    public static final int XP_REPORT_Y = 162;
    public static final int XP_STATUS_Y = 214;

    /* Frames 7: the words, the schedule and its button, the list of disks, the result, the buttons, Close. */
    public static final int F7_SCHEDULE_Y = 28;
    public static final int F7_SCHEDULE_BUTTON_X = 196;
    public static final int F7_SCHEDULE_BUTTON_Y = 38;
    public static final int F7_SCHEDULE_BUTTON_W = 100;
    public static final int F7_STATUS_Y = 66;
    public static final int F7_LIST_Y = 77;
    public static final int[] F7_COLUMNS = {8, 100, 196};
    public static final int F7_RESULT_Y = 120;
    public static final int F7_BUTTONS_Y = 143;
    public static final int[] F7_BUTTON_X = {4, 78};
    public static final int[] F7_BUTTON_W = {70, 82};

    /* Frames 10 and 11: the words, the list of drives, the result, the buttons, the schedule, Close. */
    public static final int F10_STATUS_Y = 40;
    public static final int F10_LIST_Y = 51;
    public static final int[] F10_COLUMNS = {8, 70, 130, 210};
    public static final int F10_RESULT_Y = 94;
    public static final int F10_BUTTONS_Y = 118;
    public static final int[] F10_BUTTON_X = {172, 232};
    public static final int[] F10_BUTTON_W = {56, 64};
    public static final int F10_SCHEDULE_Y = 140;
    public static final int F10_SCHEDULE_BUTTON_X = 210;
    public static final int F10_SCHEDULE_BUTTON_W = 86;

    /** Every list's frame: its left edge, its width, and how tall it is with a header and three rows. */
    public static final int LIST_X = 4;
    public static final int LIST_W = 292;
    public static final int LIST_H = 38;
    /** Close, at the foot on the right. */
    public static final int CLOSE_X = 252;
    public static final int CLOSE_W = 44;

    private DefragLayout() {
    }

    /** How tall the window of {@code look} is. */
    public static int height(final DefragLook look) {
        return switch (look) {
            case FRAMES_95 -> H_95;
            case FRAMES_XP -> H_XP;
            case FRAMES_7 -> H_7;
            default -> H_10;
        };
    }

    /** The window of {@code look} as a layout, its solid parts and the longest of its words, for a test to check. */
    public static GuiLayout layout(final DefragLook look) {
        final GuiLayout l = new GuiLayout(W, height(look));
        switch (look) {
            case FRAMES_95 -> {
                l.box("map", MAP_X - 1, MAP_Y - 1, MAP_COLS * MAP_CELL + 2, MAP_ROWS * MAP_CELL + 2);
                l.box("bar", MAP_X, F95_BAR_Y, F95_BAR_W, 10);
                for (int i = 0; i < F95_BUTTON_X.length; i++) {
                    l.box("button" + i, F95_BUTTON_X[i], F95_BUTTON_Y[i], F95_BUTTON_W, BUTTON_H);
                }
                l.text("counts", MAP_X, F95_COUNTS_Y, 48, 1.0f);
            }
            case FRAMES_XP -> {
                l.box("list", 2, XP_LIST_Y, W - 4, XP_LIST_H);
                l.box("before", 2, XP_BEFORE_BAR_Y, W - 4, XP_BAR_H);
                l.box("after", 2, XP_AFTER_BAR_Y, W - 4, XP_BAR_H);
                for (int i = 0; i < XP_BUTTON_X.length; i++) {
                    l.box("button" + i, XP_BUTTON_X[i], XP_BUTTONS_Y, XP_BUTTON_W[i], BUTTON_H);
                }
                l.box("status", 0, XP_STATUS_Y, W, BUTTON_H);
                l.text("report", 2, XP_REPORT_Y, 48, 1.0f);
            }
            case FRAMES_7 -> {
                l.box("schedule", F7_SCHEDULE_BUTTON_X, F7_SCHEDULE_BUTTON_Y, F7_SCHEDULE_BUTTON_W, BUTTON_H);
                l.box("list", LIST_X, F7_LIST_Y, LIST_W, LIST_H);
                for (int i = 0; i < F7_BUTTON_X.length; i++) {
                    l.box("button" + i, F7_BUTTON_X[i], F7_BUTTONS_Y, F7_BUTTON_W[i], BUTTON_H);
                }
                l.box("close", CLOSE_X, H_7 - BUTTON_H - 4, CLOSE_W, BUTTON_H);
                l.text("result", LIST_X, F7_RESULT_Y, 48, 1.0f);
            }
            default -> {
                l.box("list", LIST_X, F10_LIST_Y, LIST_W, LIST_H);
                for (int i = 0; i < F10_BUTTON_X.length; i++) {
                    l.box("button" + i, F10_BUTTON_X[i], F10_BUTTONS_Y, F10_BUTTON_W[i], BUTTON_H);
                }
                l.box("schedule", F10_SCHEDULE_BUTTON_X, F10_SCHEDULE_Y - 3, F10_SCHEDULE_BUTTON_W, BUTTON_H);
                l.box("close", CLOSE_X, H_10 - BUTTON_H - 4, CLOSE_W, BUTTON_H);
                l.text("result", LIST_X, F10_RESULT_Y, 48, 1.0f);
            }
        }
        return l;
    }
}
