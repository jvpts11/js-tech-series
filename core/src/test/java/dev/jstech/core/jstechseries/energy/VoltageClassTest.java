/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class VoltageClassTest {

    @Test
    void joinedTo_neighbouringAlternatingClassesNeedATransformer() {
        assertEquals(Optional.of(GridEquipment.TRANSFORMER), VoltageClass.LOW.joinedTo(VoltageClass.MEDIUM));
        assertEquals(Optional.of(GridEquipment.TRANSFORMER), VoltageClass.HIGH.joinedTo(VoltageClass.MEDIUM));
        assertEquals(Optional.of(GridEquipment.TRANSFORMER), VoltageClass.HIGH.joinedTo(VoltageClass.EXTRA_HIGH));
    }

    @Test
    void joinedTo_theDirectCurrentClassNeedsAConverterStation() {
        assertEquals(Optional.of(GridEquipment.HVDC_STATION),
                VoltageClass.EXTRA_HIGH.joinedTo(VoltageClass.ULTRA_HIGH));
        assertEquals(Optional.of(GridEquipment.HVDC_STATION),
                VoltageClass.ULTRA_HIGH.joinedTo(VoltageClass.EXTRA_HIGH));
    }

    @Test
    void joinedTo_nothingJoinsClassesFurtherApartOrAClassToItself() {
        assertTrue(VoltageClass.LOW.joinedTo(VoltageClass.HIGH).isEmpty());
        assertTrue(VoltageClass.MEDIUM.joinedTo(VoltageClass.ULTRA_HIGH).isEmpty());
        assertTrue(VoltageClass.LOW.joinedTo(VoltageClass.LOW).isEmpty());
    }

    @Test
    void ultraHigh_isTheOnlyDirectCurrentClass() {
        for (final VoltageClass each : VoltageClass.values()) {
            assertEquals(each == VoltageClass.ULTRA_HIGH, each.directCurrent());
        }
        assertEquals(400L, VoltageClass.LOW.volts());
        assertEquals(1_100_000L, VoltageClass.ULTRA_HIGH.volts());
    }
}
