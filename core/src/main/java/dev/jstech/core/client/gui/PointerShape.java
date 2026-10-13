/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * The shape of the mouse pointer over a screen: the system's arrow, or its pointing hand over what takes a click, as a
 * link on a web page shows. A screen says on each frame whether the pointer is on something it can click, and puts
 * the arrow back when it closes. The shape is changed only when it differs, and only on the render thread, where
 * frames are drawn.
 */
public final class PointerShape {

    /** The system's pointing hand, made the first time it is asked for and kept for the rest of the game. */
    private static long hand;
    private static boolean pointing;

    private PointerShape() {
    }

    /** Shows the hand when the pointer is on something that takes a click, and the arrow when it is not. */
    public static void point(final boolean onSomethingClickable) {
        if (onSomethingClickable == pointing) {
            return;
        }
        pointing = onSomethingClickable;
        if (onSomethingClickable && hand == 0L) {
            hand = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        }
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), onSomethingClickable ? hand : 0L);
    }

    /** Puts the arrow back, as a screen that showed the hand does when it closes. */
    public static void reset() {
        point(false);
    }

    /** Whether the hand is shown now. */
    public static boolean pointing() {
        return pointing;
    }
}
