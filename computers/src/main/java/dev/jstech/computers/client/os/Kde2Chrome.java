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
 * KDE of the early 2000s: a title in a vertical blue gradient, single-pixel borders, soft greys, and small pale
 * studs for the title buttons rather than bevelled blocks. Its colours are {@code jsc:chrome/kde2}.
 */
@PaletteHolder
final class Kde2Chrome implements IFormChrome {

    private final Palette<Colours> palette;

    static final Kde2Chrome INSTANCE = new Kde2Chrome(Palettes.declare(JsComputers.MODID, "chrome/kde2",
            new Colours(0xFF8B857E, 0xFFD6D2CD, 0xFF6F6A64,
                    0xFFB4B0AA, 0xFF98938C, 0xFF938E87, 0xFF7E7972,
                    0xFF6F9FD0, 0xFF33679F, 0xFF2E5F95, 0xFF1D4C80,
                    0xFFFFFFFF, 0xFFF2F1EF, 0xFFC9C4BE, 0xFFF4F2EF, 0xFFCEC9C2, 0xFF17324F, 0xFF1D4C80,
                    0xFFDCD8D2, 0xFFC2BDB6, 0xFFE2DED8, 0xFFBFBAB3)));

    private Kde2Chrome(final Palette<Colours> palette) {
        this.palette = palette;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.KDE2;
    }

    @Override
    public int edge(final OsSkin skin) {
        return this.palette.get().edge();
    }

    @Override
    public int panelFill(final OsSkin skin) {
        return this.palette.get().panel();
    }

    @Override
    public void titleBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right) {
        final Colours c = this.palette.get();
        // Three stops, a light crown over a dark base, closed by a border line. A window behind keeps its
        // gradient and loses its colour, as an inactive window of that age did.
        if (active) {
            g.fillGradient(x, y, x + w, y + h / 2, c.crownFrom(), c.crownTo());
            g.fillGradient(x, y + h / 2, x + w, y + h, c.baseFrom(), c.baseTo());
        } else {
            g.fillGradient(x, y, x + w, y + h / 2, c.idleCrownFrom(), c.idleCrownTo());
            g.fillGradient(x, y + h / 2, x + w, y + h, c.idleBaseFrom(), c.idleBaseTo());
        }
        g.fill(x, y + h - 1, x + w, y + h, c.border());
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        if (pressed) {
            g.fillGradient(x, y, x + bw, y + bh, c.studShade(), c.studLight());
        } else {
            g.fillGradient(x, y, x + bw, y + bh, hovered ? c.hover() : c.studLight(), c.studShade());
        }
        ChromeShapes.outline(g, x, y, bw, bh, c.border());
        final int nudge = pressed ? 1 : 0;
        ChromeShapes.glyph(g, font, control, x + nudge, y + nudge, bw, bh, c.glyph());
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, c.panel());
        ChromeShapes.outline(g, x, y, w, h, c.edge());
    }

    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        final Colours c = this.palette.get();
        if (pressed) {
            g.fillGradient(x, y, x + w, y + h, c.studShade(), c.light());
        } else {
            g.fillGradient(x, y, x + w, y + h, hovered ? c.hover() : c.light(), c.shade());
        }
        ChromeShapes.outline(g, x, y, w, h, primary ? c.focus() : c.edge());
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        final Colours c = this.palette.get();
        ChromeShapes.outline(g, x, y, w, h, focused ? c.focus() : c.edge());
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        final Colours c = this.palette.get();
        if (active) {
            g.fillGradient(x, y, x + w, y + h, c.light(), c.panel());
        } else {
            g.fillGradient(x, y, x + w, y + h, c.idleTabFrom(), c.idleTabTo());
        }
        ChromeShapes.outline(g, x, y, w, h, c.edge());
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final Colours c = this.palette.get();
        g.fillGradient(x, y, x + w, y + h, c.light(), c.shade());
        ChromeShapes.outline(g, x, y, w, h, c.edge());
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final Colours c = this.palette.get();
        g.fillGradient(x, y, x + w, y + h, c.statusFrom(), c.statusTo());
        g.fill(x, y, x + w, y + 1, c.border());
    }

    /**
     * The colours of the period KDE chrome.
     *
     * @param edge          separators, and the rim of a panel, a button, a field and a tab
     * @param panel         a content panel, and the foot of the tab in front
     * @param border        the line under a title, round a title button, and over a status bar
     * @param idleCrownFrom the top of an idle title's upper half
     * @param idleCrownTo   the bottom of that half
     * @param idleBaseFrom  the top of its lower half
     * @param idleBaseTo    the bottom of that half
     * @param crownFrom     the top of the active title's upper half
     * @param crownTo       the bottom of that half
     * @param baseFrom      the top of its lower half
     * @param baseTo        the bottom of that half
     * @param hover         the lit top of a hovered button or stud
     * @param studLight     the top of a title button
     * @param studShade     its bottom, and the top of a pressed push button
     * @param light         the top of a push button, a thumb and the tab in front
     * @param shade         the bottom of a push button and a thumb
     * @param glyph         the marks on the title buttons
     * @param focus         the rim of the default button and of the field that has the keyboard
     * @param idleTabFrom   the top of a tab behind
     * @param idleTabTo     its bottom
     * @param statusFrom    the top of a status bar
     * @param statusTo      its bottom
     */
    public record Colours(int edge, int panel, int border,
                          int idleCrownFrom, int idleCrownTo, int idleBaseFrom, int idleBaseTo,
                          int crownFrom, int crownTo, int baseFrom, int baseTo,
                          int hover, int studLight, int studShade, int light, int shade, int glyph, int focus,
                          int idleTabFrom, int idleTabTo, int statusFrom, int statusTo) {
    }
}
