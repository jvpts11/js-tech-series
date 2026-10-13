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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuideGlossaryTest {

    @Test
    void of_gathersEveryWordExplainedAlphabetically() {
        final GuideGlossary glossary = glossary();

        assertEquals(List.of("Mainframe", "network", "network engine"),
                glossary.items().stream().map(GuideGlossary.Item::term).toList());
    }

    @Test
    void linked_leadsTheLongestWordFirstAndOnlyItsFirstUse() {
        final Set<String> used = new HashSet<>();

        final List<GuideLinks.Run> runs = glossary().linked(List.of(new GuideLinks.Run(
                "The network engine plans; the network engine runs.", "")), "mod:other", used);

        assertEquals(List.of(new GuideLinks.Run("The ", ""), new GuideLinks.Run("network engine", "mod:engines"),
                new GuideLinks.Run(" plans; the network engine runs.", "")), runs);
        assertTrue(used.contains("network engine"));
    }

    @Test
    void linked_matchesWholeWordsWhateverTheirCaseAndLeavesLinksAlone() {
        final List<GuideLinks.Run> runs = glossary().linked(List.of(new GuideLinks.Run("Mainframes and the ", ""),
                new GuideLinks.Run("mainframe (1.2)", "mod:parts"), new GuideLinks.Run(", a MAINFRAME.", "")),
                "mod:other", new HashSet<>());

        assertEquals(new GuideLinks.Run("MAINFRAME", "mod:mainframes"), runs.get(3),
                "not inside Mainframes, not inside a link, but the whole word in capitals");
    }

    @Test
    void linked_leavesAWordThatIsPartOfAName() {
        final List<GuideLinks.Run> runs = glossary().linked(List.of(new GuideLinks.Run(
                "Open the Network Manager on a Mainframe.", "")), "mod:other", new HashSet<>());

        assertEquals(List.of(new GuideLinks.Run("Open the Network Manager on a ", ""),
                new GuideLinks.Run("Mainframe", "mod:mainframes"), new GuideLinks.Run(".", "")), runs,
                "the network is not led from inside the Network Manager's name");
        assertEquals(List.of(new GuideLinks.Run("Type cd /mnt/network or run network_check.", "")),
                glossary().linked(List.of(new GuideLinks.Run("Type cd /mnt/network or run network_check.", "")),
                        "mod:other", new HashSet<>()), "nor from inside a path or a name in code");
    }

    @Test
    void linked_leavesTheWordsAnEntryExplainsItself() {
        final List<GuideLinks.Run> runs = glossary().linked(List.of(new GuideLinks.Run("The network.", "")),
                "mod:network", new HashSet<>());

        assertEquals(List.of(new GuideLinks.Run("The network.", "")), runs);
    }

    private static GuideGlossary glossary() {
        final GuideEntry network = entry("mod:network", "network");
        final GuideEntry engines = entry("mod:engines", "network engine");
        final GuideEntry mainframes = entry("mod:mainframes", "Mainframe");
        return GuideGlossary.of(new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("mod", 0, "mod.title", "", ""), List.of(new GuideContents.Section(
                        new GuideSection("mod:section", 0, "section.title", ""),
                        List.of(network, engines, mainframes)))))), new Words());
    }

    private static GuideEntry entry(final String id, final String term) {
        return new GuideEntry(id, "mod:section", 0, id + ".title", "", List.of(),
                List.of(new GuideBlock.Define(term, term + ".def")));
    }

    /** Words that are their own keys. */
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
