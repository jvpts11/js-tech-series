/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.cli.CliTokenizer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShLineTest {

    private static ShLine of(final String line) {
        return ShLine.of(ShWord.typed(CliTokenizer.words(line)));
    }

    @Test
    void of_aPlainLineIsOneStageAndNothingElse() {
        final ShLine line = of("interac list stone");

        assertTrue(line.isSimple());
        assertEquals(1, line.stages().size());
        assertEquals("interac", line.stages().get(0).word());
        assertEquals(List.of("list", "stone"), line.stages().get(0).args());
        assertFalse(line.writes());
    }

    @Test
    void of_aPipeEndsOneStageAndStartsTheNext() {
        final ShLine line = of("interac list | grep stone | wc -l");

        assertFalse(line.isSimple());
        assertEquals(3, line.stages().size());
        assertEquals("grep", line.stages().get(1).word());
        assertEquals(List.of("stone"), line.stages().get(1).args());
        assertEquals("wc", line.stages().get(2).word());
        assertEquals(List.of("-l"), line.stages().get(2).args());
    }

    @Test
    void of_readsAnArrowWrittenAgainstItsNameOrApartFromIt() {
        final ShLine against = of("interac list >stock.txt");
        final ShLine apart = of("interac list > stock.txt");

        assertEquals("stock.txt", against.into());
        assertEquals("stock.txt", apart.into());
        assertFalse(against.append());
        assertTrue(against.writes());
    }

    @Test
    void of_readsTheArrowThatAdds() {
        final ShLine line = of("interac ops >> log.txt");

        assertEquals("log.txt", line.into());
        assertTrue(line.append());
    }

    @Test
    void of_readsWhatFeedsTheFirstCommand() {
        final ShLine line = of("sort < names.txt");

        assertEquals("names.txt", line.from());
        assertEquals("sort", line.stages().get(0).word());
        assertTrue(line.stages().get(0).args().isEmpty(), "the arrow and its name are not the command's words");
    }

    @Test
    void of_keepsTheArrowsOutOfTheWordsOfAPipeline() {
        final ShLine line = of("cat notes.txt | grep oak > found.txt");

        assertEquals(2, line.stages().size());
        assertEquals(List.of("oak"), line.stages().get(1).args());
        assertEquals("found.txt", line.into());
    }

    @Test
    void of_anEmptyLineHasNothingToRun() {
        assertTrue(of("").isEmpty());
        assertTrue(of("   ").isEmpty());
        assertTrue(ShLine.NOTHING.isEmpty());
    }

    @Test
    void of_aNameInQuotesStaysOneWord() {
        final ShLine line = of("interac where \"oak log\" > where.txt");

        assertEquals(List.of("where", "oak log"), line.stages().get(0).args());
        assertEquals("where.txt", line.into());
    }

    @Test
    void of_anArrowInQuotesIsAWordForTheCommand() {
        final ShLine line = of("grep \">\" notes.txt");

        assertEquals(List.of(">", "notes.txt"), line.stages().get(0).args());
        assertFalse(line.writes(), "nothing is written over notes.txt");
    }

    @Test
    void of_anArrowWithNoNameAfterItIsASyntaxError() {
        assertEquals("newline", of("cat a.txt >").badToken());
        assertEquals("newline", of("cat a.txt >>").badToken());
        assertEquals("newline", of("cat < ").badToken());
    }

    @Test
    void of_aPipeWithNothingOnOneSideIsASyntaxError() {
        assertEquals("newline", of("ls |").badToken());
        assertEquals("|", of("| wc").badToken());
        assertEquals("|", of("ls | | wc").badToken());
    }

    @Test
    void of_anArrowFollowedByAMarkNamesThatMarkAsUnexpected() {
        assertEquals("|", of("ls > | wc").badToken());
    }

    @Test
    void of_aWellFormedLineHasNoBadToken() {
        assertEquals("", of("ls | grep a > out.txt").badToken());
        assertEquals("", of("ls").badToken());
    }

    @Test
    void of_aWordThatOnlyReadsAsAMarkIsStillAWord() {
        // What a star opened out into, or what a name stood for, is never one of the shell's marks.
        final ShLine line = ShLine.of(List.of(ShWord.plain("cat"), ShWord.plain(">old.txt"), ShWord.plain("|")));

        assertTrue(line.isSimple(), "one command and nothing else");
        assertEquals(List.of(">old.txt", "|"), line.stages().get(0).args());
    }
}
