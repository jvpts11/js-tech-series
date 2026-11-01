/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void terrace_stepsTheApronIntoLevelPoolsDownFromItsMiddle() {
        assertEquals(8, LandformShapes.terrace(0, 40, 8, 2), CLOSE);
        assertEquals(6, LandformShapes.terrace(5, 40, 8, 2), CLOSE);
        assertEquals(0, LandformShapes.terrace(39, 40, 8, 2), CLOSE);
        assertEquals(0, LandformShapes.terrace(45, 40, 8, 2), CLOSE);
        double last = Double.MAX_VALUE;
        for (int at = 0; at < 40; at++) {
            final double floor = LandformShapes.terrace(at, 40, 8, 2);
            assertEquals(0, floor % 2, CLOSE, "a pool's floor is a whole step: " + floor);
            assertTrue(floor <= last, "and none stands over the one inside it");
            last = floor;
        }
    }

    @Test
    void terraceRim_holdsEachPoolAtItsLowerEdge() {
        assertTrue(LandformShapes.terraceRim(9.5, 40, 8, 2), "just inside the step down from the pool at 6");
        assertFalse(LandformShapes.terraceRim(10.5, 40, 8, 2), "just past it, the next pool's upper end");
        assertFalse(LandformShapes.terraceRim(5, 40, 8, 2), "nor in a pool's middle");
        assertFalse(LandformShapes.terraceRim(40, 40, 8, 2), "nor past the apron");
    }

    @Test
    void chimney_standsWholeInItsMiddleAndLowerAtItsEdge() {
        assertEquals(5, LandformShapes.chimney(0, 2, 5), CLOSE);
        assertTrue(LandformShapes.chimney(1.5, 2, 5) < 5 && LandformShapes.chimney(1.5, 2, 5) > 0);
        assertEquals(0, LandformShapes.chimney(2, 2, 5), CLOSE);
    }

    @Test
    void spider_cutsAPitAndLegsThatThinAndFork() {
        final double[] legs = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
        assertEquals(3, LandformShapes.spider(1, 0.7, 20, legs), "its pit, whichever way");
        assertEquals(2, LandformShapes.spider(5, 0, 20, legs), "a leg near the pit, two deep");
        assertEquals(1, LandformShapes.spider(15, Math.PI, 20, legs), "a leg further out, one deep");
        assertEquals(0, LandformShapes.spider(10, Math.PI / 4, 20, legs), "nothing between the legs");
        assertEquals(1, LandformShapes.spider(18, 0.28, 20, legs), "a branch of the fork near its tip");
        assertEquals(0, LandformShapes.spider(18, 0.14, 20, legs), "nothing between the branches");
        assertEquals(0, LandformShapes.spider(20, 0, 20, legs), "nothing past it");
    }

    @Test
    void ridge_slopesEvenlyFromItsCrestToItsFoot() {
        assertEquals(4, LandformShapes.ridge(0, 6, 4), CLOSE);
        assertEquals(2, LandformShapes.ridge(-1.5, 6, 4), CLOSE);
        assertEquals(0, LandformShapes.ridge(3, 6, 4), CLOSE);
    }

    @Test
    void layeredMound_climbsInWholeStepsToItsMiddle() {
        assertEquals(30, LandformShapes.layeredMound(0, 50, 30, 3), CLOSE);
        assertEquals(18, LandformShapes.layeredMound(25, 50, 30, 3), CLOSE);
        assertEquals(0, LandformShapes.layeredMound(50, 50, 30, 3), CLOSE);
        double last = Double.MAX_VALUE;
        for (int at = 0; at < 50; at++) {
            final double top = LandformShapes.layeredMound(at, 50, 30, 3);
            assertEquals(0, top % 3, CLOSE, "each step is a whole band: " + top);
            assertTrue(top <= last, "and the mound only falls outwards");
            last = top;
        }
    }

    @Test
    void chamber_isWholeHeightInItsMiddleFallingAsAnEllipse() {
        assertEquals(6, LandformShapes.chamber(0, 10, 6), CLOSE);
        assertEquals(4.8, LandformShapes.chamber(6, 10, 6), CLOSE);
        assertEquals(0, LandformShapes.chamber(10, 10, 6), CLOSE);
        assertEquals(0, LandformShapes.chamber(14, 10, 6), CLOSE, "nothing past its edge");
    }

    @Test
    void heap_isAConeOfRubble() {
        assertEquals(4, LandformShapes.heap(0, 8, 4), CLOSE);
        assertEquals(2, LandformShapes.heap(4, 8, 4), CLOSE);
        assertEquals(0, LandformShapes.heap(8, 8, 4), CLOSE);
        assertEquals(0, LandformShapes.heap(12, 8, 4), CLOSE, "nothing past its foot");
    }

    @Test
    void fade_holdsInsideAndFadesAtTheEdge() {
        assertEquals(1, LandformShapes.fade(5, 20, 4), CLOSE);
        assertEquals(0.5, LandformShapes.fade(18, 20, 4), CLOSE);
        assertEquals(0, LandformShapes.fade(20, 20, 4), CLOSE);
    }
}
