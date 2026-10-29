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
 * Where everything stands on the Rocket Design screen, in the pixels of its frame, 854 by 480, drawn as large as the
 * window holds it over the hangar seen in 3D: the parts' bins on a plate at the left, the hangar's sign and the tools
 * on a plate along the top, the stages and their figures on a plate at the right, the push buttons on a plate along
 * the bottom; the middle left open on the rocket.
 */
public final class RocketDesignLayout {

    public static final int WIDTH = 854;
    public static final int HEIGHT = 480;
    /** The plates: the bins at the left, the stages at the right, the sign and the buttons along the middle. */
    public static final int LEFT_X0 = 6;
    public static final int LEFT_X1 = 178;
    public static final int RIGHT_X0 = 672;
    public static final int RIGHT_X1 = 848;
    public static final int MIDDLE_X0 = 186;
    public static final int MIDDLE_X1 = 666;
    public static final int PLATE_Y0 = 6;
    public static final int PLATE_Y1 = 474;
    public static final int TOP_Y1 = 52;
    public static final int BOTTOM_Y0 = 442;
    /** The categories' tabs, in a row under the bins' sign. */
    public static final int TAB_X0 = 14;
    public static final int TAB_Y0 = 28;
    public static final int TAB_SIZE = 22;
    public static final int TAB_STEP = 26;
    public static final int TABS = 6;
    /** The category's name and the diameters' toggles beside it, from the right. */
    public static final int CATEGORY_Y = 56;
    public static final int DIAMETER_W = 22;
    public static final int DIAMETER_H = 12;
    public static final int DIAMETER_STEP = 26;
    public static final int DIAMETERS_X1 = 170;
    /** The bins: two across, three down, the rest scrolled to. */
    public static final int CARD_X0 = 14;
    public static final int CARD_Y0 = 74;
    public static final int CARD_W = 76;
    public static final int CARD_H = 72;
    public static final int CARD_STEP_X = 80;
    public static final int CARD_STEP_Y = 76;
    public static final int CARD_COLUMNS = 2;
    public static final int CARD_ROWS = 3;
    /** The part's own picture in its bin, over its label card. */
    public static final int CARD_PICTURE_H = 42;
    /** The chosen part's spec sheet. */
    public static final int SHEET_X0 = 14;
    public static final int SHEET_Y0 = 306;
    public static final int SHEET_X1 = 170;
    public static final int SHEET_Y1 = 466;
    /** The hangar's sign, the design's name after it, the crane's lamp at the end. */
    public static final int SIGN_X0 = 192;
    public static final int SIGN_X1 = 366;
    public static final int SIGN_Y0 = 10;
    public static final int SIGN_Y1 = 26;
    public static final int NAME_X0 = 376;
    public static final int NAME_X1 = 470;
    public static final int COUNTS_X = 476;
    public static final int LAMP_X = 572;
    /** The tools' buttons, in a row under the sign. */
    public static final int TOOLS_X0 = 194;
    public static final int TOOLS_Y0 = 30;
    public static final int TOOL_H = 17;
    public static final int TOOL_GAP = 6;
    /** The push buttons along the bottom. */
    public static final int BUTTON_Y0 = 449;
    public static final int BUTTON_H = 18;
    public static final int BUTTON_GAP = 10;
    public static final int CONFIRM_X0 = 196;
    public static final int CONFIRM_W = 142;
    public static final int SAVE_W = 52;
    public static final int PLANS_W = 58;
    public static final int CLEAR_W = 58;
    public static final int HINT_X1 = 656;
    /** The right plate's inner edges. */
    public static final int INNER_X0 = 682;
    public static final int INNER_X1 = 838;
    /** The stages, three in sight, the rest scrolled to. */
    public static final int STAGE_Y0 = 28;
    public static final int STAGE_H = 26;
    public static final int STAGE_STEP = 30;
    public static final int STAGE_ROWS = 3;
    /** The delta-v on its display, the thrust against weight under it. */
    public static final int DELTA_V_Y = 120;
    public static final int LED_Y0 = 132;
    public static final int LED_Y1 = 156;
    public static final int LIFT_Y = 161;
    /** The destinations. */
    public static final int DESTINATIONS_Y = 174;
    public static final int REACH_Y0 = 187;
    public static final int REACH_STEP = 30;
    public static final int REACHES = 3;
    /** The checks, five in sight. */
    public static final int CHECKS_Y = 280;
    public static final int CHECK_Y0 = 293;
    public static final int CHECK_STEP = 14;
    public static final int CHECK_ROWS = 5;
    /** The sign lit READY or NOT READY. */
    public static final int STATUS_Y0 = 368;
    public static final int STATUS_Y1 = 390;
    /** The view's keys. */
    public static final int VIEW_Y = 398;
    public static final int KEY_Y0 = 411;
    public static final int KEY_STEP = 15;
    public static final int KEYS = 4;
    /** The plans' list, over the middle when open. */
    public static final int PLANS_X0 = 256;
    public static final int PLANS_X1 = 596;
    public static final int PLANS_Y0 = 80;
    public static final int PLAN_Y0 = 104;
    public static final int PLAN_STEP = 18;
    public static final int PLAN_ROWS = 16;
    public static final int PLANS_Y1 = PLAN_Y0 + PLAN_ROWS * PLAN_STEP + 6;
    public static final int PLAN_BUTTON_W = 44;
    /* The widest a line of the right plate is, in glyphs. */
    private static final int RIGHT_GLYPHS = 25;

