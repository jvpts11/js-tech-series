/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * What a tab of the Computational Research Centre's own draws on its enamel face, and how it answers the mouse. The
 * points it is given are the screen's, from its top left.
 */
interface ICrcTab {

    /** Draws it, under the items in the slots. */
    void draw(CrcView view);

    /** Answers a click of mouse {@code button} at ({@code x}, {@code y}); whether it took it. */
    default boolean clicked(final CrcView view, final double x, final double y, final int button) {
        return false;
    }

    /** Answers the wheel turned by {@code amount} at ({@code x}, {@code y}); whether it took it. */
    default boolean scrolled(final CrcView view, final double x, final double y, final double amount) {
        return false;
    }

    /**
     * Answers the mouse dragged with its left button held, {@code across} and {@code down} since the last time, now
     * at ({@code x}, {@code y}); whether it took it.
     */
    default boolean dragged(final CrcView view, final double x, final double y, final double across,
                            final double down) {
        return false;
    }

    /** What the mouse at ({@code x}, {@code y}) is told; nothing unless it says. */
    default List<Component> tooltip(final CrcView view, final double x, final double y) {
        return List.of();
    }
}
