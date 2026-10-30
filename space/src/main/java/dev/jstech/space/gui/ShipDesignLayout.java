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
 * Where everything stands on the Starship Design screen, in the pixels of its frame, 854 by 480, drawn as large as the
 * window holds it over the ship seen in 3D on its dotted floor: the parts on a panel of dark glass at the left, the
 * screen's tag, the ship's name and the tools along the top, the flight check, the ship's figures, its room, its
 * finish and the dock on a panel at the right, the keys along the bottom; the middle left open on the ship.
 */
public final class ShipDesignLayout {

    public static final int WIDTH = 854;
    public static final int HEIGHT = 480;
    /** The panels: the parts at the left, the checks at the right, the tag and the keys along the middle. */
    public static final int LEFT_X0 = 6;
    public static final int LEFT_X1 = 178;
    public static final int RIGHT_X0 = 672;
    public static final int RIGHT_X1 = 848;
    public static final int MIDDLE_X0 = 186;
    public static final int MIDDLE_X1 = 666;
    public static final int PANEL_Y0 = 6;
    public static final int PANEL_Y1 = 474;
    public static final int TOP_Y1 = 52;
    public static final int BOTTOM_Y0 = 450;
    /** How much a panel's top left corner is cut. */
    public static final int CUT = 7;
    /** The screen's tag, the ship's name and its figures after it. */
    public static final int TAG_X1 = 318;
    public static final int TAG_Y1 = 21;
    public static final int NAME_X0 = 326;
    public static final int FIGURES_X0 = 380;
    public static final int TITLE_Y = 9;
    /** The tools' buttons, in a row under the tag. */
    public static final int TOOLS_X0 = 194;
    public static final int TOOLS_Y0 = 28;
    public static final int TOOL_H = 18;
    public static final int TOOL_GAP = 4;
    /** The parts' search line. */
    public static final int SEARCH_X0 = 14;
    public static final int SEARCH_X1 = 170;
    public static final int SEARCH_Y0 = 24;
    public static final int SEARCH_Y1 = 38;
    /** The kinds' tabs, in a row under the search. */
    public static final int TAB_X0 = 14;
    public static final int TAB_Y0 = 44;
    public static final int TAB_SIZE = 18;
    public static final int TAB_W = 20;
    public static final int TAB_STEP = 22;
    public static final int TABS = 7;
    /** The kind's name, and the makers' toggles under it. */
    public static final int KIND_Y = 67;
    public static final int MAKER_Y0 = 80;
    public static final int MAKER_H = 11;
    public static final int MAKER_GAP = 4;
    /** The cards: two across, three down, the rest scrolled to. */
    public static final int CARD_X0 = 14;
    public static final int CARD_Y0 = 96;
    public static final int CARD_W = 76;
    public static final int CARD_H = 62;
    public static final int CARD_STEP_X = 80;
    public static final int CARD_STEP_Y = 66;
    public static final int CARD_COLUMNS = 2;
    public static final int CARD_ROWS = 3;
    /** A card's picture of its part, over its name and maker. */
    public static final int CARD_PICTURE_H = 44;
    /** The chosen part's figures under the cards. */
    public static final int CHOSEN_Y = 300;
    public static final int CHOSEN_X1 = 170;
    public static final int FIGURE_STEP = 17;
    public static final int FIGURE_ROWS = 4;
    public static final int NOTE_Y = CHOSEN_Y + 98;
    public static final int NOTE_STEP = 9;
    public static final int NOTE_ROWS = 3;
    /** Whose interior it has and how it is painted, in the accent, under its note. */
    public static final int MAKER_LINE_Y = CHOSEN_Y + 130;
    /** The right panel's inner edges. */
    public static final int INNER_X0 = 682;
    public static final int INNER_X1 = 836;
    /** The flight check's lines. */
    public static final int CHECK_Y0 = 26;
    public static final int CHECK_STEP = 14;
    public static final int CHECK_ROWS = 8;
    public static final int CHECK_BOX = 10;
    /** The ship's figures. */
    public static final int SHIP_Y = 144;
    public static final int SHIP_ROWS = 5;
    /** The room in the hull, its three slots. */
    public static final int ROOM_Y = 246;
    public static final int SLOT_W = 50;
    public static final int SLOT_H = 20;
    public static final int SLOT_STEP = 54;
    public static final int SLOTS = 3;
    /** The finishes' swatches. */
    public static final int FINISH_Y = 302;
    public static final int SWATCH_W = 50;
    public static final int SWATCH_H = 22;
    public static final int SWATCH_STEP = 54;
    public static final int SWATCHES = 3;
    /** The dock's blocks. */
    public static final int DOCK_Y = 350;
    /** The keys along the bottom, and what the screen is doing, from the right. */
    public static final int KEYS_X0 = 196;
    public static final int KEYS_Y0 = 456;
    public static final int KEY_H = 12;
    public static final int STATUS_X1 = 656;
    public static final int STATUS_Y = 459;
    /* The widest a line of the right panel or of the chosen part's note is, in glyphs. */
    private static final int LINE_GLYPHS = 26;