    private RocketDesignLayout() {
    }

    /** The frame's solid parts and its lines of text, laid out, for a test to hold to the frame and apart. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT)
                .box("bins", LEFT_X0, PLATE_Y0, LEFT_X1 - LEFT_X0, PLATE_Y1 - PLATE_Y0)
                .box("stages", RIGHT_X0, PLATE_Y0, RIGHT_X1 - RIGHT_X0, PLATE_Y1 - PLATE_Y0)
                .box("top", MIDDLE_X0, PLATE_Y0, MIDDLE_X1 - MIDDLE_X0, TOP_Y1 - PLATE_Y0)
                .box("bottom", MIDDLE_X0, BOTTOM_Y0, MIDDLE_X1 - MIDDLE_X0, PLATE_Y1 - BOTTOM_Y0);
        for (int i = 0; i < CARD_COLUMNS * CARD_ROWS; i++) {
            layout.textBox("card" + i, cardX(i), cardY(i), CARD_W, CARD_H);
        }
        layout.textBox("sheet", SHEET_X0, SHEET_Y0, SHEET_X1 - SHEET_X0, SHEET_Y1 - SHEET_Y0);
        for (int i = 0; i < TABS; i++) {
            layout.textBox("tab" + i, TAB_X0 + i * TAB_STEP, TAB_Y0, TAB_SIZE, TAB_SIZE);
        }
        for (int i = 0; i < STAGE_ROWS; i++) {
            layout.textBox("stage" + i, INNER_X0, STAGE_Y0 + i * STAGE_STEP, INNER_X1 - INNER_X0, STAGE_H);
        }
        final int line = Math.round(RIGHT_GLYPHS * GuiLayout.GLYPH_WIDTH);
        final int tall = Math.round(GuiLayout.LINE_HEIGHT);
        layout.textBox("led", INNER_X0, LED_Y0, INNER_X1 - INNER_X0, LED_Y1 - LED_Y0)
                .textBox("liftoff", INNER_X0, LIFT_Y, line, tall);
        for (int i = 0; i < REACHES; i++) {
            layout.textBox("reach" + i, INNER_X0, REACH_Y0 + i * REACH_STEP, line, REACH_STEP - 2);
        }
        for (int i = 0; i < CHECK_ROWS; i++) {
            layout.textBox("check" + i, INNER_X0, CHECK_Y0 + i * CHECK_STEP, line, tall);
        }
        layout.textBox("status", INNER_X0, STATUS_Y0, INNER_X1 - INNER_X0, STATUS_Y1 - STATUS_Y0);
        for (int i = 0; i < KEYS; i++) {
            layout.textBox("key" + i, INNER_X0, KEY_Y0 + i * KEY_STEP, line, tall);
        }
        int x = CONFIRM_X0;
        for (final int width : new int[] {CONFIRM_W, SAVE_W, PLANS_W, CLEAR_W}) {
            layout.textBox("button" + x, x, BUTTON_Y0, width, BUTTON_H);
            x += width + BUTTON_GAP;
        }
        return layout.textBox("plans", PLANS_X0, PLANS_Y0, PLANS_X1 - PLANS_X0, PLANS_Y1 - PLANS_Y0);
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
                || x >= MIDDLE_X0 && x < MIDDLE_X1 && (y >= PLATE_Y0 && y < TOP_Y1 || y >= BOTTOM_Y0 && y < PLATE_Y1);
    }
}
