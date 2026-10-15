/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything sits on the network probe held up, in the GUI's pixels from the top left of its picture, as the
 * approved design draws it: the holstered body, the display with its header and its four pages' tabs, the four soft
 * keys under it, the pad with its OK key, and the HOLD, SHOW, LIGHT and POWER keys. Pure: the screen draws from these
 * and the test checks they keep apart.
 */
public final class ProbeLayout {

    /** The picture. */
    public static final int WIDTH = 250;
    public static final int HEIGHT = 270;
    /** The label across the top of the face, and the lamp that says it is linked, with its word. */
    public static final int TITLE_X = 39;
    public static final int TITLE_Y = 18;
    public static final int LAMP_X = 203;
    public static final int LAMP_Y = 18;
    public static final int LAMP_SIZE = 4;
    public static final int LINK_RIGHT = 200;
    /** The display, its header and the row of its pages' tabs along its foot. */
    public static final int DISPLAY_LEFT = 41;
    public static final int DISPLAY_TOP = 31;
    public static final int DISPLAY_RIGHT = 209;
    public static final int DISPLAY_BOTTOM = 162;
    public static final int HEADER_HEIGHT = 11;
    public static final int TABS_HEIGHT = 10;
    public static final int TEXT_LEFT = DISPLAY_LEFT + 4;
    public static final int TEXT_RIGHT = DISPLAY_RIGHT - 4;
    /** The first line of a page, under the header, and the last a page may write on, above the tabs. */
    public static final int PAGE_TOP = DISPLAY_TOP + HEADER_HEIGHT + 4;
    public static final int PAGE_BOTTOM = DISPLAY_BOTTOM - TABS_HEIGHT - 2;
    /** The four soft keys under the display, one for each page. */
    public static final int SOFT_X = DISPLAY_LEFT + 1;
    public static final int SOFT_Y = DISPLAY_BOTTOM + 9;
    public static final int SOFT_WIDTH = 40;
    public static final int SOFT_HEIGHT = 10;
    public static final int SOFT_PITCH = 42;
    public static final int PAGES = 4;
    /** The pad: its middle, its four arrow keys and the OK key between them. */
    public static final int PAD_X = 74;
    public static final int PAD_Y = 216;
    public static final int ARROW_SIZE = 12;
    public static final int OK_SIZE = 14;
    /** The four keys on the right, each its top left corner, all the same size. */
    public static final int KEY_WIDTH = 34;
    public static final int KEY_HEIGHT = 12;
    public static final int HOLD_X = 130;
    public static final int HOLD_Y = 198;
    public static final int SHOW_X = 172;
    public static final int SHOW_Y = 198;
    public static final int LIGHT_X = 130;
    public static final int LIGHT_Y = 218;
    public static final int POWER_X = 172;
    public static final int POWER_Y = 218;
    /** The small text of the face and the display, as the game's font is drawn at three quarters. */
    public static final float SMALL = 0.75F;
    /** A row of the members' page, and how many fit. */
    public static final int ROW_PITCH = 11;
    public static final int GROUP_PITCH = 9;
    /** The most characters a line of the display holds in small text. */
    public static final int DISPLAY_CHARS = 36;

    private ProbeLayout() {
    }

    /** The layout, for the overlap and bounds checks. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("display", DISPLAY_LEFT, DISPLAY_TOP, DISPLAY_RIGHT - DISPLAY_LEFT, DISPLAY_BOTTOM - DISPLAY_TOP);
        for (int k = 0; k < PAGES; k++) {
            layout.box("soft" + k, softX(k), SOFT_Y, SOFT_WIDTH, SOFT_HEIGHT);
        }
        layout.box("up", PAD_X - ARROW_SIZE / 2, PAD_Y - 22, ARROW_SIZE, ARROW_SIZE);
        layout.box("down", PAD_X - ARROW_SIZE / 2, PAD_Y + 10, ARROW_SIZE, ARROW_SIZE);
        layout.box("left", PAD_X - 22, PAD_Y - ARROW_SIZE / 2, ARROW_SIZE, ARROW_SIZE);
        layout.box("right", PAD_X + 10, PAD_Y - ARROW_SIZE / 2, ARROW_SIZE, ARROW_SIZE);
        layout.box("ok", PAD_X - OK_SIZE / 2, PAD_Y - OK_SIZE / 2, OK_SIZE, OK_SIZE);
        layout.box("hold", HOLD_X, HOLD_Y, KEY_WIDTH, KEY_HEIGHT);
        layout.box("show", SHOW_X, SHOW_Y, KEY_WIDTH, KEY_HEIGHT);
        layout.box("light", LIGHT_X, LIGHT_Y, KEY_WIDTH, KEY_HEIGHT);
        layout.box("power", POWER_X, POWER_Y, KEY_WIDTH, KEY_HEIGHT);
        layout.box("lamp", LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE);
        layout.text("title", TITLE_X, TITLE_Y, 16, SMALL);
        layout.text("line", TEXT_LEFT, PAGE_TOP, DISPLAY_CHARS, SMALL);
        return layout;
    }

    /** The left of the {@code k}th soft key. */
    public static int softX(final int k) {
        return SOFT_X + k * SOFT_PITCH;
    }

    /** The soft key under ({@code x}, {@code y}), or -1 for none. */
    public static int softAt(final double x, final double y) {
        for (int k = 0; k < PAGES; k++) {
            if (x >= softX(k) && x < softX(k) + SOFT_WIDTH && y >= SOFT_Y && y < SOFT_Y + SOFT_HEIGHT) {
                return k;
            }
        }
        return -1;
    }

    /** Whether ({@code x}, {@code y}) falls on the key whose top left corner is ({@code keyX}, {@code keyY}). */
    public static boolean onKey(final double x, final double y, final int keyX, final int keyY) {
        return x >= keyX && x < keyX + KEY_WIDTH && y >= keyY && y < keyY + KEY_HEIGHT;
    }

    /** The pad's key under ({@code x}, {@code y}), or none. */
    public static Pad padAt(final double x, final double y) {
        if (within(x, y, PAD_X - OK_SIZE / 2, PAD_Y - OK_SIZE / 2, OK_SIZE)) {
            return Pad.OK;
        }
        if (within(x, y, PAD_X - ARROW_SIZE / 2, PAD_Y - 22, ARROW_SIZE)) {
            return Pad.UP;
        }
        if (within(x, y, PAD_X - ARROW_SIZE / 2, PAD_Y + 10, ARROW_SIZE)) {
            return Pad.DOWN;
        }
        if (within(x, y, PAD_X - 22, PAD_Y - ARROW_SIZE / 2, ARROW_SIZE)) {
            return Pad.LEFT;
        }
        if (within(x, y, PAD_X + 10, PAD_Y - ARROW_SIZE / 2, ARROW_SIZE)) {
            return Pad.RIGHT;
        }
        return Pad.NONE;
    }

    /** How many rows of the members' page fit between the page's top and the line under them. */
    public static int rowsShown() {
        return (PAGE_BOTTOM - PAGE_TOP - 2 * GROUP_PITCH) / ROW_PITCH;
    }

    private static boolean within(final double x, final double y, final int left, final int top, final int size) {
        return x >= left && x < left + size && y >= top && y < top + size;
    }

    /** A key of the pad. */
    public enum Pad {
        UP, DOWN, LEFT, RIGHT, OK, NONE
    }
}
