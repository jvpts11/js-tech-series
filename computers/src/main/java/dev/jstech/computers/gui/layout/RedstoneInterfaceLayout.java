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
 * Where everything sits on a Redstone Interface's screen: its header, the name field and the line under it, the
 * computer and the signal side by side, the two modes, the strength's cells, and the software box across the foot.
 */
public final class RedstoneInterfaceLayout {

    public static final int WIDTH = 200;
    public static final int HEIGHT = 180;

    public static final int MARGIN = 8;
    /** The era's header bar, a line under it: the glass bands of the Transition, the accent rule of the newer eras. */
    public static final int HEADER_Y = 2;
    public static final int TITLE_Y = 6;

    public static final int NAME_LABEL_Y = 25;
    public static final int NAME_Y = 33;
    public static final int NAME_H = 14;
    public static final int NOTE_Y = 50;

    public static final int TILE_Y = 62;
    public static final int TILE_W = 90;
    public static final int TILE_H = 22;
    public static final int COMPUTER_X = MARGIN;
    public static final int SIGNAL_X = 102;

    public static final int MODE_LABEL_Y = 88;
    public static final int MODE_Y = 98;
    public static final int MODE_W = 88;
    public static final int MODE_H = 16;
    public static final int IN_X = MARGIN;
    public static final int OUT_X = 104;

    public static final int STRENGTH_LABEL_Y = 118;
    public static final int CELLS_Y = 128;
    /** One cell a strength, 0 to 15, the first crossed for 0. */
    public static final int CELLS = 16;
    public static final int CELL_W = 9;
    public static final int CELL_H = 10;
    public static final int CELL_STEP = 10;
    public static final int NUMBER_X = 172;

    public static final int SOFTWARE_Y = 144;
    public static final int SOFTWARE_H = 30;
    public static final int CODE_Y = SOFTWARE_Y + 11;
    public static final int CODE_PITCH = 9;

    private RedstoneInterfaceLayout() {
    }

    /** Where cell {@code strength} of the strength's row starts, across. */
    public static int cellX(final int strength) {
        return MARGIN + strength * CELL_STEP;
    }

    /** The strength under {@code x}, {@code y} on the screen, or -1 when no cell is there. */
    public static int cellAt(final int x, final int y) {
        if (y < CELLS_Y || y >= CELLS_Y + CELL_H || x < MARGIN) {
            return -1;
        }
        final int strength = (x - MARGIN) / CELL_STEP;
        return strength < CELLS && x < cellX(strength) + CELL_W ? strength : -1;
    }

    /** The screen, with its longest words in every place. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT)
                .box("nameField", MARGIN, NAME_Y, WIDTH - 2 * MARGIN, NAME_H)
                .box("computerTile", COMPUTER_X, TILE_Y, TILE_W, TILE_H)
                .box("signalTile", SIGNAL_X, TILE_Y, TILE_W, TILE_H)
                .box("in", IN_X, MODE_Y, MODE_W, MODE_H)
                .box("out", OUT_X, MODE_Y, MODE_W, MODE_H)
                .box("cells", MARGIN, CELLS_Y, cellX(CELLS - 1) + CELL_W - MARGIN, CELL_H)
                .box("software", MARGIN, SOFTWARE_Y, WIDTH - 2 * MARGIN, SOFTWARE_H);
        l.text("title", MARGIN, TITLE_Y, 18, 1.0f);                       // "REDSTONE INTERFACE"
        // "TRANSITION", right-aligned
        l.text("era", WIDTH - MARGIN - Math.round(10 * GuiLayout.GLYPH_WIDTH), TITLE_Y, 10, 1.0f);
        l.text("nameLabel", MARGIN + 1, NAME_LABEL_Y, 4, 0.75f);           // "NAME"
        l.text("note", MARGIN, NOTE_Y, 40, 0.75f);                        // "Programs find this interface by its name"
        l.text("modeLabel", MARGIN + 1, MODE_LABEL_Y, 4, 0.75f);           // "MODE"
        l.text("mark", OUT_X, MODE_LABEL_Y, 21, 0.75f);                    // "Set by " and a class name, cut to fit
        l.text("strengthLabel", MARGIN + 1, STRENGTH_LABEL_Y, 8, 0.75f);   // "STRENGTH"
        l.text("number", NUMBER_X, CELLS_Y, 2, 1.0f);                      // "15"
        l.text("code", MARGIN + 3, CODE_Y + CODE_PITCH, 39, 0.75f);        // a line of code, cut to fit
        return l;
    }
}
