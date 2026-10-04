/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import dev.jstech.computers.os.fs.PixImage;

/**
 * A picture as a printer puts it on paper, each its own way: the dot matrix in black dots, one pass of its head every
 * eight rows leaving a faint shift; the old inkjet in cyan, magenta, yellow and black dots, coarse, its head's passes
 * showing as paler bands; the newer inkjet the same, finer; the laser in a grey halftone of round toner dots; the ink
 * tank printer as the picture is. The picture is fitted to the sheet keeping its shape, and a pixel left clear in the
 * picture is paper: it is left transparent, so the sheet it is drawn on shows through.
 *
 * <p>Pure: the reading screen and the framed sheet make their textures from it, and a test can check every printer's
 * hand without the game. The result depends on nothing but the picture and the printer, so the same sheet always
 * looks the same.
 */
public final class PrintedPicture {

    /** How many dots across the widest side of a dot matrix picture. */
    private static final int DOT_GRID = 96;
    /** How many pixels a dot matrix dot spans. */
    private static final int DOT_SIZE = 4;
    /** The rows one pass of a nine-pin head prints. */
    private static final int HEAD_ROWS = 8;
    /** How many ink dots across the widest side of the old inkjet's picture. */
    private static final int COARSE_GRID = 96;
    /** The rows one pass of the old inkjet's head prints, each pass's first row paler. */
    private static final int COARSE_BAND = 24;
    /** How many ink dots across the widest side of the newer inkjet's picture. */
    private static final int FINE_GRID = 192;
    /** How many halftone cells across the widest side of a laser picture. */
    private static final int LASER_GRID = 72;
    /** How many pixels a halftone cell spans. */
    private static final int LASER_CELL = 4;
    /** How many pixels across the widest side of a clean print. */
    private static final int CLEAN_SIZE = 192;
    /** The ordered dither the inkjets lay their inks down with. */
    private static final int[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};
    /** A pixel fully covered. */
    private static final int OPAQUE = 0xFF << 24;
    /** A ribbon's dot, which lets a little paper through. */
    private static final int RIBBON = 0xE1 << 24;
    /** The inkjets' black and the laser's toner, each a little warmer or cooler than a pure black. */
    private static final int BLACK_INK = argb(30, 30, 34);
    private static final int TONER = argb(28, 28, 30);

    private PrintedPicture() {
    }

    /** {@code picture} as {@code ink} prints it. */
    public static Raster print(final PixImage picture, final PrinterModel.Ink ink) {
        return switch (ink) {
            case DOT_MATRIX -> dotMatrix(picture);
            case INKJET_COARSE -> inkjet(picture, COARSE_GRID, true);
            case INKJET_FINE -> inkjet(picture, FINE_GRID, false);
            case LASER -> laser(picture);
            case CLEAN -> clean(picture);
        };
    }

    /*
     * One bit a dot, error-diffused so greys come out as more or fewer dots; each dot a small round blot, a pass of
     * the head every eight rows shifted by a pixel the way a worn ribbon and a loose head print.
     */
    private static Raster dotMatrix(final PixImage picture) {
        final int[] size = fit(picture, DOT_GRID);
        final float[] grey = new float[size[0] * size[1]];
        for (int y = 0; y < size[1]; y++) {
            for (int x = 0; x < size[0]; x++) {
                final int rgb = sample(picture, x, y, size[0], size[1]);
                grey[y * size[0] + x] = (float) Math.pow(luma(rgb) / 255.0, 0.8);
            }
        }
        final Raster out = new Raster(size[0] * DOT_SIZE + 1, size[1] * DOT_SIZE);
        for (int y = 0; y < size[1]; y++) {
            final int shift = (y / HEAD_ROWS) % 2;
            for (int x = 0; x < size[0]; x++) {
                final int at = y * size[0] + x;
                final boolean dot = grey[at] < 0.5F;
                final float error = grey[at] - (dot ? 0F : 1F);
                diffuse(grey, size[0], size[1], x, y, error);
                if (dot) {
                    blot(out, x * DOT_SIZE + shift, y * DOT_SIZE, (x * 31 + y * 17) % 5);
                }
            }
        }
        return out;
    }

