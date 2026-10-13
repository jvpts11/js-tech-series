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
import dev.jstech.core.text.ITextLanguage;
import dev.jstech.core.text.TextKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The line being typed, on the grid: the prompt, a space, the typing, and the cursor's cell. */
class TermInputTest {

    private static final CliLine LIVECD = CliLine.of(new CliSpan("livecd", CliStyle.ERROR),
            new CliSpan(" ~ #", CliStyle.BLUE));

    @Test
    void lay_putsOneSpaceBetweenThePromptAndTheTyping_howeverThePromptEnded() {
        assertEquals("livecd ~ # lsblk", lay(LIVECD, "lsblk", 5, 80).rows().get(0).text());
        final CliLine asked = CliLine.plain("Command (m for help): ");
        assertEquals("Command (m for help): n", lay(asked, "n", 1, 80).rows().get(0).text());
    }

    @Test
    void lay_keepsThePromptsColoursARunAtATime() {
        final TermRow row = lay(LIVECD, "ls", 2, 80).rows().get(0);
        assertEquals(CliStyle.ERROR, row.runs().get(0).style());
        assertEquals("livecd", row.runs().get(0).text());
        assertEquals(CliStyle.BLUE, row.runs().get(1).style());
    }

    @Test
    void lay_theCursorIsCountedFromTheEndOfThePrompt() {
        final TermInput.Laid end = lay(LIVECD, "lsblk", 5, 80);
        assertEquals(0, end.cursorRow());
        assertEquals("livecd ~ # lsblk".length(), end.cursorColumn());
        assertEquals("livecd ~ # ".length(), lay(LIVECD, "lsblk", 0, 80).cursorColumn());
    }

    /** The partition editor's longest question is wider than the glass, and used to run off its edge. */
    @Test
    void lay_aQuestionWiderThanTheGlass_carriesOnAtTheStartOfTheNextRow() {
        final CliLine asked = CliLine.plain(
                "Last sector, +/-sectors or +/-size{K,M,G,T,P} (2048-1048575966, default 1048573951): ");
        final TermInput.Laid laid = lay(asked, "+512M", 5, 64);
        assertEquals(2, laid.rows().size());
        assertEquals(64, laid.rows().get(0).length(), "cut where the row ends and nowhere else");
        assertEquals(1, laid.cursorRow());
        assertEquals(laid.rows().get(1).length(), laid.cursorColumn());
        assertEquals(asked.text() + "+512M", laid.rows().get(0).text() + laid.rows().get(1).text(),
                "and nothing is lost or doubled at the cut");
    }

    @Test
    void lay_aCursorPastTheLastCellOfAFullRow_isAtTheStartOfARowOfItsOwn() {
        final TermInput.Laid laid = lay(CliLine.plain(">"), "abcdefgh", 8, 10);
        assertEquals(2, laid.rows().size());
        assertEquals(1, laid.cursorRow());
        assertEquals(0, laid.cursorColumn());
    }

    @Test
    void lay_withNothingBeforeAndNothingTyped_isStillOneRowToPutTheCursorOn() {
        final TermInput.Laid laid = lay(CliLine.plain(""), "", 0, 80);
        assertEquals(1, laid.rows().size());
        assertEquals(0, laid.cursorColumn());
    }

    /** A question a tool asks is read in the player's language, and the typing starts after it however long it is. */
    @Test
    void lay_aDeclaredQuestion_isLaidOutInTheLanguageItIsReadIn() {
        final CliLine asked = CliLine.of(TextKey.of("jsc.test.question", "Proceed?"), CliStyle.PLAIN);
        final ITextLanguage other = key -> "jsc.test.question".equals(key) ? "Continuar mesmo assim?" : null;
        final TermInput.Laid laid = TermInput.lay(asked, "y", CliStyle.PROMPT, 1, 80, other);
        assertEquals("Continuar mesmo assim? y", laid.rows().get(0).text());
        assertEquals("Continuar mesmo assim? y".length(), laid.cursorColumn());
    }

    private static TermInput.Laid lay(final CliLine before, final String typed, final int cursor, final int columns) {
        return TermInput.lay(before, typed, CliStyle.PROMPT, cursor, columns, ITextLanguage.ENGLISH);
    }
}
