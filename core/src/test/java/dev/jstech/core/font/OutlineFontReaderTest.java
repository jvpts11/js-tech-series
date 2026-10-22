/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutlineFontReaderTest {

    /* The fixed 6 by 10 font, and the same drawn as a square for each of its pixels in a TrueType file. */
    private static final Path FIXED = Path.of("src", "main", "resources", "assets", "jscore", "font", "source",
            "fixed_6x10.bdf");
    private static final Path SQUARES = Path.of("src", "test", "resources", "font", "test_squares.ttf");
    private static final int PIXELS = 10;
    private static final int COLUMNS = 16;

    @Test
    void read_givesTheCellAndBaselineOfTheFontAtItsSize() throws IOException {
        final BitmapFont read = squares();
        assertEquals(6, read.cellWidth());
        assertEquals(10, read.cellHeight());
        assertEquals(8, read.baseline());
    }

    @Test
    void read_drawsAFontOnThePixelGridBackPixelForPixel() throws IOException {
        final BitmapFont outline = squares();
        final BitmapFont bitmap;
        try (Reader reader = Files.newBufferedReader(FIXED, StandardCharsets.US_ASCII)) {
            bitmap = BdfReader.read(reader);
        }
        final Map<Integer, BitmapGlyph> same = new HashMap<>();
        for (final BitmapGlyph glyph : outline.glyphs()) {
            final BitmapGlyph original = bitmap.glyph(glyph.codePoint());
            assertNotNull(original, "the bitmap font has U+" + Integer.toHexString(glyph.codePoint()));
            same.put(glyph.codePoint(), original);
        }
        assertTrue(same.size() > 150, "the outline font holds Latin: " + same.size());
        final FontSheet expected = FontSheet.of(new BitmapFont("fixed", bitmap.cellWidth(), bitmap.cellHeight(),
                bitmap.cellX(), bitmap.cellY(), "", same), COLUMNS);
        final FontSheet drawn = FontSheet.of(outline, COLUMNS);
        assertEquals(expected.rows(), drawn.rows());
        for (int y = 0; y < expected.height(); y++) {
            for (int x = 0; x < expected.width(); x++) {
                assertEquals(expected.lit(x, y), drawn.lit(x, y), "the pixel at " + x + ", " + y);
            }
        }
    }

    @Test
    void read_refusesWhatIsNoFont() {
        final InputStream text = new ByteArrayInputStream("not a font".getBytes(StandardCharsets.US_ASCII));
        assertThrows(IOException.class, () -> OutlineFontReader.read(text, PIXELS, "nothing"));
    }

    private static BitmapFont squares() throws IOException {
        try (InputStream in = Files.newInputStream(SQUARES)) {
            return OutlineFontReader.read(in, PIXELS, "test_squares");
        }
    }
}
