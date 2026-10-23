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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GhsPictogramsTest {

    private static final Set<HazardClass> CHLORINE = EnumSet.of(HazardClass.OXIDISING, HazardClass.ACUTELY_TOXIC,
            HazardClass.HARMFUL, HazardClass.ENVIRONMENTAL);

    @Test
    void of_showsEachClassesPictogramInTheOrderOfTheirCodes() {
        assertEquals(List.of(GhsPictogram.FLAME, GhsPictogram.EXCLAMATION_MARK, GhsPictogram.HEALTH_HAZARD,
                GhsPictogram.ENVIRONMENT), GhsPictograms.of(EnumSet.of(HazardClass.ENVIRONMENTAL,
                HazardClass.FLAMMABLE, HazardClass.HEALTH_HAZARD, HazardClass.HARMFUL), false));
    }

    @Test
    void of_dropsTheExclamationMarkForTheSkull() {
        assertEquals(List.of(GhsPictogram.FLAME_OVER_CIRCLE, GhsPictogram.SKULL, GhsPictogram.ENVIRONMENT),
                GhsPictograms.of(CHLORINE, false));
    }

    @Test
    void of_addsTheGasCylinderOnlyForAGasUnderPressure() {
        assertEquals(List.of(GhsPictogram.FLAME_OVER_CIRCLE, GhsPictogram.GAS_CYLINDER, GhsPictogram.SKULL,
                GhsPictogram.ENVIRONMENT), GhsPictograms.of(CHLORINE, true));
        assertEquals(List.of(GhsPictogram.GAS_CYLINDER), GhsPictograms.of(EnumSet.of(HazardClass.ASPHYXIANT), true));
    }

    @Test
    void of_showsNoPictogramForTheHazardsBeyondTheGhs() {
        final Set<HazardClass> radon = EnumSet.of(HazardClass.ASPHYXIANT, HazardClass.RADIOACTIVE);
        assertTrue(GhsPictograms.of(radon, false).isEmpty());
        assertEquals(List.of(HazardClass.ASPHYXIANT, HazardClass.RADIOACTIVE), GhsPictograms.beyond(radon));
    }

    @Test
    void pictograms_haveTheNineCodesOfTheGhs() {
        final StringBuilder codes = new StringBuilder();
        for (final GhsPictogram pictogram : GhsPictogram.values()) {
            codes.append(pictogram.code()).append(' ');
        }
        assertEquals("GHS01 GHS02 GHS03 GHS04 GHS05 GHS06 GHS07 GHS08 GHS09", codes.toString().strip());
    }

    @Test
    void byName_readsWhatADataPackWrites() {
        assertEquals(HazardClass.ACUTELY_TOXIC, HazardClass.byName("acutely_toxic").orElseThrow());
        assertTrue(HazardClass.byName("spicy").isEmpty());
    }
}
