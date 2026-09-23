/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.text.ITextLanguage;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import org.junit.jupiter.api.Test;

class CliLineTest {

    private static final TextKey MEMORY = TextKey.of("jsc.test.memory", "Memory");

    /** A language in which the label is longer than its English. */
    private static final ITextLanguage LONGER = key -> "jsc.test.memory".equals(key) ? "Memoria livre" : null;

    @Test
    void text_isWhatTheLineSaysInEnglish() {
        final CliLine line = CliLine.of(CliSpan.plain(MEMORY.text()), CliSpan.plain(": 512 MB"));
        assertEquals("Memory: 512 MB", line.text());
    }

    @Test
    void resolve_putsEachRunInTheLanguageAndKeepsItsColour() {
        final CliLine line = CliLine.of(new CliSpan(MEMORY.text(), CliStyle.ACCENT), CliSpan.plain(" ok"));
        assertEquals(List.of(new CliRun("Memoria livre", CliStyle.ACCENT), CliRun.plain(" ok")),
                line.resolve(LONGER));
    }

    @Test
    void resolve_aClosingFill_leavesTheValueAgainstTheColumnInEveryLanguage() {
        final CliLine row = CliLine.of(CliSpan.plain(MEMORY.text()), CliSpan.fill(24, true),
                CliSpan.plain(Text.literal("512 MB")));
        assertEquals("Memory " + ".".repeat(10) + " 512 MB", row.text());
        assertEquals(24, row.text().length());
        assertEquals("Memoria livre " + ".".repeat(3) + " 512 MB", row.text(LONGER));
        assertEquals(24, row.text(LONGER).length());
    }

    @Test
    void resolve_anOpeningFill_startsWhatFollowsAtTheColumnInEveryLanguage() {
        final CliLine entry = CliLine.of(CliSpan.plain("ls"), CliSpan.fill(10, false),
                CliSpan.plain(MEMORY.text()));
        assertEquals("ls " + ".".repeat(6) + " Memory", entry.text());
        assertEquals("ls " + ".".repeat(6) + " Memoria livre", entry.text(LONGER));
    }

    @Test
    void resolve_aFillWithNoRoom_stillKeepsTheWordsApart() {
        final CliLine row = CliLine.of(CliSpan.plain("a-very-long-label"), CliSpan.fill(10, true),
                CliSpan.plain("value"));
        assertEquals("a-very-long-label  value", row.text());
        final CliLine entry = CliLine.of(CliSpan.plain("a-very-long-name"), CliSpan.fill(10, false),
                CliSpan.plain("what it does"));
        assertEquals("a-very-long-name what it does", entry.text());
    }

    @Test
    void resolve_leavesOutRunsThatComeToNothing() {
        final CliLine line = CliLine.of(CliSpan.plain(""), CliSpan.plain("x"));
        assertEquals(List.of(CliRun.plain("x")), line.resolve(ITextLanguage.ENGLISH));
    }
}
