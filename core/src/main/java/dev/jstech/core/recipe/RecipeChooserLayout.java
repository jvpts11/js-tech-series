/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where the chooser of recipes sits, in the GUI's pixels from a window's top left: upright beside the result's slot,
 * as tall as the result's big frame, with an arrow up, the count and an arrow down; the same beside the crafting
 * table's result and the furnace's. Where it would stand out of the window, as beside the inventory's small grid, it
 * is not shown. Pure: the chooser is drawn from these, and the test checks they keep apart from the result.
 */
public final class RecipeChooserLayout {

    /** The chooser: as wide as a slot, as tall as the result's frame. */
    public static final int WIDTH = 16;
    public static final int HEIGHT = 26;
    /** How far right of the result's slot it starts, and how far above the slot's top. */
    public static final int BESIDE = 27;
    public static final int ABOVE = 4;
    /** The arrows: three rows each, from the top and up from the bottom; the count between them. */
    public static final int ARROW_TOP = 3;
    public static final int ARROW_BOTTOM = 4;
    public static final int ARROW_ROWS = 3;
    public static final int COUNT_Y = 10;
    /** The game's windows the chooser is shown in, and their results' slots. */
    public static final int WINDOW_WIDTH = 176;
    public static final int WINDOW_HEIGHT = 166;
    public static final int CRAFTING_RESULT_X = 124;
    public static final int FURNACE_RESULT_X = 116;
    public static final int RESULT_Y = 35;
    /** A result's big frame round its slot. */
    public static final int FRAME_OUT = 5;
    public static final int FRAME = 26;

    private RecipeChooserLayout() {
    }

    /** The chooser's left, beside a result's slot whose left is {@code resultX}. */
    public static int x(final int resultX) {
        return resultX + BESIDE;
    }

    /** The chooser's top, beside a result's slot whose top is {@code resultY}. */
    public static int y(final int resultY) {
        return resultY - ABOVE;
    }

    /** Whether the chooser beside a result's slot whose left is {@code resultX} stays in a window that wide. */
    public static boolean fits(final int resultX, final int windowWidth) {
        return x(resultX) + WIDTH <= windowWidth;
    }

    /** The result's frame and the chooser beside it, in one of the game's windows. */
    public static GuiLayout layout(final int resultX, final int resultY) {
        final GuiLayout layout = new GuiLayout(WINDOW_WIDTH, WINDOW_HEIGHT);
        layout.box("result", resultX - FRAME_OUT, resultY - FRAME_OUT, FRAME, FRAME);
        layout.box("chooser", x(resultX), y(resultY), WIDTH, HEIGHT);
        return layout;
    }
}
