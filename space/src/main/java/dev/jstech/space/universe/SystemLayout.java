/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.space.orbit.Orbit;
import org.jetbrains.annotations.Nullable;

/**
 * Where the bodies of a system stand in its space, how big they are drawn and how they are turned. Each world keeps its
 * place there, so the stations, satellites and debris in its orbit stay over it: a planet stands where its orbit starts
 * as the world begins, and the Moon a quarter of the way round the Overworld from the Sun, so each is seen half lit
 * from the other. How the worlds go round their sun lives in their orbits, which give the launch windows and the
 * trips; the space only gives each its place.
 *
 * <p>Distances are squeezed far more than sizes, so worlds look grand and journeys stay short: an astronomical unit is
 * 10,000 blocks, while the Overworld is a sphere 1,000 blocks in radius and every other world in proportion to its
 * width. Each world hangs below the space's floor of stations, its low orbit, at a third of its radius above its
 * ground, at the height a player arrives at, so the world below fills the view. The Moon stands 3,000 blocks from the
 * Overworld's middle. The Sun stands in the middle, level with the Overworld's middle, a sphere of 1,500 blocks: at the
 * Overworld's distance it looks as big as the game's own sun.
 *
 * <p>A planet's poles lie level and square to its sun, so its equator runs under the floor of stations with the line of
 * dawn across it: day on the side towards the Sun, night on the other. It turns east once a day about them, its map's
 * middle facing the Sun at its noon, the Overworld's by the Overworld's clock. The Moon keeps its near side to the
 * Overworld, so its poles stand up and down: over its north pole the Sun stays low and the Overworld rises over its
 * limb.
 *
 * <p>Directions are unit vectors of x, y and z. Every size and distance here is an estimate, set by playing. Pure: no
 * game types.
 */
public final class SystemLayout {

    /** The Overworld's radius in its system's space, in blocks: every world's in proportion to its width. */
    public static final double OVERWORLD_RADIUS = 1_000;
    /** How high above a world's ground its low orbit lies, as a share of its radius. */
    public static final double LOW_ORBIT = 0.3;
    /** The height in a system's space where every world's low orbit lies, where stations stand and players arrive. */
    public static final double ORBIT_Y = 64;
    /** The Sun's radius, in blocks. */
    public static final double SUN_RADIUS = 1_500;
    /** How far the Moon stands from the Overworld's middle, in blocks. */
    public static final double MOON_DISTANCE = 3_000;
    /* The tick of a world's noon, in its day. */
    private static final double NOON = 6_000;
    private static final double[] UP = {0, 1, 0};

    private SystemLayout() {
    }

    /** The radius of {@code body}'s sphere, in blocks. */
    public static double radius(final SolarBody body) {
        return OVERWORLD_RADIUS * body.diameter() / SphereMap.EARTH_DIAMETER;
    }

    /** Where {@code body}'s middle stands, in blocks: x, y and z. */
    public static double[] centre(final SolarBody body) {
        final double y = ORBIT_Y - radius(body) * (1 + LOW_ORBIT);
        final Orbit orbit = body.orbit();
        if (orbit != null) {
            return new double[] {orbit.xAt(0), y, orbit.zAt(0)};
        }
        // A quarter of the way round from the Sun, so the Moon and the Overworld each see the other half lit.
        final double[] home = centre(SolarBody.OVERWORLD);
        return new double[] {home[0], y, home[2] + MOON_DISTANCE};
    }

    /** Where the Sun's middle stands, in blocks: in the middle, level with the Overworld's. */
    public static double[] sun() {
        return new double[] {0, centre(SolarBody.OVERWORLD)[1], 0};
    }

    /**
     * Where {@code body}'s north pole points: for a planet level, square to the Sun and on the side of the space's
     * north; for the Moon, up.
     */
    public static double[] north(final SolarBody body) {
        if (body.dayTicks() <= 0) {
            return UP.clone();
        }
        final double[] sunward = levelSunward(body);
        // Of the two level directions square to the Sun, the one towards the space's north, which is -z.
        final double[] north = {-sunward[2], 0, sunward[0]};
        return north[2] <= 0 ? north : new double[] {-north[0], 0, -north[2]};
    }

    /**
     * Where the middle of {@code body}'s map (its longitude and latitude 0) faces at {@code tick}: a planet's the Sun
     * at its noon, turning east once a day; the Moon's the Overworld, always.
     */
    public static double[] middle(final SolarBody body, final long tick) {
        if (body.dayTicks() <= 0) {
            final double[] home = centre(SolarBody.OVERWORLD);
            final double[] moon = centre(body);
            return unit(new double[] {home[0] - moon[0], 0, home[2] - moon[2]});
        }
        final double[] sunward = levelSunward(body);
        final double[] east = cross(north(body), sunward);
        final double turns = (tick - NOON) / body.dayTicks();
        final double angle = (turns - Math.floor(turns)) * 2 * Math.PI;
        final double c = Math.cos(angle);
        final double s = Math.sin(angle);
        return new double[] {c * sunward[0] + s * east[0], c * sunward[1] + s * east[1], c * sunward[2] + s * east[2]};
    }

    /** The direction from {@code body}'s middle to the Sun's. */
    public static double[] sunward(final SolarBody body) {
        final double[] from = centre(body);
        final double[] sun = sun();
        return unit(new double[] {sun[0] - from[0], sun[1] - from[1], sun[2] - from[2]});
    }

    /** Which body's low orbit a point of the space at ({@code x}, {@code z}) lies over, if any. */
    public static @Nullable SolarBody over(final double x, final double z) {
        for (final SolarBody body : SolarBody.values()) {
            final double[] c = centre(body);
            final double reach = radius(body) * (1 + LOW_ORBIT);
            if ((x - c[0]) * (x - c[0]) + (z - c[2]) * (z - c[2]) < reach * reach) {
                return body;
            }
        }
        return null;
    }

    /** {@code a} across {@code b}: square to both, by the right hand. */
    public static double[] cross(final double[] a, final double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    /* The level part of the direction to the Sun. */
    private static double[] levelSunward(final SolarBody body) {
        final double[] towards = sunward(body);
        return unit(new double[] {towards[0], 0, towards[2]});
    }

    private static double[] unit(final double[] v) {
        final double length = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new double[] {v[0] / length, v[1] / length, v[2] / length};
    }
}
