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
    void usedBeforeExplained_namesAWordAnEarlierEntryUsesBeforeItIsExplained() {
        final GuideEntry first = new GuideEntry("mod:first", "mod:section", 0, "first.title", "", List.of(),
                List.of(new GuideBlock.Paragraph("first.text")));
        final GuideEntry second = new GuideEntry("mod:second", "mod:section", 1, "second.title", "", List.of(),
                List.of(new GuideBlock.Define("bus.term", "bus.def")));
        final GuideContents contents = new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("mod", 0, "mod.title", "", ""), List.of(new GuideContents.Section(
                        new GuideSection("mod:section", 0, "section.title", ""), List.of(first, second))))));
        final Words words = new Words(Map.of("first.text", "Every bus carries items.", "bus.term", "bus",
                "bus.def", "A part on a cable that moves items."));

        assertEquals(List.of(new GuideProse.Early("bus", "mod:first", "mod:second")),
                GuideProse.usedBeforeExplained(contents, words));
        assertEquals(List.of(), GuideProse.usedBeforeExplained(new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("mod", 0, "mod.title", "", ""), List.of(new GuideContents.Section(
                        new GuideSection("mod:section", 0, "section.title", ""), List.of(second, first)))))), words),
                "explained first, used after: as it should be");
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
