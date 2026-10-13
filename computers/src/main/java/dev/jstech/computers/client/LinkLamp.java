/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.client.gui.theme.JsTechTheme;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The lamp of a device's link at the right end of its window's header, as the bus windows have it: a lit square in a
 * dark bezel, green while a computer is linked and red while none is, with a lighter corner so it reads as a lens.
 */
public final class LinkLamp {

    /** How much brighter the lens's corner is than the lamp's colour. */
    private static final int LENS_LIFT = 70;

    private LinkLamp() {
    }

    public static void draw(final GuiGraphics g, final int x, final int y, final int size, final boolean linked) {
        final int colour = linked ? JsTechTheme.green() : JsTechTheme.red();
        g.fill(x, y, x + size, y + size, JsTechTheme.outer());
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, colour);
        final int r = Math.min(255, ((colour >> 16) & 0xFF) + LENS_LIFT);
        final int gr = Math.min(255, ((colour >> 8) & 0xFF) + LENS_LIFT);
        final int b = Math.min(255, (colour & 0xFF) + LENS_LIFT);
        g.fill(x + 1, y + 1, x + 2, y + 2, (colour >>> 24) << 24 | r << 16 | gr << 8 | b);
    }
}
