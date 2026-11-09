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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IsaFitTest {

    private static final List<IsaTranslator> NONE = List.of();
    private static final List<IsaTranslator> BOTH = List.of(IsaTranslators.CHAMPOLLION, IsaTranslators.CHAMPOLLION_2);

    @Test
    void of_aBuildForTheProcessor_runsAtFullSpeed() {
        final IsaFit fit = IsaFit.of(Isas.POWERPC, List.of(Isas.POWERPC.id()), NONE).orElseThrow();
        assertEquals(0, fit.build());
        assertEquals(IsaFit.FULL_SPEED, fit.speedPercent());
        assertFalse(fit.translated());
    }

    /** A universal build runs on each processor it carries a build for, each taking its own. */
    @Test
    void of_aUniversalBuild_givesEachProcessorItsOwn() {
        final List<String> universal = List.of(Isas.POWERPC.id(), Isas.X86_64.id());
        assertEquals(0, IsaFit.of(Isas.POWERPC, universal, NONE).orElseThrow().build());
        assertEquals(1, IsaFit.of(Isas.X86_64, universal, NONE).orElseThrow().build());
    }

    /** Of two builds the processor runs as they are, the one made for it is taken, wherever it is carried. */
    @Test
    void of_theBuildForThatVeryProcessor_comesBeforeAnOlderOneOfItsLine() {
        final IsaFit fit = IsaFit.of(Isas.X86_64, List.of(Isas.X86.id(), Isas.X86_64.id()), NONE).orElseThrow();
        assertEquals(1, fit.build());
    }

    @Test
    void of_anOlderBuildOfTheLine_runsAsItIs() {
        final IsaFit fit = IsaFit.of(Isas.X86_64, List.of(Isas.IA_16.id()), BOTH).orElseThrow();
        assertEquals(IsaFit.FULL_SPEED, fit.speedPercent());
        assertFalse(fit.translated());
    }

    @Test
    void of_aBuildOnlyATranslatorRuns_runsThroughItAtItsSpeed() {
        final IsaFit fit = IsaFit.of(Isas.X86_64, List.of(Isas.POWERPC.id()), BOTH).orElseThrow();
        assertTrue(fit.translated());
        assertEquals(IsaTranslators.CHAMPOLLION, fit.through());
        assertEquals(50, fit.speedPercent());
    }

    /** A build the processor runs as it is always wins over a faster translator. */
    @Test
    void of_aBuildThatRunsAsItIs_isTakenBeforeATranslatedOne() {
        final IsaFit fit = IsaFit.of(Isas.ARM64, List.of(Isas.X86_64.id(), Isas.ARM64.id()), BOTH).orElseThrow();
        assertEquals(1, fit.build());
        assertNull(fit.through());
    }

    @Test
    void of_theFastestTranslatorThatCarriesABuild_isTaken() {
        final IsaTranslator slow = new IsaTranslator("test:slow", "Slow", Isas.X86_64.id(), Isas.ARM64.id(), 30);
        final IsaFit fit = IsaFit.of(Isas.ARM64, List.of(Isas.X86_64.id()), List.of(slow,
                IsaTranslators.CHAMPOLLION_2)).orElseThrow();
        assertEquals(IsaTranslators.CHAMPOLLION_2, fit.through());
        assertEquals(80, fit.speedPercent());
    }

    @Test
    void of_aBuildNothingRuns_isNone() {
        assertTrue(IsaFit.of(Isas.M68K, List.of(Isas.POWERPC.id(), Isas.X86.id()), BOTH).isEmpty());
        assertTrue(IsaFit.of(Isas.X86_64, List.of(Isas.POWERPC.id()), NONE).isEmpty(),
                "with no translator on the system, a PowerPC program does not run on x86-64");
    }
}
