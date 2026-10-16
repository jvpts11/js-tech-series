/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BuildOrderTest {

    private static final String BRICK = "test:brick";
    private static final String GLASS = "test:glass";
    private static final String BARS = "test:bars";

    @Test
    void plan_takesTheChosenBlockFirstAndCountsWhatItUses() {
        final List<BuildOrder.Want> wants = List.of(want(0, BRICK), want(1, GLASS, BARS), want(2, GLASS, BARS));

        final BuildOrder order = BuildOrder.plan(wants, Map.of(GLASS + "," + BARS, BARS),
                Map.of(BRICK, 5, GLASS, 5, BARS, 5)::get);

        assertEquals(List.of(BRICK, BARS, BARS), order.steps().stream().map(BuildOrder.Step::block).toList());
        assertEquals(Map.of(BRICK, 1, BARS, 2), order.uses());
        assertTrue(order.ready());
    }

    @Test
    void plan_turnsToAnotherBlockThatFitsWhenTheChosenRunsOut() {
        final List<BuildOrder.Want> wants = List.of(want(0, GLASS, BARS), want(1, GLASS, BARS));

        final BuildOrder order = BuildOrder.plan(wants, Map.of(GLASS + "," + BARS, GLASS),
                Map.of(GLASS, 1, BARS, 1)::get);

        assertEquals(List.of(GLASS, BARS), order.steps().stream().map(BuildOrder.Step::block).toList());
        assertTrue(order.ready());
    }

    @Test
    void plan_countsWhatTheSourceLacksAndMarksItsSteps() {
        final List<BuildOrder.Want> wants = List.of(want(0, BRICK), want(1, BRICK), want(2, BRICK));

        final BuildOrder order = BuildOrder.plan(wants, Map.of(), block -> BRICK.equals(block) ? 1 : 0);

        assertFalse(order.ready());
        assertEquals(Map.of(BRICK, 2), order.lacks());
        assertEquals(Map.of(BRICK, 3), order.uses());
        assertFalse(order.steps().getFirst().lacking());
        assertTrue(order.steps().get(2).lacking());
    }

    @Test
    void want_takesABlockAtLeastAndKeysItsChoiceByWhatFits() {
        assertThrows(IllegalArgumentException.class, () -> new BuildOrder.Want(0, 0, 0, List.of()));
        assertEquals(GLASS + "," + BARS, want(0, GLASS, BARS).key());
    }

    private static BuildOrder.Want want(final int x, final String... fits) {
        return new BuildOrder.Want(x, 0, 0, List.of(fits));
    }
}
