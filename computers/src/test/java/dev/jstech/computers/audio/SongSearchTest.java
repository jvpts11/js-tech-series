/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SongSearchTest {

    @Test
    void matches_findsEveryWordAnywhereInAnyCase() {
        final SongSearch search = new SongSearch("  tides  HARBOUR ");
        assertTrue(search.matches("Low Tides", "The Tin Radios", "Harbour Lights"));
        assertFalse(search.matches("Low Tides", "The Tin Radios", "Signal Fires"), "every word has to be there");
    }

    @Test
    void matches_findsEverythingWhenNothingWasTyped() {
        final SongSearch search = new SongSearch("   ");
        assertTrue(search.everything());
        assertTrue(search.matches("anything"));
    }

    @Test
    void new_keepsToTheLongestSearchAPlayerCanType() {
        final String typed = "a".repeat(SongSearch.MAX_QUERY) + " zzz";
        assertTrue(new SongSearch(typed).matches("a".repeat(SongSearch.MAX_QUERY)),
                "what goes past the longest search is not looked for");
    }
}
