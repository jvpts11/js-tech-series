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
 * An ASIC's window (computers/docs/design/teracoin.md): its maker and model in the header with its state; the panel of
 * where it pays and its pool, the pool's button beside Solo and SET; its hashboards, each one's share and heat; its
 * hashrate, draw and fans beside them; the inventory, and beside it the blocks it found, that it has no system, and how
 * long it has mined. Pure: the screen draws from here and the menu seats the inventory from here.
 */
public final class AsicLayout {

    public static final int WIDTH = 268;
    public static final int HEIGHT = 254;
    public static final int HEADER_X = 6;
    public static final int HEADER_Y = 6;
    public static final int HEADER_W = 256;
    public static final int TITLE_X = 10;
    public static final int TITLE_Y = 10;
    /** The panel of the payout address and the pool. */
    public static final int PAYS_X = 8;
    public static final int PAYS_Y = 28;
    public static final int PAYS_W = 252;
    public static final int PAYS_H = 66;
    /** The address well, its caption over it. */
    public static final int ADDRESS_X = 12;
    public static final int ADDRESS_Y = 41;
    public static final int ADDRESS_W = 244;
    public static final int ADDRESS_H = 14;
    public static final int PAYS_CAPTION_Y = 32;
    public static final int POOL_CAPTION_Y = 62;
    /** The pool's button, Solo and SET, on one row. */
    public static final int BUTTON_Y = 71;
    public static final int BUTTON_H = 16;
    public static final int POOL_X = 12;
    public static final int POOL_W = 120;
    public static final int SOLO_X = 136;
    public static final int SOLO_W = 50;
    public static final int SET_X = 190;
    public static final int SET_W = 66;
    /** The hashboards' panel and its rows. */
    public static final int BOARDS_X = 8;
    public static final int BOARDS_Y = 100;
    public static final int BOARDS_W = 160;
    public static final int BOARDS_H = 66;
    public static final int BOARD_ROW_Y = 114;
    public static final int BOARD_PITCH = 12;
    public static final int BOARD_BAR_X = 30;
    public static final int BOARD_BAR_W = 48;
    public static final int BOARD_RATE_X = 84;
    public static final int BOARD_HEAT_X = 140;
    /** The readouts beside the hashboards, and those beside the inventory. */
    public static final int READOUT_X = 174;
    public static final int READOUT_W = 86;
    public static final int READOUT_H = 20;
    public static final int READOUT_PITCH = 22;
    public static final int LOWER_X = 176;
    public static final int LOWER_Y = 174;
    public static final int LOWER_PITCH = 24;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 174;
    /** The most hashboards a model has: the Forge A2's four. */
    public static final int MOST_BOARDS = 4;

    private AsicLayout() {
    }

    /** Everything on the window of a model of {@code boards} hashboards. */
    public static GuiLayout layout(final int boards) {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        l.box("header", HEADER_X, HEADER_Y, HEADER_W, 16);
        l.box("pays", PAYS_X, PAYS_Y, PAYS_W, PAYS_H);
        l.box("boards", BOARDS_X, BOARDS_Y, BOARDS_W, BOARDS_H);
        for (int i = 0; i < 3; i++) {
            l.box("readout_" + i, READOUT_X, BOARDS_Y + i * READOUT_PITCH, READOUT_W, READOUT_H);
            l.box("lower_" + i, LOWER_X, LOWER_Y + i * LOWER_PITCH, READOUT_W, READOUT_H);
        }
        l.text("paysCaption", ADDRESS_X, PAYS_CAPTION_Y, 7, 0.75f);
        l.text("address", ADDRESS_X + 3, ADDRESS_Y + 3, 35, 1.0f);
        l.text("poolCaption", ADDRESS_X, POOL_CAPTION_Y, 4, 0.75f);
        l.text("boardsCaption", BOARDS_X + 4, BOARDS_Y + 4, 10, 0.75f);
        for (int i = 0; i < boards; i++) {
            final int y = BOARD_ROW_Y + i * BOARD_PITCH;
            l.text("board_" + i, BOARDS_X + 4, y, 2, 0.75f);
            l.text("rate_" + i, BOARD_RATE_X, y, 11, 0.75f);
            l.text("heat_" + i, BOARD_HEAT_X, y, 4, 0.75f);
        }
        l.playerInventory(INVENTORY_X, INVENTORY_Y);
        return l;
    }
}
