/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class WildcardsTest {

    @Test
    void matches_readsStarDotStarAsEveryName() {
        assertTrue(Wildcards.matches("*.*", "AUTOEXEC.BAT"));
        assertTrue(Wildcards.matches("*.*", "DOS"));
    }

    @Test
    void matches_takesAStarForAnyRunAndAQuestionMarkForOne() {
        assertTrue(Wildcards.matches("*.TXT", "readme.txt"));
        assertFalse(Wildcards.matches("*.TXT", "README.BAT"));
        assertTrue(Wildcards.matches("CONFIG.SY?", "CONFIG.SYS"));
        assertFalse(Wildcards.matches("CONFIG.SY?", "CONFIG.SY"));
        assertTrue(Wildcards.matches("A*B", "AxxB"));
    }

    @Test
    void matches_letsNameStarTakeANameWithNoExtension() {
        assertTrue(Wildcards.matches("FOO.*", "FOO"));
        assertTrue(Wildcards.matches("FOO.*", "FOO.TXT"));
        assertTrue(Wildcards.matches("F*.*", "foo"));
        assertFalse(Wildcards.matches("FOO.*", "FOOD"));
        assertFalse(Wildcards.matches("FOO.*", "BAR"));
    }

    @Test
    void matches_wantsTheWholeName() {
        assertFalse(Wildcards.matches("READ", "README.TXT"));
        assertTrue(Wildcards.matches("readme.txt", "README.TXT"));
    }

    @Test
    void matches_answersQuicklyAPatternOfManyStarsThatFails() {
        final String pattern = "*A".repeat(40) + "B";
        assertTimeoutPreemptively(Duration.ofSeconds(1),
                () -> assertFalse(Wildcards.matches(pattern, "A".repeat(64))));
    }
}
