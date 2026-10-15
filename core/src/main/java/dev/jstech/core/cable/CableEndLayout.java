/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything stands on the screen of one end of a line, as the approved design lays it out, in the game's own
 * look: a window with the end's mode, its priority and rate, its channel and its filter, and below it, in a window of
 * its own, the player's inventory, from which a stack is dropped on a filter slot. Both the menu and the screen read
 * their places from here.
 */
public final class CableEndLayout {

    /** The whole screen: the end's window, a gap, and the inventory's window. */
    public static final int WIDTH = 176;
    public static final int PANEL_HEIGHT = 156;
    public static final int INVENTORY_TOP = PANEL_HEIGHT + 4;
    public static final int INVENTORY_HEIGHT = 100;
    public static final int HEIGHT = INVENTORY_TOP + INVENTORY_HEIGHT;
    /** The top-left item of the inventory's grid, and its word. */
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = INVENTORY_TOP + 18;
    public static final int INVENTORY_WORD_Y = INVENTORY_TOP + 6;
    /** The end's name across the top, shrunk to fit if long. */
    public static final int TITLE_X = 8;
    public static final int TITLE_Y = 6;
    public static final int TITLE_WIDTH = WIDTH - 16;
    /** The words on the left of each row, shrunk to fit the room before the row's keys. */
    public static final int WORD_X = 8;
    public static final int KEY_HEIGHT = 16;
    /** The four mode keys, the chosen one framed. */
    public static final int MODE_Y = 18;
    public static final int MODE_X = 40;
    public static final int MODE_GAP = 2;
    public static final int MODES = 4;
    /** The priority and the rate: a minus, a field, a plus, and the rate's unit after it. */
    public static final int PRIORITY_Y = 38;
    public static final int RATE_Y = 56;
    public static final int STEP_X = 52;
    public static final int STEP_WIDTH = 14;
    public static final int FIELD_X = STEP_X + STEP_WIDTH + 2;
    public static final int PRIORITY_FIELD_WIDTH = 28;
    public static final int RATE_FIELD_WIDTH = 40;
    public static final int UNIT_GAP = 4;
    /** The sixteen channels, in the dyes' colours, eight to a row. */
    public static final int CHANNEL_X = 52;
    public static final int CHANNEL_Y = 74;
    public static final int CHANNEL_WIDTH = 12;
    public static final int CHANNEL_HEIGHT = 10;
    public static final int CHANNEL_PITCH_X = 14;
    public static final int CHANNEL_PITCH_Y = 12;
    public static final int CHANNELS_A_ROW = 8;
    public static final int CHANNELS = 16;
    /** The filter's key, only or all but, and its nine filter slots below. */
    public static final int FILTER_Y = 100;
    public static final int FILTER_X = 52;
    public static final int FILTER_WIDTH = 50;
    public static final int GHOST_X = 8;
    public static final int GHOST_Y = 119;
    public static final int GHOSTS = 9;
    public static final int SLOT_PITCH = 18;
    /** The line under the filter saying what the end does. */
    public static final int HINT_Y = 142;
    /** The small text's scale: the hint and the amounts kept. */
    public static final float SMALL = 0.75F;

    /* The room a row's word has, before its keys, and the mode keys' widths, the wider first. */
    private static final int ROW_WORD_ROOM = STEP_X - WORD_X - 4;
    private static final int MODE_WORD_ROOM = MODE_X - WORD_X - 4;
    private static final int[] MODE_WIDTHS = {38, 28, 28, 26};
    private static final int WORD_FROM_KEY = 4;
    private static final int SLOT_SIZE = 18;
    private static final int TEXT_LINE = 9;

    private CableEndLayout() {
    }

    /** The width of the {@code k}th mode key, from the left. */
    public static int modeWidth(final int k) {
        return MODE_WIDTHS[k];
    }

    /** The left of the {@code k}th mode key. */
    public static int modeX(final int k) {
        int x = MODE_X;
        for (int i = 0; i < k; i++) {
            x += MODE_WIDTHS[i] + MODE_GAP;
        }
        return x;
    }

    /** The room before a row's keys its word is shrunk to; the mode row's word has less. */
    public static int wordRoom(final boolean modeRow) {
        return modeRow ? MODE_WORD_ROOM : ROW_WORD_ROOM;
    }

    /** The top of a row's word, set level with the middle of the row's keys. */
    public static int wordY(final int rowY) {
        return rowY + WORD_FROM_KEY;
    }

