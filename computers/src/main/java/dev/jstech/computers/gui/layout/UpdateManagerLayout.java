/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.layout.SoftwareManagerLayout.Rect;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything of Linux Mint's Update Manager sits (computers/docs/design/operating-systems.md): its toolbar, the
 * list of updates with their columns, what the update picked out is, and the bar along the foot.
 *
 * <p>Positions are measured from the window's content corner. This class is pure and carries no Minecraft
 * dependency.
 */
public final class UpdateManagerLayout {

    public static final int WIDTH = 310;
    public static final int HEIGHT = 186;
    public static final int LINE = SoftwareManagerLayout.LINE;

    /* The toolbar: Clear, Select All, Refresh and Install Updates, each as wide as its word. */
    public static final int TOOLBAR_H = 20;
    public static final int BUTTON_Y = 4;
    public static final int BUTTON_H = 12;
    public static final int BUTTON_PAD = 10;
    public static final int BUTTON_GAP = 4;
    public static final int BUTTONS_X = 5;

    /* The list's columns, and its rows. */
    public static final int HEADS_Y = 25;
    public static final int RULE_Y = 35;
    public static final int CHECK_X = 7;
    public static final int CHECK = 7;
    public static final int ICON_X = 17;
    public static final int ICON = 16;
    public static final int NAME_X = 36;
    public static final int NEW_X = 170;
    public static final int INSTALLED_X = 218;
    public static final int SIZE_X = 266;
    public static final int ROW_TOP = 38;
    public static final int ROW_H = 18;
    public static final int ROWS = 5;

    /* What the update picked out is, under the list. */
    public static final int ABOUT_RULE_Y = ROW_TOP + ROWS * ROW_H + 2;
    public static final int ABOUT_TITLE_Y = ABOUT_RULE_Y + 4;
    public static final int ABOUT_TEXT_Y = ABOUT_TITLE_Y + LINE + 2;

    /* The bar along the foot, which counts what is ticked. */
    public static final Rect STATUS = new Rect(0, HEIGHT - 14, WIDTH, 14);

    private UpdateManagerLayout() {
    }

    /** The toolbar's buttons, left to right, each as wide as its word and the pad. */
    public static Rect[] buttons(final int[] wordWidths) {
        final Rect[] out = new Rect[wordWidths.length];
        int x = BUTTONS_X;
        for (int i = 0; i < wordWidths.length; i++) {
            final int w = wordWidths[i] + BUTTON_PAD;
            out[i] = new Rect(x, BUTTON_Y, w, BUTTON_H);
            x += w + BUTTON_GAP;
        }
        return out;
    }

    /** The row of the list at that place. */
    public static Rect row(final int index) {
        return new Rect(5, ROW_TOP + index * ROW_H, WIDTH - 10, ROW_H - 1);
    }

    /** The tick box of the row at that place. */
    public static Rect check(final int index) {
        return new Rect(CHECK_X, ROW_TOP + index * ROW_H + 5, CHECK, CHECK);
    }

    /** Which row a point is on, or -1. */
    public static int rowAt(final double px, final double py) {
        for (int i = 0; i < ROWS; i++) {
            if (row(i).contains(px, py)) {
                return i;
            }
        }
        return -1;
    }

    /** Every piece of the window with every row full, and buttons as wide as the words given. */
    public static GuiLayout layout(final int[] wordWidths) {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        final Rect[] buttons = buttons(wordWidths);
        for (int i = 0; i < buttons.length; i++) {
            layout.box("button_" + i, buttons[i].x(), buttons[i].y(), buttons[i].w(), buttons[i].h());
        }
        layout.textBox("heads", CHECK_X, HEADS_Y, WIDTH - 2 * CHECK_X, LINE);
        layout.box("rule", 5, RULE_Y, WIDTH - 10, 1);
        for (int i = 0; i < ROWS; i++) {
            final Rect r = row(i);
            layout.box("row_" + i, r.x(), r.y(), r.w(), r.h());
        }
        layout.box("about_rule", 5, ABOUT_RULE_Y, WIDTH - 10, 1);
        layout.textBox("about_title", CHECK_X, ABOUT_TITLE_Y, WIDTH - 2 * CHECK_X, LINE);
        layout.textBox("about_text", CHECK_X, ABOUT_TEXT_Y, WIDTH - 2 * CHECK_X, 2 * (LINE + 1));
        layout.box("status", STATUS.x(), STATUS.y(), STATUS.w(), STATUS.h());
        return layout;
    }
}
