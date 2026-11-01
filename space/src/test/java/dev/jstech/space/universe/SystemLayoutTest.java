/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SystemLayoutTest {

    private static final double CLOSE = 1e-6;
    private static final long NOON = 6_000;
    private static final long DAY = 24_000;

    @Test
    void radius_isTheOverworldsTimesTheWidthOverTheEarths() {
        assertEquals(3_000, SystemLayout.radius(SolarBody.OVERWORLD), CLOSE);
        assertEquals(818.1, SystemLayout.radius(SolarBody.MOON), 0.1);
        assertEquals(1_596.1, SystemLayout.radius(SolarBody.MARS), 0.1);
    }

    @Test
    void centre_putsEveryLowOrbitAtTheHeightPlayersArriveAt() {
        for (final SolarBody body : SolarBody.values()) {
            final double ground = SystemLayout.centre(body)[1] + SystemLayout.radius(body);
            assertEquals(SystemLayout.ORBIT_Y, ground + SystemLayout.radius(body) * SystemLayout.LOW_ORBIT, CLOSE,
                    body.name());
        }
    }

    @Test
    void centre_keepsEachWorldInItsPlaceWhateverTheTime() {
        final double[] overworld = SystemLayout.centre(SolarBody.OVERWORLD);
        assertEquals(30_000, Math.hypot(overworld[0], overworld[2]), CLOSE);
        final double[] mars = SystemLayout.centre(SolarBody.MARS);
        assertEquals(45_720, Math.hypot(mars[0], mars[2]), 1);
        assertArrayEquals(overworld, SystemLayout.centre(SolarBody.OVERWORLD), CLOSE);
    }

    @Test
    void centre_putsTheMoonAQuarterRoundTheOverworldFromTheSun() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double[] sun = SystemLayout.sun();
        final double toMoonX = moon[0] - home[0];
        final double toMoonZ = moon[2] - home[2];
        assertEquals(SystemLayout.MOON_DISTANCE, Math.hypot(toMoonX, toMoonZ), CLOSE);
        assertEquals(0, toMoonX * (sun[0] - home[0]) + toMoonZ * (sun[2] - home[2]), CLOSE);
    }

    @Test
    void north_leansAPlanetsPoleHalfWayUpFromTheSpacesNorthSquareToTheSun() {
        final double half = Math.sqrt(0.5);
        for (final SolarBody planet : new SolarBody[] {SolarBody.OVERWORLD, SolarBody.MARS}) {
            final double[] north = SystemLayout.north(planet);
            final double[] sunward = SystemLayout.sunward(planet);
            assertEquals(half, north[1], CLOSE, planet.name());
            assertEquals(0, north[0] * sunward[0] + north[2] * sunward[2], CLOSE, planet.name());
            assertTrue(north[2] < 0, planet.name());
        }
        assertArrayEquals(new double[] {0, half, -half}, SystemLayout.north(SolarBody.OVERWORLD), CLOSE);
    }

    @Test
    void middle_facesTheSunAtNoonTheFloorAtSunsetAndAwayAtMidnight() {
        final double[] sunward = SystemLayout.sunward(SolarBody.OVERWORLD);
        assertArrayEquals(sunward, SystemLayout.middle(SolarBody.OVERWORLD, NOON), CLOSE);
        // Turning east, the map's middle goes from the Sun up towards the floor of stations, where its dusk falls.
        final double[] dusk = SystemLayout.middle(SolarBody.OVERWORLD, NOON + DAY / 4);
        assertArrayEquals(SystemLayout.cross(SystemLayout.north(SolarBody.OVERWORLD), sunward), dusk, CLOSE);
        assertTrue(dusk[1] > 0, "up, towards the stations");
        assertArrayEquals(new double[] {-sunward[0], -sunward[1], -sunward[2]},
                SystemLayout.middle(SolarBody.OVERWORLD, NOON + DAY / 2), CLOSE);
        assertArrayEquals(sunward, SystemLayout.middle(SolarBody.OVERWORLD, NOON + DAY * 5), 1e-9);
    }

    @Test
    void middle_staysSquareToThePoles() {
        for (final SolarBody body : SolarBody.values()) {
            final double[] north = SystemLayout.north(body);
            for (long tick = 0; tick < DAY * 2; tick += 1_777) {
                final double[] middle = SystemLayout.middle(body, tick);
                assertEquals(0, north[0] * middle[0] + north[1] * middle[1] + north[2] * middle[2], CLOSE);
                assertEquals(1, Math.sqrt(middle[0] * middle[0] + middle[1] * middle[1] + middle[2] * middle[2]),
                        CLOSE);
            }
        }
    }

    @Test
    void middle_keepsTheOverworldStillJustNorthOfTheMoonsNearSide() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double apart = Math.sqrt((home[0] - moon[0]) * (home[0] - moon[0])
                + (home[1] - moon[1]) * (home[1] - moon[1]) + (home[2] - moon[2]) * (home[2] - moon[2]));
        final double[] homeward = {(home[0] - moon[0]) / apart, (home[1] - moon[1]) / apart,
            (home[2] - moon[2]) / apart};
        final double[] north = SystemLayout.north(SolarBody.MOON);
        final double[] near = SystemLayout.middle(SolarBody.MOON, 0);
        final double lean = Math.toRadians(SystemLayout.OVERWORLD_OVER_THE_MOON);
        assertEquals(Math.sin(lean), dot(homeward, north), CLOSE, "the Overworld stands that far north");
        assertEquals(Math.cos(lean), dot(homeward, near), CLOSE, "over the meridian of the map's middle");
        assertTrue(north[1] > 0.8, "the poles stand nearly upright");
        assertArrayEquals(near, SystemLayout.middle(SolarBody.MOON, 123_456_789), CLOSE);
    }

    @Test
    void moonDistance_makesTheOverworldLookAboutEighteenDegreesWideFromTheMoon() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double apart = Math.sqrt((home[0] - moon[0]) * (home[0] - moon[0])
                + (home[1] - moon[1]) * (home[1] - moon[1]) + (home[2] - moon[2]) * (home[2] - moon[2]));
        assertEquals(18, 2 * Math.toDegrees(Math.asin(SystemLayout.radius(SolarBody.OVERWORLD) / apart)), 0.5);
    }

    private static double dot(final double[] a, final double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    @Test
    void over_findsTheWorldWhoseLowOrbitAPointLiesIn() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        assertEquals(SolarBody.OVERWORLD, SystemLayout.over(home[0] + 3_600, home[2]));
        assertEquals(SolarBody.MOON, SystemLayout.over(moon[0], moon[2] - 900));
        assertNull(SystemLayout.over(home[0] + 6_000, home[2]));
    }

    @Test
    void shown_isTheWholeSphereInItsPlanetsRegionAndAPointFarOutside() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] overHome = {home[0], SystemLayout.ORBIT_Y, home[2]};
        assertEquals(1, SystemLayout.shown(SolarBody.OVERWORLD, overHome), CLOSE);
        assertEquals(1, SystemLayout.shown(SolarBody.MOON, overHome), CLOSE, "the Moon is the Overworld's own");
        assertEquals(0, SystemLayout.shown(SolarBody.MARS, overHome), CLOSE, "Mars is a point from the Overworld");
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double[] overMoon = {moon[0], SystemLayout.ORBIT_Y, moon[2]};
        assertEquals(1, SystemLayout.shown(SolarBody.OVERWORLD, overMoon), CLOSE, "the Overworld from the Moon");
        assertEquals(0, SystemLayout.shown(SolarBody.MARS, overMoon), CLOSE, "Mars from the Moon");
        final double[] mars = SystemLayout.centre(SolarBody.MARS);
        final double[] overMars = {mars[0], SystemLayout.ORBIT_Y, mars[2]};
        assertEquals(1, SystemLayout.shown(SolarBody.MARS, overMars), CLOSE);
        assertEquals(0, SystemLayout.shown(SolarBody.OVERWORLD, overMars), CLOSE, "the Overworld from Mars");
        assertEquals(0, SystemLayout.shown(SolarBody.MOON, overMars), CLOSE, "the Moon from Mars");
    }

    @Test
    void shown_growsOutOfThePointWithoutAJumpComingTowardsAPlanet() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] mars = SystemLayout.centre(SolarBody.MARS);
        double last = SystemLayout.shown(SolarBody.MARS, home);
        for (int step = 1; step <= 1_000; step++) {
            final double t = step / 1_000.0;
            final double[] eye = {home[0] + (mars[0] - home[0]) * t, home[1] + (mars[1] - home[1]) * t,
                home[2] + (mars[2] - home[2]) * t};
            final double now = SystemLayout.shown(SolarBody.MARS, eye);
            assertTrue(now >= last - CLOSE && now - last < 0.05, "smoothly, never back, at step " + step);
            last = now;
        }
        assertEquals(1, last, CLOSE);
    }

    @Test
    void regions_holdTheirMoonsLowOrbits() {
        final double[] home = SystemLayout.centre(SolarBody.OVERWORLD);
        final double[] moon = SystemLayout.centre(SolarBody.MOON);
        final double reach = Math.hypot(moon[0] - home[0], moon[2] - home[2])
                + SystemLayout.radius(SolarBody.MOON) * (1 + SystemLayout.LOW_ORBIT);
        assertTrue(reach < SystemLayout.radius(SolarBody.OVERWORLD) * SystemLayout.REGION_RADII,
                "the Moon's low orbit lies in the Overworld's region");
    }

    @Test
    void orbitSpot_andPlaceUnder_goThereAndBackWithinTheLowOrbit() {
        for (final SolarBody body : SolarBody.values()) {
            final double[] spot = SystemLayout.orbitSpot(body, 123.4, -56.7);
            assertEquals(body, SystemLayout.over(spot[0], spot[1]), "the spot lies in the low orbit");
            assertArrayEquals(new double[] {123.4, -56.7}, SystemLayout.placeUnder(body, spot[0], spot[1]), 1e-9);
        }
        final double[] corner = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 180, 90);
        assertEquals(SolarBody.OVERWORLD, SystemLayout.over(corner[0], corner[1]), "even the world's far corner");
        assertTrue(corner[1] < SystemLayout.centre(SolarBody.OVERWORLD)[2], "the north lies to the space's north");
    }

    @Test
    void lowOrbits_neverMeet() {
        final SolarBody[] bodies = SolarBody.values();
        for (int a = 0; a < bodies.length; a++) {
            for (int b = a + 1; b < bodies.length; b++) {
                final double[] one = SystemLayout.centre(bodies[a]);
                final double[] other = SystemLayout.centre(bodies[b]);
                final double reach = (SystemLayout.radius(bodies[a]) + SystemLayout.radius(bodies[b]))
                        * (1 + SystemLayout.LOW_ORBIT);
                assertTrue(Math.hypot(one[0] - other[0], one[2] - other[2]) > reach,
                        bodies[a] + " and " + bodies[b]);
            }
        }
    }
}
