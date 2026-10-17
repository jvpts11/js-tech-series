/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.OptionalLong;

/**
 * Where everything sits on the Space Persistor's screen, as the approved design lays it out, without the game: the
 * window, its steel face, the display of the land and its zoom keys, and beside it the plates, the LCD, the shape keys,
 * the radius' drums, the switch and the lamps. Everything below the window's top is measured from the face's corner;
 * the display's own parts from the display's.
 */
public final class SpacePersistorLayout {

    /** The screen. */
    public static final int WIDTH = 244;
    public static final int HEIGHT = 191;
    /** The window round the face, under the tabs. */
    public static final int WINDOW_TOP = 27;
    public static final int WINDOW_BOTTOM = 189;
    /** The steel face. */
    public static final int FACE_X = 7;
    public static final int FACE_Y = 34;
    public static final int FACE_WIDTH = 230;
    public static final int FACE_HEIGHT = 148;
    /** The picture the face is drawn from, and its sprites: the lit key and the Main tab's icon. */
    public static final int SHEET = 256;
    public static final int SPRITES_Y = 152;
    public static final int LIT_KEY_WIDTH = 20;
    public static final int LIT_KEY_HEIGHT = 15;
    public static final int TAB_ICON_X = 24;
    public static final int TAB_ICON = 16;
    /** The display of the land, square. */
    public static final int DISPLAY_X = 9;
    public static final int DISPLAY_Y = 9;
    public static final int DISPLAY = 128;
    /** The column beside it: the plates. */
    public static final int COLUMN_X = 146;
    public static final int PLATE_Y = 5;
    public static final int NAME_Y = 19;
    public static final int PLATE_HEIGHT = 12;
    /** The longest a name plate draws, in pixels. */
    public static final int NAME_MOST = 82;
    /** The LCD, four lines. */
    public static final int LCD_X = 149;
    public static final int LCD_Y = 37;
    public static final int LCD_WIDTH = 72;
    public static final int LCD_HEIGHT = 43;
    public static final int LCD_PITCH = 10;
    /** The shape keys, in their order. */
    public static final int SHAPE_WORD_Y = 88;
    public static final int KEYS_Y = 96;
    public static final int KEY_PITCH = 19;
    public static final int KEY_WIDTH = 17;
    public static final int KEY_HEIGHT = 12;
    public static final int KEYS = 4;
    /** The radius' two drums and the size of the area beside them. */
    public static final int RADIUS_WORD_Y = 114;
    public static final int WHEEL_X = 148;
    public static final int WHEEL_Y = 122;
    public static final int WHEEL_DIGIT = 7;
    public static final int WHEEL_DRUMS = 2;
    public static final int WHEEL_HEIGHT = 11;
    public static final int SIZE_X = 169;
    public static final int SIZE_Y = 124;
    /** The switch and its words. */
    public static final int SWITCH_X = 209;
    public static final int SWITCH_Y = 112;
    public static final int SWITCH_WIDTH = 14;
    public static final int SWITCH_HEIGHT = 22;
    public static final int ON_X = 196;
    public static final int ON_Y = 115;
    public static final int OFF_X = 193;
    public static final int OFF_Y = 127;
    /** The lamps, KEEPING and FULL. */
    public static final int LAMPS_Y = 139;
    public static final int KEEPING_X = 147;
    public static final int FULL_X = 196;
    public static final int LAMP_WORD_GAP = 10;
    /** The display's zoom keys, on the glass at its top right. */
    public static final int ZOOM_X = DISPLAY - 12;
    public static final int ZOOM_Y = 3;
    public static final int ZOOM_PITCH = 12;
    public static final int ZOOM_SIZE = 10;
    /** The label of how many chunks the display shows, at its bottom left. */
    public static final int ACROSS_X = 2;
    public static final int ACROSS_Y = 119;
    /** How many pixels a chunk takes on the display at each zoom, the closest first. */
    private static final int[] CELLS = {8, 4, 2, 1};
    private static final float SMALL = 0.75f;
    private static final int BEZEL = 3;
    private static final int LAMP_BOX = 9;
    private static final int WHEEL_BEZEL_WIDTH = WHEEL_DRUMS * WHEEL_DIGIT + 6;
    private static final int WHEEL_BEZEL_HEIGHT = 15;
    private static final int SIZE_CHARS = 9;

    private SpacePersistorLayout() {
    }

