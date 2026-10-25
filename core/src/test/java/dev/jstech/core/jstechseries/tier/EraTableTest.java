/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EraTableTest {

    @Test
    void get_givesAnEraItsOwnValue() {
        final EraTable<Integer> table = EraTable.of(1).from(HardwareEra.LEGACY, 2).from(HardwareEra.STANDARD, 4);
        assertEquals(1, table.get(HardwareEra.VINTAGE));
        assertEquals(2, table.get(HardwareEra.LEGACY));
        assertEquals(4, table.get(HardwareEra.STANDARD));
    }

    @Test
    void get_givesAnEraWithoutItsOwnTheValueOfTheNearestEraBefore() {
        final EraTable<Integer> table = EraTable.of(1).from(HardwareEra.LEGACY, 2).from(HardwareEra.STANDARD, 4);
        assertEquals(2, table.get(HardwareEra.TRANSITION));
        assertEquals(4, table.get(HardwareEra.SINGULARITY));
    }

    @Test
    void ofFive_givesTheErasAfterTheAdvancedTheAdvanceds() {
        final EraTable<String> table = EraTable.ofFive("v", "l", "t", "s", "a");
        assertEquals("t", table.get(HardwareEra.TRANSITION));
        assertEquals("a", table.get(HardwareEra.EXA));
        assertEquals("a", table.get(HardwareEra.SINGULARITY));
    }

    @Test
    void from_leavesTheTableItWasCalledOnAsItWas() {
        final EraTable<Integer> before = EraTable.of(1);
        final EraTable<Integer> after = before.from(HardwareEra.ADVANCED, 9);
        assertEquals(1, before.get(HardwareEra.ADVANCED));
        assertEquals(9, after.get(HardwareEra.ADVANCED));
    }

    @Test
    void get_keepsANullAsAValue() {
        final EraTable<String> table = EraTable.<String>of(null).from(HardwareEra.LEGACY, "tray");
        assertNull(table.get(HardwareEra.VINTAGE));
        assertEquals("tray", table.get(HardwareEra.EXA));
    }

    @Test
    void hasOwn_tellsAGivenValueFromOneTaken() {
        final EraTable<Integer> table = EraTable.of(1).from(HardwareEra.STANDARD, 4);
        assertTrue(table.hasOwn(HardwareEra.STANDARD));
        assertFalse(table.hasOwn(HardwareEra.ADVANCED));
    }
}
