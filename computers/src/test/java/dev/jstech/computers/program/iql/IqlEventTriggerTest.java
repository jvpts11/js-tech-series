/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IqlEventTriggerTest {

    @Test
    void parse_readsEachStep() {
        assertEquals(new IqlEventTrigger("mymod:solar_storm", IqlEventTrigger.Stage.COMING),
                IqlEventTrigger.parse("EVENT mymod:solar_storm COMING"));
        assertEquals(new IqlEventTrigger("mymod:solar_storm", IqlEventTrigger.Stage.UNDER_WAY),
                IqlEventTrigger.parse("EVENT mymod:solar_storm UNDER WAY"));
        assertEquals(new IqlEventTrigger("mymod:solar_storm", IqlEventTrigger.Stage.ENDED),
                IqlEventTrigger.parse("EVENT mymod:solar_storm ENDED"));
    }

    @Test
    void parse_takesAnyCaseAndSpacing() {
        assertEquals(new IqlEventTrigger("mymod:quake", IqlEventTrigger.Stage.UNDER_WAY),
                IqlEventTrigger.parse("  event  MyMod:Quake   under   way "));
    }

    @Test
    void parse_leavesAnyOtherWhenToTheConditions() {
        assertNull(IqlEventTrigger.parse("qty(iron_ingot) < 64"));
        assertNull(IqlEventTrigger.parse("EVENT mymod:quake"));
        assertNull(IqlEventTrigger.parse("EVENT mymod:quake SOON"));
        assertNull(IqlEventTrigger.parse("TIME BETWEEN 6 AND 18"));
    }

    @Test
    void names_matchesTheIdOrItsPath() {
        assertTrue(IqlEventTrigger.parse("EVENT mymod:quake COMING").names("mymod:quake"));
        assertTrue(IqlEventTrigger.parse("EVENT quake COMING").names("mymod:quake"));
        assertFalse(IqlEventTrigger.parse("EVENT othermod:quake COMING").names("mymod:quake"));
        assertFalse(IqlEventTrigger.parse("EVENT quake COMING").names("mymod:earthquake"));
    }
}
