/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.guide.TextSize;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HelpTreeTest {

    private static final List<HelpCommand> COMMANDS = List.of(new HelpCommand("Files", "ls", "list"),
            new HelpCommand("Network", "ping", "reach"));

    @Test
    void rows_showOnlyWhatIsInsideOpenBooks() {
        final List<HelpTree.Row> shut = HelpTree.rows(List.of(reader()), COMMANDS, "Commands", Set.of());

        assertEquals(List.of("The Manual", "Commands"), shut.stream().map(HelpTree.Row::label).toList());
        assertEquals(HelpTree.Icon.BOOK, shut.getFirst().icon());

        final Set<String> open = new HashSet<>(HelpTree.holding(HelpTarget.node("test:manual", "test:cards"),
                List.of(reader()), COMMANDS));
        final List<HelpTree.Row> opened = HelpTree.rows(List.of(reader()), COMMANDS, "Commands", open);

        assertEquals(List.of("The Manual", "1 Chapter", "1.1 Hardware", "1.1.1 Graphics cards", "Commands"),
                opened.stream().map(HelpTree.Row::label).toList());
        assertEquals(List.of(0, 1, 2, 3, 0), opened.stream().map(HelpTree.Row::depth).toList());
        assertEquals(HelpTree.Icon.OPEN_BOOK, opened.get(2).icon());
        assertEquals(HelpTree.Icon.PAGE, opened.get(3).icon());
    }

    @Test
    void rows_fileTheCommandsUnderTheirGroups() {
        final Set<String> open = new HashSet<>(HelpTree.holding(HelpTarget.command("ping"), List.of(reader()),
                COMMANDS));
        final List<HelpTree.Row> rows = HelpTree.rows(List.of(reader()), COMMANDS, "Commands", open);

        assertEquals(List.of("The Manual", "Commands", "Files", "Network", "ping"),
                rows.stream().map(HelpTree.Row::label).toList());
        assertEquals(HelpTarget.command("ping"), rows.getLast().target());
        assertEquals(null, rows.get(2).target(), "a group only opens");
    }

    private static ManualReader reader() {
        final GuideEntry cards = new GuideEntry("test:cards", "test:hardware", 0, "Graphics cards", "", List.of(),
                List.of(new GuideBlock.Paragraph("The part that draws.")));
        return new ManualReader(new GuideManual("test:manual", "The Manual", List.of(), "", "", "",
                List.of(GuideManual.EVERY_CHAPTER), List.of(), 0, ""), new GuideContents(List.of(
                new GuideContents.Chapter(new GuideChapter("test", 0, "Chapter", "", ""), List.of(
                        new GuideContents.Section(new GuideSection("test:hardware", 0, "Hardware", ""),
                                List.of(cards)))))), new KeysAsWords());
    }

    /** Words that are their own keys, which the manual above is written in. */
    private record KeysAsWords() implements IGuideText {

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
