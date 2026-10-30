/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.skin;

import java.util.Arrays;

/**
 * A skin of a structure's face, painted as large as the face at sixteen texels a block, as a block's own texture has
 * them, so a big face never shows a small texture tiled over it: its plates, bevels, rivets, hazard stripes, lamps and
 * stencils, then a fine grain of light and dark over the whole of it, two texels a grain, as worn paint has. Painted as
 * the approved pictures' skins are.
 */
public final class PaintedSkin {

    private final int width;
    private final int height;
    private final int[] pixels;
    private final float grain;
    private final int seed;

    /** Texels a block. */
    public static final int TEXELS = 16;
    /* The stripes of a hazard band at their usual width, in texels; the side of a grain. */
    private static final int STRIPE = 4;
    private static final int GRAIN = 2;
    private static final int CHANNEL = 0xFF;
    /* The steps of a stencil's marks, worked out as the approved pictures' are. */
    private static final long MARKS_MULTIPLIER = 1103515245L;
    private static final long MARKS_INCREMENT = 12345L;
    private static final long MARKS_MASK = 0x7FFFFFFFL;
    private static final int MARKS_SEED = 7;
    private static final int MARKS_START = 3;
    private static final int MARK_WIDTHS = 3;
    private static final long MIX = 0x9E3779B97F4A7C15L;
    private static final double UNIT = 0x1.0p-53;

    /**
     * A skin {@code across} by {@code down} blocks in {@code base}, its grain {@code grain} either way and laid by
     * {@code seed}.
     */
    public PaintedSkin(final double across, final double down, final int base, final float grain, final int seed) {
        this.width = texels(across);
        this.height = texels(down);
        this.pixels = new int[this.width * this.height];
        this.grain = grain;
        this.seed = seed;
        Arrays.fill(this.pixels, base);
    }

    /** How many texels {@code blocks} span, two at the least; a half rounded to the even, as the approved skins are. */
    public static int texels(final double blocks) {
        return Math.max(2, (int) Math.rint(blocks * TEXELS));
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /** Fills from {@code x0}, {@code y0} up to but not over {@code x1}, {@code y1}; what is off the skin is not. */
    public void rect(final int x0, final int y0, final int x1, final int y1, final int argb) {
        for (int y = Math.max(0, y0); y < Math.min(this.height, y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(this.width, x1); x++) {
                this.pixels[y * this.width + x] = argb;
            }
        }
    }

    public void hline(final int y, final int argb) {
        rect(0, y, this.width, y + 1, argb);
    }

    public void hline(final int y, final int argb, final int x0, final int x1) {
        rect(x0, y, x1, y + 1, argb);
    }

    public void vline(final int x, final int argb) {
        rect(x, 0, x + 1, this.height, argb);
    }

    public void vline(final int x, final int argb, final int y0, final int y1) {
        rect(x, y0, x + 1, y1, argb);
    }

    /** A plate standing out, lit on its top and left and shaded below and right; or sunk, the other way round. */
    public void bevel(final int x0, final int y0, final int x1, final int y1, final int fill, final int lit,
                      final int shaded, final boolean raised) {
        rect(x0, y0, x1, y1, fill);
        final int a = raised ? lit : shaded;
        final int b = raised ? shaded : lit;
        hline(y0, a, x0, x1);
        vline(x0, a, y0, y1);
        hline(y1 - 1, b, x0, x1);
        vline(x1 - 1, b, y0, y1);
    }

    public void rivet(final int x, final int y, final int lit, final int shaded) {
        rect(x, y, x + 1, y + 1, lit);
        rect(x + 1, y + 1, x + 2, y + 2, shaded);
    }

    /** Diagonal stripes of {@code a} and {@code b} over the rectangle, at their usual width. */
    public void hazard(final int x0, final int y0, final int x1, final int y1, final int a, final int b) {
        hazard(x0, y0, x1, y1, a, b, STRIPE);
    }

    /** Diagonal stripes of {@code a} and {@code b} over the rectangle, {@code stripe} texels wide. */
    public void hazard(final int x0, final int y0, final int x1, final int y1, final int a, final int b,
                       final int stripe) {
        for (int y = Math.max(0, y0); y < Math.min(this.height, y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(this.width, x1); x++) {
                this.pixels[y * this.width + x] = (x + y) / stripe % 2 == 0 ? a : b;
            }
        }
    }

    /** A lamp's glass in {@code glass}, in its dark housing. */
    public void lamp(final int x, final int y, final int glass, final int housing) {
        rect(x - 1, y - 1, x + 2, y + 2, housing);
        rect(x, y, x + 1, y + 1, glass);
    }

    /** A stencilled word of {@code marks} marks, read as letters from a few blocks away. */
    public void text(final int x, final int y, final int marks, final int argb) {
        long k = (long) this.seed * MARKS_SEED + MARKS_START;
        int at = x;
        for (int mark = 0; mark < marks; mark++) {
            k = (k * MARKS_MULTIPLIER + MARKS_INCREMENT) & MARKS_MASK;
            final int wide = 1 + (int) (k % MARK_WIDTHS);
            rect(at, y, at + wide, y + 2, argb);
            at += wide + 1;
        }
    }

    /** The skin as painted, its grain laid over it. */
    public Picture finish() {
        final int[] out = this.pixels.clone();
        if (this.grain > 0) {
            for (int y = 0; y < this.height; y++) {
                for (int x = 0; x < this.width; x++) {
                    final double shade = 1 - this.grain + 2 * this.grain * grainAt(x / GRAIN, y / GRAIN);
                    final int argb = out[y * this.width + x];
                    out[y * this.width + x] = argb & CHANNEL << 24 | channel(argb >> 16, shade) << 16
                            | channel(argb >> 8, shade) << 8 | channel(argb, shade);
                }
            }
        }
        return new Picture(this.width, this.height, out);
    }

    /* The grain at the cell {@code x}, {@code y}, from nought to one. */
    private double grainAt(final int x, final int y) {
        long h = this.seed * MIX + x * 0xBF58476D1CE4E5B9L + y * 0x94D049BB133111EBL;
        h = (h ^ h >>> 30) * 0xBF58476D1CE4E5B9L;
        h = (h ^ h >>> 27) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * UNIT;
    }

    private static int channel(final int value, final double shade) {
        return Math.min(CHANNEL, Math.max(0, (int) Math.round((value & CHANNEL) * shade)));
    }
}
