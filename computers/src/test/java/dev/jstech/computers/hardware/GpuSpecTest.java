/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuSpecTest {

    @Test
    void power_isCoresTimesClockTimesTheDesignsWork() {
        // 2,880 cores at 875 MHz of a design worth 1.0 per core per gigahertz.
        assertEquals(2_520L, card(2880, Microarchitectures.KEPLER, "GK110", 875).power());
    }

    @Test
    void power_countsTheShadersOfAFiveWideDesignAGroupAtATime() {
        // 960 shaders in 192 groups of five, at 775 MHz of a design worth 5.0 a group.
        assertEquals(744L, card(960, Microarchitectures.TERASCALE_2, "Barts", 775).power());
    }

    @Test
    void power_countsAHotClockedDesignAtItsShadersClock() {
        assertEquals(403L, card(112, Microarchitectures.TESLA, "G92", 1500).power());
        assertEquals(622L, card(192, Microarchitectures.FERMI, "GF116", 1800).power());
    }

    @Test
    void power_neverFallsBelowOne() {
        // A VGA card works out to a quarter of an item a tick; one that lights a screen does some work.
        assertEquals(1L, card(1, Microarchitectures.VGA, "", 25).power());
    }

    @Test
    void power_roundsAHalfUp() {
        // One core at 25 MHz of a design worth 60: 1.5 exactly.
        assertEquals(2L, card(1, Microarchitectures.RENDITION, "V1000", 25).power());
    }

    @Test
    void power_climbsWithTheGenerations() {
        final long[] ladder = {
                card(3, Microarchitectures.THREEDFX, "", 90).power(),
                card(8, Microarchitectures.R300, "R350", 380).power(),
                card(240, Microarchitectures.TESLA, "GT200", 1296).power(),
                card(2048, Microarchitectures.GCN, "Tahiti", 925).power(),
                card(2880, Microarchitectures.KEPLER, "GK110", 875).power()};
        for (int i = 1; i < ladder.length; i++) {
            assertTrue(ladder[i - 1] < ladder[i], "card " + i + " should outrun the one before it");
        }
    }

    @Test
    void designLabel_namesTheDesignAndTheChip() {
        assertEquals("Kepler GK110", card(2880, Microarchitectures.KEPLER, "GK110", 875).designLabel());
        assertEquals("V1000", card(1, Microarchitectures.RENDITION, "V1000", 25).designLabel());
        assertEquals("", card(3, Microarchitectures.THREEDFX, "", 90).designLabel());
    }

    @Test
    void aCardThatSaysNothingOfItsDesign_countsAGigahertzAtTheP6sWork() {
        final GpuSpec plain = new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 2048, 3072, 250);
        assertEquals(1000, plain.clockMhz());
        assertEquals(2_048L, plain.power());
    }

    @Test
    void constructor_rejectsANonPositiveClock() {
        assertThrows(IllegalArgumentException.class,
                () -> card(1, Microarchitectures.VGA, "", 0));
    }

    private static GpuSpec card(final int cores, final Microarchitecture arch, final String chip, final int mhz) {
        return new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, cores, 1024, 100).on(arch, chip, mhz);
    }
}
