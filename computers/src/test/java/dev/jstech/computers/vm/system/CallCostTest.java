/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CallCostTest {

    @Test
    void describe_saysWhatATooltipShows() {
        assertEquals("free", CallCost.FREE.describe());
        assertEquals("50", CallCost.of(50).describe());
        assertEquals("50 plus one for every row it brings back", CallCost.perRow(50).describe());
        assertEquals("100 plus 2 for every 4 KB it reads or writes", CallCost.perBlock(100, 2).describe());
    }

    @Test
    void at_addsWhatEveryRowBroughtBack() {
        assertEquals(53, CallCost.perRow(50).at(3, 0));
        assertEquals(50, CallCost.of(50).at(3, 0));
        assertEquals(50, CallCost.perRow(50).at(-2, 0));
    }

    @Test
    void at_countsAPartOfABlockAsAWholeBlock() {
        final CallCost read = CallCost.perBlock(50, 1);
        assertEquals(50, read.at(0, 0));
        assertEquals(51, read.at(0, 1));
        assertEquals(51, read.at(0, CallCost.BLOCK_BYTES));
        assertEquals(52, read.at(0, CallCost.BLOCK_BYTES + 1));
    }

    @Test
    void at_neverGoesPastTheLargestCost() {
        final CallCost dearest = new CallCost(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, dearest.at(Integer.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test
    void new_refusesACostBelowNothing() {
        assertThrows(IllegalArgumentException.class, () -> new CallCost(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new CallCost(0, 0, -1));
    }
}
