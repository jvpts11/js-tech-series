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
 * A 3D printer's window (computers/docs/design/teracoin.md, "The 3D Printer"), in the look of the mod's devices of its
 * era: on the left the item printing now, its layer or what it waits for, the time an item takes and the keys; on the
 * right the queue, each job with its item, copies and the wallet that pays it, and the job now with its bar; under
 * them the inventory, and beside it the output, what the printer is linked to and how many items it printed. Pure: the
 * screen draws from here and the menu seats its slots from here.
 */
public final class Printer3DLayout {

    public static final int WIDTH = 268;
    public static final int HEIGHT = 254;
    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 256;
    public static final int TITLE_X = 10;
    public static final int TITLE_Y = 10;
    /** The left panel: the item now, its readouts, the keys and the note. */
    public static final int LEFT_X = 8;
    public static final int LEFT_Y = 28;
    public static final int LEFT_W = 92;
    public static final int PANEL_H = 138;
    public static final int NOW_SLOT_X = 12;
    public static final int NOW_SLOT_Y = 41;
    public static final int NOW_TEXT_X = 32;
    public static final int READOUT_X = 12;
    public static final int READOUT_W = 84;
    public static final int READOUT_H = 20;
    public static final int LAYER_Y = 68;
    public static final int AN_ITEM_Y = 92;
    public static final int KEYS_Y = 120;
    public static final int KEY_H = 14;
    public static final int PAUSE_X = 12;
    public static final int CANCEL_X = 56;
    public static final int KEY_W = 40;
    public static final int NOTE_Y = 142;
    /** The right panel: the queue and the job now. */
    public static final int RIGHT_X = 106;
    public static final int RIGHT_Y = 28;
    public static final int RIGHT_W = 154;
    public static final int QUEUE_Y = 41;
    public static final int QUEUE_PITCH = 24;
    public static final int QUEUE_ROWS = 3;
    public static final int QUEUE_TEXT_X = 130;
    public static final int JOB_Y = 117;
    public static final int BAR_Y = 137;
    public static final int BAR_W = 146;
    /** The inventory, and beside it the output, the link and the count. */
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 174;
    public static final int OUT_X = 176;
    public static final int OUT_CAPTION_Y = 174;
    public static final int OUT_Y = 182;
    public static final int LINKED_Y = 204;
    public static final int PRINTED_Y = 228;
    public static final int LOWER_W = 86;
    /** The output's places. */
    public static final int OUT_SLOTS = 3;
    /** Small text's line. */
    public static final int LINE = 8;

    private Printer3DLayout() {
    }

    /** Everything on the window: the slots, the keys, the readouts and the bar as boxes, the words. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("header", HEADER_X, HEADER_Y, HEADER_W, 16);
        l.text("title", TITLE_X, TITLE_Y, 30, 1.0f);
        // The two panels are the ground the rest stands on, so they are drawn under it and not checked against it.
        l.box("now", NOW_SLOT_X, NOW_SLOT_Y, GuiLayout.SLOT_SIZE, GuiLayout.SLOT_SIZE);
        l.box("pause", PAUSE_X, KEYS_Y, KEY_W, KEY_H);
        l.box("cancel", CANCEL_X, KEYS_Y, KEY_W, KEY_H);
        for (int k = 0; k < QUEUE_ROWS; k++) {
            l.box("queued_" + k, RIGHT_X + 4, QUEUE_Y + k * QUEUE_PITCH, GuiLayout.SLOT_SIZE, GuiLayout.SLOT_SIZE);
        }
        l.box("bar", RIGHT_X + 4, BAR_Y, BAR_W, 4);
        l.box("layer", READOUT_X, LAYER_Y, READOUT_W, READOUT_H);
        l.box("anItem", READOUT_X, AN_ITEM_Y, READOUT_W, READOUT_H);
        l.box("linked", OUT_X, LINKED_Y, LOWER_W, READOUT_H);
        l.box("printed", OUT_X, PRINTED_Y, LOWER_W, READOUT_H);
        for (int i = 0; i < OUT_SLOTS; i++) {
            l.slot("out" + i, OUT_X + i * 18, OUT_Y);
        }
        l.text("nowCaption", NOW_SLOT_X, LEFT_Y + 4, 3, 0.75f);
        l.text("nowName", NOW_TEXT_X, NOW_SLOT_Y, 14, 0.75f);
        l.text("nowSub", NOW_TEXT_X, NOW_SLOT_Y + 2 * LINE, 14, 0.75f);
        l.text("note_0", READOUT_X, NOTE_Y, 18, 0.75f);
        l.text("note_1", READOUT_X, NOTE_Y + LINE, 18, 0.75f);
        l.text("queueCaption", RIGHT_X + 4, RIGHT_Y + 4, 5, 0.75f);
        for (int k = 0; k < QUEUE_ROWS; k++) {
            l.text("job_" + k, QUEUE_TEXT_X, QUEUE_Y + k * QUEUE_PITCH + 1, 28, 0.75f);
            l.text("wallet_" + k, QUEUE_TEXT_X, QUEUE_Y + k * QUEUE_PITCH + 9, 28, 0.75f);
        }
        l.text("jobCaption", RIGHT_X + 4, JOB_Y, 3, 0.75f);
        l.text("jobNow", RIGHT_X + 4, JOB_Y + 9, 32, 0.75f);
        l.text("jobLine", RIGHT_X + 4, JOB_Y + 28, 32, 0.75f);
        l.text("outCaption", OUT_X, OUT_CAPTION_Y, 3, 0.75f);
        l.playerInventory(INVENTORY_X, INVENTORY_Y);
        return l;
    }
}
