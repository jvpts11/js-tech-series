/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * How big a text is, wrapped at its words to the room it gets: the measure of a label in a layout. The widths come
 * from whoever knows the font (the game's on the client, a fixed width per letter in a unit test), so the layout
 * never reads the font itself.
 *
 * @param width      how wide a run of text is drawn, in pixels at the font's own size
 * @param lineHeight how tall a line is, at the font's own size
 * @param scale      the size the text is drawn at, 1 being the font's own
 * @param wraps      whether the text breaks into lines at its words when it is too wide, or stays one line
 */
public record TextMeasure(ToIntFunction<String> width, int lineHeight, float scale, boolean wraps) {

    /** The gap the game leaves under each line of text, part of {@link #lineHeight} but not of the last line drawn. */
    private static final int LINE_GAP = 1;

    public TextMeasure {
        Objects.requireNonNull(width, "width");
        if (scale <= 0) {
            throw new IllegalArgumentException("text cannot be drawn at a size of " + scale);
        }
    }

    /** The measure of {@code text}, for a leaf of a layout. */
    public IMeasure of(final String text) {
        return maxWidth -> {
            final List<String> lines = lines(text, maxWidth);
            int widest = 0;
            for (final String line : lines) {
                widest = Math.max(widest, scaled(width.applyAsInt(line)));
            }
            final int tall = lines.isEmpty() ? 0 : scaled(lines.size() * lineHeight - LINE_GAP);
            return new Size(widest, tall);
        };
    }

    /**
     * The lines {@code text} is drawn in with {@code maxWidth} pixels across: one when it fits or does not wrap;
     * otherwise broken at the spaces between words, a word wider than the room on a line of its own.
     */
    public List<String> lines(final String text, final int maxWidth) {
        final List<String> out = new ArrayList<>();
        for (final String paragraph : text.split("\n", -1)) {
            if (!wraps || scaled(width.applyAsInt(paragraph)) <= maxWidth) {
                out.add(paragraph);
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (final String word : paragraph.split(" ")) {
                final String tried = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && scaled(width.applyAsInt(tried)) > maxWidth) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(tried);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    private int scaled(final int pixels) {
        return (int) Math.ceil(pixels * scale);
    }
}
