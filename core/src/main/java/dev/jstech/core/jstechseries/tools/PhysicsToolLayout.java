/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything sits on the physics tool's panel, in the GUI's pixels from its top left, as the approved design
 * lays it out: its name across the top, the three modes below it, the chosen one framed, and the strength's slider
 * with what the wheel does beside it. The panel stands at the screen's left, above the hotbar and the action bar, so a
 * word the game tells there never runs over it on a narrow screen. Pure: the panel draws from these, and the test
 * checks they keep apart.
 */
public final class PhysicsToolLayout {

    /** The panel. */
    public static final int WIDTH = 188;
    public static final int HEIGHT = 55;
    /** How far the panel stands from the screen's left and from its foot: above the line the action bar is told on. */
    public static final int LEFT = 8;
    public static final int ABOVE_FOOT = 74;
    /** The tool's name. */
    public static final int TITLE_X = 5;
    public static final int TITLE_Y = 4;
    /* The longest the title and a line of the hint are, in characters, for the test. */
    private static final int TITLE_CHARS = 12;
    private static final int HINT_CHARS = 16;
    /** The three modes. */
    public static final int MODES = 3;
    public static final int MODE_X = 4;
    public static final int MODE_Y = 15;
    public static final int MODE_WIDTH = 58;
    public static final int MODE_HEIGHT = 13;
    public static final int MODE_PITCH = 61;
    /** The strength's slider. */
    public static final int SLIDER_X = 4;
    public static final int SLIDER_Y = 32;
    public static final int SLIDER_WIDTH = 104;
    public static final int SLIDER_HEIGHT = 18;
    public static final int KNOB_WIDTH = 6;
    /** What the wheel does, in three small lines beside the slider. */
    public static final int HINT_X = 112;
    public static final int HINT_Y = 33;
    public static final int HINT_PITCH = 6;
    public static final int HINT_LINES = 3;
    public static final float SMALL = 0.75F;

    private PhysicsToolLayout() {
    }

    /** The top of the panel on a screen {@code height} pixels tall. */
    public static int top(final int height) {
        return height - ABOVE_FOOT - HEIGHT;
    }

    /** The left edge of mode {@code mode}, of the three. */
    public static int modeX(final int mode) {
        return MODE_X + mode * MODE_PITCH;
    }

    /** Where the slider's knob stands for a strength of {@code hundredths}, from its left. */
    public static int knobX(final int hundredths) {
        return SLIDER_X + Math.round((SLIDER_WIDTH - KNOB_WIDTH) * Math.clamp(hundredths, 0, 100) / 100F);
    }

    /** Everything on the panel, for the test that keeps it apart. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", TITLE_X, TITLE_Y, TITLE_CHARS, 1F);
        for (int mode = 0; mode < MODES; mode++) {
            layout.box("mode_" + mode, modeX(mode), MODE_Y, MODE_WIDTH, MODE_HEIGHT);
        }
        layout.box("slider", SLIDER_X, SLIDER_Y, SLIDER_WIDTH, SLIDER_HEIGHT);
        layout.box("hint", HINT_X, HINT_Y, WIDTH - HINT_X - 2, HINT_PITCH * HINT_LINES);
        for (int line = 0; line < HINT_LINES; line++) {
            layout.text("hint_" + line, HINT_X, HINT_Y + line * HINT_PITCH, HINT_CHARS, SMALL);
        }
        return layout;
    }
}
