/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A bitmap font laid out the way the game's bitmap fonts are drawn from: one picture cut into equal cells, a row of
 * characters for each row of cells saying which character each cell holds.
 *
 * <p>Every cell is the font's own cell, and every glyph is put in it where the font puts it against the pen and the
 * baseline, so the cell carries the glyph's place as well as its pixels and a glyph drawn at a cell's left edge lands
 * where the font means it to. A glyph that reaches outside the font's cell is cut to it. The glyphs fill the cells in
 * code point order, {@code columns} to a row, and the cells after the last one hold nothing, which the row says with
 * the character U+0000.
 *
 * <p>A glyph with no pixels is a space of some width and has nothing to put in a cell; those are kept apart with how
 * wide each is ({@link #spaces()}), since the game measures a cell's character by its lit pixels and would make a
 * space one pixel wide. A letter or a digit with no pixels is not a space but a glyph the font's makers left empty,
 * and is left out, so the game's font draws it instead of nothing. Control characters are left out too: they are
 * not drawn.
 */
public final class FontSheet {

    private final int columns;
    private final int cellWidth;
    private final int cellHeight;
    private final List<String> rows;
    private final Map<Integer, Integer> spaces;
    private final BitSet pixels;

    private FontSheet(final int columns, final int cellWidth, final int cellHeight, final List<String> rows,
                      final Map<Integer, Integer> spaces, final BitSet pixels) {
        this.columns = columns;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.rows = List.copyOf(rows);
        this.spaces = Collections.unmodifiableMap(new LinkedHashMap<>(spaces));
        this.pixels = pixels;
    }

    /** That font laid out {@code columns} cells across. */
    public static FontSheet of(final BitmapFont font, final int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("a sheet needs at least one column, not " + columns);
        }
        final List<BitmapGlyph> drawn = new ArrayList<>();
        final Map<Integer, Integer> spaces = new LinkedHashMap<>();
        for (final BitmapGlyph glyph : font.glyphs()) {
            if (!drawable(glyph.codePoint())) {
                continue;
            }
            if (glyph.isBlank() && !Character.isLetterOrDigit(glyph.codePoint())) {
                spaces.put(glyph.codePoint(), glyph.advance());
            } else if (!glyph.isBlank()) {
                drawn.add(glyph);
            }
        }
        final int rowCount = Math.max(1, (drawn.size() + columns - 1) / columns);
        final int width = columns * font.cellWidth();
        final BitSet pixels = new BitSet(width * rowCount * font.cellHeight());
        final List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < rowCount * columns; i++) {
            if (i < drawn.size()) {
                final BitmapGlyph glyph = drawn.get(i);
                row.appendCodePoint(glyph.codePoint());
                place(font, glyph, (i % columns) * font.cellWidth(), (i / columns) * font.cellHeight(), width,
                        pixels);
            } else {
                row.append('\u0000');
            }
            if (i % columns == columns - 1) {
                rows.add(row.toString());
                row = new StringBuilder();
            }
        }
        return new FontSheet(columns, font.cellWidth(), font.cellHeight(), rows, spaces, pixels);
    }

    public int columns() {
        return columns;
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    /** How wide the picture is, in pixels. */
    public int width() {
        return columns * cellWidth;
    }

    /** How tall the picture is, in pixels. */
    public int height() {
        return rows.size() * cellHeight;
    }

    /** The characters each row of cells holds, U+0000 for an empty cell. */
    public List<String> rows() {
        return rows;
    }

    /** The characters with nothing to draw, and how far each moves the pen, in code point order. */
    public Map<Integer, Integer> spaces() {
        return spaces;
    }

    /** Whether that pixel of the picture is lit. */
    public boolean lit(final int x, final int y) {
        return x >= 0 && x < width() && y >= 0 && y < height() && pixels.get(y * width() + x);
    }

    /** The picture as a PNG: white where it is lit, clear everywhere else, the way the game's font pictures are. */
    public byte[] png() {
        return PngWriter.rgba(width(), height(), (x, y) -> lit(x, y) ? 0xFFFFFFFF : 0);
    }

    private static boolean drawable(final int codePoint) {
        return codePoint > 0 && Character.isValidCodePoint(codePoint) && !Character.isISOControl(codePoint)
                && (codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE);
    }

    /** Copies a glyph's pixels into the cell whose top left corner is at ({@code left}, {@code top}). */
    private static void place(final BitmapFont font, final BitmapGlyph glyph, final int left, final int top,
                              final int width, final BitSet pixels) {
        final int column = glyph.xOffset() - font.cellX();
        final int row = font.baseline() - glyph.yOffset() - glyph.height();
        for (int y = 0; y < glyph.height(); y++) {
            final int cellY = row + y;
            if (cellY < 0 || cellY >= font.cellHeight()) {
                continue;
            }
            for (int x = 0; x < glyph.width(); x++) {
                final int cellX = column + x;
                if (cellX >= 0 && cellX < font.cellWidth() && glyph.lit(x, y)) {
                    pixels.set((top + cellY) * width + left + cellX);
                }
            }
        }
    }
}
