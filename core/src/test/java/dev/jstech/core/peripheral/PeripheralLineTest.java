/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class PeripheralLineTest {

    @Test
    void range_growsWithTheEra() {
        assertEquals(8, PeripheralLine.range(HardwareEra.VINTAGE));
        assertEquals(12, PeripheralLine.range(HardwareEra.LEGACY));
        assertEquals(14, PeripheralLine.range(HardwareEra.TRANSITION));
        assertEquals(16, PeripheralLine.range(HardwareEra.STANDARD));
        assertEquals(20, PeripheralLine.range(HardwareEra.ADVANCED));
    }

    @Test
    void range_afterTheAdvanced_isTheAdvanceds() {
        assertEquals(20, PeripheralLine.range(HardwareEra.EXA));
        assertEquals(20, PeripheralLine.range(HardwareEra.SINGULARITY));
    }
}
