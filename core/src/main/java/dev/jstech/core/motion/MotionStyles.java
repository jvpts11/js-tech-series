/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

/**
 * The ways a thing can move, as a motion file names them.
 *
 * <p>Scaling and sliding come first: they transform what is drawn without asking it to be drawn anywhere else, so a
 * window grows or a menu slides with every letter and picture of it in place. Fading a whole window needs it drawn
 * off the screen first, and waits for that.
 */
public final class MotionStyles {

    /** Nothing moves. */
    public static final String NONE = "none";
    /**
     * Grows or shrinks about a point: {@code from} is the size it starts at against its own and {@code to} the size
     * it ends at, both 1 where not given, and {@code from_y} and {@code to_y} the same downwards when it grows
     * unevenly; {@code pivot_x} and {@code pivot_y} are where the point is across and down it, from 0 to 1, the
     * middle by default.
     */
    public static final String SCALE = "scale";
    /**
     * Slides in from an edge, or out to one: {@code distance} is how far away it starts, in its own heights, and
     * {@code from_y} the side it comes from (1 below, -1 above); {@code out} set to 1 slides it away instead.
     */
    public static final String SLIDE = "slide";
    /**
     * Goes down to the place that stands for it, or comes back up from there: the whole thing drawn ever smaller
     * along the way from where it stood to that place (a window to its button on the panel), or the other way;
     * {@code out} set to 1 goes there, otherwise it comes back.
     */
    public static final String ZOOM = "zoom";
    /**
     * As {@link #ZOOM}, but only the outline of the thing travels, the thing itself already gone or not yet back,
     * which is how the oldest window managers drew a window going down to its button: {@code out} set to 1 goes
     * there, and {@code trail} is how many outlines are drawn behind the leading one, none by default.
     */
    public static final String OUTLINE = "outline";
    /**
     * A flat cover of one colour over the thing thins away, or thickens over it with {@code out} set to 1: the one
     * fade drawn without the thing being drawn off the screen first, as a boot picture's colour giving way to the
     * desktop behind it.
     */
    public static final String FADE = "fade";

    private MotionStyles() {
    }
}
