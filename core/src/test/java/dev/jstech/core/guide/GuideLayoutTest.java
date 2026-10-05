/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuideLayoutTest {

    private static final GuideStyle STYLE = new GuideStyle("", Map.of(), true, 166, 201, 10, true, "", "",
            GuideStyle.Folios.CHAPTER_PAGE);
    private static final GuideManual MANUAL = new GuideManual("test:manual", "manual.title", List.of(), "", "",
            "test:style", List.of(GuideManual.EVERY_CHAPTER), List.of("about.one"), 0);

    @Test
    void lay_numbersChaptersSectionsAndEntries() {
        final GuideBook book = lay(contents());

        assertEquals("1.1", book.numbers().get("alpha:parts"));
        assertEquals("1.1.2", book.numbers().get("alpha:second"));
        assertEquals("2.1.1", book.numbers().get("beta:only"));
    }

    @Test
    void lay_startsEveryEntryOnAPageOfItsOwn() {
        final GuideBook book = lay(contents());

        final int first = book.pageOf("alpha:first").orElseThrow();
        final int second = book.pageOf("alpha:second").orElseThrow();
        assertNotEquals(first, second);
        final GuidePiece opening = book.pages().get(second).pieces().getFirst();
        assertTrue(opening instanceof GuidePiece.Text text && text.text().startsWith("1.1.2"),
                () -> "the entry's page opens with " + opening);
    }

    @Test
    void lay_runsALongParagraphOnToTheNextPage() {
        final String words = "word ".repeat(600);
        final GuideBook book = lay(new GuideContents(List.of(chapter("alpha", entry("alpha:long",
                new GuideBlock.Paragraph(words))))), new FixedText(Map.of("long.words", words)));

        final int start = book.pageOf("alpha:long").orElseThrow();
        assertTrue(book.pages().get(start + 1).chapter().equals("alpha"), "the paragraph runs on to a second page");
    }

    @Test
    void lay_placesNothingBelowTheFootOfAPage() {
        final String words = "word ".repeat(600);
        final GuideBook book = lay(new GuideContents(List.of(chapter("alpha", entry("alpha:long",
                new GuideBlock.Paragraph(words), table(30), new GuideBlock.Steps(List.of(words, words)))))),
                new FixedText(Map.of()));

        final int bottom = STYLE.pageHeight() - GuideLayout.FOOTER_ROOM;
        for (final GuideBook.Page page : book.pages()) {
            for (final GuidePiece piece : page.pieces()) {
                assertTrue(piece.y() <= bottom, () -> page.folio() + " has " + piece + " below " + bottom);
            }
        }
    }

    @Test
    void lay_numbersPagesWithinTheirChapter() {
        final GuideBook book = lay(contents());

        assertEquals("1-1", book.pages().get(book.pageOf("alpha").orElseThrow()).folio());
        assertEquals("2-1", book.pages().get(book.pageOf("beta").orElseThrow()).folio());
    }

    @Test
    void lay_numbersTheFrontMatterInRomanAndTheIndexAfterIt() {
        final GuideBook book = lay(contents());

        assertEquals("i", book.pages().getFirst().folio());
        assertEquals("ii", book.pages().get(1).folio());
        assertEquals("X-1", book.pages().get(book.indexAt()).folio());
        assertEquals(book.pages().size() - 1, book.indexAt());
    }

    @Test
    void lay_listsEntriesAndExplainedWordsInTheIndexAlphabetically() {
        final GuideBook book = lay(contents());

        final List<String> lines = book.index().stream().map(GuideBook.IndexLine::text).toList();
        final List<String> sorted = new ArrayList<>(lines);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        assertEquals(sorted, lines);
        assertTrue(book.index().stream().anyMatch(line -> line.term() && line.text().equals("term.fe")));
        assertTrue(book.index().stream().anyMatch(line -> !line.term() && line.target().equals("beta:only")));
    }

    @Test
    void lay_numbersFiguresAndTablesWithinTheirChapter() {
        final GuideBook book = lay(contents());

        final List<String> labels = new ArrayList<>();
        for (final GuideBook.Page page : book.pages()) {
            for (final GuidePiece piece : page.pieces()) {
                if (piece instanceof GuidePiece.Text text && text.size() == TextSize.SMALL_BOLD) {
                    labels.add(text.text());
                }
            }
        }
        assertEquals(List.of("jscore.guide.figure[1-1]", "jscore.guide.figure[1-2]", "jscore.guide.table[1-1]",
                "jscore.guide.figure[2-1]"), labels);
    }

    @Test
    void lay_writesTheNumberOfEachEntryALinkLeadsTo() {
        final GuideBook book = lay(contents());

        final GuideBook.Page page = book.pages().get(book.pageOf("beta:only").orElseThrow());
        assertTrue(page.pieces().stream().anyMatch(piece -> piece instanceof GuidePiece.Text text
                && text.text().equals("1.1.2") && text.link().equals("alpha:second")), () -> "" + page.pieces());
    }

    @Test
    void lay_leadsTheContentsToEveryChapterAndSection() {
        final GuideBook book = lay(contents());

        final List<String> links = book.pages().get(book.contents()).pieces().stream()
                .filter(piece -> piece instanceof GuidePiece.Leader)
                .map(piece -> ((GuidePiece.Leader) piece).link()).toList();
        assertEquals(List.of("alpha", "alpha:parts", "beta", "beta:things", ""), links);
    }

    @Test
    void lay_leavesAKindWithNoRecipesASentenceSayingSo() {
        final GuideBook book = lay(new GuideContents(List.of(chapter("alpha", entry("alpha:machine",
                new GuideBlock.Recipes("none:kind", ""))))), new FixedText(Map.of()));

        final GuideBook.Page page = book.pages().get(book.pageOf("alpha:machine").orElseThrow());
        assertTrue(page.pieces().stream().anyMatch(piece -> piece instanceof GuidePiece.Text text
                && text.text().equals(GuideTexts.NO_RECIPES.key())));
        assertFalse(page.pieces().stream().anyMatch(piece -> piece instanceof GuidePiece.Recipes));
    }

    @Test
    void roman_writesSmallRomanNumerals() {
        assertEquals("i", GuideLayout.roman(1));
        assertEquals("iv", GuideLayout.roman(4));
        assertEquals("xiv", GuideLayout.roman(14));
    }

    @Test
    void wrap_breaksBetweenWordsAndBreaksAWordTooLongForTheLine() {
        final FixedText text = new FixedText(Map.of());

        assertEquals(List.of("one two", "three"), GuideLayout.wrap("one two three", 42, TextSize.BODY, text));
        assertEquals(List.of("abcde", "fghij"), GuideLayout.wrap("abcdefghij", 30, TextSize.BODY, text));
    }

    private static GuideBook lay(final GuideContents contents) {
        return lay(contents, new FixedText(Map.of()));
    }

    private static GuideBook lay(final GuideContents contents, final FixedText text) {
        return GuideLayout.lay(MANUAL, STYLE, contents, text);
    }

    private static GuideContents contents() {
        final GuideEntry first = entry("alpha:first", new GuideBlock.Paragraph("first.text"),
                new GuideBlock.Figure("minecraft:stone", "first.figure"), new GuideBlock.Define("term.fe", "term.def"));
        final GuideEntry second = entry("alpha:second", new GuideBlock.Figure("minecraft:dirt", "second.figure"),
                table(2));
        final GuideEntry only = new GuideEntry("beta:only", "beta:things", 0, "only.title", "minecraft:sand",
                List.of(), List.of(new GuideBlock.Figure("minecraft:sand", "only.figure"),
                new GuideBlock.SeeAlso(List.of("alpha:second"))));
        return new GuideContents(List.of(
                new GuideContents.Chapter(new GuideChapter("alpha", 0, "alpha.title", ""), List.of(
                        new GuideContents.Section(new GuideSection("alpha:parts", 0, "parts.title", ""),
                                List.of(first, second)))),
                new GuideContents.Chapter(new GuideChapter("beta", 1, "beta.title", ""), List.of(
                        new GuideContents.Section(new GuideSection("beta:things", 0, "things.title", ""),
                                List.of(only))))));
    }

    private static GuideContents.Chapter chapter(final String namespace, final GuideEntry entry) {
        return new GuideContents.Chapter(new GuideChapter(namespace, 0, namespace + ".title", ""), List.of(
                new GuideContents.Section(new GuideSection(namespace + ":section", 0, "section.title", ""),
                        List.of(entry))));
    }

    private static GuideEntry entry(final String id, final GuideBlock... blocks) {
        return new GuideEntry(id, GuideIds.namespace(id) + ":section", 0, id + ".title", "", List.of(),
                List.of(blocks));
    }

    private static GuideBlock.Table table(final int rows) {
        final List<GuideBlock.TableRow> list = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            list.add(new GuideBlock.TableRow("row" + i, new GuideBlock.GuideValue.Amount(1000L * i, "FE")));
        }
        return new GuideBlock.Table("table.caption", list);
    }

    /**
     * Words that are their own keys, with any arguments after them in brackets, six pixels to a character at every
     * size, and no recipes but those of one kind.
     */
    private record FixedText(Map<String, String> words) implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            final String found = this.words.getOrDefault(key, key);
            if (args.length == 0) {
                return found;
            }
            final List<String> written = new ArrayList<>();
            for (final Object arg : args) {
                written.add(String.valueOf(arg));
            }
            return found + "[" + String.join(",", written) + "]";
        }

        @Override
        public int width(final String text, final TextSize size) {
            return text.length() * 6;
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return "some:kind".equals(type) ? 3 : 0;
        }

        @Override
        public String amount(final long value, final String unit) {
            return value + " " + unit;
        }
    }
}
