/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LootTallyTest {

    @Test
    void rows_sayInHowManyRollsEachItemCameAndItsSpread() {
        final LootTally tally = new LootTally();
        tally.roll(Map.of("ore", 2, "shard", 1));
        tally.roll(Map.of("ore", 4));
        tally.roll(Map.of("ore", 3));
        tally.roll(Map.of("ore", 1, "shard", 1));
        final List<LootTally.Row> rows = tally.rows();
        assertEquals(4, tally.rolls());
        assertEquals(new LootTally.Row("ore", 4, 2.5, 1, 4), rows.get(0));
        assertEquals(new LootTally.Row("shard", 2, 0.5, 0, 1), rows.get(1));
    }

    @Test
    void roll_leavesOutAnItemThatCameNone() {
        final LootTally tally = new LootTally();
        tally.roll(Map.of("ore", 0));
        assertEquals(List.of(), tally.rows());
        assertEquals(1, tally.rolls());
    }

    @Test
    void rows_isEmptyBeforeAnyRoll() {
        assertEquals(List.of(), new LootTally().rows());
    }
}
