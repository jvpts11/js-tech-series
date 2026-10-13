/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ViDialectTest {

    @Test
    void opened_nviNamesTheFileBareWithNoLineCount() {
        assertEquals("notes.txt: unmodified: line 1", ViDialect.NVI.opened("notes.txt", 3, 72).english());
    }

    @Test
    void openedNew_nviNamesTheFileBareTooWideNothingExists() {
        assertEquals("notes.txt: new file: line 1", ViDialect.NVI.openedNew("notes.txt").english());
    }

    @Test
    void written_nviCountsTheLinesAndTheCharacters() {
        assertEquals("notes.txt: 3 lines, 72 characters", ViDialect.NVI.written("notes.txt", 3, 72).english());
    }

    @Test
    void opened_systemVQuotesTheNameAndCountsTheFile() {
        assertEquals("\"notes\" 3 lines, 72 characters", ViDialect.SYSTEM_V.opened("notes", 3, 72).english());
    }

    @Test
    void openedNew_systemVQuotesTheNameAndSaysItIsNew() {
        assertEquals("\"notes\" [New file]", ViDialect.SYSTEM_V.openedNew("notes").english());
    }

    @Test
    void written_systemVSaysTheSameAsOpeningAnExistingFile() {
        assertEquals(ViDialect.SYSTEM_V.opened("notes", 3, 72).english(),
                ViDialect.SYSTEM_V.written("notes", 3, 72).english());
    }

    @Test
    void opened_theTwoDialectsNeverAgreeOnTheSameFile() {
        final String nvi = ViDialect.NVI.opened("notes", 3, 72).english();
        final String systemV = ViDialect.SYSTEM_V.opened("notes", 3, 72).english();
        assertFalse(nvi.equals(systemV), "each system keeps its own voice: " + nvi + " vs " + systemV);
    }
}