    /*
     * Cyan, magenta and yellow laid against the ordered dither, black where all three would be heavy; the old head's
     * passes leave the first row of each band paler.
     */
    private static Raster inkjet(final PixImage picture, final int grid, final boolean bands) {
        final int[] size = fit(picture, grid);
        final Raster out = new Raster(size[0], size[1]);
        for (int y = 0; y < size[1]; y++) {
            for (int x = 0; x < size[0]; x++) {
                final int rgb = sample(picture, x, y, size[0], size[1]);
                final float t = (BAYER[y % 4][x % 4] + 0.5F) / 16F;
                final float c = 1F - ((rgb >> 16) & 0xFF) / 255F;
                final float m = 1F - ((rgb >> 8) & 0xFF) / 255F;
                final float yel = 1F - (rgb & 0xFF) / 255F;
                final float k = Math.min(c, Math.min(m, yel));
                int ink;
                if (k > t * 0.9F + 0.35F) {
                    ink = BLACK_INK;
                } else {
                    float r = 255F;
                    float g = 255F;
                    float b = 255F;
                    if (c > t) {
                        r *= 0.05F;
                        g *= 0.68F;
                        b *= 0.93F;
                    }
                    if (m > t) {
                        r *= 0.92F;
                        g *= 0.12F;
                        b *= 0.55F;
                    }
                    if (yel > t) {
                        r *= 0.99F;
                        g *= 0.92F;
                        b *= 0.08F;
                    }
                    ink = c > t || m > t || yel > t ? argb((int) r, (int) g, (int) b) : 0;
                }
                if (bands && ink != 0 && y % COARSE_BAND == 0) {
                    ink = lighter(ink, 40);
                }
                out.set(x, y, ink);
            }
        }
        return out;
    }

    /* Toner in round dots on a grid, each as big as its cell is dark. */
    private static Raster laser(final PixImage picture) {
        final int[] size = fit(picture, LASER_GRID);
        final Raster out = new Raster(size[0] * LASER_CELL, size[1] * LASER_CELL);
        final float centre = (LASER_CELL - 1) / 2F;
        for (int y = 0; y < size[1]; y++) {
            for (int x = 0; x < size[0]; x++) {
                final double dark = 1.0 - Math.pow(luma(sample(picture, x, y, size[0], size[1])) / 255.0, 0.8);
                final double radius = 2.2 * Math.pow(dark, 0.9);
                if (radius <= 0.3) {
                    continue;
                }
                for (int dy = 0; dy < LASER_CELL; dy++) {
                    for (int dx = 0; dx < LASER_CELL; dx++) {
                        final double d = Math.hypot(dx - centre, dy - centre);
                        if (d <= radius) {
                            out.set(x * LASER_CELL + dx, y * LASER_CELL + dy, TONER);
                        }
                    }
                }
            }
        }
        return out;
    }

    /* The picture as it is, each of its pixels a square of the print. */
    private static Raster clean(final PixImage picture) {
        final int[] size = fit(picture, CLEAN_SIZE);
        final Raster out = new Raster(size[0], size[1]);
        for (int y = 0; y < size[1]; y++) {
            for (int x = 0; x < size[0]; x++) {
                final int index = picture.get(x * picture.width() / size[0], y * picture.height() / size[1]);
                out.set(x, y, PixImage.isTransparent(index) ? 0 : OPAQUE | PixImage.colourOf(index));
            }
        }
        return out;
    }

    /* The grid a picture is fitted to: its widest side {@code most} across, the other kept in proportion. */
    private static int[] fit(final PixImage picture, final int most) {
        final int w = Math.max(1, picture.width());
        final int h = Math.max(1, picture.height());
        if (w >= h) {
            return new int[] {most, Math.max(1, Math.round((float) most * h / w))};
        }
        return new int[] {Math.max(1, Math.round((float) most * w / h)), most};
    }

