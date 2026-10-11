/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import dev.jstech.computers.os.fs.PixImage;
import java.util.Arrays;

/**
 * A picture as a printer puts it on paper, each its own way and at a resolution a real one prints at, so its
 * technique shows as a grain when looked at closely and the picture reads whole from a step back: the dot matrix in the
 * round dots of its ribbon, every other pass of its head a pixel over; the old inkjet in cyan, magenta, yellow and
 * black dots, grainy, its head's passes showing as paler rows; the newer inkjet the same with dots too fine to see;
 * the laser in a fine halftone of round toner dots on a screen turned forty-five degrees; the ink tank printer as the
 * picture is. The picture is fitted to the sheet keeping its shape, and a pixel left clear in the picture is paper:
 * it is left transparent, so the sheet it is drawn on shows through.
 *
 * <p>Pure: the reading screen and the framed sheet make their textures from it, and a test can check every printer's
 * hand without the game. The result depends on nothing but the picture and the printer, so the same sheet always
 * looks the same. Every printer but the ink tank one is meant to be drawn smoothed, as the eye sees paper.
 */
public final class PrintedPicture {

    /** How many dots across the widest side of a dot matrix picture, and how many pixels apart they stand. */
    private static final int DOT_GRID = 168;
    private static final int DOT_PITCH = 5;
    /** A ribbon dot's radius in pixels, and how much of the paper under it it covers. */
    private static final float DOT_RADIUS = 2.7F;
    private static final float RIBBON_COVER = 0.86F;
    /** The rows one pass of a nine-pin head prints; every other pass lands a pixel over. */
    private static final int HEAD_ROWS = 8;
    /** The old inkjet: dots across the widest side, pixels apart, their radius, and the rows one head pass prints. */
    private static final int COARSE_GRID = 432;
    private static final int COARSE_PITCH = 2;
    private static final float COARSE_RADIUS = 1.25F;
    private static final int COARSE_BAND = 24;
    /** How much paler the first row of each of the old inkjet's passes prints. */
    private static final float BAND_FADE = 0.15F;
    /** The newer inkjet: a dot a pixel, too fine for the eye to tell apart. */
    private static final int FINE_GRID = 648;
    /** The laser: halftone cells across the widest side, and pixels a cell. */
    private static final int LASER_GRID = 200;
    private static final int LASER_CELL = 4;
    /** How sharp a toner dot's edge is, and how much paper its densest toner still lets through. */
    private static final float LASER_EDGE = 6F;
    private static final float TONER_COVER = 0.95F;
    /** How many pixels across the widest side of a clean print. */
    private static final int CLEAN_SIZE = 192;
    /** A pixel fully covered. */
    private static final int OPAQUE = 0xFF << 24;
    /** The ribbon's black and the laser's toner, each a little cooler than a pure black. */
    private static final int RIBBON = argb(34, 34, 40) & 0xFFFFFF;
    private static final int TONER = argb(40, 40, 42) & 0xFFFFFF;
    /*
     * What share of red, green and blue light each ink lets back from the paper: black, cyan, magenta and yellow, laid
     * in that order. Inks over one another multiply, as they do on paper.
     */
    private static final float[][] INKS = {{0.10F, 0.10F, 0.12F}, {0F, 1F, 1F}, {1F, 0F, 1F}, {1F, 1F, 0F}};
    /** The screen's turn, forty-five degrees: a cell's diagonal. */
    private static final double SCREEN = LASER_CELL * Math.sqrt(2.0);

    private PrintedPicture() {
    }

    /** {@code picture} as {@code ink} prints it. */
    public static Raster print(final PixImage picture, final PrinterModel.Ink ink) {
        return switch (ink) {
            case DOT_MATRIX -> dotMatrix(picture);
            case INKJET_COARSE -> inkjet(picture, COARSE_GRID, COARSE_PITCH, COARSE_RADIUS, true);
            case INKJET_FINE -> inkjet(picture, FINE_GRID, 1, 0F, false);
            case LASER -> laser(picture);
            case CLEAN -> clean(picture);
        };
    }

