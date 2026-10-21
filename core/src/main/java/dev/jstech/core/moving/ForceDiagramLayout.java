/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything sits on the diagram of forces of a moving structure, in the GUI's pixels from its top left, as the
 * approved design lays it out: the title across the top; the view of the structure on the left with its three views
 * and the switch between the forces apart and summed; the list of the forces on the right, each with its value, a bar
 * of how much it weighs now and its own switch; the five tiles below, its mass, the sum, its climb, its speed and the
 * air; and the two buttons along the foot. Pure: the screen draws from these, and the test checks they keep apart.
 */
public final class ForceDiagramLayout {

    /** The whole diagram. */
    public static final int WIDTH = 460;
    public static final int HEIGHT = 300;
    /** The title's bar across the top. */
    public static final int TITLE_HEIGHT = 23;
    public static final int TITLE_Y = 8;
    /** The view of the structure. */
    public static final int VIEW_X = 7;
    public static final int VIEW_Y = 29;
    public static final int VIEW_WIDTH = 270;
    public static final int VIEW_HEIGHT = 208;
    /** The three views, 3D, from the side and from above, and the switch between the forces apart and summed. */
    public static final int VIEW_BUTTON_Y = VIEW_Y + 5;
    public static final int VIEW_BUTTON_WIDTH = 40;
    public static final int VIEW_BUTTON_HEIGHT = 14;
    public static final int VIEW_BUTTON_X = VIEW_X + 5;
    public static final int VIEW_BUTTON_PITCH = 42;
    public static final int APART_WIDTH = 74;
    public static final int APART_X = VIEW_X + VIEW_WIDTH - APART_WIDTH - 5;
    /** The list of the forces. */
    public static final int LIST_X = 284;
    public static final int LIST_Y = 29;
    public static final int LIST_WIDTH = 169;
    public static final int LIST_HEIGHT = 208;
    public static final int LIST_HEAD_Y = LIST_Y + 5;
    /** The forces listed, and where each row of the list is. */
    public static final int ROWS = 7;
    public static final int ROW_Y = LIST_Y + 16;
    public static final int ROW_PITCH = 26;
    public static final int ROW_HEIGHT = 24;
    public static final int ROW_ICON_X = LIST_X + 6;
    public static final int ROW_NAME_X = LIST_X + 18;
    public static final int ROW_VALUE_RIGHT = LIST_X + 134;
    public static final int ROW_BAR_X = LIST_X + 18;
    public static final int ROW_BAR_WIDTH = 84;
    public static final int ROW_BAR_Y = 16;
    public static final int ROW_BAR_HEIGHT = 2;
    public static final int SWITCH_X = LIST_X + 140;
    public static final int SWITCH_Y = 6;
    public static final int SWITCH_WIDTH = 22;
    public static final int SWITCH_HEIGHT = 10;
    /** The five tiles below: the mass, the sum, the climb, the speed and the air. */
    public static final int TILES = 5;
    public static final int TILE_Y = 242;
    public static final int TILE_HEIGHT = 30;
    private static final int[] TILE_X = {8, 95, 193, 284, 374};
    private static final int[] TILE_WIDTH = {80, 91, 83, 84, 78};
    /** The two buttons along the foot: the view put back, and done. */
    public static final int FOOT_Y = 280;
    public static final int FOOT_BUTTON_HEIGHT = 15;
    public static final int RESET_X = 8;
    public static final int RESET_WIDTH = 90;
    public static final int DONE_WIDTH = 80;
    public static final int DONE_X = WIDTH - DONE_WIDTH - 8;
    /** The small text of the list's head and the tiles' names. */
    public static final float SMALL = 0.75F;

    private ForceDiagramLayout() {
    }

    /** The left edge of tile {@code tile}, of the five. */
    public static int tileX(final int tile) {
        return TILE_X[tile];
    }

    /** The width of tile {@code tile}, of the five. */
    public static int tileWidth(final int tile) {
        return TILE_WIDTH[tile];
    }

    /** The top of row {@code row} of the list. */
    public static int rowY(final int row) {
        return ROW_Y + row * ROW_PITCH;
    }

    /** Which row of the list the point {@code (x, y)} of the diagram is on, or -1 for none. */
    public static int rowAt(final double x, final double y) {
        if (x < LIST_X || x >= LIST_X + LIST_WIDTH) {
            return -1;
        }
        for (int row = 0; row < ROWS; row++) {
            if (y >= rowY(row) && y < rowY(row) + ROW_HEIGHT) {
                return row;
            }
        }
        return -1;
    }

    /** Whether the point {@code (x, y)} of the diagram is on the view of the structure. */
    public static boolean onView(final double x, final double y) {
        return x >= VIEW_X && x < VIEW_X + VIEW_WIDTH && y >= VIEW_Y && y < VIEW_Y + VIEW_HEIGHT;
    }

    /** Everything on the diagram, for the test that keeps it apart. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("title", 0, 0, WIDTH, TITLE_HEIGHT);
        layout.textBox("view", VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT);
        layout.textBox("list", LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT);
        for (int tile = 0; tile < TILES; tile++) {
            layout.box("tile_" + tile, tileX(tile), TILE_Y, tileWidth(tile), TILE_HEIGHT);
        }
        layout.box("reset", RESET_X, FOOT_Y, RESET_WIDTH, FOOT_BUTTON_HEIGHT);
        layout.box("done", DONE_X, FOOT_Y, DONE_WIDTH, FOOT_BUTTON_HEIGHT);
        return layout;
    }

    /** The parts inside the view and the list, which stand over them, for the test that keeps them apart. */
    public static GuiLayout inside() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        for (int view = 0; view < 3; view++) {
            layout.box("view_" + view, VIEW_BUTTON_X + view * VIEW_BUTTON_PITCH, VIEW_BUTTON_Y,
                    VIEW_BUTTON_WIDTH, VIEW_BUTTON_HEIGHT);
        }
        layout.box("apart", APART_X, VIEW_BUTTON_Y, APART_WIDTH, VIEW_BUTTON_HEIGHT);
        for (int row = 0; row < ROWS; row++) {
            layout.box("row_" + row, LIST_X + 2, rowY(row), LIST_WIDTH - 4, ROW_HEIGHT);
        }
        return layout;
    }
}
