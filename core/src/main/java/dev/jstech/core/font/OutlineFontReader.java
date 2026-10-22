/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a TrueType or OpenType font into a bitmap font, drawn at a size of so many pixels with no smoothing, every
 * glyph a pixel lit or not as the game's bitmap fonts are: the characters of Latin and of drawing boxes it has, each in
 * a cell as wide as its widest and as tall as its ascent and descent, the baseline where the font puts it. A font drawn
 * on the pixel grid, as a pixel font is, comes back pixel for pixel.
 */
public final class OutlineFontReader {

    /** The characters read, as ranges of code points, first and last: Latin, and lines and blocks for drawing boxes. */
    public static final List<int[]> CHARACTERS = List.of(new int[] {0x20, 0x7E}, new int[] {0xA0, 0x24F},
            new int[] {0x2500, 0x259F});

    private static final String MEASURED = "Hg";

    private OutlineFontReader() {
    }

    /**
     * The font {@code in} holds, a TrueType or OpenType file, drawn {@code pixels} tall, named {@code name}.
     *
     * @throws IOException when the file cannot be read as a font
     */
    public static BitmapFont read(final InputStream in, final int pixels, final String name) throws IOException {
        final Font font;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont((float) pixels);
        } catch (final FontFormatException notAFont) {
            throw new IOException("the font " + name + " is no TrueType or OpenType font", notAFont);
        }
        return of(font, name);
    }

    /** {@code font}, at the size it is, as a bitmap font named {@code name}. */
    public static BitmapFont of(final Font font, final String name) {
        final FontRenderContext context = new FontRenderContext(null, false, false);
        final LineMetrics metrics = font.getLineMetrics(MEASURED, context);
        final int ascent = Math.round(metrics.getAscent());
        final int descent = Math.max(0, Math.round(metrics.getDescent()));
        final Map<Integer, Integer> advances = new HashMap<>();
        final Map<Integer, Shape> outlines = new HashMap<>();
        int widest = 1;
        for (final int[] range : CHARACTERS) {
            for (int codePoint = range[0]; codePoint <= range[1]; codePoint++) {
                if (!font.canDisplay(codePoint)) {
                    continue;
                }
                final GlyphVector glyph = font.createGlyphVector(context, Character.toChars(codePoint));
                final int advance = Math.max(0, Math.round(glyph.getGlyphMetrics(0).getAdvance()));
                advances.put(codePoint, advance);
                outlines.put(codePoint, glyph.getGlyphOutline(0, 0, ascent));
                widest = Math.max(widest, advance);
            }
        }
        final int height = Math.max(1, ascent + descent);
        final Map<Integer, BitmapGlyph> glyphs = new HashMap<>();
        for (final Map.Entry<Integer, Shape> outline : outlines.entrySet()) {
            glyphs.put(outline.getKey(), new BitmapGlyph(outline.getKey(), widest, height, 0, -descent,
                    advances.get(outline.getKey()), lit(outline.getValue(), widest, height)));
        }
        return new BitmapFont(name, widest, height, 0, -descent, "", glyphs);
    }

    /* The pixels of a cell {@code width} by {@code height} the outline covers, drawn with no smoothing. */
    private static BitSet lit(final Shape outline, final int width, final int height) {
        final BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics2D g = picture.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.fill(outline);
        } finally {
            g.dispose();
        }
        final BitSet pixels = new BitSet(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (picture.getRaster().getSample(x, y, 0) != 0) {
                    pixels.set(y * width + x);
                }
            }
        }
        return pixels;
    }
}
