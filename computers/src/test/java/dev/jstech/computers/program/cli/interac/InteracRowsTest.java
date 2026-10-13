/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteracRowsTest {

    @Test
    void matches_trimsTheSearchAndIgnoresCase() {
        assertTrue(InteracRows.matches("Oak Log", "  oAK "));
        assertFalse(InteracRows.matches("Oak Log", "birch"));
    }

    @Test
    void matches_keepsEverythingForAnEmptySearch() {
        assertTrue(InteracRows.matches("Oak Log", ""));
        assertTrue(InteracRows.matches("Oak Log", "   "));
        assertTrue(InteracRows.matches("Oak Log", null));
    }
}
