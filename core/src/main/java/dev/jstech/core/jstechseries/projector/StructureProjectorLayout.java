/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.text.TextKey;

/**
 * Where everything of the Structure Projector's screen stands, as the approved design lays it out, in the screen's
 * pixels from its top left: the projector's tab above the window; on the steel face the name plate, the 3D preview
 * with its layer and its hint, the list of multiblocks and what is in the world on two displays, and the controls on
 * the right, the turn's knob, the mirror's and "missing only"'s toggles, the layer's wheel, the switch and its lamp.
 * There is no inventory: a projector holds nothing. Pure, so its layout is tested without the game.
 */
public final class StructureProjectorLayout {

    public static final int WIDTH = 314;
    public static final int HEIGHT = 206;
    public static final int TAB_X = 4;
    public static final int TAB_SIZE = 28;
    /** The window's top, below the tab. */
    public static final int WINDOW_Y = 26;
    public static final int FACE_LEFT = 6;
    public static final int FACE_TOP = 32;
    public static final int FACE_RIGHT = 308;
    public static final int FACE_BOTTOM = 200;
    public static final int PLATE_X = 14;
    public static final int PLATE_Y = 38;
    public static final int PREVIEW_X = 16;
    public static final int PREVIEW_Y = 57;
    public static final int PREVIEW_WIDTH = 130;
    public static final int PREVIEW_HEIGHT = 127;
    /** The line along the preview's foot: its layer on the left, the hint on the right. */
    public static final int PREVIEW_WORDS_Y = 176;
    public static final int STATUS_X = 16;
    public static final int STATUS_Y = 191;
    public static final int LIST_LABEL_X = 158;
    public static final int LIST_LABEL_Y = 41;
    public static final int LIST_X = 160;
    public static final int LIST_Y = 51;
    public static final int LIST_WIDTH = 88;
    public static final int ROWS = 5;
    public static final int ROW_HEIGHT = 10;
    public static final int LIST_HEIGHT = ROWS * ROW_HEIGHT + 4;
    public static final int WORLD_LABEL_X = 158;
    public static final int WORLD_LABEL_Y = 113;
    public static final int WORLD_X = 160;
    public static final int WORLD_Y = 123;
    public static final int WORLD_WIDTH = 88;
    public static final int WORLD_LINES = 4;
    public static final int WORLD_HEIGHT = WORLD_LINES * ROW_HEIGHT + 4;
    public static final int KNOB_X = 282;
    public static final int KNOB_Y = 67;
    public static final int KNOB_RADIUS = 9;
    /** How far round the knob its letters stand, and so how far a click on it reaches. */
    public static final int KNOB_REACH = KNOB_RADIUS + 13;
    public static final int TOGGLE_X = 262;
    public static final int MIRROR_Y = 93;
    public static final int MISSING_Y = 110;
    public static final int TOGGLE_WIDTH = 8;
    public static final int TOGGLE_HEIGHT = 12;
    public static final int TOGGLE_LABEL_X = 274;
    public static final int LAYER_LABEL_Y = 131;
    public static final int WHEEL_X = 263;
    public static final int WHEEL_Y = 141;
    public static final int WHEEL_WIDTH = 9;
    public static final int WHEEL_HEIGHT = 13;
    public static final int OF_X = 277;
    public static final int OF_Y = 144;
    public static final int ROCKER_X = 286;
    public static final int ROCKER_Y = 160;
    public static final int ROCKER_WIDTH = 14;
    public static final int ROCKER_HEIGHT = 22;
    public static final int ON_WORD_X = 266;
    public static final int ON_WORD_Y = 162;
    public static final int OFF_WORD_Y = 175;
    public static final int LAMP_X = 263;
    public static final int LAMP_Y = 190;
    public static final int LAMP_SIZE = 7;
    public static final int SHOWING_X = 274;
    /** The small text's scale. */
    public static final float SMALL = 0.75F;
    /** The most letters of the line along the face's foot: a long name, its size and its mod. */
    public static final int STATUS_CHARS = 52;

    private StructureProjectorLayout() {
    }

    /** The row of the list a point is on, or -1 for none. */
    public static int rowAt(final double x, final double y) {
        if (x < LIST_X || x >= LIST_X + LIST_WIDTH || y < LIST_Y + 2 || y >= LIST_Y + 2 + ROWS * ROW_HEIGHT) {
            return -1;
        }
        return (int) ((y - LIST_Y - 2) / ROW_HEIGHT);
    }

