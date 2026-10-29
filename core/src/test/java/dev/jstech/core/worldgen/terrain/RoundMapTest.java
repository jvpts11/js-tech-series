/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoundMapTest {

    /* The map keeps its values as floats. */
    private static final double CLOSE = 1e-6;
    /* A world 3,600 blocks across: ten blocks a degree. */
    private static final double WIDTH = 3_600;

    @Test
    void at_readsATexelAtItsMiddle() {
        final RoundMap map = quarters();
        // The map is four texels across and two down: the texel at the top left spans longitudes -180 to -90 and
        // latitudes 90 to 0, its middle at -135, 45.
        assertEquals(0.1, map.at(-135, 45), CLOSE);
        assertEquals(0.2, map.at(-45, 45), CLOSE);
        assertEquals(0.8, map.at(135, -45), CLOSE);
    }

    @Test
    void at_shadesBetweenTexels() {
        final RoundMap map = quarters();
        assertEquals(0.15, map.at(-90, 45), CLOSE, "halfway between the first two texels of the top row");
        assertEquals(0.3, map.at(-135, 0), CLOSE, "and between the top row and the bottom");
    }

    @Test
    void at_wrapsRoundTheWorldWhereMinus180MeetsPlus180() {
        final RoundMap map = quarters();
        assertEquals(0.25, map.at(180, 45), CLOSE, "the east edge shades into the west's first texel");
        assertEquals(map.at(180, 45), map.at(-180, 45), CLOSE, "and both edges are the same place");
    }

    @Test
    void at_holdsThePolesAtTheirRow() {
        final RoundMap map = quarters();
        assertEquals(0.1, map.at(-135, 90), CLOSE, "north of the top row's middles it keeps their value");
        assertEquals(0.5, map.at(-135, -90), CLOSE, "and south of the bottom row's");
    }

    @Test
    void atBlock_laysLongitudeOnXAndLatitudeOnZNorthTowardsMinusZ() {
        final RoundMap map = quarters();
        assertEquals(map.at(-135, 45), map.atBlock(-1_350, -450, WIDTH), CLOSE);
        assertEquals(map.at(135, -45), map.atBlock(1_350, 450, WIDTH), CLOSE);
        assertEquals(90, RoundMap.longitude(900, WIDTH), CLOSE);
        assertEquals(-45, RoundMap.latitude(450, WIDTH), CLOSE);
    }

    @Test
    void constructor_refusesValuesOfAnotherSize() {
        assertThrows(IllegalArgumentException.class, () -> new RoundMap(4, 2, new float[7]));
    }

    /* Four texels across and two down: 0.1, 0.2, 0.3, 0.4 on top and 0.5, 0.6, 0.7, 0.8 below. */
    private static RoundMap quarters() {
        return new RoundMap(4, 2, new float[] {0.1F, 0.2F, 0.3F, 0.4F, 0.5F, 0.6F, 0.7F, 0.8F});
    }
}
