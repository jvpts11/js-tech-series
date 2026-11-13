/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.jetbrains.annotations.Nullable;

/**
 * A picture a player brought or took, made small on the server: an album's cover, a camera's picture in its gallery.
 * Read only when it says it is no larger than it may be, then brought down with each pixel the average of the part
 * of the picture it stands for, so a large picture keeps its look, and written out as a PNG.
 *
 * <p>Pure: no part of the game.
 */
public final class PictureScaling {

    private PictureScaling() {
    }

    /**
     * The pixels of the picture in {@code picture}, or null when it is no picture this reads or says it is larger
     * than {@code mostPixels}. The size a file declares is read before a pixel of it is: a few bytes of PNG can
     * declare a picture whose pixels fill gigabytes.
     */
    @Nullable
    public static BufferedImage decode(final byte[] picture, final long mostPixels) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(picture))) {
            if (stream == null) {
                return null;
            }
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }
            final ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                final long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                return pixels <= 0 || pixels > mostPixels ? null : reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * The part of {@code image} {@code w} by {@code h} from {@code (left, top)}, brought to {@code outW} by
     * {@code outH} pixels, each the average of the part it stands for.
     */
    public static BufferedImage scale(final BufferedImage image, final int left, final int top, final int w,
                                      final int h, final int outW, final int outH) {
        final BufferedImage small = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < outH; y++) {
            for (int x = 0; x < outW; x++) {
                // In longs, as a part millions of pixels wide would go round an int.
                small.setRGB(x, y, average(image, left + (int) ((long) x * w / outW),
                        top + (int) ((long) y * h / outH), left + (int) ((long) (x + 1) * w / outW),
                        top + (int) ((long) (y + 1) * h / outH)));
            }
        }
        return small;
    }

    /** {@code image} written out as a PNG. */
    public static byte[] png(final BufferedImage image) throws IOException {
        final ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return png.toByteArray();
    }

    private static int average(final BufferedImage image, final int x0, final int y0, final int x1, final int y1) {
        long a = 0;
        long r = 0;
        long g = 0;
        long b = 0;
        int n = 0;
        for (int y = y0; y < Math.max(y0 + 1, y1); y++) {
            for (int x = x0; x < Math.max(x0 + 1, x1); x++) {
                final int argb = image.getRGB(x, y);
                a += argb >>> 24;
                r += argb >> 16 & 0xFF;
                g += argb >> 8 & 0xFF;
                b += argb & 0xFF;
                n++;
            }
        }
        return (int) (a / n) << 24 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }
}
