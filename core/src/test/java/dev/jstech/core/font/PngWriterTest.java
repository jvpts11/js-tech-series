/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PngWriterTest {

    @Test
    void rgba_isReadBackAsTheSamePicture() throws IOException {
        final byte[] png = PngWriter.rgba(3, 2, (x, y) -> x == 1 && y == 0 ? 0xFFFFFFFF : x == 2 ? 0x80FF0000 : 0);
        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertNotNull(image, "a PNG reader takes it");
        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(0xFFFFFFFF, image.getRGB(1, 0));
        assertEquals(0x80FF0000, image.getRGB(2, 1));
        assertEquals(0, image.getRGB(0, 1) >>> 24, "an unlit pixel is clear");
    }

    @Test
    void rgba_writesTheSameBytesForTheSamePicture() {
        assertArrayEquals(PngWriter.rgba(4, 4, (x, y) -> (x + y) % 2 == 0 ? 0xFFFFFFFF : 0),
                PngWriter.rgba(4, 4, (x, y) -> (x + y) % 2 == 0 ? 0xFFFFFFFF : 0));
    }

    @Test
    void rgba_refusesAPictureOfNoSize() {
        assertThrows(IllegalArgumentException.class, () -> PngWriter.rgba(0, 4, (x, y) -> 0));
    }
}
