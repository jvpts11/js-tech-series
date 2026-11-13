/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import dev.jstech.core.guide.ManualReader;
import java.util.List;
import java.util.Optional;

/** The search step both help viewers share: which hit to show next. */
final class HelpSearch {

    private HelpSearch() {
    }

    /**
     * The first hit that comes after the entry being read, going round to the first hit when none does.
     *
     * @param order  every entry of the manual, in reading order
     * @param found  the entries that matched, not empty
     * @param hereId the entry being read, or null when the page shown is not an entry
     */
    static String nextHit(final List<String> order, final List<String> found, final String hereId) {
        return nextAfter(order, found, hereId == null ? -1 : order.indexOf(hereId));
    }

    /** The same, from the place {@code here} in the reading order: the first hit past it, or round to the first. */
    static String nextAfter(final List<String> order, final List<String> found, final int here) {
        for (final String entry : found) {
            if (order.indexOf(entry) > here) {
                return entry;
            }
        }
        return found.getFirst();
    }

    /**
     * Where the page {@code id} of {@code reader} stands in its reading order: an entry at its own place, a chapter or
     * a section just before the first entry inside it, so the hits inside come next; -1 for any other page.
     */
    static int placeOf(final ManualReader reader, final String id) {
        final Optional<ManualReader.Node> node = reader.node(id);
        final List<String> order = reader.entries();
        if (node.isEmpty()) {
            return -1;
        }
        if (node.get().kind() == ManualReader.Kind.ENTRY) {
            return order.indexOf(id);
        }
        final String inside = node.get().number() + ".";
        for (int at = 0; at < order.size(); at++) {
            if (reader.node(order.get(at)).map(ManualReader.Node::number).orElse("").startsWith(inside)) {
                return at - 1;
            }
        }
        return -1;
    }
}
