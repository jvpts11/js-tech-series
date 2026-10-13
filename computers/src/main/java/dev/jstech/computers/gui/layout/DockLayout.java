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
 * Geometry of the Dock Station's window, shared by its menu (the trays, the port and the inventory) and its screen:
 * the header with its name and the lamp of its link, the computer it is docked to, a row for each tray and for the
 * USB port (its label, the disk in it, its letter, name, era and use, whether it is mounted, and Eject), the note on
 * what it takes, and the player's inventory under its label.
 */
public final class DockLayout {

    public static final int WIDTH = 196;
    public static final int HEIGHT = 244;
    public static final int SLOT = 18;

    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = WIDTH - 12;
    public static final int HEADER_H = 16;
    public static final int TITLE_X = 12;
    public static final int TITLE_Y = 11;
    public static final int LAMP_SIZE = 5;
    public static final int LAMP_X = HEADER_X + HEADER_W - 12;
    public static final int LAMP_Y = TITLE_Y;

    public static final float SMALL = 0.75F;
    public static final int LABEL_X = 8;
    public static final int CONTROL_X = 46;
    public static final int RIGHT = WIDTH - 8;

    /** The computer it is docked to and the note after it. */
    public static final int HOST_Y = 28;
    public static final int HOST_CHARS = 32;

    /** The rows of the trays and the port: their top, their height, and what each holds within. */
    public static final int ROWS_Y = 40;
    public static final int ROW_H = 22;
    public static final int ROWS = 4;
    public static final int TEXT_X = CONTROL_X + SLOT + 4;
    /** How many characters of a disk's name and its details fit before the Eject button. */
    public static final int NAME_CHARS = 19;
    public static final int EJECT_W = 30;
    public static final int EJECT_H = 11;
    public static final int EJECT_X = RIGHT - EJECT_W;
    public static final int STATE_RIGHT = EJECT_X - 4;

    /** The note on what the dock takes, on two lines under the rows. */
    public static final int NOTE_Y = ROWS_Y + ROWS * ROW_H + 4;
    public static final int NOTE_CHARS = 40;

    public static final int INV_X = (WIDTH - 9 * SLOT) / 2;
    public static final int INV_Y = 166;
    public static final int INV_LABEL_Y = INV_Y - 10;

    private DockLayout() {
    }

    /** The top of a tray's or the port's row. */
    public static int rowY(final int row) {
        return ROWS_Y + row * ROW_H;
    }

    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("header", HEADER_X, HEADER_Y, HEADER_W - 14, HEADER_H);
        layout.box("lamp", LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE);
        layout.text("title", TITLE_X, TITLE_Y, 14, 1.0F);
        layout.text("hostLabel", LABEL_X, HOST_Y, 6, SMALL);
        layout.text("host", CONTROL_X, HOST_Y, HOST_CHARS, SMALL);
        for (int row = 0; row < ROWS; row++) {
            final int y = rowY(row);
            layout.text("rowLabel" + row, LABEL_X, y + 7, 8, SMALL);
            layout.slot("bay" + row, CONTROL_X + 1, y + 1);
            layout.text("name" + row, TEXT_X, y + 3, NAME_CHARS, SMALL);
            layout.text("detail" + row, TEXT_X, y + 11, NAME_CHARS, SMALL);
            layout.box("eject" + row, EJECT_X, y + 4, EJECT_W, EJECT_H);
        }
        layout.text("note1", LABEL_X, NOTE_Y, NOTE_CHARS, SMALL);
        layout.text("note2", LABEL_X, NOTE_Y + 8, NOTE_CHARS, SMALL);
        layout.text("inventoryLabel", INV_X, INV_LABEL_Y, 9, SMALL);
        layout.playerInventory(INV_X, INV_Y);
        return layout;
    }
}