    /*
     * The colour under a cell of the grid: the average of the picture's pixels it covers when the grid is smaller,
     * the pixel it falls on when larger. A clear pixel counts as the paper's white.
     */
    private static int sample(final PixImage picture, final int x, final int y, final int gw, final int gh) {
        final int x0 = x * picture.width() / gw;
        final int y0 = y * picture.height() / gh;
        final int x1 = Math.max(x0 + 1, (x + 1) * picture.width() / gw);
        final int y1 = Math.max(y0 + 1, (y + 1) * picture.height() / gh);
        long r = 0;
        long g = 0;
        long b = 0;
        int n = 0;
        for (int py = y0; py < y1 && py < picture.height(); py++) {
            for (int px = x0; px < x1 && px < picture.width(); px++) {
                final int index = picture.get(px, py);
                final int rgb = PixImage.isTransparent(index) ? 0xFFFFFF : PixImage.colourOf(index);
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                n++;
            }
        }
        if (n == 0) {
            return 0xFFFFFF;
        }
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /* Floyd and Steinberg's spread of a dot's rounding to the dots after it. */
    private static void diffuse(final float[] grey, final int w, final int h, final int x, final int y,
                                final float error) {
        spread(grey, w, h, x + 1, y, error * 7F / 16F);
        spread(grey, w, h, x - 1, y + 1, error * 3F / 16F);
        spread(grey, w, h, x, y + 1, error * 5F / 16F);
        spread(grey, w, h, x + 1, y + 1, error / 16F);
    }

    private static void spread(final float[] grey, final int w, final int h, final int x, final int y,
                               final float amount) {
        if (x >= 0 && x < w && y < h) {
            grey[y * w + x] += amount;
        }
    }

    /* A dot of the ribbon: a rounded blot three pixels across, its shade varying a little with the ribbon's wear. */
    private static void blot(final Raster out, final int x, final int y, final int wear) {
        final int shade = 30 + wear * 5;
        final int colour = RIBBON | shade << 16 | shade << 8 | (shade + 6);
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = 0; dx < 3; dx++) {
                if ((dx == 0 || dx == 2) && (dy == 0 || dy == 2)) {
                    continue;
                }
                out.set(x + dx, y + dy, colour);
            }
        }
    }

    private static int luma(final int rgb) {
        return (((rgb >> 16) & 0xFF) * 299 + ((rgb >> 8) & 0xFF) * 587 + (rgb & 0xFF) * 114) / 1000;
    }

    private static int argb(final int r, final int g, final int b) {
        return OPAQUE | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    private static int lighter(final int argb, final int lift) {
        return argb((argb >> 16 & 0xFF) + lift, (argb >> 8 & 0xFF) + lift, (argb & 0xFF) + lift);
    }

    private static int clamp(final int v) {
        return Math.max(0, Math.min(255, v));
    }

    /**
     * A printed picture: its pixels as ARGB, a transparent pixel where the paper shows.
     *
     * @param width  how many pixels across
     * @param height how many pixels down
     * @param pixels the pixels, row by row
     */
    public record Raster(int width, int height, int[] pixels) {

        Raster(final int width, final int height) {
            this(width, height, new int[width * height]);
        }

        /** The pixel at {@code x}, {@code y}, transparent outside the picture. */
        public int get(final int x, final int y) {
            return x < 0 || y < 0 || x >= width || y >= height ? 0 : pixels[y * width + x];
        }

        void set(final int x, final int y, final int argb) {
            if (x >= 0 && y >= 0 && x < width && y < height) {
                pixels[y * width + x] = argb;
            }
        }

        /** How many pixels carry ink. */
        public int inked() {
            int n = 0;
            for (final int p : pixels) {
                if ((p >>> 24) != 0) {
                    n++;
                }
            }
            return n;
        }
    }
}
