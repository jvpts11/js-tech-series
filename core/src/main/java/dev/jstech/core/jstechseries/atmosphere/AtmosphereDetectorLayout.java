/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.text.TextKey;

/**
 * Where everything of the Atmosphere Detector's screen stands, as the approved design lays it out, in the screen's
 * pixels from its top left: its tab above the window; on the steel face its plate; the pressure gauge with the figure
 * and how fast it changes under it; the air, its gases on a display and a bar of their shares; the room as the
 * detector draws it, with its size and leaks under it; the four lamps; and the key that shows the room in the world.
 * There is no inventory: a detector holds nothing. Pure, so its layout is tested without the game.
 */
public final class AtmosphereDetectorLayout {

    public static final int WIDTH = 320;
    public static final int HEIGHT = 207;
    public static final int TAB_X = 3;
    public static final int TAB_SIZE = 28;
    /** The window's top, below the tab. */
    public static final int WINDOW_Y = 27;
    public static final int FACE_LEFT = 7;
    public static final int FACE_TOP = 34;
    public static final int FACE_RIGHT = 313;
    public static final int FACE_BOTTOM = 200;
    public static final int PLATE_X = 16;
    public static final int PLATE_Y = 39;
    /** The gauge: its middle, and its radius. */
    public static final int DIAL_X = 53;
    public static final int DIAL_Y = 98;
    public static final int DIAL_RADIUS = 32;
    /** The highest pressure on the gauge, in kilopascals. */
    public static final int DIAL_MOST = 120;
    public static final int FIGURE_Y = 138;
    public static final int RATE_Y = 151;
    public static final int AIR_LABEL_X = 103;
    public static final int LABEL_Y = 58;
    public static final int AIR_X = 106;
    public static final int AIR_Y = 68;
    public static final int AIR_WIDTH = 104;
    public static final int AIR_HEIGHT = 53;
    public static final int AIR_LINES = 5;
    public static final int AIR_PITCH = 10;
    /** The bar of the gases' shares, and its words. */
    public static final int BAR_X = 106;
    public static final int BAR_Y = 128;
    public static final int BAR_WIDTH = 104;
    public static final int BAR_HEIGHT = 6;
    public static final int BAR_WORDS_Y = 138;
    public static final int ROOM_LABEL_X = 221;
    public static final int VIEW_X = 224;
    public static final int VIEW_Y = 68;
    public static final int VIEW_WIDTH = 82;
    public static final int VIEW_HEIGHT = 67;
    public static final int ROOM_X = 224;
    public static final int ROOM_Y = 145;
    public static final int ROOM_WIDTH = 82;
    public static final int ROOM_HEIGHT = 10;
    /** The lamps, two by two, and how far their words stand from them. */
    public static final int LAMP_LEFT = 105;
    public static final int LAMP_RIGHT = 163;
    public static final int LAMP_TOP = 159;
    public static final int LAMP_BOTTOM = 173;
    public static final int LAMP_SIZE = 7;
    public static final int LAMP_WORD = 10;
    /** The key that shows the room in the world, and its words. */
    public static final int SHOW_X = 224;
    public static final int SHOW_Y = 164;
    public static final int SHOW_WIDTH = 14;
    public static final int SHOW_HEIGHT = 12;
    public static final int WORLD_WORDS_Y = 181;
    /** The small text's scale. */
    public static final float SMALL = 0.75F;
    /* How many letters a line of the air's display holds, and the room's display. */
    private static final int AIR_CHARS = 26;
    private static final int ROOM_CHARS = 18;

    private AtmosphereDetectorLayout() {
    }

    /** Whether a point is on the key that shows the room in the world. */
    public static boolean onShow(final double x, final double y) {
        return x >= SHOW_X && x < SHOW_X + SHOW_WIDTH && y >= SHOW_Y && y < SHOW_Y + SHOW_HEIGHT;
    }

    /** The screen's layout, its longest words in, for the test that nothing on it overlaps. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("tab", TAB_X, 0, TAB_SIZE, TAB_SIZE);
        layout.box("dial", DIAL_X - DIAL_RADIUS - 4, DIAL_Y - DIAL_RADIUS - 4, DIAL_RADIUS * 2 + 8,
                DIAL_RADIUS * 2 + 8);
        layout.box("air", AIR_X - 3, AIR_Y - 3, AIR_WIDTH + 6, AIR_HEIGHT + 6);
        layout.box("bar", BAR_X - 1, BAR_Y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2);
        layout.box("view", VIEW_X - 3, VIEW_Y - 3, VIEW_WIDTH + 6, VIEW_HEIGHT + 6);
        layout.box("room", ROOM_X - 3, ROOM_Y - 3, ROOM_WIDTH + 6, ROOM_HEIGHT + 6);
        for (final int x : new int[] {LAMP_LEFT, LAMP_RIGHT}) {
            for (final int y : new int[] {LAMP_TOP, LAMP_BOTTOM}) {
                layout.box("lamp" + x + "," + y, x - 1, y - 1, LAMP_SIZE + 2, LAMP_SIZE + 2);
            }
        }
        layout.box("show", SHOW_X, SHOW_Y, SHOW_WIDTH, SHOW_HEIGHT);
        layout.text("plate", PLATE_X + 8, PLATE_Y + 3, chars(AtmosphereTexts.PLATE), SMALL);
        layout.text("figure", DIAL_X - 20, FIGURE_Y, chars(AtmosphereTexts.KILOPASCALS) + 2, 1.0F);
        layout.text("rate", DIAL_X - 34, RATE_Y, chars(AtmosphereTexts.FALLING), SMALL);
        layout.text("airWord", AIR_LABEL_X, LABEL_Y, chars(AtmosphereTexts.THE_AIR), SMALL);
        layout.text("roomWord", ROOM_LABEL_X, LABEL_Y, chars(AtmosphereTexts.THE_ROOM), SMALL);
        layout.text("airLines", AIR_X + 3, AIR_Y + 3, AIR_CHARS, SMALL);
        layout.text("roomLine", ROOM_X + 3, ROOM_Y + 2, ROOM_CHARS, SMALL);
        layout.text("breathable", LAMP_LEFT + LAMP_WORD, LAMP_TOP + 1, chars(AtmosphereTexts.BREATHABLE), SMALL);
        layout.text("hypoxia", LAMP_LEFT + LAMP_WORD, LAMP_BOTTOM + 1, chars(AtmosphereTexts.HYPOXIA), SMALL);
        layout.text("leaking", LAMP_RIGHT + LAMP_WORD, LAMP_TOP + 1, chars(AtmosphereTexts.LEAKING), SMALL);
        layout.text("vacuum", LAMP_RIGHT + LAMP_WORD, LAMP_BOTTOM + 1, chars(AtmosphereTexts.VACUUM), SMALL);
        layout.text("showWord", SHOW_X + SHOW_WIDTH + 3, SHOW_Y + 3, chars(AtmosphereTexts.SHOW), SMALL);
        layout.text("worldWords", SHOW_X, WORLD_WORDS_Y, chars(AtmosphereTexts.IN_THE_WORLD), SMALL);
        return layout;
    }

    /* How many letters a key's English holds, a placeholder counted as three. */
    private static int chars(final TextKey key) {
        return key.english().replace("%s", "999").length();
    }
}
