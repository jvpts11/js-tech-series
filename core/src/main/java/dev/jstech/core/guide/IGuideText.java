/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Optional;

/**
 * What the layout of a manual asks of whoever shows it: the words of a key in the reader's language, how wide a
 * piece of text is drawn, and what only the running game knows.
 *
 * <p>The screen answers from the game's fonts and the loaded language; a test answers with fixed widths, which is how
 * the layout is checked with no game at all.
 */
public interface IGuideText {

    /**
     * The sentence of a key in the reader's language, its arguments put in; with none given, the sentence as its
     * language writes it; the key itself when it has none. Whoever shows a manual to a player also writes in the
     * values the sentence names, with {@link GuideFacts#fill}.
     */
    String text(String key, Object... args);

    /** How many pixels wide that text is drawn at that size. */
    int width(String text, TextSize size);

    /** How many recipes of that type, making that item when one is named, the game holds. */
    int recipeCount(String type, String output);

    /** An amount written the reader's way, with its unit after it: "12,000 FE". */
    String amount(long value, String unit);

    /** A table value as the reader sees it: its words translated, its amount written with the unit, or its literal. */
    default String valueText(final GuideBlock.GuideValue value) {
        return switch (value) {
            case GuideBlock.GuideValue.Words words -> text(words.key());
            case GuideBlock.GuideValue.Amount amount -> amount(amount.value(), amount.unit());
            case GuideBlock.GuideValue.Literal literal -> literal.text();
            case GuideBlock.GuideValue.Said said -> said(said.text());
        };
    }

    /**
     * Words a mod says, in the reader's language: each sentence in it as {@link #text} has it with nothing put in (its
     * {@code %s} still in it), and in the English it was declared with when the reader's language has none.
     */
    default String said(final Text words) {
        return words.resolve(key -> {
            final String sentence = text(key);
            return sentence.equals(key) ? null : sentence;
        });
    }

    /** The page of a command, as its mod answers it now ({@link GuideFacts#commandOf}). */
    default Optional<GuideCommand> command(final String source) {
        return GuideFacts.commandOf(source);
    }

    /**
     * The colours of a listing's lines in a language, as runs of each line. Whoever knows no language colours every
     * line plain, as a test does; the game colours them as {@link GuideHighlighters} says.
     */
    default List<List<GuideCode.Run>> highlight(final String language, final List<String> lines) {
        return GuideCode.plain(lines);
    }

    /** The rows of a table read from its source as the manual is laid out, each with {@code columns} cells. */
    default List<GuideBlock.TableRow> rows(final String source, final int columns) {
        return GuideFacts.rowsOf(source, columns);
    }

    /**
     * The name of an item, {@code namespace:path}, in the reader's language. Whoever knows no better names it by its
     * path, as a test does.
     */
    default String itemName(final String item) {
        return GuideIds.path(item);
    }

    /**
     * The title, in the reader's language, of another book holding a link's target (an entry, a section or a chapter)
     * that the manual being read does not hold; empty when no book does. Whoever knows only one manual, as a test
     * does, knows of none.
     */
    default String elsewhere(final String target) {
        return "";
    }

    /**
     * The title, in the reader's language, of a link's target another book holds, written where a link has no words
     * of its own and so no number to show; empty when it is not known.
     */
    default String titleOf(final String target) {
        return "";
    }

    /**
     * The blocks of the multiblock of {@code id} as it stands now, as a scene holds them, its least corner at the
     * grid's: none where it is not installed or is too big for a scene. Whoever knows no multiblocks, as a test does,
     * knows none.
     */
    default List<GuideBlock.SceneBlock> multiblock(final String id) {
        return List.of();
    }
}
