/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

/**
 * How big what a leaf shows wants to be, given the widest it may be: a text that wraps answers taller when it is
 * given less room, an item answers 16 by 16 whatever it is given.
 */
@FunctionalInterface
public interface IMeasure {

    /** Nothing to show. */
    IMeasure NOTHING = maxWidth -> Size.ZERO;

    /** The size wanted with at most {@code maxWidth} pixels across. */
    Size measure(int maxWidth);

    /** Always the same size. */
    static IMeasure fixed(final int width, final int height) {
        final Size size = new Size(width, height);
        return maxWidth -> size;
    }
}
