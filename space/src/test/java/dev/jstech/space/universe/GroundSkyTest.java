/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GroundSkyTest {

    private static final double CLOSE = 1e-9;
    private static final double LEAN = SystemLayout.OVERWORLD_OVER_THE_MOON;

    @Test
    void towards_putsTheOverworldOverheadWhereItStandsStill() {
        final double[] over = GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, 0, LEAN, 0);
        assertEquals(1, over[1], CLOSE, "straight up");
    }

    @Test
    void towards_putsItHighInTheNorthAtTheMiddleOfTheMap() {
        final double[] spawn = GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, 0, 0, 0);
        assertEquals(Math.cos(Math.toRadians(LEAN)), spawn[1], CLOSE, "seventy degrees high");
        assertTrue(spawn[2] < 0, "towards the north");
        assertEquals(0, spawn[0], CLOSE, "neither east nor west");
    }

    @Test
    void towards_lowersItAsOneGoesAndHidesItOnTheFarSide() {
        final double near = GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, 30, 0, 0)[1];
        final double farther = GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, 60, 0, 0)[1];
        final double[] quarter = GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, 90, 0, 0);
        assertTrue(near > farther && farther > 0, "lower the farther east");
        assertEquals(0, quarter[1], CLOSE, "on the horizon a quarter of the way round");
        assertTrue(quarter[0] < 0, "towards the west, where the near side's middle is");
        assertTrue(GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, 180, 0, 0)[1] < 0, "hidden on the far side");
    }

    @Test
    void towards_standsStillWhateverTheTime() {
        final double[] now = GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, 25, -10, 0);
        final double[] later = GroundSky.towards(SolarBody.MOON, SolarBody.OVERWORLD, 25, -10, 987_654);
        for (int i = 0; i < 3; i++) {
            assertEquals(now[i], later[i], CLOSE);
        }
    }

    @Test
    void seen_keepsDirectionsWhole() {
        final double[] north = GroundSky.seen(SolarBody.MOON, 40, 15, 0, SystemLayout.north(SolarBody.OVERWORLD));
        assertEquals(1, Math.sqrt(north[0] * north[0] + north[1] * north[1] + north[2] * north[2]), CLOSE);
    }

    @Test
    void radius_isAboutNineDegrees() {
        assertEquals(9, Math.toDegrees(GroundSky.radius(SolarBody.MOON, SolarBody.OVERWORLD)), 0.25);
    }
}
