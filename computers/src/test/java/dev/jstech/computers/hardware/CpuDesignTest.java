/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CpuDesignTest {

    @Test
    void label_namesTheDesignThenTheCodename() {
        assertEquals("Centro Conroe", CpuDesign.of(Microarchitectures.CENTRO, "Conroe").label());
        assertEquals("Haswell", CpuDesign.of(Microarchitectures.HASWELL, "").label());
        assertEquals("", CpuDesign.UNSPECIFIED.label());
    }

    @Test
    void withSmt_keepsEverythingElse() {
        final CpuDesign plain = CpuDesign.of(Microarchitectures.HASWELL, "Devil's Canyon");
        final CpuDesign threaded = plain.withSmt();
        assertFalse(plain.smt());
        assertTrue(threaded.smt());
        assertEquals(plain.arch(), threaded.arch());
        assertEquals(plain.codename(), threaded.codename());
    }

    @Test
    void withEfficiencyCores_addsThemAtTheirOwnClockAndDesign() {
        final CpuDesign hybrid = CpuDesign.of(Microarchitectures.ARROW_LAKE, "").withSmt()
                .withEfficiencyCores(16, 3200, Microarchitectures.SKYMONT);
        assertEquals(16, hybrid.efficiencyCores());
        assertEquals(3200, hybrid.efficiencyMhz());
        assertEquals(Microarchitectures.SKYMONT, hybrid.efficiencyArch());
        assertTrue(hybrid.smt());
    }

    @Test
    void construct_efficiencyCoresWithNoClock_areRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> CpuDesign.of(Microarchitectures.ALDER_LAKE, "").withEfficiencyCores(8, 0,
                        Microarchitectures.GRACEMONT));
    }

    @Test
    void of_carriesNoMemoryOfItsOwn() {
        assertEquals(0L, CpuDesign.of(Microarchitectures.HASWELL, "").memoryItems());
    }

    @Test
    void withMemory_keepsTheRestOfTheChip() {
        final CpuDesign chip = CpuDesign.of(Microarchitectures.FIRESTORM, "")
                .withEfficiencyCores(4, 2064, Microarchitectures.ICESTORM)
                .withGraphics(new IntegratedGraphics("Test GPU", 8, 1278))
                .withMemory(2048L);
        assertEquals(2048L, chip.memoryItems());
        assertEquals(4, chip.efficiencyCores());
        assertTrue(chip.graphics().present());
        assertEquals(2048L, chip.withSmt().memoryItems(), "what comes after keeps the memory");
    }

    @Test
    void construct_negativeMemory_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> CpuDesign.of(Microarchitectures.FIRESTORM, "").withMemory(-1L));
    }

    @Test
    void of_carriesNoHeatSinkOfItsOwn() {
        assertEquals(0, CpuDesign.of(Microarchitectures.PPC_750, "Arthur").heatSinkWatts());
    }

    @Test
    void withHeatSink_keepsTheRestOfTheChip() {
        final CpuDesign module = CpuDesign.of(Microarchitectures.PPC_7450, "Apollo").withHeatSink(60);
        assertEquals(60, module.heatSinkWatts());
        assertEquals("7450 Apollo", module.label());
        assertEquals(60, module.withSmt().heatSinkWatts(), "what comes after keeps the heat sink");
    }

    @Test
    void construct_negativeHeatSink_isRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> CpuDesign.of(Microarchitectures.PPC_7450, "").withHeatSink(-1));
    }
}
