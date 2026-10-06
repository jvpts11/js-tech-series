/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

/**
 * What the layout of a manual asks of whoever shows it: the words of a key in the reader's language, how wide a
 * piece of text is drawn, and what only the running game knows.
 *
 * <p>The screen answers from the game's fonts and the loaded language; a test answers with fixed widths, which is how
 * the layout is checked with no game at all.
 */
public interface IGuideText {

    /** The sentence of a key in the reader's language, its arguments put in; the key itself when it has none. */
    String text(String key, Object... args);

    /** How many pixels wide that text is drawn at that size. */
    int width(String text, TextSize size);

    /** How many recipes of that type, making that item when one is named, the game holds. */
    int recipeCount(String type, String output);

    /** An amount written the reader's way, with its unit after it: "12,000 FE". */
    String amount(long value, String unit);

    /**
     * The name of an item, {@code namespace:path}, in the reader's language. Whoever knows no better names it by its
     * path, as a test does.
     */
    default String itemName(final String item) {
        return GuideIds.path(item);
    }
}
