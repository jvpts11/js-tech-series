/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import java.util.Objects;

/**
 * The pixels drawn over the faces round a strike, laid flat across the surface where it struck, sixteen to a block
 * as the game's textures are: bare metal torn along a hole's rim, soot and char round it, dithered as pixel art shades,
 * the last of a burn's glow, the scrapes of a blow. A face takes its pixel by where it was before the strike, so the
 * pixels stay with the metal as it bends. Pure, so it is tested without the game.
 *
 * <pre>{@code
 * DamageLook look = new DamageLook(strike, colours);  // the colours from a palette a resource pack can change
 * int[] argb = look.pixels();          // side() by side(), row by row, nothing seen where alpha is 0
 * look.uv(x, y, z, uv);                // where a face's corner, as it was, takes its pixel
 * }</pre>
 */
public final class DamageLook {

    private final StrikeFrame frame;
    private final Colours colours;
    private final int side;
    private final double reach;

    /** Pixels to a block, as the game's textures have them. */
    public static final int TEXELS = 16;
    /* The most pixels to a side, for the biggest strikes. */
    private static final int MOST_SIDE = 256;
    /* How far inside and outside the rim the torn band runs, in blocks and shares of the size. */
    private static final double BAND_INSIDE = 0.02;
    private static final double TORN_BAND = 0.16;
    private static final double SPALL_BAND = 0.3;
    /* How far out soot and char reach past the rim, shares of the size, and how dark they are at the rim. */
    private static final double SOOT_REACH = 0.9;
    private static final double SOOT_DARK = 0.6;
    private static final double CHAR_REACH = 1.0;
    private static final double CHAR_DARK = 0.85;
    /* How far past the rim the glow runs, a share of the size, and how many of the seven speckles glow. */
    private static final double GLOW_BAND = 0.08;
    private static final int GLOWING_SPECKS = 4;
    /* How many of the seven speckles of torn metal are dark, and how far out a blow scrapes, a share of the size. */
    private static final int DARK_SPECKS = 2;
    private static final int SPECKS = 7;
    private static final double SCRAPED = 0.35;
    private static final int SCRAPE_ONE_IN = 17;
    /* The order a four by four dither lights its pixels in. */
    private static final int[] DITHER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
    private static final int DITHER_SIDE = 4;

    /** The pixels of {@code strike}'s edge, in {@code colours}. */
    public DamageLook(final Strike strike, final Colours colours) {
        this.frame = new StrikeFrame(strike);
        this.colours = Objects.requireNonNull(colours, "colours");
        this.reach = strike.reach();
        this.side = Math.clamp((int) Math.ceil(2 * this.reach * TEXELS), 2, MOST_SIDE);
    }

    /** How many pixels its square has to a side: its reach either way, sixteen to a block. */
    public int side() {
        return this.side;
    }

    /** Its pixels, {@link #side()} by {@link #side()}, row by row, ARGB, nothing seen where the alpha is 0. */
    public int[] pixels() {
        return paint(false);
    }

    /** Its pixels that glow in the dark, laid as {@link #pixels()}; all unseen for a stamp whose edge does not glow. */
    public int[] glow() {
        return paint(true);
    }

    /** Whether any of its pixels glow. */
    public boolean glows() {
        return this.frame.strike.stamp().edge() == DamageStamp.Edge.SCORCHED;
    }

    /**
     * Puts in {@code out} where on its pixels a face's corner takes its pixel, by where the corner was before the
     * strike, (x, y, z) in the model's space: u and v, from 0 to 1 across its square.
     */
    public void uv(final double x, final double y, final double z, final double[] out) {
        out[0] = (this.frame.a(x, y, z) + this.reach) / (2 * this.reach);
        out[1] = (this.frame.b(x, y, z) + this.reach) / (2 * this.reach);
    }

