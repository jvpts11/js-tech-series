/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import java.util.ArrayList;
import java.util.List;

/**
 * The arithmetic of coloured light. A source of a level spreads as the game's own light does, a level less each block
 * it goes; where it arrives is lit by its colour as much as by its share of the light there, and light of several
 * colours mixes. A source the game's light shows did not reach a place (a wall between them) gives it no colour, so
 * colour never shows where its light does not. Pure; colours are {@code 0xRRGGBB}.
 */
public final class LightMath {

    /** The brightest a level of the game's light is. */
    public static final int BRIGHTEST = 15;
    /** White: light that tints nothing. */
    public static final int WHITE = 0xFFFFFF;
    /** How much of a place's colour comes from the coloured light at its brightest. */
    public static final double MOST_TINT = 0.85;
    /** How much open sky at its brightest takes from a place's tint. */
    public static final double SKY_FADE = 0.6;
    /** How much of a place's colour comes from daylight through blocks that tint it, at its brightest. */
    public static final double SKY_TINT = 0.55;

    private static final int CHANNEL = 0xFF;
    private static final double FULL = 255.0;
    private static final double DEGREES = 360.0;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;

    private LightMath() {
    }

    /** The level a source of {@code level} gives a place {@code steps} blocks from it along the way light goes. */
    public static int arriving(final int level, final int steps) {
        return Math.max(0, level - steps);
    }

    /**
     * Whether a source's light of {@code expected} at a place is what lights it, against the game's light there,
     * {@code actual}: the game's light, which goes round walls, is at least as bright as the source's would be.
     */
    public static boolean reaches(final int expected, final int actual) {
        return expected > 0 && actual >= expected - 1;
    }

    /**
     * The colour the light of {@code sources} gives a place where the game's light is {@code blockLight} and the sky's
     * {@code skyLight}: their colours mixed by the light each brings, laid over white by the share the coloured light
     * has. White where none arrives.
     */
    public static int tint(final List<Arrival> sources, final int blockLight, final int skyLight) {
        double red = 0;
        double green = 0;
        double blue = 0;
        double weight = 0;
        int strongest = 0;
        for (final Arrival source : sources) {
            if (!reaches(source.level(), blockLight) || source.colour() == WHITE) {
                continue;
            }
            final double w = source.level();
            red += w * (source.colour() >> RED_SHIFT & CHANNEL);
            green += w * (source.colour() >> GREEN_SHIFT & CHANNEL);
            blue += w * (source.colour() & CHANNEL);
            weight += w;
            strongest = Math.max(strongest, source.level());
        }
        if (weight <= 0) {
            return WHITE;
        }
        final int mixed = normalised(red / weight, green / weight, blue / weight);
        final double share = (double) strongest / BRIGHTEST * MOST_TINT
                * (1.0 - SKY_FADE * Math.clamp(skyLight, 0, BRIGHTEST) / BRIGHTEST);
        return blend(WHITE, mixed, share);
    }

    /**
     * The colour of a place lit by {@code tint} and by daylight of {@code skyLight} come down through blocks that gave
     * it {@code filter}: the daylight's hue laid over the place by the share of the light it brings, as a glass roof of
     * a colour tints the floor beneath it by day.
     */
    public static int skyThrough(final int tint, final int filter, final int skyLight) {
        if (filter == WHITE || skyLight <= 0) {
            return tint;
        }
        final int hue = normalised(filter >> RED_SHIFT & CHANNEL, filter >> GREEN_SHIFT & CHANNEL, filter & CHANNEL);
        final double share = (double) Math.clamp(skyLight, 0, BRIGHTEST) / BRIGHTEST * SKY_TINT;
        return blend(tint, multiply(tint, hue), share);
    }

    /** Between two colours, {@code share} of the way from the first. */
    public static int blend(final int from, final int to, final double share) {
        final double s = Math.clamp(share, 0.0, 1.0);
        int out = 0;
        for (int shift = 0; shift <= RED_SHIFT; shift += GREEN_SHIFT) {
            final int a = from >> shift & CHANNEL;
            final int b = to >> shift & CHANNEL;
            out |= (int) Math.round(a + (b - a) * s) << shift;
        }
        return out;
    }

    /** {@code colour} multiplied by {@code tint}, channel by channel, as light colours a surface. */
    public static int multiply(final int colour, final int tint) {
        int out = colour & ~WHITE;
        for (int shift = 0; shift <= RED_SHIFT; shift += GREEN_SHIFT) {
            final int a = colour >> shift & CHANNEL;
            final int b = tint >> shift & CHANNEL;
            out |= (int) Math.round(a * b / FULL) << shift;
        }
        return out;
    }

    /** The hue of {@code colour} in degrees, from 0 (red) round to 360; none (-1) for a grey. */
    public static double hue(final int colour) {
        final double r = (colour >> RED_SHIFT & CHANNEL) / FULL;
        final double g = (colour >> GREEN_SHIFT & CHANNEL) / FULL;
        final double b = (colour & CHANNEL) / FULL;
        final double max = Math.max(r, Math.max(g, b));
        final double min = Math.min(r, Math.min(g, b));
        final double span = max - min;
        if (span < 1.0 / FULL) {
            return -1;
        }
        final double sixth;
        if (max == r) {
            sixth = (g - b) / span;
        } else if (max == g) {
            sixth = 2.0 + (b - r) / span;
        } else {
            sixth = 4.0 + (r - g) / span;
        }
        final double degrees = sixth * DEGREES / 6.0;
        return degrees < 0 ? degrees + DEGREES : degrees;
    }

    /**
     * The ways a spotlight's light goes: rays through its cone of {@code angle} degrees about the way it points,
     * {@code rings} rings of them out from its middle, each a unit vector in the light's own frame, its axis along Z.
     */
    public static List<double[]> cone(final double angle, final int rings) {
        final List<double[]> out = new ArrayList<>();
        out.add(new double[] {0, 0, 1});
        final double half = Math.toRadians(Math.clamp(angle, 0.0, 179.0) / 2);
        for (int ring = 1; ring <= rings; ring++) {
            final double tilt = half * ring / rings;
            final int around = ring * 6;
            for (int i = 0; i < around; i++) {
                final double turn = 2 * Math.PI * i / around;
                out.add(new double[] {Math.sin(tilt) * Math.cos(turn), Math.sin(tilt) * Math.sin(turn),
                        Math.cos(tilt)});
            }
        }
        return out;
    }

    /**
     * The level a spotlight of {@code level} reaching {@code reach} blocks gives a place {@code distance} blocks
     * along its beam: its whole level near it, fading to half at its reach, as a lens keeps light together, and none
     * past it.
     */
    public static int along(final int level, final double distance, final double reach) {
        if (reach <= 0 || distance > reach) {
            return 0;
        }
        return (int) Math.round(level * (1.0 - 0.5 * distance / reach));
    }

    /** A colour with its brightest channel at full, the rest as they stand to it. */
    private static int normalised(final double red, final double green, final double blue) {
        final double max = Math.max(red, Math.max(green, blue));
        if (max <= 0) {
            return WHITE;
        }
        return (int) Math.round(red / max * FULL) << RED_SHIFT | (int) Math.round(green / max * FULL) << GREEN_SHIFT
                | (int) Math.round(blue / max * FULL);
    }

    /**
     * Light of a colour arriving at a place.
     *
     * @param level  the level it brings there, from 0 to 15
     * @param colour its colour, {@code 0xRRGGBB}
     */
    public record Arrival(int level, int colour) {
    }
}
