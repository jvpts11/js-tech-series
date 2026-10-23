/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.formula;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EnergyFormulasTest {

    @Test
    void pumpedStorage_holdsWhatTheDesignSays() {
        assertEquals(490.5e6, EnergyFormulas.pumpedStorage(1_000.0, 50.0), 1e-3);
        assertEquals(0.0, EnergyFormulas.pumpedStorage(1_000.0, -5.0));
    }

    @Test
    void evaporativeCooling_carriesAwayTheWatersHeat() {
        assertEquals(2.26e6, EnergyFormulas.evaporativeCooling(1.0));
        assertEquals(3.6e6, EnergyFormulas.KILOWATT_HOUR);
        assertEquals(3.6e9, EnergyFormulas.MEGAWATT_HOUR);
    }
}