    private int[] paint(final boolean glowing) {
        final int[] out = new int[this.side * this.side];
        if (glowing && !glows()) {
            return out;
        }
        for (int j = 0; j < this.side; j++) {
            for (int i = 0; i < this.side; i++) {
                final double a = (i + 0.5) / this.side * 2 * this.reach - this.reach;
                final double b = (j + 0.5) / this.side * 2 * this.reach - this.reach;
                out[j * this.side + i] = glowing ? glowAt(i, j, a, b) : pixelAt(i, j, a, b);
            }
        }
        return out;
    }

    /* The pixel at column {@code i}, row {@code j}, the point ({@code a}, {@code b}) across the surface. */
    private int pixelAt(final int i, final int j, final double a, final double b) {
        final Strike strike = this.frame.strike;
        final double size = strike.size();
        final double dist = Math.hypot(a, b);
        final double rim = this.frame.rim(Math.atan2(b, a));
        final int speck = Math.floorMod(hash(i, j, strike.seed()), SPECKS);
        final Colours c = this.colours;
        final int metal = speck < DARK_SPECKS ? c.tornDark() : c.torn();
        return switch (strike.stamp().edge()) {
            case TORN -> dist >= rim - BAND_INSIDE && dist < rim + TORN_BAND * size ? metal : 0;
            case SOOTED -> {
                if (dist < rim - BAND_INSIDE) {
                    yield 0;
                }
                if (dist < rim + TORN_BAND * size) {
                    yield metal;
                }
                yield dithered(i, j, Math.clamp(1 - (dist - rim) / (SOOT_REACH * size), 0, 1) * SOOT_DARK)
                        ? c.soot() : 0;
            }
            case SPALL -> dist >= rim - BAND_INSIDE && dist < rim + SPALL_BAND * size ? metal : 0;
            case SCORCHED -> dist >= rim - BAND_INSIDE && dithered(i, j,
                    Math.clamp(1 - (dist - rim) / (CHAR_REACH * size), 0, 1) * CHAR_DARK) ? c.charred() : 0;
            case SCUFFED -> dist < SCRAPED * size
                    && Math.floorMod(hash(j, i, ~strike.seed()), SCRAPE_ONE_IN) == 0 ? c.scrape() : 0;
        };
    }

    /* The glowing pixel at column {@code i}, row {@code j}: the last of a burn's glow along its rim. */
    private int glowAt(final int i, final int j, final double a, final double b) {
        final Strike strike = this.frame.strike;
        final double dist = Math.hypot(a, b);
        final double rim = this.frame.rim(Math.atan2(b, a));
        final boolean lit = dist >= rim - BAND_INSIDE && dist < rim + GLOW_BAND * strike.size()
                && Math.floorMod(hash(i, j, strike.seed()), SPECKS) < GLOWING_SPECKS;
        return lit ? this.colours.glow() : 0;
    }

    /* Whether the pixel at column {@code i}, row {@code j} is lit by a dither of {@code share} of its pixels. */
    private static boolean dithered(final int i, final int j, final double share) {
        final int order = DITHER[Math.floorMod(j, DITHER_SIDE) * DITHER_SIDE + Math.floorMod(i, DITHER_SIDE)];
        return (order + 0.5) / DITHER.length < share;
    }

    private static int hash(final int i, final int j, final long seed) {
        return (int) (i * 73_856_093L ^ j * 19_349_663L ^ seed * 83_492_791L ^ seed >>> 32);
    }

    /**
     * The colours a strike's edge is painted in, ARGB, opaque: a palette's, so a resource pack can change them.
     *
     * @param torn     bare metal torn along a rim
     * @param tornDark the darker speckles of torn metal
     * @param soot     soot round an explosion's hole
     * @param charred  char round a burn
     * @param glow     the last of a burn's glow
     * @param scrape   the scrapes of a blow
     */
    public record Colours(int torn, int tornDark, int soot, int charred, int glow, int scrape) {
    }
}
