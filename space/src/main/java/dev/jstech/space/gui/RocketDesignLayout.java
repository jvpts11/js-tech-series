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
 * Where everything stands on the Rocket Design screen, in the points of its frame, 480 by 270, the game's interface at
 * 1920 by 1080, so its letters are the game's own size; drawn whole points to the interface's where there is room,
 * over the hangar seen in 3D: the parts' bins on a plate at the left, the hangar's sign and the tools on a plate along
 * the top, the stages and their figures on a plate at the right, the view's keys on a strip at the view's foot, the
 * push buttons on a plate along the bottom; the middle left open on the rocket.
 */
public final class RocketDesignLayout {

    public static final int WIDTH = 480;
    public static final int HEIGHT = 270;
    /** The plates: the bins at the left, the stages at the right, the sign and the buttons along the middle. */
    public static final int LEFT_X0 = 2;
    public static final int LEFT_X1 = 120;
    public static final int RIGHT_X0 = 360;
    public static final int RIGHT_X1 = 478;
    public static final int MIDDLE_X0 = 122;
    public static final int MIDDLE_X1 = 358;
    public static final int PLATE_Y0 = 2;
    public static final int PLATE_Y1 = 250;
    public static final int TOP_Y1 = 36;
    public static final int BOTTOM_Y0 = 252;
    public static final int BOTTOM_Y1 = 268;
    /** The categories' tabs, in a row under the bins' sign. */
    public static final int TAB_X0 = 6;
    public static final int TAB_Y0 = 19;
    public static final int TAB_W = 16;
    public static final int TAB_H = 14;
    public static final int TAB_STEP = 18;
    public static final int TABS = 6;
    /** The category's name and the diameters' toggles beside it, from the right. */
    public static final int CATEGORY_Y = 37;
    public static final int DIAMETER_Y0 = 36;
    public static final int DIAMETER_W = 13;
    public static final int DIAMETER_H = 9;
    public static final int DIAMETER_STEP = 15;
    public static final int DIAMETERS_X1 = 114;
    /** The bins: two across, two down, the rest scrolled to; how many more there are under them. */
    public static final int CARD_X0 = 6;
    public static final int CARD_Y0 = 49;
    public static final int CARD_W = 53;
    public static final int CARD_H = 45;
    public static final int CARD_STEP_X = 56;
    public static final int CARD_STEP_Y = 47;
    public static final int CARD_COLUMNS = 2;
    public static final int CARD_ROWS = 2;
    public static final int MORE_X = 66;
    public static final int MORE_Y = 144;
    /** The part's own picture in its bin, over its label card. */
    public static final int CARD_PICTURE_H = 26;
    /** The chosen part's spec sheet, its rows of figures, and its note at its foot. */
    public static final int SHEET_X0 = 6;
    public static final int SHEET_Y0 = 152;
    public static final int SHEET_X1 = 116;
    public static final int SHEET_Y1 = 247;
    public static final int SHEET_ROW_Y0 = SHEET_Y0 + 22;
    public static final int SHEET_ROW_STEP = 16;
    public static final int SHEET_ROWS = 4;
    /** The hangar's sign, the design's name after it, the crane's lamp at the end. */
    public static final int SIGN_X0 = 126;
    public static final int SIGN_X1 = 228;
    public static final int SIGN_Y0 = 5;
    public static final int SIGN_Y1 = 16;
    public static final int NAME_X0 = 233;
    public static final int NAME_X1 = 290;
    public static final int CRANE_X1 = 340;
    public static final int LAMP_X = 344;
    /** The tools as icons in a row under the sign, the chosen one named, the rocket's counts at the right. */
    public static final int TOOLS_X0 = 126;
    public static final int TOOLS_Y0 = 20;
    public static final int TOOL_W = 15;
    public static final int TOOL_H = 13;
    public static final int TOOL_STEP = 18;
    public static final int TOOL_NAME_Y = 23;
    public static final int TOOLS = 6;
    public static final int COUNTS_X1 = 354;
    /** The view's keys, on a strip at the view's foot; what the screen says, over it. */
    public static final int STRIP_Y0 = 240;
    public static final int SAID_Y = 226;
    /** The push buttons along the bottom. */
    public static final int BUTTON_Y0 = 255;
    public static final int BUTTON_H = 10;
    public static final int BUTTON_GAP = 5;
    public static final int CONFIRM_X0 = 126;
    public static final int CONFIRM_W = 94;
    public static final int SAVE_W = 36;
    public static final int PLANS_W = 40;
    public static final int CLEAR_W = 40;
    /** The right plate's inner edges. */
    public static final int INNER_X0 = 365;
    public static final int INNER_X1 = 473;
    /** The stages, two in sight, the rest scrolled to. */
    public static final int STAGE_Y0 = 18;
    public static final int STAGE_H = 14;
    public static final int STAGE_STEP = 16;
    public static final int STAGE_ROWS = 2;
    /** The delta-v on its display, the thrust against weight under it. */
    public static final int DELTA_V_Y = 51;
    public static final int LED_Y0 = 59;
    public static final int LED_Y1 = 72;
    public static final int LIFT_Y = 75;
    /** The destinations. */
    public static final int DESTINATIONS_Y = 83;
    public static final int REACH_Y0 = 91;
    public static final int REACH_STEP = 21;
    public static final int REACHES = 3;
    /** The checks, their lines wrapped whole, and the sign lit READY or NOT READY under them. */
    public static final int CHECKS_Y = 156;
    public static final int CHECK_Y0 = 164;
    public static final int CHECK_LINE = 9;
    public static final int CHECK_LINES = 7;
    public static final int STATUS_Y0 = 210;
    public static final int STATUS_H = 14;
    /** The plans' list, over the middle when open. */
    public static final int PLANS_X0 = 130;
    public static final int PLANS_X1 = 350;
    public static final int PLANS_Y0 = 40;
    public static final int PLAN_Y0 = 58;
    public static final int PLAN_STEP = 13;
    public static final int PLAN_ROWS = 12;
    public static final int PLANS_Y1 = PLAN_Y0 + PLAN_ROWS * PLAN_STEP + 4;
    public static final int PLAN_BUTTON_W = 30;

