/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.theme;

import dev.jstech.core.palette.Palette;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;

/**
 * One GUI skin: the {@link EraPalette} it is coloured with plus the {@link EraStyle} it is drawn in, with the
 * palette-coupled drawing helpers that used to be the {@code static} methods on {@code JsTechTheme}. A computing screen
 * resolves the theme of its host computer's {@code HardwareEra} and paints through it, so the same screen code renders
 * in the skin of whatever era it is running on.
 *
 * <p>The colours are asked for each time something is painted, never kept: a skin is usually coloured by a declared
 * {@link Palette}, which a resource pack can change while the screen is open.
 *
 * <p>The STANDARD theme's style is {@linkplain EraStyle#flat flat} (every overlay off), so its helpers execute the
 * identical {@code g.fill(...)} sequence the original {@code JsTechTheme} did, byte-for-byte the same pixels.
 */
public final class EraTheme {

    private final Supplier<EraPalette> colours;
    private final EraStyle s;

    public EraTheme(final Supplier<EraPalette> colours, final EraStyle style) {
        this.colours = colours;
        this.s = style;
    }

    /** The colours the skin paints with now. */
    public EraPalette palette() {
        return colours.get();
    }

    public EraStyle style() {
        return s;
    }

    // Semantic color accessors (these replace the former public static final int constants).

    public int outer() {
        return colours.get().outer();
    }

    public int screen() {
        return colours.get().screen();
    }

    public int rail() {
        return colours.get().rail();
    }

    public int panel() {
        return colours.get().panel();
    }

    public int line() {
        return colours.get().line();
    }

    public int track() {
        return colours.get().track();
    }

    public int slotBg() {
        return colours.get().slotBg();
    }

    public int slotEdge() {
        return colours.get().slotEdge();
    }

    public int accent() {
        return colours.get().accent();
    }

    public int accent2() {
        return colours.get().accent2();
    }

    public int green() {
        return colours.get().green();
    }

    public int amber() {
        return colours.get().amber();
    }

    public int red() {
        return colours.get().red();
    }

    public int text() {
        return colours.get().text();
    }

    public int dim() {
        return colours.get().dim();
    }

    public int tabOn() {
        return colours.get().tabOn();
    }

    public int tabLabelOn() {
        return colours.get().tabLabelOn();
    }

    public int hover() {
        return colours.get().hover();
    }

    public float small() {
        return s.fontScaleSmall();
    }

    // Palette-coupled drawing. With a flat style these are the exact sequences the original helpers ran.

    public void window(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final EraPalette p = colours.get();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, p.outer());
        g.fill(x, y, x + w, y + h, p.screen());
        /*
         * Double-bevel themes (e.g., Legacy) draw a raised 3D frame just inside the outer border,
         * giving the window the "dialog box" look authentic to the era.
         */
        bevel(g, p, x, y, w, h, true);
        if (s.scanlines() && p.scanline() != 0) {
            // Square, low-alpha 1px lines every 2px across the screen rect, a CRT scanline finish.
            for (int ly = y + 1; ly < y + h; ly += 2) {
                g.fill(x, ly, x + w, ly + 1, p.scanline());
            }
        }
    }

    public void slot(final GuiGraphics g, final int x, final int y) {
        final EraPalette p = colours.get();
        g.fill(x - 1, y - 1, x + 17, y + 17, p.slotEdge());
        g.fill(x, y, x + 16, y + 16, p.slotBg());
        bevel(g, p, x, y, 18, 18, true);
    }

    public void panel(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final EraPalette p = colours.get();
        g.fill(x, y, x + w, y + h, p.panel());
        g.fill(x, y, x + w, y + 1, p.line());
        // Panels use a sunken bevel (inverted edges) on double-bevel themes to convey a recessed display area.
        bevelSunken(g, p, x, y, w, h);
    }

    public void hLine(final GuiGraphics g, final int x, final int y, final int w) {
        g.fill(x, y, x + w, y + 1, colours.get().line());
    }

    public void vLine(final GuiGraphics g, final int x, final int y, final int h) {
        g.fill(x, y, x + 1, y + h, colours.get().line());
    }

    public void headerBar(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final EraPalette p = colours.get();
        g.fill(cx, cy, cx + cw, cy + 16, p.panel());
        g.fill(cx, cy + 16, cx + cw, cy + 17, p.line());
    }

    public void button(final GuiGraphics g, final int x, final int y, final int w, final int h,
                       final boolean hovered) {
        final EraPalette p = colours.get();
        g.fill(x, y, x + w, y + h, hovered ? p.hover() : p.panel());
        g.fill(x, y, x + w, y + 1, p.line());
        bevel(g, p, x, y, w, h, !hovered);
    }

    public void track(final GuiGraphics g, final int x, final int y, final int w,
                      final double frac, final int fillColor) {
        final EraPalette p = colours.get();
        g.fill(x, y, x + w, y + 7, p.track());
        g.fill(x, y, x + w, y + 1, p.line());
        final int fw = (int) Math.round((w - 2) * Math.max(0.0, Math.min(1.0, frac)));
        if (fw > 0) {
            g.fill(x + 1, y + 1, x + 1 + fw, y + 6, fillColor);
            if (s.glowAccent() && p.glow() != 0 && fillColor == p.accent()) {
                g.fill(x + 1, y, x + 1 + fw, y + 1, p.glow());
                g.fill(x + 1, y + 6, x + 1 + fw, y + 7, p.glow());
            }
        }
    }

    /**
     * Draws the optional raised double bevel on a rectangle: a 1px light top-left edge and a 1px dark
     * bottom-right edge. A no-op unless the style enables it and the palette gives the edges a colour, so
     * STANDARD never bevels and output is byte-identical to before.
     */
    private void bevel(final GuiGraphics g, final EraPalette p, final int x, final int y, final int w, final int h,
                       final boolean raised) {
        if (!s.doubleBevel() || !raised) {
            return;
        }
        if (p.bevelLight() != 0) {
            g.fill(x, y, x + w, y + 1, p.bevelLight());
            g.fill(x, y, x + 1, y + h, p.bevelLight());
        }
        if (p.bevelDark() != 0) {
            g.fill(x, y + h - 1, x + w, y + h, p.bevelDark());
            g.fill(x + w - 1, y, x + w, y + h, p.bevelDark());
        }
    }

    /**
     * Draws the optional sunken double bevel: dark top-left and light bottom-right, giving a recessed
     * appearance suitable for panels and display areas. A no-op on flat-style themes.
     */
    private void bevelSunken(final GuiGraphics g, final EraPalette p, final int x, final int y, final int w,
                             final int h) {
        if (!s.doubleBevel()) {
            return;
        }
        if (p.bevelDark() != 0) {
            g.fill(x, y, x + w, y + 1, p.bevelDark());
            g.fill(x, y, x + 1, y + h, p.bevelDark());
        }
        if (p.bevelLight() != 0) {
            g.fill(x, y + h - 1, x + w, y + h, p.bevelLight());
            g.fill(x + w - 1, y, x + w, y + h, p.bevelLight());
        }
    }
}
