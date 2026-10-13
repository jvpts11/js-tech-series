/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.TaskbarGroups;
import java.util.List;

/**
 * Where every panel entry sits this frame, so the drawing and the hit tests share one reckoning.
 *
 * @param entries    the programs on the panel, in panel order
 * @param x          the left edge of each entry's button or cell
 * @param w          the width of each; zero for an entry the strip does not show (a Frames XP pin, which lives on
 *                   the quick launch instead)
 * @param quickX     where the Frames XP quick launch starts
 * @param quickCount how many icons the quick launch holds
 * @param right      where the strip must stop, clear of the notification area
 */
record TaskStrip(List<TaskbarGroups.Entry> entries, int[] x, int[] w, int quickX, int quickCount, int right) {

    /** The pitch of the Frames XP quick launch icons beside Start. */
    static final int QL_W = 16;

    /** The entry whose button or cell is under a desktop-local x, or -1. */
    int indexAt(final double mx) {
        for (int i = 0; i < entries.size(); i++) {
            if (w[i] > 0 && x[i] + w[i] <= right && mx >= x[i] && mx < x[i] + w[i]) {
                return i;
            }
        }
        return -1;
    }

    /** The place of entry {@code index} on the quick launch (its rank among the pinned), or -1. */
    int quickIndexOf(final int index) {
        if (quickCount == 0 || !entries.get(index).pinned()) {
            return -1;
        }
        int j = 0;
        for (int i = 0; i < index; i++) {
            if (entries.get(i).pinned()) {
                j++;
            }
        }
        return j;
    }

    /** The entry whose quick launch icon is under a desktop-local x, or -1. */
    int quickEntryAt(final double mx) {
        if (quickCount == 0 || mx < quickX || mx >= quickX + quickCount * QL_W) {
            return -1;
        }
        final int wanted = (int) ((mx - quickX) / QL_W);
        int j = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).pinned()) {
                if (j == wanted) {
                    return i;
                }
                j++;
            }
        }
        return -1;
    }
}
