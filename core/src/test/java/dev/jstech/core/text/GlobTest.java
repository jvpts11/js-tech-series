/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class GlobTest {

    @Test
    void matches_aStarStandsForAnyRunNoneIncluded() {
        assertTrue(Glob.matches("*.txt", "notes.txt", true));
        assertTrue(Glob.matches("notes*", "notes", true));
        assertTrue(Glob.matches("*", "", true));
        assertFalse(Glob.matches("*.txt", "notes.sgs", true));
    }

    @Test
    void matches_aQuestionMarkStandsForOneCharacterWhereItIsAWildcard() {
        assertTrue(Glob.matches("file?.txt", "file1.txt", true));
        assertFalse(Glob.matches("file?.txt", "file.txt", true));
        assertFalse(Glob.matches("file?.txt", "file1.txt", false), "a literal question mark matches only itself");
        assertTrue(Glob.matches("what?", "what?", false));
    }

    @Test
    void matches_regardlessOfCase() {
        assertTrue(Glob.matches("*.TXT", "Notes.txt", true));
    }

    @Test
    void matches_needsTheWholeNameToAnswer() {
        assertFalse(Glob.matches("a*b", "a-b-c", true));
        assertTrue(Glob.matches("a*b*c", "a-b-c", true));
    }

    @Test
    void matches_answersQuicklyAPatternOfManyStarsThatFails() {
        // Tried every way its stars could split, this pattern against this name takes longer than a game lasts.
        final String pattern = "*a".repeat(40) + "b";
        final String name = "a".repeat(200);
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertFalse(Glob.matches(pattern, name, true)));
    }

    @Test
    void isPattern_seesAStarAndAQuestionMarkWhereItStandsForOne() {
        assertTrue(Glob.isPattern("*.txt", false));
        assertTrue(Glob.isPattern("a?", true));
        assertFalse(Glob.isPattern("a?", false));
        assertFalse(Glob.isPattern("plain", true));
    }
}
