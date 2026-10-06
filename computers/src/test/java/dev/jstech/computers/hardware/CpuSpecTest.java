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

class CpuSpecTest {

    @Test
    void orchestrationCapacity_minimalVintageCpu_isOne() {
        // 486SX-class: 1 core at 25 MHz -> 1 x 25 x 40 = 1000, rounds to 1 item/tick
        final CpuSpec cpu = new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 25, 3, false);
        assertEquals(1L, cpu.orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_roundsToNearest() {
        // 486DX2-class: 1 core at 66 MHz -> 2.64, rounds to 3 (not 2)
        final CpuSpec cpu = new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 66, 5, false);
        assertEquals(3L, cpu.orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_serverCpu() {
        // Epic 9654-class: 96 cores at 2400 MHz -> 9,216
        final CpuSpec cpu = new CpuSpec(HardwareEra.EXA, CpuSocketId.SP5, 96, 2400, 360, false);
        assertEquals(9_216L, cpu.orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_quantumFlagship() {
        // Quantum flagship: 256 cores at 6000 MHz -> 61,440
        final CpuSpec cpu = new CpuSpec(HardwareEra.SINGULARITY, CpuSocketId.SOCKET_Q, 256, 6000, 500, false);
        assertEquals(61_440L, cpu.orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_alienAppliesTripleMultiplier() {
        // EM core: 128 cores at 5500 MHz -> 28,160, tripled by the alien factor -> 84,480
        final CpuSpec cpu = new CpuSpec(HardwareEra.SINGULARITY, CpuSocketId.SOCKET_EM, 128, 5500, 450, true);
        assertEquals(84_480L, cpu.orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_nonAlienEquivalentIsOneThird() {
        final CpuSpec alien = new CpuSpec(HardwareEra.SINGULARITY, CpuSocketId.SOCKET_EM, 128, 5500, 450, true);
        final CpuSpec plain = new CpuSpec(HardwareEra.SINGULARITY, CpuSocketId.SOCKET_EM, 128, 5500, 450, false);
        assertEquals(plain.orchestrationCapacity() * 3, alien.orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_countsTheWorkOfTheDesign() {
        // A Haswell quad at 3.3 GHz: 4 x 3.3 x 2.7 x 40 = 1,425.6.
        assertEquals(1_426L, haswell(4, 3300).orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_twoThreadsACoreAddAFifth() {
        // The same design at 4.0 GHz with two threads a core: 4 x 4.0 x 2.7 x 1.2 x 40 = 2,073.6.
        assertEquals(2_074L, haswell(4, 4000).withSmt().orchestrationCapacity());
        assertEquals(1_728L, haswell(4, 4000).orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_roundsAHalfUpWithoutLosingItOnTheWay() {
        // 350 MHz x 0.75 x 40 is 10.5 exactly; worked out in fractions it could come to 10.4999 and round down.
        final CpuSpec k6 = new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_7, 1, 350, 15, false)
                .on(Microarchitectures.K6, "Chomper");
        assertEquals(11L, k6.orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_neverFallsBelowOne() {
        // The 486's design makes a 25 MHz chip 0.45 items a tick, and a processor that runs does some work.
        final CpuSpec sx = new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 25, 3, false)
                .on(Microarchitectures.I486, "");
        assertEquals(1L, sx.orchestrationCapacity());
    }

    @Test
    void orchestrationCapacity_efficiencyCoresRunAtTheirOwnClockAndDesign() {
        // 8 performance cores at 3.2 GHz with two threads each (4,423.68) and 8 efficiency cores at 2.4 (1,766.4).
        final CpuSpec hybrid = new CpuSpec(HardwareEra.ADVANCED, CpuSocketId.LGA_1150, 8, 3200, 125, false)
                .on(Microarchitectures.ALDER_LAKE, "").withSmt()
                .withEfficiencyCores(8, 2400, Microarchitectures.GRACEMONT);
        assertEquals(6_190L, hybrid.orchestrationCapacity());
    }

    /**
     * The pairs the formula was chosen for: counted by clock and cores alone, each of these came out in the wrong
     * order, the older high-clock chip above the better one after it.
     */
    @Test
    void orchestrationCapacity_ranksTheChipsTheWayTheyReallyRanked() {
        final long p4560 = chip(1, 3600, Microarchitectures.NETBURST).withSmt().orchestrationCapacity();
        final long fx55 = chip(1, 2600, Microarchitectures.K8).orchestrationCapacity();
        final long pentiumD805 = chip(2, 2660, Microarchitectures.NETBURST).orchestrationCapacity();
        final long e4300 = chip(2, 1800, Microarchitectures.CENTRO).orchestrationCapacity();
        final long e6600 = chip(2, 2400, Microarchitectures.CENTRO).orchestrationCapacity();
        final long fx8350 = chip(8, 4000, Microarchitectures.PILEDRIVER).orchestrationCapacity();
        final long fx9590 = chip(8, 4700, Microarchitectures.PILEDRIVER).orchestrationCapacity();
        final long i74790k = haswell(4, 4000).withSmt().orchestrationCapacity();
        final long x61090t = chip(6, 3200, Microarchitectures.K10).orchestrationCapacity();
        final long i54590 = haswell(4, 3300).orchestrationCapacity();

        assertEquals(121L, p4560);
        assertEquals(135L, fx55);
        assertEquals(149L, pentiumD805);
        assertEquals(230L, e4300);
        assertEquals(307L, e6600);
        assertEquals(1_728L, fx8350);
        assertEquals(2_030L, fx9590);
        assertEquals(2_074L, i74790k);
        assertEquals(1_152L, x61090t);
        assertEquals(1_426L, i54590);
        assertTrue(p4560 < fx55);
        assertTrue(pentiumD805 < e4300 && e4300 < e6600);
        assertTrue(fx8350 < i74790k && Math.abs(i74790k - fx9590) * 20 < i74790k, "within 5% of each other");
        assertTrue(x61090t < i54590);
    }

    @Test
    void orchestrationCapacity_aChipThatSaysNothingOfItsDesignCountsAsBefore() {
        final CpuSpec plain = new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, 4, 4000, 88, false);
        assertEquals(Microarchitectures.UNSPECIFIED, plain.design().arch());
        assertEquals(640L, plain.orchestrationCapacity());
    }

    @Test
    void constructor_rejectsNonPositiveCores() {
        assertThrows(IllegalArgumentException.class,
                () -> new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 0, 25, 3, false));
    }

    @Test
    void constructor_rejectsNonPositiveFrequency() {
        assertThrows(IllegalArgumentException.class,
                () -> new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 0, 3, false));
    }

    @Test
    void constructor_rejectsNullSocket() {
        assertThrows(NullPointerException.class,
                () -> new CpuSpec(HardwareEra.VINTAGE, null, 1, 25, 3, false));
    }

    @Test
    void isa_ofOurOwnChips_followsTheEra() {
        assertEquals(Isas.IA_16, new CpuSpec(HardwareEra.VINTAGE, CpuSocketId.SOCKET_3, 1, 25, 3, false).isa());
        assertEquals(Isas.X86, new CpuSpec(HardwareEra.LEGACY, CpuSocketId.SOCKET_370, 1, 800, 25, false).isa());
        assertEquals(Isas.X86_64,
                new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 8, 3500, 130, false).isa());
    }

    @Test
    void isa_canBeToldInsteadOfDerived() {
        final CpuSpec cpu = new CpuSpec(HardwareEra.VINTAGE, Isas.X86_64, CpuSocketId.SOCKET_3, 1, 25, 3, false);
        assertEquals(Isas.X86_64, cpu.isa());
    }

    @Test
    void constructor_rejectsNullIsa() {
        assertThrows(NullPointerException.class,
                () -> new CpuSpec(HardwareEra.VINTAGE, null, CpuSocketId.SOCKET_3, 1, 25, 3, false));
    }

    private static CpuSpec haswell(final int cores, final int mhz) {
        return chip(cores, mhz, Microarchitectures.HASWELL);
    }

    private static CpuSpec chip(final int cores, final int mhz, final Microarchitecture arch) {
        return new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_1150, cores, mhz, 88, false).on(arch, "");
    }
}
