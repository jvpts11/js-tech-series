/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.IntBinaryOperator;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Writes a picture as a PNG file, eight bits to a channel with an alpha channel, from a colour for each pixel.
 *
 * <p>The same picture always comes out as the same bytes, so a picture the data generator writes again unchanged is
 * seen to be unchanged and left alone.
 */
public final class PngWriter {

    /** The eight bytes every PNG file starts with. */
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    /** The colour type of red, green, blue and alpha, eight bits each. */
    private static final int RGBA = 6;

    private PngWriter() {
    }

    /**
     * That picture as PNG bytes.
     *
     * @param colourAt the colour of the pixel at ({@code x}, {@code y}), as ARGB
     */
    public static byte[] rgba(final int width, final int height, final IntBinaryOperator colourAt) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("a picture cannot be " + width + " by " + height);
        }
        final byte[] raw = new byte[height * (1 + width * 4)];
        int at = 0;
        for (int y = 0; y < height; y++) {
            // Each row starts with the filter it was written with; none, so a reader takes the bytes as they are.
            raw[at++] = 0;
            for (int x = 0; x < width; x++) {
                final int argb = colourAt.applyAsInt(x, y);
                raw[at++] = (byte) (argb >> 16);
                raw[at++] = (byte) (argb >> 8);
                raw[at++] = (byte) argb;
                raw[at++] = (byte) (argb >>> 24);
            }
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(SIGNATURE);
        final ByteArrayOutputStream header = new ByteArrayOutputStream();
        writeInt(header, width);
        writeInt(header, height);
        header.write(8);
        header.write(RGBA);
        header.write(0);
        header.write(0);
        header.write(0);
        chunk(out, "IHDR", header.toByteArray());
        chunk(out, "IDAT", deflate(raw));
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static byte[] deflate(final byte[] raw) {
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(raw);
            deflater.finish();
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                out.write(buffer, 0, deflater.deflate(buffer));
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /** One chunk: its length, its type, its data and the checksum of the type and data. */
    private static void chunk(final ByteArrayOutputStream out, final String type, final byte[] data) {
        final byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        writeInt(out, data.length);
        out.writeBytes(name);
        out.writeBytes(data);
        final CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        writeInt(out, (int) crc.getValue());
    }

    private static void writeInt(final ByteArrayOutputStream out, final int value) {
        out.write(value >>> 24);
        out.write(value >>> 16);
        out.write(value >>> 8);
        out.write(value);
    }
}