    private ShipDesignLayout() {
    }

    /** The frame's panels and its lines of text, laid out, for a test to hold to the frame and apart. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT)
                .box("parts", LEFT_X0, PANEL_Y0, LEFT_X1 - LEFT_X0, PANEL_Y1 - PANEL_Y0)
                .box("checks", RIGHT_X0, PANEL_Y0, RIGHT_X1 - RIGHT_X0, PANEL_Y1 - PANEL_Y0)
                .box("top", MIDDLE_X0, PANEL_Y0, MIDDLE_X1 - MIDDLE_X0, TOP_Y1 - PANEL_Y0)
                .box("bottom", MIDDLE_X0, BOTTOM_Y0, MIDDLE_X1 - MIDDLE_X0, PANEL_Y1 - BOTTOM_Y0);
        layout.textBox("search", SEARCH_X0, SEARCH_Y0, SEARCH_X1 - SEARCH_X0, SEARCH_Y1 - SEARCH_Y0);
        for (int i = 0; i < TABS; i++) {
            layout.textBox("tab" + i, tabX(i), TAB_Y0, TAB_W, TAB_SIZE);
        }
        for (int i = 0; i < CARD_COLUMNS * CARD_ROWS; i++) {
            layout.textBox("card" + i, cardX(i), cardY(i), CARD_W, CARD_H);
        }
        final int line = Math.round(LINE_GLYPHS * GuiLayout.GLYPH_WIDTH * 3 / 4);
        final int tall = Math.round(GuiLayout.LINE_HEIGHT);
        for (int i = 0; i < FIGURE_ROWS; i++) {
            layout.textBox("figure" + i, CARD_X0, figureY(i), CHOSEN_X1 - CARD_X0, FIGURE_STEP - 2);
        }
        for (int i = 0; i < NOTE_ROWS; i++) {
            layout.textBox("note" + i, CARD_X0, NOTE_Y + i * NOTE_STEP, line, NOTE_STEP);
        }
        layout.textBox("maker", CARD_X0, MAKER_LINE_Y, line, 2 * NOTE_STEP);
        for (int i = 0; i < CHECK_ROWS; i++) {
            layout.textBox("check" + i, INNER_X0, CHECK_Y0 + i * CHECK_STEP, INNER_X1 - INNER_X0, CHECK_BOX);
        }
        for (int i = 0; i < SHIP_ROWS; i++) {
            layout.textBox("ship" + i, INNER_X0, shipY(i), INNER_X1 - INNER_X0, FIGURE_STEP - 2);
        }
        for (int i = 0; i < SLOTS; i++) {
            layout.textBox("slot" + i, INNER_X0 + i * SLOT_STEP, ROOM_Y + 14, SLOT_W, SLOT_H);
        }
        for (int i = 0; i < SWATCHES; i++) {
            layout.textBox("swatch" + i, INNER_X0 + i * SWATCH_STEP, FINISH_Y + 13, SWATCH_W, SWATCH_H);
        }
        return layout.textBox("dock", INNER_X0, DOCK_Y + 13, INNER_X1 - INNER_X0, 2 * tall + 20);
    }

    /** The left edge of the kind's tab {@code i}. */
    public static int tabX(final int i) {
        return TAB_X0 + i * TAB_STEP;
    }

    /** The left edge of the card {@code i} in sight, from the top left, across then down. */
    public static int cardX(final int i) {
        return CARD_X0 + i % CARD_COLUMNS * CARD_STEP_X;
    }

    /** The top edge of the card {@code i} in sight. */
    public static int cardY(final int i) {
        return CARD_Y0 + i / CARD_COLUMNS * CARD_STEP_Y;
    }

    /** The top of the chosen part's figure {@code i}. */
    public static int figureY(final int i) {
        return CHOSEN_Y + 26 + i * FIGURE_STEP;
    }

    /** The top of the ship's figure {@code i}. */
    public static int shipY(final int i) {
        return SHIP_Y + 13 + i * FIGURE_STEP;
    }

    /** Whether {@code x, y} of the frame is on a panel, where the mouse works the screen rather than the view. */
    public static boolean onPanel(final double x, final double y) {
        return x >= LEFT_X0 && x < LEFT_X1 && y >= PANEL_Y0 && y < PANEL_Y1
                || x >= RIGHT_X0 && x < RIGHT_X1 && y >= PANEL_Y0 && y < PANEL_Y1
                || x >= MIDDLE_X0 && x < MIDDLE_X1 && (y >= PANEL_Y0 && y < TOP_Y1 || y >= BOTTOM_Y0 && y < PANEL_Y1);
    }
}
