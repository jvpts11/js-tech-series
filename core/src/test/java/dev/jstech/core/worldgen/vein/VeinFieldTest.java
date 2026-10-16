/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class VeinFieldTest {

    private static final long SEED = 1_234_567L;
    private static final int ZONES = 200;

    @Test
    void in_isTheSameForTheSameSeedOnAnyThread() {
        final VeinField field = field(0.3, 0.2);
        final ConcurrentHashMap<Integer, VeinField.Vein> seen = new ConcurrentHashMap<>();
        IntStream.range(0, ZONES).parallel().forEach(i -> {
            final VeinField.Vein vein = field.in(SEED, i, -i);
            if (vein != null) {
                seen.put(i, vein);
            }
        });
        for (int i = 0; i < ZONES; i++) {
            assertEquals(field.in(SEED, i, -i), seen.get(i), "zone " + i);
        }
    }

    @Test
    void in_fillsZonesByTheKindsFrequencies() {
        final VeinField field = field(0.3, 0.1);
        int first = 0;
        int second = 0;
        int empty = 0;
        for (int x = 0; x < ZONES; x++) {
            for (int z = 0; z < ZONES; z++) {
                final VeinField.Vein vein = field.in(SEED, x, z);
                if (vein == null) {
                    empty++;
                } else if (vein.kind() == 0) {
                    first++;
                } else {
                    second++;
                }
            }
        }
        final double all = ZONES * ZONES;
        assertEquals(0.3, first / all, 0.02);
        assertEquals(0.1, second / all, 0.02);
        assertEquals(0.6, empty / all, 0.02);
    }

    @Test
    void in_sharesEveryZoneWhenTheFrequenciesPassOne() {
        final VeinField field = field(0.9, 0.9);
        for (int x = 0; x < ZONES; x++) {
            assertTrue(field.in(SEED, x, 3) != null, "every zone holds a vein");
        }
    }

    @Test
    void in_holdsNothingWithNoKinds() {
        assertNull(new VeinField(List.of()).in(SEED, 4, 4));
    }

    @Test
    void in_placesEachVeinInsideItsZoneAndItsHeights() {
        final VeinField field = field(1.0);
        for (int x = -20; x < 20; x++) {
            final VeinField.Vein vein = field.in(SEED, x, 7);
            assertTrue(Math.floorDiv(vein.x(), VeinField.ZONE) == x && Math.floorDiv(vein.z(), VeinField.ZONE) == 7);
            assertTrue(vein.y() >= -32 && vein.y() <= 48, "height " + vein.y());
            assertTrue(vein.radius() >= 15 && vein.radius() <= 25, "radius " + vein.radius());
        }
    }

    @Test
    void reaching_findsTheSameVeinsFromEveryChunkTheyTouch() {
        final VeinField field = field(0.5);
        final List<VeinField.Vein> all = new ArrayList<>();
        for (int cx = 0; cx < 12; cx++) {
            for (final VeinField.Vein vein : field.reaching(SEED, cx * 16, 0, cx * 16 + 15, 15)) {
                if (!all.contains(vein)) {
                    all.add(vein);
                }
                assertTrue(vein.x() + vein.radius() >= cx * 16 && vein.x() - vein.radius() <= cx * 16 + 15);
            }
        }
        for (final VeinField.Vein vein : all) {
            assertTrue(field.reaching(SEED, vein.x(), vein.z(), vein.x(), vein.z()).contains(vein),
                    "a vein reaches its own middle");
        }
    }

    @Test
    void share_isWholeAtTheMiddleAndNoneAtTheEdge() {
        final VeinField.Vein vein = new VeinField.Vein(0, 0, 10, 0, 20, 5, 1L);
        assertEquals(1.0, vein.share(0, 10, 0), 1e-9);
        assertEquals(0.75, vein.share(10, 10, 0), 1e-9);
        assertEquals(0.0, vein.share(20, 10, 0), 1e-9);
        assertEquals(0.0, vein.share(0, 15, 0), 1e-9);
        assertTrue(vein.share(0, 12, 0) > vein.share(0, 14, 0), "it thins upwards too");
    }

    @Test
    void blocks_countsTwoFifthsOfTheBallAtItsDensity() {
        final VeinField.Vein vein = new VeinField.Vein(0, 0, 0, 0, 10, 10, 1L);
        assertEquals(Math.round(4.0 / 3.0 * Math.PI * 1000 * 0.5 * 0.4), vein.blocks(0.5));
    }

    @Test
    void near_listsTheNearestFirst() {
        final VeinField field = field(1.0);
        final List<VeinField.Vein> near = field.near(SEED, 100, 100, 120);
        assertTrue(near.size() > 2);
        for (int i = 1; i < near.size(); i++) {
            assertTrue(Math.hypot(near.get(i - 1).x() - 100, near.get(i - 1).z() - 100)
                    <= Math.hypot(near.get(i).x() - 100, near.get(i).z() - 100));
        }
    }

    private static VeinField field(final double... frequencies) {
        final List<VeinField.Kind> kinds = new ArrayList<>();
        for (final double frequency : frequencies) {
            kinds.add(new VeinField.Kind(frequency, -32, 48, 20, 6));
        }
        return new VeinField(kinds);
    }
}
