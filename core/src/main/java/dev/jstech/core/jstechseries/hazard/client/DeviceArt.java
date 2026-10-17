/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The faces of the Geiger counter and the pocket dosimeter, drawn live over their cases: the needle, the figures, the
 * lamps and the words, in the player's language. Each is drawn in units of {@code s}, 1 on the HUD and 2 held up, over
 * the case's picture of that size; every place below is the case's at a unit of 1.
 */
@PaletteHolder
public final class DeviceArt {

    /** The Geiger counter's case at each size, and how big it is at a unit of 1. */
    public static final ResourceLocation GEIGER = texture("geiger_counter");
    public static final ResourceLocation GEIGER_BIG = texture("geiger_counter_big");
    public static final int GEIGER_WIDTH = 126;
    public static final int GEIGER_HEIGHT = 74;
    /** The dosimeter's case at each size, and how big it is at a unit of 1. */
    public static final ResourceLocation DOSIMETER = texture("pocket_dosimeter");
    public static final ResourceLocation DOSIMETER_BIG = texture("pocket_dosimeter_big");
    public static final int DOSIMETER_WIDTH = 66;
    public static final int DOSIMETER_HEIGHT = 56;
    /** The Geiger counter's buttons, each its top left and width. */
    public static final int RANGE_X = 7;
    public static final int SOUND_X = 49;
    public static final int GEIGER_BUTTON_Y = 63;
    public static final int GEIGER_BUTTON_WIDTH = 12;
    /** The dosimeter's button. */
    public static final int LIGHT_X = 7;
    public static final int LIGHT_Y = 47;
    public static final int LIGHT_WIDTH = 10;
    /** A button's height. */
    public static final int BUTTON_HEIGHT = 4;
    /** How many lamps the Geiger counter lights for its clicks. */
    public static final int CLICK_LAMPS = 6;
    /** How many bands the dosimeter has lamps for. */
    public static final int BAND_LAMPS = 5;
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "hazard_devices", new Colours(
            0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0x8C8C8C, 0xFF << 24 | 0xBEBEBE, 0xFF << 24 | 0x969696,
            0xFF << 24 | 0x3C3C3C, 0xFF << 24 | 0x323232, 0xFF << 24 | 0x6EC83C, 0xFF << 24 | 0xE63C32,
            0xFF << 24 | 0xFF503C, 0xFF << 24 | 0xC8C8C8, 0xFF << 24 | 0x3C2E00, 0xFF << 24 | 0xE6783C,
            0xFF << 24 | 0xF0BE3C, 0x3C << 24 | 0x60D070));

    /* The face of each, and the Geiger counter's dial: its centre and radius. */
    private static final int GEIGER_FACE_X = 5;
    private static final int GEIGER_FACE_Y = 9;
    private static final int DIAL_X = 31;
    private static final int DIAL_Y = 50;
    private static final int DIAL_RADIUS = 22;
    private static final int DOSIMETER_FACE_X0 = 5;
    private static final int DOSIMETER_FACE_Y0 = 9;
    private static final int DOSIMETER_FACE_X1 = 59;
    private static final int DOSIMETER_FACE_Y1 = 43;
    /* The small words, as the game's font is drawn at half its size on the HUD. */
    private static final float SMALL = 0.5F;
    /* The decades the dial is printed with, from its left end, as powers of ten: a hundredth, one, a hundred. */
    private static final List<Integer> DECADES = List.of(-2, 0, 2);

    private DeviceArt() {
    }

    /**
     * The Geiger counter with its case's top left at {@code (x, y)}, in units of {@code s}: the rate on its needle and
     * in figures, or OFF when there is nothing to read, its range, and its lamps lit as {@code lit} says, a bit each.
     */
    public static void geiger(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                              final Optional<Double> rate, final String unit, final String rangeLabel,
                              final int multiplier, final int lit, final String rangeWord, final String soundWord,
                              final String off) {
        final Colours c = PALETTE.get();
        g.blit(s == 1 ? GEIGER : GEIGER_BIG, x, y, 0, 0, GEIGER_WIDTH * s, GEIGER_HEIGHT * s, GEIGER_WIDTH * s,
                GEIGER_HEIGHT * s);
        final int cx = x + DIAL_X * s;
        final int cy = y + DIAL_Y * s;
        final int r = DIAL_RADIUS * s;
        final int shift = (int) Math.round(Math.log10(multiplier));
        for (int k = 0; k < DECADES.size(); k++) {
            final double a = Math.PI * (1 + k / 2.0);
            final int lx = cx + (int) Math.round((r - 9 * s) * Math.cos(a));
            final int ly = cy + (int) Math.round((r - 9 * s) * Math.sin(a)) - 3 * s;
            centred(g, font, decade(DECADES.get(k) + shift), lx, ly, c.dial(), SMALL * s);
        }
        final double frac = rate.map(value -> value <= 0 ? 0 : (Math.log10(value / multiplier) + 2) / 4).orElse(0.0);
        final double a = Math.PI * (1 + Math.clamp(frac, 0, 1));
        for (int q = 0; q < r - 3 * s; q++) {
            final int px = cx + (int) Math.round(q * Math.cos(a));
            final int py = cy + (int) Math.round(q * Math.sin(a));
            g.fill(px, py, px + s, py + 1, c.needle());
        }
        g.fill(cx - s, cy - s, cx + 2 * s, cy + 2 * s, c.pivot());
        final int tx = x + (GEIGER_FACE_X + 56) * s;
        final String figure = rate.map(DeviceReadings::figure).orElse(off);
        Draw.textScaled(g, font, figure, tx, y + (GEIGER_FACE_Y + 5) * s, c.white(), s);
        if (rate.isPresent()) {
            Draw.textScaled(g, font, unit, tx + font.width(figure) * s + 3 * s, y + (GEIGER_FACE_Y + 7) * s,
                    c.unit(), SMALL * s);
        }
        Draw.textScaled(g, font, rangeLabel, tx, y + (GEIGER_FACE_Y + 20) * s, c.dim(), SMALL * s);
        for (int k = 0; k < CLICK_LAMPS; k++) {
            final int lx = tx + k * 8 * s;
            final int ly = y + (GEIGER_FACE_Y + 30) * s;
            g.fill(lx, ly, lx + 5 * s, ly + 6 * s, (lit >> k & 1) != 0 ? c.white() : c.lampOff());
        }
        button(g, font, x, y, s, RANGE_X + GEIGER_BUTTON_WIDTH, GEIGER_BUTTON_Y, rangeWord);
        button(g, font, x, y, s, SOUND_X + GEIGER_BUTTON_WIDTH, GEIGER_BUTTON_Y, soundWord);
    }

    /**
     * The dosimeter with its case's top left at {@code (x, y)}, in units of {@code s}: the dose taken in figures and
     * its unit, or OFF, the band it reached, a lamp for each band reached, and its face lit when {@code light}.
     */
    public static void dosimeter(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                                 final Optional<Double> dose, final String unit, final String band, final int reached,
                                 final boolean light, final String doseWord, final String lightWord,
                                 final String off) {
        final Colours c = PALETTE.get();
        g.blit(s == 1 ? DOSIMETER : DOSIMETER_BIG, x, y, 0, 0, DOSIMETER_WIDTH * s, DOSIMETER_HEIGHT * s,
                DOSIMETER_WIDTH * s, DOSIMETER_HEIGHT * s);
        if (light) {
            g.fill(x + DOSIMETER_FACE_X0 * s, y + DOSIMETER_FACE_Y0 * s, x + DOSIMETER_FACE_X1 * s,
                    y + DOSIMETER_FACE_Y1 * s, c.backlight());
        }
        final int fx = x + DOSIMETER_FACE_X0 * s;
        final int fy = y + DOSIMETER_FACE_Y0 * s;
        Draw.textScaled(g, font, doseWord, fx + 3 * s, fy + 3 * s, c.dim(), SMALL * s);
        if (!band.isEmpty()) {
            final int w = (int) Math.ceil(font.width(band) * SMALL * s);
            Draw.textScaled(g, font, band, x + (DOSIMETER_FACE_X1 - 3) * s - w, fy + 3 * s, c.band(), SMALL * s);
        }
        final String figure = dose.map(DeviceReadings::figure).orElse(off);
        Draw.textScaled(g, font, figure, fx + 3 * s, fy + 11 * s, c.white(), s);
        if (dose.isPresent()) {
            Draw.textScaled(g, font, unit, fx + 5 * s + font.width(figure) * s, fy + 13 * s, c.unit(), SMALL * s);
        }
        for (int k = 0; k < BAND_LAMPS; k++) {
            final int lx = fx + 3 * s + k * 9 * s;
            final int ly = y + (DOSIMETER_FACE_Y1 - 6) * s;
            g.fill(lx, ly, lx + 7 * s, ly + 3 * s, k < reached ? mix(c.green(), c.red(), k / 4.0) : c.lampDark());
        }
        button(g, font, x, y, s, LIGHT_X + LIGHT_WIDTH, LIGHT_Y, lightWord);
    }

    /** Ten to the {@code power} as the dial prints it: .01, .1, 1, 10, 100, 1k, 10k. */
    public static String decade(final int power) {
        if (power >= 3) {
            return "1" + "0".repeat(power - 3) + "k";
        }
        if (power >= 0) {
            return "1" + "0".repeat(power);
        }
        return "." + "0".repeat(-power - 1) + "1";
    }

    /* A button's word, moulded on the case after its right end. */
    private static void button(final GuiGraphics g, final Font font, final int x, final int y, final int s,
                               final int after, final int top, final String word) {
        Draw.textScaled(g, font, word, x + (after + 2) * s, y + top * s - (s == 1 ? 1 : 0), PALETTE.get().mould(),
                SMALL * s);
    }

    private static void centred(final GuiGraphics g, final Font font, final String text, final int cx, final int y,
                                final int colour, final float scale) {
        Draw.textScaled(g, font, text, cx - (int) Math.ceil(font.width(text) * scale / 2), y, colour, scale);
    }

    private static int mix(final int from, final int to, final double share) {
        final int r = (int) Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * share);
        final int gr = (int) Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * share);
        final int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * share);
        return 0xFF << 24 | r << 16 | gr << 8 | b;
    }

    private static ResourceLocation texture(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "textures/gui/hazard/" + name + ".png");
    }

    /**
     * The colours of the devices' faces.
     *
     * @param white     the figures and a lit click lamp
     * @param dim       the faint labels: the range, DOSE
     * @param unit      the units after the figures
     * @param dial      the figures printed on the dial
     * @param lampOff   a click lamp unlit
     * @param lampDark  a band lamp unlit
     * @param green     the mildest band's lamp
     * @param red       the worst band's lamp
     * @param needle    the needle
     * @param pivot     the needle's pivot
     * @param mould     the words moulded on the case beside its buttons
     * @param band      the band's name on the dosimeter
     * @param amber     a hazard's name on the map device
     * @param backlight the dosimeter's face lit, over its black
     */
    public record Colours(int white, int dim, int unit, int dial, int lampOff, int lampDark, int green, int red,
                          int needle, int pivot, int mould, int band, int amber, int backlight) {
    }
}
