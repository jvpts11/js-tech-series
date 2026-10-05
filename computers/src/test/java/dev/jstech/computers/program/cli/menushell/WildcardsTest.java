/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void matches_wantsTheWholeName() {
        assertFalse(Wildcards.matches("READ", "README.TXT"));
        assertTrue(Wildcards.matches("readme.txt", "README.TXT"));
    }
}
