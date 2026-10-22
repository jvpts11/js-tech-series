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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LightFloodTest {

    private static final int RED_GLASS = 0xB02E26;
    private static final int BLUE_GLASS = 0x3C44AA;
    private static final int GREEN = 0x40FF60;

    @Test
    void spread_losesALevelEachPlaceThroughOpenAir() {
        final LightFlood flood = LightFlood.spread(0, 0, 0, 15, LightMath.WHITE, new World());
        assertEquals(15, flood.level(0, 0, 0));
        assertEquals(12, flood.level(3, 0, 0));
        assertEquals(10, flood.level(2, 1, -2));
        assertEquals(1, flood.level(14, 0, 0));
        assertEquals(0, flood.level(15, 0, 0), "a source of fifteen reaches fourteen places");
        assertEquals(14, flood.reach());
    }

    @Test
    void spread_goesRoundAWallNotThroughIt() {
        final World world = new World();
        for (int y = -20; y <= 20; y++) {
            for (int z = -20; z <= 20; z++) {
                if (!(y == 0 && z == 6)) {
                    world.wall(2, y, z);
                }
            }
        }
        final LightFlood flood = LightFlood.spread(0, 0, 0, 15, GREEN, world);
        assertEquals(0, flood.level(2, 0, 0), "nothing is lit inside a wall");
        // Behind the wall, straight across, the light comes the long way, through the gap six places along.
        assertEquals(15 - (2 + 6 + 6 + 1), flood.level(3, 0, 0));
        assertEquals(GREEN, flood.colour(3, 0, 0));
    }

    @Test
    void spread_takesTheColourOfTheGlassItPasses() {
        final World world = new World();
        for (int y = -20; y <= 20; y++) {
            for (int z = -20; z <= 20; z++) {
                world.tint(2, y, z, RED_GLASS);
            }
        }
        final LightFlood flood = LightFlood.spread(0, 0, 0, 15, LightMath.WHITE, world);
        assertTrue(flood.tinted());
        assertEquals(LightMath.WHITE, flood.colour(1, 0, 0), "white on the near side");
        assertEquals(RED_GLASS, flood.colour(2, 0, 0), "red from the pane on");
        assertEquals(RED_GLASS, flood.colour(4, 1, 0));
        assertEquals(11, flood.level(4, 0, 0), "glass dims light no more than air");
    }

    @Test
    void spread_throughTwoTintsTakesBoth() {
        final World world = new World();
        for (int y = -20; y <= 20; y++) {
            for (int z = -20; z <= 20; z++) {
                world.tint(2, y, z, RED_GLASS);
                world.tint(4, y, z, BLUE_GLASS);
            }
        }
        final LightFlood flood = LightFlood.spread(0, 0, 0, 15, LightMath.WHITE, world);
        assertEquals(LightMath.multiply(RED_GLASS, BLUE_GLASS), flood.colour(5, 0, 0));
    }

    @Test
    void spread_keepsNoColoursOfItsOwnWhereNothingTints() {
        final LightFlood flood = LightFlood.spread(4, 5, 6, 10, GREEN, new World());
        assertFalse(flood.tinted());
        assertEquals(GREEN, flood.colour(6, 5, 6));
        assertEquals(GREEN, flood.colour(100, 5, 6), "the source's own colour where it does not reach");
    }

    @Test
    void spread_dimsMoreThroughABlockThatDims() {
        final World world = new World();
        world.dims(1, 0, 0, 3);
        final LightFlood flood = LightFlood.spread(0, 0, 0, 15, LightMath.WHITE, world);
        assertEquals(12, flood.level(1, 0, 0));
        assertEquals(13, flood.level(-2, 0, 0));
    }

    @Test
    void equals_isTheSameLightWhereverItWasWorkedOut() {
        final LightFlood one = LightFlood.spread(0, 0, 0, 12, GREEN, new World());
        final LightFlood two = LightFlood.spread(0, 0, 0, 12, GREEN, new World());
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());
        final World walled = new World();
        walled.wall(1, 0, 0);
        assertNotEquals(one, LightFlood.spread(0, 0, 0, 12, GREEN, walled), "a wall put up changes the light");
    }

    /* Open air, with walls, tints and dimming blocks where a test puts them. */
    private static final class World implements LightFlood.IPassage {

        private final Map<Long, Integer> dims = new HashMap<>();
        private final Map<Long, Integer> tints = new HashMap<>();

        void wall(final int x, final int y, final int z) {
            dims(x, y, z, LightFlood.OPAQUE);
        }

        void dims(final int x, final int y, final int z, final int amount) {
            this.dims.put(key(x, y, z), amount);
        }

        void tint(final int x, final int y, final int z, final int colour) {
            this.tints.put(key(x, y, z), colour);
        }

        @Override
        public int dims(final int x, final int y, final int z) {
            return this.dims.getOrDefault(key(x, y, z), 0);
        }

        @Override
        public int tint(final int x, final int y, final int z) {
            return this.tints.getOrDefault(key(x, y, z), LightMath.WHITE);
        }

        private static long key(final int x, final int y, final int z) {
            return ((long) x & 0xFFFFF) << 40 | ((long) y & 0xFFFFF) << 20 | (long) z & 0xFFFFF;
        }
    }
}
