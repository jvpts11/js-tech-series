/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import java.util.BitSet;

/**
 * One character of a bitmap font: its pixels, where they sit against the pen, and how far the pen moves on after it.
 *
 * <p>The pixels form a box {@link #width()} wide and {@link #height()} tall. Its bottom left corner is
 * {@link #xOffset()} to the right of the pen and {@link #yOffset()} above the baseline, so a letter with a descender
 * has a negative offset, the way bitmap font formats describe it.
 */
public final class BitmapGlyph {

    private final int codePoint;
    private final int width;
    private final int height;
    private final int xOffset;
    private final int yOffset;
    private final int advance;
    private final BitSet pixels;

    /**
     * @param pixels the lit pixels, row after row from the top, {@code width} to a row: pixel ({@code x},
     *               {@code y}) is bit {@code y * width + x}
     */
    public BitmapGlyph(final int codePoint, final int width, final int height, final int xOffset, final int yOffset,
                       final int advance, final BitSet pixels) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("a glyph cannot be " + width + " by " + height);
        }
        this.codePoint = codePoint;
        this.width = width;
        this.height = height;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.advance = advance;
        this.pixels = (BitSet) pixels.clone();
    }

    public int codePoint() {
        return codePoint;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int xOffset() {
        return xOffset;
    }

    public int yOffset() {
        return yOffset;
    }

    /** How far the pen moves to the right after this character. */
    public int advance() {
        return advance;
    }

    /** Whether the pixel that far right and down from the top left corner of the glyph's box is lit. */
    public boolean lit(final int x, final int y) {
        return x >= 0 && x < width && y >= 0 && y < height && pixels.get(y * width + x);
    }

    /** Whether nothing of it is drawn: a space of some width. */
    public boolean isBlank() {
        return pixels.isEmpty();
    }
}
