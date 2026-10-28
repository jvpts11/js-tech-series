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

import java.util.Arrays;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class OverworldMapTest {

    private static final int WIDTH = 256;
    private static final long SEED = 42;
    /* The tones of the polar caps. */
    private static final Set<Integer> CAP = Set.of(0xD8E4F2, 0xE4EDF7, 0xEEF4FA, 0xC4D4EA);
    private static final int TILE = 256;

    @Test
    void maps_areTheSameForTheSameSeedAndOtherForAnother() {
        final OverworldMap.Maps one = OverworldMap.maps(WIDTH, SEED);
        assertArrayEquals(one.colours(), OverworldMap.maps(WIDTH, SEED).colours());
        assertArrayEquals(one.heights(), OverworldMap.maps(WIDTH, SEED).heights());
        assertFalse(Arrays.equals(one.colours(), OverworldMap.maps(WIDTH, SEED + 1).colours()));
    }

    @Test
    void maps_coverBothPolesWithTheirCaps() {
        final int[] colours = OverworldMap.maps(WIDTH, SEED).colours();
        final int last = (WIDTH / 2 - 1) * WIDTH;
        for (int x = 0; x < WIDTH; x++) {
            assertTrue(CAP.contains(colours[x]), "north pole at " + x);
            assertTrue(CAP.contains(colours[last + x]), "south pole at " + x);
        }
    }

    @Test
    void maps_leaveTheSeaFlatAndRaiseTheLand() {
        final double[] heights = OverworldMap.maps(WIDTH, SEED).heights();
        final long flat = Arrays.stream(heights).filter(height -> height == 0).count();
        final long raised = Arrays.stream(heights).filter(height -> height > 0.01).count();
        // About three fifths of the world is sea; the caps and a little of the land's roughness take some of each.
        assertTrue(flat > heights.length * 0.4 && flat < heights.length * 0.7, "sea share " + flat);
        assertTrue(raised > heights.length * 0.2, "land share " + raised);
        assertTrue(Arrays.stream(heights).max().orElseThrow() < 1.2, "the highest peaks stay near 1");
    }

    @Test
    void clouds_coverSomeOfTheWorldAndMoveAsTheyTurn() {
        final boolean[] tile = tile();
        final double[] weather = OverworldMap.weather(WIDTH, SEED);
        final boolean[] now = OverworldMap.clouds(WIDTH, 0.0, SEED, tile, weather);
        final boolean[] later = OverworldMap.clouds(WIDTH, 0.1, SEED, tile, weather);
        final double share = share(now);
        assertTrue(share > 0.05 && share < 0.6, "cloud cover " + share);
        assertFalse(Arrays.equals(now, later));
        assertArrayEquals(now, OverworldMap.clouds(WIDTH, 0.0, SEED, tile, weather));
    }

    @Test
    void clouds_comeOnlyFromTheGamesCloudMap() {
        final boolean[] clear = new boolean[TILE * TILE];
        final boolean[] none = OverworldMap.clouds(WIDTH, 0.3, SEED, clear, OverworldMap.weather(WIDTH, SEED));
        assertEquals(0, share(none));
    }

    /* A cloud map like the game's: clumps of cloud, about half of it. */
    private static boolean[] tile() {
        final SplittableRandom random = new SplittableRandom(SEED);
        final boolean[] tile = new boolean[TILE * TILE];
        for (int y = 0; y < TILE; y += 8) {
            for (int x = 0; x < TILE; x += 8) {
                final boolean cloud = random.nextBoolean();
                for (int dy = 0; dy < 8; dy++) {
                    for (int dx = 0; dx < 8; dx++) {
                        tile[(y + dy) * TILE + x + dx] = cloud;
                    }
                }
            }
        }
        return tile;
    }

    private static double share(final boolean[] cover) {
        int count = 0;
        for (final boolean cloud : cover) {
            if (cloud) {
                count++;
            }
        }
        return (double) count / cover.length;
    }
}
