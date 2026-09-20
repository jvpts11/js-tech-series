/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * A picture: a grid of pixels, each one naming a colour in a palette of 256.
 *
 * <p>Indexed rather than a colour per pixel, and run length encoded on the way to a file, because a disk
 * here counts the bytes of what is on it. A drawing this size with a full colour on every pixel would be
 * the heaviest file on a Legacy machine's disk; a byte per pixel with the runs collapsed is about a
 * kilobyte, which is the difference between a program a player uses and one they delete.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class PixImage {

    /** The extension a picture is written under. */
    public static final String EXTENSION = "pix";

    /** What every picture file starts with, so one that is not a picture is recognised before it is read. */
    public static final String MAGIC = "JSPIX1";

    /** The largest canvas, which is what a machine of this age could hold and redraw. */
    public static final int MAX_SIDE = 128;

    /** How many colours the palette names. */
    public static final int COLOURS = 256;

    /**
     * The colours a picture may use, in the order their indexes name them.
     *
     * <p>The first sixteen are the ones a player reaches for, laid out the way a paint program's strip is;
     * the rest fill the space evenly so a picture has somewhere to go between them. Index 0 is transparent,
     * which is what an untouched pixel is and what makes the canvas start empty rather than black.
     */
    private static final int[] PALETTE = buildPalette();

    private final int width;
    private final int height;
    private final byte[] pixels;

    public PixImage(final int width, final int height) {
        if (width < 1 || height < 1 || width > MAX_SIDE || height > MAX_SIDE) {
            throw new IllegalArgumentException(
                    "a canvas is between 1 and " + MAX_SIDE + " on a side, was " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.pixels = new byte[width * height];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The colour at a pixel as an index into the palette, or 0 for one never painted. */
    public int get(final int x, final int y) {
        return inside(x, y) ? pixels[x + y * width] & 0xFF : 0;
    }

    /** Paints one pixel; a point outside the canvas is ignored rather than refused. */
    public void set(final int x, final int y, final int colour) {
        if (inside(x, y)) {
            pixels[x + y * width] = (byte) (colour & 0xFF);
        }
    }

    /** Paints every pixel the same colour. */
    public void fillAll(final int colour) {
        Arrays.fill(pixels, (byte) (colour & 0xFF));
    }

    /** A copy, for an undo step to hold onto. */
    public PixImage copy() {
        final PixImage out = new PixImage(width, height);
        System.arraycopy(pixels, 0, out.pixels, 0, pixels.length);
        return out;
    }

    /** Takes another picture's pixels, which is how an undo step is put back. */
    public void copyFrom(final PixImage other) {
        if (other.width != width || other.height != height) {
            throw new IllegalArgumentException("a picture can only take one of its own size");
        }
        System.arraycopy(other.pixels, 0, pixels, 0, pixels.length);
    }

    /** Draws a straight line between two points, the way a pencil dragged across the canvas would. */
    public void line(final int x0, final int y0, final int x1, final int y1, final int colour) {
        // Bresenham: whole numbers only, so the line lands on the same pixels every time it is drawn.
        int x = x0;
        int y = y0;
        final int dx = Math.abs(x1 - x0);
        final int dy = -Math.abs(y1 - y0);
        final int sx = x0 < x1 ? 1 : -1;
        final int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            set(x, y, colour);
            if (x == x1 && y == y1) {
                return;
            }
            final int doubled = error * 2;
            if (doubled >= dy) {
                error += dy;
                x += sx;
            }
            if (doubled <= dx) {
                error += dx;
                y += sy;
            }
        }
    }

    /** An outlined rectangle between two corners. */
    public void rectangle(final int x0, final int y0, final int x1, final int y1, final int colour) {
        line(x0, y0, x1, y0, colour);
        line(x1, y0, x1, y1, colour);
        line(x1, y1, x0, y1, colour);
        line(x0, y1, x0, y0, colour);
    }

    /** An outlined ellipse inside the box between two corners. */
    public void ellipse(final int x0, final int y0, final int x1, final int y1, final int colour) {
        final int left = Math.min(x0, x1);
        final int right = Math.max(x0, x1);
        final int top = Math.min(y0, y1);
        final int bottom = Math.max(y0, y1);
        final double cx = (left + right) / 2.0;
        final double cy = (top + bottom) / 2.0;
        final double rx = Math.max(0.5, (right - left) / 2.0);
        final double ry = Math.max(0.5, (bottom - top) / 2.0);
        /*
         * Walked by angle rather than solved per column, because a short axis solved per column leaves gaps
         * at the ends of the ellipse where the curve runs faster than one pixel a step.
         */
        final int steps = Math.max(16, (int) ((rx + ry) * 4));
        int previousX = Integer.MIN_VALUE;
        int previousY = Integer.MIN_VALUE;
        for (int i = 0; i <= steps; i++) {
            final double angle = 2 * Math.PI * i / steps;
            final int px = (int) Math.round(cx + rx * Math.cos(angle));
            final int py = (int) Math.round(cy + ry * Math.sin(angle));
            if (previousX != Integer.MIN_VALUE) {
                line(previousX, previousY, px, py, colour);
            }
            previousX = px;
            previousY = py;
        }
    }

    /**
     * Fills the area of one colour that a point sits in.
     *
     * <p>Walked with a stack rather than by calling itself, because a fill across a whole canvas is
     * thousands of pixels deep and would take the stack down with it.
     */
    public void fill(final int x, final int y, final int colour) {
        if (!inside(x, y)) {
            return;
        }
        final int target = get(x, y);
        if (target == (colour & 0xFF)) {
            return;
        }
        /*
         * A cell is repainted the moment it is taken off, and only cells still of the target colour are put
         * on, so the same pixel can be queued more than once before it is reached. The stack therefore has
         * no fixed bound and grows rather than overflowing partway through a fill.
         */
        int[] stack = new int[Math.max(64, width * height / 4)];
        int top = 0;
        stack[top++] = x + y * width;
        while (top > 0) {
            final int cell = stack[--top];
            if ((pixels[cell] & 0xFF) != target) {
                continue;
            }
            pixels[cell] = (byte) (colour & 0xFF);
            final int cx = cell % width;
            final int cy = cell / width;
            if (top + 4 > stack.length) {
                stack = Arrays.copyOf(stack, stack.length * 2);
            }
            if (cx > 0 && (pixels[cell - 1] & 0xFF) == target) {
                stack[top++] = cell - 1;
            }
            if (cx < width - 1 && (pixels[cell + 1] & 0xFF) == target) {
                stack[top++] = cell + 1;
            }
            if (cy > 0 && (pixels[cell - width] & 0xFF) == target) {
                stack[top++] = cell - width;
            }
            if (cy < height - 1 && (pixels[cell + width] & 0xFF) == target) {
                stack[top++] = cell + width;
            }
        }
    }

    /** The colour an index names, as an opaque packed colour, or fully transparent for index 0. */
    public static int colourOf(final int index) {
        return PALETTE[Math.floorMod(index, COLOURS)];
    }

    /** Whether an index names the colour that is no colour at all. */
    public static boolean isTransparent(final int index) {
        return (index & 0xFF) == 0;
    }

    /**
     * Writes the picture as text, which is what goes on a disk.
     *
     * <p>The pixels are compressed and written as base64 rather than as a list of runs in plain numbers.
     * Runs in numbers read nicely and are wonderfully small for flat areas, but a busy picture falls back
     * to about six characters a pixel, which for a full canvas is ninety thousand of them: past what a
     * file may be handed across to a screen, so saving a detailed drawing would have failed rather than
     * been large. This way a full canvas is at most about twenty-two thousand characters whatever is drawn
     * on it, and an ordinary drawing is a fraction of that.
     */
    public String encode() {
        return MAGIC + '\n' + width + ' ' + height + '\n' + encodePixels(pixels);
    }

    /** Reads a picture back, or null when the text is not one. */
    public static PixImage decode(final String text) {
        if (text == null || !text.startsWith(MAGIC)) {
            return null;
        }
        final String[] lines = text.split("\n", 3);
        if (lines.length < 3) {
            return null;
        }
        final String[] size = lines[1].trim().split(" ");
        if (size.length != 2) {
            return null;
        }
        final PixImage image;
        try {
            image = new PixImage(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        } catch (final IllegalArgumentException bad) {
            return null;
        }
        final byte[] read = decodePixels(lines[2].trim());
        if (read == null || read.length != image.pixels.length) {
            return null;
        }
        System.arraycopy(read, 0, image.pixels, 0, read.length);
        return image;
    }

    /** Compresses the pixels and writes them as characters that cost one byte each on a disk. */
    private static String encodePixels(final byte[] raw) {
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(raw);
            deflater.finish();
            // Sized for the worst case deflate has, which is a little larger than what it was given.
            final byte[] buffer = new byte[raw.length + raw.length / 8 + 64];
            final int size = deflater.deflate(buffer);
            return Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, size));
        } finally {
            deflater.end();
        }
    }

    /** Takes the pixels back, or null when the text is not what it claims to be. */
    private static byte[] decodePixels(final String encoded) {
        final byte[] packed;
        try {
            packed = Base64.getDecoder().decode(encoded);
        } catch (final IllegalArgumentException notBase64) {
            return null;
        }
        final Inflater inflater = new Inflater();
        try {
            inflater.setInput(packed);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[4096];
            while (!inflater.finished()) {
                final int read = inflater.inflate(buffer);
                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    return null; // truncated
                }
                out.write(buffer, 0, read);
                if (out.size() > MAX_SIDE * MAX_SIDE) {
                    return null; // more pixels than any canvas has
                }
            }
            return out.toByteArray();
        } catch (final DataFormatException damaged) {
            return null;
        } finally {
            inflater.end();
        }
    }

    private boolean inside(final int x, final int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /** The first sixteen by hand, and the rest spread evenly so the palette has somewhere between them. */
    private static int[] buildPalette() {
        final int[] out = new int[COLOURS];
        final int[] first = {
                0x00000000, 0xFF000000, 0xFF3B2F2F, 0xFF7A4A2B, 0xFFB4231F, 0xFFE0553F, 0xFFF0B23A,
                0xFFF6E27A, 0xFFFFFFFF, 0xFF1F6B3A, 0xFF4FA05C, 0xFF9ED97A, 0xFF1C4FA8, 0xFF3A86D6,
                0xFF7FC4E8, 0xFF5B3A8C,
        };
        System.arraycopy(first, 0, out, 0, first.length);
        /*
         * A six by six by six cube of colour after them, then a ramp of greys, which is how an indexed
         * palette of this size has always been laid out and what keeps a photograph-like picture possible.
         */
        int at = first.length;
        for (int r = 0; r < 6 && at < COLOURS; r++) {
            for (int g = 0; g < 6 && at < COLOURS; g++) {
                for (int b = 0; b < 6 && at < COLOURS; b++) {
                    out[at++] = 0xFF000000 | (r * 51) << 16 | (g * 51) << 8 | (b * 51);
                }
            }
        }
        for (int i = 0; at < COLOURS; i++) {
            final int grey = Math.min(255, i * 255 / Math.max(1, COLOURS - at));
            out[at++] = 0xFF000000 | grey << 16 | grey << 8 | grey;
        }
        return out;
    }
}
