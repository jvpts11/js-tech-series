/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LandformShapesTest {

    private static final double CLOSE = 1e-9;

    @Test
    void crater_isDeepestInItsMiddleRaisedAtItsRimAndFlatBeyond() {
        assertEquals(-10, LandformShapes.crater(0, 20, 10, 3, 0), CLOSE);
        assertTrue(LandformShapes.crater(19.9, 20, 10, 3, 0) > 2.5, "up at the rim");
        assertEquals(3, LandformShapes.crater(20, 20, 10, 3, 0), CLOSE);
        assertEquals(0, LandformShapes.crater(31, 20, 10, 3, 0), CLOSE);
    }

    @Test
    void crater_aCentralPeakRisesInItsMiddle() {
        assertEquals(-10 + 6, LandformShapes.crater(0, 20, 10, 3, 6), CLOSE);
        assertEquals(LandformShapes.crater(10, 20, 10, 3, 0), LandformShapes.crater(10, 20, 10, 3, 6), CLOSE);
    }

    @Test
    void volcano_aShieldIsGentlerThanAStratovolcano() {
        final double shield = LandformShapes.volcano(20, 40, 30, true);
        final double strato = LandformShapes.volcano(20, 40, 30, false);
        assertTrue(strato > shield, "the strato stands higher half way: " + strato + " " + shield);
        assertEquals(0, LandformShapes.volcano(40, 40, 30, true), CLOSE);
        assertTrue(LandformShapes.volcano(0, 40, 30, false) < LandformShapes.volcano(5, 40, 30, false),
                "its summit crater dips");
        assertTrue(LandformShapes.inSummit(1, 40) && !LandformShapes.inSummit(10, 40));
    }

    @Test
    void mesa_isFlatOnTopWithCliffs() {
        assertEquals(12, LandformShapes.mesa(0, 30, 12, 4), CLOSE);
        assertEquals(12, LandformShapes.mesa(26, 30, 12, 4), CLOSE);
        assertEquals(6, LandformShapes.mesa(28, 30, 12, 4), CLOSE);
        assertEquals(0, LandformShapes.mesa(30, 30, 12, 4), CLOSE);
    }

    @Test
    void rille_isASmoothTroughDeepestInItsMiddle() {
        assertEquals(6, LandformShapes.rille(0, 10, 6), CLOSE);
        final double near = LandformShapes.rille(2, 10, 6);
        final double far = LandformShapes.rille(4, 10, 6);
        assertTrue(near < 6 && far < near && far > 0, "it rises evenly to its rims: " + near + ", " + far);
        assertEquals(LandformShapes.rille(-3, 10, 6), LandformShapes.rille(3, 10, 6), CLOSE);
        assertEquals(0, LandformShapes.rille(5, 10, 6), CLOSE);
    }

    @Test
    void glacier_isThickNearItsHeadAndThinsToItsSnout() {
        final double head = LandformShapes.glacier(-20, 0, 40, 12, 10);
        final double middle = LandformShapes.glacier(0, 0, 40, 12, 10);
        final double snout = LandformShapes.glacier(36, 0, 40, 12, 10);
        assertTrue(head > middle && middle > snout && snout > 0, "it thins down its length: " + head + ", "
                + middle + ", " + snout);
        assertTrue(LandformShapes.glacier(-39, 0, 40, 12, 10) < head, "and rises out of the land at its head");
        assertTrue(LandformShapes.glacier(0, 4, 40, 12, 10) < middle, "and thins to its edges");
        assertEquals(0, LandformShapes.glacier(0, 6, 40, 12, 10), CLOSE);
        assertEquals(0, LandformShapes.glacier(40, 0, 40, 12, 10), CLOSE);
    }

    @Test
    void dune_risesGentlyAndFallsSteeply() {
        assertEquals(0, LandformShapes.dune(0, 20, 4), CLOSE);
        assertEquals(4, LandformShapes.dune(14, 20, 4), CLOSE);
        assertTrue(LandformShapes.dune(20, 20, 4) < 1e-6, "back to the trough at a wavelength");
        assertTrue(LandformShapes.dune(-6, 20, 4) > 0, "repeats either way");
    }

    @Test
    void canyon_isDeepOnItsFloorAndNothingPastItsRim() {
        assertEquals(15, LandformShapes.canyon(0, 20, 15), CLOSE);
        assertEquals(15, LandformShapes.canyon(3, 20, 15), CLOSE);
        assertTrue(LandformShapes.canyon(8, 20, 15) < 15 && LandformShapes.canyon(8, 20, 15) > 0);
        assertEquals(0, LandformShapes.canyon(10, 20, 15), CLOSE);
    }

    @Test
    void fade_holdsInsideAndFadesAtTheEdge() {
        assertEquals(1, LandformShapes.fade(5, 20, 4), CLOSE);
        assertEquals(0.5, LandformShapes.fade(18, 20, 4), CLOSE);
        assertEquals(0, LandformShapes.fade(20, 20, 4), CLOSE);
    }
}
