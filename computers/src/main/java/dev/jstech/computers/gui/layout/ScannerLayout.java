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
 * Where everything sits in a scanner's window: the header with its name and state; on the left the glass, what lies on
 * it, the kind of file it makes, its pace, Scan and Stop and the note that nothing is used up; on the right where the
 * file goes, the page being read and what a scanner reads; under them the player's inventory and three readouts.
 * The screen draws from these and the menu places its slots by them.
 */
public final class ScannerLayout {

    public static final int WIDTH = 268;
    public static final int HEIGHT = 250;
    public static final int SLOT = 18;
    public static final float SMALL = 0.75F;

    /** The header bar, the name at its left and the state at its right, after its lamp. */
    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 256;
    public static final int HEADER_H = 14;
    public static final int TITLE_X = 10;
    public static final int TITLE_Y = 9;
    public static final int TITLE_CHARS = 24;
    public static final int STATE_RIGHT = HEADER_X + HEADER_W - 4;
    public static final int STATE_CHARS = 14;
    public static final int LAMP_SIZE = 4;

    /** The left panel. */
    public static final int LEFT_X = 8;
    public static final int LEFT_Y = 24;
    public static final int LEFT_W = 92;
    public static final int PANEL_H = 136;
    public static final int GLASS_LABEL_Y = 28;
    public static final int GLASS_X = 12;
    public static final int GLASS_Y = 35;
    public static final int ITEM_X = 33;
    public static final int ITEM_Y = 36;
    public static final int ITEM_LINE = 8;
    public static final int ITEM_LINES = 3;
    public static final int ITEM_CHARS = 16;
    public static final int MAKES_LABEL_Y = 62;
    public static final int MAKES_Y = 70;
    public static final int SPEED_LABEL_Y = 86;
    public static final int SPEED_Y = 94;
    public static final int VALUE_RIGHT = LEFT_X + LEFT_W - 4;
    public static final int VALUE_CHARS = 12;
    public static final int BUTTON_Y = 110;
    public static final int BUTTON_H = 14;
    public static final int SCAN_X = 12;
    public static final int STOP_X = 56;
    public static final int BUTTON_W = 40;
    public static final int NOTE_Y = 132;
    public static final int NOTE_LINE = 8;
    public static final int NOTE_CHARS = 22;

    /** The right panel. */
    public static final int RIGHT_X = 104;
    public static final int RIGHT_W = 158;
    public static final int TEXT_X = 108;
    public static final int SAVES_LABEL_Y = 28;
    public static final int COMPUTER_Y = 36;
    public static final int FOLDER_Y = 47;
    public static final int FILE_Y = 55;
    /* As many small characters as the panel's room holds; the screen cuts a longer line to that room. */
    public static final int SAVES_CHARS = 33;
    public static final int PREVIEW_X = 108;
    public static final int PREVIEW_Y = 64;
    public static final int PREVIEW_W = 70;
    public static final int PREVIEW_H = 90;
    public static final int SIDE_X = 184;
    public static final int SIDE_W = 74;
    public static final int SIDE_CHARS = 18;
    public static final int PAGE_LABEL_Y = 66;
    public static final int PAGE_Y = 74;
    public static final int BAR_Y = 86;
    public static final int BAR_H = 4;
    public static final int READS_LABEL_Y = 98;
    public static final int READS_Y = 106;
    public static final int READS_LINE = 8;
    public static final int READS_LINES = 5;

    /** The player's inventory, and the three readouts beside it. */
    public static final int INV_X = 8;
    public static final int INV_Y = 166;
    public static final int READOUT_X = 178;
    public static final int READOUT_W = 84;
    public static final int READOUT_H = 22;
    public static final int READOUT_Y = 166;
    public static final int READOUT_STEP = 26;
    public static final int READOUTS = 3;
    public static final int READOUT_CHARS = 18;

    private ScannerLayout() {
    }

    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("header", HEADER_X, HEADER_Y, HEADER_W, HEADER_H);
        layout.text("title", TITLE_X, TITLE_Y, TITLE_CHARS, 1.0F);
        // The two panels are the ground the rest stands on, so they are drawn under it and not checked against it.
        layout.text("glassLabel", GLASS_X, GLASS_LABEL_Y, 6, SMALL);
        layout.slot("glass", GLASS_X + 1, GLASS_Y + 1);
        for (int i = 0; i < ITEM_LINES; i++) {
            layout.text("item" + i, ITEM_X, ITEM_Y + i * ITEM_LINE, ITEM_CHARS, SMALL);
        }
        layout.text("makesLabel", GLASS_X, MAKES_LABEL_Y, 6, SMALL);
        layout.text("makes", VALUE_RIGHT - VALUE_CHARS * 6, MAKES_Y, VALUE_CHARS, 1.0F);
        layout.text("speedLabel", GLASS_X, SPEED_LABEL_Y, 6, SMALL);
        layout.text("speed", VALUE_RIGHT - VALUE_CHARS * 6, SPEED_Y, VALUE_CHARS, 1.0F);
        layout.box("scan", SCAN_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        layout.box("stop", STOP_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        layout.text("note0", GLASS_X, NOTE_Y, NOTE_CHARS, SMALL);
        layout.text("note1", GLASS_X, NOTE_Y + NOTE_LINE, NOTE_CHARS, SMALL);
        layout.text("savesLabel", TEXT_X, SAVES_LABEL_Y, 9, SMALL);
        layout.text("computer", TEXT_X, COMPUTER_Y, SAVES_CHARS / 2, 1.0F);
        layout.text("folder", TEXT_X, FOLDER_Y, SAVES_CHARS, SMALL);
        layout.text("file", TEXT_X, FILE_Y, SAVES_CHARS, SMALL);
        layout.box("preview", PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H);
        layout.text("pageLabel", SIDE_X, PAGE_LABEL_Y, 6, SMALL);
        layout.text("page", SIDE_X, PAGE_Y, SIDE_CHARS / 2, 1.0F);
        layout.box("bar", SIDE_X, BAR_Y, SIDE_W, BAR_H);
        layout.text("readsLabel", SIDE_X, READS_LABEL_Y, 6, SMALL);
        for (int i = 0; i < READS_LINES; i++) {
            layout.text("reads" + i, SIDE_X, READS_Y + i * READS_LINE, SIDE_CHARS, SMALL);
        }
        layout.playerInventory(INV_X, INV_Y);
        for (int i = 0; i < READOUTS; i++) {
            layout.box("readout" + i, READOUT_X, READOUT_Y + i * READOUT_STEP, READOUT_W, READOUT_H);
        }
        return layout;
    }
}
