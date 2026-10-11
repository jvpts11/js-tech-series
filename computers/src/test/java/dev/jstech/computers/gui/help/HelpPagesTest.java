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
import java.util.Map;
import org.junit.jupiter.api.Test;

class HelpPagesTest {

    private static final int COLUMNS = 80;

    @Test
    void article_dosSetsHeadingsAtOneAndTextAtThreeUnderARuledTitle() {
        final List<String> lines = text(HelpPages.article(reader(), article(), HelpPages.Voice.DOS, COLUMNS,
                ITextLanguage.ENGLISH, (type, output) -> List.of()));

        assertEquals("", lines.get(0), "a row left under the buttons");
        assertEquals(" 1.1.1 Graphics cards", lines.get(1));
        assertTrue(lines.get(2).startsWith(" ───"), "a rule under the title; got " + lines.get(2));
        assertEquals(" What it is", lines.get(3));
        assertEquals("   The part that draws.", lines.get(4));
        assertTrue(lines.contains(" VRAM 3 GB   Cores 2,880"), "the table on one line; got " + lines);
        assertTrue(lines.getLast().contains("<1.1.2 Monitors>"), "the links in brackets; got " + lines.getLast());
    }

    @Test
    void article_infoUnderlinesTheTitleAndWritesLinksAsNotes() {
        final List<String> lines = text(HelpPages.article(reader(), article(), HelpPages.Voice.INFO, COLUMNS,
                ITextLanguage.ENGLISH, (type, output) -> List.of()));

        assertEquals("1.1.1 Graphics cards", lines.get(0));
        assertEquals("====================", lines.get(1));
        assertEquals("What it is", lines.get(3));
        assertEquals("   The part that draws.", lines.get(4));
        assertTrue(lines.getLast().contains("*Note 1.1.2 Monitors::"), "an info cross-reference; got " + lines);
    }

    @Test
    void article_manHasItsHeaderNameCapitalsAndSeeAlso() {
        final List<String> lines = text(HelpPages.article(reader(), article(), HelpPages.Voice.MAN, COLUMNS,
                ITextLanguage.ENGLISH, (type, output) -> List.of("Card + Chip -> Board (5 s)")));

        assertTrue(lines.get(0).startsWith("GRAPHICS_CARDS(7)") && lines.get(0).endsWith("GRAPHICS_CARDS(7)"),
                "the page's name at both ends; got " + lines.get(0));
        assertTrue(lines.get(0).contains("The Manual"), "and the manual's title between; got " + lines.get(0));
        assertTrue(lines.contains("NAME") && lines.contains("       graphics_cards - Graphics cards"),
                "a NAME section; got " + lines);
        assertTrue(lines.contains("WHAT IT IS"), "headings in capitals; got " + lines);
        assertTrue(lines.contains("       Card + Chip -> Board (5 s)"), "a recipe as a line; got " + lines);
        assertTrue(lines.contains("SEE ALSO") && lines.contains("       monitors(7)"), "see also; got " + lines);
    }

    @Test
    void article_dosWrapsATableCellWiderThanTheGlass() {
        final int narrow = 12;
        for (final String line : text(HelpPages.article(reader(), article(), HelpPages.Voice.DOS, narrow,
                ITextLanguage.ENGLISH, (type, output) -> List.of()))) {
            if (line.contains("Cores") || line.contains("2,880")) {
                assertTrue(line.length() <= narrow, "'" + line + "' is " + line.length() + " wide");
            }
        }
    }

    @Test
    void article_keepsEveryLineInsideTheGlass() {
        for (final HelpPages.Voice voice : HelpPages.Voice.values()) {
            for (final String line : text(HelpPages.article(reader(), article(), voice, 30, ITextLanguage.ENGLISH,
                    (type, output) -> List.of()))) {
                assertTrue(line.length() <= 30, voice + ": '" + line + "' is " + line.length() + " wide");
            }
        }
    }

    @Test
    void article_setsATableOfCommandsInColumnsAndASessionAKeyAndASceneAsText() {
        final GuideEntry files = new GuideEntry("test:files", "test:hardware", 0, "files.title", "", List.of(),
                List.of(new GuideBlock.Table("files.table", List.of("head.command", "head.does"), List.of(
                                new GuideBlock.TableRow(List.of(new GuideBlock.GuideValue.Literal("dir"),
                                        new GuideBlock.GuideValue.Words("does.dir"))),
                                new GuideBlock.TableRow(List.of(new GuideBlock.GuideValue.Literal("del"),
                                        new GuideBlock.GuideValue.Words("does.del"))))),
                        new GuideBlock.Session("dos", "MC-DOS 6.22", List.of(
                                GuideBlock.SessionLine.typed("C:\\>", "dir"),
                                GuideBlock.SessionLine.answer(" Volume in drive C"))),
                        new GuideBlock.Paragraph("files.keys"),
                        new GuideBlock.Scene(List.of(new GuideBlock.SceneBlock(0, 0, 0, "test:case", "")),
                                GuideBlock.SceneView.CORNER, List.of(new GuideBlock.SceneBalloon(1,
                                GuideBlock.ScenePart.of(0, 0, 0), "files.balloon", "")), false, 90,
                                "files.scene")));
        final ManualReader reader = new ManualReader(new GuideManual("test:manual", "manual.title", List.of(), "",
                "", "", List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, ""), new GuideContents(List.of(
                new GuideContents.Chapter(new GuideChapter("test", 0, "chapter.title", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("test:hardware", 0, "hardware.title", ""),
                                List.of(files)))))), new Words());