    /*
     * One bit a dot, error-diffused so greys come out as more or fewer dots; each dot a round blot of the ribbon,
     * soft at its edge, every other pass of the head shifted by a pixel the way a loose head prints.
     */
    private static Raster dotMatrix(final PixImage picture) {
        final int[] size = fit(picture, DOT_GRID);
        final int gw = size[0];
        final int gh = size[1];
        final float[] grey = new float[gw * gh];
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                grey[y * gw + x] = (float) Math.pow(luma(sample(picture, x, y, gw, gh)) / 255.0, 0.8);
            }
        }
        final boolean[] dots = dither(grey, gw, gh);
        final int w = gw * DOT_PITCH + DOT_PITCH;
        final int h = gh * DOT_PITCH;
        final float[] cover = new float[w * h];
        for (int y = 0; y < gh; y++) {
            final int shift = (y / HEAD_ROWS) % 2;
            for (int x = 0; x < gw; x++) {
                if (dots[y * gw + x]) {
                    stamp(cover, w, h, x * DOT_PITCH + DOT_PITCH / 2F + shift, y * DOT_PITCH + DOT_PITCH / 2F,
                            DOT_RADIUS, RIBBON_COVER);
                }
            }
        }
        return toned(cover, w, h, RIBBON);
    }

    /*
     * Black, cyan, magenta and yellow, each error-diffused on its own so every tone is a mix of dots, the inks
     * multiplying where they overlap; the old head's dots spread a little round each, and the first row of each of its
     * passes prints paler.
     */
    private static Raster inkjet(final PixImage picture, final int grid, final int pitch, final float radius,
                                 final boolean bands) {
        final int[] size = fit(picture, grid);
        final int gw = size[0];
        final int gh = size[1];
        final float[][] amounts = new float[INKS.length][gw * gh];
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                final int rgb = sample(picture, x, y, gw, gh);
                final float c = 1F - ((rgb >> 16) & 0xFF) / 255F;
                final float m = 1F - ((rgb >> 8) & 0xFF) / 255F;
                final float yel = 1F - (rgb & 0xFF) / 255F;
                final float k = Math.min(c, Math.min(m, yel));
                final float rest = k < 1F ? 1F - k : 1F;
                final int at = y * gw + x;
                amounts[0][at] = k;
                amounts[1][at] = (c - k) / rest;
                amounts[2][at] = (m - k) / rest;
                amounts[3][at] = (yel - k) / rest;
            }
        }
        final int w = gw * pitch;
        final int h = gh * pitch;
        final float[] red = filled(w * h);
        final float[] green = filled(w * h);
        final float[] blue = filled(w * h);
        final float[] inked = new float[w * h];
        for (int layer = 0; layer < INKS.length; layer++) {
            final float[] paper = new float[gw * gh];
            for (int i = 0; i < paper.length; i++) {
                paper[i] = 1F - amounts[layer][i];
            }
            final boolean[] dots = dither(paper, gw, gh);
            final float[] cover = new float[w * h];
            for (int y = 0; y < gh; y++) {
                for (int x = 0; x < gw; x++) {
                    if (!dots[y * gw + x]) {
                        continue;
                    }
                    if (pitch == 1) {
                        cover[y * w + x] = 1F;
                    } else {
                        stamp(cover, w, h, x * pitch + pitch / 2F, y * pitch + pitch / 2F, radius, 1F);
                    }
                }
            }
            final float[] ink = INKS[layer];
            for (int i = 0; i < cover.length; i++) {
                final float a = cover[i];
                if (a > 0F) {
                    red[i] *= 1F - a + ink[0] * a;
                    green[i] *= 1F - a + ink[1] * a;
                    blue[i] *= 1F - a + ink[2] * a;
                    inked[i] = Math.max(inked[i], a);
                }
            }
        }
        final Raster out = new Raster(w, h);
        for (int y = 0; y < h; y++) {
            final boolean faded = bands && y % (pitch * COARSE_BAND) < pitch;
            for (int x = 0; x < w; x++) {
                final int i = y * w + x;
                if (inked[i] <= 0F) {
                    continue;
                }
                final float lift = faded ? BAND_FADE : 0F;
                out.set(x, y, argb(Math.round((red[i] * (1F - lift) + lift) * 255F),
                        Math.round((green[i] * (1F - lift) + lift) * 255F),
                        Math.round((blue[i] * (1F - lift) + lift) * 255F)));
            }
        }
        return out;
    }

    /*
     * Toner in round dots on a screen turned forty-five degrees, each dot as big as its place is dark: a cell's
     * darkness sets how much of the spot that peaks at the cell's middle the toner fills, so every grey has a dot size of
     * its own and the edge of a dot is soft. White prints nothing at all.
     */
    private static Raster laser(final PixImage picture) {
        final int[] size = fit(picture, LASER_GRID);
        final int gw = size[0];
        final int gh = size[1];
        final float[] dark = new float[gw * gh];
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                dark[y * gw + x] = 1F - luma(sample(picture, x, y, gw, gh)) / 255F;
            }
        }
        final int w = gw * LASER_CELL;
        final int h = gh * LASER_CELL;
        final float[] cover = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final float d = dark[(y / LASER_CELL) * gw + x / LASER_CELL];
                if (d <= 0F) {
                    continue;
                }
                final double u = (x + y) / SCREEN * 2.0 * Math.PI;
                final double v = (x - y) / SCREEN * 2.0 * Math.PI;
                final float spot = (float) ((Math.cos(u) + Math.cos(v)) / 4.0 + 0.5);
                cover[y * w + x] = clamp01((d - (1F - spot)) * LASER_EDGE + 0.5F) * TONER_COVER;
            }
        }
        return toned(cover, w, h, TONER);
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

    /* One colour laid by how much of each pixel it covers, the paper showing through the rest. */
    private static Raster toned(final float[] cover, final int w, final int h, final int rgb) {
        final Raster out = new Raster(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int alpha = Math.round(cover[y * w + x] * 255F);
                if (alpha > 0) {
                    out.set(x, y, alpha << 24 | rgb);
                }
            }
        }
        return out;
    }

    /*
     * A round dot of the given radius centred at (cx, cy), soft over its last pixel, laid into the cover: where dots
     * overlap the paper left under one is covered by the next by the same share.
     */
    private static void stamp(final float[] cover, final int w, final int h, final float cx, final float cy,
                              final float radius, final float density) {
        final int x0 = Math.max(0, (int) Math.floor(cx - radius - 1F));
        final int x1 = Math.min(w - 1, (int) Math.ceil(cx + radius + 1F));
        final int y0 = Math.max(0, (int) Math.floor(cy - radius - 1F));
        final int y1 = Math.min(h - 1, (int) Math.ceil(cy + radius + 1F));
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                final float d = (float) Math.hypot(x + 0.5F - cx, y + 0.5F - cy);
                final float blot = clamp01(radius + 0.5F - d) * density;
                if (blot > 0F) {
                    final int at = y * w + x;
                    cover[at] = 1F - (1F - cover[at]) * (1F - blot);
                }
            }
        }
    }

    /* Floyd and Steinberg's error diffusion of a field of paper, 1 bare and 0 covered: where a dot goes. */
    private static boolean[] dither(final float[] paper, final int w, final int h) {
        final float[] field = paper.clone();
        final boolean[] dots = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int at = y * w + x;
                final boolean dot = field[at] < 0.5F;
                dots[at] = dot;
                diffuse(field, w, h, x, y, field[at] - (dot ? 0F : 1F));
            }
        }
        return dots;
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
    private static void diffuse(final float[] field, final int w, final int h, final int x, final int y,
                                final float error) {
        spread(field, w, h, x + 1, y, error * 7F / 16F);
        spread(field, w, h, x - 1, y + 1, error * 3F / 16F);
        spread(field, w, h, x, y + 1, error * 5F / 16F);
        spread(field, w, h, x + 1, y + 1, error / 16F);
    }

    private static void spread(final float[] field, final int w, final int h, final int x, final int y,
                               final float amount) {
        if (x >= 0 && x < w && y < h) {
            field[y * w + x] += amount;
        }
    }

    private static float[] filled(final int n) {
        final float[] out = new float[n];
        Arrays.fill(out, 1F);
        return out;
    }

    private static float clamp01(final float v) {
        return Math.max(0F, Math.min(1F, v));
    }

    private static int luma(final int rgb) {
        return (((rgb >> 16) & 0xFF) * 299 + ((rgb >> 8) & 0xFF) * 587 + (rgb & 0xFF) * 114) / 1000;
    }

    private static int argb(final int r, final int g, final int b) {
        return OPAQUE | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
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
