/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * What a read of one of the network's tables brings back with all its columns: their names, as the language writes
 * them, and the rows, a cell for each column in the same order. A cell is text, read in whoever's language shows it:
 * a number or a name is data, a state or a kind is a word. Pure logic, so the language's tests read it.
 */
public record IqlTable(List<String> columns, List<List<Text>> rows) {

    /** No table: what a statement that reads nothing answers. */
    public static final IqlTable NONE = new IqlTable(List.of(), List.of());

    public IqlTable {
        columns = List.copyOf(columns);
        final List<List<Text>> copied = new ArrayList<>(rows.size());
        for (final List<Text> row : rows) {
            copied.add(List.copyOf(row));
        }
        rows = List.copyOf(copied);
    }

    /** Whether there is no table at all, as opposed to a table with no rows. */
    public boolean isNone() {
        return columns.isEmpty();
    }

    /** The cell of {@code row} under {@code column}, or empty text where there is none. */
    public Text cell(final int row, final String column) {
        final int at = columns.indexOf(column);
        if (row < 0 || row >= rows.size() || at < 0 || at >= rows.get(row).size()) {
            return Text.EMPTY;
        }
        return rows.get(row).get(at);
    }
}