        for (final HelpPages.Voice voice : HelpPages.Voice.values()) {
            final List<String> lines = text(HelpPages.article(reader, reader.article("test:files").orElseThrow(),
                    voice, COLUMNS, ITextLanguage.ENGLISH, (type, output) -> List.of()));
            assertTrue(lines.stream().anyMatch(line -> line.strip().equals("Command  What it does")),
                    voice + ": the heads over their columns; got " + lines);
            assertTrue(lines.stream().anyMatch(line -> line.strip().equals("dir      Lists the folder.")),
                    voice + ": a command beside what it does; got " + lines);
            assertTrue(lines.stream().anyMatch(line -> line.strip().equals("C:\\>dir")),
                    voice + ": a session as the terminal printed it; got " + lines);
            assertTrue(lines.stream().anyMatch(line -> line.contains("[Ctrl]+[S]")),
                    voice + ": a key as its name in brackets; got " + lines);
            assertTrue(lines.stream().anyMatch(line -> line.strip().startsWith("1 The front")),
                    voice + ": a scene as the list of its balloons; got " + lines);
        }
    }

    @Test
    void contents_listsTheTreeAsLinksThenTheCommandsAcross() {
        final List<HelpLine> lines = HelpPages.contents(reader(), List.of(), List.of(
                new HelpCommand("Files", "dir", "list"), new HelpCommand("Files", "copy", "copy")), COLUMNS,
                ITextLanguage.ENGLISH);
        final List<String> text = text(lines);

        assertTrue(text.contains("   <1 Chapter>"), "the chapter; got " + text);
        assertTrue(text.contains("       <1.1.1 Graphics cards>"), "an entry set in under its section; got " + text);
        assertTrue(text.stream().anyMatch(line -> line.contains("<dir>") && line.contains("<copy>")),
                "the commands across one line; got " + text);
        assertTrue(lines.stream().flatMap(line -> line.links().stream())
                .anyMatch(link -> link.span().link().equals(HelpTarget.command("dir").written())), "dir is a link");
    }

    @Test
    void command_showsThePageOrSaysItIsBeingAskedFor() {
        assertTrue(text(HelpPages.command("dir", null, HelpPages.Voice.DOS, COLUMNS, ITextLanguage.ENGLISH))
                .contains("   " + HelpTexts.NOTHING_YET.english()));
        assertEquals(List.of("", " DIR", " ─".concat("─".repeat(77)), " NAME", "   dir - list"),
                text(HelpPages.command("dir", List.of("NAME", "  dir - list"), HelpPages.Voice.DOS, COLUMNS,
                        ITextLanguage.ENGLISH)));
    }

    @Test
    void wrap_breaksAtSpacesAndCutsAWordLongerThanALine() {
        assertEquals(List.of("one two", "three"), HelpPages.wrap("one two three", 8));
        assertEquals(List.of("abcde", "fgh"), HelpPages.wrap("abcdefgh", 5));
        assertEquals(List.of(), HelpPages.wrap("", 5));
    }

    private static List<String> text(final List<HelpLine> lines) {
        return lines.stream().map(HelpLine::text).toList();
    }

    private static ManualReader.Article article() {
        return reader().article("test:graphics_cards").orElseThrow();
    }

    /** A manual of one chapter: an entry on graphics cards with a table and a link, and one on monitors. */
    private static ManualReader reader() {
        final GuideEntry cards = new GuideEntry("test:graphics_cards", "test:hardware", 0, "cards.title", "",
                List.of(), List.of(new GuideBlock.Heading("what"), new GuideBlock.Paragraph("cards.what"),
                new GuideBlock.Table("cards.table", List.of(
                        new GuideBlock.TableRow("vram", new GuideBlock.GuideValue.Literal("3 GB")),
                        new GuideBlock.TableRow("cores", new GuideBlock.GuideValue.Literal("2,880")))),
                new GuideBlock.Recipes("test:press", ""),
                new GuideBlock.SeeAlso(List.of("test:monitors"))));
        final GuideEntry monitors = new GuideEntry("test:monitors", "test:hardware", 1, "monitors.title", "",
                List.of(), List.of(new GuideBlock.Paragraph("monitors.what")));
        return new ManualReader(new GuideManual("test:manual", "manual.title", List.of(), "", "", "",
                List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, ""), new GuideContents(List.of(
                new GuideContents.Chapter(new GuideChapter("test", 0, "chapter.title", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("test:hardware", 0, "hardware.title", ""),
                                List.of(cards, monitors)))))), new Words());
    }

    /** The manual's words in English. */
    private record Words() implements IGuideText {

        private static final Map<String, String> ENGLISH = Map.ofEntries(Map.entry("manual.title", "The Manual"),
                Map.entry("chapter.title", "Chapter"), Map.entry("hardware.title", "Hardware"),
                Map.entry("cards.title", "Graphics cards"), Map.entry("monitors.title", "Monitors"),
                Map.entry("what", "What it is"), Map.entry("cards.what", "The part that draws."),
                Map.entry("monitors.what", "The glass."), Map.entry("cards.table", "A card"),
                Map.entry("vram", "VRAM"), Map.entry("cores", "Cores"), Map.entry("jscore.guide.see_also",
                        "See also:"), Map.entry("jscore.guide.table", "Table %s."),
                Map.entry("jscore.guide.figure", "Figure %s."), Map.entry("files.title", "Files"),
                Map.entry("files.table", "Commands"), Map.entry("head.command", "Command"),
                Map.entry("head.does", "What it does"), Map.entry("does.dir", "Lists the folder."),
                Map.entry("does.del", "Deletes a file."), Map.entry("files.keys", "Save with [[Ctrl]]+[[S]]."),
                Map.entry("files.scene", "A case."), Map.entry("files.balloon", "The front"));

        @Override
        public String text(final String key, final Object... args) {
            final String found = ENGLISH.getOrDefault(key, key);
            return args.length == 0 ? found : String.format(found, args);
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
