/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

/**
 * Where things stand in a sky and what follows from it, without the game: the sun's turn round the sky by the clock as
 * the game works it out, each body's path from the sun and the direction it lies in, how much of a sun a body hides
 * (an eclipse, from the real geometry of two discs), how much of a body its sun lights (its phase), and how strong
 * sunlight is at a sun's distance. Angles are in turns round the sky unless said; directions are unit vectors.
 */
public final class SkyMath {

    /** A whole turn, in radians. */
    public static final double TURN = Math.PI * 2;
    /** How long the game's clock counts a day, in ticks. */
    public static final double DAY = 24_000.0;
    /**
     * The sun's disc drawn by the game: a quad half 30 wide, a hundred away, whose bright middle is half of it; a
     * moon's: half 20 wide, a hundred away, its disc the whole of it.
     */
    public static final double SUN_HALF = 15.0;
    public static final double MOON_HALF = 20.0;
    public static final double SKY_DISTANCE = 100.0;
    /** The weakest and the strongest sunlight shown, beside the overworld's. */
    public static final double DIMMEST = 0.02;
    public static final double BRIGHTEST = 2.0;
    /** How much of the day's light a sun wholly hidden leaves: the glow of its corona and the sky round it. */
    public static final double TOTALITY = 0.1;

    private SkyMath() {
    }

    /**
     * How far round the sky the sun stands at {@code dayTime}, in turns, as the game works it out: 0 at noon, a half at
     * midnight, lingering a little near the horizon.
     */
    public static double celestial(final long dayTime) {
        final double d = frac(dayTime / DAY - 0.25);
        final double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
        return (d * 2.0 + e) / 3.0;
    }

    /**
     * Where a body stands from the first sun after {@code days} of the clock, in turns: where it stood as the world
     * began, moving round once in {@code orbitDays}; still for 0.
     */
    public static double drift(final double offset, final double orbitDays, final double days) {
        return orbitDays <= 0 ? offset : offset + days / orbitDays;
    }

    /** How far round a surface turning once in {@code turnDays} has turned after {@code days}, in turns. */
    public static double turned(final double turnDays, final double days) {
        return turnDays <= 0 ? 0 : frac(days / turnDays);
    }

    /**
     * The direction a thing {@code turns} round the sky lies in, on a path leaning {@code tiltDegrees} from the sun's,
     * as the game turns its sky: overhead at 0, below the ground at a half.
     *
     * @return {x, y, z}, a unit vector
     */
    public static double[] direction(final double turns, final double tiltDegrees) {
        final double angle = turns * TURN;
        final double tilt = Math.toRadians(tiltDegrees);
        final double up = Math.cos(angle);
        return new double[] {-up * Math.sin(tilt), up * Math.cos(tilt), Math.sin(angle)};
    }

    /** The angle between two directions, in radians. */
    public static double separation(final double[] a, final double[] b) {
        final double dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return Math.acos(Math.max(-1.0, Math.min(1.0, dot)));
    }

    /** How wide a sun of {@code apparentSize} (the overworld's is 1) looks, as an angle from its middle, in radians. */
    public static double sunRadius(final double apparentSize) {
        return Math.atan(SUN_HALF * apparentSize / SKY_DISTANCE);
    }

    /** How wide a body of {@code size} (the overworld's moon is 1) looks, as an angle from its middle, in radians. */
    public static double bodyRadius(final double size) {
        return Math.atan(MOON_HALF * size / SKY_DISTANCE);
    }

    /**
     * How much of a sun's disc a body's hides, 0 to 1: the share of the sun's disc the two discs overlap on, from
     * their radii and the angle between their middles, all in radians.
     */
    public static double coverage(final double sun, final double body, final double apart) {
        if (sun <= 0 || apart >= sun + body) {
            return 0;
        }
        if (apart <= Math.abs(body - sun)) {
            return body >= sun ? 1 : body * body / (sun * sun);
        }
        final double a = sun * sun * Math.acos((apart * apart + sun * sun - body * body) / (2 * apart * sun));
        final double b = body * body * Math.acos((apart * apart + body * body - sun * sun) / (2 * apart * body));
        final double c = 0.5 * Math.sqrt(Math.max(0, (-apart + sun + body) * (apart + sun - body)
                * (apart - sun + body) * (apart + sun + body)));
        return Math.max(0, Math.min(1, (a + b - c) / (Math.PI * sun * sun)));
    }

    /**
     * How much of a body its sun lights as seen from the ground, 0 to 1, from the angle between them: a new moon beside
     * the sun, a full one opposite it.
     */
    public static double litShare(final double apart) {
        return (1 - Math.cos(apart)) / 2;
    }

    /** How much of the day's light is left with {@code coverage} of the sun hidden. */
    public static double eclipseLight(final double coverage) {
        return 1 - (1 - TOTALITY) * Math.max(0, Math.min(1, coverage));
    }

    /**
     * How strong a sun's light is {@code distance} astronomical units away, beside the overworld's at one: falling
     * with the square of the distance, between the dimmest and the brightest shown.
     */
    public static double sunlight(final double distance) {
        if (distance <= 0) {
            return 1;
        }
        return Math.max(DIMMEST, Math.min(BRIGHTEST, 1 / (distance * distance)));
    }

    /** The part of {@code value} past its whole number: 0 to below 1, for negatives too. */
    public static double frac(final double value) {
        return value - Math.floor(value);
    }
}
