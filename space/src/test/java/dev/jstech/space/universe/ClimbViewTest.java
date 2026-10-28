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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClimbViewTest {

    private static final double CLOSE = 1e-9;
    private static final double LIMIT = 320;
    private static final double SEA = 63;
    /* The default Overworld: about sixty million blocks round. */
    private static final double WIDTH = 59_999_968;
    private static final double LINE = LIMIT + SpaceClimbs.ABOVE_THE_LIMIT;

    @Test
    void share_runsFromTheBuildLimitToTheLine() {
        assertEquals(0, ClimbView.share(LIMIT - 10, LIMIT), CLOSE);
        assertEquals(0.5, ClimbView.share(LIMIT + SpaceClimbs.ABOVE_THE_LIMIT / 2.0, LIMIT), CLOSE);
        assertEquals(1, ClimbView.share(LINE + 50, LIMIT), CLOSE);
    }

    @Test
    void shareInSpace_runsFromTheLineAtTheOrbitsToTheArrivalAtTheFloor() {
        assertEquals(1, ClimbView.shareInSpace(SystemLayout.ORBIT_Y + 10), CLOSE);
        assertEquals(1, ClimbView.shareInSpace(SystemLayout.ORBIT_Y), CLOSE);
        final double floor = SystemLayout.ORBIT_Y - SpaceClimbs.UNDER_THE_FLOOR;
        // Where one going down through the floor arrives in the world.
        final double arrives = LIMIT + SpaceClimbs.ABOVE_THE_LIMIT - SpaceClimbs.ARRIVE_UNDER;
        assertEquals(ClimbView.share(arrives, LIMIT), ClimbView.shareInSpace(floor), CLOSE);
        assertEquals(ClimbView.share(arrives, LIMIT), ClimbView.shareInSpace(floor - 30), CLOSE);
    }

    @Test
    void at_isTheClimbersOwnEyeUntilTheBlocksGiveWay() {
        for (final double height : new double[] {LIMIT, LIMIT + 100, LIMIT + 300}) {
            final ClimbView.Climb climb = at(height);
            assertEquals(height - SEA, climb.eyeHeight(), CLOSE);
            assertFalse(climb.blocksGone());
            assertArrayEquals(new double[] {0, 1, 0}, climb.up(), CLOSE);
        }
        final double middle = LIMIT + ClimbView.BLOCKS_GIVE_WAY * SpaceClimbs.ABOVE_THE_LIMIT;
        assertEquals(middle - SEA, at(middle).eyeHeight(), 1e-6);
        assertTrue(at(middle + 1).blocksGone());
        assertTrue(at(middle + 1).eyeHeight() > middle - SEA, "rising faster from there");
    }

    @Test
    void at_keepsTheHorizonWithinTheFarMapsReach() {
        for (double height = LIMIT; height <= LINE; height += 10) {
            final ClimbView.Climb climb = at(height);
            final double horizon = Math.sqrt(2 * climb.bend() * climb.eyeHeight());
            assertTrue(horizon <= ClimbView.REACH * (1 + 1e-9), "horizon " + horizon + " at " + height);
            assertTrue(climb.bend() <= WIDTH / (2 * Math.PI), "never rounder than the world is");
        }
    }

    @Test
    void at_endsWhereTheClimberArrivesInSpace() {
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 40, -12);
        final ClimbView.Climb top = ClimbView.at(SolarBody.OVERWORLD, 40, -12, LINE, LIMIT, SEA, WIDTH);
        assertArrayEquals(new double[] {spot[0], SystemLayout.ORBIT_Y, spot[1]}, top.space(), 1e-6);
        assertEquals(0, top.ground(), CLOSE);
        assertEquals(1, top.stars(), CLOSE);
        assertEquals(1, top.sphere(), CLOSE);
    }

    @Test
    void at_meltsTheRealGroundOnlyUnderTheVeil() {
        for (double height = LIMIT; height <= LINE; height += 5) {
            final ClimbView.Climb climb = at(height);
            if (climb.ground() > 0 && climb.ground() < 1) {
                assertTrue(climb.veil() > 0.5, "the ground melts under a thick veil at " + height);
                assertEquals(1, climb.sphere(), CLOSE, "over a whole sphere, never seen through");
            }
        }
    }

    @Test
    void at_risesSmoothly() {
        double last = 0;
        for (double height = LIMIT; height <= LINE; height += 1) {
            final double eye = at(height).eyeHeight();
            assertTrue(eye > last, "higher at " + height);
            assertTrue(last == 0 || eye / last < 1.05, "no jump at " + height);
            last = eye;
        }
    }

    private static ClimbView.Climb at(final double height) {
        return ClimbView.at(SolarBody.OVERWORLD, 0, 0, height, LIMIT, SEA, WIDTH);
    }
}
