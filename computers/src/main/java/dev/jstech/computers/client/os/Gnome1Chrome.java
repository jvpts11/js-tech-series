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
 * GNOME of the late 1990s: a thick frame in relief, a centred title in muted purple, warm greys and chunky
 * bevelled studs. A separate window manager drew the frame, which is why it looks nothing like KDE's of the same
 * years. Its colours are {@code jsc:chrome/gnome1}.
 */
@PaletteHolder
final class Gnome1Chrome implements IFormChrome {

    private final Palette<Colours> palette;

    static final Gnome1Chrome INSTANCE = new Gnome1Chrome(Palettes.declare(JsComputers.MODID, "chrome/gnome1",
            new Colours(0xFFA49E8F, 0xFFCDC8BC,
                    0xFFAFAAA0, 0xFF938E84, 0xFF8C877D, 0xFF767168,
                    0xFF8F7D99, 0xFF6D5A78, 0xFF63506E, 0xFF55455F,
                    0xFFD6D2C8, 0xFFE2DED4, 0xFFC4BFB2, 0xFF2A2A2A, 0xFF2A2A2A, 0xFF85806F, 0xFFF0EDE6,
                    0xFFFFFFFF, 0xFFDFDFDF, 0xFF808080, 0xFF000000)));

    private Gnome1Chrome(final Palette<Colours> palette) {
        this.palette = palette;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.GNOME1;
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
    public void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final int t = frameThickness();
        ChromeShapes.roundedRect(g, x - t, y - t, w + t * 2, h + t * 2, skin.windowBorder(), skin.topRadius(),
                skin.bottomRadius());
        // The thick period frame is relief, not a flat band: a slab of plain colour at this width looks like a mistake.
        relief(g, this.palette.get(), x - t, y - t, w + t * 2, h + t * 2, true);
        ChromeShapes.roundedRect(g, x, y, w, h, skin.windowBg(), skin.topRadius(), skin.bottomRadius());
    }

    @Override
    public void titleBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right) {
        final Colours c = this.palette.get();
        // Muted purple, the colour that told a GNOME box from a KDE one across the room; a window behind greys out.
        if (active) {
            g.fillGradient(x, y, x + w, y + h / 2, c.crownFrom(), c.crownTo());
            g.fillGradient(x, y + h / 2, x + w, y + h, c.baseFrom(), c.baseTo());
        } else {
            g.fillGradient(x, y, x + w, y + h / 2, c.idleCrownFrom(), c.idleCrownTo());
            g.fillGradient(x, y + h / 2, x + w, y + h, c.idleBaseFrom(), c.idleBaseTo());
        }
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + bw, y + bh, hovered ? c.hover() : c.face());
        relief(g, c, x, y, bw, bh, !pressed);
        final int nudge = pressed ? 1 : 0;
        ChromeShapes.glyph(g, font, control, x + nudge, y + nudge, bw, bh, c.glyph());
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, c.panel());
        relief(g, c, x, y, w, h, false);
    }

    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, hovered ? c.hover() : c.face());
        relief(g, c, x, y, w, h, !pressed);
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
        g.fill(x, y, x + w, y + tall, active ? c.face() : c.idleTab());
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
        g.fill(x, y, x + w, y + h, c.panel());
        g.fill(x, y, x + w, y + 1, c.statusShadow());
        g.fill(x, y + 1, x + w, y + 2, c.statusLight());
    }

    /** The only form besides Motif that centres a title, which is exactly why it reads as another desktop. */
    @Override
    public boolean titleCentered() {
        return true;
    }

    @Override
    public int frameThickness() {
        return 3;
    }

    private static void relief(final GuiGraphics g, final Colours c, final int x, final int y, final int w,
                               final int h, final boolean raised) {
        ChromeShapes.bevelDouble(g, x, y, w, h, raised, c.highlight(), c.light(), c.shadow(), c.darkShadow());
    }

    /**
     * The colours of the period GNOME chrome.
     *
     * @param edge          separators
     * @param panel         a content panel, and a status bar
     * @param idleCrownFrom the top of an idle title's upper half
     * @param idleCrownTo   the bottom of that half
     * @param idleBaseFrom  the top of its lower half
     * @param idleBaseTo    the bottom of that half
     * @param crownFrom     the top of the active title's upper half
     * @param crownTo       the bottom of that half
     * @param baseFrom      the top of its lower half
     * @param baseTo        the bottom of that half
     * @param face          a button, a stud, a thumb and the tab in front
     * @param hover         a hovered button or stud
     * @param idleTab       a tab behind
     * @param glyph         the marks on the title buttons
     * @param focus         the dotted ring inside the default button
     * @param statusShadow  the shadow line over a status bar
     * @param statusLight   the lit line under it
     * @param highlight     the outer lit edge of anything in relief
     * @param light         its inner lit edge
     * @param shadow        its inner shaded edge
     * @param darkShadow    its outer shaded edge
     */
    public record Colours(int edge, int panel,
                          int idleCrownFrom, int idleCrownTo, int idleBaseFrom, int idleBaseTo,
                          int crownFrom, int crownTo, int baseFrom, int baseTo,
                          int face, int hover, int idleTab, int glyph, int focus, int statusShadow,
                          int statusLight, int highlight, int light, int shadow, int darkShadow) {
    }
}
