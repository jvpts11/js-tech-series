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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FontSheetTest {

    @Test
    void of_putsEachGlyphInItsCellWhereTheFontPutsIt() {
        final FontSheet sheet = FontSheet.of(font(glyph('.', 2, 2, 2, 0)), 4);
        /*
         * A dot two pixels wide, two right of the pen, sitting on the baseline: rows 6 and 7 of a cell with eight
         * rows above the baseline.
         */
        assertTrue(sheet.lit(2, 6) && sheet.lit(3, 6) && sheet.lit(2, 7) && sheet.lit(3, 7));
        assertFalse(sheet.lit(2, 8), "below the baseline is the descender, empty for a dot");
        assertFalse(sheet.lit(1, 6));
    }

    @Test
    void of_putsADescenderBelowTheBaseline() {
        final FontSheet sheet = FontSheet.of(font(glyph('g', 1, 2, 0, -2)), 4);
        assertTrue(sheet.lit(0, 8) && sheet.lit(0, 9), "the two rows under the baseline");
    }

    @Test
    void of_cutsAGlyphToTheFontsCell() {
        final FontSheet sheet = FontSheet.of(font(glyph('W', 8, 1, -1, 0)), 1);
        assertEquals(6, sheet.width(), "one column, one cell wide");
        for (int x = 0; x < 6; x++) {
            assertTrue(sheet.lit(x, 7), "column " + x);
        }
    }

    @Test
    void of_fillsTheRowsInCodePointOrderAndMarksTheEmptyCells() {
        final FontSheet sheet = FontSheet.of(font(glyph('c', 1, 1, 0, 0), glyph('a', 1, 1, 0, 0),
                glyph('b', 1, 1, 0, 0)), 2);
        assertEquals(2, sheet.rows().size());
        assertEquals("ab", sheet.rows().get(0));
        assertEquals("c\u0000", sheet.rows().get(1));
        assertEquals(12, sheet.width());
        assertEquals(20, sheet.height());
        assertTrue(sheet.lit(6, 7), "b, the second cell of the first row");
        assertTrue(sheet.lit(0, 17), "c, the first cell of the second row");
    }

    @Test
    void of_keepsTheSpacesApartWithTheirWidths() {
        final FontSheet sheet = FontSheet.of(font(glyph('a', 1, 1, 0, 0),
                new BitmapGlyph(' ', 6, 10, 0, -2, 6, new BitSet())), 4);
        assertEquals(Map.of((int) ' ', 6), sheet.spaces());
        assertEquals("a\u0000\u0000\u0000", sheet.rows().get(0), "a space has no cell");
    }

    @Test
    void of_leavesOutALetterLeftEmptyRatherThanMakeItASpace() {
        final FontSheet sheet = FontSheet.of(font(glyph('a', 1, 1, 0, 0),
                new BitmapGlyph('B', 6, 10, 0, -2, 6, new BitSet())), 4);
        assertTrue(sheet.spaces().isEmpty());
        assertEquals("a\u0000\u0000\u0000", sheet.rows().get(0));
    }

    @Test
    void of_leavesOutTheControlCharacters() {
        final FontSheet sheet = FontSheet.of(font(glyph(0, 1, 1, 0, 0), glyph(0x07, 1, 1, 0, 0),
                glyph(0x9B, 1, 1, 0, 0), glyph('x', 1, 1, 0, 0)), 4);
        assertEquals("x\u0000\u0000\u0000", sheet.rows().get(0));
    }

    @Test
    void of_laysTheWholeTerminalFontOut() throws IOException {
        final BitmapFont font = fixed();
        final FontSheet sheet = FontSheet.of(font, 32);
        int cells = 0;
        for (final String row : sheet.rows()) {
            assertEquals(32, row.length(), "every row holds a character, or nothing, for each of its cells");
            cells += (int) row.chars().filter(c -> c != 0).count();
        }
        assertEquals(font.glyphs().stream().filter(glyph -> glyph.codePoint() > 0 && !glyph.isBlank()).count(),
                cells, "every glyph with pixels has a cell, but the one for code point 0");
        assertEquals(192, sheet.width());
        assertTrue(sheet.spaces().containsKey((int) ' ') && sheet.spaces().get((int) ' ') == 6);
        // The full block fills its whole cell.
        final int block = cellOf(sheet, 0x2588);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 6; x++) {
                assertTrue(sheet.lit(block % 32 * 6 + x, block / 32 * 10 + y), "the full block at " + x + "," + y);
            }
        }
    }

    @Test
    void png_isThePictureTheSheetHolds() throws IOException {
        final FontSheet sheet = FontSheet.of(font(glyph('a', 1, 1, 0, 0)), 2);
        final byte[] png = sheet.png();
        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
    }

    private static int cellOf(final FontSheet sheet, final int codePoint) {
        int index = 0;
        for (final String row : sheet.rows()) {
            for (final int c : row.codePoints().toArray()) {
                if (c == codePoint) {
                    return index;
                }
                index++;
            }
        }
        throw new AssertionError(Integer.toHexString(codePoint) + " has no cell");
    }

    /** A font with a six by ten cell and eight rows above the baseline, holding those glyphs. */
    private static BitmapFont font(final BitmapGlyph... glyphs) {
        final Map<Integer, BitmapGlyph> byCode = new HashMap<>();
        for (final BitmapGlyph glyph : glyphs) {
            byCode.put(glyph.codePoint(), glyph);
        }
        return new BitmapFont("test", 6, 10, 0, -2, "", byCode);
    }

    /** A glyph whose every pixel is lit. */
    private static BitmapGlyph glyph(final int codePoint, final int width, final int height, final int xOffset,
                                     final int yOffset) {
        final BitSet pixels = new BitSet();
        pixels.set(0, width * height);
        return new BitmapGlyph(codePoint, width, height, xOffset, yOffset, 6, pixels);
    }

    private static BitmapFont fixed() throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of("src", "main", "resources", "assets", "jscore", "font",
                "source", "fixed_6x10.bdf"), StandardCharsets.US_ASCII)) {
            return BdfReader.read(reader);
        }
    }
}
