/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * KDE 4 with its Oxygen windows: a soft grey title that fades into the window's own grey, the title centred on it,
 * round title buttons lit from above with a red mark on the close, and the top corners rounded. Its colours are
 * {@code jsc:chrome/oxygen}.
 */
@PaletteHolder
final class OxygenChrome implements IFormChrome {

    private final Palette<Colours> palette;

    static final OxygenChrome INSTANCE = new OxygenChrome(Palettes.declare(JsComputers.MODID, "chrome/oxygen",
            new Colours(0xFFBCBAB8, 0xFFE0DFDE,
                    0xFFF2F1F0, 0xFFD9D7D5, 0xFFEAE9E8, 0xFFDDDCDB,
                    0xFFFFFFFF, 0xFFC9C7C5, 0xFFA4A2A0, 0xFF444444, 0xFFB72D2D,
                    0xFFF7F6F5, 0xFFDAD8D6, 0xFFA8A6A4, 0xFF4C93DC, 0xFFEFEEED,
                    0xFFE8E7E6, 0xFFD3D1CF, 0xFFE9E8E7, 0xFFD6D5D4, 0xFFCFCDCB)));

    private OxygenChrome(final Palette<Colours> palette) {
        this.palette = palette;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.OXYGEN;
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
        final int r = skin.topRadius();
        final int top = active ? c.titleTop() : c.idleTop();
        final int bottom = active ? c.titleBottom() : c.idleBottom();
        // Rounded at the top, the grey falling into the window's own so the title and the body read as one piece.
        for (int i = 0; i < r; i++) {
            final int in = r - i;
            g.fill(x + in, y + i, x + w - in, y + i + 1, top);
        }
        ChromeShapes.vGradient(g, x, y + r, x + w, y + h, top, bottom);
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        final int d = Math.min(bw, bh) - 1;
        final int dx = x + (bw - d) / 2;
        final int dy = y + (bh - d) / 2;
        final int lit = hovered ? c.buttonLit() : c.buttonTop();
        ChromeShapes.disc(g, dx, dy, d, pressed ? c.buttonBottom() : lit, pressed ? lit : c.buttonBottom(),
                c.buttonRim());
        final int nudge = pressed ? 1 : 0;
        ChromeShapes.glyph(g, font, control, dx + nudge, dy + nudge, d, d,
                control == OsSkin.Control.CLOSE ? c.closeGlyph() : c.glyph());
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        final Colours c = this.palette.get();
        Grounds.fill(g, x, y, x + w, y + h, c.panel());
        ChromeShapes.outline(g, x, y, w, h, c.edge());
    }

    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        final Colours c = this.palette.get();
        if (pressed) {
            ChromeShapes.vGradient(g, x, y, x + w, y + h, c.pushBottom(), c.pushTop());
        } else {
            ChromeShapes.vGradient(g, x, y, x + w, y + h, hovered ? c.buttonLit() : c.pushTop(), c.pushBottom());
        }
        // Oxygen's buttons are softly rounded: the corner pixel of each side is left to the ground.
        ChromeShapes.roundedOutline(g, x, y, w, h, primary || hovered ? c.focus() : c.pushRim(), 1);
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        final Colours c = this.palette.get();
        ChromeShapes.roundedOutline(g, x, y, w, h, focused ? c.focus() : c.edge(), 1);
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        final Colours c = this.palette.get();
        if (active) {
            ChromeShapes.vGradient(g, x, y, x + w, y + h, c.pushTop(), c.panel());
        } else {
            ChromeShapes.vGradient(g, x, y, x + w, y + h, c.idleTabTop(), c.idleTabBottom());
        }
        ChromeShapes.roundedOutline(g, x, y, w, h, c.edge(), 1);
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final Colours c = this.palette.get();
        ChromeShapes.vGradient(g, x, y, x + w, y + h, c.pushTop(), c.pushBottom());
        ChromeShapes.roundedOutline(g, x, y, w, h, c.pushRim(), 1);
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final Colours c = this.palette.get();
        ChromeShapes.vGradient(g, x, y, x + w, y + h, c.statusTop(), c.statusBottom());
        g.fill(x, y, x + w, y + 1, c.statusRule());
    }

    /** KDE 4 centred its window titles over the whole bar. */
    @Override
    public boolean titleCentered() {
        return true;
    }

    @Override
    public int shadowSpread() {
        return 6;
    }

    @Override
    public int shadowStrength() {
        return 0x16;
    }

    /**
     * The colours of the Oxygen chrome.
     *
     * @param edge          separators, and the rim of a panel, a field and a tab
     * @param panel         a content panel, and the foot of the tab in front
     * @param titleTop      the crown of the title of the window in front
     * @param titleBottom   its foot
     * @param idleTop       the crown of a title behind
     * @param idleBottom    its foot
     * @param buttonLit     the crown of a hovered title button or push button
     * @param buttonBottom  the foot of a title button
     * @param buttonRim     the ring round a title button
     * @param glyph         the marks on the title buttons
     * @param closeGlyph    the cross on the close button
     * @param pushTop       the crown of a push button, a thumb and the tab in front
     * @param pushBottom    their foot
     * @param pushRim       the rim of a push button and a thumb
     * @param focus         the rim of the default or hovered button and of the field that has the keyboard
     * @param buttonTop     the crown of a title button at rest
     * @param idleTabTop    the crown of a tab behind
     * @param idleTabBottom its foot
     * @param statusTop     the crown of a status bar
     * @param statusBottom  its foot
     * @param statusRule    the line over a status bar
     */
    public record Colours(int edge, int panel,
                          int titleTop, int titleBottom, int idleTop, int idleBottom,
                          int buttonLit, int buttonBottom, int buttonRim, int glyph, int closeGlyph,
                          int pushTop, int pushBottom, int pushRim, int focus, int buttonTop,
                          int idleTabTop, int idleTabBottom, int statusTop, int statusBottom, int statusRule) {
    }
}
