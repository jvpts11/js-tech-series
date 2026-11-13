/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import net.minecraft.client.gui.screens.Screen;

/**
 * The mouse's gestures on a screen as the game hands them to it: a drag, the button pressed, moved in steps and let
 * go, and a turn of the wheel. The agent's tools, the studio's recorded tests and the test kit all make them here, so a
 * drag one of them makes is the drag the others replay.
 */
public final class ScreenGestures {

    /* How many moves a drag is made of between where it starts and where it ends. */
    private static final int DRAG_MOVES = 8;

    private ScreenGestures() {
    }

    /**
     * Presses {@code button} at the screen's point ({@code x}, {@code y}), moves it in steps to ({@code toX},
     * {@code toY}) and lets it go there, as a hand dragging the mouse does.
     */
    public static void drag(final Screen screen, final double x, final double y, final double toX, final double toY,
                            final int button) {
        screen.mouseClicked(x, y, button);
        double lastX = x;
        double lastY = y;
        for (int move = 1; move <= DRAG_MOVES; move++) {
            final double atX = x + (toX - x) * move / DRAG_MOVES;
            final double atY = y + (toY - y) * move / DRAG_MOVES;
            screen.mouseDragged(atX, atY, button, atX - lastX, atY - lastY);
            lastX = atX;
            lastY = atY;
        }
        screen.mouseReleased(toX, toY, button);
    }

    /** Turns the wheel {@code notches} at the screen's point ({@code x}, {@code y}): up when more than nought. */
    public static void scroll(final Screen screen, final double x, final double y, final double notches) {
        screen.mouseScrolled(x, y, 0.0, notches);
    }
}
