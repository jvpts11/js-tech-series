/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

/**
 * Going from one world's path to another's round the same sun, the way a real transfer goes: the cheapest leaves when
 * the destination stands ahead of the start by just the angle it will move while the ship coasts half an ellipse to it,
 * so windows come round once every synodic period (Mars about every five real hours at the game's week-long year,
 * Venus about every 3.7, Jupiter about every 2.5). A player can always launch: out of the window it costs more
 * delta-v, up to twice the window's from the worst place. And the trip is as long as the delta-v spent on it allows:
 * spending more than the least shortens it.
 */
public final class Transfers {

    /** How far from the best angle a launch still counts as in the window, in radians: ten degrees. */
    public static final double WINDOW = Math.toRadians(10);
    /** The most a launch from the worst place costs, beside one in the window. */
    public static final double WORST = 2.0;
    /** The shortest a trip gets, however much delta-v is spent, as a share of the usual one. */
    public static final double FASTEST = 0.25;

    private Transfers() {
    }

    /** How long between two windows from {@code from} to {@code to}, in ticks: the synodic period. */
    public static double synodicTicks(final Orbit from, final Orbit to) {
        return 1.0 / Math.abs(1.0 / from.periodTicks() - 1.0 / to.periodTicks());
    }

    /**
     * How far ahead of the start the destination stands when the cheapest transfer leaves, in radians from minus pi to
     * pi: half a turn, less the angle the destination moves while the ship coasts half the ellipse that touches both
     * paths.
     */
    public static double hohmannLead(final Orbit from, final Orbit to) {
        final double transferAxis = (from.semiMajorAxis() + to.semiMajorAxis()) / 2;
        final double coast = new Orbit(transferAxis, 0).periodTicks() / 2;
        return wrap(Math.PI - 2 * Math.PI * coast / to.periodTicks());
    }

    /** How far ahead of the start the destination stands at {@code tick}, in radians from minus pi to pi. */
    public static double phaseAt(final Orbit from, final Orbit to, final long tick) {
        return wrap(to.angleAt(tick) - from.angleAt(tick));
    }

    /** How far the angle at {@code tick} is from the cheapest one, in radians from 0 to pi. */
    public static double phaseError(final Orbit from, final Orbit to, final long tick) {
        return Math.abs(wrap(phaseAt(from, to, tick) - hohmannLead(from, to)));
    }

    /** Whether a launch at {@code tick} is in the window. */
    public static boolean inWindow(final Orbit from, final Orbit to, final long tick) {
        return phaseError(from, to, tick) <= WINDOW;
    }

    /** How many ticks from {@code tick} until the angle is the cheapest again; 0 while the window is open. */
    public static long ticksToWindow(final Orbit from, final Orbit to, final long tick) {
        if (inWindow(from, to, tick)) {
            return 0;
        }
        // The angle between them changes at the difference of their angular speeds.
        final double drift = 2 * Math.PI / to.periodTicks() - 2 * Math.PI / from.periodTicks();
        final double left = wrapPositive((hohmannLead(from, to) - phaseAt(from, to, tick)) * Math.signum(drift));
        return Math.round(left / Math.abs(drift));
    }

    /**
     * How much a transfer launched at {@code tick} costs beside one in the window: 1 at the best angle, growing with
     * the square of how far off it is, to {@link #WORST} from the opposite place.
     */
    public static double deltaVFactor(final Orbit from, final Orbit to, final long tick) {
        final double off = phaseError(from, to, tick) / Math.PI;
        return 1 + (WORST - 1) * off * off;
    }

    /**
     * How long a trip takes, in ticks: the usual trip's {@code usualTicks} at the least delta-v it needs, shorter in
     * proportion as more is spent, never shorter than {@link #FASTEST} of it.
     */
    public static double travelTicks(final double usualTicks, final double leastDeltaV, final double spentDeltaV) {
        if (spentDeltaV < leastDeltaV) {
            throw new IllegalArgumentException("a trip needs at least " + leastDeltaV + " m/s; " + spentDeltaV
                    + " was spent");
        }
        return Math.max(usualTicks * FASTEST, usualTicks * leastDeltaV / spentDeltaV);
    }

    private static double wrap(final double angle) {
        final double positive = wrapPositive(angle);
        return positive > Math.PI ? positive - 2 * Math.PI : positive;
    }

    private static double wrapPositive(final double angle) {
        final double turn = 2 * Math.PI;
        return ((angle % turn) + turn) % turn;
    }
}
