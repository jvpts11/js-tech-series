/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.jstech.core.tier.IndustrialTier;
import org.junit.jupiter.api.Test;

class PhiCoprocessorSpecTest {

    @Test
    void constructor_refusesNoCores() {
        assertThrows(IllegalArgumentException.class, () -> phi(0, 1050, 225));
    }

    @Test
    void constructor_refusesANonPositiveClock() {
        assertThrows(IllegalArgumentException.class, () -> phi(60, -5, 225));
    }

    @Test
    void constructor_refusesANegativeDraw() {
        assertThrows(IllegalArgumentException.class, () -> phi(60, 1050, -1));
    }

    @Test
    void craftsForSlot_doublesWithEachSlot() {
        assertEquals(8L, PhiCoprocessorSpec.craftsForSlot(0));
        assertEquals(16L, PhiCoprocessorSpec.craftsForSlot(1));
        assertEquals(256L, PhiCoprocessorSpec.craftsForSlot(PhiCoprocessorSpec.SLOT_COUNT - 1));
    }

    @Test
    void craftsForSlot_refusesASlotOutsideTheSix() {
        assertThrows(IllegalArgumentException.class, () -> PhiCoprocessorSpec.craftsForSlot(-1));
        assertThrows(IllegalArgumentException.class,
                () -> PhiCoprocessorSpec.craftsForSlot(PhiCoprocessorSpec.SLOT_COUNT));
    }

    private static PhiCoprocessorSpec phi(final int cores, final int mhz, final int tdpWatts) {
        return new PhiCoprocessorSpec(IndustrialTier.T3, 2, cores, mhz, tdpWatts);
    }
}
