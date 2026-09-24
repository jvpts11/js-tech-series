/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A menu the way Motif drew one: a raised slab in the window's own colour, the entry under the pointer raised
 * again on it, an etched line between groups, and the keys that do the same thing written small at the right.
 *
 * <p>It is a list and nothing more: it knows where it is, what it holds and which entry a point is on. What an
 * entry does is the entry's own business, and when the menu opens and closes is the business of whoever owns it.
 */
@PaletteHolder
final class MotifMenu {

    /**
     * One entry of the menu.
     *
     * @param label   what it says, or empty for the etched line between two groups
     * @param keys    the keys that do the same, as Motif wrote them, or empty
     * @param enabled whether it can be chosen now; one that cannot is written dim
     * @param action  what choosing it does
     */
    record Entry(String label, String keys, boolean enabled, Runnable action) {

        static Entry line() {
            return new Entry("", "", false, () -> { });
        }

        boolean isLine() {
            return this.label.isEmpty();
        }
    }

    private List<Entry> entries = List.of();
    private boolean open;
    private int x;
    private int y;
    private int w;

    private static final int ROW_H = 11;
    private static final int LINE_H = 5;
    private static final int PAD = 3;
    private static final int KEYS_GAP = 12;
    /** A disabled entry's ink, {@code jsc:desktop/motif_menu}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "desktop/motif_menu",
            new Colours(0xFF5A5E6C));

    boolean isOpen() {
        return this.open;
    }

    List<Entry> entries() {
        return this.entries;
    }

    /** Opens the menu with its top left at that point, moved as needed to stay inside a desktop that size. */
    void open(final List<Entry> shown, final int atX, final int atY, final Font font, final int sw, final int sh) {
        this.entries = List.copyOf(shown);
        int widest = 0;
        for (final Entry entry : this.entries) {
            final int keys = entry.keys().isEmpty() ? 0 : KEYS_GAP + Texts.smallWidth(font, entry.keys());
            widest = Math.max(widest, font.width(entry.label()) + keys);
        }
        this.w = widest + PAD * 4;
        this.x = Math.max(0, Math.min(atX, sw - this.w));
        this.y = Math.max(0, Math.min(atY, sh - height()));
        this.open = true;
    }

    void close() {
        this.open = false;
    }

    boolean holds(final double mx, final double my) {
        return this.open && mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + height();
    }

    /** The entry under that point, or -1 when it is on a line, on the rim, or off the menu. */
    int entryAt(final double mx, final double my) {
        if (!holds(mx, my) || mx < this.x + PAD || mx >= this.x + this.w - PAD) {
            return -1;
        }
        int top = this.y + PAD;
        for (int i = 0; i < this.entries.size(); i++) {
            final int h = this.entries.get(i).isLine() ? LINE_H : ROW_H;
            if (my >= top && my < top + h) {
                return this.entries.get(i).isLine() ? -1 : i;
            }
            top += h;
        }
        return -1;
    }

    /** The middle of entry {@code index}, where a test clicks it. */
    int[] entryCentre(final int index) {
        int top = this.y + PAD;
        for (int i = 0; i < index; i++) {
            top += this.entries.get(i).isLine() ? LINE_H : ROW_H;
        }
        return new int[] {this.x + this.w / 2, top + ROW_H / 2};
    }

    /**
     * A click while the menu is open: on an entry that can be chosen it runs it, and wherever it lands the menu
     * is gone afterwards, which is what a menu does.
     */
    void clicked(final double mx, final double my) {
        final int at = entryAt(mx, my);
        close();
        if (at >= 0 && this.entries.get(at).enabled()) {
            this.entries.get(at).action().run();
        }
    }

    void render(final GuiGraphics g, final Font font, final int mx, final int my, final CdePalette p) {
        if (!this.open) {
            return;
        }
        MotifChrome.raised(g, this.x, this.y, this.w, height(), p.window(), p);
        final int armed = entryAt(mx, my);
        int top = this.y + PAD;
        for (int i = 0; i < this.entries.size(); i++) {
            final Entry entry = this.entries.get(i);
            if (entry.isLine()) {
                // Etched: the shade over the light, so the line reads as cut into the slab.
                g.fill(this.x + PAD, top + 1, this.x + this.w - PAD, top + 2, p.shade());
                g.fill(this.x + PAD, top + 2, this.x + this.w - PAD, top + 3, p.light());
                top += LINE_H;
                continue;
            }
            if (i == armed && entry.enabled()) {
                MotifChrome.raised(g, this.x + PAD, top, this.w - PAD * 2, ROW_H, p.window(), p);
            }
            final int ink = entry.enabled() ? p.ink() : PALETTE.get().dimInk();
            g.drawString(font, entry.label(), this.x + PAD * 2, top + 2, ink, false);
            if (!entry.keys().isEmpty()) {
                final int keysW = Texts.smallWidth(font, entry.keys());
                Texts.small(g, font, entry.keys(), this.x + this.w - PAD * 2 - keysW, top + 3, PALETTE.get().dimInk());
            }
            top += ROW_H;
        }
    }

    private int height() {
        int h = PAD * 2;
        for (final Entry entry : this.entries) {
            h += entry.isLine() ? LINE_H : ROW_H;
        }
        return h;
    }

    /** A disabled entry's ink. */
    private record Colours(int dimInk) {
    }
}