    /** Whether a point is on the turn's knob or its letters. */
    public static boolean onKnob(final double x, final double y) {
        return Math.hypot(x - KNOB_X, y - KNOB_Y) <= KNOB_REACH;
    }

    /** Whether a point is on the toggle whose top is at {@code top}, its words beside it counted in. */
    public static boolean onToggle(final double x, final double y, final int top) {
        return x >= TOGGLE_X && x < FACE_RIGHT && y >= top && y < top + TOGGLE_HEIGHT;
    }

    /** Whether a point is on the layer's wheel; its upper half raises it, its lower half lowers it. */
    public static boolean onWheel(final double x, final double y) {
        return x >= WHEEL_X - 2 && x < WHEEL_X + WHEEL_WIDTH + 2 && y >= WHEEL_Y - 2 && y < WHEEL_Y + WHEEL_HEIGHT;
    }

    /** Whether a point is on the switch. */
    public static boolean onRocker(final double x, final double y) {
        return x >= ROCKER_X && x < ROCKER_X + ROCKER_WIDTH && y >= ROCKER_Y && y < ROCKER_Y + ROCKER_HEIGHT;
    }

    /** The screen's layout, its longest words in, for the test that nothing on it overlaps. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.box("tab", TAB_X, 0, TAB_SIZE, TAB_SIZE);
        layout.box("preview", PREVIEW_X - 3, PREVIEW_Y - 3, PREVIEW_WIDTH + 6, PREVIEW_HEIGHT + 6);
        layout.box("list", LIST_X - 3, LIST_Y - 3, LIST_WIDTH + 6, LIST_HEIGHT + 6);
        layout.box("world", WORLD_X - 3, WORLD_Y - 3, WORLD_WIDTH + 6, WORLD_HEIGHT + 6);
        layout.box("knob", KNOB_X - KNOB_REACH + 2, KNOB_Y - KNOB_REACH + 4, (KNOB_REACH - 2) * 2,
                (KNOB_REACH - 4) * 2);
        layout.box("mirror", TOGGLE_X, MIRROR_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
        layout.box("missing", TOGGLE_X, MISSING_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
        layout.box("wheel", WHEEL_X - 2, WHEEL_Y - 2, WHEEL_WIDTH + 4, WHEEL_HEIGHT + 2);
        layout.box("rocker", ROCKER_X, ROCKER_Y, ROCKER_WIDTH, ROCKER_HEIGHT);
        layout.box("lamp", LAMP_X - 1, LAMP_Y - 1, LAMP_SIZE + 2, LAMP_SIZE + 2);
        layout.text("plate", PLATE_X + 8, PLATE_Y + 3, chars(ProjectorTexts.TITLE), SMALL);
        layout.text("multiblock", LIST_LABEL_X, LIST_LABEL_Y, chars(ProjectorTexts.MULTIBLOCK), SMALL);
        layout.text("worldWord", WORLD_LABEL_X, WORLD_LABEL_Y, chars(ProjectorTexts.IN_THE_WORLD), SMALL);
        layout.text("turn", KNOB_X - 8, KNOB_Y - KNOB_RADIUS - 17, chars(ProjectorTexts.TURN), SMALL);
        layout.text("mirrorWord", TOGGLE_LABEL_X, MIRROR_Y + 3, chars(ProjectorTexts.MIRROR), SMALL);
        layout.text("missingWord", TOGGLE_LABEL_X, MISSING_Y, chars(ProjectorTexts.MISSING_WORD), SMALL);
        layout.text("onlyWord", TOGGLE_LABEL_X, MISSING_Y + 7, chars(ProjectorTexts.ONLY), SMALL);
        layout.text("layer", TOGGLE_X, LAYER_LABEL_Y, chars(ProjectorTexts.LAYER), SMALL);
        layout.text("of", OF_X, OF_Y, chars(ProjectorTexts.OF), SMALL);
        layout.text("on", ON_WORD_X, ON_WORD_Y, chars(ProjectorTexts.ON), SMALL);
        layout.text("off", ON_WORD_X - 2, OFF_WORD_Y, chars(ProjectorTexts.OFF), SMALL);
        layout.text("showing", SHOWING_X, LAMP_Y, chars(ProjectorTexts.SHOWING), SMALL);
        layout.text("status", STATUS_X, STATUS_Y, STATUS_CHARS, SMALL);
        return layout;
    }

    /* How many letters a key's English holds, a placeholder counted as two. */
    private static int chars(final TextKey key) {
        return key.english().replace("%s", "99").length();
    }
}
