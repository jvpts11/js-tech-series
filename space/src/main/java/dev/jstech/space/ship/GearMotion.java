/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

/**
 * How a small ship's gear stands as it goes up and comes down, and how far its wheels have rolled. Going up, the struts
 * fold into their bays first and the bays' doors close after them; coming down, the doors open first and the struts
 * unfold after, the same way back. A player's game keeps one for each ship it draws, moved on by the time between two
 * frames, so the gear moves smoothly whatever the frame rate; this is the arithmetic of it, nothing of the game's.
 */
public final class GearMotion {

    private double up;
    private double rolled;

    /** How long the gear takes to go all the way up, or all the way down, in seconds. */
    public static final double CYCLE = 2.0;
    /* The share of the way the struts take; the doors take the rest. */
    private static final double FOLDING = 0.8;
    private static final double TAU = 2 * Math.PI;

    private GearMotion(final double up) {
        this.up = up;
    }

    /** Gear standing as it is when first seen: all the way down, or all the way up. */
    public static GearMotion standing(final boolean down) {
        return new GearMotion(down ? 0 : 1);
    }

    /** Moves it on {@code seconds}: toward all the way down while {@code down}, else toward all the way up. */
    public void advance(final double seconds, final boolean down) {
        final double step = Math.max(0, seconds) / CYCLE;
        this.up = down ? Math.max(0, this.up - step) : Math.min(1, this.up + step);
    }

    /** Rolls its wheels {@code metres} along the land, forward. */
    public void roll(final double metres) {
        this.rolled += metres;
    }

    /** How far up it is, from nought, all the way down, to one, its struts in and its doors shut. */
    public double up() {
        return this.up;
    }

    /** How far its struts have folded, from nought, standing, to one, in their bays: easing in and out. */
    public double folded() {
        final double t = Math.min(1, this.up / FOLDING);
        return t * t * (3 - 2 * t);
    }

    /** How far its bays' doors have shut, from nought, hanging open, to one, flush with the hull. */
    public double shut() {
        return Math.max(0, (this.up - FOLDING) / (1 - FOLDING));
    }

    /** Whether it is all the way up, nothing of it out of its bays. */
    public boolean stowed() {
        return this.up >= 1;
    }

    /** How far a wheel of {@code radius} blocks has turned rolling, in radians, from nought up to a whole turn. */
    public double turned(final double radius) {
        if (radius <= 0) {
            return 0;
        }
        final double angle = this.rolled / radius;
        return angle - Math.floor(angle / TAU) * TAU;
    }
}
