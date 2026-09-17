/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RackThermalsTest {

    @Test
    void budgetWatts_addsOneCoolingUnitsWorthPerUnit() {
        assertEquals(RackThermals.PASSIVE_BUDGET_W, RackThermals.budgetWatts(0));
        assertEquals(RackThermals.PASSIVE_BUDGET_W + RackThermals.COOLING_UNIT_BUDGET_W,
                RackThermals.budgetWatts(1));
        assertEquals(RackThermals.PASSIVE_BUDGET_W + 3 * RackThermals.COOLING_UNIT_BUDGET_W,
                RackThermals.budgetWatts(3));
    }

    @Test
    void budgetWatts_treatsANegativeCountAsNone() {
        assertEquals(RackThermals.PASSIVE_BUDGET_W, RackThermals.budgetWatts(-2));
    }

    @Test
    void throttlePercent_isFullWithinBudget() {
        assertEquals(100, RackThermals.throttlePercent(500, 1000));
        assertEquals(100, RackThermals.throttlePercent(1000, 1000));
    }

    @Test
    void throttlePercent_fallsInProportionPastBudget() {
        assertEquals(50, RackThermals.throttlePercent(2000, 1000));
        assertEquals(80, RackThermals.throttlePercent(1250, 1000));
    }

    @Test
    void throttlePercent_neverFallsBelowTheFloor() {
        // Twenty times over budget would be five percent without the floor.
        assertEquals(RackThermals.MIN_THROTTLE_PERCENT, RackThermals.throttlePercent(20_000, 1000));
    }

    @Test
    void throttlePercent_isFullWhenNothingIsDrawing() {
        // Also the guard that keeps the proportion below from ever dividing by zero.
        assertEquals(100, RackThermals.throttlePercent(0, 0));
        assertEquals(100, RackThermals.throttlePercent(-5, 0));
    }

    @Test
    void throttlePercent_holdsTheFloorWithNoBudgetAtAll() {
        assertEquals(RackThermals.MIN_THROTTLE_PERCENT, RackThermals.throttlePercent(1, 0));
    }

    @Test
    void throttlePercent_doesNotOverflowOnAHugeLoad() {
        /*
         * One watt over a budget near the int ceiling is barely throttled at all, and the exact answer is
         * the point: worked out in int the numerator wraps and the answer collapses to the floor, so
         * anything looser than this would pass on the broken arithmetic too.
         */
        assertEquals(99, RackThermals.throttlePercent(Integer.MAX_VALUE, Integer.MAX_VALUE - 1));
    }

    @Test
    void throttled_saysSoOnlyPastBudget() {
        assertFalse(RackThermals.throttled(1000, 1000));
        assertTrue(RackThermals.throttled(1001, 1000));
    }
}
