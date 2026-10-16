/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.strata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StrataMathTest {

    @Test
    void layerAt_countsDownThroughTheLayers() {
        final List<Integer> layers = List.of(4, 10, 6);
        assertEquals(0, StrataMath.layerAt(0, layers));
        assertEquals(0, StrataMath.layerAt(3.9, layers));
        assertEquals(1, StrataMath.layerAt(4, layers));
        assertEquals(2, StrataMath.layerAt(19.9, layers));
        assertEquals(-1, StrataMath.layerAt(20, layers));
        assertEquals(-1, StrataMath.layerAt(-1, layers));
    }

    @Test
    void noise_staysWithinOneAndIsSmooth() {
        double last = StrataMath.noise(3, 0, 17, 64);
        for (int x = 1; x < 2_000; x++) {
            final double now = StrataMath.noise(3, x, 17, 64);
            assertTrue(now >= -1 && now <= 1, "within one: " + now);
            assertTrue(Math.abs(now - last) < 0.1, "a block moves it little");
            last = now;
        }
    }

    @Test
    void noise_isTheSameForTheSameSeed() {
        assertEquals(StrataMath.noise(8, 123.5, -77, 32), StrataMath.noise(8, 123.5, -77, 32));
    }

    @Test
    void region_picksEverySequenceSomewhereAndOneForOne() {
        final Set<Integer> seen = new HashSet<>();
        for (int x = 0; x < 20_000; x += 97) {
            for (int z = 0; z < 20_000; z += 97) {
                final int region = StrataMath.region(5, x, z, 512, 3);
                assertTrue(region >= 0 && region < 3);
                seen.add(region);
            }
        }
        assertEquals(3, seen.size());
        assertEquals(0, StrataMath.region(5, 100, 100, 512, 1));
    }
}
