/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArmMotionTest {

    private static final double CLOSE = 1.0e-6;
    /* A rail along z on the bay's west edge, its feeder past its far end, at rest its tool over the bay. */
    private static final ArmMotion.Rig RIG = new ArmMotion.Rig(new double[] {-8.5, 4, -11.5},
            new double[] {-8.5, 4, 1.4}, new double[] {-8.5, 5.5, 2.5}, new double[] {1.5, 4, 0}, 5.0, 4.6);
    private static final double[] PLACE = {-1.5, 2.5, -6.5};

    @Test
    void of_carriesTheBlockFromItsFeederToItsPlace() {
        final double[] start = RIG.restAt(0.5);
        final ArmMotion.State gripping = ArmMotion.of(RIG, 0.5, start, PLACE, ArmMotion.FETCH + ArmMotion.GRIP - 1);
        assertArrayEquals(RIG.feeder(), gripping.tool(), CLOSE, "it grips the block in its feeder");
        assertTrue(gripping.holding());
        assertEquals(1.0, gripping.rail(), CLOSE, "at the end of its rail by the feeder");
        final ArmMotion.State set = ArmMotion.of(RIG, 0.5, start, PLACE, ArmMotion.SET - 1.0e-9);
        assertArrayEquals(PLACE, set.tool(), 1.0e-6, "the block over its place as the swing ends");
        assertEquals(RIG.along(PLACE), set.rail(), CLOSE, "its base the nearest it comes to the place");
    }

    @Test
    void of_weldsWhatItSetThenFoldsUpToRest() {
        final ArmMotion.State welding = ArmMotion.of(RIG, 0.5, RIG.restAt(0.5), PLACE, ArmMotion.SET + 2);
        assertTrue(welding.welding() && !welding.holding());
        final ArmMotion.State folded = ArmMotion.of(RIG, 0.5, RIG.restAt(0.5), PLACE,
                ArmMotion.END + ArmMotion.REST + ArmMotion.FOLD + 1);
        assertArrayEquals(RIG.restAt(RIG.along(PLACE)), folded.tool(), CLOSE);
        assertFalse(folded.welding());
    }

    @Test
    void idle_restsByItsFeeder() {
        final ArmMotion.State idle = ArmMotion.idle(RIG);
        assertEquals(1.0, idle.rail(), CLOSE);
        assertArrayEquals(RIG.restAt(1.0), idle.tool(), CLOSE);
    }

    @Test
    void reaches_coversTheHullsMiddleFromItsRail() {
        assertTrue(RIG.reaches(new double[] {-0.5, 2.5, -6.5}), "the hull's middle on its side");
        assertFalse(RIG.reaches(new double[] {6.5, 2.5, -6.5}), "not the far wing's tip");
    }
}
