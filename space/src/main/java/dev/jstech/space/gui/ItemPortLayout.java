/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.gui;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where the Item Port's screen puts its things, as the approved one draws it, in the screen's own pixels: the
 * display across the top with the ship's count and its list of what the build still lacks, a row each, its scroll bar
 * at the right; the hold's label and its two rows of nine places under it; and the player's inventory. The screen
 * draws them here and the menu puts its slots here, so the two always agree. Pure, so it is tested without the game.
 */
public final class ItemPortLayout {

    /** The screen's size. */
    public static final int WIDTH = 176;
    public static final int HEIGHT = 253;
    /** Where its title is written, and the inventory's. */
    public static final int TITLE_X = 8;
    public static final int TITLE_Y = 5;
    public static final int INVENTORY_LABEL_Y = 160;
    /** The display's dark glass. */
    public static final int DISPLAY_X = 10;
    public static final int DISPLAY_Y = 18;
    public static final int DISPLAY_W = 156;
    public static final int DISPLAY_H = 88;
    /** Inside the display: its margin, the ship's line and bar, and the list's first row and the rows' pitch. */
    public static final int MARGIN = 3;
    public static final int BAR_Y = 11;
    public static final int LIST_Y = 17;
    public static final int ROW = 17;
    /** In a row: where its name and its bar start, the bar's height and drop, and the scroll bar's width. */
    public static final int NAME_X = 22;
    public static final int ROW_BAR_Y = 10;
    public static final int BAR_H = 2;
    public static final int SCROLL_W = 2;
    /** The hold's label, and its places' first one, two rows of nine. */
    public static final int HOLD_LABEL_X = 10;
    public static final int HOLD_LABEL_Y = 109;
    public static final int HOLD_X = 8;
    public static final int HOLD_Y = 119;
    public static final int HOLD_COLUMNS = 9;
    public static final int HOLD_ROWS = 2;
    /** Where the player's inventory's grid starts, its hotbar under it as the game lays it. */
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 172;
    /** How many rows of the list the display shows at once. */
    public static final int ROWS_SHOWN = (DISPLAY_H - LIST_Y - 1) / ROW;
    /* How wide the title, the labels and the display's lines are, in characters at most, and the small text's size. */
    private static final int TITLE_CHARS = 18;
    private static final int LABEL_CHARS = 12;
    private static final int HOLD_LABEL_CHARS = 30;
    private static final int SHIP_CHARS = 16;
    private static final int COUNT_CHARS = 17;
    private static final int NAME_CHARS = 18;
    private static final int ROW_COUNT_CHARS = 12;
    private static final float SMALL = 0.75F;
    private static final int SLOT = 18;
    /* Where the ship's count and each row's count end, right-aligned, inside the display. */
    private static final int COUNT_RIGHT = DISPLAY_X + DISPLAY_W - MARGIN;
    private static final int ROW_COUNT_RIGHT = DISPLAY_X + DISPLAY_W - 9;
    /* The small text's glyphs, as the layout reckons them, in pixels. */
    private static final float GLYPH = 6;

    private ItemPortLayout() {
    }

    /** Where the hold's place {@code k} is, its top-left corner, nine to a row. */
    public static int[] place(final int k) {
        return new int[] {HOLD_X + k % HOLD_COLUMNS * SLOT, HOLD_Y + k / HOLD_COLUMNS * SLOT};
    }

    /** The screen's layout: its labels, the display's lines and rows, the hold's places and the inventory. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", TITLE_X, TITLE_Y, TITLE_CHARS, 1.0F);
        layout.text("inventory", TITLE_X, INVENTORY_LABEL_Y, LABEL_CHARS, 1.0F);
        layout.text("hold", HOLD_LABEL_X, HOLD_LABEL_Y, HOLD_LABEL_CHARS, SMALL);
        layout.text("ship", DISPLAY_X + MARGIN, DISPLAY_Y + MARGIN, SHIP_CHARS, SMALL);
        layout.text("count", COUNT_RIGHT - Math.round(COUNT_CHARS * GLYPH * SMALL), DISPLAY_Y + MARGIN, COUNT_CHARS,
                SMALL);
        layout.box("bar", DISPLAY_X + MARGIN, DISPLAY_Y + BAR_Y, DISPLAY_W - 2 * MARGIN, BAR_H);
        for (int row = 0; row < ROWS_SHOWN; row++) {
            final int y = DISPLAY_Y + LIST_Y + row * ROW;
            layout.box("icon" + row, DISPLAY_X + MARGIN, y, 16, 16);
            layout.text("name" + row, DISPLAY_X + NAME_X, y + 1, NAME_CHARS, SMALL);
            layout.text("have" + row, ROW_COUNT_RIGHT - Math.round(ROW_COUNT_CHARS * GLYPH * SMALL), y + 1,
                    ROW_COUNT_CHARS, SMALL);
            layout.box("rowbar" + row, DISPLAY_X + NAME_X, y + ROW_BAR_Y, DISPLAY_W - NAME_X - 12, BAR_H);
        }
        layout.box("scroll", DISPLAY_X + DISPLAY_W - MARGIN - SCROLL_W, DISPLAY_Y + LIST_Y, SCROLL_W,
                DISPLAY_H - LIST_Y - MARGIN);
        for (int k = 0; k < HOLD_COLUMNS * HOLD_ROWS; k++) {
            layout.slot("hold" + k, place(k)[0], place(k)[1]);
        }
        layout.playerInventory(INVENTORY_X, INVENTORY_Y);
        return layout;
    }
}
