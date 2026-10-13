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
 * Frames 10: square everywhere, a white window with a thin border in the accent, a white title bar written in black,
 * caption buttons with no face until the cursor finds them (the close one turning red), grey flat push buttons with a
 * rim, and fields with a plain grey line round them. It comes light and dark, {@code jsc:chrome/metro} and
 * {@code jsc:chrome/metro_dark}; the accent and the window's ground are the skin's.
 *
 * <p>It is not Frames 11's flat form under another name: 11 rounds its corners, softens its edges and lays its
 * selection on with a bar, where 10 draws hard rectangles and marks a selection with a pale wash of the accent.
 */
@PaletteHolder
final class MetroChrome implements IFormChrome {

    private final Palette<Colours> palette;
    private final boolean dark;

    static final MetroChrome LIGHT = new MetroChrome(Palettes.declare(JsComputers.MODID, "chrome/metro",
            new Colours(0xFFDADBDC, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFE5E5E5, 0xFFCCCCCC, 0xFFE81123, 0xFFF1707A,
                    0xFF000000, 0xFFFFFFFF, 0xFFE1E1E1, 0xFFE5F1FB, 0xFFCCE4F7, 0xFFADADAD, 0xFF7A7A7A,
                    0xFFC2C2C2, 0xFFF0F0F0)), false);

    static final MetroChrome DARK = new MetroChrome(Palettes.declare(JsComputers.MODID, "chrome/metro_dark",
            new Colours(0xFF3C3C3C, 0xFF2B2B2B, 0xFF1F1F1F, 0xFF333333, 0xFF4A4A4A, 0xFFE81123, 0xFFF1707A,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFF333333, 0xFF3E3E3E, 0xFF4F4F4F, 0xFF5A5A5A, 0xFF8A8A8A,
                    0xFF5C5C5C, 0xFF262626)), true);

    private MetroChrome(final Palette<Colours> palette, final boolean dark) {
        this.palette = palette;
        this.dark = dark;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.METRO;
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

    /** Square, a one-pixel border in the accent, the body in the window's ground. */
    @Override
    public void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, skin.accent());
        Grounds.fill(g, x, y, x + w, y + h, skin.windowBg());
    }

    @Override
    public void titleBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right) {
        // The bar is the window's own white, in front or behind: only the words and the border say which.
        Grounds.fill(g, x, y, x + w, y + h, active ? this.palette.get().title() : skin.windowBg());
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        final boolean isClose = control == OsSkin.Control.CLOSE;
        if (pressed) {
            g.fill(x, y, x + bw, y + bh, isClose ? c.closePressed() : c.pressed());
        } else if (hovered) {
            g.fill(x, y, x + bw, y + bh, isClose ? c.closeHover() : c.hover());
        }
        final boolean lit = isClose && (hovered || pressed);
        ChromeShapes.glyph(g, font, control, x, y, bw, bh, lit ? c.closeGlyph() : c.glyph());
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        final Colours c = this.palette.get();
        Grounds.fill(g, x, y, x + w, y + h, c.panel());
        ChromeShapes.outline(g, x, y, w, h, c.edge());
    }

    /** Flat grey with a grey rim; the default one ringed in the accent, and lit blue under the cursor. */
    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        final Colours c = this.palette.get();
        final int face = pressed ? c.buttonPressed() : hovered ? c.buttonHover() : c.button();
        Grounds.fill(g, x, y, x + w, y + h, face);
        ChromeShapes.outline(g, x, y, w, h, primary || hovered || pressed ? skin.accent() : c.buttonRim());
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        ChromeShapes.outline(g, x, y, w, h, focused ? skin.accent() : this.palette.get().fieldRim());
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        final Colours c = this.palette.get();
        if (active) {
            // The ribbon's tab in front: boxed, its foot open into the band under it.
            Grounds.fill(g, x, y, x + w, y + h, c.band());
            g.fill(x, y, x + w, y + 1, c.edge());
            g.fill(x, y, x + 1, y + h, c.edge());
            g.fill(x + w - 1, y, x + w, y + h, c.edge());
        }
    }

    @Override
    public int tabText(final OsSkin skin, final boolean active) {
        return active ? skin.text() : skin.dim();
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        g.fill(x + 1, y, x + w - 1, y + h, this.palette.get().thumb());
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final Colours c = this.palette.get();
        Grounds.fill(g, x, y, x + w, y + h, skin.windowBg());
        g.fill(x, y, x + w, y + 1, c.band());
    }

    @Override
    public int shadowSpread() {
        return 4;
    }

    @Override
    public int shadowStrength() {
        return 0x14;
    }

    /**
     * The colours of Frames 10's chrome.
     *
     * @param edge          separators, and the rim of a panel
     * @param panel         a content panel
     * @param title         the title bar of the window in front
     * @param hover         a hovered caption button
     * @param pressed       a pressed caption button
     * @param closeHover    the close button hovered, red
     * @param closePressed  the close button pressed
     * @param glyph         the marks on the caption buttons
     * @param closeGlyph    the cross on the lit close button
     * @param button        a push button
     * @param buttonHover   a hovered push button
     * @param buttonPressed a pressed push button
     * @param buttonRim     a push button's rim
     * @param fieldRim      a text field's rim
     * @param thumb         a scrollbar thumb
     * @param band          the pale band of a ribbon, and the line over a status bar
     */
    public record Colours(int edge, int panel, int title, int hover, int pressed, int closeHover, int closePressed,
                          int glyph, int closeGlyph, int button, int buttonHover, int buttonPressed, int buttonRim,
                          int fieldRim, int thumb, int band) {
    }
}
