/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

/**
 * A ship's door moving, and the boarding stair its opening lets down, as the approved storyboard has them. A door's
 * leaf slides open over {@link #PRESSURE_DOOR} seconds, or {@link #OUTER_DOOR} for the airlock's outer door. Opened on
 * the land, the outer door's stair comes down in {@link #STAIR} seconds, in the storyboard's five frames a quarter of
 * that apart: the bay's doors open down as the leaf slides; the folded stair slides out under the sill; its first
 * flight swings down as the others unfold; and its foot stands on the land, its handrails rising last. Shutting goes
 * the same way back, the bay's doors closing before the leaf slides shut. In the air, or in space, the leaf moves
 * alone and a stair let down goes up. Pure: moved on by the time since the last frame.
 */
public final class DoorMotion {

    private double leaf;
    private double stair;
    /** How long a pressure door's leaf takes to slide, in seconds. */
    public static final double PRESSURE_DOOR = 0.5;
    /** How long the airlock's outer door's leaf takes to slide, in seconds: the storyboard's first two frames. */
    public static final double OUTER_DOOR = 1.25;
    /** How long the boarding stair takes to come down, in seconds. */
    public static final double STAIR = 2.5;
    /** How long its handrails take to rise at the end, in seconds. */
    public static final double RAILS_RISE = 0.25;
    /* The storyboard's frames, a quarter of STAIR apart: the bay's doors open, in degrees; how far the folded stair
     * has slid out, from nought to one; the first flight's turn, in degrees; how folded the other two still are, from
     * one to nought; and how far the leaf is open, from nought to one. */
    private static final double[] BAY = {0, 70, 90, 90, 90};
    private static final double[] SLID = {0, 0, 1, 1, 1};
    private static final double[] FIRST = {0, 0, 0, -90, -125};
    private static final double[] FOLD = {1, 1, 1, 0.5, 0};
    private static final double[] LEAF = {0, 0.5, 1, 1, 1};

    private DoorMotion(final double leaf, final double stair) {
        this.leaf = leaf;
        this.stair = stair;
    }

    /** A door standing as it is: open or shut, and its stair, if it has one, down or not. */
    public static DoorMotion standing(final boolean open, final boolean letDown) {
        return new DoorMotion(open ? 1 : 0, open && letDown ? STAIR : 0);
    }

    /**
     * Moves it on {@code seconds} toward how it is to stand: {@code open} or shut, its stair {@code letDown} or not,
     * its leaf sliding over {@code leafTime} seconds.
     */
    public void advance(final double seconds, final boolean open, final boolean letDown, final double leafTime) {
        if (open && letDown) {
            this.stair = Math.min(STAIR, this.stair + seconds);
            this.leaf = Math.max(Math.min(1, this.leaf + seconds / leafTime), frame(LEAF));
        } else if (open) {
            this.stair = Math.max(0, this.stair - seconds);
            this.leaf = Math.min(1, this.leaf + seconds / leafTime);
        } else if (this.stair > 0) {
            this.stair = Math.max(0, this.stair - seconds);
        } else {
            this.leaf = Math.max(0, this.leaf - seconds / leafTime);
        }
    }

    /** How far the leaf is open, from nought (shut) to one. */
    public double leaf() {
        return this.leaf;
    }

    /** How far the stair has come down, in seconds of its {@link #STAIR}: nought folded in its bay. */
    public double stair() {
        return this.stair;
    }

    /** Whether the stair is folded in its bay, its bay shut: nothing of it to draw. */
    public boolean stowed() {
        return this.stair <= 0;
    }

    /** How far the bay's doors hang open, in degrees. */
    public double bay() {
        return frame(BAY);
    }

    /** How far the folded stair has slid out under the sill, from nought to one. */
    public double slid() {
        return frame(SLID);
    }

    /** How far the first flight has turned down from folded in the bay, in degrees: less than nought, down. */
    public double firstFlight() {
        return frame(FIRST);
    }

    /** How folded the second and third flights still are on the first, from one (folded) to nought (in line). */
    public double fold() {
        return frame(FOLD);
    }

    /** How far the handrails have risen, from nought (not to be seen) to one (standing). */
    public double rails() {
        return Math.clamp((this.stair - (STAIR - RAILS_RISE)) / RAILS_RISE, 0, 1);
    }

    /* The storyboard's {@code values} where the stair is now, straight between its frames. */
    private double frame(final double[] values) {
        final double at = Math.clamp(this.stair / STAIR, 0, 1) * (values.length - 1);
        final int from = Math.min(values.length - 2, (int) Math.floor(at));
        return values[from] + (values[from + 1] - values[from]) * (at - from);
    }
}
