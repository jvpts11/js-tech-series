/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitecturesTest {

    @Test
    void x86_64_runsWhatWasBuiltForX86() {
        assertTrue(Architectures.X86_64.runs(Architectures.X86));
    }

    @Test
    void x86_doesNotRunWhatWasBuiltForX86_64() {
        assertFalse(Architectures.X86.runs(Architectures.X86_64));
    }

    @Test
    void x86_16_runsOnlyItsOwn() {
        assertTrue(Architectures.X86_16.runs(Architectures.X86_16));
        assertFalse(Architectures.X86_16.runs(Architectures.X86));
        assertFalse(Architectures.X86_16.runs(Architectures.X86_64));
    }

    @Test
    void x86_doesNotRunWhatWasBuiltForTheSixteenBitOne() {
        assertFalse(Architectures.X86.runs(Architectures.X86_16));
        assertFalse(Architectures.X86_64.runs(Architectures.X86_16));
    }

    @Test
    void bits_growWithTheSeries() {
        assertEquals(16, Architectures.X86_16.bits());
        assertEquals(32, Architectures.X86.bits());
        assertEquals(64, Architectures.X86_64.bits());
    }

    @Test
    void of_vintage_isTheSixteenBitOne() {
        assertEquals(Architectures.X86_16, Architectures.of(HardwareEra.VINTAGE));
    }

    @Test
    void of_legacy_isX86() {
        assertEquals(Architectures.X86, Architectures.of(HardwareEra.LEGACY));
    }

    @Test
    void of_standardAndLater_isX86_64() {
        assertEquals(Architectures.X86_64, Architectures.of(HardwareEra.STANDARD));
        assertEquals(Architectures.X86_64, Architectures.of(HardwareEra.ADVANCED));
        assertEquals(Architectures.X86_64, Architectures.of(HardwareEra.EXA));
        assertEquals(Architectures.X86_64, Architectures.of(HardwareEra.SINGULARITY));
    }

    @Test
    void byId_findsTheOnesThisModBrings() {
        assertEquals(Architectures.X86_16, Architectures.byId("jsc:x86_16").orElseThrow());
        assertEquals(Architectures.X86, Architectures.byId("jsc:x86").orElseThrow());
        assertEquals(Architectures.X86_64, Architectures.byId("jsc:x86_64").orElseThrow());
    }

    @Test
    void byId_anIdNoModBrought_isEmpty() {
        assertTrue(Architectures.byId("other:risc").isEmpty());
    }

    @Test
    void all_holdTheThreeThisModBrings_oldestFirst() {
        final List<ArchitectureSpec> all = Architectures.all();
        assertTrue(all.containsAll(List.of(Architectures.X86_16, Architectures.X86, Architectures.X86_64)));
        assertTrue(all.indexOf(Architectures.X86_16) < all.indexOf(Architectures.X86)
                && all.indexOf(Architectures.X86) < all.indexOf(Architectures.X86_64),
                "the series is listed in the order it was built: " + all);
    }

    @Test
    void find_takesAnIdOrTheNameItIsWrittenUnder() {
        assertEquals(Architectures.X86_64, Architectures.find("jsc:x86_64").orElseThrow());
        assertEquals(Architectures.X86_64, Architectures.find("x86-64").orElseThrow());
        assertEquals(Architectures.X86_64, Architectures.find("X86-64").orElseThrow());
    }

    @Test
    void find_somethingNoModBrought_isEmpty() {
        assertTrue(Architectures.find("risc").isEmpty());
    }

    /*
     * The listing format falls back to 32-bit x86 for a program that names no architecture, and it holds that name
     * itself because the virtual machine is not allowed to know the hardware. This is the seam where the two
     * spellings are held together.
     */
    @Test
    void x86_isWhatAListingFallsBackTo() {
        assertEquals(Architectures.X86.id(), AsmProgram.DEFAULT_ARCHITECTURE);
    }

    @Test
    void add_anIdAlreadyTaken_isRefused() {
        final ArchitectureSpec impostor = new ArchitectureSpec("jsc:x86", "x86", 64, Set.of("jsc:x86"));
        assertThrows(IllegalStateException.class, () -> Architectures.add(impostor));
    }

    @Test
    void add_theSameArchitectureTwice_isLetThrough() {
        Architectures.add(Architectures.X86);
        assertEquals(Architectures.X86, Architectures.byId("jsc:x86").orElseThrow());
    }
}
