/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * How Motif draws things, which is how CDE looks: everything is the same grey, and what is raised or sunken
 * is said by two lines of light and two of shade.
 *
 * <p>There are no gradients, no outlines and no rounded corners in it. A button is a slab lit from the top
 * left; pressed, the light and the shade change places. A well is the same trick inside out, on a slightly
 * different grey. A window's frame is a thick raised border, and the window in front is told apart by one
 * colour, the palette's active one, under its title. Every colour comes from the palette it is handed, which
 * is what lets the Style Manager change the whole desktop by changing one thing.
 */
final class MotifChrome {

    /** How wide the relief of a frame is, which is what makes a Motif window look like a carved block. */
    static final int FRAME = 3;

    /** The diagonal hatch CDE's backdrop wears until another is chosen, and the side of its tile. */
    private static final ResourceLocation HATCH =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/cde/backdrop_hatch.png");
    private static final int TILE = 8;

    private MotifChrome() {
    }

    /** A slab standing out of the grey, filled with {@code fill}. */
    static void raised(final GuiGraphics g, final int x, final int y, final int w, final int h, final int fill,
                       final CdePalette p) {
        g.fill(x, y, x + w, y + h, fill);
        relief(g, x, y, w, h, p.light(), p.shade());
    }

    /** The same slab pushed in: a well, a pressed button, the workspace that is up. */
    static void sunken(final GuiGraphics g, final int x, final int y, final int w, final int h, final int fill,
                       final CdePalette p) {
        g.fill(x, y, x + w, y + h, fill);
        relief(g, x, y, w, h, p.shade(), p.light());
    }

    /** The frame of a window: a thick raised border, with a sunken line inside it where the client begins. */
    static void windowFrame(final GuiGraphics g, final int x, final int y, final int w, final int h,
                            final CdePalette p) {
        raised(g, x - FRAME, y - FRAME, w + FRAME * 2, h + FRAME * 2, p.window(), p);
        g.fill(x, y, x + w, y + h, p.window());
    }

    /**
     * The bar across the head of a window. The buttons keep the frame's grey; only the strip the title is
     * written on takes the active colour, and it is a raised strip like everything else.
     *
     * @param left  how much of the bar the button at its left end takes
     * @param right how much the buttons at its right end take
     */
    static void titleBar(final GuiGraphics g, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right, final CdePalette p) {
        g.fill(x, y, x + w, y + h, p.window());
        raised(g, x + left, y + 1, w - left - right, h - 2, active ? p.active() : p.window(), p);
    }

    /** One of the buttons on that bar, with its mark: a bar for the menu, a dot to minimise, a square to grow. */
    static void control(final GuiGraphics g, final int x, final int y, final int bw, final int bh,
                        final OsSkin.Control control, final boolean pressed, final CdePalette p) {
        if (pressed) {
            sunken(g, x, y, bw, bh, p.window(), p);
        } else {
            raised(g, x, y, bw, bh, p.window(), p);
        }
        final int cx = x + bw / 2;
        final int cy = y + bh / 2;
        switch (control) {
            case CLOSE -> raised(g, x + 2, cy - 1, bw - 4, 3, p.window(), p);
            case MINIMIZE -> raised(g, cx - 1, cy - 1, 3, 3, p.window(), p);
            case MAXIMIZE -> raised(g, x + 2, y + 2, bw - 4, bh - 4, p.window(), p);
            case RESTORE -> raised(g, x + 3, y + 3, bw - 6, bh - 6, p.window(), p);
        }
    }

    /** A push button. The default one stands inside a second, sunken ring, which is Motif's way of saying so. */
    static void button(final GuiGraphics g, final int x, final int y, final int w, final int h,
                       final boolean pressed, final boolean primary, final CdePalette p) {
        if (primary) {
            relief(g, x - 2, y - 2, w + 4, h + 4, p.shade(), p.light());
        }
        if (pressed) {
            sunken(g, x, y, w, h, p.inset(), p);
        } else {
            raised(g, x, y, w, h, p.window(), p);
        }
    }

    /** A tab: raised, and the one in front a little taller so it joins the page under it. */
    static void tab(final GuiGraphics g, final int x, final int y, final int w, final int h, final boolean active,
                    final CdePalette p) {
        raised(g, x, y, w, h + (active ? 2 : 0), active ? p.window() : p.inset(), p);
    }

    /** The strip along the foot of a window: the frame's grey, with one line of shade over it. */
    static void statusBar(final GuiGraphics g, final int x, final int y, final int w, final int h,
                          final CdePalette p) {
        g.fill(x, y, x + w, y + h, p.window());
        g.fill(x, y, x + w, y + 1, p.shade());
        g.fill(x, y + 1, x + w, y + 2, p.light());
    }

    /**
     * The backdrop: the palette's first backdrop colour, with a pattern laid over it in the second.
     *
     * <p>A pattern is a small white mask tiled across the desktop in one draw and tinted, never a picture of
     * its own colours, so one tile serves every palette and a whole desktop of hatching costs what one
     * rectangle does.
     */
    static void backdrop(final GuiGraphics g, final int w, final int h, final CdePalette p) {
        g.fill(0, 0, w, h, p.backdropA());
        final int tint = p.backdropB();
        g.setColor((tint >> 16 & 0xFF) / 255.0F, (tint >> 8 & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, 1.0F);
        g.blit(HATCH, 0, 0, 0.0F, 0.0F, w, h, TILE, TILE);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void relief(final GuiGraphics g, final int x, final int y, final int w, final int h,
                               final int topLeft, final int bottomRight) {
        g.fill(x, y, x + w, y + 1, topLeft);
        g.fill(x, y, x + 1, y + h, topLeft);
        g.fill(x, y + h - 1, x + w, y + h, bottomRight);
        g.fill(x + w - 1, y, x + w, y + h, bottomRight);
    }
}
