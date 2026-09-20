/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.List;

/**
 * Where everything sits in the Help Viewer: the three buttons, the search field, the list of what this
 * computer can do and the page beside it.
 *
 * <p>Pure, and the window and its test read the same numbers, so a row that would overlap another is caught
 * by arithmetic rather than by a player finding it.
 */
public final class HelpViewerLayout {

    /** The window, at the size the desktop opens it. */
    public static final int W = 300;
    public static final int H = 190;

    public static final int PAD = 4;
    public static final int ROW_H = 10;

    /** The row of buttons along the top: the three that viewer always had, and printing, which is nobody's. */
    public static final List<String> BUTTONS = List.of("Backtrack", "History", "Index");
    public static final int BUTTON_Y = PAD;
    public static final int BUTTON_W = 54;
    public static final int BUTTON_H = 12;

    /** The search field under them: what is typed narrows the list, the way apropos narrows a manual. */
    public static final int SEARCH_Y = BUTTON_Y + BUTTON_H + PAD;
    public static final int SEARCH_X = 44;
    public static final int SEARCH_W = 150;

    /** The two wells: the list of commands, and the page. */
    public static final int BODY_Y = SEARCH_Y + ROW_H + PAD;
    public static final int LIST_W = 92;
    public static final int DOC_X = PAD + LIST_W + PAD;

    private HelpViewerLayout() {
    }

    /** Everything solid in the window, for the test that says none of it overlaps. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(W, H);
        int x = PAD;
        for (final String button : BUTTONS) {
            layout.box(button, x, BUTTON_Y, BUTTON_W, BUTTON_H);
            x += BUTTON_W + PAD;
        }
        layout.text("search label", PAD, SEARCH_Y + 2, "Search:".length(), 1.0f);
        layout.box("search", SEARCH_X, SEARCH_Y, SEARCH_W, ROW_H);
        layout.box("list", PAD, BODY_Y, LIST_W, H - BODY_Y - PAD);
        layout.box("page", DOC_X, BODY_Y, W - DOC_X - PAD, H - BODY_Y - PAD);
        return layout;
    }
}
