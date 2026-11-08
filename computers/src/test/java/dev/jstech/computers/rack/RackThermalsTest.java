/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class RackThermalsTest {

    @Test
    void budgetWatts_addsWhatTheCoolingUnitsShed() {
        assertEquals(RackThermals.PASSIVE_BUDGET_W, RackThermals.budgetWatts(0));
        assertEquals(RackThermals.PASSIVE_BUDGET_W + 1_500, RackThermals.budgetWatts(1_500));
        assertEquals(RackThermals.PASSIVE_BUDGET_W + 300 + 2_500, RackThermals.budgetWatts(300 + 2_500));
    }

    @Test
    void budgetWatts_addsAVentPanelsShare() {
        assertEquals(1_000 + 1_500 + 500, RackThermals.budgetWatts(1_500 + RackThermals.VENT_PANEL_W));
    }

    @Test
    void budgetWatts_treatsANegativeSumAsNone() {
        assertEquals(RackThermals.PASSIVE_BUDGET_W, RackThermals.budgetWatts(-2));
    }

    @Test
    void coolingUnitWatts_growsWithEachEra() {
        assertEquals(300, RackThermals.coolingUnitWatts(HardwareEra.VINTAGE));
        assertEquals(600, RackThermals.coolingUnitWatts(HardwareEra.LEGACY));
        assertEquals(1_000, RackThermals.coolingUnitWatts(HardwareEra.TRANSITION));
        assertEquals(1_500, RackThermals.coolingUnitWatts(HardwareEra.STANDARD));
        assertEquals(2_500, RackThermals.coolingUnitWatts(HardwareEra.ADVANCED));
    }

    @Test
    void coolingUnitWatts_holdsTheAdvancedFigurePastIt() {
        assertEquals(2_500, RackThermals.coolingUnitWatts(HardwareEra.SINGULARITY));
    }
}
