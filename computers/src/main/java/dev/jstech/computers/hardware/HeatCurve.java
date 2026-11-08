/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * A machine's temperature on its way to where its load leaves it: it closes in on that temperature as a warm body
 * does, quickly at first and slower as it nears it, a little more than half the way in each {@link #TAU_TICKS} and
 * nearly all of it in five times as long. It warms so after the machine starts or works harder, and cools so after it
 * stops or trips. The curve is kept as where it set out from, where it heads and when it set out, so reading it at any
 * moment is one sum and nothing is looked at between.
 *
 * <p>Plain arithmetic, tested without the game; the time is game ticks.
 */
public final class HeatCurve {

    private double from;
    private double to;
    private long since;

    /** The time over which it closes in on its target by about 63%, in ticks: half a minute. */
    public static final long TAU_TICKS = 600L;

    /* Targets closer than this to the one it heads for leave it heading there, so a steady load sets out only once. */
    private static final double STEADY = 1e-6;

    /** A curve at rest at {@code celsius}. */
    public HeatCurve(final double celsius) {
        this(celsius, celsius, 0L);
    }

    /** A curve that set out from {@code from} at {@code since} and heads for {@code to}, as one was saved. */
    public HeatCurve(final double from, final double to, final long since) {
        this.from = from;
        this.to = to;
        this.since = since;
    }

    /** Its temperature at tick {@code now}. */
    public double at(final long now) {
        final long elapsed = Math.max(0L, now - since);
        if (Double.isInfinite(to)) {
            // Heat with nothing to take it away climbs without end: as fast as the curve's first moments, forever.
            return from + (double) elapsed / TAU_TICKS * ThermalModel.RISE_AT_BUDGET;
        }
        return to + (from - to) * Math.exp(-(double) elapsed / TAU_TICKS);
    }

    /** Heads for {@code target} from where it stands at {@code now}; a target it already heads for changes nothing. */
    public void aim(final long now, final double target) {
        if (target == to || Math.abs(target - to) < STEADY) {
            return;
        }
        this.from = at(now);
        this.to = target;
        this.since = now;
    }

    /** Sets out afresh at {@code now} from {@code celsius} for {@code target}, wherever it stood before. */
    public void restart(final long now, final double celsius, final double target) {
        this.from = celsius;
        this.to = target;
        this.since = now;
    }

    /** Stands at its target at {@code now}: as a machine left long enough would. */
    public void settle(final long now) {
        restart(now, to, to);
    }

    /** Where it heads. */
    public double target() {
        return to;
    }

    /** Where it set out from, and when: what is saved to read it again later. */
    public double from() {
        return from;
    }

    public long since() {
        return since;
    }
}
