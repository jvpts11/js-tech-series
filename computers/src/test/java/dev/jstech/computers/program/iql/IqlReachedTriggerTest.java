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

class IqlReachedTriggerTest {

    @Test
    void parse_readsAnEraAndATier() {
        assertEquals(new IqlReachedTrigger(IqlReachedTrigger.Axis.ERA, "standard"),
                IqlReachedTrigger.parse("ERA standard REACHED"));
        assertEquals(new IqlReachedTrigger(IqlReachedTrigger.Axis.TIER, "t3"),
                IqlReachedTrigger.parse("TIER t3 REACHED"));
    }

    @Test
    void parse_ignoresCaseAndSpaces() {
        assertEquals(new IqlReachedTrigger(IqlReachedTrigger.Axis.ERA, "jscore:legacy"),
                IqlReachedTrigger.parse("  era   JSCore:Legacy   reached "));
    }

    @Test
    void parse_refusesWhatIsNoStepReached() {
        assertNull(IqlReachedTrigger.parse("ERA standard"));
        assertNull(IqlReachedTrigger.parse("ERA standard COMING"));
        assertNull(IqlReachedTrigger.parse("AGE standard REACHED"));
        assertNull(IqlReachedTrigger.parse("EVENT mymod:quake COMING"));
        assertNull(IqlReachedTrigger.parse("qty(iron_ingot) < 64"));
    }

    @Test
    void names_matchesTheStepByItsIdOrItsPath() {
        final IqlReachedTrigger byPath = IqlReachedTrigger.parse("ERA standard REACHED");
        assertTrue(byPath.names("jscore:hardware_era", "jscore:standard"));
        assertFalse(byPath.names("jscore:hardware_era", "jscore:advanced"));
        assertFalse(byPath.names("jscore:industrial_tier", "jscore:standard"));
        final IqlReachedTrigger byId = IqlReachedTrigger.parse("ERA mymod:quantum REACHED");
        assertTrue(byId.names("jscore:hardware_era", "mymod:quantum"));
        assertFalse(byId.names("jscore:hardware_era", "othermod:quantum"));
    }

    @Test
    void names_matchesATierWithNoNamespace() {
        final IqlReachedTrigger tier = IqlReachedTrigger.parse("TIER T3 REACHED");
        assertTrue(tier.names("jscore:industrial_tier", "t3"));
        assertFalse(tier.names("jscore:industrial_tier", "t4"));
        assertFalse(tier.names("jscore:hardware_era", "t3"));
    }
}
