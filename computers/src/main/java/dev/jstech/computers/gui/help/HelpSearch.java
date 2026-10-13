/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import java.util.List;

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
        final int here = hereId == null ? -1 : order.indexOf(hereId);
        for (final String entry : found) {
            if (order.indexOf(entry) > here) {
                return entry;
            }
        }
        return found.getFirst();
    }
}
