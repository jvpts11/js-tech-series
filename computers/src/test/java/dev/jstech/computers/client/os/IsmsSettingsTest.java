/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsmsSettingsTest {

    @Test
    void read_givesEveryChoiceItsUsualValueForAnEmptyFile() {
        final IsmsSettings settings = IsmsSettings.read("");
        assertTrue(settings.askFirst);
        assertTrue(settings.stopOnError);
        assertEquals(IsmsSettings.Results.GRID, settings.results);
        assertTrue(settings.recent.isEmpty());
    }

    @Test
    void write_isReadBackTheSame() {
        final IsmsSettings settings = new IsmsSettings();
        settings.askFirst = false;
        settings.results = IsmsSettings.Results.TEXT;
        settings.opened("Documents/restock.iql");
        settings.opened("stock.iql");
        final IsmsSettings back = IsmsSettings.read(settings.write());
        assertFalse(back.askFirst);
        assertEquals(IsmsSettings.Results.TEXT, back.results);
        assertEquals(List.of("stock.iql", "Documents/restock.iql"), back.recent);
    }

    @Test
    void read_leavesAChoiceItCannotReadAsItComes() {
        final IsmsSettings settings = IsmsSettings.read("ask_first=maybe\nresults=sideways\nwhat=ever");
        assertTrue(settings.askFirst);
        assertEquals(IsmsSettings.Results.GRID, settings.results);
    }

    @Test
    void opened_keepsAFileOnceAndDropsTheOldest() {
        final IsmsSettings settings = new IsmsSettings();
        for (int i = 0; i < IsmsSettings.MOST_RECENT + 2; i++) {
            settings.opened("f" + i + ".iql");
        }
        settings.opened("f5.iql");
        assertEquals(IsmsSettings.MOST_RECENT, settings.recent.size());
        assertEquals("f5.iql", settings.recent.get(0));
        assertEquals(1, settings.recent.stream().filter("f5.iql"::equals).count());
    }

    @Test
    void next_stepsThroughTheThreeAndRoundAgain() {
        assertEquals(IsmsSettings.Results.TEXT, IsmsSettings.Results.GRID.next());
        assertEquals(IsmsSettings.Results.GRID, IsmsSettings.Results.FILE.next());
    }
}
