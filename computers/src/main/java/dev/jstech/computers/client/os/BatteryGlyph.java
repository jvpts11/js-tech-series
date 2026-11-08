/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Draw;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The battery a system draws for its UPS: a cell lying on its side with its nub, filled as far as it is charged. The
 * notification area draws it small, beside the network; Frames' notice and Plasma's draw it larger, as their mark.
 */
final class BatteryGlyph {

    private static final int FULL = 100;

    private BatteryGlyph() {
    }

    /**
     * Draws it {@code w} by {@code h} at {@code (x, y)}, the nub included: its edge in {@code ink}, its charge of
     * {@code percent} in {@code fill}.
     */
    static void draw(final GuiGraphics g, final int x, final int y, final int w, final int h, final int percent,
                     final int ink, final int fill) {
        final int body = w - 1;
        Draw.outline(g, x, y, body, h, ink);
        final int nub = Math.max(1, h / 3);
        g.fill(x + body, y + nub, x + w, y + h - nub, ink);
        final int inner = body - 2;
        final int charged = inner * Math.max(0, Math.min(FULL, percent)) / FULL;
        if (charged > 0) {
            g.fill(x + 1, y + 1, x + 1 + charged, y + h - 1, fill);
        }
    }
}
