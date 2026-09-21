/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.cli.CliSpan;
import java.util.List;
import org.junit.jupiter.api.Test;

class TermSelectionTest {

    private static final List<TermRow> GLASS = List.of(
            row("ITEM                        COUNT"),
            row("Oak Log                     3,322"),
            row("Spruce Log                  1,020"),
            row("minecraft:oak_log stored    3,322"));

    private static TermRow row(final String text) {
        return new TermRow(List.of(CliSpan.plain(text)));
    }

    @Test
    void at_takesNothingUntilItIsDragged() {
        assertTrue(TermSelection.at(1, 4).isEmpty());
        assertFalse(TermSelection.at(1, 4).reachingTo(1, 5).isEmpty());
    }

    @Test
    void covers_takesTheCellsBetweenTheTwoEnds() {
        final TermSelection selection = TermSelection.at(1, 0).reachingTo(1, 7);

        assertTrue(selection.covers(1, 0));
        assertTrue(selection.covers(1, 6));
        assertFalse(selection.covers(1, 7), "the far end is the cell after the last one taken");
        assertFalse(selection.covers(0, 3));
        assertFalse(selection.covers(2, 3));
    }

    @Test
    void covers_readsTheSameWhenItWasDraggedBackwards() {
        final TermSelection forwards = TermSelection.at(1, 2).reachingTo(2, 5);
        final TermSelection backwards = TermSelection.at(2, 5).reachingTo(1, 2);

        for (int row = 0; row < GLASS.size(); row++) {
            for (int column = 0; column < 34; column++) {
                assertEquals(forwards.covers(row, column), backwards.covers(row, column),
                        "row " + row + " cell " + column);
            }
        }
    }

    @Test
    void textOf_joinsTheRowsAndDropsWhatPaddedThem() {
        final TermSelection two = TermSelection.at(1, 0).reachingTo(2, 33);

        assertEquals("Oak Log                     3,322\nSpruce Log                  1,020", two.textOf(GLASS));
    }

    @Test
    void textOf_takesOnlyWhatIsPickedOutOfTheFirstAndLastRows() {
        final TermSelection part = TermSelection.at(1, 4).reachingTo(2, 6);

        assertEquals("Log                     3,322\nSpruce", part.textOf(GLASS),
                "the first row is taken from where it was started to its end, and the last one only as far as it goes");
    }

    @Test
    void textOf_isNothingWhenNothingIsPickedOut() {
        assertEquals("", TermSelection.NONE.textOf(GLASS));
        assertEquals("", TermSelection.at(1, 4).textOf(GLASS));
    }

    @Test
    void textOf_staysInsideAGlassThatHasSinceScrolled() {
        final TermSelection past = TermSelection.at(2, 0).reachingTo(9, 80);

        assertEquals("Spruce Log                  1,020\nminecraft:oak_log stored    3,322", past.textOf(GLASS));
    }

    @Test
    void wordAt_takesTheWholeWordPunctuationAndAll() {
        final TermSelection word = TermSelection.wordAt(GLASS, 3, 4);

        assertEquals("minecraft:oak_log", word.textOf(GLASS));
    }

    @Test
    void wordAt_takesTheRunOfSpacesWhenTheClickIsOnOne() {
        final TermSelection gap = TermSelection.wordAt(GLASS, 1, 10);

        assertEquals("", gap.textOf(GLASS), "a run of spaces copies as nothing, since a row is cut at what it says");
        assertEquals(7, gap.startColumn());
        assertEquals(28, gap.endColumn());
    }

    @Test
    void wordAt_takesNothingPastTheEndOfARow() {
        assertTrue(TermSelection.wordAt(GLASS, 1, 400).isEmpty());
        assertTrue(TermSelection.wordAt(GLASS, 9, 1).isEmpty());
    }

    @Test
    void lineAt_takesTheWholeRow() {
        assertEquals("Oak Log                     3,322", TermSelection.lineAt(GLASS, 1).textOf(GLASS));
        assertTrue(TermSelection.lineAt(GLASS, 9).isEmpty());
    }
}
