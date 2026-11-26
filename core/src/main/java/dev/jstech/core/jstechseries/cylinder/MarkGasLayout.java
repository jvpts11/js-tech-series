/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything stands on the list an empty Gas Cylinder used in the air opens, as the approved design lays it out
 * in the game's own look: the title, the search, the gases a row each with the cylinder painted for it, its name and
 * its formula, the line saying every gas of the Core is listed, and Mark and Cancel.
 */
public final class MarkGasLayout {

    public static final int WIDTH = 184;
    public static final int HEIGHT = 178;
    public static final int TITLE_Y = 7;
    public static final int INSET = 8;
    public static final int INNER = WIDTH - 2 * INSET;
    public static final int SEARCH_Y = 20;
    public static final int SEARCH_HEIGHT = 16;
    /** The list's ground, and its rows inside it. */
    public static final int LIST_Y = 40;
    public static final int LIST_HEIGHT = 102;
    public static final int ROW_Y = LIST_Y + 3;
    public static final int ROW_PITCH = 19;
    public static final int ROWS = 5;
    public static final int ROW_INSET = 2;
    public static final int ICON_X = INSET + 4;
    public static final int NAME_X = INSET + 24;
    public static final int WORDS_DROP = 4;
    public static final int FORMULA_RIGHT = WIDTH - INSET - 6;
    /** The line under the list, in the small font. */
    public static final int EVERY_Y = LIST_Y + LIST_HEIGHT + 5;
    public static final float SMALL = 0.75F;
    /** Mark at the left, Cancel at the right. */
    public static final int BUTTON_Y = 156;
    public static final int BUTTON_WIDTH = 80;
    public static final int BUTTON_HEIGHT = 18;
    public static final int MARK_X = INSET;
    public static final int CANCEL_X = WIDTH - INSET - BUTTON_WIDTH;
    /* The longest name the list writes, in letters, and a formula's. */
    private static final int NAME_CHARS = 22;
    private static final int FORMULA_CHARS = 6;
    private static final int EVERY_CHARS = 38;

    private MarkGasLayout() {
    }

    /** The window's left, centred on a screen {@code width} wide. */
    public static int x(final int width) {
        return (width - WIDTH) / 2;
    }

    /** Its top, centred on a screen {@code height} tall. */
    public static int y(final int height) {
        return (height - HEIGHT) / 2;
    }

    /** The row at ({@code x}, {@code y}) from the window's top left among the rows shown, or -1 for none. */
    public static int rowAt(final double x, final double y) {
        if (x < INSET + ROW_INSET || x >= WIDTH - INSET - ROW_INSET || y < ROW_Y - 1) {
            return -1;
        }
        final int row = (int) ((y - (ROW_Y - 1)) / ROW_PITCH);
        return row < ROWS ? row : -1;
    }

    /** The window, its pieces kept apart and inside. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("titleLine", INSET, TITLE_Y, NAME_CHARS, 1.0F);
        layout.box("searchField", INSET, SEARCH_Y, INNER, SEARCH_HEIGHT);
        layout.box("gasList", INSET, LIST_Y, INNER, LIST_HEIGHT);
        layout.text("everyLine", INSET, EVERY_Y, EVERY_CHARS, SMALL);
        layout.box("markKey", MARK_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        layout.box("cancelKey", CANCEL_X, BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        for (int row = 0; row < ROWS; row++) {
            final int y = ROW_Y + row * ROW_PITCH;
            layout.text("nameCell" + row, NAME_X, y + WORDS_DROP, NAME_CHARS, 1.0F);
            layout.text("formulaCell" + row, FORMULA_RIGHT - FORMULA_CHARS * 6, y + WORDS_DROP, FORMULA_CHARS, 1.0F);
        }
        return layout;
    }
}
