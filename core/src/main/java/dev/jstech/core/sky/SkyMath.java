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

    /** The same at a time of the clock with a part of a tick, as a place's own hour is. */
    public static double celestial(final double dayTime) {
        final double d = frac(dayTime / DAY - 0.25);
        final double e = 0.5 - Math.cos(d * Math.PI) / 2.0;
        return (d * 2.0 + e) / 3.0;
    }

    /**
     * How far round a round world a place at {@code x} stands from the middle of its border, in turns east: the border
     * from west to east goes once round it.
     */
    public static double longitude(final double x, final double centreX, final double borderSize) {
        return (x - centreX) / borderSize;
    }

    /**
     * How far north a place at {@code z} stands on a round world, in degrees from -90 to 90: its border from north to
     * south, half as long as from west to east, goes from pole to pole.
     */
    public static double latitude(final double z, final double centreZ, final double borderSize) {
        return Math.max(-90, Math.min(90, -(z - centreZ) / borderSize * 360));
    }

    /** The clock's time where a place {@code longitude} turns east of the middle stands: later the farther east. */
    public static double localTime(final long dayTime, final double longitude) {
        return dayTime + longitude * DAY;
    }

    /**
     * The direction a sun {@code turns} round the sky lies in, as the game turns its sky, seen from {@code latitude}
     * degrees north, where the sky's pole stands that high over the north and the sun at noon that far from overhead,
     * towards the south.
     *
     * @return {x, y, z} in the world's directions, a unit vector
     */
    public static double[] sky(final double turns, final double latitude) {
        final double angle = turns * TURN;
        final double lean = Math.toRadians(latitude);
        return new double[] {-Math.sin(angle), Math.cos(angle) * Math.cos(lean), Math.cos(angle) * Math.sin(lean)};
    }

    /** How high a sun {@code turns} round the sky stands from {@code latitude}, as the sine of its height. */
    public static double sunHeight(final double turns, final double latitude) {
        return Math.cos(turns * TURN) * Math.cos(Math.toRadians(latitude));
    }

    /**
     * How light the day is with the sun {@code height} high (the sine of it), in rain and thunder of their levels, as
     * the game works out its darkening of the day: from 0.2 at night to 1.
     */
    public static double dayLight(final double height, final double rain, final double thunder) {
        double light = 1 - Math.max(0, Math.min(1, 1 - (height * 2 + 0.2)));
        light *= 1 - rain * 5 / 16;
        light *= 1 - thunder * 5 / 16;
        return light * 0.8 + 0.2;
    }

    /** How bright the stars shine with the sun {@code height} high, as the game works it out: 0.5 at midnight. */
    public static double starlight(final double height) {
        final double dark = Math.max(0, Math.min(1, 1 - (height * 2 + 0.25)));
        return dark * dark * 0.5;
    }

    /**
     * Where a body stands from the first sun after {@code days} of the clock, in turns: where it stood as the world
     * began, moving round once in {@code orbitDays}, the other way when below 0; still for 0.
     */
    public static double drift(final double offset, final double orbitDays, final double days) {
        return orbitDays == 0 ? offset : offset + days / orbitDays;
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
