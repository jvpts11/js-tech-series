/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IsaTranslatorTest {

    @Test
    void runs_champollion_runsPowerPcProgramsOnX86_64() {
        assertTrue(IsaTranslators.CHAMPOLLION.runs(Isas.X86_64, Isas.POWERPC.id()));
    }

    /** Only what was built for its own instruction set: Rosetta 2 left the 32-bit Intel programs behind. */
    @Test
    void runs_champollion2_takesTheX86_64ProgramsAndNotTheOlderOnes() {
        assertTrue(IsaTranslators.CHAMPOLLION_2.runs(Isas.ARM64, Isas.X86_64.id()));
        assertFalse(IsaTranslators.CHAMPOLLION_2.runs(Isas.ARM64, Isas.X86.id()));
        assertFalse(IsaTranslators.CHAMPOLLION_2.runs(Isas.ARM64, Isas.IA_16.id()));
    }

    @Test
    void runs_onAProcessorThatCannotRunTheTranslator_isNothing() {
        assertFalse(IsaTranslators.CHAMPOLLION.runs(Isas.ARM64, Isas.POWERPC.id()));
        assertFalse(IsaTranslators.CHAMPOLLION.runs(Isas.X86, Isas.POWERPC.id()));
    }

    @Test
    void speeds_areHalfAndFourFifths() {
        assertEquals(50, IsaTranslators.CHAMPOLLION.speedPercent());
        assertEquals(80, IsaTranslators.CHAMPOLLION_2.speedPercent());
    }

    @Test
    void byId_findsTheOnesThisModBrings() {
        assertEquals(IsaTranslators.CHAMPOLLION, IsaTranslators.byId("jsc:champollion").orElseThrow());
        assertEquals(IsaTranslators.CHAMPOLLION_2, IsaTranslators.byId("jsc:champollion_2").orElseThrow());
        assertTrue(IsaTranslators.byId("other:bridge").isEmpty());
    }

    @Test
    void add_anIdAlreadyTaken_isRefused() {
        final IsaTranslator impostor = new IsaTranslator("jsc:champollion", "Champollion", "jsc:arm64", "jsc:x86_64",
                10);
        assertThrows(IllegalStateException.class, () -> IsaTranslators.add(impostor));
    }

    @Test
    void construct_aSpeedOutsideOneToAHundred_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new IsaTranslator("test:bridge", "Bridge", "jsc:x86", "jsc:arm64", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new IsaTranslator("test:bridge", "Bridge", "jsc:x86", "jsc:arm64", 101));
    }

    @Test
    void construct_translatingAnInstructionSetToItself_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new IsaTranslator("test:bridge", "Bridge", "jsc:x86", "jsc:x86", 50));
    }
}
