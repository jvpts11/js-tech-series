/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FamilyRouteTest {

    @Test
    void smeltsRaw_onlyTheSimpleMinerals() {
        assertTrue(FamilyRoute.smeltsRaw("native"));
        assertTrue(FamilyRoute.smeltsRaw("oxide"));
        assertTrue(FamilyRoute.smeltsRaw("carbonate"));
        assertFalse(FamilyRoute.smeltsRaw("sulfide"));
        assertFalse(FamilyRoute.smeltsRaw("arsenide"));
        assertFalse(FamilyRoute.smeltsRaw("silicate"));
    }

    @Test
    void smeltsRaw_noMineralOfAFamilyItDoesNotKnow() {
        assertFalse(FamilyRoute.smeltsRaw("some_other_mods_family"));
        assertTrue(FamilyRoute.of("some_other_mods_family").isEmpty());
    }

    @Test
    void families_areTheCoresEleven() {
        assertEquals(11, FamilyRoute.values().length);
        assertEquals(FamilyRoute.VANADATE, FamilyRoute.of("vanadate").orElseThrow());
    }
}
