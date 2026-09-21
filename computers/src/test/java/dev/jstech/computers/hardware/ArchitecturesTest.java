/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.Opcode;
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

    /**
     * Every chip of the line runs what was built for the ones before it, as the real ones did.
     *
     * <p>The newest one reaching the oldest is not written down anywhere: it follows from each one taking on
     * what the one below it ran, which is what keeps a program written for the first machines working on
     * machines two ages later without anybody listing the pairs.
     */
    @Test
    void x86_runsWhatWasBuiltForTheSixteenBitOne() {
        assertTrue(Architectures.X86.runs(Architectures.X86_16));
        assertTrue(Architectures.X86_64.runs(Architectures.X86_16));
        assertTrue(Architectures.X86_64.runs(Architectures.X86));
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

    /**
     * Nothing has been added to a later architecture yet, so every instruction there is belongs to the oldest.
     *
     * <p>This is the answer being right rather than the question going unasked, and it is what makes a program
     * built today run on the oldest machine of its line instead of only on the newest.
     */
    @Test
    void oldestWith_everyInstructionThereIs_isStillTheOldestOfTheLine() {
        assertEquals(Architectures.X86,
                Architectures.oldestWith(Architectures.X86, java.util.EnumSet.allOf(Opcode.class)));
        assertTrue(Architectures.has(Architectures.X86, Opcode.ADD));
    }

    /**
     * A program starting at the oldest machine stays there, though newer ones would take it.
     *
     * <p>Both chips above it run its programs, so both are candidates and neither is chosen: choosing the
     * oldest is what makes one program serve all three ages instead of only the age it was compiled on.
     */
    @Test
    void oldestWith_staysAtTheOldestEvenWhereNewerOnesWouldTakeIt() {
        assertTrue(Architectures.X86.runs(Architectures.X86_16), "the newer ones are candidates");
        assertEquals(Architectures.X86_16,
                Architectures.oldestWith(Architectures.X86_16, java.util.EnumSet.of(Opcode.ADD)));
    }

    /** Going the other way is not offered: a program of a later chip never drifts down to an earlier one. */
    @Test
    void oldestWith_neverMovesAProgramDownToAnEarlierChip() {
        assertEquals(Architectures.X86,
                Architectures.oldestWith(Architectures.X86, java.util.EnumSet.of(Opcode.ADD)));
        assertFalse(Architectures.X86_16.runs(Architectures.X86),
                "which is why the oldest of all is not a candidate for it");
    }

    /**
     * A program that reaches for something only a later architecture has is built for that one, and no further.
     *
     * <p>The table the mod keeps is empty until a later cycle writes in it, so this hands the choosing a table
     * that says something. Both halves matter: a program that stays within the older one has to stay there, or
     * every program would drift up to the newest chip and stop running on the machines it was written for.
     */
    @Test
    void oldestWith_aProgramThatReachesForSomethingNewer_movesUpToIt() {
        final java.util.Map<Opcode, String> addedIn = java.util.Map.of(Opcode.CONV_I8, "jsc:x86_64");
        assertEquals(Architectures.X86_64, Architectures.oldestWith(Architectures.X86,
                java.util.EnumSet.of(Opcode.ADD, Opcode.CONV_I8), addedIn));
        assertEquals(Architectures.X86, Architectures.oldestWith(Architectures.X86,
                java.util.EnumSet.of(Opcode.ADD, Opcode.SUB), addedIn));
    }
}
