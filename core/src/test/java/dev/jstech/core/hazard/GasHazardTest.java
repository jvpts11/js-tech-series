/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class GasHazardTest {

    @Test
    void of_readsACloudsHazardsOffTheGasesClasses() {
        assertEquals(EnumSet.of(GasHazard.OXIDISING, GasHazard.TOXIC), GasHazard.of(EnumSet.of(
                HazardClass.OXIDISING, HazardClass.ACUTELY_TOXIC, HazardClass.HARMFUL, HazardClass.ENVIRONMENTAL)));
        assertEquals(EnumSet.of(GasHazard.FLAMMABLE), GasHazard.of(EnumSet.of(HazardClass.FLAMMABLE)));
        assertEquals(EnumSet.of(GasHazard.ASPHYXIANT), GasHazard.of(EnumSet.of(HazardClass.ASPHYXIANT)));
    }

    @Test
    void of_givesNothingForHarmOnlyOverTime() {
        assertTrue(GasHazard.of(EnumSet.of(HazardClass.HEALTH_HAZARD, HazardClass.ENVIRONMENTAL)).isEmpty());
    }
}
