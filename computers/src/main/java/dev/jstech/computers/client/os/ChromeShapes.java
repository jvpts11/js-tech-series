/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The shapes window chrome is built from, in whatever colours they are handed: rounded fills, outlines, relief,
 * gradients and the marks on a window's buttons. None of them knows a colour of its own, so every form draws with
 * them in its palette's.
 */
final class ChromeShapes {

    /** Full alpha, laid over a colour worked out from its red, green and blue. */
    private static final int OPAQUE = 0xFF << 24;

    private ChromeShapes() {
    }

    /** A filled rectangle whose TOP corners are rounded by {@code rTop}px and BOTTOM by {@code rBottom}px. */
    static void roundedRect(final GuiGraphics g, final int x, final int y, final int w, final int h,
                            final int color, final int rTop, final int rBottom) {
        final int top = Math.max(0, rTop);
        final int bottom = Math.max(0, rBottom);
        g.fill(x, y + top, x + w, y + h - bottom, color);
        for (int i = 0; i < top; i++) {
            final int inset = top - i;
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
        for (int i = 0; i < bottom; i++) {
            final int inset = bottom - i;
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /**
     * The two-tone 3D border of the classic desktops: an outer ring over an inner one, giving a raised button or a
     * sunken well that a single 1px bevel only approximates. Raised, the outer ring is lit top-left with
     * {@code highlight} and shaded bottom-right with {@code darkShadow}, the inner with {@code light} and
     * {@code shadow}; sunken, each ring turns the other way round.
     */
    static void bevelDouble(final GuiGraphics g, final int x, final int y, final int w, final int h,
                            final boolean raised, final int highlight, final int light, final int shadow,
                            final int darkShadow) {
        final int outerLight = raised ? highlight : shadow;
        final int outerDark = raised ? darkShadow : highlight;
        final int innerLight = raised ? light : darkShadow;
        final int innerDark = raised ? shadow : light;
        // Outer ring.
        g.fill(x, y, x + w, y + 1, outerLight);
        g.fill(x, y, x + 1, y + h, outerLight);
        g.fill(x, y + h - 1, x + w, y + h, outerDark);
        g.fill(x + w - 1, y, x + w, y + h, outerDark);
        // Inner ring, inset by one pixel.
        g.fill(x + 1, y + 1, x + w - 1, y + 2, innerLight);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, innerLight);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, innerDark);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, innerDark);
    }

    /** A left-to-right gradient fill (GuiGraphics.fillGradient is vertical only), stepped in 2px columns. */
    static void hGradient(final GuiGraphics g, final int x, final int y, final int w, final int h,
                          final int left, final int right) {
        final int lr = left >> 16 & 0xFF;
        final int lg = left >> 8 & 0xFF;
        final int lb = left & 0xFF;
        final int rr = right >> 16 & 0xFF;
        final int rg = right >> 8 & 0xFF;
        final int rb = right & 0xFF;
        for (int i = 0; i < w; i += 2) {
            final float t = w <= 1 ? 0f : i / (float) (w - 1);
            final int cr = (int) (lr + (rr - lr) * t);
            final int cg = (int) (lg + (rg - lg) * t);
            final int cb = (int) (lb + (rb - lb) * t);
            g.fill(x + i, y, x + Math.min(w, i + 2), y + h, OPAQUE | cr << 16 | cg << 8 | cb);
        }
    }

    /** A 1px outline of a single colour. */
    static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h, final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** A 1px outline that skips the corner pixels, to match a rounded fill of radius {@code r}. */
    static void roundedOutline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                               final int color, final int r) {
        g.fill(x + r, y, x + w - r, y + 1, color);
        g.fill(x + r, y + h - 1, x + w - r, y + h, color);
        g.fill(x, y + r, x + 1, y + h - r, color);
        g.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }

    /** A 1px dotted rectangle (every other pixel), the focus ring of the classic desktops. */
    static void dottedRect(final GuiGraphics g, final int x, final int y, final int w, final int h,
                           final int color) {
        for (int i = 0; i < w; i += 2) {
            g.fill(x + i, y, x + i + 1, y + 1, color);
            g.fill(x + i, y + h - 1, x + i + 1, y + h, color);
        }
        for (int i = 0; i < h; i += 2) {
            g.fill(x, y + i, x + 1, y + i + 1, color);
            g.fill(x + w - 1, y + i, x + w, y + i + 1, color);
        }
    }

    /** The mark on a window's button, centred on it: a bar, a box, two boxes or a cross. */
    static void glyph(final GuiGraphics g, final Font font, final OsSkin.Control control, final int x, final int y,
                      final int bw, final int bh, final int color) {
        final String s = switch (control) {
            case MINIMIZE -> "_";
            case MAXIMIZE -> "□";
            case RESTORE -> "❐";
            case CLOSE -> "✕";
        };
        g.drawString(font, s, x + (bw - font.width(s)) / 2, y + (bh - 7) / 2, color, false);
    }

    /** The colour a little lighter, for the lit face of a hovered button. */
    static int lighten(final int argb) {
        final int r = Math.min(255, (argb >> 16 & 0xFF) + 24);
        final int gg = Math.min(255, (argb >> 8 & 0xFF) + 24);
        final int b = Math.min(255, (argb & 0xFF) + 24);
        return OPAQUE | r << 16 | gg << 8 | b;
    }

    /** The colour a little darker, for the pressed face and the rim of a primary button. */
    static int darken(final int argb) {
        final int r = Math.max(0, (argb >> 16 & 0xFF) - 28);
        final int gg = Math.max(0, (argb >> 8 & 0xFF) - 28);
        final int b = Math.max(0, (argb & 0xFF) - 28);
        return OPAQUE | r << 16 | gg << 8 | b;
    }
}
