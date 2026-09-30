/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroarchitecturesTest {

    @Test
    void all_haveIdsOfTheirOwn() {
        final Set<String> seen = new HashSet<>();
        for (final Microarchitecture design : Microarchitectures.all()) {
            assertTrue(seen.add(design.id()), () -> design.id() + " is in the table twice");
        }
    }

    @Test
    void efficiency_countsTheP6AsOne() {
        assertEquals(1000, Microarchitectures.P6.efficiencyMilli());
        assertEquals(1000, Microarchitectures.UNSPECIFIED.efficiencyMilli());
    }

    @Test
    void efficiency_runsFromThe486ToTheNewestDesign() {
        assertEquals(450, Microarchitectures.I486.efficiencyMilli());
        assertEquals(700, Microarchitectures.NETBURST.efficiencyMilli());
        assertEquals(1600, Microarchitectures.CENTRO.efficiencyMilli());
        assertEquals(1500, Microarchitectures.K10.efficiencyMilli());
        assertEquals(1350, Microarchitectures.PILEDRIVER.efficiencyMilli());
        assertEquals(2700, Microarchitectures.HASWELL.efficiencyMilli());
        assertEquals(4100, Microarchitectures.WAY_5.efficiencyMilli());
        assertEquals(2300, Microarchitectures.GRACEMONT.efficiencyMilli());
    }

    @Test
    void efficiency_climbsFromOneGenerationOfALineToTheNext() {
        final Microarchitecture[] intel = {Microarchitectures.P5, Microarchitectures.P6, Microarchitectures.CENTRO,
                Microarchitectures.NEHALEM, Microarchitectures.SANDY_BRIDGE, Microarchitectures.HASWELL,
                Microarchitectures.SKYLAKE, Microarchitectures.ALDER_LAKE, Microarchitectures.ARROW_LAKE};
        final Microarchitecture[] amd = {Microarchitectures.K5, Microarchitectures.K6, Microarchitectures.K7,
                Microarchitectures.K8, Microarchitectures.WAY_1, Microarchitectures.WAY_2, Microarchitectures.WAY_3,
                Microarchitectures.WAY_4, Microarchitectures.WAY_5};
        for (final Microarchitecture[] line : new Microarchitecture[][]{intel, amd}) {
            for (int i = 1; i < line.length; i++) {
                assertTrue(line[i - 1].efficiencyMilli() < line[i].efficiencyMilli(),
                        line[i].id() + " should do more per clock than " + line[i - 1].id());
            }
        }
    }

    @Test
    void names_giveTheBrandsTheirParodyAndKeepTheCodenames() {
        assertEquals("Centro", Microarchitectures.CENTRO.name());
        assertEquals("Way 2", Microarchitectures.WAY_2.name());
        assertEquals("Haswell", Microarchitectures.HASWELL.name());
        assertEquals("Tesla", Microarchitectures.TESLA.name());
        // Designs sold under a brand and never named otherwise stay unnamed.
        assertEquals("", Microarchitectures.RENDITION.name());
        assertEquals("", Microarchitectures.THREEDFX.name());
    }

    @Test
    void unitsOf_groupsTheShadersOfAFiveWideDesign() {
        assertEquals(192, Microarchitectures.TERASCALE_2.unitsOf(960));
        assertEquals(960, Microarchitectures.GCN.unitsOf(960));
        assertEquals(1, Microarchitectures.TERASCALE_2.unitsOf(3));
    }

    @Test
    void construct_aDesignThatDoesNothing_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Microarchitecture("none", "None", 0));
    }
}
