/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ManualReaderTest {

    private static final GuideManual MANUAL = new GuideManual("test:manual", "manual.title", List.of(), "", "",
            "test:style", List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, "");

    @Test
    void tree_numbersAsThePrintedManualDoes() {
        final ManualReader reader = reader();

        assertEquals(List.of("1", "1.1", "1.1.1", "1.1.2", "2", "2.1", "2.1.1"),
                reader.tree().stream().map(ManualReader.Node::number).toList());
        assertEquals(List.of(0, 1, 2, 2, 0, 1, 2), reader.tree().stream().map(ManualReader.Node::depth).toList());
        assertEquals(Optional.of("alpha:parts"), reader.parent("alpha:second"));
        assertEquals(Optional.of("alpha"), reader.parent("alpha:parts"));
        assertEquals(List.of("alpha:first", "alpha:second"),
                reader.children("alpha:parts").stream().map(ManualReader.Node::id).toList());
    }

    @Test
    void article_turnsEveryBlockIntoText() {
        final ManualReader.Article first = reader().article("alpha:first").orElseThrow();

        assertEquals("1.1.1", first.number());
        assertEquals("alpha:parts", first.section());
        assertEquals(List.of(
                new ManualReader.Piece.Heading("jscore.guide.what_it_is", false),
                new ManualReader.Piece.Paragraph("first.text", ManualReader.Tone.PLAIN),
                new ManualReader.Piece.Picture("minecraft:stone", "jscore.guide.figure[1-1] first.figure"),
                new ManualReader.Piece.Item("1.", "step.one"),
                new ManualReader.Piece.Item("2.", "step.two"),
                new ManualReader.Piece.Heading("jscore.guide.what_can_go_wrong", true),
                new ManualReader.Piece.Term("problem", "fix"),
                new ManualReader.Piece.Paragraph("jscore.guide.warning_line[careful]", ManualReader.Tone.WARNING),
                new ManualReader.Piece.Term("term.fe", "term.def")), first.pieces());
    }

    @Test
    void article_numbersFiguresAndTablesByChapter() {
        final ManualReader reader = reader();

        final ManualReader.Article second = reader.article("alpha:second").orElseThrow();
        final ManualReader.Article only = reader.article("beta:only").orElseThrow();

        assertEquals(new ManualReader.Piece.Picture("minecraft:dirt", "jscore.guide.figure[1-2] second.figure"),
                second.pieces().get(0));
        assertEquals(new ManualReader.Piece.Table("jscore.guide.table[1-1] table.caption", List.of(
                new ManualReader.Row("row0", "0 FE"), new ManualReader.Row("row1", "1000 FE"))),
                second.pieces().get(1));
        assertEquals(new ManualReader.Piece.Picture("minecraft:sand", "jscore.guide.figure[2-1] only.figure"),
                only.pieces().get(0));
    }

    @Test
    void article_namesALinkByTheNumberAndTitleOfWhereItGoes() {
        final ManualReader.Article only = reader().article("beta:only").orElseThrow();

        assertEquals(new ManualReader.Piece.Links("jscore.guide.see_also", List.of(
                new ManualReader.Link("alpha:second", "1.1.2 alpha:second.title"),
                new ManualReader.Link("alpha:parts", "1.1 parts.title"))), only.pieces().get(1));
    }

    @Test
    void article_readsAPlanAndABlockSeenFromThreeSides() {
        final ManualReader.Article plant = new ManualReader(MANUAL, new GuideContents(List.of(chapter("gamma",
                entry("gamma:plant", new GuideBlock.Views("minecraft:furnace", List.of(new GuideBlock.Callout(1,
                                GuideBlock.View.FRONT, 8, 8, "door"))),
                        new GuideBlock.Break(GuideBlock.BreakKind.PAGE),
                        new GuideBlock.Plan("plan.caption", List.of(new GuideBlock.PlanPart("minecraft:furnace",
                                "furnace", false), new GuideBlock.PlanPart("", "spare", true))))))),
                new FixedText()).article("gamma:plant").orElseThrow();

        assertEquals(List.of(new ManualReader.Piece.Picture("minecraft:furnace", ""),
                new ManualReader.Piece.Item("1", "door"),
                new ManualReader.Piece.Paragraph("plan.caption", ManualReader.Tone.PLAIN),
                new ManualReader.Piece.Item("-", "furnace"),
                new ManualReader.Piece.Item("-", "jscore.guide.optional_part[spare]")), plant.pieces());
    }

    @Test
    void nextAndPrevious_walkTheEntriesAcrossChapters() {
        final ManualReader reader = reader();

        assertEquals(Optional.of("beta:only"), reader.next("alpha:second"));
        assertEquals(Optional.of("alpha:second"), reader.previous("beta:only"));
        assertEquals(Optional.empty(), reader.previous("alpha:first"));
        assertEquals(Optional.empty(), reader.next("beta:only"));
    }

    @Test
    void find_takesAnIdANumberOrATitleHoweverItsGapsAreWritten() {
        final ManualReader reader = reader();

        assertEquals(Optional.of("alpha:second"), reader.find("alpha:second"));
        assertEquals(Optional.of("alpha:second"), reader.find("SECOND"));
        assertEquals(Optional.of("beta:only"), reader.find("2.1.1"));
        assertEquals(Optional.of("beta:only"), reader.find("ONLY.TITLE"));
        assertEquals(Optional.of("alpha:parts"), reader.find("parts"));
        assertEquals(Optional.empty(), reader.find("nothing"));
        assertEquals(Optional.empty(), reader.find("  "));

        final ManualReader cards = new ManualReader(MANUAL, new GuideContents(List.of(chapter("gamma",
                entry("gamma:graphics_cards", new GuideBlock.Paragraph("text"))))), new FixedText());

        assertEquals(Optional.of("gamma:graphics_cards"), cards.find("graphics-cards"));
        assertEquals(Optional.of("gamma:graphics_cards"), cards.find("Graphics Cards"));
    }

    @Test
    void search_findsEveryWordInTheTitleOrTheText() {
        final ManualReader reader = reader();

        assertEquals(List.of("alpha:first"), reader.search("STEP two"));
        assertEquals(List.of("alpha:second"), reader.search("row1"));
        assertEquals(List.of(), reader.search("step nothing"));
        assertEquals(List.of(), reader.search(""));
    }

    @Test
    void index_listsTitlesAndExplainedWordsAlphabetically() {
        final List<ManualReader.IndexLine> index = reader().index();

        assertEquals(List.of("alpha:first.title", "alpha:second.title", "only.title", "term.fe"),
                index.stream().map(ManualReader.IndexLine::text).toList());
        assertTrue(index.get(3).term());
        assertEquals("alpha:first", index.get(3).target());
    }

    private static ManualReader reader() {
        final GuideEntry first = entry("alpha:first", new GuideBlock.Heading("jscore.guide.what_it_is"),
                new GuideBlock.Paragraph("first.text"), new GuideBlock.Figure("minecraft:stone", "first.figure"),
                new GuideBlock.Steps(List.of("step.one", "step.two")),
                new GuideBlock.Heading("jscore.guide.what_can_go_wrong"),
                new GuideBlock.Problems(List.of(new GuideBlock.Problem("problem", "fix"))),
                new GuideBlock.Warning("careful"), new GuideBlock.Define("term.fe", "term.def"));
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            rows.add(new GuideBlock.TableRow("row" + i, new GuideBlock.GuideValue.Amount(1000L * i, "FE")));
        }
        final GuideEntry second = entry("alpha:second", new GuideBlock.Figure("minecraft:dirt", "second.figure"),
                new GuideBlock.Table("table.caption", rows));
        final GuideEntry only = new GuideEntry("beta:only", "beta:things", 0, "only.title", "minecraft:sand",
                List.of(), List.of(new GuideBlock.Figure("minecraft:sand", "only.figure"),
                new GuideBlock.SeeAlso(List.of("alpha:second", "alpha:parts"))));
        return new ManualReader(MANUAL, new GuideContents(List.of(
                new GuideContents.Chapter(new GuideChapter("alpha", 0, "alpha.title", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("alpha:parts", 0, "parts.title", ""),
                                List.of(first, second)))),
                new GuideContents.Chapter(new GuideChapter("beta", 1, "beta.title", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("beta:things", 0, "things.title", ""),
                                List.of(only)))))), new FixedText());
    }

    private static GuideContents.Chapter chapter(final String namespace, final GuideEntry entry) {
        return new GuideContents.Chapter(new GuideChapter(namespace, 0, namespace + ".title", "", ""), List.of(
                new GuideContents.Section(new GuideSection(namespace + ":section", 0, "section.title", ""),
                        List.of(entry))));
    }

    private static GuideEntry entry(final String id, final GuideBlock... blocks) {
        return new GuideEntry(id, GuideIds.namespace(id) + ":parts", 0, id + ".title", "", List.of(),
                List.of(blocks));
    }

    /** Words that are their own keys, with any arguments after them in brackets. */
    private record FixedText() implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            if (args.length == 0) {
                return key;
            }
            final List<String> written = new ArrayList<>();
            for (final Object arg : args) {
                written.add(String.valueOf(arg));
            }
            return key + "[" + String.join(",", written) + "]";
        }

        @Override
        public int width(final String text, final TextSize size) {
            return text.length() * 6;
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return 0;
        }

        @Override
        public String amount(final long value, final String unit) {
            return value + " " + unit;
        }
    }
}
