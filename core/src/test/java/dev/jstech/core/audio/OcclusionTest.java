/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OcclusionTest {

    @Test
    void factor_isWholeWithNothingInTheWay() {
        assertEquals(1.0F, Occlusion.factor(0));
        assertEquals(1.0F, Occlusion.factor(-3));
    }

    @Test
    void walls_countsTheWallsBackFromTheShareThatGetsThrough() {
        assertEquals(0, Occlusion.walls(1.0F));
        for (int blocks = 1; blocks <= 3; blocks++) {
            assertEquals(blocks, Occlusion.walls(Occlusion.factor(blocks)));
        }
        assertEquals(4, Occlusion.walls(Occlusion.factor(9)));
    }

    @Test
    void factor_fallsWithEveryWallButNeverToNothing() {
        assertEquals(0.6F, Occlusion.factor(1), 0.001F);
        assertTrue(Occlusion.factor(2) < Occlusion.factor(1));
        assertEquals(0.15F, Occlusion.factor(40), 0.001F);
    }
}
