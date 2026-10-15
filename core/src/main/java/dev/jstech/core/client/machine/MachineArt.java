/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The strokes the machine screen is drawn with, as the approved design draws them, pixel for pixel: raised blocks
 * lit from the top left and their shadows, bolts, bezels, letters engraved in the steel, aluminium plates, pilot
 * lamps, LCDs, panel meters, sight glasses, rockers, toggles, pushbuttons, thumbwheels, knobs, isometric cubes, and the
 * game's own windows, tabs and slots. A face of a mod's own machine is drawn with them too, so it matches every other.
 *
 * <p>Each takes the screen's pixels; a fill covers from its first corner to before its second, as the game's do.
 */
@PaletteHolder
public final class MachineArt {

    /** The colours of the steel face and its parts, and of the game's windows around it. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "machine_screen", new Colours(
            0xFF << 24, 0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0x555555, 0xFF << 24 | 0xC6C6C6, 0xFF << 24 | 0x8B8B8B,
            0xFF << 24 | 0x373737, 0xFF << 24 | 0x404040, 0xFF << 24 | 0x7A8088, 0xFF << 24 | 0xC9CCD0,
            0xFF << 24 | 0xE8E4D8, 0xFF << 24 | 0x5FE07A, 0xFF << 24 | 0xF0B23A, 0xFF << 24 | 0xEF6A5A,
            0xFF << 24 | 0xE8C547, 0xFF << 24 | 0x3C2C00, 0xFF << 24 | 0x343740, 0xFF << 24 | 0x7CFF7C,
            0xFF << 24 | 0x182418, 0xFF << 24 | 0x3668D2, 0xFF << 24 | 0xDC1E14, 0xFF << 24 | 0xFFECA0,
            0xFF << 24 | 0xECECE6, 0xFF << 24 | 0x468CEB, 0xFF << 24 | 0xF0AA32, 0xFF << 24 | 0x5ACD6E,
            0xFF << 24 | 0x787C82, 0xFF << 24 | 0xFFC446));
    private static final float SMALL = MachineScreenLayout.SMALL;
    /* A sight glass's width; an arrow's length, and its shaft's. */
    private static final int GLASS = 8;
    private static final int ARROW = 14;
    private static final int ARROW_SHAFT = 9;

    private MachineArt() {
    }

