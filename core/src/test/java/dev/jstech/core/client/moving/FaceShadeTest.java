/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FaceShadeTest {

    private static final float CLOSE = 1.0e-6F;
    /* The game's shades in a world with a sky: east or west, north or south, up and down. */
    private static final float EAST = 0.6F;
    private static final float NORTH = 0.8F;
    private static final float UP = 1.0F;
    private static final float DOWN = 0.5F;

    @Test
    void of_givesAFaceFacingASideTheGamesShadeOfThatSide() {
        assertEquals(EAST, FaceShade.of(1, 0, 0, EAST, NORTH, UP, DOWN), CLOSE);
        assertEquals(EAST, FaceShade.of(-1, 0, 0, EAST, NORTH, UP, DOWN), CLOSE);
        assertEquals(NORTH, FaceShade.of(0, 0, -1, EAST, NORTH, UP, DOWN), CLOSE);
        assertEquals(UP, FaceShade.of(0, 1, 0, EAST, NORTH, UP, DOWN), CLOSE);
        assertEquals(DOWN, FaceShade.of(0, -1, 0, EAST, NORTH, UP, DOWN), CLOSE);
    }

    @Test
    void of_givesAFaceFacingBetweenTwoSidesHalfOfEach() {
        final float half = (float) Math.sqrt(0.5);
        assertEquals((EAST + NORTH) / 2, FaceShade.of(half, 0, half, EAST, NORTH, UP, DOWN), CLOSE);
        assertEquals((NORTH + DOWN) / 2, FaceShade.of(0, -half, half, EAST, NORTH, UP, DOWN), CLOSE);
    }

    @Test
    void of_givesATopTurnedUpsideDownTheShadeOfABottom() {
        assertEquals(DOWN, FaceShade.of(0, -1, 0, EAST, NORTH, UP, DOWN), CLOSE);
    }

    @Test
    void shaded_takesAFaceDarkerBuiltShadedAsShaded() {
        assertTrue(FaceShade.shaded(204, 255, 0));
        assertTrue(FaceShade.shaded(64, 128, -1));
    }

    @Test
    void shaded_takesASideFaceAsBrightBothWaysAsUnshaded() {
        assertFalse(FaceShade.shaded(255, 255, 0));
        assertFalse(FaceShade.shaded(180, 180, 0.3F));
    }

    @Test
    void shaded_takesAFaceFacingUpAsShadedThoughAsBrightBothWays() {
        assertTrue(FaceShade.shaded(255, 255, 1));
    }

    @Test
    void shaded_takesABlackFaceAsUnshaded() {
        assertFalse(FaceShade.shaded(0, 0, 1));
    }

    @Test
    void shade_scalesRedGreenAndBlueAndKeepsAlpha() {
        final int colour = 0x80_40_80_FF;
        final int shaded = FaceShade.shade(colour, 0.5F);
        assertEquals(0x80, shaded >>> 24);
        assertEquals(0x20, shaded >> 16 & 0xFF);
        assertEquals(0x40, shaded >> 8 & 0xFF);
        assertEquals(0x80, shaded & 0xFF);
    }

    @Test
    void shade_neverGoesPastTheBrightest() {
        assertEquals(0xFF, FaceShade.shade(0xFF_F0_F0_F0, 1.5F) & 0xFF);
    }

    @Test
    void brightest_findsTheBrightestOfRedGreenAndBlue() {
        assertEquals(0xC0, FaceShade.brightest(0xFF_10_C0_20));
        assertEquals(0x30, FaceShade.brightest(0x00_30_10_20));
    }
}
