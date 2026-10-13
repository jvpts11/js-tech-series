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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuideProseTest {

    @Test
    void speaksToTheReader_findsYouOrYourAsWholeWords() {
        assertTrue(GuideProse.speaksToTheReader("Your computer starts when you press its button."));
        assertFalse(GuideProse.speaksToTheReader("The computer starts. Youth is not a reader."));
    }

    @Test
    void bareNumbers_findsANumberThatSaysNotWhatItMeasures() {
        assertEquals(List.of("4"), GuideProse.bareNumbers("A Personal Computer holds 4."));
        assertEquals(List.of(), GuideProse.bareNumbers("A Personal Computer holds 4 memory modules."));
        assertEquals(List.of(), GuideProse.bareNumbers("It draws 12,000 FE a tick, 75% of its power."));
    }

    @Test
    void bareNumbers_leavesNamesBracketsLinksAndFiguresAlone() {
        assertEquals(List.of(), GuideProse.bareNumbers("MC-DOS 6.22 and Frames 95 run on x86, error S1001."));
        assertEquals(List.of(), GuideProse.bareNumbers("The cases (see 3.2) and [the eras](mod:eras) differ."));
        assertEquals(List.of(), GuideProse.bareNumbers("Press [[F12]] while it spends {{mod:energy}}."));
    }

    @Test
    void bareNumbers_leavesCodeQuotesTimesAndVersionsAlone() {
        assertEquals(List.of(), GuideProse.bareNumbers("So 2 + 3 * 4 is 14, not 20, and 7 / 2 is 3."));
        assertEquals(List.of(), GuideProse.bareNumbers("Type mkfs.fat -F 32 /dev/sda1, or taskkill /PID 12."));
        assertEquals(List.of(), GuideProse.bareNumbers("It prints \"stopped 3\" at 06:00, as release 1.0.0 did."));
        assertEquals(List.of(), GuideProse.bareNumbers("Cast it with (int) 2.5, and gateway-1 becomes gateway_1."));
    }

    @Test
    void bareNumbers_leavesANumberItsMeasureNamesOrItsListSharesAlone() {
        assertEquals(List.of(), GuideProse.bareNumbers("Give a strength from 0 to 15, and set version 2."));
        assertEquals(List.of(), GuideProse.bareNumbers("The card asks 1, 1 or 2 levels, and the count starts at 1."));
        assertEquals(List.of(), GuideProse.bareNumbers("Before the first craft, Mainframe.PeakToday is 0."));
        assertEquals(List.of("32"), GuideProse.bareNumbers("The log holds the last 32, newest first."));
    }

    @Test
    void usedBeforeExplained_passesAFirstUseThatLeadsToTheExplanation() {
        final GuideEntry first = new GuideEntry("mod:first", "mod:section", 0, "first.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("first.text")));
        final GuideEntry linked = new GuideEntry("mod:linked", "mod:section", 0, "linked.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("linked.text")));
        final Words words = new Words(Map.of("first.text", "Every bus carries items.", "linked.text",
                "Read about [the bus on a cable](mod:cables) first.", "bus.term", "bus",
                "bus.def", "A part on a cable that moves items."));

        assertEquals(List.of(), GuideProse.usedBeforeExplained(contentsOf(first, explainer()), words),
                "the manual makes the first bus a link to where it is explained");
        assertEquals(List.of(), GuideProse.usedBeforeExplained(contentsOf(linked, explainer()), words),
                "the writer made the first bus a link of their own");
        assertEquals(List.of(), GuideProse.usedBeforeExplained(contentsOf(explainer(), first), words),
                "explained first, used after: as it should be");
    }

    @Test
    void usedBeforeExplained_namesAFirstUseThatLeadsNowhereNearTheExplanation() {
        // A problem's own words are drawn without links; what a machine says, quoted, is not the book's own use.
        final GuideEntry trouble = new GuideEntry("mod:trouble", "mod:section", 0, "trouble.title", "", List.of(),
                List.of(new GuideBlock.Problems(List.of(new GuideBlock.Problem("trouble.problem", "trouble.fix")))));
        final GuideEntry quoted = new GuideEntry("mod:quoted", "mod:section", 0, "quoted.title", "", List.of(),
                List.of(new GuideBlock.Problems(List.of(new GuideBlock.Problem("quoted.problem", "trouble.fix")))));
        final Words words = new Words(Map.of("trouble.problem", "The bus moves nothing.", "quoted.problem",
                "The screen says \"no bus here\".", "trouble.fix", "Give it a filter.", "bus.term", "bus",
                "bus.def", "A part on a cable that moves items."));

        assertEquals(List.of(new GuideProse.Early("bus", "mod:trouble", "mod:explainer")),
                GuideProse.usedBeforeExplained(contentsOf(trouble, explainer()), words));
        assertEquals(List.of(), GuideProse.usedBeforeExplained(contentsOf(quoted, explainer()), words));
    }

    /** An entry explaining the word bus. */
    private static GuideEntry explainer() {
        return new GuideEntry("mod:explainer", "mod:section", 1, "explainer.title", "", List.of(),
                List.of(new GuideBlock.Define("bus.term", "bus.def")));
    }

    /** A book of one chapter of one section holding those entries in that order. */
    private static GuideContents contentsOf(final GuideEntry... entries) {
        return new GuideContents(List.of(new GuideContents.Chapter(new GuideChapter("mod", 0, "mod.title", "", ""),
                List.of(new GuideContents.Section(new GuideSection("mod:section", 0, "section.title", ""),
                        List.of(entries))))));
    }

    /** The words a map holds, a key's own words when it holds none. */
    private record Words(Map<String, String> words) implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            return this.words.getOrDefault(key, key);
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
