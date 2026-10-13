/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EngineVersionsTest {

    @Test
    void compare_readsADottedVersionAndAYearAsNumbers() {
        assertTrue(EngineVersions.compare("4.2", "2000") < 0);
        assertTrue(EngineVersions.compare("2008", "2000") > 0);
        assertTrue(EngineVersions.compare("2012", "2022") < 0);
        assertTrue(EngineVersions.compare("1.10", "1.9") > 0);
    }

    @Test
    void compare_countsAMissingPartAsNothing() {
        assertEquals(0, EngineVersions.compare("2.0", "2"));
        assertTrue(EngineVersions.compare("2.0.1", "2") > 0);
    }

    @Test
    void atLeast_isTheVersionOrNewer() {
        assertTrue(EngineVersions.atLeast("2012", "2000"));
        assertTrue(EngineVersions.atLeast("2000", "2000"));
        assertFalse(EngineVersions.atLeast("4.2", "2000"));
        assertFalse(EngineVersions.atLeast("", "2000"));
    }
}
