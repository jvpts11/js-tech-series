/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.chemistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReactionsTest {

    @Test
    void ammonia_balances() {
        assertTrue(Reactions.AMMONIA.balanced());
    }

    @Test
    void haberBosch_runsAtItsRealTemperatureAndPressure() {
        assertEquals(673.15, Reactions.HABER_BOSCH.leastKelvin());
        assertEquals(773.15, Reactions.HABER_BOSCH.mostKelvin());
        assertEquals(15_000.0, Reactions.HABER_BOSCH.leastKpa());
        assertEquals(25_000.0, Reactions.HABER_BOSCH.mostKpa());
        assertEquals("jscore:iron", Reactions.HABER_BOSCH.catalyst());
    }

    @Test
    void conditions_refuseAHottestBelowTheCoolest() {
        assertThrows(IllegalArgumentException.class, () -> new Reactions.Conditions(500.0, 400.0, 1.0, 2.0,
                "jscore:iron"));
    }
}
