/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class EraMemoryTest {

    @Test
    void scaled_growsByTheRatioOfEachErasTypicalMemory() {
        assertEquals(3, EraMemory.scaled(48, HardwareEra.VINTAGE));
        assertEquals(48, EraMemory.scaled(48, HardwareEra.LEGACY));
        assertEquals(384, EraMemory.scaled(48, HardwareEra.TRANSITION));
        assertEquals(1_536, EraMemory.scaled(48, HardwareEra.STANDARD));
        assertEquals(6_144, EraMemory.scaled(48, HardwareEra.ADVANCED));
    }

    @Test
    void scaled_neverHoldsLessThanAMegabyteNorAnythingForNothing() {
        assertEquals(1, EraMemory.scaled(1, HardwareEra.VINTAGE));
        assertEquals(0, EraMemory.scaled(0, HardwareEra.ADVANCED));
    }

    @Test
    void scaled_takesAMachineOfNoKnownEraForALegacyOne() {
        assertEquals(16, EraMemory.scaled(16, null));
    }

    @Test
    void scaled_fitsTheTargetOfAboutEightProgramsOnTheTypicalMachine() {
        // Frames XP on two 256 MB sticks: the system's 128 MB and eight average programs fill it, a ninth does not fit.
        final int typical = 512;
        final int system = 128;
        final int average = EraMemory.scaled(48, HardwareEra.LEGACY);
        assertEquals(typical, system + 8 * average);
    }
}
