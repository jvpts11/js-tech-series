/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SystemLayout;
import org.junit.jupiter.api.Test;

class PulseCourseTest {

    private static final double CLOSE = 1e-6;

    @Test
    void mayBegin_notOverAWorldsLowOrbitNorTheSunButBetweenThem() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double[] sun = SystemLayout.sun();
        assertFalse(PulseCourse.mayBegin(home[0], home[2]), "over the Overworld's low orbit");
        assertFalse(PulseCourse.mayBegin(moon[0], moon[2]), "over the Moon's");
        assertFalse(PulseCourse.mayBegin(sun[0], sun[2]), "over the Sun");
        assertTrue(PulseCourse.mayBegin(home[0], (home[2] + moon[2]) / 2), "half way to the Moon");
    }

    @Test
    void stopOn_dropsOutWhereTheWayFirstComesOverAWorldsLowOrbit() {
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double reach = SystemLayout.radius(SolarBody.MOON) * (1 + SystemLayout.LOW_ORBIT);
        // Coming south to the Moon along its meridian, from 2,000 blocks north of its middle to its middle.
        final PulseCourse.Stop stop = PulseCourse.stopOn(moon[0], moon[2] - 2_000, moon[0], moon[2]);
        assertNotNull(stop, "it drops out coming to the Moon");
        assertEquals(SolarBody.MOON, stop.body());
        assertEquals(moon[0], stop.x(), CLOSE);
        assertEquals(moon[2] - reach, stop.z(), CLOSE, "right where the way comes over its low orbit");
    }

    @Test
    void stopOn_goesOnWhereTheWayIsClearAndOutOfAnOrbitItStartedOver() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double half = (home[2] + moon[2]) / 2;
        assertNull(PulseCourse.stopOn(home[0], half, home[0], half + 150), "nothing on its way");
        assertNull(PulseCourse.stopOn(home[0], home[2], home[0], home[2] - 150), "going out of the orbit it is over");
    }

    @Test
    void stopOn_dropsOutAtTheEdgeOfTheSun() {
        final double[] sun = SystemLayout.sun();
        final PulseCourse.Stop stop = PulseCourse.stopOn(sun[0] - SystemLayout.SUN_RADIUS - 100, sun[2], sun[0],
                sun[2]);
        assertNotNull(stop, "it drops out coming to the Sun");
        assertNull(stop.body(), "the Sun, no world");
        assertEquals(sun[0] - SystemLayout.SUN_RADIUS, stop.x(), CLOSE);
    }

    @Test
    void nearest_isTheWorldWhoseLowOrbitIsNearestAndHowFarOffItIs() {
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double reach = SystemLayout.radius(SolarBody.MOON) * (1 + SystemLayout.LOW_ORBIT);
        final PulseCourse.Nearest nearest = PulseCourse.nearest(moon[0], moon[2] + reach + 500);
        assertEquals(SolarBody.MOON, nearest.body());
        assertEquals(500, nearest.range(), CLOSE);
    }
}
