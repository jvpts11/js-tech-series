/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdeScheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Motif, which is CDE: the chrome drawn by {@link MotifChrome} out of one colour scheme. It has no colours of its
 * own; every one is the scheme's, asked for each time, so the Style Manager, or a resource pack, changes it all.
 */
final class MotifFormChrome implements IFormChrome {

    private final CdeScheme scheme;

    MotifFormChrome(final CdeScheme scheme) {
        this.scheme = scheme;
    }

    @Override
    public OsSkin.Form form() {
        return OsSkin.Form.MOTIF;
    }

    @Override
    public int edge(final OsSkin skin) {
        return skin.windowBorder();
    }

    @Override
    public int panelFill(final OsSkin skin) {
        return skin.fieldBg();
    }

    @Override
    public void windowFrame(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        MotifChrome.windowFrame(g, x, y, w, h, this.scheme.colours());
    }

    @Override
    public void titleBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                         final boolean active, final int left, final int right) {
        MotifChrome.titleBar(g, x, y, w, h, active, left, right, this.scheme.colours());
    }

    /** Its marks are relief like everything else in it, not letters, so it draws them itself. */
    @Override
    public void control(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                        final int bw, final int bh, final OsSkin.Control control, final boolean hovered,
                        final boolean pressed) {
        MotifChrome.control(g, x, y, bw, bh, control, pressed, this.scheme.colours());
    }

    @Override
    public void panel(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h) {
        MotifChrome.sunken(g, x, y, w, h, panelFill(skin), this.scheme.colours());
    }

    @Override
    public void button(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                       final boolean hovered, final boolean pressed, final boolean primary) {
        MotifChrome.button(g, x, y, w, h, pressed, primary, this.scheme.colours());
    }

    @Override
    public void field(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        MotifChrome.sunken(g, x, y, w, h, skin.fieldBg(), this.scheme.colours());
    }

    @Override
    public void tab(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w, final int h,
                    final boolean active) {
        MotifChrome.tab(g, x, y, w, h, active, this.scheme.colours());
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                            final int h) {
        MotifChrome.raised(g, x, y, w, h, skin.windowBg(), this.scheme.colours());
    }

    @Override
    public void statusBar(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int w,
                          final int h) {
        MotifChrome.statusBar(g, x, y, w, h, this.scheme.colours());
    }

    @Override
    public boolean titleCentered() {
        return true;
    }

    /** Motif's one way out is the menu button at the left end of the bar; minimise and maximise stand at the right. */
    @Override
    public boolean menuAtLeft() {
        return true;
    }

    @Override
    public int frameThickness() {
        return MotifChrome.FRAME;
    }
}
