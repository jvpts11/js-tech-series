/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

/**
 * The system's space seen from the ground of one of its worlds: a direction of the space, turned into the directions of
 * a place on the world's surface (east, up and south, as the game's x, y and z), the place given by its longitude and
 * latitude on the world's map. So the Overworld, standing still over the Moon's near side, hangs high over the places
 * near the middle of the Moon's map, lower the farther one goes, on the horizon a quarter of the way round and hidden
 * on the far side; and its continents, its poles and its clouds turn as they do seen from the Moon's orbit.
 *
 * <p>The worlds are far apart beside their size, so the sky is the same from anywhere on the ground but for how it is
 * turned. Pure: no game types.
 */
public final class GroundSky {

    private GroundSky() {
    }

    /**
     * {@code direction}, a direction of the system's space, as seen from the place at {@code longitude} and
     * {@code latitude}, in degrees, on {@code world} at {@code tick}: east, up and south.
     */
    public static double[] seen(final SolarBody world, final double longitude, final double latitude,
                                final long tick, final double[] direction) {
        final double[] north = SystemLayout.north(world);
        final double[] middle = SystemLayout.middle(world, tick);
        final double[] east = SystemLayout.cross(north, middle);
        final double lon = Math.toRadians(longitude);
        final double lat = Math.toRadians(latitude);
        // The place's own directions in the space: up through it, east along its parallel, north along its meridian.
        final double[] round = new double[3];
        final double[] up = new double[3];
        final double[] eastward = new double[3];
        final double[] northward = new double[3];
        for (int i = 0; i < 3; i++) {
            round[i] = Math.cos(lon) * middle[i] + Math.sin(lon) * east[i];
            up[i] = Math.cos(lat) * round[i] + Math.sin(lat) * north[i];
            eastward[i] = -Math.sin(lon) * middle[i] + Math.cos(lon) * east[i];
            northward[i] = -Math.sin(lat) * round[i] + Math.cos(lat) * north[i];
        }
        return new double[] {dot(direction, eastward), dot(direction, up), -dot(direction, northward)};
    }

    /** Where {@code body} lies from {@code world}, seen from the place at {@code longitude} and {@code latitude}. */
    public static double[] towards(final SolarBody world, final SolarBody body, final double longitude,
                                   final double latitude, final long tick) {
        final double[] from = SystemLayout.centre(world);
        final double[] to = SystemLayout.centre(body);
        final double apart = distance(from, to);
        return seen(world, longitude, latitude, tick,
                new double[] {(to[0] - from[0]) / apart, (to[1] - from[1]) / apart, (to[2] - from[2]) / apart});
    }

    /** How wide {@code body} looks from {@code world}, from its middle to its edge, in radians. */
    public static double radius(final SolarBody world, final SolarBody body) {
        return Math.asin(SystemLayout.radius(body) / distance(SystemLayout.centre(world), SystemLayout.centre(body)));
    }

    private static double dot(final double[] a, final double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double distance(final double[] a, final double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
    }
}
