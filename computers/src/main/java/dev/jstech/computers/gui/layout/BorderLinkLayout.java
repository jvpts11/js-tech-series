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
 * Where everything of Nouvell BorderLink's window stands on the Frames desktops: the line naming the gateway, the list
 * of its four ports, the port picked with what the far side may do here, and OK, Cancel and Apply over the count of
 * lines up. Frames 7 writes that count in a status bar of its own under the buttons. Pure, so the window draws from it
 * and a test holds it to account.
 */
public final class BorderLinkLayout {

    /** How wide the window is, and how tall: Frames 7's is taller by its status bar. */
    public static final int W = 300;
    public static final int H = 192;
    public static final int STATUS_H = 12;
    public static final int H_WITH_STATUS = H + STATUS_H;

    /** The line naming the gateway. */
    public static final int HEADER_X = 4;
    public static final int HEADER_Y = 4;

    /** The list of ports: its box, its heading row, the step from row to row, and where each column starts in it. */
    public static final int LIST_X = 4;
    public static final int LIST_Y = 16;
    public static final int LIST_W = W - 8;
    public static final int LIST_HEAD = 12;
    public static final int ROW = 11;
    public static final int ROWS = 4;
    public static final int LIST_H = LIST_HEAD + ROWS * ROW + 2;
    public static final int[] COLUMNS = {4, 18, 104, 196, 256};

    /** The port picked: its name, what its line is, and what the far side may do here. */
    public static final int DETAIL_Y = LIST_Y + LIST_H + 8;
    public static final int INFO_Y = DETAIL_Y + 11;
    public static final int RIGHTS_Y = INFO_Y + 15;
    public static final int[] CHECK_X = {10, 160};
    public static final int[] CHECK_Y = {RIGHTS_Y + 13, RIGHTS_Y + 26};
    public static final int CHECK_W = 130;
    public static final int CHECK_H = 11;
    public static final int CHECK_BOX = 9;
    /** The port's own two switches, under the four rights. */
    public static final int SWITCHES_Y = RIGHTS_Y + 43;
    public static final int RELAY_W = 145;
    public static final int ON_W = 80;

    /** OK, Cancel and Apply at the foot, and the count of lines up beside them. */
    public static final int BUTTON_W = 50;
    public static final int BUTTON_H = 14;
    public static final int BUTTONS_Y = H - 18;
    public static final int[] BUTTON_X = {132, 188, 244};
    public static final int FOOTER_X = 8;
    public static final int FOOTER_Y = BUTTONS_Y + 3;

    private BorderLinkLayout() {
    }

    /** How tall the window is, with Frames 7's status bar or without. */
    public static int height(final boolean statusBar) {
        return statusBar ? H_WITH_STATUS : H;
    }

    /** The window as a layout, its solid parts and the longest of its words, for a test to check. */
    public static GuiLayout layout(final boolean statusBar) {
        final GuiLayout l = new GuiLayout(W, height(statusBar));
        l.text("header", HEADER_X, HEADER_Y, 48, 1.0f);
        l.box("ports", LIST_X, LIST_Y, LIST_W, LIST_H);
        l.text("detail", LIST_X, DETAIL_Y, 48, 1.0f);
        l.text("info", LIST_X, INFO_Y, 48, 1.0f);
        l.text("rights", LIST_X, RIGHTS_Y, 40, 1.0f);
        for (int i = 0; i < 4; i++) {
            l.box("right" + i, CHECK_X[i % 2], CHECK_Y[i / 2], CHECK_W, CHECK_H);
        }
        l.box("relay", CHECK_X[0], SWITCHES_Y, RELAY_W, CHECK_H);
        l.box("on", CHECK_X[1], SWITCHES_Y, ON_W, CHECK_H);
        for (int i = 0; i < BUTTON_X.length; i++) {
            l.box("button" + i, BUTTON_X[i], BUTTONS_Y, BUTTON_W, BUTTON_H);
        }
        if (statusBar) {
            l.box("status", 0, H, W, STATUS_H);
        } else {
            l.text("footer", FOOTER_X, FOOTER_Y, 18, 1.0f);
        }
        return l;
    }
}
