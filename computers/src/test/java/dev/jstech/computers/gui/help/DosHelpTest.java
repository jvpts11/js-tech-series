/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DosHelpTest {

    private static final int COLUMNS = 80;
    private static final int ROWS = 25;

    private FakeSource source;

    @BeforeEach
    void setUp() {
        this.source = new FakeSource();
    }

    @Test
    void paint_drawsTheMenuBarTheButtonsTheTopicAndTheKeys() {
        final DosHelp help = new DosHelp("MC-DOS", "", this.source, ITextLanguage.ENGLISH);

        final TextScreen screen = help.paint(COLUMNS, ROWS);

        assertTrue(screen.text(0).contains("File") && screen.text(0).contains("Search"), screen.text(0));
        assertTrue(screen.text(0).contains("MC-DOS Help: Contents"), screen.text(0));
        assertTrue(screen.text(1).startsWith(" <Next>  <Back>  <Contents>  <Index>"), screen.text(1));
        assertTrue(screen.text(2).isBlank(), "a row left under the buttons; got " + screen.text(2));
        assertTrue(screen.text(3).contains("The Manual"), "the topic under it; got " + screen.text(3));
        assertTrue(screen.text(ROWS - 1).startsWith("<Alt+C=Contents>"), screen.text(ROWS - 1));
        assertEquals(TextScreen.cga(TextScreen.BLUE), screen.ground(5, 5), "white on blue");
        assertEquals(TextScreen.cga(TextScreen.CYAN), screen.ground(0, ROWS - 1), "the keys on cyan");
    }

    @Test
    void opening_findsAnEntryByNameOrTakesItForACommand() {
        assertEquals(HelpTarget.node("test:manual", "test:monitors"),
                new DosHelp("MC-DOS", "monitors", this.source, ITextLanguage.ENGLISH).shown());
        assertEquals(HelpTarget.command("dir"),
                new DosHelp("MC-DOS", "DIR", this.source, ITextLanguage.ENGLISH).shown());
    }

    @Test
    void lay_turnsANameNothingAnswersToIntoTheContentsOnceTheMachineHasListed() {
        final DosHelp help = new DosHelp("MC-NET", "nonsense", this.source, ITextLanguage.ENGLISH);
        this.source.commands = List.of(new HelpCommand("Files", "dir", "list"));
        help.answered();

        final TextScreen screen = help.paint(COLUMNS, ROWS);

        assertEquals(HelpTexts.CONTENTS.english(), help.title());
        assertTrue(screen.text(ROWS - 1).startsWith(HelpTexts.DOS_NOT_FOUND.english()), screen.text(ROWS - 1));
        assertTrue(screen.text(0).contains("MC-NET Help"), screen.text(0));
    }

    @Test
    void follow_goesWhereTheLinkPickedLeadsAndBackComesHome() {
        final DosHelp help = new DosHelp("MC-DOS", "", this.source, ITextLanguage.ENGLISH);
        help.paint(COLUMNS, ROWS);
        // The contents' links: the chapter, the section, then the two entries.
        help.focusNext(false);
        help.focusNext(false);
        help.focusNext(false);
        help.follow();

        assertEquals(HelpTarget.node("test:manual", "test:graphics_cards"), help.shown());

        help.back();

        assertEquals(HelpTarget.contents("test:manual"), help.shown());
    }

    @Test
    void next_walksTheEntriesAndTheCommands() {
        final DosHelp help = new DosHelp("MC-DOS", "graphics cards", this.source, ITextLanguage.ENGLISH);
        help.next();

        assertEquals(HelpTarget.node("test:manual", "test:monitors"), help.shown());

        this.source.commands = List.of(new HelpCommand("Files", "dir", "list"), new HelpCommand("Files", "copy",
                "copy"));
        final DosHelp commands = new DosHelp("MC-DOS", "dir", this.source, ITextLanguage.ENGLISH);
        commands.next();

        assertEquals(HelpTarget.command("copy"), commands.shown());
    }

    @Test
    void command_asksTheMachineForItsPageAndShowsItWhenItComes() {
        this.source.commands = List.of(new HelpCommand("Files", "dir", "list"));
        final DosHelp help = new DosHelp("MC-DOS", "dir", this.source, ITextLanguage.ENGLISH);

        help.paint(COLUMNS, ROWS);

        assertEquals(List.of("dir"), this.source.asked);
        this.source.pages.put("dir", List.of("NAME", "  dir - list the files"));
        help.answered();

        assertTrue(help.paint(COLUMNS, ROWS).text().contains("dir - list the files"));
    }

    @Test
    void find_goesToTheNextEntryHoldingTheWordsOrSaysThereIsNone() {
        final DosHelp help = new DosHelp("MC-DOS", "", this.source, ITextLanguage.ENGLISH);
        help.startFind();
        for (final char c : "glass".toCharArray()) {
            help.typed(c);
        }
        help.submitFind();

        assertEquals(HelpTarget.node("test:manual", "test:monitors"), help.shown());

        help.startFind();
        help.typed('z');
        help.submitFind();

        assertEquals(HelpTexts.DOS_NOT_FOUND.english(), help.message());
    }

    @Test
    void menu_exitAsksToLeave() {
        final DosHelp help = new DosHelp("MC-DOS", "", this.source, ITextLanguage.ENGLISH);
        help.openMenu(0);

        assertTrue(help.paint(COLUMNS, ROWS).text(2).contains(HelpTexts.DOS_EXIT.english()), "the File menu down");

        help.pickMenuItem();

        assertTrue(help.leaving());
        assertFalse(help.menuOpen());
    }

    @Test
    void clicked_aButtonDoesWhatItSays() {
        final DosHelp help = new DosHelp("MC-DOS", "monitors", this.source, ITextLanguage.ENGLISH);
        help.paint(COLUMNS, ROWS);

        // <Contents> is the third button along the row under the menu bar.
        help.clicked(1, " <Next>  <Back>  <".length());

        assertEquals(HelpTarget.contents("test:manual"), help.shown());
    }

    /** A manual of one chapter and one section: an entry on graphics cards and one on monitors. */
    private static ManualReader reader() {
        final GuideEntry cards = new GuideEntry("test:graphics_cards", "test:hardware", 0, "Graphics cards", "",
                List.of(), List.of(new GuideBlock.Heading("What it is"),
                new GuideBlock.Paragraph("The part that draws."), new GuideBlock.SeeAlso(List.of("test:monitors"))));
        final GuideEntry monitors = new GuideEntry("test:monitors", "test:hardware", 1, "Monitors", "", List.of(),
                List.of(new GuideBlock.Paragraph("The glass.")));
        return new ManualReader(new GuideManual("test:manual", "The Manual", List.of(), "", "", "",
                List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, ""), new GuideContents(List.of(
                new GuideContents.Chapter(new GuideChapter("test", 0, "Chapter", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("test:hardware", 0, "Hardware", ""),
                                List.of(cards, monitors)))))), new KeysAsWords());
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

    /** The test manual, and a machine that answers when it is told to. */
    private static final class FakeSource implements IHelpSource {

        private final ManualReader reader = reader();
        private final Map<String, List<String>> pages = new HashMap<>();
        private final List<String> asked = new ArrayList<>();
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
            if (!this.pages.containsKey(name) && !this.asked.contains(name)) {
                this.asked.add(name);
            }
            return this.pages.get(name);
        }

        @Override
        public List<String> recipeLines(final String type, final String output) {
            return List.of();
        }
    }
}
