/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.text.ITextLanguage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InfoHelpTest {

    private static final int COLUMNS = 80;
    private static final int ROWS = 24;
    private static final HelpTarget CARDS = HelpTarget.node("test:the_manual", "test:graphics_cards");
    private static final HelpTarget MEMORY = HelpTarget.node("test:the_manual", "test:memory");

    private FakeSource source;

    @BeforeEach
    void setUp() {
        this.source = new FakeSource();
    }

    @Test
    void paint_namesTheNeighboursAtTheTopAndTheFileAndNodeOnTheModeLine() {
        final InfoHelp info = new InfoHelp("graphics cards", this.source, ITextLanguage.ENGLISH);

        final TextScreen screen = info.paint(COLUMNS, ROWS);

        assertEquals("Next: 1.1.3 Sound cards,  Prev: 1.1.1 Memory,  Up: 1.1 Hardware", screen.text(0).strip());
        assertEquals("1.1.2 Graphics cards", screen.text(2).strip());
        assertTrue(screen.text(3).startsWith("===================="), screen.text(3));
        assertTrue(screen.text(ROWS - 2).startsWith("-----Info: (the-manual)Graphics cards, "),
                screen.text(ROWS - 2));
        assertEquals(TextScreen.cga(TextScreen.GREY), screen.ground(0, 0), "the top line in reverse");
        assertEquals(TextScreen.cga(TextScreen.BLACK), screen.ground(0, 5), "the node on black");
    }

    @Test
    void opening_withNothingShowsTheDirectoryOfManualsAndCommands() {
        final InfoHelp info = new InfoHelp("", this.source, ITextLanguage.ENGLISH);

        final String glass = info.paint(COLUMNS, ROWS).text();

        assertEquals(HelpTarget.contents(""), info.shown());
        assertTrue(glass.contains("* The Manual: (the-manual)."), glass);
        assertTrue(glass.contains("* Commands: (commands)."), glass);
        assertTrue(glass.contains(HelpTexts.INFO_WELCOME.english()), glass);
    }

    @Test
    void opening_aCommandNotYetListedFindsItWhenTheMachineAnswers() {
        final InfoHelp info = new InfoHelp("ls", this.source, ITextLanguage.ENGLISH);

        assertEquals(HelpTarget.contents(""), info.shown());

        this.source.commands = List.of(new HelpCommand("Files", "ls", "list the files"));
        info.answered();

        assertEquals(HelpTarget.command("ls"), info.shown());
    }

    @Test
    void nextPreviousAndUp_walkTheNodesAtTheirLevel() {
        final InfoHelp info = new InfoHelp("graphics cards", this.source, ITextLanguage.ENGLISH);

        info.previous();
        assertEquals(MEMORY, info.shown());
        info.previous();
        assertEquals(HelpTexts.INFO_NO_PREV.english(), info.message());
        info.up();
        assertEquals(HelpTarget.node("test:the_manual", "test:hardware"), info.shown());
        info.up();
        assertEquals(HelpTarget.node("test:the_manual", "test"), info.shown());
        info.up();
        assertEquals(HelpTarget.contents("test:the_manual"), info.shown());
        info.lastNode();
        assertEquals(HelpTarget.node("test:the_manual", "test"), info.shown());
    }

    @Test
    void menuItem_goesToTheItemNamedOrSaysThereIsNone() {
        final InfoHelp info = new InfoHelp("hardware", this.source, ITextLanguage.ENGLISH);
        info.paint(COLUMNS, ROWS);
        info.askMenuItem();
        for (final char c : "graph".toCharArray()) {
            info.typed(c);
        }
        info.submit();

        assertEquals(CARDS, info.shown());

        info.askMenuItem();
        info.typed('x');
        info.typed('y');
        info.submit();

        assertEquals("No menu item 'xy' in node 'Graphics cards'.", info.message());
    }

    @Test
    void search_goesToTheNextEntryHoldingTheWords() {
        final InfoHelp info = new InfoHelp("memory", this.source, ITextLanguage.ENGLISH);
        info.askSearch();
        for (final char c : "draws".toCharArray()) {
            info.typed(c);
        }
        info.submit();

        assertEquals(CARDS, info.shown());
    }

    @Test
    void follow_takesTheLinkPickedWithTab() {
        final InfoHelp info = new InfoHelp("hardware", this.source, ITextLanguage.ENGLISH);
        info.paint(COLUMNS, ROWS);
        info.focusNext(false);
        info.focusNext(false);
        info.follow();

        assertEquals(CARDS, info.shown());
    }

    /** A manual of one chapter and one section of three entries, the second about graphics cards. */
    private static ManualReader reader() {
        final GuideEntry memory = entry("test:memory", 0, "Memory", "The part that remembers.");
        final GuideEntry cards = entry("test:graphics_cards", 1, "Graphics cards", "The part that draws.");
        final GuideEntry sound = entry("test:sound_cards", 2, "Sound cards", "The part that plays.");
        return new ManualReader(new GuideManual("test:the_manual", "The Manual", List.of(), "", "", "",
                List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, ""), new GuideContents(List.of(
                new GuideContents.Chapter(new GuideChapter("test", 0, "Chapter", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("test:hardware", 0, "Hardware", ""),
                                List.of(memory, cards, sound)))))), new KeysAsWords());
    }

    private static GuideEntry entry(final String id, final int order, final String title, final String text) {
        return new GuideEntry(id, "test:hardware", order, title, "", List.of(), List.of(
                new GuideBlock.Heading("What it is"), new GuideBlock.Paragraph(text)));
    }

    /** Words that are their own keys, which the manual above is written in. */
    private record KeysAsWords() implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            return args.length == 0 ? key : key + " " + List.of(args);
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

    /** The test manual, and a machine that lists what it is told to. */
    private static final class FakeSource implements IHelpSource {

        private final ManualReader reader = reader();
        private List<HelpCommand> commands = List.of();

        @Override
        public List<ManualReader> manuals() {
            return List.of(this.reader);
        }

        @Override
        public List<HelpCommand> commands() {
            return this.commands;
        }

        @Override
        public List<String> commandPage(final String name) {
            return null;
        }

        @Override
        public List<String> recipeLines(final String type, final String output) {
            return List.of();
        }
    }
}
