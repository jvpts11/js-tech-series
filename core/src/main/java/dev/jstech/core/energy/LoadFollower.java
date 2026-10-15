/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

/**
 * A generator's output following what its grid draws, as a power station with a governor does: it makes only what is
 * drawn, so it burns only that much. A generator may declare a minimum it always makes (a reactor that cannot idle) and
 * a slow change: its output moves at most so much a tick from the last, up or down, and what it must make above what is
 * drawn while it comes down is made and lost.
 *
 * <p>Each tick the grid draws up to {@link #offer()}; then the generator's block settles the tick ({@link #settle()})
 * and burns fuel for what it made. Pure: no game types.
 */
public final class LoadFollower {

    private final long maximum;
    private final long minimum;
    private final long ramp;
    private long output;
    private long drawn;

    /**
     * A generator that makes at most {@code maximum} a tick and never less than {@code minimum} while it runs, whose
     * output changes by at most {@code ramp} a tick; a ramp of 0 changes at once.
     */
    public LoadFollower(final long maximum, final long minimum, final long ramp) {
        if (maximum < 0 || minimum < 0 || minimum > maximum || ramp < 0) {
            throw new IllegalArgumentException("a generator makes from its minimum to its maximum, changing by no "
                    + "less than nothing: " + minimum + " to " + maximum + ", " + ramp + " a tick");
        }
        this.maximum = maximum;
        this.minimum = minimum;
        this.ramp = ramp;
    }

    /** A generator that makes at once whatever is drawn, up to {@code maximum}. */
    public static LoadFollower instant(final long maximum) {
        return new LoadFollower(maximum, 0L, 0L);
    }

    public long maximum() {
        return this.maximum;
    }

    public long minimum() {
        return this.minimum;
    }

    /** How much its output changes at most in a tick; 0 when it changes at once. */
    public long ramp() {
        return this.ramp;
    }

    /** What it made last tick. */
    public long output() {
        return this.output;
    }

    /** The most it can give this tick: no more than its maximum, nor more than its ramp above last tick's output. */
    public long offer() {
        return this.ramp == 0 ? this.maximum : Math.min(this.maximum, saturatingAdd(this.output, this.ramp));
    }

    /** The least it makes this tick, whatever is drawn: its minimum, and no more than its ramp below last tick. */
    public long floor() {
        return this.ramp == 0 ? this.minimum : Math.min(this.maximum, Math.max(this.minimum, this.output - this.ramp));
    }

    /** What is still there to draw this tick. */
    public long available() {
        return Math.max(0L, offer() - this.drawn);
    }

    /** Draws up to {@code amount}; how much was drawn, or would be when simulated. */
    public long draw(final long amount, final boolean simulate) {
        final long taken = Math.min(Math.max(0L, amount), available());
        if (!simulate) {
            this.drawn += taken;
        }
        return taken;
    }

    /** What was drawn this tick so far. */
    public long drawn() {
        return this.drawn;
    }

    /**
     * Ends the tick: what it made, which its block burns fuel for, being what was drawn or its floor, whichever is more.
     * What it made above what was drawn is lost.
     */
    public long settle() {
        final long made = Math.max(this.drawn, floor());
        this.output = made;
        this.drawn = 0L;
        return made;
    }

    /** Stops it, as when it runs out of fuel: it starts again from nothing. */
    public void stop() {
        this.output = 0L;
        this.drawn = 0L;
    }

    /** Puts back the output it had when its block was saved. */
    public void restore(final long saved) {
        this.output = Math.max(0L, Math.min(this.maximum, saved));
    }

    private static long saturatingAdd(final long a, final long b) {
        final long sum = a + b;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }
}
