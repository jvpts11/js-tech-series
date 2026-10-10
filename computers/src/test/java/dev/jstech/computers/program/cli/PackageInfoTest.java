/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PackageInfoTest {

    private static ICliComputer.PackageInfo editor() {
        return new ICliComputer.PackageInfo("Nano", "A small Text Editor", false);
    }

    @Test
    void matches_ignoresCaseInNameAndDescription() {
        assertTrue(editor().matches("nano"));
        assertTrue(editor().matches("text editor"));
    }

    @Test
    void matches_trimsTheTerm() {
        assertTrue(editor().matches("  nano  "));
    }

    @Test
    void matches_anEmptyTermFindsEverything() {
        assertTrue(editor().matches(""));
        assertTrue(editor().matches("   "));
    }

    @Test
    void matches_aStrangerTermFindsNothing() {
        assertFalse(editor().matches("compiler"));
    }
}
