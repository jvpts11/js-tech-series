/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

/**
 * The shapes of landforms, as heights over or under the ground at a distance from their middle: a crater's bowl, raised
 * rim and central peak; a volcano's cone, broad and gentle for a shield, steep for a stratovolcano, with its summit
 * crater; a mesa's flat top and cliffs; the ridges of dunes; the cross-section of a canyon. In blocks; pure.
 */
public final class LandformShapes {

    /* A crater's rim is as wide as half its radius; its central peak stands within a fifth of it. */
    private static final double RIM_WIDTH = 0.5;
    private static final double PEAK_REACH = 0.2;
    /* A volcano's summit crater is this share of its radius across, and this share of its height deep. */
    private static final double SUMMIT = 0.12;
    private static final double SUMMIT_DEPTH = 0.15;
    private static final double SHIELD_POWER = 2.0;
    private static final double STRATO_POWER = 1.15;
    /* How the lee side of a dune falls steeper than its windward side rises. */
    private static final double LEE = 0.7;

    private LandformShapes() {
    }

    /**
     * How far a crater of {@code radius}, {@code depth} and a rim {@code rim} high moves the ground at {@code at}
     * blocks from its middle: down in its bowl, up on its rim, nothing beyond; a {@code peak} high hill in its middle.
     */
    public static double crater(final double at, final double radius, final double depth, final double rim,
                                final double peak) {
        if (at < radius) {
            final double share = at / radius;
            double height = -depth * (1 - share * share) + rim * share * share * share * share;
            if (peak > 0 && at < radius * PEAK_REACH) {
                height += peak * (1 - at / (radius * PEAK_REACH));
            }
            return height;
        }
        final double out = (at - radius) / (radius * RIM_WIDTH);
        return out >= 1 ? 0 : rim * (1 - out) * (1 - out);
    }

    /**
     * How high a volcano of {@code radius} and {@code height} stands at {@code at} blocks from its middle: a shield's
     * slope gentle and wide, a stratovolcano's steep; down into its summit crater at the top.
     */
    public static double volcano(final double at, final double radius, final double height, final boolean shield) {
        if (at >= radius) {
            return 0;
        }
        final double share = 1 - at / radius;
        double rise = height * Math.pow(share, shield ? SHIELD_POWER : STRATO_POWER);
        final double summit = radius * SUMMIT;
        if (at < summit) {
            rise -= height * SUMMIT_DEPTH * (1 - at / summit);
        }
        return rise;
    }

    /** Whether {@code at} is within a volcano's summit crater, where its lava lies. */
    public static boolean inSummit(final double at, final double radius) {
        return at < radius * SUMMIT * 0.6;
    }

    /**
     * How high a mesa of {@code radius} and {@code height} stands at {@code at}: its whole height over its top, falling
     * by cliffs over the last {@code edge} blocks of it.
     */
    public static double mesa(final double at, final double radius, final double height, final double edge) {
        if (at >= radius) {
            return 0;
        }
        final double fromEdge = radius - at;
        return fromEdge >= edge ? height : height * fromEdge / edge;
    }

    /**
     * How high dunes {@code height} high and {@code wavelength} apart stand at {@code along} blocks across their
     * ridges: up the windward side, down a steeper lee.
     */
    public static double dune(final double along, final double wavelength, final double height) {
        final double phase = ((along / wavelength) % 1 + 1) % 1;
        return phase < LEE ? height * Math.sin(phase / LEE * Math.PI / 2)
                : height * Math.cos((phase - LEE) / (1 - LEE) * Math.PI / 2);
    }

    /**
     * How deep a canyon {@code width} wide and {@code depth} deep is at {@code across} blocks from its middle line:
     * its whole depth on its floor, its walls steep, nothing past its rim.
     */
    public static double canyon(final double across, final double width, final double depth) {
        final double half = width / 2;
        final double out = Math.abs(across);
        if (out >= half) {
            return 0;
        }
        final double floor = half * 0.4;
        return out <= floor ? depth : depth * Math.sqrt(1 - (out - floor) / (half - floor));
    }

    /** How much of a landform's change holds at {@code at} of {@code radius}: all of it inside, fading to none. */
    public static double fade(final double at, final double radius, final double edge) {
        if (at <= radius - edge) {
            return 1;
        }
        return at >= radius ? 0 : (radius - at) / edge;
    }
}
