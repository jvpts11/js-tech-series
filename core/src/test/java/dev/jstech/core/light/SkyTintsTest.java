/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SkyTintsTest {

    private static final int GREEN_GLASS = 0x5E7C16;
    private static final int RED_GLASS = 0xB02E26;
    private static final int FLOOR = 0;
    private static final int ROOF = 6;

    @Test
    void below_lightsThePlacesUnderAGlassRoofDownToTheFloor() {
        final Map<Integer, Integer> lit = below(Map.of(ROOF, GREEN_GLASS), FLOOR, 0, ROOF);
        assertEquals(5, lit.size(), "the five places between the roof and the floor");
        assertEquals(GREEN_GLASS, lit.get(1));
        assertEquals(GREEN_GLASS, lit.get(5));
    }

    @Test
    void below_takesEveryTintItPassesAndStartsOnlyAtTheTopmost() {
        final Map<Integer, Integer> tints = Map.of(ROOF, GREEN_GLASS, ROOF - 2, RED_GLASS);
        final Map<Integer, Integer> lit = below(tints, FLOOR, 0, ROOF);
        assertEquals(GREEN_GLASS, lit.get(ROOF - 1));
        assertEquals(LightMath.multiply(GREEN_GLASS, RED_GLASS), lit.get(ROOF - 3));
        assertTrue(below(tints, FLOOR, 0, ROOF - 2).isEmpty(), "the lower pane's places are the roof's");
    }

    @Test
    void below_goesNoFartherThanItsDepth() {
        final Map<Integer, Integer> lit = below(Map.of(40, GREEN_GLASS), -100, 0, 40);
        assertEquals(SkyTints.DEPTH, lit.size());
        assertEquals(40 - SkyTints.DEPTH, lit.keySet().stream().mapToInt(Integer::intValue).min().orElseThrow());
    }

    @Test
    void skyThrough_laysTheGlassHueOverAPlaceByDaylight() {
        assertEquals(LightMath.WHITE, LightMath.skyThrough(LightMath.WHITE, GREEN_GLASS, 0), "none by night");
        final int tinted = LightMath.skyThrough(LightMath.WHITE, GREEN_GLASS, LightMath.BRIGHTEST);
        assertTrue((tinted >> 8 & 0xFF) > (tinted >> 16 & 0xFF), "green above red: " + Integer.toHexString(tinted));
        assertEquals(LightMath.WHITE, LightMath.skyThrough(LightMath.WHITE, LightMath.WHITE, LightMath.BRIGHTEST));
    }

    /* The places lit below the block at {@code y} of a column of tints over a floor, by height. */
    private static Map<Integer, Integer> below(final Map<Integer, Integer> tints, final int floor, final int x,
                                               final int y) {
        final Map<Integer, Integer> out = new HashMap<>();
        final LightFlood.IPassage column = new LightFlood.IPassage() {
            @Override
            public int dims(final int px, final int py, final int pz) {
                return py <= floor ? LightFlood.OPAQUE : 0;
            }

            @Override
            public int tint(final int px, final int py, final int pz) {
                return tints.getOrDefault(py, LightMath.WHITE);
            }
        };
        SkyTints.below(x, y, 0, column, (px, py, pz, colour) -> out.put(py, colour));
        return out;
    }
}
