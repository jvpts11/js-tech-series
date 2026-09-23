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
 * Frames XP: glossy blue gradients, white title text, a green line on the tab in front, and only the top corners
 * of a window rounded. Its colours are {@code jsc:chrome/luna}.
 */
@PaletteHolder
final class LunaChrome implements IFormChrome {

    private final Palette<Colours> palette;

    static final LunaChrome INSTANCE = new LunaChrome(Palettes.declare(JsComputers.MODID, "chrome/luna",
            new Colours(0xFFB9C4DA, 0xFFF4F6FC,
                    0xFF8FA8C4, 0xFF9DB2C9, 0xFF7E93AC,
                    0xFF3F7FD6, 0xFF4B91E2, 0xFF2F6FC6, 0xFF2C66BD, 0xFF1C4D9C, 0x66FFFFFF, 0xFF16407F,
                    0xFFE58A6F, 0xFFC5341A, 0xFF8E2010, 0xFF6F9FE0, 0xFF2F63B8, 0xFF15448E, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xFFFDFDFF, 0xFFEAF0FB, 0xFFDDE7F6, 0xFFCBD9F0, 0xFFD0DBEF, 0xFFE6EDF9, 0x88FFFFFF,
                    0xFF7A9BD0, 0xFF2C66BD, 0xFF7F9DB9, 0xFF2C66BD,
                    0xFFFFFFFF, 0xFFDFEECB, 0xFF8FD14F, 0xFF7FA83F, 0xFFF4F7FD, 0xFFCDD9EE, 0xFF93A9CC,
                    0xFF2B5A16, 0xFF22324D,
                    0xFFFDFDFF, 0xFFC2D2EE, 0xFF93A9CC, 0xFFECECF6)));

    private LunaChrome(final Palette<Colours> palette) {
        this.palette = palette;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.LUNA;
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
        if (!active) {
            ChromeShapes.roundedRect(g, x, y, w, h, c.idleTitle(), radius, 0);
            g.fillGradient(x, y + radius, x + w, y + h, c.idleTitleFrom(), c.idleTitleTo());
            return;
        }
        // A smooth two-stop vertical gradient with a bright top gloss line, closer to the Luna glass.
        ChromeShapes.roundedRect(g, x, y, w, h, c.title(), radius, 0);
        g.fillGradient(x, y + radius, x + w, y + h / 2, c.titleCrownFrom(), c.titleCrownTo());
        g.fillGradient(x, y + h / 2, x + w, y + h, c.titleBaseFrom(), c.titleBaseTo());
        g.fill(x + radius, y + 1, x + w - radius, y + 2, c.titleGloss());
        g.fill(x, y + h - 1, x + w, y + h, c.titleShade());
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        final Colours c = this.palette.get();
        final boolean isClose = control == OsSkin.Control.CLOSE;
        final int top = isClose ? c.closeTop() : c.controlTop();
        final int bottom = isClose ? c.closeBottom() : c.controlBottom();
        if (pressed) {
            // The gradient turned over reads as pushed in.
            g.fillGradient(x, y, x + bw, y + bh, bottom, top);
        } else {
            g.fillGradient(x, y, x + bw, y + bh, hovered ? ChromeShapes.lighten(top) : top, bottom);
        }
        ChromeShapes.outline(g, x, y, bw, bh, isClose ? c.closeRim() : c.controlRim());
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
        // A glossier vertical sheen: bright top half, then the blue-tinted body.
        final int lit = hovered ? c.buttonHover() : c.buttonTop();
        g.fillGradient(x, y, x + w, y + h / 2, pressed ? c.pressedTop() : lit,
                pressed ? c.pressedMid() : c.buttonMid());
        g.fillGradient(x, y + h / 2, x + w, y + h, pressed ? c.pressedMid() : c.buttonLower(),
                pressed ? lit : c.buttonBottom());
        g.fill(x + 1, y + 1, x + w - 1, y + 2, c.buttonGloss());
        ChromeShapes.outline(g, x, y, w, h, primary ? c.primaryRim() : c.buttonRim());
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        final Colours c = this.palette.get();
        ChromeShapes.outline(g, x, y, w, h, focused ? c.fieldFocus() : c.fieldRim());
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        final Colours c = this.palette.get();
        if (active) {
            g.fillGradient(x, y, x + w, y + h, c.tabTop(), c.tabBottom());
            g.fill(x, y, x + w, y + 2, c.tabStrip());
            ChromeShapes.outline(g, x, y, w, h, c.tabRim());
        } else {
            g.fillGradient(x, y, x + w, y + h, c.idleTabTop(), c.idleTabBottom());
            ChromeShapes.outline(g, x, y, w, h, c.idleTabRim());
        }
    }

