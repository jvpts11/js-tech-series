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
 * GNOME 2 with Clearlooks: a blue title in three stops with its words in white, small glossy studs for the title
 * buttons, a warm grey body, buttons lit from above with their corners just rounded, and a blue frame round the window
 * in front. Its colours are {@code jsc:chrome/clearlooks}.
 */
@PaletteHolder
final class ClearlooksChrome implements IFormChrome {

    private final Palette<Colours> palette;

    static final ClearlooksChrome INSTANCE = new ClearlooksChrome(Palettes.declare(JsComputers.MODID,
            "chrome/clearlooks", new Colours(0xFFCFCCC6, 0xFFEDEDED,
                    0xFF8DB2E3, 0xFF6892CC, 0xFF5885C4, 0xFFDCDAD5, 0xFFC9C6C0, 0xFFBDBAB3,
                    0xFFA9C6EC, 0xFF6D97CF, 0xFF4A6F9F, 0xFFFFFFFF, 0xFFC3D7F2,
                    0xFFFBFBFA, 0xFFE4E2DE, 0xFFB5B2AC, 0xFF6A96CF, 0xFFFFFFFF,
                    0xFFE6E4E0, 0xFFD3D0CA, 0xFFEDEDED, 0xFFE4E3E1, 0xFFCFCCC6)));

    private ClearlooksChrome(final Palette<Colours> palette) {
        this.palette = palette;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.CLEARLOOKS;
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
        final int middle = active ? c.titleMiddle() : c.idleMiddle();
        final int bottom = active ? c.titleBottom() : c.idleBottom();
        for (int i = 0; i < r; i++) {
            final int in = r - i;
            g.fill(x + in, y + i, x + w - in, y + i + 1, top);
        }
        // Two stops into the middle and a flatter fall below it, the gloss of the theme's title.
        ChromeShapes.vGradient(g, x, y + r, x + w, y + h / 2, top, middle);
        ChromeShapes.vGradient(g, x, y + h / 2, x + w, y + h, middle, bottom);
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        final int lit = hovered ? c.studLit() : c.studTop();
        if (pressed) {
            ChromeShapes.vGradient(g, x, y, x + bw, y + bh, c.studBottom(), lit);
        } else {
            ChromeShapes.vGradient(g, x, y, x + bw, y + bh, lit, c.studBottom());
        }
        ChromeShapes.roundedOutline(g, x, y, bw, bh, c.studRim(), 1);
        final int nudge = pressed ? 1 : 0;
        ChromeShapes.glyph(g, font, control, x + nudge, y + nudge, bw, bh, c.studGlyph());
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
            ChromeShapes.vGradient(g, x, y, x + w, y + h, hovered ? c.pushLit() : c.pushTop(), c.pushBottom());
        }
        ChromeShapes.roundedOutline(g, x, y, w, h, primary ? c.focus() : c.pushRim(), 1);
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        final Colours c = this.palette.get();
        ChromeShapes.outline(g, x, y, w, h, focused ? c.focus() : c.pushRim());
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
        ChromeShapes.roundedOutline(g, x, y, w, h, c.pushRim(), 1);
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final Colours c = this.palette.get();
        ChromeShapes.vGradient(g, x, y, x + w, y + h, c.studTop(), c.studBottom());
        ChromeShapes.roundedOutline(g, x, y, w, h, c.studRim(), 1);
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final Colours c = this.palette.get();
        ChromeShapes.vGradient(g, x, y, x + w, y + h, c.statusTop(), c.statusBottom());
        g.fill(x, y, x + w, y + 1, c.statusRule());
    }

    /**
     * The colours of the Clearlooks chrome.
     *
     * @param edge          separators, and the rim of a panel
     * @param panel         a content panel, and the foot of the tab in front
     * @param titleTop      the crown of the title of the window in front
     * @param titleMiddle   its middle
     * @param titleBottom   its foot
     * @param idleTop       the crown of a title behind
     * @param idleMiddle    its middle
     * @param idleBottom    its foot
     * @param studTop       the crown of a title button and a scroll thumb
     * @param studBottom    their foot
     * @param studRim       their rim
     * @param studGlyph     the marks on the title buttons
     * @param studLit       the crown of a hovered title button
     * @param pushTop       the crown of a push button and of the tab in front
     * @param pushBottom    the foot of a push button
     * @param pushRim       the rim of a push button, a field and a tab
     * @param focus         the rim of the default button and of the field that has the keyboard
     * @param pushLit       the crown of a hovered push button
     * @param idleTabTop    the crown of a tab behind
     * @param idleTabBottom its foot
     * @param statusTop     the crown of a status bar
     * @param statusBottom  its foot
     * @param statusRule    the line over a status bar
     */
    public record Colours(int edge, int panel,
                          int titleTop, int titleMiddle, int titleBottom, int idleTop, int idleMiddle, int idleBottom,
                          int studTop, int studBottom, int studRim, int studGlyph, int studLit,
                          int pushTop, int pushBottom, int pushRim, int focus, int pushLit,
                          int idleTabTop, int idleTabBottom, int statusTop, int statusBottom, int statusRule) {
    }
}
