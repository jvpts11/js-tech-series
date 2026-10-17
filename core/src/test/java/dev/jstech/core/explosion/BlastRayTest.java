/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlastRayTest {

    @Test
    void advance_startsInTheCentresOwnBlock() {
        final BlastRay ray = new BlastRay(4.5, 2.5, 7.5, 1, 0, 0);
        assertEquals(0, ray.nextDistance(), 1.0e-12);
        ray.advance();
        assertTrue(ray.atCentre());
        assertEquals(4, ray.x());
        assertEquals(2, ray.y());
        assertEquals(7, ray.z());
    }

    @Test
    void advance_crossesEachBlockAlongTheLine() {
        final BlastRay ray = new BlastRay(0.5, 0.5, 0.5, 1, 0, 0);
        ray.advance();
        for (int block = 1; block <= 5; block++) {
            assertEquals(block - 0.5, ray.nextDistance(), 1.0e-12);
            ray.advance();
            assertFalse(ray.atCentre());
            assertEquals(block, ray.x());
            assertEquals(block - 0.5, ray.distance(), 1.0e-12);
        }
    }

    @Test
    void advance_aDiagonalStepsOneAxisAtATime() {
        final double unit = 1 / Math.sqrt(2);
        final BlastRay ray = new BlastRay(0.5, 0.5, 0.25, unit, 0, -unit);
        ray.advance();
        ray.advance();
        // The face of z at 0 is a quarter away and is crossed first, then the face of x at 1.
        assertEquals(0, ray.x());
        assertEquals(-1, ray.z());
        ray.advance();
        assertEquals(1, ray.x());
        assertEquals(-1, ray.z());
    }

    @Test
    void spend_stopsTheRayWhenNothingIsLeft() {
        final BlastRay ray = new BlastRay(0, 0, 0, 0, 1, 0);
        ray.spend(0.6);
        assertTrue(ray.alive());
        assertEquals(0.4, ray.share(), 1.0e-12);
        ray.spend(0.4);
        assertFalse(ray.alive());
    }
}
