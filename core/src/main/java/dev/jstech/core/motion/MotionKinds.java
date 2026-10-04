/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

/**
 * The kinds of thing a system moves, as a motion file names them: each system's file says how each moves on it, and
 * a kind it does not name does not move.
 */
public final class MotionKinds {

    /** A window coming up. */
    public static final String WINDOW_OPEN = "window_open";
    /** A dialog coming up over the window that asked it; a system that does not name it opens dialogs at once. */
    public static final String DIALOG_OPEN = "dialog_open";
    /** A window going away; the screen keeps drawing what it was while it goes. */
    public static final String WINDOW_CLOSE = "window_close";
    /** A window going down to its button. */
    public static final String WINDOW_MINIMIZE = "window_minimize";
    /** A window coming back up from its button. */
    public static final String WINDOW_RESTORE = "window_restore";
    /** A menu or the launcher coming up. */
    public static final String MENU_SHOW = "menu_show";
    /** A boot or splash screen giving way to the desktop behind it. */
    public static final String SCENE_FADE = "scene_fade";

    private MotionKinds() {
    }
}
