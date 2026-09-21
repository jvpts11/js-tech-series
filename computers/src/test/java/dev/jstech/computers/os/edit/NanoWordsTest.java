/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What nano says, which is only worth having if it is what the real one says. */
class NanoWordsTest {

    @Test
    void wrote_countsLinesTheWayItIsSaid() {
        assertEquals("[ Wrote 1 line ]", NanoWords.wrote(1));
        assertEquals("[ Wrote 12 lines ]", NanoWords.wrote(12));
        assertEquals("[ Read 0 lines ]", NanoWords.read(0));
        assertEquals("[ Replaced 1 occurrence ]", NanoWords.replaced(1));
        assertEquals("[ Replaced 3 occurrences ]", NanoWords.replaced(3));
    }

    @Test
    void notFound_quotesWhatWasLookedFor() {
        assertEquals("[ \"noatime\" not found ]", NanoWords.notFound("noatime"));
        assertEquals("[ File \"make.conf\" not found ]", NanoWords.noSuchFile("make.conf"));
    }

    @Test
    void searching_offersTheLastThingSearchedForAgain() {
        assertEquals("Search: UU", NanoWords.searching(NanoWords.SEARCH, "", "UU"));
        assertEquals("Search [UUID]: ", NanoWords.searching(NanoWords.SEARCH, "UUID", ""));
        assertEquals("Search (to replace) [-j1]: -j",
                NanoWords.searching(NanoWords.SEARCH_TO_REPLACE, "-j1", "-j"));
    }

    @Test
    void position_saysHowFarThroughEachOfTheThreeIs() {
        assertEquals("[ line 3/12 (25%), col 1/20 (5%), char 40/300 (13%) ]",
                NanoWords.position(3, 12, 1, 20, 40, 300));
        assertEquals("[ line 1/1 (100%), col 1/1 (100%), char 1/1 (100%) ]",
                NanoWords.position(1, 1, 1, 1, 1, 1));
    }

    @Test
    void shown_isWhatWasTypedWithoutTheMarkInFrontOfIt() {
        assertEquals("/etc/fstab", NanoWords.shown("live:/etc/fstab"));
        assertEquals("make.conf", NanoWords.shown("live:make.conf"));
        assertEquals("progs/hello.sgs", NanoWords.shown("progs/hello.sgs"));
        assertEquals("/odd/na:me", NanoWords.shown("/odd/na:me"), "a colon inside a name is part of the name");
    }

    @Test
    void theRowsOfKeys_areAlwaysTwoRowsOfFour() {
        for (final List<List<TtyLook.Key>> rows : List.of(NanoWords.EDITING, NanoWords.TYPING,
                NanoWords.YES_OR_NO, NanoWords.EACH_OR_ALL, NanoWords.READING_HELP)) {
            assertEquals(2, rows.size());
            for (final List<TtyLook.Key> row : rows) {
                assertEquals(4, row.size(), "the columns line up from one question to the next");
            }
        }
    }

    @Test
    void theHelpText_listsEveryKeyTheTwoRowsList() {
        final String help = String.join("\n", NanoWords.HELP);
        for (final List<TtyLook.Key> row : NanoWords.EDITING) {
            for (final TtyLook.Key key : row) {
                assertTrue(help.contains(key.chord() + " "), key.chord() + " is listed and not explained");
                assertFalse(key.does().isEmpty());
            }
        }
    }
}
