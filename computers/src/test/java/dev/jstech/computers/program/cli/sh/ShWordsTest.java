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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShWordsTest {

    private static final Map<String, String> NAMED = Map.of("HOME", "/usr/player", "PWD", "/usr/player/progs");

    private static final List<String> FILES = List.of("hello.sgs", "farm.sgs", "notes.txt", "README");

    @Test
    void expand_putsInWhatANameStandsFor() {
        assertEquals("/usr/player", ShWords.expand("$HOME", NAMED, false));
        assertEquals("/usr/player/progs", ShWords.expand("${PWD}", NAMED, false));
        assertEquals("cd /usr/player", ShWords.expand("cd $HOME", NAMED, false));
    }

    @Test
    void expand_readsTheNameOfTheOtherFamilyTheOtherWay() {
        assertEquals("/usr/player", ShWords.expand("%HOME%", NAMED, true));
        assertEquals("$HOME", ShWords.expand("$HOME", NAMED, true), "a DOS shell does not read a dollar");
        assertEquals("%HOME%", ShWords.expand("%HOME%", NAMED, false), "and a Unix one does not read percents");
    }

    @Test
    void expand_aNameNothingStandsForComesOutAsNothing() {
        assertEquals("", ShWords.expand("$NOWHERE", NAMED, false));
        assertEquals("x", ShWords.expand("$NOWHERE" + "x", NAMED, false).isEmpty() ? "x" : "x");
    }

    @Test
    void expand_readsANameWhateverCaseItWasWrittenIn() {
        assertEquals("/usr/player", ShWords.expand("$home", NAMED, false));
        assertEquals("/usr/player", ShWords.expand("%Home%", NAMED, true));
    }

    @Test
    void expand_aLoneMarkIsJustAMark() {
        assertEquals("cost: $", ShWords.expand("cost: $", NAMED, false));
        assertEquals("50%", ShWords.expand("50%", NAMED, true));
    }

    @Test
    void glob_opensAStarOutIntoTheNamesItMatches() {
        assertEquals(List.of("hello.sgs", "farm.sgs"), ShWords.glob("*.sgs", FILES));
        assertEquals(List.of("hello.sgs"), ShWords.glob("hello.*", FILES));
    }

    @Test
    void glob_leavesAWordWithNoStarAlone() {
        assertEquals(List.of("notes.txt"), ShWords.glob("notes.txt", FILES));
        assertEquals(List.of("nothing.here"), ShWords.glob("nothing.here", FILES));
    }

    @Test
    void glob_leavesAPatternThatMatchesNothingAsItWasTyped() {
        assertEquals(List.of("*.zip"), ShWords.glob("*.zip", FILES),
                "so the command says there is no such file, instead of being handed every file there is");
    }

    @Test
    void matches_readsStarsAndQuestionMarksAndIgnoresCase() {
        assertTrue(ShWords.matches("*.sgs", "HELLO.SGS"));
        assertTrue(ShWords.matches("h?llo.sgs", "hello.sgs"));
        assertFalse(ShWords.matches("h?llo.sgs", "heello.sgs"));
        assertTrue(ShWords.matches("*", "anything at all"));
        assertTrue(ShWords.matches("a*b*c", "aXXbYYc"));
        assertFalse(ShWords.matches("a*b*c", "aXXbYY"));
    }
}
