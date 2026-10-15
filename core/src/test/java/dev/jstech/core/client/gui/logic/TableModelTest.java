/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TableModelTest {

    private static final List<String[]> ROWS = List.of(new String[] {"Crusher #1", "1284"},
            new String[] {"Furnace #1", "980"}, new String[] {"Crusher #2", "1102"});

    @Test
    void sortBy_sortsAnAmountFromTheLargestFirstThenTheOtherWay() {
        final TableModel<String[]> table = table();

        table.sortBy(1);
        assertEquals("1284", table.shown().get(0)[1]);
        table.sortBy(1);
        assertEquals("980", table.shown().get(0)[1]);
    }

    @Test
    void filter_keepsTheRowsHoldingTheWordsAndTheCheck() {
        final TableModel<String[]> table = table();

        table.filter("CRUSHER");
        assertEquals(2, table.shown().size());
        table.keep(r -> !r[0].endsWith("2"));
        assertEquals(List.of("Crusher #1"), table.shown().stream().map(r -> r[0]).toList());
    }

    private static TableModel<String[]> table() {
        return new TableModel<>(List.of(
                TableModel.Column.<String[]>words("Machine", 80, r -> r[0]),
                TableModel.Column.<String[]>amounts("Made", 40, r -> r[1], r -> Double.parseDouble(r[1]))),
                () -> ROWS);
    }
}
