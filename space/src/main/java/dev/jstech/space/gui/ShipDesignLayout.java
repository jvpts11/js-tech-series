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
 * Where everything stands on the Starship Design screen, in the points of its frame, 480 by 270, the game's interface
 * at 1920 by 1080, so its letters are the game's own size; drawn whole points to the interface's where there is room,
 * over the ship seen in 3D on its dotted floor: the parts on a panel of dark glass at the left, the screen's tag, the
 * ship's name and the tools along the top, the flight check, the ship's figures and the dock on a panel at the right,
 * the keys along the bottom; the middle left open on the ship.
 */
public final class ShipDesignLayout {

    public static final int WIDTH = 480;
    public static final int HEIGHT = 270;
    /** The panels: the parts at the left, the checks at the right, the tag and the keys along the middle. */
    public static final int LEFT_X0 = 2;
    public static final int LEFT_X1 = 120;
    public static final int RIGHT_X0 = 360;
    public static final int RIGHT_X1 = 478;
    public static final int MIDDLE_X0 = 122;
    public static final int MIDDLE_X1 = 358;
    public static final int PANEL_Y0 = 2;
    public static final int PANEL_Y1 = 250;
    public static final int TOP_Y1 = 34;
    public static final int BOTTOM_Y0 = 252;
    public static final int BOTTOM_Y1 = 268;
    /** How much each panel's top left corner is cut: the sides', the top's, the bottom's. */
    public static final int CUT = 6;
    public static final int TOP_CUT = 5;
    public static final int BOTTOM_CUT = 4;
    /** The screen's tag, its words, the ship's name, and how far its figures stand after the name. */
    public static final int TAG_X1 = 222;
    public static final int TAG_Y1 = 14;
    public static final int TAG_TEXT_X = 128;
    public static final int NAME_X0 = 226;
    public static final int NAME_X1 = 300;
    public static final int FIGURES_GAP = 6;
    public static final int TITLE_Y = 4;
    /** The tools as icons in a row under the tag, the chosen one's name after them. */
    public static final int TOOLS_X0 = 126;
    public static final int TOOLS_Y0 = 17;
    public static final int TOOL_W = 17;
    public static final int TOOL_H = 15;
    public static final int TOOL_STEP = 19;
    public static final int TOOL_NAME_Y = 21;
    public static final int TOOLS = 6;
    /** The parts' search line. */
    public static final int SEARCH_X0 = 8;
    public static final int SEARCH_X1 = 114;
    public static final int SEARCH_Y0 = 14;
    public static final int SEARCH_Y1 = 25;
    /** The kinds' tabs, in a row under the search. */
    public static final int TAB_X0 = 8;
    public static final int TAB_Y0 = 29;
    public static final int TAB_W = 14;
    public static final int TAB_SIZE = 15;
    public static final int TAB_STEP = 15;
    public static final int TABS = 7;
    /** The kind's name, and the makers' toggles after it, from the right. */
    public static final int KIND_Y = 48;
    public static final int MAKER_H = 9;
    public static final int MAKER_GAP = 2;
    /** The cards: two across, two down, the rest scrolled to; how many more there are under them. */
    public static final int CARD_X0 = 8;
    public static final int CARD_Y0 = 59;
    public static final int CARD_W = 52;
    public static final int CARD_H = 48;
    public static final int CARD_STEP_X = 54;
    public static final int CARD_STEP_Y = 50;
    public static final int CARD_COLUMNS = 2;
    public static final int CARD_ROWS = 2;
    public static final int MORE_X = 64;
    public static final int MORE_Y = 160;
    /** A card's picture of its part, over its name and maker. */
    public static final int CARD_PICTURE_H = 28;
    /** The chosen part's figures under the cards, and its note. */
    public static final int CHOSEN_Y = 170;
    public static final int CHOSEN_X1 = 114;
    public static final int FIGURE_Y0 = 180;
    public static final int FIGURE_STEP = 11;
    public static final int FIGURE_ROWS = 4;
    public static final int NOTE_Y = 226;
    public static final int NOTE_STEP = 8;
    public static final int NOTE_ROWS = 3;
    /** The right panel's inner edges. */
    public static final int INNER_X0 = 366;
    public static final int INNER_X1 = 472;
    /** The flight check's lines, each its box and its words. */
    public static final int CHECK_Y0 = 15;
    public static final int CHECK_STEP = 10;
    public static final int CHECK_ROWS = 8;
    public static final int CHECK_BOX = 8;
    public static final int CHECK_TEXT_X = 378;
    /** The ship's figures, each its name and value over its bar. */
    public static final int SHIP_Y = 99;
    public static final int SHIP_ROWS = 5;
    public static final int SHIP_STEP = 15;
    /** The dock's blocks. */
    public static final int DOCK_Y = 187;
    /** The Release button under the dock's lines, the panel's width, and how tall it is. */
    public static final int RELEASE_Y0 = DOCK_Y + 47;
    public static final int RELEASE_H = 12;
    /**
     * The keys along the bottom, from and to, and the room between two, which narrows down to the least where the
     * words are long; what the screen says, over the keys.
     */
    public static final int KEYS_X0 = 127;
    public static final int KEYS_X1 = 354;
    public static final int KEYS_Y0 = 255;
    public static final int KEY_GAP = 7;
    public static final int KEY_GAP_LEAST = 3;
    public static final int SAID_Y = 238;
    /**
     * The plans' list over the middle, as the approved mock lays it: its glass, the tab of its title, how many plans it
     * keeps at its right, its rows of a plan each (the name, under it the length and mass, at the right its load and
     * delete), as many as show at once, and its line of help at its foot.
     */
    public static final int PLANS_X0 = 131;
    public static final int PLANS_X1 = 351;
    public static final int PLANS_Y0 = 39;
    public static final int PLANS_Y1 = 201;
    public static final int PLANS_TAB_X1 = 179;
    public static final int PLANS_TAB_Y1 = 50;
    public static final int PLANS_TITLE_Y = 41;
    public static final int PLAN_X0 = 135;
    public static final int PLAN_X1 = 347;
    public static final int PLAN_Y0 = 55;
    public static final int PLAN_H = 20;
    public static final int PLAN_STEP = 23;
    public static final int PLAN_ROWS = 6;
    public static final int PLAN_TEXT_X = 140;
    public static final int PLAN_NAME_DY = 2;
    public static final int PLAN_SIZE_DY = 12;
    public static final int PLAN_LOAD_X = 267;
    public static final int PLAN_DELETE_X = 306;
    public static final int PLAN_BUTTON_W = 36;
    public static final int PLAN_BUTTON_DY = 6;
    public static final int PLAN_BUTTON_H = 9;
    public static final int PLANS_HELP_Y = 192;
    /* The widest a line of the right panel is, the keys' row, the note's lines, in the game's letters. */
    private static final int CHECK_GLYPHS = 16;
    private static final int NOTE_GLYPHS = 28;

