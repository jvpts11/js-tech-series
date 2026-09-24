/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import dev.jstech.computers.program.cli.ICliComputer;
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
     * The rows that match, in the order asked for.
     *
     * @param rows what the network holds
     * @param text what the player is looking for; empty keeps everything
     * @param sort {@code count} for most first, anything else for by name
     */
    public static List<ICliComputer.StoredItem> filtered(final List<ICliComputer.StoredItem> rows,
                                                         final String text, final String sort) {
        final String wanted = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        final List<ICliComputer.StoredItem> kept = new ArrayList<>();
        for (final ICliComputer.StoredItem row : rows) {
            if (wanted.isEmpty() || row.name().english().toLowerCase(Locale.ROOT).contains(wanted)) {
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
