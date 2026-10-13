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
 * <p>Scaling and sliding transform what is drawn without asking it to be drawn anywhere else, so a window grows or a
 * menu slides with every letter and picture of it in place. Fading a whole thing draws it off the screen first and
 * lays it over what is behind it as one picture, so what is inside it never shows through itself.
 *
 * <p>The waits with no known end, the blinking and the turning ({@link #BLINK}, {@link #LOOP}, {@link #BLOCKS},
 * {@link #SEGMENT}, {@link #GROW}, {@link #BOUNCE}) go round and round rather than end, each pass taking the motion's
 * time; {@link Rhythm} reads them.
 */
public final class MotionStyles {

    /** Nothing moves. */
    public static final String NONE = "none";
    /**
     * Grows or shrinks about a point: {@code from} is the size it starts at against its own and {@code to} the size
     * it ends at, both 1 where not given, and {@code from_y} and {@code to_y} the same downwards when it grows
     * unevenly; {@code pivot_x} and {@code pivot_y} are where the point is across and down it, from 0 to 1, the
     * middle by default. {@code opacity_from} and {@code opacity_to} say how solid it is drawn on the way, from 0
     * clear to 1 solid, both 1 where not given: one that changes them is drawn off the screen first.
     */
    public static final String SCALE = "scale";
    /**
     * The thing itself grows from clear to solid, or fades away with {@code out} set to 1: drawn off the screen
     * first, and laid over what is behind it at the strength the motion has reached.
     */
    public static final String APPEAR = "appear";
    /**
     * As {@link #ZOOM}, but only the thing's title bar travels, the thing itself already gone or not yet back, which
     * is how the systems of the nineties carried a window to its button: {@code out} set to 1 goes there.
     */
    public static final String CAPTION = "caption";
    /** On for its time and off for as long again, over and over: a text cursor, a busy light. */
    public static final String BLINK = "blink";
    /** Through its {@code frames} pictures over and over, one pass in its time: a throbber turning. */
    public static final String LOOP = "loop";
    /**
     * A bar for a wait with no known end: {@code blocks} small blocks, three where not given, crossing it, one pass
     * in its time.
     */
    public static final String BLOCKS = "blocks";
    /** A bar for a wait with no known end: one segment, {@code length} of the bar long, crossing it in its time. */
    public static final String SEGMENT = "segment";
    /** A bar for a wait with no known end: one segment crossing it in its time while it grows and shrinks again. */
    public static final String GROW = "grow";
    /**
     * A bar for a wait with no known end: one block, {@code length} of the bar long, going from one end to the other
     * in its time and back again in as long.
     */
    public static final String BOUNCE = "bounce";
    /** A number on its way to a new value over its time rather than at once: a progress bar filling. */
    public static final String EASE = "ease";
    /** What is behind a dialog loses its colour, to grey with {@code amount} 1, over its time. */
    public static final String GREY = "grey";
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
