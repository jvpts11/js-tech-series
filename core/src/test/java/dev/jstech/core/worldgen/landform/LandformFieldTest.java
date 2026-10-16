/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LandformFieldTest {

    private static final LandformField CRATERS = new LandformField(128, 0.6, 8, 40, 77);

    @Test
    void in_isTheSameForTheSameSeedAndCell() {
        assertEquals(CRATERS.in(42, 3, -5), CRATERS.in(42, 3, -5));
    }

    @Test
    void in_liesWithinItsCellAndItsSizes() {
        for (int cx = -20; cx <= 20; cx++) {
            for (int cz = -20; cz <= 20; cz++) {
                final LandformField.Instance one = CRATERS.in(9, cx, cz);
                if (one != null) {
                    assertTrue(one.x() >= cx * 128 && one.x() < (cx + 1) * 128, "in its cell across");
                    assertTrue(one.z() >= cz * 128 && one.z() < (cz + 1) * 128, "and along");
                    assertTrue(one.radius() >= 8 && one.radius() <= 40, "of its sizes");
                }
            }
        }
    }

    @Test
    void in_holdsOneInAboutItsChanceOfCells() {
        int held = 0;
        for (int cx = 0; cx < 100; cx++) {
            for (int cz = 0; cz < 100; cz++) {
                held += CRATERS.in(5, cx, cz) != null ? 1 : 0;
            }
        }
        assertTrue(held > 5_500 && held < 6_500, "about six in ten: " + held);
    }

    @Test
    void reaching_findsALandformFromEveryChunkItTouches() {
        final LandformField.Instance one = firstOf(CRATERS, 11);
        final int chunkX = Math.floorDiv(one.x() + one.radius() - 1, 16) * 16;
        final int chunkZ = Math.floorDiv(one.z(), 16) * 16;
        final List<LandformField.Instance> found = CRATERS.reaching(11, chunkX, chunkZ, chunkX + 15, chunkZ + 15);
        assertTrue(found.contains(one), "the chunk at its edge finds it");
    }

    @Test
    void constructor_refusesACellSmallerThanAChunkAndSizesBackwards() {
        assertThrows(IllegalArgumentException.class, () -> new LandformField(8, 0.5, 4, 8, 1));
        assertThrows(IllegalArgumentException.class, () -> new LandformField(64, 0.5, 9, 8, 1));
    }

    @Test
    void instance_measuresAlongAndAcrossItsTurn() {
        final LandformField.Instance east = new LandformField.Instance(0, 0, 10, 0, 1);
        assertEquals(5, east.along(5, 2), 1e-9);
        assertEquals(2, east.across(5, 2), 1e-9);
        assertEquals(Math.hypot(5, 2), east.distance(5, 2), 1e-9);
    }

    private static LandformField.Instance firstOf(final LandformField field, final long seed) {
        for (int cx = 0; ; cx++) {
            final LandformField.Instance one = field.in(seed, cx, 0);
            if (one != null) {
                return one;
            }
        }
    }
}
