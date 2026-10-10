/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.tier.HardwareEra;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BusSettingsTest {

    @Test
    void constructor_padsPerSlotListsThatAreShorterThanTheFilter() {
        final BusSettings settings = settingsOf(5, List.of(3, 4), List.of(7));

        assertEquals(List.of(3, 4, 0, 0, 0), settings.itemKeep());
        assertEquals(List.of(7, 0, 0, 0, 0), settings.itemMax());
    }

    @Test
    void constructor_cutsPerSlotListsThatAreLongerThanTheFilter() {
        final BusSettings settings = settingsOf(2, List.of(1, 2, 3, 4), List.of(5, 6, 7));

        assertEquals(List.of(1, 2), settings.itemKeep());
        assertEquals(List.of(5, 6), settings.itemMax());
    }

    private static BusSettings settingsOf(final int slots, final List<Integer> itemKeep, final List<Integer> itemMax) {
        return new BusSettings("", HardwareEra.VINTAGE, Collections.nCopies(slots, ""), false, 0, 0,
                itemKeep, itemMax, 0, List.of(), List.of(), false, true, false, Map.of());
    }
}
