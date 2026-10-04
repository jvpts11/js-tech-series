/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;

/**
 * A bitmap font as its source describes it: the box every glyph fits in, where the baseline runs through that box,
 * the notice its makers put in it, and its glyphs by code point.
 *
 * <p>The box is the font's cell: {@link #cellWidth()} by {@link #cellHeight()} pixels, its bottom left corner
 * {@link #cellX()} to the right of the pen and {@link #cellY()} above the baseline (a font with descenders has a
 * negative {@code cellY}). A character-cell font, the kind terminals use, draws every glyph inside that one box.
 */
public final class BitmapFont {

    private final String name;
    private final int cellWidth;
    private final int cellHeight;
    private final int cellX;
    private final int cellY;
    private final String copyright;
    private final Map<Integer, BitmapGlyph> glyphs;

    /** @param copyright the notice the font carries about who made it and on what terms, or empty when none */
    public BitmapFont(final String name, final int cellWidth, final int cellHeight, final int cellX, final int cellY,
                      final String copyright, final Map<Integer, BitmapGlyph> glyphs) {
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException("a font's cell cannot be " + cellWidth + " by " + cellHeight);
        }
        this.name = name;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.cellX = cellX;
        this.cellY = cellY;
        this.copyright = copyright;
        this.glyphs = Collections.unmodifiableMap(new TreeMap<>(glyphs));
    }

    public String name() {
        return name;
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    public int cellX() {
        return cellX;
    }

    public int cellY() {
        return cellY;
    }

    /** How many rows of the cell are above the baseline; the rest are for descenders. */
    public int baseline() {
        return cellHeight + cellY;
    }

    public String copyright() {
        return copyright;
    }

    /** The glyph for that code point, or null when the font has none. */
    public @Nullable BitmapGlyph glyph(final int codePoint) {
        return glyphs.get(codePoint);
    }

    public boolean covers(final int codePoint) {
        return glyphs.containsKey(codePoint);
    }

    /** Every glyph, in code point order. */
    public Collection<BitmapGlyph> glyphs() {
        return glyphs.values();
    }
}
