/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A mod's mark on its settings screen: a small drawing in one colour, in the square at the header's left, so the
 * screen says whose settings it holds at a glance. A mod hands its own to {@link CoreConfigScreen}; one that hands none
 * gets {@link #CHIP}.
 */
@FunctionalInterface
public interface IConfigBadge {

    /** A chip with its pins: the Core's mark, and any mod's that names none of its own. */
    IConfigBadge CHIP = (g, x, y, size, colour) -> {
        final int body = size / 2;
        final int left = x + (size - body) / 2;
        final int top = y + (size - body) / 2;
        g.fill(left, top, left + body, top + body, colour);
        for (int pin = 1; pin < body; pin += 2) {
            g.fill(left + pin, top - 2, left + pin + 1, top - 1, colour);
            g.fill(left + pin, top + body + 1, left + pin + 1, top + body + 2, colour);
            g.fill(left - 2, top + pin, left - 1, top + pin + 1, colour);
            g.fill(left + body + 1, top + pin, left + body + 2, top + pin + 1, colour);
        }
    };

    /** Draws the mark in a square {@code size} on a side with its top left at ({@code x}, {@code y}). */
    void draw(GuiGraphics g, int x, int y, int size, int colour);
}
