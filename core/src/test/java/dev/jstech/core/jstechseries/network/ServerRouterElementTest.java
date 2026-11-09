/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerRouterElementTest {

    @Test
    void maxRacksFor_followsEraBudget() {
        assertEquals(4, ServerRouterElement.maxRacksFor(HardwareEra.LEGACY));
        assertEquals(8, ServerRouterElement.maxRacksFor(HardwareEra.TRANSITION));
        assertEquals(16, ServerRouterElement.maxRacksFor(HardwareEra.STANDARD));
        assertEquals(32, ServerRouterElement.maxRacksFor(HardwareEra.ADVANCED));
    }

    @Test
    void maxRacksFor_vintage_isZero() {
        assertEquals(0, ServerRouterElement.maxRacksFor(HardwareEra.VINTAGE));
    }

    @Test
    void maxRacksFor_pastTheAdvanced_keepsTheAdvancedBudget() {
        assertEquals(32, ServerRouterElement.maxRacksFor(HardwareEra.EXA));
        assertEquals(32, ServerRouterElement.maxRacksFor(HardwareEra.SINGULARITY));
    }

    @Test
    void maxRacks_matchesEra() {
        final ServerRouterElement element =
                new ServerRouterElement(NetworkUuid.random(), 5L, HardwareEra.TRANSITION);
        assertEquals(8, element.maxRacks());
    }

    @Test
    void constructor_rejectsNullNetwork() {
        assertThrows(NullPointerException.class,
                () -> new ServerRouterElement(null, 0L, HardwareEra.STANDARD));
    }

    @Test
    void constructor_rejectsNullEra() {
        assertThrows(NullPointerException.class,
                () -> new ServerRouterElement(NetworkUuid.random(), 0L, null));
    }
}