    @Override
    public int tabText(final OsSkin skin, final boolean active) {
        final Colours c = this.palette.get();
        return active ? c.tabText() : c.idleTabText();
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        final Colours c = this.palette.get();
        g.fillGradient(x, y, x + w, y + h, c.thumbTop(), c.thumbBottom());
        ChromeShapes.outline(g, x, y, w, h, c.thumbRim());
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        final Colours c = this.palette.get();
        g.fill(x, y, x + w, y + h, c.statusBar());
        g.fill(x, y, x + w, y + 1, c.edge());
    }

    /**
     * The colours of Frames XP's chrome.
     *
     * @param edge           separators, and the rim of a panel
     * @param panel          a content panel
     * @param idleTitle      the rounded cap of an idle title bar
     * @param idleTitleFrom  the top of an idle title's gradient
     * @param idleTitleTo    its bottom
     * @param title          the rounded cap of the active title bar
     * @param titleCrownFrom the top of the upper half of the active title
     * @param titleCrownTo   the bottom of that half
     * @param titleBaseFrom  the top of the lower half
     * @param titleBaseTo    the bottom of that half
     * @param titleGloss     the bright line along the top of the active title
     * @param titleShade     the dark line along its foot
     * @param closeTop       the top of the close button's red
     * @param closeBottom    its bottom
     * @param closeRim       its rim
     * @param controlTop     the top of the other title buttons' blue
     * @param controlBottom  its bottom
     * @param controlRim     their rim
     * @param glyph          the marks on the title buttons
     * @param buttonHover    the top of a hovered button
     * @param buttonTop      the top of a button
     * @param buttonMid      where a button's upper half ends
     * @param buttonLower    where its lower half starts
     * @param buttonBottom   its bottom
     * @param pressedTop     the top of a pressed button
     * @param pressedMid     the middle of a pressed button
     * @param buttonGloss    the bright line inside a button's top
     * @param buttonRim      a button's rim
     * @param primaryRim     the default button's rim
     * @param fieldRim       a text field's rim
     * @param fieldFocus     the rim of the field that has the keyboard
     * @param tabTop         the top of the tab in front
     * @param tabBottom      its bottom
     * @param tabStrip       the line across its top
     * @param tabRim         its rim
     * @param idleTabTop     the top of a tab behind
     * @param idleTabBottom  its bottom
     * @param idleTabRim     its rim
     * @param tabText        what the tab in front is labelled in
     * @param idleTabText    what a tab behind is labelled in
     * @param thumbTop       the top of a scrollbar thumb
     * @param thumbBottom    its bottom
     * @param thumbRim       its rim
     * @param statusBar      a status bar
     */
    public record Colours(int edge, int panel,
                          int idleTitle, int idleTitleFrom, int idleTitleTo,
                          int title, int titleCrownFrom, int titleCrownTo, int titleBaseFrom, int titleBaseTo,
                          int titleGloss, int titleShade,
                          int closeTop, int closeBottom, int closeRim, int controlTop, int controlBottom,
                          int controlRim, int glyph,
                          int buttonHover, int buttonTop, int buttonMid, int buttonLower, int buttonBottom,
                          int pressedTop, int pressedMid, int buttonGloss,
                          int buttonRim, int primaryRim, int fieldRim, int fieldFocus,
                          int tabTop, int tabBottom, int tabStrip, int tabRim, int idleTabTop, int idleTabBottom,
                          int idleTabRim, int tabText, int idleTabText,
                          int thumbTop, int thumbBottom, int thumbRim, int statusBar) {
    }
}
