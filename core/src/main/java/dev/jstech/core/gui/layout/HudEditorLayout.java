/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

/**
 * Where the HUD editor puts its parts: a box round each element with its name on a label above it and its eye in its
 * top right, the four squares of the chosen one's corners, and a small window a little under the middle of the screen
 * with what to do and Reset all, Cancel and Done. Pure, so a unit test checks that nothing overlaps.
 */
public final class HudEditorLayout {

    /** The window's size, and how far under the screen's middle its top is. */
    public static final int WIDTH = 200;
    public static final int HEIGHT = 68;
    public static final int BELOW_MIDDLE = 11;
    /** The room inside its edge, and where its lines are. */
    public static final int PAD = 8;
    public static final int TITLE_Y = 6;
    public static final int HOW_Y = 18;
    public static final int SNAPS_Y = 25;
    /** The buttons: their top, their height, their widths and the room between them. */
    public static final int BUTTONS_Y = 38;
    public static final int BUTTON_HEIGHT = 20;
    public static final int RESET_WIDTH = 58;
    public static final int CANCEL_WIDTH = 58;
    public static final int DONE_WIDTH = 60;
    public static final int BUTTON_GAP = 4;
    /** A box's label: how tall, and its text's room. */
    public static final int LABEL_HEIGHT = 12;
    public static final int LABEL_PAD = 4;
    /** A box's eye: its size and how far in from the box's top right. */
    public static final int EYE_WIDTH = 9;
    public static final int EYE_HEIGHT = 5;
    public static final int EYE_IN = 12;
    public static final int EYE_DOWN = 3;
    /** The squares at the chosen box's corners, and how far round one a press still takes it. */
    public static final int HANDLE = 3;
    public static final int HANDLE_REACH = 2;
    /** Every how many pixels the dashes of a box's edge start, and how long each is. */
    public static final int DASH_EVERY = 4;
    public static final int DASH = 2;

    private HudEditorLayout() {
    }

    /** The window's left on a screen {@code width} wide. */
    public static int left(final int width) {
        return (width - WIDTH) / 2;
    }

    /** The window's top on a screen {@code height} tall. */
    public static int top(final int height) {
        return height / 2 + BELOW_MIDDLE;
    }

    /** Cancel's left, from the window's. */
    public static int cancelX() {
        return PAD + RESET_WIDTH + BUTTON_GAP;
    }

    /** Done's left, from the window's. */
    public static int doneX() {
        return cancelX() + CANCEL_WIDTH + BUTTON_GAP;
    }

    /** The window, as the overlap check reads it. */
    public static GuiLayout layout() {
        final GuiLayout layout = new GuiLayout(WIDTH, HEIGHT);
        layout.text("title", PAD, TITLE_Y, 12, 1f);
        layout.text("how", PAD, HOW_Y, 40, 0.75f);
        layout.text("snaps", PAD, SNAPS_Y, 41, 0.75f);
        layout.box("reset", PAD, BUTTONS_Y, RESET_WIDTH, BUTTON_HEIGHT);
        layout.box("cancel", cancelX(), BUTTONS_Y, CANCEL_WIDTH, BUTTON_HEIGHT);
        layout.box("done", doneX(), BUTTONS_Y, DONE_WIDTH, BUTTON_HEIGHT);
        return layout;
    }
}
