/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SystemLayout;
import org.junit.jupiter.api.Test;

class ShipHomeWayTest {

    private static final double EPSILON = 1.0e-6;
    /* The Overworld's width as a default world border makes it, and a place on its map well inside its edges. */
    private static final double WIDTH = 59_999_968;
    private static final double HOME_X = 812_400;
    private static final double HOME_Z = -1_032_000;

    @Test
    void bearing_readsAsTheHeadingTapeDoes() {
        assertEquals(0, ShipHomeWay.bearing(0, -10), EPSILON);
        assertEquals(90, ShipHomeWay.bearing(10, 0), EPSILON);
        assertEquals(180, ShipHomeWay.bearing(0, 10), EPSILON);
        assertEquals(270, ShipHomeWay.bearing(-10, 0), EPSILON);
        assertEquals(45, ShipHomeWay.bearing(10, -10), EPSILON);
    }

    @Test
    void below_ofTheSpotOverAPlaceIsThatPlace() {
        final double[] spot = ShipHomeWay.over(SolarBody.OVERWORLD, HOME_X, HOME_Z, WIDTH);
        final double[] under = ShipHomeWay.below(SolarBody.OVERWORLD, spot[0], spot[1], WIDTH);
        assertEquals(HOME_X, under[0], 1.0e-3);
        assertEquals(HOME_Z, under[1], 1.0e-3);
    }

    @Test
    void over_theWorldsSpawnIsTheMiddleOfItsOrbit() {
        final double[] spot = ShipHomeWay.over(SolarBody.MOON, 0, 0, WIDTH);
        final double[] middle = SystemLayout.centre(SolarBody.MOON);
        assertEquals(middle[0], spot[0], EPSILON);
        assertEquals(middle[2], spot[1], EPSILON);
    }

    @Test
    void over_aPlaceNorthAndEastIsNorthAndEastOfTheMiddle() {
        final double[] spot = ShipHomeWay.over(SolarBody.OVERWORLD, HOME_X, HOME_Z, WIDTH);
        final double[] middle = SystemLayout.centre(SolarBody.OVERWORLD);
        assertEquals(45, ShipHomeWay.bearing(Math.signum(spot[0] - middle[0]), Math.signum(spot[1] - middle[2])),
                EPSILON);
    }
}
