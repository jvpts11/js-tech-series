/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What a table shows: its columns, its rows as they come, and the rows as the player sorted and filtered them. A click
 * on a column's head sorts by it, a second the other way round; words typed in the filter keep the rows any of whose
 * cells holds them, and a check of the owner's own keeps the rows it passes.
 *
 * @param <R> a row
 */
public final class TableModel<R> {

    private final List<Column<R>> columns;
    private final Supplier<List<R>> rows;
    private int sortColumn = -1;
    private boolean descending;
    private String filter = "";
    private Predicate<R> keep = row -> true;

    public TableModel(final List<Column<R>> columns, final Supplier<List<R>> rows) {
        this.columns = List.copyOf(columns);
        this.rows = Objects.requireNonNull(rows, "rows");
    }

    public List<Column<R>> columns() {
        return columns;
    }

    /** Sorts by the column at {@code index}: the other way round when it already sorts by it. */
    public void sortBy(final int index) {
        if (index < 0 || index >= columns.size() || columns.get(index).order() == null) {
            return;
        }
        if (sortColumn == index) {
            descending = !descending;
        } else {
            sortColumn = index;
            descending = columns.get(index).descendingFirst();
        }
    }

    /** The column the rows are sorted by, or -1. */
    public int sortColumn() {
        return sortColumn;
    }

    /** Whether the rows run from the largest down. */
    public boolean descending() {
        return descending;
    }

    /** Keeps only the rows any of whose cells holds {@code words}, case aside; empty keeps them all. */
    public void filter(final String words) {
        filter = words == null ? "" : words.trim().toLowerCase(Locale.ROOT);
    }

    /** Keeps only the rows {@code check} passes, beside the filter's words. */
    public void keep(final Predicate<R> check) {
        keep = Objects.requireNonNull(check, "check");
    }

    /** The rows as shown: filtered, then sorted. */
    public List<R> shown() {
        final List<R> out = new ArrayList<>();
        for (final R row : rows.get()) {
            if (keep.test(row) && matches(row)) {
                out.add(row);
            }
        }
        if (sortColumn >= 0) {
            final Comparator<R> order = columns.get(sortColumn).order();
            out.sort(descending ? order.reversed() : order);
        }
        return out;
    }

    private boolean matches(final R row) {
        if (filter.isEmpty()) {
            return true;
        }
        for (final Column<R> column : columns) {
            if (column.text().apply(row).toLowerCase(Locale.ROOT).contains(filter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * One column.
     *
     * @param title          its head
     * @param width          its width in pixels
     * @param text           what a row's cell of it says
     * @param order          how rows sort by it, or null when they cannot
     * @param right          whether its cells stand at the right, as numbers do
     * @param descendingFirst whether a first click sorts it from the largest down, as amounts are
     * @param <R>            a row
     */
    public record Column<R>(String title, int width, Function<R, String> text, Comparator<R> order, boolean right,
                            boolean descendingFirst) {

        /** A column of words, sorted alphabetically. */
        public static <R> Column<R> words(final String title, final int width, final Function<R, String> text) {
            return new Column<>(title, width, text, Comparator.comparing(text, String.CASE_INSENSITIVE_ORDER), false,
                    false);
        }

        /** A column of amounts at the right, sorted by {@code amount} from the largest down at first. */
        public static <R> Column<R> amounts(final String title, final int width, final Function<R, String> text,
                                            final Function<R, Double> amount) {
            return new Column<>(title, width, text, Comparator.comparing(amount), true, true);
        }
    }
}
