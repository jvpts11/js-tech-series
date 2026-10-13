/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * The geometry of Virtual Studio's Properties window, in desktop units measured from the window's top left: the
 * project's lines, the platform it is built for, and the version of the language it is held to, with the words under
 * each and the Close button. The window and its test both read it, so what the test proves clean is what is drawn.
 */
public final class StudioPropertiesLayout {

    public static final int WIDTH = 290;
    public static final int HEIGHT = 170;
    public static final int PAD = 4;
    /** Where the window's content starts, under its title. */
    public static final int CONTENT_Y = 14;
    public static final int LINE_H = 9;
    /** The small lines under the version buttons, written at three quarters of the font. */
    public static final int SMALL_LINE_H = 8;
    public static final float SMALL = 0.75f;
    public static final int LINES = 5;
    public static final int LINES_Y = CONTENT_Y + 2;
    public static final int PLATFORM_LABEL_Y = LINES_Y + LINES * LINE_H + 2;
    public static final int PLATFORM_BUTTONS_Y = PLATFORM_LABEL_Y + 10;
    public static final int BUTTON_H = 11;
    public static final int PLATFORM_BUTTON_W = 30;
    public static final int BUTTON_GAP = 2;
    public static final int PLATFORM_HINT_Y = PLATFORM_BUTTONS_Y + BUTTON_H + 2;
    public static final int VERSION_LABEL_Y = PLATFORM_HINT_Y + LINE_H + 3;
    public static final int VERSION_BUTTONS_Y = VERSION_LABEL_Y + 10;
    /** Wide enough for "Default (Σ# 2)" in the small font, with its margins. */
    public static final int DEFAULT_BUTTON_W = 64;
    /** Wide enough for "Σ# 2" in the small font, with its margins. */
    public static final int VERSION_BUTTON_W = 26;
    /** The lines under the version buttons: what Default follows, the error a lower version gives, and the line. */
    public static final int VERSION_HINTS = 4;
    public static final int VERSION_HINT_Y = VERSION_BUTTONS_Y + BUTTON_H + 2;
    public static final int CLOSE_W = 34;
    public static final int CLOSE_Y = HEIGHT - 14;
    /**
     * The longest line under the version buttons, in full glyph widths, which the width is chosen to hold: the
     * example error for a project with a ten-letter name, 73 characters that come to about five pixels each in the
     * font because so many are spaces, quotes and commas. A longer name is cut short by the label, not spilled.
     */
    public static final int LONGEST_HINT = 61;

    private StudioPropertiesLayout() {
    }

    public static int platformButtonX(final int index) {
        return PAD + index * (PLATFORM_BUTTON_W + BUTTON_GAP);
    }

    /** Where a version's button starts: Default first, then one for each version. */
    public static int versionButtonX(final int index) {
        return index == 0 ? PAD : PAD + DEFAULT_BUTTON_W + BUTTON_GAP + (index - 1) * (VERSION_BUTTON_W + BUTTON_GAP);
    }

    public static int versionButtonW(final int index) {
        return index == 0 ? DEFAULT_BUTTON_W : VERSION_BUTTON_W;
    }

    public static int versionHintY(final int line) {
        return VERSION_HINT_Y + line * SMALL_LINE_H;
    }

    public static int closeX() {
        return WIDTH - CLOSE_W - PAD;
    }

    /**
     * Every piece of the window, the buttons as solid boxes and the words as text, for {@code platforms} instruction
     * sets and {@code versions} versions of the language beside Default.
     */
    public static GuiLayout layout(final int platforms, final int versions) {
        final GuiLayout l = new GuiLayout(WIDTH, HEIGHT);
        for (int i = 0; i < LINES; i++) {
            l.text("line-" + i, PAD, LINES_Y + i * LINE_H, 30, 1.0f);
        }
        l.text("platform-label", PAD, PLATFORM_LABEL_Y, 15, 1.0f);
        for (int i = 0; i < platforms; i++) {
            l.box("platform-" + i, platformButtonX(i), PLATFORM_BUTTONS_Y, PLATFORM_BUTTON_W, BUTTON_H);
        }
        l.text("platform-hint", PAD, PLATFORM_HINT_Y, 36, 1.0f);
        l.text("version-label", PAD, VERSION_LABEL_Y, 16, 1.0f);
        for (int i = 0; i <= versions; i++) {
            l.box("version-" + i, versionButtonX(i), VERSION_BUTTONS_Y, versionButtonW(i), BUTTON_H);
        }
        for (int i = 0; i < VERSION_HINTS; i++) {
            l.text("version-hint-" + i, PAD, versionHintY(i), LONGEST_HINT, SMALL);
        }
        l.box("close", closeX(), CLOSE_Y, CLOSE_W, BUTTON_H);
        return l;
    }
}
