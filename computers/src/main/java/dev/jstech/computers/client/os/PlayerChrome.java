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
 * The frames of a desktop a player wrote (computers/docs/design/player-systems.md): the style of frame it names draws
 * every control inside its windows and the relief around them, and the band of a window's title and its rim take the
 * desktop's own colours. On flat frames a window's buttons are plain squares: the least in a quiet shade, the middle
 * one in the accent, and the one that closes in the colour that closes.
 */
final class PlayerChrome implements IFormChrome {

    private final IFormChrome base;
    private final PlayerLooks.Colours colours;

    /* How far a flat frame's square button stands in from the box it is clicked in. */
    private static final int SQUARE_INSET = 2;

    PlayerChrome(final IFormChrome base, final PlayerLooks.Colours colours) {
        this.base = base;
        this.colours = colours;
    }

    @Override
    public OsSkin.Form form() {
        return this.base.form();
    }

    @Override
    public int edge(final OsSkin skin) {
        return this.colours.accent();
    }

    @Override
    public int panelFill(final OsSkin skin) {
        return this.base.panelFill(skin);
    }

    @Override
    public void titleBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right) {
        g.fill(x, y, x + w, y + h, this.colours.panel());
        g.fill(x, y + h - 1, x + w, y + h, this.colours.accent());
    }

    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        if (this.base.form() != OsSkin.Form.FLAT) {
            this.base.control(g, font, skin, x, y, bw, bh, control, hovered, pressed);
            return;
        }
        final int face = switch (control) {
            case MINIMIZE -> this.colours.quiet();
            case MAXIMIZE, RESTORE -> this.colours.accent();
            case CLOSE -> this.colours.close();
        };
        final int drawn = pressed ? ChromeShapes.darken(face) : hovered ? ChromeShapes.lighten(face) : face;
        g.fill(x + SQUARE_INSET, y + SQUARE_INSET, x + bw - SQUARE_INSET, y + bh - SQUARE_INSET, drawn);
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        this.base.panel(g, skin, x, y, w, h);
    }

    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        this.base.button(g, skin, x, y, w, h, hovered, pressed, primary);
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        this.base.field(g, skin, x, y, w, h, focused);
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        this.base.tab(g, skin, x, y, w, h, active);
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        this.base.scrollThumb(g, skin, x, y, w, h);
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        this.base.statusBar(g, skin, x, y, w, h);
    }

    @Override
    public void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        this.base.windowFrame(g, skin, x, y, w, h);
    }

    @Override
    public void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h, final int titleH, final int inset) {
        this.base.windowFrame(g, skin, x, y, w, h, titleH, inset);
    }

    @Override
    public void titleGlow(final GuiGraphics g, final int x, final int y, final int textW) {
        this.base.titleGlow(g, x, y, textW);
    }

    @Override
    public boolean dark() {
        return this.base.dark();
    }

    @Override
    public int buttonText(final OsSkin skin, final boolean primary) {
        return this.base.buttonText(skin, primary);
    }

    @Override
    public int tabText(final OsSkin skin, final boolean active) {
        return this.base.tabText(skin, active);
    }

    @Override
    public boolean selectionBar() {
        return this.base.selectionBar();
    }

    @Override
    public int frameThickness() {
        return this.base.frameThickness();
    }

    @Override
    public boolean titleCentered() {
        return this.base.titleCentered();
    }

    @Override
    public boolean menuAtLeft() {
        return this.base.menuAtLeft();
    }

    @Override
    public OsSkin.Controls controls() {
        return this.base.controls();
    }

    @Override
    public boolean focusOutline() {
        return this.base.focusOutline();
    }

    @Override
    public int shadowSpread() {
        return this.base.shadowSpread();
    }

    @Override
    public int shadowStrength() {
        return this.base.shadowStrength();
    }
}
