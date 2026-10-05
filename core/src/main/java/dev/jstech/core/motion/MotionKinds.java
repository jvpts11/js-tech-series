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
    /** A tooltip coming up under the pointer. */
    public static final String TOOLTIP_SHOW = "tooltip_show";
    /** A notice coming up, a balloon over the tray. */
    public static final String NOTICE_SHOW = "notice_show";
    /** A notice going away; it is drawn going for as long as that takes. */
    public static final String NOTICE_HIDE = "notice_hide";
    /** What is behind a dialog that asks for the whole screen, dimming or losing its colour. */
    public static final String DIM = "dim";
    /** The text cursor of a console or a terminal. */
    public static final String CARET_BLINK = "caret_blink";
    /** A progress bar's fill moving to a new amount. */
    public static final String PROGRESS_FILL = "progress_fill";
    /** A bar for a wait with no known end. */
    public static final String PROGRESS_WAIT = "progress_wait";
    /** The sign that something is under way: a throbber turning, a busy light blinking. */
    public static final String BUSY = "busy";
    /** The pointer while the machine does not answer: an hourglass, a watch, a ring. */
    public static final String POINTER_BUSY = "pointer_busy";
    /** The pointer while something works and clicks still go through: the arrow with a small hourglass or ring. */
    public static final String POINTER_WORKING = "pointer_working";
    /** The pointer while a program starts, on the systems that bounced its icon beside the arrow. */
    public static final String POINTER_LAUNCH = "pointer_launch";
    /** A file copy's own animation, the paper flying from folder to folder. */
    public static final String COPY = "copy";

    private MotionKinds() {
    }
}
