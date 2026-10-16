/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LightMathTest {

    private static final int RED = 0xFF2010;
    private static final int BLUE = 0x2040FF;
    private static final double EPSILON = 1.0e-6;

    @Test
    void arriving_losesALevelEachBlock() {
        assertEquals(15, LightMath.arriving(15, 0));
        assertEquals(10, LightMath.arriving(15, 5));
        assertEquals(0, LightMath.arriving(5, 9), "never below dark");
    }

    @Test
    void reaches_onlyWhereTheGamesLightShowsItArrived() {
        assertTrue(LightMath.reaches(12, 12));
        assertTrue(LightMath.reaches(12, 11), "a level of rounding allowed");
        assertFalse(LightMath.reaches(12, 6), "a wall between: the game's light is darker than the source's would be");
        assertFalse(LightMath.reaches(0, 15), "a source that brings nothing");
    }

    @Test
    void tint_isWhiteWhereNoColourArrives() {
        assertEquals(LightMath.WHITE, LightMath.tint(List.of(), 15, 0));
        assertEquals(LightMath.WHITE, LightMath.tint(List.of(new LightMath.Arrival(14, RED)), 3, 0),
                "the game's light says the red lamp's does not reach here");
        assertEquals(LightMath.WHITE, LightMath.tint(List.of(new LightMath.Arrival(14, LightMath.WHITE)), 14, 0));
    }

    @Test
    void tint_coloursAPlaceByItsShareOfTheLight() {
        final int near = LightMath.tint(List.of(new LightMath.Arrival(15, RED)), 15, 0);
        final int far = LightMath.tint(List.of(new LightMath.Arrival(5, RED)), 5, 0);
        assertEquals(0xFF, near >> 16 & 0xFF, "red stays whole");
        assertTrue((near >> 8 & 0xFF) < 80, "green falls away near the lamp: " + Integer.toHexString(near));
        assertTrue((far >> 8 & 0xFF) > (near >> 8 & 0xFF), "less of a tint farther off");
        final int outdoors = LightMath.tint(List.of(new LightMath.Arrival(15, RED)), 15, 15);
        assertTrue((outdoors >> 8 & 0xFF) > (near >> 8 & 0xFF), "the open sky washes it out");
    }

    @Test
    void tint_mixesColoursByTheLightEachBrings() {
        final int mixed = LightMath.tint(List.of(new LightMath.Arrival(15, RED), new LightMath.Arrival(15, BLUE)),
                15, 0);
        assertTrue((mixed >> 16 & 0xFF) > 150 && (mixed & 0xFF) > 150, "red and blue make a purple: "
                + Integer.toHexString(mixed));
        assertTrue((mixed >> 8 & 0xFF) < (mixed & 0xFF) - 60, "with little green");
    }

    @Test
    void multiply_coloursASurface() {
        assertEquals(0xFF804020, LightMath.multiply(0xFF804020, LightMath.WHITE), "white leaves it");
        assertEquals(0xFF800000, LightMath.multiply(0xFF808080, 0xFF0000), "a grey under red light is red");
        assertEquals(0x7F000000, LightMath.multiply(0x7F000000, RED) & 0xFF000000, "alpha untouched");
    }

    @Test
    void hue_readsAColoursHue() {
        assertEquals(0.0, LightMath.hue(0xFF0000), EPSILON);
        assertEquals(120.0, LightMath.hue(0x00FF00), EPSILON);
        assertEquals(240.0, LightMath.hue(0x0000FF), EPSILON);
        assertEquals(300.0, LightMath.hue(0xFF00FF), EPSILON);
        assertEquals(-1.0, LightMath.hue(0x808080), EPSILON, "grey has none");
    }

    @Test
    void cone_spreadsRaysWithinItsAngle() {
        final List<double[]> rays = LightMath.cone(40, 3);
        assertEquals(1 + 6 + 12 + 18, rays.size());
        for (final double[] ray : rays) {
            assertEquals(1.0, Math.sqrt(ray[0] * ray[0] + ray[1] * ray[1] + ray[2] * ray[2]), EPSILON);
            assertTrue(Math.toDegrees(Math.acos(ray[2])) <= 20.0 + EPSILON, "within half the angle of the axis");
        }
    }

    @Test
    void along_keepsABeamBrightFarOff() {
        assertEquals(15, LightMath.along(15, 0, 64));
        assertEquals(8, LightMath.along(15, 64, 64), "half at its reach, past the game's fifteen blocks");
        assertEquals(0, LightMath.along(15, 65, 64));
    }

    @Test
    void blend_goesFromOneColourToTheOther() {
        assertEquals(0x808080, LightMath.blend(0x000000, LightMath.WHITE, 0.5), "halfway");
        assertEquals(LightMath.WHITE, LightMath.blend(RED, LightMath.WHITE, 2.0), "held at the far end");
    }
}
