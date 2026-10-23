/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OreLevelTest {

    @Test
    void levels_giveAnIngotThenTwoDusts() {
        assertEquals(1, OreLevel.FURNACE.mainOut());
        assertEquals(2, OreLevel.MACERATOR.mainOut());
    }

    @Test
    void byproduct_isNothingInAFurnaceALittleInTheMaceratorAndAllByTheRoute() {
        assertEquals(0.0, OreLevel.FURNACE.byproduct(0.3, 0.3, 1));
        assertEquals(0.5, OreLevel.MACERATOR.byproduct(0.3, 0.3, 2), 1e-12);
        assertEquals(2.0, OreLevel.FAMILY_ROUTE.byproduct(0.3, 0.3, 2), 1e-12);
    }

    @Test
    void byproduct_followsTheMineralsOwnShares() {
        // Gold at a hundredth of a chalcopyrite whose copper is 0.29 of it: a share of a dust each grinding.
        assertEquals(2 * 0.01 / 0.29 * 0.25, OreLevel.MACERATOR.byproduct(0.29, 0.01, 2), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> OreLevel.MACERATOR.byproduct(0.0, 0.1, 2));
    }
}