    /** The left of the plus key of a number row whose field is {@code fieldWidth} wide. */
    public static int plusX(final int fieldWidth) {
        return FIELD_X + fieldWidth + 2;
    }

    /** The left of the rate's unit. */
    public static int unitX() {
        return plusX(RATE_FIELD_WIDTH) + STEP_WIDTH + UNIT_GAP;
    }

    /** The left of channel {@code k}'s key. */
    public static int channelX(final int k) {
        return CHANNEL_X + k % CHANNELS_A_ROW * CHANNEL_PITCH_X;
    }

    /** The top of channel {@code k}'s key. */
    public static int channelY(final int k) {
        return CHANNEL_Y + k / CHANNELS_A_ROW * CHANNEL_PITCH_Y;
    }

    /** The channel whose key holds the point ({@code x}, {@code y}) on the screen, or -1 for none. */
    public static int channelAt(final double x, final double y) {
        for (int k = 0; k < CHANNELS; k++) {
            if (x >= channelX(k) && x < channelX(k) + CHANNEL_WIDTH && y >= channelY(k)
                    && y < channelY(k) + CHANNEL_HEIGHT) {
                return k;
            }
        }
        return -1;
    }

    /** The left of filter slot {@code k}'s item. */
    public static int ghostX(final int k) {
        return GHOST_X + k * SLOT_PITCH;
    }

    /** The filter slot whose frame holds the point ({@code x}, {@code y}) on the screen, or -1 for none. */
    public static int ghostAt(final double x, final double y) {
        if (y < GHOST_Y - 1 || y >= GHOST_Y - 1 + SLOT_SIZE || x < GHOST_X - 1) {
            return -1;
        }
        final int k = (int) ((x - (GHOST_X - 1)) / SLOT_PITCH);
        return k < GHOSTS ? k : -1;
    }

    /** Everything on the screen, for a test to hold apart and inside. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT)
                .textBox("title", TITLE_X, TITLE_Y, TITLE_WIDTH, TEXT_LINE)
                .textBox("modeWord", WORD_X, wordY(MODE_Y), MODE_WORD_ROOM, TEXT_LINE)
                .textBox("priorityWord", WORD_X, wordY(PRIORITY_Y), ROW_WORD_ROOM, TEXT_LINE)
                .textBox("rateWord", WORD_X, wordY(RATE_Y), ROW_WORD_ROOM, TEXT_LINE)
                .textBox("channelWord", WORD_X, wordY(CHANNEL_Y), ROW_WORD_ROOM, TEXT_LINE)
                .textBox("filterWord", WORD_X, wordY(FILTER_Y), ROW_WORD_ROOM, TEXT_LINE);
        layout.text("hint", WORD_X, HINT_Y, (WIDTH - 2 * WORD_X) * 4 / 3 / 6, SMALL);
        layout.text("inventoryWord", WORD_X, INVENTORY_WORD_Y, 9, 1.0F);
        for (int k = 0; k < MODES; k++) {
            layout.box("mode" + k, modeX(k), MODE_Y, modeWidth(k), KEY_HEIGHT);
        }
        numberRow(layout, "priority", PRIORITY_Y, PRIORITY_FIELD_WIDTH);
        numberRow(layout, "rate", RATE_Y, RATE_FIELD_WIDTH);
        layout.text("rateUnit", unitX(), wordY(RATE_Y), 7, 1.0F);
        for (int k = 0; k < CHANNELS; k++) {
            layout.box("channel" + k, channelX(k), channelY(k), CHANNEL_WIDTH, CHANNEL_HEIGHT);
        }
        layout.box("filterMode", FILTER_X, FILTER_Y, FILTER_WIDTH, KEY_HEIGHT);
        for (int k = 0; k < GHOSTS; k++) {
            layout.box("ghost" + k, ghostX(k) - 1, GHOST_Y - 1, SLOT_SIZE, SLOT_SIZE);
        }
        // The two windows: the gap between them is no slot's and no key's.
        layout.box("gap", 0, PANEL_HEIGHT, WIDTH, INVENTORY_TOP - PANEL_HEIGHT);
        return layout.playerInventory(INVENTORY_X - 1, INVENTORY_Y - 1);
    }

    private static void numberRow(final GuiLayout layout, final String name, final int y, final int fieldWidth) {
        layout.box(name + "Minus", STEP_X, y, STEP_WIDTH, KEY_HEIGHT)
                .box(name + "Field", FIELD_X, y, fieldWidth, KEY_HEIGHT)
                .box(name + "Plus", plusX(fieldWidth), y, STEP_WIDTH, KEY_HEIGHT);
    }
}
