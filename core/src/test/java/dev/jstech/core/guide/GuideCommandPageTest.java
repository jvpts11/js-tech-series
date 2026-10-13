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

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextFormat;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GuideCommandPageTest {

    /** The English of the manual's own words, and one sentence a reader's language has. */
    private static final Words WORDS = new Words(Map.of(GuideTexts.AND.key(), "and",
            GuideTexts.RUNS_ON.key(), GuideTexts.RUNS_ON.english(),
            GuideTexts.READ_NEXT.key(), GuideTexts.READ_NEXT.english(),
            "test.usage", "<pasta>"));

    @Test
    void lead_writesWhatACommandIsForAsASentence() {
        assertEquals("List the files in a folder.", GuideCommandPage.lead(command(Text.literal(
                "list the files in a folder")), WORDS));
        assertEquals("Stops where it said so!", GuideCommandPage.lead(command(Text.literal(
                "stops where it said so!")), WORDS));
        assertEquals("", GuideCommandPage.lead(command(Text.EMPTY), WORDS));
    }

    @Test
    void synopsis_writesTheNameAndWhatItTakesInTheReadersLanguage() {
        assertEquals("dir <pasta> [/w]", GuideCommandPage.synopsis(new GuideCommand("dir", Text.EMPTY,
                TextKey.of("test.usage", "<folder>").text(), List.of(), List.of(), List.of(), List.of(), List.of()),
                new Words(Map.of("test.usage", "<pasta> [/w]"))));
        assertEquals("cls", GuideCommandPage.synopsis(command(Text.EMPTY), WORDS));
    }

    @Test
    void said_readsASentenceInTheReadersLanguageAndInItsEnglishWhereItHasNone() {
        assertEquals("<pasta>", WORDS.said(TextKey.of("test.usage", "<folder>").text()));
        assertEquals("Not translated", WORDS.said(TextKey.of("test.other", "Not translated").text()));
        assertEquals("C:\\", WORDS.said(Text.literal("C:\\")));
    }

    @Test
    void runsOn_listsTheSystemsAsASentenceListsThem() {
        assertEquals("Runs on MC-DOS 6.22, Frames 95 and Frames 98.", GuideCommandPage.runsOn(new GuideCommand(
                "dir", Text.EMPTY, Text.EMPTY, List.of(), List.of(), List.of(), List.of(Text.literal("MC-DOS 6.22"),
                Text.literal("Frames 95"), Text.literal("Frames 98")), List.of()), WORDS));
        assertEquals("", GuideCommandPage.runsOn(command(Text.EMPTY), WORDS));
    }

    @Test
    void readNext_writesEachNameAsTheManualLinksIt() {
        final GuideCommand dir = new GuideCommand("dir", Text.EMPTY, Text.EMPTY, List.of(), List.of(), List.of(),
                List.of(), List.of("copy", "tree"));

        assertEquals("Read next: <copy> and <tree>.", GuideCommandPage.readNext(dir, WORDS, name -> "<" + name + ">"));
    }

    @Test
    void options_tabulatesEachOptionAsTypedBesideWhatItDoes() {
        final GuideBlock.Table table = GuideCommandPage.options(new GuideCommand("dir", Text.EMPTY, Text.EMPTY,
                List.of(), List.of(new GuideCommand.Option("/w", Text.literal("Names only, across."))), List.of(),
                List.of(), List.of()));

        assertEquals(List.of(GuideTexts.OPTION.key(), GuideTexts.WHAT_IT_DOES.key()), table.headKeys());
        assertTrue(table.rows().getFirst().cells().get(0).data(), "the option is written as typed, in the code font");
        assertFalse(table.rows().getFirst().cells().get(1).data(), "what it does is words");
        assertEquals("Names only, across.", WORDS.valueText(table.rows().getFirst().cells().get(1)));
    }

    @Test
    void entries_findsTheEntryThatPrintsEachCommandsPage() {
        final GuideEntry dir = new GuideEntry("mod:dir", "mod:section", 0, "dir.title", "", List.of(),
                List.of(new GuideBlock.Command("mod:command/dir")));
        final GuideContents contents = new GuideContents(List.of(new GuideContents.Chapter(
                new GuideChapter("mod", 0, "mod.title", "", ""), List.of(new GuideContents.Section(
                        new GuideSection("mod:section", 0, "section.title", ""), List.of(dir))))));

        assertEquals(Map.of("dir", "mod:dir"), GuideCommandPage.entries(contents, new Words(Map.of()) {
            @Override
            public Optional<GuideCommand> command(final String source) {
                return Optional.of(new GuideCommand(source.substring(source.lastIndexOf('/') + 1), Text.EMPTY,
                        Text.EMPTY, List.of(), List.of(), List.of(), List.of(), List.of()));
            }
        }));
    }

    private static GuideCommand command(final Text summary) {
        return new GuideCommand("cls", summary, Text.EMPTY, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** The words a map holds, a key's own words when it holds none, at six pixels a character. */
    private static class Words implements IGuideText {

        private final Map<String, String> words;

        Words(final Map<String, String> words) {
            this.words = words;
        }

        @Override
        public String text(final String key, final Object... args) {
            if (args.length == 0) {
                return this.words.getOrDefault(key, key);
            }
            final List<String> written = new ArrayList<>();
            for (final Object arg : args) {
                written.add(String.valueOf(arg));
            }
            return TextFormat.apply(this.words.getOrDefault(key, key), written);
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
