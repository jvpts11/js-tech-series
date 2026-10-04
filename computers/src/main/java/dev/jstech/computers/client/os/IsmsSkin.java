/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.skin.ISkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

/**
 * The controls of the IQL Server Management Studio in the dress of one of its ages: the buttons, fields, tabs,
 * menus and popups it draws through the desktop's components. A program has an identity of its own, so the studio
 * wears its age's look on every desktop rather than the system's.
 */
final class IsmsSkin implements ISkin {

    private IsmsLook look = IsmsLook.STUDIO_2012;

    @Override
    public int text() {
        return colours().text();
    }

    @Override
    public int dim() {
        return colours().dim();
    }

    @Override
    public int accent() {
        return colours().menuLit();
    }

    @Override
    public int edge() {
        return colours().edge();
    }

    @Override
    public int windowBg() {
        return colours().window();
    }

    @Override
    public int panelBg() {
        return colours().menu();
    }

    @Override
    public int fieldBg() {
        return colours().panel();
    }

    @Override
    public int listHover() {
        return colours().select();
    }

    @Override
    public int listRowText(final boolean selected) {
        return colours().text();
    }

    @Override
    public void panel(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, colours().panel());
        if (look.bevelled()) {
            sunken(g, x, y, w, h);
        } else {
            Draw.outline(g, x, y, w, h, colours().edge());
        }
    }

    @Override
    public void button(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                       final String label, final boolean hovered, final boolean pressed, final boolean primary) {
        final IsmsLook.Colours c = colours();
        g.fill(x, y, x + w, y + h, hovered && !look.bevelled() ? c.select() : c.button());
        if (look.bevelled()) {
            if (pressed) {
                sunken(g, x, y, w, h);
            } else {
                raised(g, x, y, w, h);
            }
        } else {
            Draw.outline(g, x, y, w, h, hovered || primary ? c.selectEdge() : c.buttonEdge());
        }
        final int shift = pressed && look.bevelled() ? 1 : 0;
        Draw.text(g, font, label, x + (w - font.width(label)) / 2 + shift, y + (h - 8) / 2 + shift, c.text(),
                c.button());
    }

    @Override
    public void field(final GuiGraphics g, final int x, final int y, final int w, final int h, final boolean focused) {
        g.fill(x, y, x + w, y + h, colours().panel());
        if (look.bevelled()) {
            sunken(g, x, y, w, h);
        } else {
            Draw.outline(g, x, y, w, h, focused ? colours().accent() : colours().buttonEdge());
        }
    }

    @Override
    public void tab(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                    final String label, final boolean active) {
        final IsmsLook.Colours c = colours();
        if (active && c.tabActive() != 0) {
            g.fill(x, y, x + w, y + h, c.tabActive());
        }
        if (active && c.tabMark() != 0) {
            g.fill(x, y, x + w, y + 2, c.tabMark());
        }
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, c.edge());
        Draw.text(g, font, label, x + 5, y + (h - 8) / 2 + 1, active ? c.tabActiveText() : c.tabText(),
                active && c.tabActive() != 0 ? c.tabActive() : c.tabs());
    }

    @Override
    public void listRow(final GuiGraphics g, final int x, final int y, final int w, final int h,
                        final boolean hovered, final boolean selected) {
        if (selected) {
            g.fill(x, y, x + w, y + h, colours().select());
            Draw.outline(g, x, y, w, h, colours().selectEdge());
        } else if (hovered) {
            g.fill(x, y, x + w, y + h, colours().select());
        }
    }

    @Override
    public void scrollThumb(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, colours().splitEdge());
    }

    @Override
    public void statusBar(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final IsmsLook.Colours c = colours();
        g.fillGradient(x, y, x + w, y + h, c.status(), c.statusTo());
        g.fill(x, y, x + w, y + 1, c.statusEdge());
    }

    @Override
    public void windowFrame(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, colours().window());
        if (look.bevelled()) {
            raised(g, x, y, w, h);
        } else {
            Draw.outline(g, x, y, w, h, colours().buttonEdge());
        }
    }

    /** The look of the studio on the computer at {@code host}, by its age as this client sees it. */
    static IsmsLook lookOf(final BlockPos host) {
        final Minecraft mc = Minecraft.getInstance();
        return IsmsLook.of(mc.level != null && mc.level.getBlockEntity(host) instanceof IOsHost machine
                ? machine.displayEra() : null);
    }

    /** Dresses the controls in {@code value}'s look from now on. */
    void wear(final IsmsLook value) {
        this.look = value;
    }

    IsmsLook look() {
        return look;
    }

    /** A control standing out of its ground: lit along the top and left, shadowed along the bottom and right. */
    void raised(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final IsmsLook.Colours c = colours();
        g.fill(x, y, x + w, y + 1, c.buttonLight());
        g.fill(x, y, x + 1, y + h, c.buttonLight());
        g.fill(x, y + h - 1, x + w, y + h, c.buttonEdge());
        g.fill(x + w - 1, y, x + w, y + h, c.buttonEdge());
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c.buttonShadow());
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, c.buttonShadow());
    }

    /** A well sunk into its ground: shadowed along the top and left, lit along the bottom and right. */
    void sunken(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final IsmsLook.Colours c = colours();
        g.fill(x, y, x + w, y + 1, c.buttonShadow());
        g.fill(x, y, x + 1, y + h, c.buttonShadow());
        g.fill(x, y + h - 1, x + w, y + h, c.buttonLight());
        g.fill(x + w - 1, y, x + w, y + h, c.buttonLight());
    }

    private IsmsLook.Colours colours() {
        return look.colours();
    }
}
