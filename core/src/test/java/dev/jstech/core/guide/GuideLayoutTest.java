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

    private static final GuideStyle STYLE = new GuideStyle("", Map.of(), new GuideStyle.Pages(true, 166, 201, 10, 1),
            GuideStyle.Decor.binder(true), "", "", GuideStyle.Folios.CHAPTER_PAGE, "",
            new GuideStyle.Cover(GuideStyle.CoverKind.BINDER, true, false), GuideStyle.HoldBar.PLAIN);
    private static final GuideStyle DRAWINGS = new GuideStyle("", Map.of(),
            new GuideStyle.Pages(false, 340, 201, 15, 2), GuideStyle.Decor.drawing(), "", "",
            GuideStyle.Folios.DRAWING, "JI", new GuideStyle.Cover(GuideStyle.CoverKind.FOLDER, true, true),
            GuideStyle.HoldBar.HAZARD);
    private static final GuideManual MANUAL = new GuideManual("test:manual", "manual.title", List.of(), "", "",
            "test:style", List.of(GuideManual.EVERY_CHAPTER), List.of("about.one"), 0, "");

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
    void lay_opensEveryChapterOnALeftPageLeavingAPageBlankBeforeIt() {
        final GuideBook book = lay(new GuideContents(List.of(chapter("alpha", entry("alpha:one",
                new GuideBlock.Paragraph("one.text"))), chapter("beta", entry("beta:two",
                new GuideBlock.Paragraph("two.text"))))));

        final int alpha = book.pageOf("alpha").orElseThrow();
        final int beta = book.pageOf("beta").orElseThrow();
        assertEquals(0, alpha % 2, "the first chapter opens on a left page");
        assertEquals(0, beta % 2, "the second chapter opens on a left page");
        assertTrue(book.pages().get(beta - 1).pieces().stream().anyMatch(piece -> piece instanceof GuidePiece.Text
                text && text.text().equals(GuideTexts.BLANK.key())), "the page before it says it is left blank");
    }

    @Test
    void lay_listsAChaptersSectionsAndTheirPagesOnItsOpeningSpread() {
        final GuideBook book = lay(contents());

        final GuideBook.Page facing = book.pages().get(book.pageOf("alpha").orElseThrow() + 1);
        final GuideBook.Page first = book.pages().get(book.pageOf("alpha:first").orElseThrow());
        assertEquals(GuideTexts.IN_THIS_CHAPTER.key(), facing.header());
        assertTrue(facing.pieces().stream().anyMatch(piece -> piece instanceof GuidePiece.Leader leader
                && leader.lead().equals("1.1") && leader.link().equals("alpha:parts")
                && leader.right().equals(first.folio())), () -> "" + facing.pieces());
    }

    @Test
    void lay_numbersDrawingsBySectionInHundreds() {
        final GuideBook book = drawings();

        assertEquals("JI-001", book.numbers().get("alpha:intro"));
        assertEquals("JI-101", book.numbers().get("alpha:press"));
        assertEquals("JI-102", book.numbers().get("alpha:mill"));
        assertEquals("JI-101", book.pages().get(book.pageOf("alpha:press").orElseThrow()).folio());
    }

    @Test
    void lay_opensASetOfDrawingsWithItsDrawingListAndNoIndex() {
        final GuideBook book = drawings();

        final GuideBook.Page list = book.pages().getFirst();
        assertEquals("JI-000", list.folio());
        assertEquals(GuideTexts.DRAWING_LIST.key(), list.title());
        for (final String target : List.of("alpha:intro", "alpha:press", "alpha:mill")) {
            assertTrue(list.pieces().stream().anyMatch(piece -> piece instanceof GuidePiece.Text text
                    && text.link().equals(target)), () -> "the list leads to " + target);
        }
        assertEquals(book.pages().size(), book.indexAt(), "a set of drawings has no index pages");
        assertFalse(book.index().isEmpty(), "but the search still has its index");
    }

    @Test
    void lay_countsTheSheetsOfEachDrawing() {
        final GuideBook book = drawings();

        final int press = book.pageOf("alpha:press").orElseThrow();
        assertEquals(1, book.pages().get(press).sheet());
        assertEquals(2, book.pages().get(press).sheets());
        assertEquals(2, book.pages().get(press + 1).sheet());
        assertEquals("JI-101", book.pages().get(press + 1).folio());
        assertEquals(1, book.pages().get(book.pageOf("alpha:mill").orElseThrow()).sheets());
    }

    @Test
    void lay_runsTextOnInTheSecondColumnAndKeepsItAboveTheTitleBlock() {
        final String words = "word ".repeat(300);
        final GuideBook book = GuideLayout.lay(MANUAL, DRAWINGS, new GuideContents(List.of(chapter("alpha",
                entry("alpha:long", new GuideBlock.Paragraph(words))))), new FixedText(Map.of()));

        final GuideBook.Page sheet = book.pages().get(book.pageOf("alpha:long").orElseThrow());
        final int second = GuideLayout.columnX(DRAWINGS, 1);
        assertTrue(sheet.pieces().stream().anyMatch(piece -> piece.x() == second), "the text runs on in column 2");
        for (final GuidePiece piece : sheet.pieces()) {
            final int bottom = GuideLayout.bottom(DRAWINGS, piece.x() >= second ? 1 : 0);
            assertTrue(piece.y() <= bottom, () -> piece + " stands below " + bottom);
        }
        assertTrue(GuideLayout.bottom(DRAWINGS, 1) < GuideLayout.bottom(DRAWINGS, 0),
                "the column over the title block ends above it");
    }

    @Test
    void lay_writesADrawingsHeadingsInCapitalsAndItsStepsWithPlainNumbers() {
        final GuideBook book = GuideLayout.lay(MANUAL, DRAWINGS, new GuideContents(List.of(chapter("alpha",
                entry("alpha:one", new GuideBlock.Heading("what"), new GuideBlock.Steps(List.of("a", "b")))))),
                new FixedText(Map.of()));

        final List<String> words = book.pages().get(book.pageOf("alpha:one").orElseThrow()).pieces().stream()
                .filter(piece -> piece instanceof GuidePiece.Text).map(piece -> ((GuidePiece.Text) piece).text())
                .toList();
        assertEquals(List.of("WHAT", "1", "a", "2", "b"), words);
    }

    @Test
    void lay_putsThePlateOfAnEntrysItemsUnderItsTitle() {
        final GuideEntry pressed = new GuideEntry("alpha:press", "alpha:section", 0, "press.title", "",
                List.of("alpha:press_block"), List.of("alpha:gear", "alpha:plate"),
                List.of(new GuideBlock.Paragraph("press.text")));
        final GuideBook book = GuideLayout.lay(MANUAL, STYLE, new GuideContents(List.of(chapter("alpha", pressed))),
                new FixedText(Map.of()));

        final List<GuidePiece> pieces = book.pages().get(book.pageOf("alpha:press").orElseThrow()).pieces();
        final GuidePiece.Plate plate = pieces.stream().filter(piece -> piece instanceof GuidePiece.Plate)
                .map(piece -> (GuidePiece.Plate) piece).findFirst().orElseThrow();
        assertEquals(List.of("alpha:gear", "alpha:plate"), plate.items(), "the plate shows what the entry names");
        assertEquals("alpha:press", plate.entry());
        final GuidePiece.Text words = pieces.stream().filter(piece -> piece instanceof GuidePiece.Text text
                && text.text().equals("press.text")).map(piece -> (GuidePiece.Text) piece).findFirst().orElseThrow();
        assertTrue(words.y() >= plate.y() + GuideLayout.PLATE_HEIGHT, "the text starts under the plate");
    }

    @Test
    void lay_showsNoPlateOnADrawingNorForAnEntryWithNoItems() {
        final GuideEntry pressed = new GuideEntry("alpha:press", "alpha:section", 0, "press.title", "",
                List.of("alpha:press_block"), List.of(new GuideBlock.Paragraph("press.text")));
        final GuideContents contents = new GuideContents(List.of(chapter("alpha", pressed)));

        assertTrue(GuideLayout.lay(MANUAL, DRAWINGS, contents, new FixedText(Map.of())).pages().stream()
                .flatMap(page -> page.pieces().stream()).noneMatch(piece -> piece instanceof GuidePiece.Plate),
                "a drawing shows its machine in its views instead");
        assertTrue(lay(contents()).pages().stream().flatMap(page -> page.pieces().stream())
                .noneMatch(piece -> piece instanceof GuidePiece.Plate), "an entry with no items has no plate");
    }

    @Test
    void lay_countsAPictureWithTheFigures() {
        final GuideBook book = GuideLayout.lay(MANUAL, STYLE, new GuideContents(List.of(chapter("alpha", entry(
                "alpha:one", new GuideBlock.Figure("minecraft:stone", "one.figure"),
                new GuideBlock.Picture("", "alpha:screen", "{}", 0, 40, "one.picture"))))),
                new FixedText(Map.of()));

        final List<GuidePiece> pieces = book.pages().stream().flatMap(page -> page.pieces().stream()).toList();
        final GuidePiece.Picture picture = pieces.stream().filter(piece -> piece instanceof GuidePiece.Picture)
                .map(piece -> (GuidePiece.Picture) piece).findFirst().orElseThrow();
        assertEquals("alpha:screen", picture.drawing());
        assertEquals(40, picture.height());
        assertTrue(pieces.stream().anyMatch(piece -> piece instanceof GuidePiece.Text text
                && text.text().equals("jscore.guide.figure[1-2]")), "the picture is the chapter's second figure");
    }

    @Test
    void lay_drawsALinkInASentenceInTheLinksColourWithItsNumber() {
        final FixedText text = new FixedText(Map.of("first.text", "read [the second](alpha:second), then go"));
        final GuideBook book = lay(contents(), text);

        final List<GuidePiece.Text> words = book.pages().get(book.pageOf("alpha:first").orElseThrow()).pieces()
                .stream().filter(piece -> piece instanceof GuidePiece.Text).map(piece -> (GuidePiece.Text) piece)
                .toList();
        final GuidePiece.Text link = words.stream().filter(piece -> piece.link().equals("alpha:second")).findFirst()
                .orElseThrow();
        assertEquals("the second (1.1.2)", link.text(), "the link reads its words and the number it leads to");
        assertEquals(GuideStyle.LINK, link.colour());
        final GuidePiece.Text before = words.stream().filter(piece -> piece.text().equals("read ")).findFirst()
                .orElseThrow();
        assertEquals(before.x() + 6 * "read ".length(), link.x(), "the link goes on where the words before it end");
    }

    @Test
    void lay_keepsALinksNumberAndTheCommaAfterItOnOneLine() {
        final FixedText text = new FixedText(Map.of("first.text", "aaaaaaaaaaaaaaaaaa [](alpha:second), b"));
        final GuideBook book = lay(contents(), text);

        final GuidePiece.Text link = book.pages().get(book.pageOf("alpha:first").orElseThrow()).pieces().stream()
                .filter(piece -> piece instanceof GuidePiece.Text candidate && candidate.link().equals("alpha:second"))
                .map(piece -> (GuidePiece.Text) piece).findFirst().orElseThrow();
        assertEquals("1.1.2", link.text(), "a link with no words reads its number");
        assertTrue(book.pages().get(book.pageOf("alpha:first").orElseThrow()).pieces().stream()
                .anyMatch(piece -> piece instanceof GuidePiece.Text plain && plain.text().startsWith(",")
                        && plain.y() == link.y()), "the comma stays on the link's line");
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
                new GuideContents.Chapter(new GuideChapter("alpha", 0, "alpha.title", "", "alpha.about"), List.of(
                        new GuideContents.Section(new GuideSection("alpha:parts", 0, "parts.title", ""),
                                List.of(first, second)))),
                new GuideContents.Chapter(new GuideChapter("beta", 1, "beta.title", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("beta:things", 0, "things.title", ""),
                                List.of(only))))));
    }

    /** A set of drawings: a first section of one drawing, a second of two, the first of them two sheets long. */
    private static GuideBook drawings() {
        final GuideEntry intro = new GuideEntry("alpha:intro", "alpha:reading", 0, "intro.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("intro.text")));
        final GuideEntry press = new GuideEntry("alpha:press", "alpha:machines", 0, "press.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("press.text"), new GuideBlock.Break(GuideBlock.BreakKind.PAGE),
                        new GuideBlock.Paragraph("press.more")));
        final GuideEntry mill = new GuideEntry("alpha:mill", "alpha:machines", 1, "mill.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("mill.text")));
        return GuideLayout.lay(MANUAL, DRAWINGS, new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("alpha", 0, "alpha.title", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("alpha:reading", 0, "reading.title", ""),
                                List.of(intro)),
                        new GuideContents.Section(new GuideSection("alpha:machines", 1, "machines.title", ""),
                                List.of(press, mill)))))), new FixedText(Map.of()));
    }

    private static GuideContents.Chapter chapter(final String namespace, final GuideEntry entry) {
        return new GuideContents.Chapter(new GuideChapter(namespace, 0, namespace + ".title", "", ""), List.of(
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
