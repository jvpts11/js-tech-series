/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FurnaceReachTest {

    @Test
    void meltsDust_ofMetalsNoHotterThanIron() {
        assertTrue(FurnaceReach.meltsDust(505.0));
        assertTrue(FurnaceReach.meltsDust(1_811.0));
    }

    @Test
    void meltsDust_notOfTheHotterOnes() {
        assertFalse(FurnaceReach.meltsDust(1_941.0));
        assertFalse(FurnaceReach.meltsDust(0.0));
    }
}
