/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything sits on the clamp meter held up, in the GUI's pixels from the top left of its picture: the
 * instrument's body below its jaws, the display, the four keys and the dial, and the lines the display writes. Pure:
 * the screen draws from these and the test checks they keep apart.
 */
public final class ClampMeterLayout {

    /** The picture: the jaws above the holstered body. */
    public static final int WIDTH = 250;
    public static final int HEIGHT = 270;
    /** The holstered body. */
    public static final int BODY_LEFT = 35;
    public static final int BODY_TOP = 64;
    public static final int BODY_RIGHT = 215;
    /** The label across the top of the face, and the lamp that says it is clamped, with its word. */
    public static final int TITLE_X = 49;
    public static final int TITLE_Y = 76;
    public static final int LAMP_X = 193;
    public static final int LAMP_Y = 76;
    public static final int LAMP_SIZE = 4;
    public static final int CLAMPED_RIGHT = 190;
    /** The display. */
    public static final int DISPLAY_LEFT = 51;
    public static final int DISPLAY_TOP = 88;
    public static final int DISPLAY_RIGHT = 199;
    public static final int DISPLAY_BOTTOM = 178;
    public static final int HEADER_HEIGHT = 11;
    public static final int TEXT_LEFT = 56;
    public static final int TEXT_RIGHT = 194;
    /** A line's reading: the value, big, then the lines under it. */
    public static final int VALUE_Y = 103;
    public static final float VALUE_SCALE = 1.5F;
    public static final int EACH_SECOND_Y = 124;
    public static final int SINCE_Y = 134;
    public static final int CEILING_Y = 145;
    public static final int MOST_LEAST_Y = 156;
    /** A block's reading: what it holds, then a row for each face. */
    public static final int STORED_Y = 102;
    public static final int FACES_Y = 113;
    public static final int FACE_PITCH = 10;
    public static final int FACE_WAY_X = 92;
    /** The keys, each its top left corner, all the same size. */
    public static final int KEY_WIDTH = 34;
    public static final int KEY_HEIGHT = 12;
    public static final int HOLD_X = 54;
    public static final int HOLD_Y = 200;
    public static final int MAX_MIN_X = 54;
    public static final int MAX_MIN_Y = 222;
    public static final int SHOW_X = 162;
    public static final int SHOW_Y = 200;
    public static final int LIGHT_X = 162;
    public static final int LIGHT_Y = 222;
    /** The dial: its centre, its knob's radius, and how far its printed positions sit from the centre. */
    public static final int DIAL_X = 125;
    public static final int DIAL_Y = 222;
    public static final int DIAL_RADIUS = 14;
    public static final int DIAL_LABEL_REACH = 24;
    /** The small text of the face and the display, as the game's font is drawn at three quarters. */
    public static final float SMALL = 0.75F;
    /** The most characters a line of the display holds in small text. */
    public static final int DISPLAY_CHARS = 30;

    private ClampMeterLayout() {
    }

    /** The layout, for the overlap and bounds checks. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("display", DISPLAY_LEFT, DISPLAY_TOP, DISPLAY_RIGHT - DISPLAY_LEFT, DISPLAY_BOTTOM - DISPLAY_TOP);
        layout.box("hold", HOLD_X, HOLD_Y, KEY_WIDTH, KEY_HEIGHT);
        layout.box("maxmin", MAX_MIN_X, MAX_MIN_Y, KEY_WIDTH, KEY_HEIGHT);
        layout.box("show", SHOW_X, SHOW_Y, KEY_WIDTH, KEY_HEIGHT);
        layout.box("light", LIGHT_X, LIGHT_Y, KEY_WIDTH, KEY_HEIGHT);
        layout.box("dial", DIAL_X - DIAL_RADIUS, DIAL_Y - DIAL_RADIUS, DIAL_RADIUS * 2, DIAL_RADIUS * 2);
        layout.box("lamp", LAMP_X, LAMP_Y, LAMP_SIZE, LAMP_SIZE);
        layout.text("title", TITLE_X, TITLE_Y, 16, SMALL);
        layout.text("since", TEXT_LEFT, SINCE_Y, DISPLAY_CHARS, SMALL);
        layout.text("stored", TEXT_LEFT, STORED_Y, DISPLAY_CHARS, SMALL);
        return layout;
    }

    /** Whether ({@code x}, {@code y}) falls on the key whose top left corner is ({@code keyX}, {@code keyY}). */
    public static boolean onKey(final double x, final double y, final int keyX, final int keyY) {
        return x >= keyX && x < keyX + KEY_WIDTH && y >= keyY && y < keyY + KEY_HEIGHT;
    }

    /** Whether ({@code x}, {@code y}) falls on the dial's knob. */
    public static boolean onDial(final double x, final double y) {
        final double dx = x - DIAL_X;
        final double dy = y - DIAL_Y;
        return dx * dx + dy * dy <= (DIAL_RADIUS + 1) * (DIAL_RADIUS + 1);
    }
}
