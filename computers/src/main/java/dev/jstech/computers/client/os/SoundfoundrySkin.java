/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The foundry skin Soundfoundry wears on every desktop: plates of dark iron with rivets, steel buttons, and displays
 * lit in amber like molten metal. What it is drawn with, piece by piece, in the colours of
 * {@code jsc:app/soundfoundry}.
 */
@PaletteHolder
final class SoundfoundrySkin {

    /** The skin's colours, {@code jsc:app/soundfoundry}. */
    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/soundfoundry", new Colours(
            0xFF26242A, 0xFF4B4752, 0xFF0F0E11, 0x06FFFFFF,
            0xFF3A3740, 0xFF5E5966, 0xFF1A181D, 0xFF2B2830,
            0xFF17151A, 0xFF211F24, 0xFFB8570F, 0xFF4A3A2C,
            0xFF6D6673, 0xFFA59EA9, 0xFF2E2A33,
            0xFF0C0805, 0xFF3B2A18, 0xFF000000,
            0xFFFFB347, 0xFFE8892B, 0xFF5A3A16, 0xFFE3DACB, 0xFF8C8478, 0xFFC9B89C,
            0xFF08070A, 0xFFB8570F, 0xFF3A2A18, 0xFF6B3A12, 0xFF1D130A,
            0xFF1D130A, 0xFFE8702A, 0xFFFFB347, 0xFFFFE08A, 0xFFFFF1C9, 0xFF3B2A18,
            0xFF2A1A0C,
            0xFFC9C0B2, 0xFF8C8478, 0xFF5F574E,
            0xFF2A1A0C, 0xFF3B2410, 0xFF3B2A18, 0xFF3B2A18, 0xFF17151A,
            0xFFE8892B, 0xFFFFD08A, 0xFF6B5A44, 0xFF8C7A60));

    /* Which segments of a seven-segment digit are lit for each figure: a to g, clockwise from the top, g the middle. */
    private static final String[] SEGMENTS = {"abcdef", "bc", "abged", "abgcd", "fgbc", "afgcd", "afgedc", "abc",
            "abcdefg", "abcdfg"};
    /* Where each segment sits in a digit eight wide and fourteen tall: x, y, width, height. */
    private static final int[][] SEGMENT_AT = {{1, 0, 6, 2}, {6, 1, 2, 6}, {6, 7, 2, 6}, {1, 12, 6, 2}, {0, 7, 2, 6},
            {0, 1, 2, 6}, {1, 6, 6, 2}};

    private SoundfoundrySkin() {
    }

    /** What the transport buttons show. */
    enum Glyph { PREVIOUS, PLAY, PAUSE, STOP, NEXT, EJECT }

    static Colours c() {
        return PALETTE.get();
    }

    static void fill(final GuiGraphics g, final int x, final int y, final int w, final int h, final int colour) {
        g.fill(x, y, x + w, y + h, colour);
    }

    /** A raised face: lit along its top and left, in shadow along its bottom and right. */
    static void bevel(final GuiGraphics g, final int x, final int y, final int w, final int h, final int face,
                      final int hi, final int lo) {
        fill(g, x, y, w, h, face);
        fill(g, x, y, w, 1, hi);
        fill(g, x, y, 1, h, hi);
        fill(g, x, y + h - 1, w, 1, lo);
        fill(g, x + w - 1, y, 1, h, lo);
    }

    /** An iron plate with a rivet in each corner and the faint grain of rolled metal. */
    static void plate(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final Colours c = c();
        bevel(g, x, y, w, h, c.iron(), c.ironHi(), c.ironLo());
        for (int yy = y + 2; yy < y + h - 2; yy += 2) {
            fill(g, x + 1, yy, w - 2, 1, c.grain());
        }
        rivet(g, x + 2, y + 2);
        rivet(g, x + w - 5, y + 2);
        if (h > SoundfoundryLayout.SHADE_H) {
            rivet(g, x + 2, y + h - 5);
            rivet(g, x + w - 5, y + h - 5);
        }
    }

    /**
     * The bar along a plate's top: its name between two grooves, the grooves lit while the window is in front, and
     * its three buttons, the held one pushed in.
     */
    static void bar(final GuiGraphics g, final Font font, final int x, final int y, final String label,
                    final boolean active, final int pressed) {
        bar(g, font, x, y, SoundfoundryLayout.WIDTH, label, active, pressed);
    }

    /** The same, along a plate {@code w} wide. */
    static void bar(final GuiGraphics g, final Font font, final int x, final int y, final int w, final String label,
                    final boolean active, final int pressed) {
        final Colours c = c();
        final int ground = active ? c.barActive() : c.barIdle();
        fill(g, x + 1, y + 1, w - 2, 11, ground);
        final int lw = font.width(label);
        final int lx = x + w / 2 - lw / 2;
        final int groove = active ? c.grooveActive() : c.grooveIdle();
        for (final int gy : new int[] {y + 4, y + 7}) {
            fill(g, x + 8, gy, Math.max(0, lx - x - 12), 1, groove);
            fill(g, lx + lw + 4, gy, Math.max(0, x + w - 34 - (lx + lw + 4)), 1, groove);
        }
        Draw.text(g, font, label, lx, y + 3, active ? c.ink() : c.inkDim(), ground);
        for (int i = 0; i < 3; i++) {
            final int bx = x + w - 30 + i * 9;
            final boolean down = pressed == i;
            if (down) {
                bevel(g, bx, y + 3, 8, 7, c.steelPressed(), c.steelLo(), c.steelHi());
            } else {
                bevel(g, bx, y + 3, 8, 7, c.steel(), c.steelHi(), c.steelLo());
            }
            final int ink = c.ink();
            final int gx = bx + 2 + (down ? 1 : 0);
            final int gy = y + (down ? 1 : 0);
            switch (i) {
                case 0 -> fill(g, gx, gy + 7, 4, 1, ink);
                case 1 -> {
                    fill(g, gx, gy + 5, 4, 1, ink);
                    fill(g, gx, gy + 7, 4, 1, ink);
                }
                default -> {
                    for (int k = 0; k < 4; k++) {
                        fill(g, gx + k, gy + 5 + (k * 3) / 4, 1, 1, ink);
                        fill(g, gx + 3 - k, gy + 5 + (k * 3) / 4, 1, 1, ink);
                    }
                }
            }
        }
    }

    /** A display: dark glass in a bronze edge, shadowed along its top. */
    static void lcd(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final Colours c = c();
        fill(g, x, y, w, h, c.lcdEdge());
        fill(g, x + 1, y + 1, w - 2, h - 2, c.lcd());
        fill(g, x + 1, y + 1, w - 2, 1, c.lcdTop());
    }

    /** A steel button, pushed in when {@code pressed}, with an amber line along its foot when {@code lit}. */
    static void button(final GuiGraphics g, final int x, final int y, final int w, final int h, final boolean pressed,
                       final boolean lit) {
        final Colours c = c();
        if (pressed) {
            bevel(g, x, y, w, h, c.steelPressed(), c.steelLo(), c.steelHi());
        } else {
            bevel(g, x, y, w, h, c.steel(), c.steelHi(), c.steelLo());
        }
        if (lit) {
            fill(g, x + 2, y + h - 3, w - 4, 1, c.amber());
        }
    }

    /** A steel button with a word on it: amber while it is on, dim while it can do nothing. */
    static void labelButton(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                            final String label, final boolean lit, final boolean dim) {
        final Colours c = c();
        button(g, x, y, w, h, false, lit);
        final String shown = Texts.clip(font, label, w - 2);
        Draw.text(g, font, shown, x + (w - font.width(shown)) / 2, y + (h - 8) / 2,
                dim ? c.inkDim() : lit ? c.amber() : c.ink(), c.steel());
    }

    /** A slider: a slot filled up to the thumb, and the thumb. */
    static void slider(final GuiGraphics g, final int x, final int y, final int w, final double value,
                       final boolean dim) {
        final Colours c = c();
        final int thumb = SoundfoundryLayout.THUMB;
        final int at = (int) Math.round((w - thumb) * Math.clamp(value, 0.0, 1.0));
        fill(g, x, y + 3, w, 3, c.track());
        fill(g, x, y + 3, at + thumb / 2, 3, dim ? c.fillDim() : c.fill());
        bevel(g, x + at, y, thumb, 9, c.steel(), c.steelHi(), c.steelLo());
    }

    /** The bar along which a song runs, and its thumb where the song has got to. */
    static void position(final GuiGraphics g, final int x, final int y, final int w, final double value,
                         final boolean on) {
        final Colours c = c();
        final int thumb = SoundfoundryLayout.POSITION_THUMB;
        final int at = (int) Math.round((w - thumb) * Math.clamp(value, 0.0, 1.0));
        fill(g, x, y + 2, w, 5, c.track());
        fill(g, x, y + 2, at, 5, on ? c.positionFill() : c.positionDead());
        bevel(g, x + at, y, thumb, 9, c.steel(), c.steelHi(), c.steelLo());
    }

    /** Minutes and seconds in seven-segment figures, with the unlit segments a faint ghost. */
    static void clock(final GuiGraphics g, final int x, final int y, final long millis, final boolean on) {
        final long seconds = Math.min(99L * 60L + 59L, Math.max(0L, millis / 1000L));
        final int minutes = (int) (seconds / 60L);
        final int rest = (int) (seconds % 60L);
        digit(g, x, y, minutes / 10, on);
        digit(g, x + 10, y, minutes % 10, on);
        final int dot = on ? c().amber() : c().segmentOff();
        fill(g, x + 21, y + 3, 2, 2, dot);
        fill(g, x + 21, y + 9, 2, 2, dot);
        digit(g, x + 26, y, rest / 10, on);
        digit(g, x + 36, y, rest % 10, on);
    }

    /** The analyser's bars, each from 0 to 1 of its height, a peak cap above each; flat and dark while it is still. */
    static void bars(final GuiGraphics g, final int x, final int bottom, final float[] levels, final boolean alive) {
        final Colours c = c();
        final int tallest = 16;
        for (int i = 0; i < levels.length; i++) {
            final int h = alive ? Math.round(Math.clamp(levels[i], 0.0F, 1.0F) * tallest) : 0;
            for (int k = 0; k < h; k++) {
                final float t = k / (float) tallest;
                fill(g, x + i * 4, bottom - k - 1, 3, 1, t > 0.75F ? c.barHigh() : t > 0.4F ? c.barMid() : c.barLow());
            }
            fill(g, x + i * 4, bottom - (alive ? h + 2 : 1) - 1, 3, 1, alive ? c.peak() : c.peakDead());
        }
    }

    /** One of the transport buttons' pictures. */
    static void glyph(final GuiGraphics g, final Glyph glyph, final int x, final int y, final int colour) {
        switch (glyph) {
            case PLAY -> {
                for (int k = 0; k < 7; k++) {
                    fill(g, x + k, y + k / 2, 1, 7 - 2 * (k / 2), colour);
                }
            }
            case PAUSE -> {
                fill(g, x, y, 2, 7, colour);
                fill(g, x + 4, y, 2, 7, colour);
            }
            case STOP -> fill(g, x, y, 6, 6, colour);
            case PREVIOUS -> {
                fill(g, x, y, 1, 7, colour);
                for (int k = 0; k < 4; k++) {
                    fill(g, x + 1 + k, y + 3 - k, 1, 1 + 2 * k, colour);
                    fill(g, x + 5 + k, y + 3 - k, 1, 1 + 2 * k, colour);
                }
            }
            case NEXT -> {
                for (int k = 0; k < 4; k++) {
                    fill(g, x + k, y + k, 1, 7 - 2 * k, colour);
                    fill(g, x + 4 + k, y + k, 1, 7 - 2 * k, colour);
                }
                fill(g, x + 8, y, 1, 7, colour);
            }
            case EJECT -> {
                for (int k = 0; k < 4; k++) {
                    fill(g, x + 3 - k, y + k, 1 + 2 * k, 1, colour);
                }
                fill(g, x, y + 5, 7, 2, colour);
            }
        }
    }

    /** Voidsoft's mark: an anvil with the void, a hole, punched through its face. */
    static void anvil(final GuiGraphics g, final int x, final int y) {
        final Colours c = c();
        fill(g, x, y + 2, 14, 3, c.anvilMid());
        fill(g, x, y + 2, 14, 1, c.anvilLight());
        fill(g, x + 13, y + 3, 3, 1, c.anvilMid());
        fill(g, x + 4, y + 5, 6, 3, c.anvilDark());
        fill(g, x + 2, y + 8, 10, 2, c.anvilMid());
        fill(g, x + 2, y + 8, 10, 1, c.anvilLight());
        fill(g, x + 6, y + 3, 2, 2, c.lcd());
    }

    /** Voidsoft's mark at half its size, as a list row carries it beside the store's name. */
    static void smallAnvil(final GuiGraphics g, final int x, final int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        anvil(g, 0, 0);
        g.pose().popPose();
    }

    /** How good a way is, as five bars rising to the right, {@code lit} of them lit. */
    static void signal(final GuiGraphics g, final int x, final int y, final int lit) {
        final Colours c = c();
        for (int b = 0; b < 5; b++) {
            fill(g, x + b * 5, y + 6 - b, 3, 2 + b, b < lit ? c.amber() : c.signalOff());
        }
    }

    /** A song's way in: a dark slot filled as far as it has come, dimmed once it is all in. */
    static void progress(final GuiGraphics g, final int x, final int y, final int w, final int h, final double done,
                         final boolean finished) {
        final Colours c = c();
        final int filled = (int) Math.round(w * Math.clamp(done, 0.0, 1.0));
        fill(g, x, y, w, h, c.track());
        fill(g, x, y, filled, h, finished ? c.progressDone() : c.progress());
        fill(g, x, y, filled, 1, finished ? c.progressDoneTop() : c.progressTop());
    }

    /** A little steel box that is ticked, with an amber square in it, or not. */
    static void checkbox(final GuiGraphics g, final int x, final int y, final boolean ticked) {
        button(g, x, y, 9, 9, true, false);
        if (ticked) {
            fill(g, x + 2, y + 2, 5, 5, c().amber());
        }
    }

    /** Text on a display, amber or dim, with its shadow worked out from the glass it is on. */
    static void lit(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                    final int colour) {
        Draw.text(g, font, text, x, y, colour, c().lcd());
    }

    /** Text on the iron of a plate. */
    static void engraved(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                         final int colour) {
        Draw.text(g, font, text, x, y, colour, c().iron());
    }

    private static void rivet(final GuiGraphics g, final int x, final int y) {
        final Colours c = c();
        fill(g, x, y, 3, 3, c.rivet());
        fill(g, x, y, 2, 1, c.rivetHi());
        fill(g, x, y, 1, 2, c.rivetHi());
        fill(g, x + 2, y + 1, 1, 2, c.rivetLo());
        fill(g, x + 1, y + 2, 2, 1, c.rivetLo());
    }

    private static void digit(final GuiGraphics g, final int x, final int y, final int figure, final boolean on) {
        final String lit = SEGMENTS[Math.floorMod(figure, 10)];
        for (int s = 0; s < SEGMENT_AT.length; s++) {
            final int[] at = SEGMENT_AT[s];
            final boolean shown = on && lit.indexOf((char) ('a' + s)) >= 0;
            fill(g, x + at[0], y + at[1], at[2], at[3], shown ? c().amber() : c().segmentOff());
        }
    }

    /** The skin's colours. */
    record Colours(int iron, int ironHi, int ironLo, int grain,
                   int steel, int steelHi, int steelLo, int steelPressed,
                   int barActive, int barIdle, int grooveActive, int grooveIdle,
                   int rivet, int rivetHi, int rivetLo,
                   int lcd, int lcdEdge, int lcdTop,
                   int amber, int amberMid, int amberDim, int ink, int inkDim, int listInk,
                   int track, int fill, int fillDim, int positionFill, int positionDead,
                   int segmentOff, int barLow, int barMid, int barHigh, int peak, int peakDead,
                   int rowSelected,
                   int anvilLight, int anvilMid, int anvilDark,
                   int header, int rowPicked, int signalOff, int rule, int statusBar,
                   int progress, int progressTop, int progressDone, int progressDoneTop) {
    }
}
