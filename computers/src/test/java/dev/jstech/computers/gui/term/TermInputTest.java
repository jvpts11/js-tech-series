/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The line being typed, on the grid: the prompt, a space, the typing, and the cursor's cell. */
class TermInputTest {

    private static final CliLine LIVECD = CliLine.of(new CliSpan("livecd", CliStyle.ERROR),
            new CliSpan(" ~ #", CliStyle.BLUE));

    @Test
    void lay_putsOneSpaceBetweenThePromptAndTheTyping_howeverThePromptEnded() {
        assertEquals("livecd ~ # lsblk", TermInput.lay(LIVECD, "lsblk", CliStyle.PROMPT, 5, 80).rows().get(0).text());
        final CliLine asked = CliLine.plain("Command (m for help): ");
        assertEquals("Command (m for help): n", TermInput.lay(asked, "n", CliStyle.PROMPT, 1, 80).rows().get(0).text());
    }

    @Test
    void lay_keepsThePromptsColoursARunAtATime() {
        final TermRow row = TermInput.lay(LIVECD, "ls", CliStyle.PROMPT, 2, 80).rows().get(0);
        assertEquals(CliStyle.ERROR, row.runs().get(0).style());
        assertEquals("livecd", row.runs().get(0).text());
        assertEquals(CliStyle.BLUE, row.runs().get(1).style());
    }

    @Test
    void lay_theCursorIsCountedFromTheEndOfThePrompt() {
        final TermInput.Laid end = TermInput.lay(LIVECD, "lsblk", CliStyle.PROMPT, 5, 80);
        assertEquals(0, end.cursorRow());
        assertEquals("livecd ~ # lsblk".length(), end.cursorColumn());
        assertEquals("livecd ~ # ".length(), TermInput.lay(LIVECD, "lsblk", CliStyle.PROMPT, 0, 80).cursorColumn());
    }

    /** The partition editor's longest question is wider than the glass, and used to run off its edge. */
    @Test
    void lay_aQuestionWiderThanTheGlass_carriesOnAtTheStartOfTheNextRow() {
        final CliLine asked = CliLine.plain(
                "Last sector, +/-sectors or +/-size{K,M,G,T,P} (2048-1048575966, default 1048573951): ");
        final TermInput.Laid laid = TermInput.lay(asked, "+512M", CliStyle.PROMPT, 5, 64);
        assertEquals(2, laid.rows().size());
        assertEquals(64, laid.rows().get(0).length(), "cut where the row ends and nowhere else");
        assertEquals(1, laid.cursorRow());
        assertEquals(laid.rows().get(1).length(), laid.cursorColumn());
        assertEquals(asked.text() + "+512M", laid.rows().get(0).text() + laid.rows().get(1).text(),
                "and nothing is lost or doubled at the cut");
    }

    @Test
    void lay_aCursorPastTheLastCellOfAFullRow_isAtTheStartOfARowOfItsOwn() {
        final TermInput.Laid laid = TermInput.lay(CliLine.plain(">"), "abcdefgh", CliStyle.PROMPT, 8, 10);
        assertEquals(2, laid.rows().size());
        assertEquals(1, laid.cursorRow());
        assertEquals(0, laid.cursorColumn());
    }

    @Test
    void lay_withNothingBeforeAndNothingTyped_isStillOneRowToPutTheCursorOn() {
        final TermInput.Laid laid = TermInput.lay(CliLine.plain(""), "", CliStyle.PROMPT, 0, 80);
        assertEquals(1, laid.rows().size());
        assertEquals(0, laid.cursorColumn());
    }
}
