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
}
