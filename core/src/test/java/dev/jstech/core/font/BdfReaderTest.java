/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BdfReaderTest {

    /** The Core's terminal font, read where the data generator reads it. */
    private static final Path FIXED = Path.of("src", "main", "resources", "assets", "jscore", "font", "source",
            "fixed_6x10.bdf");

    @Test
    void read_takesTheBoxTheBaselineAndTheCopyright() {
        final BitmapFont font = BdfReader.read(small());
        assertEquals(6, font.cellWidth());
        assertEquals(10, font.cellHeight());
        assertEquals(0, font.cellX());
        assertEquals(-2, font.cellY());
        assertEquals(8, font.baseline());
        assertEquals("Test font.", font.copyright());
        assertEquals("-Test-Small", font.name());
    }

    @Test
    void read_takesEachGlyphsPixelsLeftmostInTheHighestBit() {
        final BitmapGlyph a = BdfReader.read(small()).glyph('A');
        assertNotNull(a);
        assertEquals(6, a.advance());
        assertTrue(a.lit(2, 1), "the apex of the A");
        assertFalse(a.lit(0, 1));
        assertTrue(a.lit(0, 5) && a.lit(4, 5), "the bar of the A runs across");
        assertFalse(a.lit(5, 5), "a bit past the glyph's width is padding, not a pixel");
    }

    @Test
    void read_keepsAGlyphsOwnBoxAndOffsets() {
        final BitmapGlyph dot = BdfReader.read(small()).glyph('.');
        assertNotNull(dot);
        assertEquals(2, dot.width());
        assertEquals(2, dot.height());
        assertEquals(2, dot.xOffset());
        assertEquals(0, dot.yOffset());
        assertTrue(dot.lit(0, 0) && dot.lit(1, 1));
    }

    @Test
    void read_leavesOutAGlyphWithNoCodePoint() {
        final BitmapFont font = BdfReader.read(small());
        assertEquals(3, font.glyphs().size(), "A, the dot and the space; the unencoded glyph has no character");
        assertTrue(font.glyph(' ').isBlank());
    }

    @Test
    void read_refusesATextThatIsNotAFont() {
        final BdfFormatException thrown = assertThrows(BdfFormatException.class, () -> BdfReader.read("hello\n"));
        assertEquals(1, thrown.line());
        assertTrue(thrown.getMessage().contains("line 1"), thrown.getMessage());
    }

    @Test
    void read_namesTheLineOfAGlyphWithTooFewRows() {
        final String broken = small().replace("BITMAP\n00\n20\n50\n88\n88\nF8\n88\n88\n00\n00\nENDCHAR",
                "BITMAP\n00\n20\nENDCHAR");
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> BdfReader.read(broken));
        assertTrue(thrown.getMessage().contains("10 rows tall has 2 rows"), thrown.getMessage());
    }

    @Test
    void read_refusesARowThatIsNotHexadecimal() {
        final String broken = small().replace("\nF8\n", "\nZ8\n");
        assertThrows(IllegalArgumentException.class, () -> BdfReader.read(broken));
    }

    @Test
    void read_refusesAFontThatNeverEnds() {
        final String broken = small().replace("ENDFONT\n", "");
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> BdfReader.read(broken));
        assertTrue(thrown.getMessage().contains("without ENDFONT"), thrown.getMessage());
    }

    @Test
    void read_readsTheWholeTerminalFont() throws IOException {
        final BitmapFont font = fixed();
        assertEquals(1597, font.glyphs().size());
        assertEquals(6, font.cellWidth());
        assertEquals(10, font.cellHeight());
        assertEquals(8, font.baseline());
        assertTrue(font.copyright().toLowerCase().contains("public domain"), font.copyright());
        final BitmapGlyph a = font.glyph('A');
        assertNotNull(a);
        for (int x = 0; x < 5; x++) {
            assertTrue(a.lit(x, 5), "the bar of the A, row 5, column " + x);
        }
        assertTrue(font.covers(0x2500) && font.covers(0x2588) && font.covers(0x00E7), "boxes, blocks and accents");
    }

    private static BitmapFont fixed() throws IOException {
        try (Reader reader = Files.newBufferedReader(FIXED, StandardCharsets.US_ASCII)) {
            return BdfReader.read(reader);
        }
    }

    /** A font of three characters and one glyph with no code point. */
    private static String small() {
        return """
                STARTFONT 2.1
                COMMENT a font for the tests
                FONT -Test-Small
                SIZE 10 75 75
                FONTBOUNDINGBOX 6 10 0 -2
                STARTPROPERTIES 2
                FONT_ASCENT 8
                COPYRIGHT "Test font."
                ENDPROPERTIES
                CHARS 4
                STARTCHAR A
                ENCODING 65
                SWIDTH 576 0
                DWIDTH 6 0
                BBX 6 10 0 -2
                BITMAP
                00
                20
                50
                88
                88
                F8
                88
                88
                00
                00
                ENDCHAR
                STARTCHAR period
                ENCODING 46
                DWIDTH 6 0
                BBX 2 2 2 0
                BITMAP
                80
                40
                ENDCHAR
                STARTCHAR space
                ENCODING 32
                DWIDTH 6 0
                BBX 6 10 0 -2
                BITMAP
                00
                00
                00
                00
                00
                00
                00
                00
                00
                00
                ENDCHAR
                STARTCHAR unencoded
                ENCODING -1
                DWIDTH 6 0
                BBX 1 1 0 0
                BITMAP
                80
                ENDCHAR
                ENDFONT
                """;
    }
}
