/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IIqlCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Narrowing and ordering a listing of what a network holds.
 *
 * <p>The rows come from the machine already; what a player asked for of them is a matter of text and numbers,
 * and is worked out here so it can be held to account without a network. The same narrowing serves the command
 * and, later, the search box of the full-screen view, so a search means one thing in both. It reads a name in
 * English, the machine's language, which is the one side it knows.
 */
public final class InteracRows {

    private InteracRows() {
    }

    /**
     * Whether {@code candidate} answers to what is being searched for: the text, trimmed, anywhere in it and
     * without regard to case. An empty search keeps everything. Every list a search narrows uses this one
     * rule, so the command and the full-screen view never disagree about what a search finds.
     */
    public static boolean matches(final String candidate, final String search) {
        final String wanted = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return wanted.isEmpty() || candidate.toLowerCase(Locale.ROOT).contains(wanted);
    }

    /**
     * What the network holds that matches a search, most first or by name, at most {@code cap} rows.
     *
     * <p>The search goes into the query itself, so the cap is applied to what matched and an item far down a
     * big network can still be found by typing its name, rather than being cut off before the text is looked at.
     */
    public static List<ICliComputer.StoredItem> search(final ICliComputer computer, final String text,
                                                       final String sort, final int cap) {
        final String wanted = text == null ? "" : text.trim();
        final IIqlCondition where = wanted.isEmpty()
                ? null
                : new IIqlCondition.Comparison("name", IIqlCondition.Op.CONTAINS, wanted);
        return filtered(computer.query(where, "", cap), wanted, sort);
    }

    /**
     * The rows that match, in the order asked for.
     *
     * @param rows what the network holds
     * @param text what the player is looking for; empty keeps everything
     * @param sort {@code count} for most first, anything else for by name
     */
    public static List<ICliComputer.StoredItem> filtered(final List<ICliComputer.StoredItem> rows,
                                                         final String text, final String sort) {
        final List<ICliComputer.StoredItem> kept = new ArrayList<>();
        for (final ICliComputer.StoredItem row : rows) {
            if (matches(row.name().english(), text)) {
                kept.add(row);
            }
        }
        if ("count".equalsIgnoreCase(sort)) {
            // Most first, and two of the same count read in the order a person would look for them.
            kept.sort((left, right) -> left.quantity() == right.quantity()
                    ? left.name().english().compareToIgnoreCase(right.name().english())
                    : Long.compare(right.quantity(), left.quantity()));
        } else {
            kept.sort((left, right) -> left.name().english().compareToIgnoreCase(right.name().english()));
        }
        return kept;
    }
}