    private RocketDesignLayout() {
    }

    /** The frame's solid parts and its lines of text, laid out, for a test to hold to the frame and apart. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT)
                .box("bins", LEFT_X0, PLATE_Y0, LEFT_X1 - LEFT_X0, PLATE_Y1 - PLATE_Y0)
                .box("stages", RIGHT_X0, PLATE_Y0, RIGHT_X1 - RIGHT_X0, PLATE_Y1 - PLATE_Y0)
                .box("top", MIDDLE_X0, PLATE_Y0, MIDDLE_X1 - MIDDLE_X0, TOP_Y1 - PLATE_Y0)
                .box("bottom", MIDDLE_X0, BOTTOM_Y0, MIDDLE_X1 - MIDDLE_X0, BOTTOM_Y1 - BOTTOM_Y0);
        for (int i = 0; i < CARD_COLUMNS * CARD_ROWS; i++) {
            layout.textBox("card" + i, cardX(i), cardY(i), CARD_W, CARD_H);
        }
        layout.textBox("sheet", SHEET_X0, SHEET_Y0, SHEET_X1 - SHEET_X0, SHEET_Y1 - SHEET_Y0);
        for (int i = 0; i < TABS; i++) {
            layout.textBox("tab" + i, TAB_X0 + i * TAB_STEP, TAB_Y0, TAB_W, TAB_H);
        }
        for (int i = 0; i < TOOLS; i++) {
            layout.textBox("tool" + i, toolX(i), TOOLS_Y0, TOOL_W, TOOL_H);
        }
        for (int i = 0; i < STAGE_ROWS; i++) {
            layout.textBox("stage" + i, INNER_X0, STAGE_Y0 + i * STAGE_STEP, INNER_X1 - INNER_X0, STAGE_H);
        }
        layout.textBox("led", INNER_X0, LED_Y0, INNER_X1 - INNER_X0, LED_Y1 - LED_Y0);
        for (int i = 0; i < REACHES; i++) {
            layout.textBox("reach" + i, INNER_X0, REACH_Y0 + i * REACH_STEP, INNER_X1 - INNER_X0, REACH_STEP - 1);
        }
        layout.textBox("checks", INNER_X0, CHECK_Y0, INNER_X1 - INNER_X0, CHECK_LINES * CHECK_LINE);
        layout.textBox("status", INNER_X0, STATUS_Y0, INNER_X1 - INNER_X0, STATUS_H);
        for (int i = 0; i < 4; i++) {
            layout.textBox("button" + i, buttonX(i), BUTTON_Y0, buttonW(i), BUTTON_H);
        }
        return layout.textBox("plans", PLANS_X0, PLANS_Y0, PLANS_X1 - PLANS_X0, PLANS_Y1 - PLANS_Y0);
    }

    /** The left edge of the tool's button {@code i}. */
    public static int toolX(final int i) {
        return TOOLS_X0 + i * TOOL_STEP;
    }

    /** The left edge of the bin {@code i} in sight, from the top left, across then down. */
    public static int cardX(final int i) {
        return CARD_X0 + i % CARD_COLUMNS * CARD_STEP_X;
    }

    /** The top edge of the bin {@code i} in sight. */
    public static int cardY(final int i) {
        return CARD_Y0 + i / CARD_COLUMNS * CARD_STEP_Y;
    }

    /** The left edge of the push button {@code i}, from the left: confirm, save, plans, clear. */
    public static int buttonX(final int i) {
        final int[] widths = {CONFIRM_W, SAVE_W, PLANS_W, CLEAR_W};
        int x = CONFIRM_X0;
        for (int k = 0; k < i; k++) {
            x += widths[k] + BUTTON_GAP;
        }
        return x;
    }

    /** The width of the push button {@code i}. */
    public static int buttonW(final int i) {
        return new int[] {CONFIRM_W, SAVE_W, PLANS_W, CLEAR_W}[i];
    }

    /** Whether {@code x, y} of the frame is on a plate, where the mouse works the screen rather than the view. */
    public static boolean onPlate(final double x, final double y) {
        return x >= LEFT_X0 && x < LEFT_X1 && y >= PLATE_Y0 && y < PLATE_Y1
                || x >= RIGHT_X0 && x < RIGHT_X1 && y >= PLATE_Y0 && y < PLATE_Y1
                || x >= MIDDLE_X0 && x < MIDDLE_X1 && (y >= PLATE_Y0 && y < TOP_Y1 || y >= STRIP_Y0 && y < BOTTOM_Y1);
    }
}
