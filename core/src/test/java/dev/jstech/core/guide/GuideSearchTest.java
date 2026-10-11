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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuideSearchTest {

    private static final GuideStyle STYLE = new GuideStyle("", Map.of(), new GuideStyle.Pages(true, 166, 201, 10, 1),
            GuideStyle.Decor.binder(true), "", "", GuideStyle.Folios.CHAPTER_PAGE, "",
            new GuideStyle.Cover(GuideStyle.CoverKind.BINDER, true, false), GuideStyle.HoldBar.PLAIN);
    private static final GuideManual MANUAL = new GuideManual("test:manual", "manual.title", List.of(), "", "",
            "test:style", List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, "");

    @Test
    void find_listsTheIndexFirstThenEachEntryFoundInItsWordsWithItsLine() {
        final GuideBook book = book();

        final List<GuideSearch.Hit> hits = GuideSearch.find(book, "DIR");

        assertEquals(1, hits.size(), () -> "" + hits);
        final GuideSearch.Hit hit = hits.getFirst();
        assertEquals("alpha:prompt", hit.target());
        assertEquals("dir", hit.line(), "the line the word was found on");
        assertEquals(book.pageOf("alpha:prompt").orElseThrow(), hit.page());
        assertEquals(book.pages().get(hit.page()).folio(), hit.folio());
    }

    @Test
    void find_listsAnEntryFoundByItsTitleOnlyOnce() {
        final List<GuideSearch.Hit> hits = GuideSearch.find(book(), "prompt");

        assertEquals(List.of("alpha:prompt"), hits.stream().map(GuideSearch.Hit::target).toList());
        assertTrue(hits.getFirst().line().isEmpty(), "found in the index, it has no line of its own");
    }

    @Test
    void find_withNothingTypedListsTheWholeIndex() {
        final GuideBook book = book();

        assertEquals(book.index().size(), GuideSearch.find(book, "  ").size());
    }

    /** A manual of two entries: one about files, one about the prompt with a table of its commands. */
    private static GuideBook book() {
        final GuideEntry files = new GuideEntry("alpha:files", "alpha:section", 0, "files.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("Files hold what you write.")));
        final GuideEntry prompt = new GuideEntry("alpha:prompt", "alpha:section", 1, "prompt.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("Type a command."), new GuideBlock.Table("caption",
                        List.of("Command", "What it does"), List.of(new GuideBlock.TableRow(List.of(
                                new GuideBlock.GuideValue.Literal("dir"),
                                new GuideBlock.GuideValue.Words("Lists a folder.")))))));
        return GuideLayout.lay(MANUAL, STYLE, new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("alpha", 0, "alpha.title", "", ""), List.of(new GuideContents.Section(
                        new GuideSection("alpha:section", 0, "section.title", ""), List.of(files, prompt)))))),
                new Words());
    }

    /** Words that are their own keys, six pixels to a letter. */
    private record Words() implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            return key;
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
