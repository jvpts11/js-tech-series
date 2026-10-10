/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SoundfoundryCoversTest {

    @Test
    void scaled_makesASmallSquareOfAPicture() throws IOException {
        final ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB), "png", png);
        final byte[] cover = SoundfoundryCovers.scaled(png.toByteArray());
        assertTrue(cover.length > 0, "an ordinary picture makes a cover");
    }

    @Test
    void scaled_refusesAPictureThatDeclaresMorePixelsThanACoverIsMadeFrom() throws IOException {
        // A few dozen bytes that say the picture is a hundred thousand pixels a side; decoded, that is 40 GB.
        final byte[] bomb = pngHeader(100_000, 100_000);
        assertEquals(0, SoundfoundryCovers.scaled(bomb).length);
    }

    @Test
    void scaled_refusesWhatIsNoPicture() {
        assertEquals(0, SoundfoundryCovers.scaled("not a picture".getBytes(StandardCharsets.UTF_8)).length);
    }

    /* The signature and the header of a PNG of that size, with no pixels after them. */
    private static byte[] pngHeader(final int width, final int height) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
            final ByteArrayOutputStream header = new ByteArrayOutputStream();
            try (DataOutputStream ihdr = new DataOutputStream(header)) {
                ihdr.writeBytes("IHDR");
                ihdr.writeInt(width);
                ihdr.writeInt(height);
                ihdr.write(new byte[] {8, 6, 0, 0, 0});
            }
            final byte[] chunk = header.toByteArray();
            out.writeInt(chunk.length - 4);
            out.write(chunk);
            final CRC32 crc = new CRC32();
            crc.update(chunk);
            out.writeInt((int) crc.getValue());
        }
        return bytes.toByteArray();
    }
}
