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
 * Where everything sits in the Large Scale Printer's window: the header with its name and the book it is on; on the
 * left the feeder's place, the sheets it holds, what a book takes, Pause and Cancel and its pace; on the right the
 * queue, three books deep, and the book printing now with its progress; under them the inventory and three readouts,
 * the first of them the switch of where the books go. The screen draws from these and the menu places its slot by them.
 */
public final class LargeScalePrinterLayout {

    public static final int WIDTH = 268;
    public static final int HEIGHT = 250;
    public static final float SMALL = 0.75F;

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 256;
    public static final int HEADER_H = 14;
    public static final int TITLE_X = 10;
    public static final int TITLE_Y = 9;
    public static final int TITLE_CHARS = 22;
    public static final int STATE_RIGHT = HEADER_X + HEADER_W - 4;
    public static final int LAMP_SIZE = 4;

    /** The left panel: the feeder, its sheets, a book's sheets, the buttons and the pace. */
    public static final int LEFT_X = 8;
    public static final int LEFT_Y = 24;
    public static final int LEFT_W = 92;
    public static final int PANEL_H = 136;
    public static final int LABEL_X = 12;
    public static final int FEEDER_LABEL_Y = 28;
    public static final int FEEDER_X = 12;
    public static final int FEEDER_Y = 35;
    public static final int FEEDER_NOTE_X = 33;
    public static final int FEEDER_NOTE_Y = 36;
    public static final int NOTE_LINE = 8;
    public static final int NOTE_CHARS = 16;
    public static final int BOX_X = 12;
    public static final int BOX_W = 84;
    public static final int BOX_H = 20;
    public static final int SHEETS_Y = 58;
    public static final int BOOK_Y = 82;
    public static final int BUTTON_Y = 108;
    public static final int BUTTON_H = 14;
    public static final int PAUSE_X = 12;
    public static final int CANCEL_X = 56;
    public static final int BUTTON_W = 40;
    public static final int PACE_Y = 130;
    public static final int PACE_CHARS = 22;

    /** The right panel: the queue's rows, then the book printing now and its bar. */
    public static final int RIGHT_X = 104;
    public static final int RIGHT_W = 158;
    public static final int TEXT_X = 108;
    public static final int QUEUE_LABEL_Y = 28;
    public static final int ROW_Y = 36;
    public static final int ROW_STEP = 24;
    public static final int ROWS = 3;
    public static final int ROW_ICON = 16;
    public static final int ROW_TEXT_X = 128;
    /* As many small characters as a row's room holds, and the room under the queue; the screen cuts a longer line. */
    public static final int ROW_CHARS = 28;
    public static final int NOW_CHARS = 33;
    public static final int NOW_LABEL_Y = 110;
    public static final int NOW_Y = 118;
    public static final int BAR_Y = 130;
    public static final int BAR_W = 150;
    public static final int BAR_H = 4;
    public static final int PAGES_Y = 138;

    /** The player's inventory, and the three readouts beside it. */
    public static final int INV_X = 8;
    public static final int INV_Y = 166;
    public static final int READOUT_X = 178;
    public static final int READOUT_W = 84;
    public static final int READOUT_H = 22;
    public static final int READOUT_Y = 166;
    public static final int READOUT_STEP = 26;
    public static final int READOUTS = 3;

    private LargeScalePrinterLayout() {
    }

    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("header", HEADER_X, HEADER_Y, HEADER_W, HEADER_H);
        layout.text("title", TITLE_X, TITLE_Y, TITLE_CHARS, 1.0F);
        // The two panels are the ground the rest stands on, so they are drawn under it and not checked against it.
        layout.text("feederLabel", LABEL_X, FEEDER_LABEL_Y, 6, SMALL);
        layout.slot("feeder", FEEDER_X + 1, FEEDER_Y + 1);
        for (int i = 0; i < 3; i++) {
            layout.text("feederNote" + i, FEEDER_NOTE_X, FEEDER_NOTE_Y + i * NOTE_LINE, NOTE_CHARS, SMALL);
        }
        layout.box("sheets", BOX_X, SHEETS_Y, BOX_W, BOX_H);
        layout.box("book", BOX_X, BOOK_Y, BOX_W, BOX_H);
        layout.box("pause", PAUSE_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        layout.box("cancel", CANCEL_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        layout.text("pace0", LABEL_X, PACE_Y, PACE_CHARS, SMALL);
        layout.text("pace1", LABEL_X, PACE_Y + NOTE_LINE, PACE_CHARS, SMALL);
        layout.text("queueLabel", TEXT_X, QUEUE_LABEL_Y, 6, SMALL);
        for (int i = 0; i < ROWS; i++) {
            layout.box("rowIcon" + i, TEXT_X, ROW_Y + i * ROW_STEP, ROW_ICON, ROW_ICON);
            layout.text("rowTitle" + i, ROW_TEXT_X, ROW_Y + i * ROW_STEP, ROW_CHARS, SMALL);
            layout.text("rowFrom" + i, ROW_TEXT_X, ROW_Y + i * ROW_STEP + NOTE_LINE, ROW_CHARS, SMALL);
        }
        layout.text("nowLabel", TEXT_X, NOW_LABEL_Y, 6, SMALL);
        layout.text("now", TEXT_X, NOW_Y, NOW_CHARS, SMALL);
        layout.box("bar", TEXT_X, BAR_Y, BAR_W, BAR_H);
        layout.text("pages", TEXT_X, PAGES_Y, NOW_CHARS, SMALL);
        layout.playerInventory(INV_X, INV_Y);
        for (int i = 0; i < READOUTS; i++) {
            layout.box("readout" + i, READOUT_X, READOUT_Y + i * READOUT_STEP, READOUT_W, READOUT_H);
        }
        return layout;
    }
}