    /** {@code a} moved toward {@code b} by {@code t}, every channel and its alpha alike. */
    public static int mix(final int a, final int b, final double t) {
        final double s = Math.max(0.0, Math.min(1.0, t));
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            final int ca = a >>> shift & 0xFF;
            final int cb = b >>> shift & 0xFF;
            out |= (int) Math.round(ca + (cb - ca) * s) << shift;
        }
        return out;
    }

    /** {@code colour} with its alpha set to {@code alpha}. */
    public static int alpha(final int colour, final int alpha) {
        return alpha << 24 | colour & 0xFFFFFF;
    }

    public static void fill(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                            final int colour) {
        if (x1 > x0 && y1 > y0) {
            g.fill(x0, y0, x1, y1, colour);
        }
    }

    public static void hline(final GuiGraphics g, final int x0, final int x1, final int y, final int colour) {
        fill(g, x0, y, x1, y + 1, colour);
    }

    public static void vline(final GuiGraphics g, final int x, final int y0, final int y1, final int colour) {
        fill(g, x, y0, x + 1, y1, colour);
    }

    public static void dot(final GuiGraphics g, final int x, final int y, final int colour) {
        fill(g, x, y, x + 1, y + 1, colour);
    }

    public static void frame(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                             final int colour) {
        hline(g, x0, x1, y0, colour);
        hline(g, x0, x1, y1 - 1, colour);
        vline(g, x0, y0, y1, colour);
        vline(g, x1 - 1, y0, y1, colour);
    }

    /** A raised block: the base colour, its top and left lit, its bottom and right in shadow. */
    public static void shade(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                             final int base, final double hi, final double lo) {
        final Colours c = PALETTE.get();
        fill(g, x0, y0, x1, y1, base);
        hline(g, x0, x1, y0, mix(base, c.white(), hi));
        vline(g, x0, y0, y1, mix(base, c.white(), hi * 0.7));
        hline(g, x0, x1, y1 - 1, mix(base, c.black(), lo));
        vline(g, x1 - 1, y0, y1, mix(base, c.black(), lo * 0.8));
    }

    /** The same, lit and shaded as most parts are. */
    public static void shade(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                             final int base) {
        shade(g, x0, y0, x1, y1, base, 0.35, 0.45);
    }

    /** A part's shadow on the steel: one pixel down and right. */
    public static void drop(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1) {
        hline(g, x0 + 1, x1 + 1, y1, 70 << 24);
        vline(g, x1, y0 + 1, y1 + 1, 70 << 24);
    }

    public static void bolt(final GuiGraphics g, final int x, final int y) {
        fill(g, x, y, x + 3, y + 3, rgb(92, 98, 106));
        dot(g, x, y, rgb(196, 202, 208));
        dot(g, x + 2, y + 2, rgb(40, 44, 48));
        dot(g, x + 1, y + 1, rgb(140, 146, 152));
    }

    /** A recessed window's frame: dark steel, its inner edge in shadow above, lit below, a bolt in each corner. */
    public static void bezel(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                             final boolean bolts) {
        drop(g, x0, y0, x1, y1);
        shade(g, x0, y0, x1, y1, rgb(66, 71, 78), 0.4, 0.5);
        hline(g, x0 + 2, x1 - 2, y0 + 2, rgb(24, 26, 30));
        vline(g, x0 + 2, y0 + 2, y1 - 2, rgb(24, 26, 30));
        hline(g, x0 + 2, x1 - 2, y1 - 3, rgb(120, 126, 134));
        vline(g, x1 - 3, y0 + 2, y1 - 2, rgb(120, 126, 134));
        if (bolts) {
            bolt(g, x0 + 1, y0 + 1);
            bolt(g, x1 - 4, y0 + 1);
            bolt(g, x0 + 1, y1 - 4);
            bolt(g, x1 - 4, y1 - 4);
        }
    }

    /** Glass's shine across a window. */
    public static void glassShine(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1) {
        for (int i = 0; i < Math.min(x1 - x0, y1 - y0) / 2; i++) {
            dot(g, x0 + 2 + i, y0 + 1 + i / 3, 30 << 24 | 0xFFFFFF);
        }
        hline(g, x0, x1, y0, 40 << 24 | 0xFFFFFF);
    }

    /** Small text at the face's scale, its top left at ({@code x}, {@code y}). */
    public static void text(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                            final int colour) {
        Draw.textScaled(g, font, text, x, y, colour, SMALL);
    }

    /** Small text centred on {@code x}. */
    public static void textCentred(final GuiGraphics g, final Font font, final String text, final int x,
                                   final int y, final int colour) {
        text(g, font, text, x - width(font, text) / 2, y, colour);
    }

    /** How wide small text is. */
    public static int width(final Font font, final String text) {
        return (int) Math.ceil(font.width(text) * SMALL);
    }

    /** Letters cut into the steel: dark, the light catching their lower lip. */
    public static void engrave(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                               final boolean centre) {
        final Colours c = PALETTE.get();
        final int lip = mix(c.steel(), c.white(), 0.4);
        final int ink = mix(c.steel(), c.black(), 0.55);
        if (centre) {
            textCentred(g, font, text, x, y + 1, lip);
            textCentred(g, font, text, x, y, ink);
        } else {
            text(g, font, text, x, y + 1, lip);
            text(g, font, text, x, y, ink);
        }
    }

    /** An aluminium plate, engraved, with a screw at each end; how wide it came. */
    public static int plate(final GuiGraphics g, final Font font, final int x, final int y, final String text,
                            final int ground, final int ink) {
        final Colours c = PALETTE.get();
        final int w = width(font, text) + 16;
        drop(g, x, y, x + w, y + MachineScreenLayout.PLATE_HEIGHT);
        shade(g, x, y, x + w, y + MachineScreenLayout.PLATE_HEIGHT, ground, 0.45, 0.35);
        for (final int sx : new int[] {x + 3, x + w - 5}) {
            fill(g, sx, y + 5, sx + 2, y + 7, mix(ground, c.black(), 0.45));
            dot(g, sx, y + 5, mix(ground, c.black(), 0.2));
        }
        text(g, font, text, x + 8, y + 4, mix(ground, c.white(), 0.5));
        text(g, font, text, x + 8, y + 3, ink);
        return w;
    }

    /** A pilot lamp: a domed lens in a chrome ring, glowing when lit. */
    public static void lamp(final GuiGraphics g, final int x, final int y, final int colour, final boolean lit) {
        final Colours c = PALETTE.get();
        if (lit) {
            for (int gy = -2; gy < 9; gy++) {
                for (int gx = -2; gx < 9; gx++) {
                    if (gx >= 0 && gx < 7 && gy >= 0 && gy < 7) {
                        continue;
                    }
                    final int d = Math.max(Math.abs(gx - 3), Math.abs(gy - 3));
                    if (d <= 5) {
                        dot(g, x + gx, y + gy, alpha(colour, d == 4 ? 40 : 22));
                    }
                }
            }
        }
        fill(g, x - 1, y - 1, x + 8, y + 8, rgb(60, 64, 70));
        frame(g, x - 1, y - 1, x + 8, y + 8, rgb(170, 176, 182));
        dot(g, x - 1, y - 1, rgb(60, 64, 70));
        dot(g, x + 7, y + 7, rgb(60, 64, 70));
        final int body = lit ? colour : mix(colour, rgb(24, 24, 24), 0.72);
        fill(g, x, y, x + 7, y + 7, body);
        dot(g, x, y, mix(body, c.black(), 0.3));
        dot(g, x + 6, y + 6, mix(body, c.black(), 0.3));
        dot(g, x + 6, y, mix(body, c.black(), 0.3));
        dot(g, x, y + 6, mix(body, c.black(), 0.3));
        fill(g, x + 1, y + 1, x + 3, y + 3, mix(body, c.white(), lit ? 0.65 : 0.25));
    }

    /**
     * An LCD behind glass in its bezel, its lines in green, the first bright and the rest dimmer; the first in the
     * game's font when {@code big}, every line small otherwise.
     */
    public static void lcd(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                           final List<String> lines, final boolean on, final boolean big) {
        final Colours c = PALETTE.get();
        bezel(g, x - 3, y - 3, x + w + 3, y + h + 3, false);
        final int ground = c.lcdGround();
        fill(g, x, y, x + w, y + h, ground);
        for (int gy = y; gy < y + h; gy += 2) {
            hline(g, x, x + w, gy, mix(ground, c.black(), 0.18));
        }
        for (int i = 0; i < lines.size(); i++) {
            int colour = on ? c.lcdGlow() : mix(c.lcdGlow(), ground, 0.55);
            if (i > 0) {
                colour = mix(colour, ground, 0.25);
            }
            if (i == 0 && big) {
                Draw.text(g, font, font.plainSubstrByWidth(lines.get(i), w - 6), x + 4, y + 3, colour);
            } else {
                text(g, font, fit(font, lines.get(i), w - 6), x + 4, y + 3 + i * 10, colour);
            }
        }
        glassShine(g, x, y, x + w, y + h);
    }

    /**
     * An edgewise panel meter: a cream face with its scale and a red band at the top, the needle at {@code share} of
     * it, behind glass in a bezel.
     */
    public static void panelMeter(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                  final int h, final double share, final List<String> marks, final boolean redBand) {
        final Colours c = PALETTE.get();
        bezel(g, x - 4, y - 4, x + w + 4, y + h + 4, false);
        for (int yy = y; yy < y + h; yy++) {
            hline(g, x, x + w, yy, mix(c.meterFace(), c.black(), 0.06 * (yy - y) / h));
        }
        if (redBand) {
            fill(g, x + w - 4, y + 2, x + w - 2, y + 2 + (h - 6) / 8, rgb(200, 60, 50));
        }
        final int span = h - 6;
        for (int i = 0; i < marks.size(); i++) {
            final int ty = y + h - 3 - i * span / (marks.size() - 1);
            hline(g, x + 1, x + 6, ty, rgb(50, 50, 50));
            text(g, font, marks.get(i), x + 8, ty - 2, rgb(50, 50, 50));
            if (i < marks.size() - 1) {
                for (int j = 1; j < 4; j++) {
                    hline(g, x + 1, x + 4, ty - j * span / (marks.size() - 1) / 4, rgb(110, 110, 110));
                }
            }
        }
        final int ny = y + h - 3 - (int) Math.round(Math.max(0.0, Math.min(1.0, share)) * span);
        hline(g, x + 1, x + w - 2, ny + 1, 50 << 24);
        hline(g, x, x + w - 2, ny, rgb(196, 30, 24));
        fill(g, x + w - 3, ny - 1, x + w - 1, ny + 2, rgb(40, 40, 40));
        glassShine(g, x, y, x + w, y + h);
    }

    /** A sight glass between bolted brackets, {@code share} full of {@code colour}, bubbles rising while it works. */
    public static void sightGlass(final GuiGraphics g, final int x, final int y, final int h, final double share,
                                  final int colour, final float tick, final boolean working) {
        final Colours c = PALETTE.get();
        glassBrackets(g, x, y, h);
        final int top = glassTop(y, h, share);
        for (int yy = top; yy < y + h; yy++) {
            hline(g, x, x + GLASS, yy, mix(colour, c.black(), 0.25 * (yy - top) / Math.max(1, y + h - top)));
        }
        glassFront(g, x, y, h, top, colour, tick, working);
    }

    /** A sight glass's two bolted brackets and the dark of the empty glass between them. */
    public static void glassBrackets(final GuiGraphics g, final int x, final int y, final int h) {
        for (final int by : new int[] {y - 4, y + h}) {
            drop(g, x - 3, by, x + 11, by + 4);
            shade(g, x - 3, by, x + 11, by + 4, rgb(96, 102, 110));
            bolt(g, x - 2, by + 1);
            bolt(g, x + 7, by + 1);
        }
        fill(g, x, y, x + GLASS, y + h, rgb(30, 38, 46));
    }

    /** The top of what fills a sight glass {@code share} full. */
    public static int glassTop(final int y, final int h, final double share) {
        return y + h - (int) Math.round(h * Math.max(0.0, Math.min(1.0, share)));
    }

    /**
     * What lies on the front of a sight glass filled to {@code top} with {@code colour}: its surface, the bubbles
     * rising while it works, and the glass's shine and level marks.
     */
    public static void glassFront(final GuiGraphics g, final int x, final int y, final int h, final int top,
                                  final int colour, final float tick, final boolean working) {
        final Colours c = PALETTE.get();
        if (top < y + h) {
            hline(g, x, x + GLASS, top, mix(colour, c.white(), 0.5));
        }
        if (working && y + h - top > 2) {
            for (int k = 0; k < 3; k++) {
                final int by = y + h - 2 - (int) ((tick * 1.5 + k * 9) % Math.max(1, y + h - top - 2));
                dot(g, x + 2 + k * 3 % 5, by, mix(colour, c.white(), 0.6));
            }
        }
        vline(g, x + 1, y, y + h, 70 << 24 | 0xFFFFFF);
        vline(g, x + 2, y, y + h, 30 << 24 | 0xFFFFFF);
        for (int i = 1; i < 4; i++) {
            hline(g, x + 6, x + GLASS, y + h * i / 4, 110 << 24 | 0xFFFFFF);
        }
    }

    /**
     * An arrow from a lane's inputs to its outputs, its tip at the right, lit from its tail to {@code share} of the
     * way: a shaft five pixels tall, and a head nine.
     */
    public static void arrow(final GuiGraphics g, final int x, final int cy, final double share, final int lit) {
        final int dark = rgb(46, 50, 56);
        final int reach = x + (int) Math.round(ARROW * Math.max(0.0, Math.min(1.0, share)));
        for (int col = 0; col < ARROW; col++) {
            final int half = col < ARROW_SHAFT ? 2 : ARROW - 1 - col;
            final int px = x + col;
            vline(g, px, cy - half, cy + half + 1, px < reach ? lit : dark);
            dot(g, px, cy + half + 1, 60 << 24 | 0xFFFFFF);
        }
    }

    /** {@code text} cut to what fits {@code width} pixels at the face's small scale. */
    public static String fit(final Font font, final String text, final int width) {
        return font.plainSubstrByWidth(text, (int) Math.floor(width / SMALL));
    }

    /** A bakelite rocker on a chrome escutcheon, up for on. */
    public static void rocker(final GuiGraphics g, final int x, final int y, final boolean on) {
        drop(g, x, y, x + 14, y + 22);
        shade(g, x, y, x + 14, y + 22, rgb(176, 182, 190), 0.4, 0.5);
        fill(g, x + 3, y + 3, x + 11, y + 19, rgb(40, 42, 46));
        final int ky = on ? y + 4 : y + 11;
        shade(g, x + 4, ky, x + 10, ky + 7, rgb(34, 34, 38), 0.6, 0.3);
        hline(g, x + 5, x + 9, ky + 1, rgb(110, 110, 118));
    }

    /** A small bat-handle toggle on a chrome plate, up for on. */
    public static void toggle(final GuiGraphics g, final int x, final int y, final boolean on) {
        final Colours c = PALETTE.get();
        shade(g, x, y, x + 8, y + 12, rgb(176, 182, 190), 0.4, 0.5);
        fill(g, x + 3, y + 5, x + 5, y + 7, rgb(40, 40, 44));
        if (on) {
            fill(g, x + 3, y + 1, x + 5, y + 6, rgb(210, 214, 220));
            dot(g, x + 3, y + 1, c.white());
        } else {
            fill(g, x + 3, y + 6, x + 5, y + 11, rgb(210, 214, 220));
        }
    }

    /** An illuminated square pushbutton in a chrome bezel, its legend engraved beside it. */
    public static void pushbutton(final GuiGraphics g, final Font font, final int x, final int y, final boolean lit,
                                  final int colour, final String label) {
        final Colours c = PALETTE.get();
        drop(g, x, y, x + 14, y + 12);
        shade(g, x, y, x + 14, y + 12, rgb(176, 182, 190), 0.4, 0.5);
        final int body = lit ? colour : mix(colour, rgb(30, 30, 30), 0.7);
        fill(g, x + 2, y + 2, x + 12, y + 10, body);
        hline(g, x + 3, x + 11, y + 3, mix(body, c.white(), lit ? 0.5 : 0.15));
        if (lit) {
            for (int gx = x - 1; gx < x + 15; gx++) {
                dot(g, gx, y - 1, alpha(colour, 40));
                dot(g, gx, y + 12, alpha(colour, 40));
            }
        }
        engrave(g, font, label, x + 17, y + 3, false);
    }

    /** A thumbwheel counter: a row of drums, each with its white digit. */
    public static void thumbwheel(final GuiGraphics g, final Font font, final int x, final int y,
                                  final String digits) {
        final int w = digits.length() * MachineScreenLayout.WHEEL_DIGIT + 2;
        bezel(g, x - 2, y - 2, x + w + 2, y + 13, false);
        for (int i = 0; i < digits.length(); i++) {
            final int dx = x + 1 + i * MachineScreenLayout.WHEEL_DIGIT;
            fill(g, dx, y, dx + 6, y + 11, rgb(28, 28, 30));
            hline(g, dx, dx + 6, y, rgb(60, 60, 64));
            hline(g, dx, dx + 6, y + 10, rgb(10, 10, 12));
            Draw.text(g, font, String.valueOf(digits.charAt(i)), dx + 1, y + 2, PALETTE.get().wheelDigit());
        }
    }

    /** A shaded disc, lit from the top left, with its shadow on the plate. */
    public static void disc(final GuiGraphics g, final int cx, final int cy, final int r, final int base,
                            final boolean shine) {
        final Colours c = PALETTE.get();
        for (int y = -r - 1; y < r + 2; y++) {
            for (int x = -r - 1; x < r + 2; x++) {
                final double d = Math.hypot(x, y);
                if (d <= r + 0.3) {
                    final double lit = -(x + y) / (2.4 * r);
                    int colour = mix(base, lit > 0 ? c.white() : c.black(), Math.abs(lit) * 0.7);
                    if (d > r - 1) {
                        colour = mix(colour, c.black(), 0.35);
                    }
                    dot(g, cx + x, cy + y, colour);
                } else if (d <= r + 1.4 && x >= 0 && y >= 0) {
                    dot(g, cx + x, cy + y, 70 << 24);
                }
            }
        }
        if (shine) {
            dot(g, cx - r / 2, cy - r / 2, mix(base, c.white(), 0.7));
        }
    }

    /**
     * A rotary selector: a fluted black knob with a white pointer at {@code angle}, its positions engraved round it,
     * the one it points at lit, and its title above.
     */
    public static void knob(final GuiGraphics g, final Font font, final int cx, final int cy, final int r,
                            final double angle, final List<KnobLabel> labels, final String title) {
        final Colours c = PALETTE.get();
        for (int k = 0; k < 24; k++) {
            final double a = k * Math.PI / 12;
            dot(g, cx + (int) Math.round((r + 3) * Math.cos(a)), cy + (int) Math.round((r + 3) * Math.sin(a)),
                    rgb(70, 74, 80));
        }
        disc(g, cx, cy, r, rgb(44, 44, 48), true);
        for (int k = 0; k < 12; k++) {
            final double a = k * Math.PI / 6;
            dot(g, cx + (int) Math.round((r - 1) * Math.cos(a)), cy + (int) Math.round((r - 1) * Math.sin(a)),
                    rgb(24, 24, 26));
        }
        for (int s = 2; s < r - 1; s++) {
            dot(g, cx + (int) Math.round(s * Math.cos(angle)), cy + (int) Math.round(s * Math.sin(angle)),
                    rgb(236, 236, 230));
        }
        for (final KnobLabel label : labels) {
            final int lx = cx + (int) Math.round((r + 9) * Math.cos(label.angle()));
            final int ly = cy + (int) Math.round((r + 8) * Math.sin(label.angle())) - 3;
            if (label.on()) {
                textCentred(g, font, label.text(), lx, ly, c.lit());
            } else {
                engrave(g, font, label.text(), lx, ly, true);
            }
            final int tx = cx + (int) Math.round((r + 3) * Math.cos(label.angle()));
            final int ty = cy + (int) Math.round((r + 3) * Math.sin(label.angle()));
            dot(g, tx, ty, label.on() ? rgb(220, 220, 214) : rgb(40, 44, 50));
        }
        if (!title.isEmpty()) {
            engrave(g, font, title, cx, cy - r - 17, true);
        }
    }

    /** Fills a small convex polygon row by row, as the design does. */
    public static void polygon(final GuiGraphics g, final int[][] corners, final int colour) {
        int top = Integer.MAX_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (final int[] corner : corners) {
            top = Math.min(top, corner[1]);
            bottom = Math.max(bottom, corner[1]);
        }
        for (int y = top; y <= bottom; y++) {
            double least = Double.MAX_VALUE;
            double most = -Double.MAX_VALUE;
            int found = 0;
            for (int i = 0; i < corners.length; i++) {
                final int[] a = corners[i];
                final int[] b = corners[(i + 1) % corners.length];
                if (a[1] != b[1] && Math.min(a[1], b[1]) <= y && y <= Math.max(a[1], b[1])) {
                    final double x = a[0] + (b[0] - a[0]) * (double) (y - a[1]) / (b[1] - a[1]);
                    least = Math.min(least, x);
                    most = Math.max(most, x);
                    found++;
                }
            }
            if (found >= 2) {
                hline(g, (int) Math.round(least), (int) Math.round(most) + 1, y, colour);
            }
        }
    }

    /**
     * An isometric cube of a machine, its top, left and right faces each tinted by what it does and named with its
     * role, and the face {@code ringed} picks ringed in white (-1 for none); its top corner at ({@code ox},
     * {@code oy}) and its half width {@code s}, the faces as {@link MachineScreenLayout#cubeFace} gives them.
     */
    public static void isoCube(final GuiGraphics g, final Font font, final int ox, final int oy, final int s,
                               final List<CubeFace> faces, final int ringed) {
        final Colours c = PALETTE.get();
        final double[] light = {0.18, -0.05, -0.25};
        for (int f = 0; f < faces.size(); f++) {
            final int[][] corners = MachineScreenLayout.cubeFace(f, ox, oy, s);
            final CubeFace face = faces.get(f);
            final int base = mix(mix(rgb(118, 124, 132), face.colour(), 0.55), light[f] > 0 ? c.white() : c.black(),
                    Math.abs(light[f]));
            polygon(g, corners, base);
            outline(g, corners, mix(base, c.black(), 0.45));
            int cx = 0;
            int cy = 0;
            for (final int[] corner : corners) {
                cx += corner[0];
                cy += corner[1];
            }
            cx /= corners.length;
            cy /= corners.length;
            textCentred(g, font, face.name(), cx, cy - 6, mix(base, c.black(), 0.55));
            textCentred(g, font, face.role(), cx, cy + 1, mix(face.colour(), c.white(), 0.6));
        }
        if (ringed >= 0) {
            outline(g, MachineScreenLayout.cubeFace(ringed, ox, oy, s), c.white());
        }
    }

    /** The edges of a polygon, a pixel wide. */
    public static void outline(final GuiGraphics g, final int[][] corners, final int colour) {
        for (int i = 0; i < corners.length; i++) {
            final int[] a = corners[i];
            final int[] b = corners[(i + 1) % corners.length];
            final int steps = Math.max(Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])), 1);
            for (int k = 0; k <= steps; k++) {
                dot(g, (int) Math.round(a[0] + (b[0] - a[0]) * (double) k / steps),
                        (int) Math.round(a[1] + (b[1] - a[1]) * (double) k / steps), colour);
            }
        }
    }

    /**
     * A window as the game draws its own: the black outline stepping in at each corner, a white bevel two pixels deep
     * on the top and left, a dark one on the bottom and right, and the grey ground; open at the top from {@code xa} to
     * {@code xb} where the shown tab joins it, or closed for {@code xa} equal to {@code xb}.
     */
    public static void window(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                              final int xa, final int xb) {
        final Colours c = PALETTE.get();
        fill(g, x0 + 2, y0 + 1, x1 - 2, y1 - 1, c.ground());
        fill(g, x0 + 1, y0 + 2, x1 - 1, y1 - 2, c.ground());
        hline(g, x0 + 2, x1 - 2, y0, c.black());
        hline(g, x0 + 2, x1 - 2, y1 - 1, c.black());
        vline(g, x0, y0 + 2, y1 - 2, c.black());
        vline(g, x1 - 1, y0 + 2, y1 - 2, c.black());
        dot(g, x0 + 1, y0 + 1, c.black());
        dot(g, x1 - 2, y0 + 1, c.black());
        dot(g, x0 + 1, y1 - 2, c.black());
        dot(g, x1 - 2, y1 - 2, c.black());
        fill(g, x0 + 2, y0 + 1, x1 - 3, y0 + 3, c.white());
        fill(g, x0 + 1, y0 + 2, x0 + 3, y1 - 3, c.white());
        fill(g, x0 + 3, y1 - 3, x1 - 2, y1 - 1, c.dark());
        fill(g, x1 - 3, y0 + 3, x1 - 1, y1 - 2, c.dark());
        dot(g, x1 - 3, y0 + 2, c.ground());
        dot(g, x0 + 2, y1 - 3, c.ground());
        if (xb > xa) {
            fill(g, xa, y0, xb, y0 + 3, c.ground());
            fill(g, xa, y0, xa + 2, y0 + 3, c.white());
        }
    }

    /** A slot as the game draws one: 18 square, dark on its top and left edges, white on its bottom and right. */
    public static void slot(final GuiGraphics g, final int x, final int y) {
        final Colours c = PALETTE.get();
        fill(g, x, y, x + 18, y + 18, c.slot());
        hline(g, x, x + 17, y, c.slotDark());
        vline(g, x, y, y + 17, c.slotDark());
        hline(g, x + 1, x + 18, y + 17, c.white());
        vline(g, x + 17, y + 1, y + 18, c.white());
    }

    /**
     * A tab on the window's top edge, as the creative inventory's, its top corners rounded: the shown one taller and
     * running into the window, the others behind it.
     */
    public static void tab(final GuiGraphics g, final int x, final int y, final boolean active) {
        final Colours c = PALETTE.get();
        final int h = active ? MachineScreenLayout.TAB_ACTIVE_HEIGHT : MachineScreenLayout.TAB_HEIGHT;
        fill(g, x + 2, y + 1, x + 26, y + h, c.ground());
        fill(g, x + 1, y + 2, x + 27, y + h, c.ground());
        hline(g, x + 2, x + 26, y, c.black());
        dot(g, x + 1, y + 1, c.black());
        dot(g, x + 26, y + 1, c.black());
        vline(g, x, y + 2, y + h, c.black());
        vline(g, x + 27, y + 2, y + h, c.black());
        fill(g, x + 2, y + 1, x + 25, y + 3, c.white());
        fill(g, x + 1, y + 2, x + 3, y + h, c.white());
        fill(g, x + 25, y + 3, x + 27, y + h, c.dark());
        if (!active) {
            hline(g, x, x + 28, y + h - 1, c.black());
            fill(g, x + 1, y + h - 3, x + 27, y + h - 1, mix(c.ground(), c.dark(), 0.35));
        }
    }

    /** An opaque colour from its channels. */
    public static int rgb(final int r, final int g, final int b) {
        return 0xFF << 24 | r << 16 | g << 8 | b;
    }

    /**
     * One position of a knob: its word, its angle and whether the knob points at it.
     *
     * @param text  its word
     * @param angle its angle, in radians, clockwise from the right
     * @param on    whether the knob points at it
     */
    public record KnobLabel(String text, double angle, boolean on) {
    }

    /**
     * One face of an isometric cube: its name, its role and the colour of its role.
     *
     * @param name   the face's name
     * @param role   what it does
     * @param colour the colour of what it does
     */
    public record CubeFace(String name, String role, int colour) {
    }

    /**
     * The machine screen's colours.
     *
     * @param black      outlines, and the shadows things are mixed toward
     * @param white      bevels, and the light things are mixed toward
     * @param dark       a window's dark bevel
     * @param ground     a window's grey
     * @param slot       a slot's grey
     * @param slotDark   a slot's dark edges
     * @param title      a window's title
     * @param steel      the face's steel, which engraving is mixed from
     * @param aluminium  the name plate's ground
     * @param meterFace  a panel meter's cream face
     * @param lampRun    the RUN lamp, and what lights for work done
     * @param lampWait   the WAIT lamp, and what lights amber
     * @param lampFault  the FAULT lamp, and what lights red
     * @param classPlate the class plate's yellow ground
     * @param classInk   the class plate's letters
     * @param plateInk   the name plate's letters
     * @param lcdGlow    an LCD's green
     * @param lcdGround  an LCD's dark
     * @param water      a sight glass's liquid when the liquid has no colour of its own
     * @param redstone   a lit lamp of the signal in
     * @param lit        the word a knob points at
     * @param wheelDigit a thumbwheel's digits
     * @param faceIn     a face that takes in, on the Sides tab's cubes
     * @param faceOut    a face that gives out
     * @param faceBoth   a face that does both
     * @param faceOff    a face that does neither
     * @param pushLit    a lit pushbutton of a resource
     */
    public record Colours(int black, int white, int dark, int ground, int slot, int slotDark, int title, int steel,
                          int aluminium, int meterFace, int lampRun, int lampWait, int lampFault, int classPlate,
                          int classInk,
                          int plateInk, int lcdGlow, int lcdGround, int water, int redstone, int lit,
                          int wheelDigit, int faceIn, int faceOut, int faceBoth, int faceOff, int pushLit) {
    }
}
