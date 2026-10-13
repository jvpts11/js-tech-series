/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Reads a font in the Glyph Bitmap Distribution Format (BDF), the plain text format X11's bitmap fonts are kept in.
 *
 * <p>A BDF file says the font's bounding box, a list of properties and then every glyph: its code point, how far the
 * pen moves after it, its own box against the pen and its pixels, one line of hexadecimal digits per row, the
 * leftmost pixel in the highest bit. Only what draws a glyph is read; comments, the size in points and the other
 * properties are passed over, except the copyright notice, which travels with the font. A glyph with no code point
 * (encoded as -1) has no character to stand for and is left out.
 *
 * <p>A file that breaks the format is refused with the line it breaks it on, rather than read into a font that
 * draws the wrong pixels.
 */
public final class BdfReader {

    private BdfReader() {
    }

    /** The font that text holds. */
    public static BitmapFont read(final String text) {
        try {
            return read(new StringReader(text));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The font that reader holds, read to its end.
     *
     * @throws BdfFormatException when the text is not a BDF font, naming the line where it stops being one
     */
    public static BitmapFont read(final Reader reader) throws IOException {
        return new Parse(reader instanceof BufferedReader buffered ? buffered : new BufferedReader(reader)).font();
    }

    /** The reading of one file, line by line. */
    private static final class Parse {

        private final BufferedReader in;
        private final Map<Integer, BitmapGlyph> glyphs = new HashMap<>();
        private int line;
        private String name = "";
        private String copyright = "";
        private int @Nullable [] box;
        private int fontAdvance = -1;

        Parse(final BufferedReader in) {
            this.in = in;
        }

        BitmapFont font() throws IOException {
            final String first = next();
            if (first == null || !first.startsWith("STARTFONT")) {
                throw new BdfFormatException(line, "a BDF font starts with STARTFONT");
            }
            for (String text = next(); text != null; text = next()) {
                final String[] words = text.trim().split("\\s+");
                switch (words[0]) {
                    case "FONT" -> name = text.trim().substring(4).trim();
                    case "FONTBOUNDINGBOX" -> box = numbers(words, 4);
                    case "DWIDTH" -> fontAdvance = numbers(words, 2)[0];
                    case "STARTPROPERTIES" -> properties();
                    case "STARTCHAR" -> glyph();
                    case "ENDFONT" -> {
                        return finish();
                    }
                    default -> {
                        // Comments, sizes, the glyph count and anything later versions add say nothing a glyph needs.
                    }
                }
            }
            throw new BdfFormatException(line, "the font ends without ENDFONT");
        }

        private BitmapFont finish() {
            if (box == null) {
                throw new BdfFormatException(line, "the font never says its FONTBOUNDINGBOX");
            }
            return new BitmapFont(name, box[0], box[1], box[2], box[3], copyright, glyphs);
        }

        private void properties() throws IOException {
            for (String text = next(); text != null; text = next()) {
                final String trimmed = text.trim();
                if (trimmed.equals("ENDPROPERTIES")) {
                    return;
                }
                if (trimmed.split("\\s+")[0].equals("COPYRIGHT")) {
                    copyright = unquote(trimmed.substring("COPYRIGHT".length()).trim());
                }
            }
            throw new BdfFormatException(line, "the properties never end with ENDPROPERTIES");
        }

        private void glyph() throws IOException {
            int codePoint = -1;
            int advance = fontAdvance;
            int[] glyphBox = null;
            for (String text = next(); text != null; text = next()) {
                final String[] words = text.trim().split("\\s+");
                switch (words[0]) {
                    case "ENCODING" -> codePoint = numbers(words, 1)[0];
                    case "DWIDTH" -> advance = numbers(words, 2)[0];
                    case "BBX" -> glyphBox = numbers(words, 4);
                    case "BITMAP" -> {
                        if (glyphBox == null) {
                            throw new BdfFormatException(line, "a glyph's BITMAP comes before its BBX");
                        }
                        final BitSet pixels = bitmap(glyphBox[0], glyphBox[1]);
                        if (codePoint >= 0) {
                            if (glyphs.containsKey(codePoint)) {
                                throw new BdfFormatException(line, "code point " + codePoint + " is defined twice");
                            }
                            glyphs.put(codePoint, new BitmapGlyph(codePoint, glyphBox[0], glyphBox[1], glyphBox[2],
                                    glyphBox[3], advance >= 0 ? advance : glyphBox[0], pixels));
                        }
                        return;
                    }
                    case "ENDCHAR" -> throw new BdfFormatException(line, "a glyph ends without a BITMAP");
                    default -> {
                        // SWIDTH and the vertical metrics are for printers and vertical writing, not for a screen.
                    }
                }
            }
            throw new BdfFormatException(line, "the font ends inside a glyph");
        }

        /** The rows after BITMAP up to ENDCHAR, which must be exactly as many as the glyph is tall. */
        private BitSet bitmap(final int width, final int height) throws IOException {
            final BitSet pixels = new BitSet(width * height);
            int row = 0;
            for (String text = next(); text != null; text = next()) {
                final String hex = text.trim();
                if (hex.equals("ENDCHAR")) {
                    if (row != height) {
                        throw new BdfFormatException(line, "a glyph " + height + " rows tall has " + row
                                + " rows of pixels");
                    }
                    return pixels;
                }
                if (row == height) {
                    throw new BdfFormatException(line, "a glyph " + height + " rows tall has more rows of pixels");
                }
                if (hex.length() * 4 < width) {
                    throw new BdfFormatException(line, "a row of pixels is shorter than the glyph is wide");
                }
                for (int x = 0; x < width; x++) {
                    final int digit = Character.digit(hex.charAt(x / 4), 16);
                    if (digit < 0) {
                        throw new BdfFormatException(line,
                                "a row of pixels holds something that is not a hexadecimal digit");
                    }
                    if ((digit & (8 >> (x % 4))) != 0) {
                        pixels.set(row * width + x);
                    }
                }
                row++;
            }
            throw new BdfFormatException(line, "the font ends inside a glyph's pixels");
        }

        private int[] numbers(final String[] words, final int count) {
            if (words.length < count + 1) {
                throw new BdfFormatException(line, words[0] + " needs " + count + " numbers");
            }
            final int[] out = new int[count];
            for (int i = 0; i < count; i++) {
                try {
                    out[i] = Integer.parseInt(words[i + 1]);
                } catch (final NumberFormatException notANumber) {
                    throw new BdfFormatException(line, words[0] + " has " + words[i + 1] + " where a number goes");
                }
            }
            return out;
        }

        private String next() throws IOException {
            final String text = in.readLine();
            if (text != null) {
                line++;
            }
            return text;
        }

        private static String unquote(final String value) {
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                return value.substring(1, value.length() - 1).replace("\"\"", "\"");
            }
            return value;
        }
    }
}
