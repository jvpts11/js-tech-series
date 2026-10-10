/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class CraftingErasTest {

    @Test
    void romPerCard_neverExceedsTheWireCap() {
        for (final HardwareEra era : HardwareEra.values()) {
            assertTrue(CraftingEras.romPerCard(era) <= CraftingEras.MAX_ROM_PER_CARD, era.name());
        }
    }
}