    private ShipDesignLayout() {
    }

    /** The frame's panels and its lines of text, laid out, for a test to hold to the frame and apart. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT)
                .box("parts", LEFT_X0, PANEL_Y0, LEFT_X1 - LEFT_X0, PANEL_Y1 - PANEL_Y0)
                .box("checks", RIGHT_X0, PANEL_Y0, RIGHT_X1 - RIGHT_X0, PANEL_Y1 - PANEL_Y0)
                .box("top", MIDDLE_X0, PANEL_Y0, MIDDLE_X1 - MIDDLE_X0, TOP_Y1 - PANEL_Y0)
                .box("bottom", MIDDLE_X0, BOTTOM_Y0, MIDDLE_X1 - MIDDLE_X0, BOTTOM_Y1 - BOTTOM_Y0);
        final int tall = Math.round(GuiLayout.LINE_HEIGHT);
        layout.textBox("search", SEARCH_X0, SEARCH_Y0, SEARCH_X1 - SEARCH_X0, SEARCH_Y1 - SEARCH_Y0);
        for (int i = 0; i < TABS; i++) {
            layout.textBox("tab" + i, tabX(i), TAB_Y0, TAB_W, TAB_SIZE);
        }
        for (int i = 0; i < TOOLS; i++) {
            layout.textBox("tool" + i, toolX(i), TOOLS_Y0, TOOL_W, TOOL_H);
        }
        for (int i = 0; i < CARD_COLUMNS * CARD_ROWS; i++) {
            layout.textBox("card" + i, cardX(i), cardY(i), CARD_W, CARD_H);
        }
        for (int i = 0; i < FIGURE_ROWS; i++) {
            layout.textBox("figure" + i, CARD_X0, figureY(i), CHOSEN_X1 - CARD_X0, FIGURE_STEP);
        }
        final int note = Math.round(NOTE_GLYPHS * GuiLayout.GLYPH_WIDTH * 3 / 4);
        for (int i = 0; i < NOTE_ROWS; i++) {
            layout.textBox("note" + i, CARD_X0, NOTE_Y + i * NOTE_STEP, note, NOTE_STEP);
        }
        final int check = Math.round(CHECK_GLYPHS * GuiLayout.GLYPH_WIDTH);
        for (int i = 0; i < CHECK_ROWS; i++) {
            layout.textBox("check" + i, CHECK_TEXT_X, CHECK_Y0 + i * CHECK_STEP, check, CHECK_BOX);
        }
        for (int i = 0; i < SHIP_ROWS; i++) {
            layout.textBox("ship" + i, INNER_X0, shipY(i), INNER_X1 - INNER_X0, SHIP_STEP - 1);
        }
        layout.textBox("dock", INNER_X0, DOCK_Y + 9, INNER_X1 - INNER_X0, 3 * tall + 10);
        return layout.textBox("release", INNER_X0, RELEASE_Y0, INNER_X1 - INNER_X0, RELEASE_H);
    }

    /**
     * The plans' list laid out, every row full: its rows apart and inside it, the name and the figures of each clear
     * of its buttons, the help under the last; for a test to hold to the frame and apart.
     */
    public static GuiLayout plansLayout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT)
                .box("tab", PLANS_X0, PLANS_Y0, PLANS_TAB_X1 - PLANS_X0, PLANS_TAB_Y1 - PLANS_Y0);
        final int small = Math.round(GuiLayout.LINE_HEIGHT * 3 / 4);
        for (int i = 0; i < PLAN_ROWS; i++) {
            final int y = planY(i);
            layout.box("load" + i, PLAN_LOAD_X, y + PLAN_BUTTON_DY, PLAN_BUTTON_W, PLAN_BUTTON_H)
                    .box("delete" + i, PLAN_DELETE_X, y + PLAN_BUTTON_DY, PLAN_BUTTON_W, PLAN_BUTTON_H)
                    .box("name" + i, PLAN_TEXT_X, y + PLAN_NAME_DY, PLAN_LOAD_X - 4 - PLAN_TEXT_X,
                            Math.round(GuiLayout.LINE_HEIGHT))
                    .box("size" + i, PLAN_TEXT_X, y + PLAN_SIZE_DY, PLAN_LOAD_X - 4 - PLAN_TEXT_X, small);
        }
        return layout.box("help", PLAN_X0, PLANS_HELP_Y, PLAN_X1 - PLAN_X0, small);
    }

    /** The top of the plans' row {@code i} in sight. */
    public static int planY(final int i) {
        return PLAN_Y0 + i * PLAN_STEP;
    }

    /** Whether {@code x, y} of the frame is on the plans' list. */
    public static boolean onPlans(final double x, final double y) {
        return x >= PLANS_X0 && x < PLANS_X1 && y >= PLANS_Y0 && y < PLANS_Y1;
    }

    /** The left edge of the kind's tab {@code i}. */
    public static int tabX(final int i) {
        return TAB_X0 + i * TAB_STEP;
    }

    /** The left edge of the tool's button {@code i}. */
    public static int toolX(final int i) {
        return TOOLS_X0 + i * TOOL_STEP;
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
        return FIGURE_Y0 + i * FIGURE_STEP;
    }

    /** The top of the ship's figure {@code i}. */
    public static int shipY(final int i) {
        return SHIP_Y + 9 + i * SHIP_STEP;
    }

    /** Whether {@code x, y} of the frame is on a panel, where the mouse works the screen rather than the view. */
    public static boolean onPanel(final double x, final double y) {
        return x >= LEFT_X0 && x < LEFT_X1 && y >= PANEL_Y0 && y < PANEL_Y1
                || x >= RIGHT_X0 && x < RIGHT_X1 && y >= PANEL_Y0 && y < PANEL_Y1
                || x >= MIDDLE_X0 && x < MIDDLE_X1 && (y >= PANEL_Y0 && y < TOP_Y1 || y >= BOTTOM_Y0 && y < BOTTOM_Y1);
    }

    /** Whether {@code x, y} of the frame is on the Release button, where it stands. */
    public static boolean onRelease(final double x, final double y) {
        return x >= INNER_X0 && x < INNER_X1 && y >= RELEASE_Y0 && y < RELEASE_Y0 + RELEASE_H;
    }
}
