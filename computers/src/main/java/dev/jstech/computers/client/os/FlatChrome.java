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
 * Frames 11 and the modern Linux desktops: flat fills, hairlines, a thin accent, gently rounded corners. It comes
 * light and dark, each with its colours, {@code jsc:chrome/flat} and {@code jsc:chrome/flat_dark}; the accent and
 * the window's ground are the skin's.
 */
@PaletteHolder
final class FlatChrome implements IFormChrome {

    private final Palette<Colours> palette;
    private final boolean dark;

    static final FlatChrome LIGHT = new FlatChrome(Palettes.declare(JsComputers.MODID, "chrome/flat",
            new Colours(0xFFE3E5EE, 0xFFFFFFFF, 0xFFEDEEF2, 0xFFD0D3DC, 0xFFE6E8F0, 0xFFC5341A, 0xFFE5413A,
                    0xFF3A4256, 0xFFFFFFFF, 0xFFEEF0F6, 0xFFFBFBFE, 0xFFCDD1DD, 0xFFFFFFFF, 0xFFC8CDDA,
                    0xFFF1F2F6)), false);

    static final FlatChrome DARK = new FlatChrome(Palettes.declare(JsComputers.MODID, "chrome/flat_dark",
            new Colours(0xFF333A48, 0xFF242833, 0xFF1B2029, 0xFF3A4150, 0xFF2C3340, 0xFFC5341A, 0xFFE5413A,
                    0xFFC4CAD6, 0xFFFFFFFF, 0xFF2C3340, 0xFF262B36, 0xFF3A4150, 0xFFFFFFFF, 0xFF3E4653,
                    0xFF242833)), true);

    private FlatChrome(final Palette<Colours> palette, final boolean dark) {
        this.palette = palette;
        this.dark = dark;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.FLAT;
    }

    @Override
    public boolean dark() {
        return this.dark;
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
        final int radius = skin.topRadius();
        // A light bar written on in dark text when the window is in front, a quieter one behind; a hairline under.
        ChromeShapes.roundedRect(g, x, y, w, h, active ? skin.windowBg() : c.idleTitle(), radius, 0);
        g.fill(x + radius, y + h - 1, x + w - radius, y + h, c.edge());
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        final boolean isClose = control == OsSkin.Control.CLOSE;
        if (pressed) {
            g.fill(x, y, x + bw, y + bh, isClose ? c.closePressed() : c.pressed());
        } else if (isClose && hovered) {
            g.fill(x, y, x + bw, y + bh, c.closeHover());
        } else if (hovered) {
            g.fill(x, y, x + bw, y + bh, c.controlHover());
        }
        final boolean closeLit = isClose && (pressed || hovered);
        ChromeShapes.glyph(g, font, control, x, y, bw, bh, closeLit ? c.closeGlyph() : c.glyph());
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
        final int accent = skin.accent();
        final int face;
        if (primary) {
            face = pressed ? ChromeShapes.darken(accent) : accent;
        } else {
            face = pressed ? c.pressed() : hovered ? c.buttonHover() : c.button();
        }
        ChromeShapes.roundedRect(g, x, y, w, h, face, 2, 2);
        ChromeShapes.roundedOutline(g, x, y, w, h, primary ? ChromeShapes.darken(accent) : c.rim(), 2);
    }

    @Override
    public int buttonText(final OsSkin skin, final boolean primary) {
        return primary ? this.palette.get().primaryText() : skin.text();
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        final int rim = focused ? skin.accent() : this.palette.get().rim();
        ChromeShapes.outline(g, x, y, w, h, rim);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h, rim);
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        if (active) {
            // The tab in front is only underlined.
            g.fill(x, y + h - 2, x + w, y + h, skin.accent());
        }
    }

    @Override
    public int tabText(final OsSkin skin, final boolean active) {
        return active ? skin.accent() : skin.dim();
    }

    @Override
    public boolean selectionBar() {
        return true;
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        ChromeShapes.roundedRect(g, x + 1, y, w - 2, h, this.palette.get().thumb(), 2, 2);
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, c.statusBar());
        g.fill(x, y, x + w, y + 1, c.edge());
    }

    /**
     * The colours of the flat chrome.
     *
     * @param edge         separators, hairlines and the rim of a panel
     * @param panel        a content panel
     * @param idleTitle    the title bar of a window behind
     * @param pressed      a pressed title button or push button
     * @param controlHover a hovered title button
     * @param closePressed the close button pressed
     * @param closeHover   the close button hovered
     * @param glyph        the marks on the title buttons
     * @param closeGlyph   the close button's mark when it is lit
     * @param buttonHover  a hovered push button
     * @param button       a push button
     * @param rim          the rim of a push button and of a text field
     * @param primaryText  what the default button is labelled in, on the accent
     * @param thumb        a scrollbar thumb
     * @param statusBar    a status bar
     */
    public record Colours(int edge, int panel, int idleTitle, int pressed, int controlHover, int closePressed,
                          int closeHover, int glyph, int closeGlyph, int buttonHover, int button, int rim,
                          int primaryText, int thumb, int statusBar) {
    }
}
