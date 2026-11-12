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
 * Where everything sits in an Extended CD Drive's window: the header with its name and the copy it is on; on the left
 * the reading tray and its disc, the speed, the copies, Copy and Eject and its note; on the right the seven writing
 * trays, each a row of its own with its bar; under them the inventory and three readouts. The screen draws from these
 * and the menu places its slots by them.
 */
public final class ExtendedCdDriveLayout {

    public static final int WIDTH = 268;
    public static final int HEIGHT = 254;
    public static final float SMALL = 0.75F;
    public static final int TRAYS = 7;

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 256;
    public static final int HEADER_H = 17;
    public static final int TITLE_X = 10;
    public static final int TITLE_Y = 10;
    public static final int STATE_RIGHT = HEADER_X + HEADER_W - 4;
    public static final int LAMP_SIZE = 4;

    public static final int LEFT_X = 8;
    public static final int LEFT_Y = 28;
    public static final int LEFT_W = 92;
    public static final int PANEL_H = 138;
    public static final int LABEL_X = 12;
    public static final int READ_LABEL_Y = 32;
    public static final int READ_X = 12;
    public static final int READ_Y = 41;
    public static final int DISC_X = 32;
    public static final int DISC_Y = 41;
    public static final int LINE = 8;
    public static final int BOX_X = 12;
    public static final int BOX_W = 84;
    public static final int BOX_H = 20;
    public static final int SPEED_Y = 64;
    public static final int COPIES_Y = 88;
    public static final int BUTTON_Y = 116;
    public static final int BUTTON_H = 14;
    public static final int COPY_X = 12;
    public static final int EJECT_X = 56;
    public static final int BUTTON_W = 40;
    public static final int NOTE_Y = 138;

    /** The writing trays, each a row of its own: the blank, its name, its bar and how far it is. */
    public static final int ROW_X = 106;
    public static final int ROW_W = 154;
    public static final int ROW_H = 18;
    public static final int ROW_Y = 28;
    public static final int ROW_STEP = 20;
    public static final int TRAY_X = 107;
    public static final int TRAY_LABEL_X = 127;
    public static final int BAR_X = 140;
    public static final int BAR_W = 84;
    public static final int BAR_H = 4;
    public static final int STATE_X = 257;

    public static final int INV_X = 8;
    public static final int INV_Y = 174;
    public static final int READOUT_X = 176;
    public static final int READOUT_W = 86;
    public static final int READOUT_H = 20;
    public static final int READOUT_Y = 174;
    public static final int READOUT_STEP = 24;
    public static final int READOUTS = 3;
    /** Where a readout's value sits under its label. */
    public static final int VALUE_Y = 9;

    private ExtendedCdDriveLayout() {
    }

    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("header", HEADER_X, HEADER_Y, HEADER_W, HEADER_H);
        // The left panel and the trays' rows are the ground the rest stands on, so they are not checked against it.
        layout.text("readLabel", LABEL_X, READ_LABEL_Y, 6, SMALL);
        layout.slot("read", READ_X, READ_Y);
        layout.text("disc0", DISC_X, DISC_Y, 16, SMALL);
        layout.text("disc1", DISC_X, DISC_Y + LINE, 16, SMALL);
        layout.box("speed", BOX_X, SPEED_Y, BOX_W, BOX_H);
        layout.box("copies", BOX_X, COPIES_Y, BOX_W, BOX_H);
        layout.box("copy", COPY_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        layout.box("eject", EJECT_X, BUTTON_Y, BUTTON_W, BUTTON_H);
        layout.text("note0", LABEL_X, NOTE_Y, 22, SMALL);
        layout.text("note1", LABEL_X, NOTE_Y + LINE, 22, SMALL);
        for (int i = 0; i < TRAYS; i++) {
            final int y = ROW_Y + i * ROW_STEP;
            layout.slot("tray" + i, TRAY_X, y + 1);
            layout.text("trayLabel" + i, TRAY_LABEL_X, y + 5, 2, SMALL);
            layout.box("bar" + i, BAR_X, y + 7, BAR_W, BAR_H);
        }
        layout.playerInventory(INV_X, INV_Y);
        for (int i = 0; i < READOUTS; i++) {
            layout.box("readout" + i, READOUT_X, READOUT_Y + i * READOUT_STEP, READOUT_W, READOUT_H);
        }
        return layout;
    }
}
