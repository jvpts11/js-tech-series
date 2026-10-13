/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EngineSwapTest {

    @Test
    void ticksFor_emptyNetworkTakesTheBase() {
        assertEquals(EngineSwap.BASE_TICKS, EngineSwap.ticksFor(0));
    }

    @Test
    void ticksFor_growsWithTheItemTypes() {
        assertEquals(EngineSwap.BASE_TICKS + 301, EngineSwap.ticksFor(1_204));
    }

    @Test
    void ticksFor_stopsAtTheMost() {
        assertEquals(EngineSwap.MAX_TICKS, EngineSwap.ticksFor(Integer.MAX_VALUE));
    }

    @Test
    void ticksFor_negativeCountsAsNone() {
        assertEquals(EngineSwap.BASE_TICKS, EngineSwap.ticksFor(-5));
    }

    @Test
    void stepAt_startsByStoppingNewPlans() {
        assertEquals(EngineSwap.Step.STOP_NEW_PLANS, EngineSwap.stepAt(0, 1_000));
    }

    @Test
    void stepAt_spendsMostOfTheTimeOnIndexes() {
        assertEquals(EngineSwap.Step.DISCOVER, EngineSwap.stepAt(299, 1_000));
        assertEquals(EngineSwap.Step.BUILD_INDEXES, EngineSwap.stepAt(300, 1_000));
        assertEquals(EngineSwap.Step.BUILD_INDEXES, EngineSwap.stepAt(899, 1_000));
        assertEquals(EngineSwap.Step.PUBLISH, EngineSwap.stepAt(900, 1_000));
    }

    @Test
    void stepAt_isReadyAtTheEnd() {
        assertEquals(EngineSwap.Step.READY, EngineSwap.stepAt(1_000, 1_000));
        assertEquals(EngineSwap.Step.READY, EngineSwap.stepAt(5_000, 1_000));
    }

    @Test
    void before_followsTheOrderOfTheSteps() {
        assertEquals(true, EngineSwap.Step.STOP_NEW_PLANS.before(EngineSwap.Step.DISCOVER));
        assertEquals(false, EngineSwap.Step.READY.before(EngineSwap.Step.PUBLISH));
        assertEquals(false, EngineSwap.Step.BUILD_INDEXES.before(EngineSwap.Step.BUILD_INDEXES));
    }

    @Test
    void permille_clampsBothEnds() {
        assertEquals(0, EngineSwap.permille(-10, 100));
        assertEquals(500, EngineSwap.permille(50, 100));
        assertEquals(1_000, EngineSwap.permille(400, 100));
    }

    @Test
    void permille_noLengthIsDone() {
        assertEquals(1_000, EngineSwap.permille(0, 0));
    }
}
