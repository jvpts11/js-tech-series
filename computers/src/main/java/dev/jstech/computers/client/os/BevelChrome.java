/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Frames 95: grey chrome in two-tone relief, a title bar in a navy gradient, square corners. Its colours are the
 * system colours of the desktop it imitates, {@code jsc:chrome/bevel}.
 */
@PaletteHolder
final class BevelChrome implements IFormChrome {

    private final Palette<Colours> palette;

    static final BevelChrome INSTANCE = new BevelChrome(Palettes.declare(JsComputers.MODID, "chrome/bevel",
            new Colours(0xFFC0C0C0, 0xFFFFFFFF, 0xFFDFDFDF, 0xFF808080, 0xFF000000,
                    0xFF000080, 0xFF1084D0, 0xFF7F7F7F, 0xFFB0B0B0, 0xFF000000, 0xFF000000)));

    private BevelChrome(final Palette<Colours> palette) {
        this.palette = palette;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.BEVEL;
    }

    @Override
    public int edge(final OsSkin skin) {
        return this.palette.get().shadow();
    }

    @Override
    public int panelFill(final OsSkin skin) {
        return this.palette.get().face();
    }

    @Override
    public void titleBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right) {
        final Colours c = this.palette.get();
        if (active) {
            // Classic active-title gradient: deep navy on the left brightening to blue on the right.
            ChromeShapes.hGradient(g, x, y, w, h, c.titleFrom(), c.titleTo());
        } else {
            ChromeShapes.hGradient(g, x, y, w, h, c.idleTitleFrom(), c.idleTitleTo());
        }
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + bw, y + bh, c.face());
        relief(g, c, x, y, bw, bh, !pressed);
        final int nudge = pressed ? 1 : 0;
        ChromeShapes.glyph(g, font, control, x + nudge, y + nudge, bw, bh, c.glyph());
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, c.face());
        relief(g, c, x, y, w, h, false);
    }

    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, c.face());
        relief(g, c, x, y, w, h, !pressed);
        // The default (primary) button carries the classic dotted focus rectangle just inside its face.
        if (primary && !pressed) {
            ChromeShapes.dottedRect(g, x + 3, y + 3, w - 6, h - 6, c.focus());
        }
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        relief(g, this.palette.get(), x, y, w, h, false);
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        final Colours c = this.palette.get();
        final int tall = h + (active ? 2 : 0);
        g.fill(x, y, x + w, y + tall, c.face());
        relief(g, c, x, y, w, tall, true);
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, c.face());
        relief(g, c, x, y, w, h, true);
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, c.face());
        // A status bar is not a raised box: only a shadow line along its top, with a lit line under it.
        g.fill(x, y, x + w, y + 1, c.shadow());
        g.fill(x, y + 1, x + w, y + 2, c.highlight());
    }

    /** A subtler shadow than the later desktops: this one sat flat on its wallpaper. */
    @Override
    public int shadowSpread() {
        return 3;
    }

    @Override
    public int shadowStrength() {
        return 0x0E;
    }

    private static void relief(final GuiGraphics g, final Colours c, final int x, final int y, final int w,
                               final int h, final boolean raised) {
        ChromeShapes.bevelDouble(g, x, y, w, h, raised, c.highlight(), c.light(), c.shadow(), c.darkShadow());
    }

    /**
     * The colours of Frames 95's chrome.
     *
     * @param face          the grey every control is made of
     * @param highlight     the outer lit edge of a raised thing
     * @param light         its inner lit edge
     * @param shadow        its inner shaded edge, and the separators
     * @param darkShadow    its outer shaded edge
     * @param titleFrom     the left end of the active title's gradient
     * @param titleTo       its right end
     * @param idleTitleFrom the left end of an idle title's gradient
     * @param idleTitleTo   its right end
     * @param glyph         the marks on a title bar's buttons
     * @param focus         the dotted ring inside the default button
     */
    public record Colours(int face, int highlight, int light, int shadow, int darkShadow, int titleFrom,
                          int titleTo, int idleTitleFrom, int idleTitleTo, int glyph, int focus) {
    }
}