    /**
     * Its face's parts, measured from the face's corner, so a test sees that none overlaps another or runs off the
     * face: the display in its bezel, the plates, the LCD, the keys, the drums, the switch and the lamps, with the
     * words engraved beside them at their longest.
     */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(FACE_WIDTH, FACE_HEIGHT);
        layout.box("display", DISPLAY_X - BEZEL, DISPLAY_Y - BEZEL, DISPLAY + 2 * BEZEL, DISPLAY + 2 * BEZEL);
        layout.box("plates", COLUMN_X, PLATE_Y, NAME_MOST, NAME_Y + PLATE_HEIGHT - PLATE_Y);
        layout.box("lcd", LCD_X - BEZEL, LCD_Y - BEZEL, LCD_WIDTH + 2 * BEZEL, LCD_HEIGHT + 2 * BEZEL);
        layout.text("shapeWord", COLUMN_X, SHAPE_WORD_Y, 5, SMALL);
        for (int k = 0; k < KEYS; k++) {
            // A key lit stands a pixel proud of its place; only one is lit, so its glow may touch the next key's.
            layout.box("key" + k, COLUMN_X + k * KEY_PITCH - 1, KEYS_Y - 1, KEY_PITCH, LIT_KEY_HEIGHT);
        }
        layout.text("radiusWord", COLUMN_X, RADIUS_WORD_Y, 6, SMALL);
        layout.box("drums", WHEEL_X - 2, WHEEL_Y - 2, WHEEL_BEZEL_WIDTH, WHEEL_BEZEL_HEIGHT);
        layout.text("size", SIZE_X, SIZE_Y, SIZE_CHARS, SMALL);
        layout.box("switch", SWITCH_X, SWITCH_Y, SWITCH_WIDTH, SWITCH_HEIGHT);
        layout.text("on", ON_X, ON_Y, 2, SMALL);
        layout.text("off", OFF_X, OFF_Y, 3, SMALL);
        layout.box("keepingLamp", KEEPING_X - 1, LAMPS_Y - 1, LAMP_BOX, LAMP_BOX);
        layout.text("keepingWord", KEEPING_X + LAMP_WORD_GAP, LAMPS_Y + 1, 7, SMALL);
        layout.box("fullLamp", FULL_X - 1, LAMPS_Y - 1, LAMP_BOX, LAMP_BOX);
        layout.text("fullWord", FULL_X + LAMP_WORD_GAP, LAMPS_Y + 1, 4, SMALL);
        return layout;
    }

    /** How many zooms the display has. */
    public static int zooms() {
        return CELLS.length;
    }

    /** How many pixels a chunk takes at {@code zoom}, held within the zooms. */
    public static int cell(final int zoom) {
        return CELLS[Math.clamp(zoom, 0, CELLS.length - 1)];
    }

    /** How many chunks across the display shows at {@code zoom}. */
    public static int across(final int zoom) {
        return DISPLAY / cell(zoom);
    }

    /** The column of the display the Persistor's own chunk stands in, at {@code zoom}. */
    public static int middle(final int zoom) {
        return across(zoom) / 2;
    }

    /** The shape key under {@code fx, fy} of the face, from 0, or -1 for none. */
    public static int keyAt(final double fx, final double fy) {
        for (int k = 0; k < KEYS; k++) {
            if (on(fx, fy, COLUMN_X + k * KEY_PITCH, KEYS_Y, KEY_WIDTH, KEY_HEIGHT)) {
                return k;
            }
        }
        return -1;
    }

    /** The radius' drum under {@code fx, fy}, the tens 0 and the units 1, or -1 for none. */
    public static int drumAt(final double fx, final double fy) {
        for (int d = 0; d < WHEEL_DRUMS; d++) {
            if (on(fx, fy, WHEEL_X + 1 + d * WHEEL_DIGIT, WHEEL_Y, WHEEL_DIGIT - 1, WHEEL_HEIGHT)) {
                return d;
            }
        }
        return -1;
    }

    /** Whether {@code fy} is on the upper half of the drums, which turns a drum up. */
    public static boolean drumUp(final double fy) {
        return fy < WHEEL_Y + WHEEL_HEIGHT / 2.0;
    }

    /** Whether {@code fx, fy} is on the switch. */
    public static boolean onSwitch(final double fx, final double fy) {
        return on(fx, fy, SWITCH_X, SWITCH_Y, SWITCH_WIDTH, SWITCH_HEIGHT);
    }

    /** Whether {@code fx, fy} is on the name plate, at most {@link #NAME_MOST} wide. */
    public static boolean onName(final double fx, final double fy) {
        return on(fx, fy, COLUMN_X, NAME_Y, NAME_MOST, PLATE_HEIGHT);
    }

    /** The zoom key under {@code fx, fy}: 0 for closer, 1 for further, -1 for none. */
    public static int zoomAt(final double fx, final double fy) {
        for (int k = 0; k < 2; k++) {
            if (on(fx, fy, DISPLAY_X + ZOOM_X, DISPLAY_Y + ZOOM_Y + k * ZOOM_PITCH, ZOOM_SIZE, ZOOM_SIZE)) {
                return k;
            }
        }
        return -1;
    }

    /**
     * The chunk under {@code fx, fy} of the face at {@code zoom}, as its offset from the Persistor's own packed by
     * {@link PersistorShapes#pack}; none off the display.
     */
    public static OptionalLong chunkAt(final double fx, final double fy, final int zoom) {
        final double dx = fx - DISPLAY_X;
        final double dy = fy - DISPLAY_Y;
        if (dx < 0 || dy < 0 || dx >= DISPLAY || dy >= DISPLAY) {
            return OptionalLong.empty();
        }
        final int cell = cell(zoom);
        return OptionalLong.of(PersistorShapes.pack((int) Math.floor(dx / cell) - middle(zoom),
                (int) Math.floor(dy / cell) - middle(zoom)));
    }

    private static boolean on(final double fx, final double fy, final int x, final int y, final int w, final int h) {
        return fx >= x && fx < x + w && fy >= y && fy < y + h;
    }
}
