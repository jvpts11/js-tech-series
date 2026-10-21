/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where the new screen from the block looked at puts its parts: the panel on the left with its title, the block and
 * the file to write, what to start from and Create; the new screen's preview beside it; what Create does along the
 * foot. Pure, so a unit test checks that nothing overlaps.
 */
public final class NewScreenLayout {

    /** The panel: where, how wide, its room inside, and its rows. */
    public static final int PANEL_X = 6;
    public static final int PANEL_Y = 40;
    public static final int WIDTH = 172;
    public static final int PAD = 6;
    public static final int TITLE_Y = 6;
    public static final int SUBTITLE_Y = 16;
    public static final int BLOCK_LABEL_Y = 28;
    public static final int BLOCK_Y = 35;
    public static final int FILE_LABEL_Y = 55;
    public static final int FILE_Y = 62;
    public static final int FIELD_HEIGHT = 14;
    public static final int START_Y = 84;
    public static final int ROWS_Y = 97;
    public static final int ROW = 12;
    public static final int ROWS = 5;
    public static final int CREATE_Y = ROWS_Y + ROWS * ROW + 6;
    public static final int CREATE_HEIGHT = 20;
    public static final int HEIGHT = CREATE_Y + CREATE_HEIGHT + PAD;
    /** The room between the panel and the preview, and from the screen's top and foot to its words. */
    public static final int GAP = 10;
    public static final int TOP_TEXT_Y = 8;
    public static final int HINT_UP = 14;

    private NewScreenLayout() {
    }

    /** The preview's left. */
    public static int previewX() {
        return PANEL_X + WIDTH + GAP;
    }

    /** The panel, as the overlap check reads it. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", PAD, TITLE_Y, 10, 1f);
        layout.text("subtitle", PAD, SUBTITLE_Y, 25, 0.75f);
        layout.text("block label", PAD, BLOCK_LABEL_Y, 5, 0.75f);
        layout.box("block", PAD, BLOCK_Y, WIDTH - 2 * PAD, FIELD_HEIGHT);
        layout.text("file label", PAD, FILE_LABEL_Y, 4, 0.75f);
        layout.box("file", PAD, FILE_Y, WIDTH - 2 * PAD, FIELD_HEIGHT);
        layout.text("start from", PAD, START_Y, 10, 1f);
        layout.box("rows", PAD, ROWS_Y, WIDTH - 2 * PAD, ROWS * ROW);
        layout.box("create", PAD, CREATE_Y, WIDTH - 2 * PAD, CREATE_HEIGHT);
        return layout;
    }
}
