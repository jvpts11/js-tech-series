/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

/**
 * Which of a test's reference pictures its page shows, two at a time from the one chosen, and which picture a click
 * on a shown row means. Pure.
 */
final class PictureRows {

    /** How many pictures the page shows at once. */
    static final int SHOWN = 2;

    private PictureRows() {
    }

    /** The first picture shown of {@code count}, while picture {@code chosen} is the one chosen. */
    static int first(final int chosen, final int count) {
        return Math.max(0, Math.min(chosen, count - SHOWN));
    }

    /** The picture shown on row {@code row} counted from the top, or -1 when none is shown there. */
    static int at(final int chosen, final int count, final int row) {
        final int index = first(chosen, count) + row;
        return row >= 0 && row < SHOWN && index < count ? index : -1;
    }
}
