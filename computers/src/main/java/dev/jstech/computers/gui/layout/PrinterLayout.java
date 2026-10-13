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
 * Geometry of a printer's window, shared by its menu (the tray, the output and the inventory) and its screen (what it
 * draws around them), in the bus windows' frame: a header with the printer's name and the lamp of its link, then a
 * labelled row each for the paper tray, the job printing now and its page, the queue, and the sheets that came out,
 * the two buttons, and the player's inventory under its label.
 */
public final class PrinterLayout {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 250;
    public static final int SLOT = 18;

    /** The header bar with the printer's name and its link lamp at the right end. */
    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 164;
    public static final int HEADER_H = 16;
    public static final int TITLE_X = 12;
    public static final int TITLE_Y = 11;
    public static final int LAMP_SIZE = 5;
    public static final int LAMP_X = HEADER_X + HEADER_W - 12;
    public static final int LAMP_Y = TITLE_Y;
    /** How many characters of the printer's name fit before the lamp. */
    public static final int TITLE_CHARS = 22;

    /** The rows' labels, at the small letters, and where their contents start. */
    public static final int LABEL_X = 8;
    public static final int CONTROL_X = 46;
    public static final float SMALL = 0.75F;
    public static final int RIGHT = 168;

    /** The paper tray's slot frame and the note beside it, on two lines. */
    public static final int PAPER_Y = 28;
    public static final int PAPER_NOTE_X = CONTROL_X + SLOT + 4;
    public static final int PAPER_NOTE_CHARS = 22;

    /** What prints now. */
    public static final int NOW_Y = 52;
    public static final int NOW_CHARS = 27;

    /** The page count in its little track, and how the page comes out beside it. */
    public static final int PAGE_Y = 64;
    public static final int PAGE_W = 40;
    public static final int PAGE_H = 9;
    public static final int PACE_X = CONTROL_X + PAGE_W + 4;
    public static final int PACE_CHARS = 17;

    /** The queue's label and its list, three rows deep. */
    public static final int QUEUE_LABEL_Y = 78;
    public static final int QUEUE_X = LABEL_X;
    public static final int QUEUE_Y = 86;
    public static final int QUEUE_W = RIGHT - LABEL_X;
    public static final int QUEUE_ROW = 11;
    public static final int QUEUE_ROWS = 3;
    public static final int QUEUE_H = QUEUE_ROW * QUEUE_ROWS;
    public static final int QUEUE_TITLE_CHARS = 14;
    public static final int QUEUE_FROM_CHARS = 16;

    /** The output's three slot frames. */
    public static final int OUT_Y = 124;
    public static final int OUT_SLOTS = 3;

    /** Pause (Resume while paused) and Cancel job. */
    public static final int BUTTON_Y = 146;
    public static final int BUTTON_H = 12;
    public static final int PAUSE_X = LABEL_X;
    public static final int PAUSE_W = 44;
    public static final int CANCEL_X = PAUSE_X + PAUSE_W + 4;
    public static final int CANCEL_W = 64;

    /** The player's inventory, under its label. */
    public static final int INV_X = (WIDTH - 9 * SLOT) / 2;
    public static final int INV_Y = 170;
    public static final int INV_LABEL_Y = INV_Y - 10;

    private PrinterLayout() {
    }

    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("header", HEADER_X, HEADER_Y, HEADER_W - 14, HEADER_H);
        layout.box("lamp", LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE);
        layout.text("title", TITLE_X, TITLE_Y, TITLE_CHARS, 1.0F);
        layout.text("paperLabel", LABEL_X, PAPER_Y + 6, 6, SMALL);
        layout.slot("paper", CONTROL_X + 1, PAPER_Y + 1);
        layout.text("paperNote1", PAPER_NOTE_X, PAPER_Y + 3, PAPER_NOTE_CHARS, SMALL);
        layout.text("paperNote2", PAPER_NOTE_X, PAPER_Y + 11, PAPER_NOTE_CHARS, SMALL);
        layout.text("nowLabel", LABEL_X, NOW_Y, 6, SMALL);
        layout.text("now", CONTROL_X, NOW_Y, NOW_CHARS, SMALL);
        layout.text("pageLabel", LABEL_X, PAGE_Y + 1, 6, SMALL);
        layout.box("page", CONTROL_X, PAGE_Y, PAGE_W, PAGE_H);
        layout.text("pace", PACE_X, PAGE_Y + 1, PACE_CHARS, SMALL);
        layout.text("queueLabel", LABEL_X, QUEUE_LABEL_Y, 6, SMALL);
        layout.box("queue", QUEUE_X, QUEUE_Y, QUEUE_W, QUEUE_H);
        layout.text("outLabel", LABEL_X, OUT_Y + 6, 6, SMALL);
        for (int i = 0; i < OUT_SLOTS; i++) {
            layout.slot("out" + i, CONTROL_X + 1 + i * SLOT, OUT_Y + 1);
        }
        layout.box("pause", PAUSE_X, BUTTON_Y, PAUSE_W, BUTTON_H);
        layout.box("cancel", CANCEL_X, BUTTON_Y, CANCEL_W, BUTTON_H);
        layout.text("inventoryLabel", INV_X, INV_LABEL_Y, 9, SMALL);
        layout.playerInventory(INV_X, INV_Y);
        return layout;
    }
}
