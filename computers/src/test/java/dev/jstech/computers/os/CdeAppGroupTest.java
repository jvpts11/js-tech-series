/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CdeAppGroupTest {

    @Test
    void of_putsWhatADeskIsUsedForEveryDayAmongTheDesktopApps() {
        for (final String path : new String[] {"files", "editor", "command_prompt", "calculator", "settings"}) {
            assertEquals(CdeAppGroup.DESKTOP_APPS, CdeAppGroup.of(path), path);
        }
    }

    @Test
    void of_putsWhatSpeaksToTheNetworkUnderNetwork() {
        for (final String path : new String[] {"network", "network_manager", "nms", "gateway_manager",
            "remote_control", "storage_insights", "crafting_manager"}) {
            assertEquals(CdeAppGroup.NETWORK, CdeAppGroup.of(path), path);
        }
    }

    @Test
    void of_putsAGameUnderGames() {
        assertEquals(CdeAppGroup.GAMES, CdeAppGroup.of("minesweeper"));
    }

    @Test
    void of_callsEverythingElseATool() {
        assertEquals(CdeAppGroup.DESKTOP_TOOLS, CdeAppGroup.of("system_monitor"));
        assertEquals(CdeAppGroup.DESKTOP_TOOLS, CdeAppGroup.of("virtual_studio"));
        assertEquals(CdeAppGroup.DESKTOP_TOOLS, CdeAppGroup.of("sigma_somebody_elses_program"));
    }

    @Test
    void labelled_findsAGroupByWhatItIsCalledAndNothingElse() {
        for (final CdeAppGroup group : CdeAppGroup.values()) {
            assertEquals(group, CdeAppGroup.labelled(group.label()));
        }
        assertNull(CdeAppGroup.labelled("desktop_apps"));
        assertNull(CdeAppGroup.labelled(""));
    }

    @Test
    void place_isDifferentForEveryGroupAndInsideTheirNumber() {
        final Set<Integer> seen = new HashSet<>();
        for (final CdeAppGroup group : CdeAppGroup.values()) {
            assertTrue(group.place() >= 0 && group.place() < CdeAppGroup.values().length);
            assertTrue(seen.add(group.place()), group + " shares its place");
        }
    }
}
