/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.layout;

import org.jetbrains.annotations.ApiStatus;

/**
 * Which tabs of a row too wide for its strip are shown, as a Windows XP tab control scrolls them: the tabs from the
 * first shown, as many as fit, and two arrows at the strip's end that move the first one by one. The first shown never
 * moves past the one from which every tab left fits, so the row never ends short of the strip, and showing a tab moves
 * the row only as far as it takes. Pure, so a unit test checks it; the tab strip's own, and no promise.
 */
@ApiStatus.Internal
public final class TabScroll {

    private TabScroll() {
    }

    /** How many tabs of {@code widths}, from {@code first}, fit in {@code room}; always one at least. */
    public static int shown(final int[] widths, final int room, final int first) {
        int used = 0;
        int count = 0;
        for (int i = first; i < widths.length; i++) {
            used += widths[i];
            if (used > room && count > 0) {
                break;
            }
            count++;
        }
        return count;
    }

    /** The furthest tab the row may start at: the one from which every tab left fits in {@code room}. */
    public static int lastFirst(final int[] widths, final int room) {
        int used = 0;
        for (int i = widths.length - 1; i >= 0; i--) {
            used += widths[i];
            if (used > room) {
                return Math.min(widths.length - 1, i + 1);
            }
        }
        return 0;
    }

    /**
     * The first tab shown once tab {@code index} is shown, from {@code first}: the same when it already is, moved
     * left to it when it lies before, or right only as far as it takes when it lies after.
     */
    public static int showing(final int[] widths, final int room, final int first, final int index) {
        if (widths.length == 0) {
            return 0;
        }
        int start = Math.max(0, Math.min(first, lastFirst(widths, room)));
        if (index < start) {
            return Math.max(0, index);
        }
        while (index >= start + shown(widths, room, start) && start < index) {
            start++;
        }
        return Math.min(start, lastFirst(widths, room));
    }
}
