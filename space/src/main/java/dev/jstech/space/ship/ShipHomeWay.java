/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.universe.SystemLayout;

/**
 * The way back by hand to where a pilot last took off from in a world, as a ship's HUD reckons it: in the world's sky,
 * the place's bearing from the ship; over the world, in its system's space, the place of its map under the ship,
 * where coming down now comes down, and the spot of its low orbit over the place taken off from, where coming down
 * comes down onto it. Both ways down follow the one map the descents follow ({@link SystemLayout#placeUnder}).
 *
 * <p>Pure: no game types.
 */
public final class ShipHomeWay {

    private static final double ROUND = 360;

    private ShipHomeWay() {
    }

    /**
     * The bearing of a place {@code dx} blocks east and {@code dz} blocks south of the ship, in degrees east of north
     * from 0 to under 360, as the heading tape reads it.
     */
    public static double bearing(final double dx, final double dz) {
        final double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        return degrees < 0 ? degrees + ROUND : degrees;
    }

    /**
     * The place of {@code body}'s world, {@code width} blocks round, under the spot ({@code x}, {@code z}) of its low
     * orbit: where a ship coming down there arrives.
     *
     * @return {x, z} on the world's map
     */
    public static double[] below(final SolarBody body, final double x, final double z, final double width) {
        final double[] place = SystemLayout.placeUnder(body, x, z);
        return new double[] {SphereMap.x(place[0], width), SphereMap.z(place[1], width)};
    }

    /**
     * The spot of {@code body}'s low orbit over the place ({@code x}, {@code z}) of its world, {@code width} blocks
     * round: coming down there comes down onto the place.
     *
     * @return {x, z} in the space
     */
    public static double[] over(final SolarBody body, final double x, final double z, final double width) {
        return SystemLayout.orbitSpot(body, SphereMap.longitude(x, width), SphereMap.latitude(z, width));
    }
}
