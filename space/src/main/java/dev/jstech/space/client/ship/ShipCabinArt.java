/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The live instruments of a starship's cockpit, drawn as the approved cockpits paint them, each into its own region
 * of a face: the horizon that banks and pitches with the ship, the map turning under its track, the engines' load
 * bars, the working ship's green CRT, the master caution, Kosmozavod's guarded switches with their guards lifted when
 * the ship is armed, and the dials' needles.
 */
@PaletteHolder
public final class ShipCabinArt {

    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "ship_cabin", new Colours(
            0xFF2878C8, 0xFF966032, 0xFFF0F0F0, 0xFFFAC828, 0xFF0E281A, 0xFF1E5A32, 0xFF5AE678, 0xFF0A100E,
            0xFF5ADC78, 0xFFF0B428, 0xFF081A0C, 0xFF50DC6E, 0xFF463814, 0xFFFFAA28, 0xFF489C98, 0xFFB02824,
            0xFFDCDCD6, 0xFFF0F0EA, 0xFFE2DECE, 0xFF282828, 0xFFC8281E, 0xFF161618, 0xFFECECE6, 0xFF5AE678,
            0xFFF04632));

    /* How far the horizon moves for a degree of pitch, in its window's heights. */
    private static final float PITCH_SHARE = 1F / 90F;
    /* The map's grid, cells across its window, and how far it turns a degree of heading. */
    private static final int MAP_CELLS = 9;
    /* The bars, and the one shown amber. */
    private static final int BARS = 5;
    private static final int AMBER_BAR = 3;
    /* The CRT's lines. */
    private static final int CRT_LINES = 14;
    /* A guarded switch's cell on its plate, and the plate's size, in the plate's texels. */
    private static final int GUARD_CELL_U = 8;
    private static final int GUARD_CELL_V = 9;
    private static final float GUARDS_U = 2.2F * 32;
    private static final float GUARDS_V = 0.55F * 32;
    /* A dial's needle sweeps this far, in degrees from straight down, clockwise. */
    private static final float DIAL_FROM = 45;
    private static final float DIAL_SWEEP = 270;
    private static final int NEEDLE_STEPS = 24;

    private ShipCabinArt() {
    }

    /** The artificial horizon across {@code w} by {@code h}: sky over ground, banked by the roll, moved by the pitch. */
    public static void horizon(final GuiGraphics g, final int w, final int h, final float pitch, final float roll) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.sky());
        final float slope = (float) Math.tan(Math.toRadians(Math.max(-60, Math.min(60, roll))));
        final float middle = h * (0.5F + pitch * PITCH_SHARE);
        for (int x = 0; x < w; x++) {
            final int line = Math.round(middle + slope * (x - w / 2F));
            if (line < h) {
                g.fill(x, Math.max(0, line), x + 1, h, c.ground());
            }
            g.fill(x, Math.max(0, line - 1), x + 1, Math.max(0, Math.min(h, line + 1)), c.white());
        }
        g.fill(w * 32 / 100, h / 2 - 1, w * 68 / 100, h / 2 + 1, c.marker());
    }

    /** The moving map across {@code w} by {@code h}: its grid turned under the ship by {@code heading}. */
    public static void map(final GuiGraphics g, final int w, final int h, final float heading) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.mapGround());
        final float cell = w / (float) MAP_CELLS;
        final float shift = (heading / 360F) * w;
        final int whole = (int) Math.floor(shift / cell);
        final float part = shift - whole * cell;
        for (int i = -1; i <= MAP_CELLS + 1; i++) {
            for (int j = -1; j <= MAP_CELLS + 1; j++) {
                if (Math.floorMod((i + whole) * 3 + j * 2, 5) == 0) {
                    final float x = i * cell - part;
                    g.fill(Math.round(x), Math.round(j * cell * h / w), Math.round(x + cell),
                            Math.round((j + 1) * cell * h / w), c.mapLand());
                }
            }
        }
        final int r = Math.min(w, h) * 28 / 100;
        ring(g, w / 2, h * 45 / 100, r, Math.max(1, w / 60), c.mapRing());
        g.fill(w / 2 - 1, h * 45 / 100 - 1, w / 2 + 2, h * 45 / 100 + 2, c.white());
        g.fill(w / 2, 0, w / 2 + Math.max(1, w / 80), h * 45 / 100, c.marker());
    }

    /** The engines' load across {@code w} by {@code h}: five bars filled by {@code load}, 0 to 1. */
    public static void bars(final GuiGraphics g, final int w, final int h, final float load) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.barsGround());
        for (int j = 0; j < BARS; j++) {
            final float level = Math.min(1, 0.3F + 0.13F * j * (1 - load) + load * (0.55F + 0.08F * j));
            final int x = Math.round(w * (0.12F + 0.18F * j));
            final int half = Math.max(1, w * 6 / 100);
            g.fill(x - half, Math.round(h * (1 - level)), x + half, h, j == AMBER_BAR ? c.barAmber() : c.bar());
        }
        g.fill(0, h / 10 - 1, w, h / 10 + 1, c.bar());
    }

    /** The working ship's CRT across {@code w} by {@code h}: lines of text that change as the clock runs. */
    public static void crt(final GuiGraphics g, final int w, final int h, final int clock) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.crtGround());
        final float line = h / (float) CRT_LINES;
        for (int k = 0; k < CRT_LINES; k += 2) {
            final long seed = (k * 0x9E3779B97F4A7C15L) ^ (clock / 2L * 0x632BE59BD9B4E019L);
            final float share = 0.2F + 0.8F * ((seed >>> 11) & 0xFFFF) / 65535F;
            g.fill(w * 8 / 100, Math.round(k * line), w * 8 / 100 + Math.round(0.8F * w * share),
                    Math.round(k * line + line * 0.7F), c.crtText());
        }
    }

    /** The master caution across {@code w} by {@code h}: amber when lit, dark when all is well. */
    public static void caution(final GuiGraphics g, final int w, final int h, final boolean lit) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, lit ? c.cautionLit() : c.cautionDark());
    }

    /**
     * Kosmozavod's round lamp {@code index} across {@code w} by {@code h}, green, amber and red in turn along the row:
     * bright when {@code lit}, a dim glass when not.
     */
    public static void lamp(final GuiGraphics g, final int w, final int h, final int index, final boolean lit) {
        final Colours c = PALETTE.get();
        final int[] colours = {c.lampGreen(), c.cautionLit(), c.lampRed()};
        final int colour = colours[Math.floorMod(index, colours.length)];
        g.fill(0, 0, w, h, lit ? colour : dim(colour));
    }

    /**
     * Kosmozavod's guarded switches across {@code w} by {@code h}, the plate's texels scaled to it: the red guards
     * down over them, or, with the ship {@code armed}, half of them lifted and their levers thrown.
     */
    public static void guards(final GuiGraphics g, final int w, final int h, final boolean armed) {
        final Colours c = PALETTE.get();
        g.fill(0, 0, w, h, c.turquoise());
        final float su = w / GUARDS_U;
        final float sv = h / GUARDS_V;
        for (int cu = 0; cu * GUARD_CELL_U < GUARDS_U; cu++) {
            for (int cv = 0; cv * GUARD_CELL_V < GUARDS_V; cv++) {
                final float u0 = cu * GUARD_CELL_U;
                final float v0 = cv * GUARD_CELL_V;
                final boolean lifted = armed && Math.floorMod(cu * 7 + cv * 3, 2) == 0;
                if (lifted) {
                    box(g, su, sv, u0 + 2, v0, u0 + 6, v0 + 2, c.guard());
                    box(g, su, sv, u0 + 3, v0 + 2, u0 + 5, v0 + 7, c.lever());
                } else {
                    box(g, su, sv, u0 + 2, v0 + 2, u0 + 6, v0 + 7, c.guard());
                    box(g, su, sv, u0 + 3, v0 + 3, u0 + 5, v0 + 6, c.lever());
                }
                box(g, su, sv, u0, v0 + 8, u0 + 7, v0 + 9, c.label());
            }
        }
    }

    /**
     * A dial across {@code w} by {@code h}: its face, light or black as its maker paints it, and its needle at
     * {@code value}, 0 to 1 of its scale.
     */
    public static void dial(final GuiGraphics g, final int w, final int h, final boolean black, final float value) {
        final Colours c = PALETTE.get();
        final int r = Math.min(w, h) / 2;
        ring(g, w / 2, h / 2, r, r, black ? c.blackFace() : c.lightFace());
        ring(g, w / 2, h / 2, r, Math.max(1, r / 8), c.rim());
        final double angle = Math.toRadians(DIAL_FROM + DIAL_SWEEP * Math.max(0, Math.min(1, value)));
        final int needle = black ? c.needleLight() : c.needleRed();
        for (int s = 0; s <= NEEDLE_STEPS; s++) {
            final float along = s / (float) NEEDLE_STEPS * r * 0.85F;
            final int x = Math.round(w / 2F - (float) Math.sin(angle) * along);
            final int y = Math.round(h / 2F + (float) Math.cos(angle) * along);
            final int half = Math.max(1, r / 14);
            g.fill(x - half, y - half, x + half, y + half, needle);
        }
    }

    /* A ring round (cx, cy), {@code thick} wide inside radius {@code r}, drawn in rows. */
    private static void ring(final GuiGraphics g, final int cx, final int cy, final int r, final int thick,
                             final int colour) {
        final int inner = Math.max(0, r - thick);
        for (int dy = -r; dy <= r; dy++) {
            final int outer = (int) Math.floor(Math.sqrt((double) r * r - (double) dy * dy));
            final int in = inner > Math.abs(dy) ? (int) Math.floor(Math.sqrt((double) inner * inner
                    - (double) dy * dy)) : -1;
            if (in < 0) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, colour);
            } else {
                g.fill(cx - outer, cy + dy, cx - in, cy + dy + 1, colour);
                g.fill(cx + in + 1, cy + dy, cx + outer + 1, cy + dy + 1, colour);
            }
        }
    }

    /* A lamp's colour seen through its unlit glass. */
    private static int dim(final int colour) {
        final int r = (colour >> 16 & 0xFF) * 3 / 10;
        final int gr = (colour >> 8 & 0xFF) * 3 / 10;
        final int b = (colour & 0xFF) * 3 / 10;
        return (colour >>> 24) << 24 | r << 16 | gr << 8 | b;
    }

    /* A box given in the plate's texels, scaled to the picture's pixels. */
    private static void box(final GuiGraphics g, final float su, final float sv, final float u0, final float v0,
                            final float u1, final float v1, final int colour) {
        g.fill(Math.round(u0 * su), Math.round(v0 * sv), Math.round(u1 * su), Math.round(v1 * sv), colour);
    }

    /**
     * The cockpits' colours.
     *
     * @param sky         the horizon's sky
     * @param ground      the horizon's ground
     * @param white       the horizon's line and the map's ship
     * @param marker      the horizon's wings and the map's track
     * @param mapGround   the map's ground
     * @param mapLand     the map's land
     * @param mapRing     the map's range ring
     * @param barsGround  the bars' window
     * @param bar         a bar
     * @param barAmber    the amber bar
     * @param crtGround   the CRT's window
     * @param crtText     the CRT's lines
     * @param cautionDark the master caution unlit
     * @param cautionLit  the master caution lit
     * @param turquoise   Kosmozavod's panel
     * @param guard       a switch's red guard
     * @param lever       a switch's lever
     * @param label       a switch's label
     * @param lightFace   a light dial's face
     * @param rim         a dial's rim
     * @param needleRed   a light dial's needle
     * @param blackFace   a black dial's face
     * @param needleLight a black dial's needle
     * @param lampGreen   a green lamp lit
     * @param lampRed     a red lamp lit
     */
    public record Colours(int sky, int ground, int white, int marker, int mapGround, int mapLand, int mapRing,
                          int barsGround, int bar, int barAmber, int crtGround, int crtText,
                          int cautionDark, int cautionLit, int turquoise, int guard, int lever, int label,
                          int lightFace, int rim, int needleRed, int blackFace, int needleLight, int lampGreen,
                          int lampRed) {
    }
}
