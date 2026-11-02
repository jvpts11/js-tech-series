/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.sky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ZodiacalPictureTest {

    private static final double CLOSE = 1e-9;

    @Test
    void light_fallsAwayFromTheSunAndIsGonePastItsReach() {
        assertTrue(ZodiacalPicture.light(10, 0) > ZodiacalPicture.light(40, 0));
        assertTrue(ZodiacalPicture.light(40, 0) > ZodiacalPicture.light(80, 0));
        assertEquals(0, ZodiacalPicture.light(ZodiacalPicture.REACH, 0), CLOSE);
        assertEquals(0, ZodiacalPicture.light(ZodiacalPicture.REACH + 10, 0), CLOSE);
    }

    @Test
    void light_isTheSameEitherSideOfTheSunAndOfItsPath() {
        assertEquals(ZodiacalPicture.light(30, 5), ZodiacalPicture.light(-30, 5), CLOSE);
        assertEquals(ZodiacalPicture.light(30, 5), ZodiacalPicture.light(30, -5), CLOSE);
    }

    @Test
    void light_widensAsItGoesFromTheSun() {
        // Ten degrees off the path, the light holds more of its middle's far from the sun than near it.
        final double near = ZodiacalPicture.light(10, 10) / ZodiacalPicture.light(10, 0);
        final double far = ZodiacalPicture.light(60, 10) / ZodiacalPicture.light(60, 0);
        assertTrue(far > near, "a cone, wider farther out: " + far + " against " + near);
    }

    @Test
    void draw_comesInSixteenLevels() {
        final double[] picture = ZodiacalPicture.draw(88, 32);
        for (final double light : picture) {
            assertEquals(Math.round(light * 16) / 16.0, light, CLOSE, "a level of sixteen");
        }
    }
}
