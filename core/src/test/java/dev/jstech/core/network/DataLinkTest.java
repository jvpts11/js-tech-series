/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class DataLinkTest {

    private static final HardwareEra[] ERAS = {HardwareEra.VINTAGE, HardwareEra.LEGACY, HardwareEra.TRANSITION,
            HardwareEra.STANDARD, HardwareEra.ADVANCED};

    @Test
    void throughputAndRange_accessFollowTheTable() {
        assertTable(DataLine.ACCESS, new long[] {4, 16, 64, 128, 256}, new int[] {32, 48, 64, 80, 96});
    }

    @Test
    void throughputAndRange_backboneFollowTheTable() {
        assertTable(DataLine.BACKBONE, new long[] {16, 64, 256, 512, 2_048}, new int[] {96, 160, 200, 512, 1_024});
    }

    @Test
    void throughputAndRange_longDistanceFollowTheTable() {
        assertTable(DataLine.LONG_DISTANCE, new long[] {1, 8, 24, 64, 256},
                new int[] {2_000, 5_000, 7_500, 10_000, 20_000});
    }

    @Test
    void throughputAndRange_hpcBeginsAtTheTransition() {
        assertTable(DataLine.HPC, new long[] {0, 0, 1_024, 2_048, 8_192}, new int[] {0, 0, 24, 32, 48});
        assertFalse(new DataLink(DataLine.HPC, HardwareEra.LEGACY).exists());
        assertTrue(new DataLink(DataLine.HPC, HardwareEra.TRANSITION).exists());
    }

    @Test
    void throughputAndRange_craftingIsTheSameInEveryEra() {
        assertTable(DataLine.CRAFTING, new long[] {32, 32, 32, 32, 32}, new int[] {16, 16, 16, 16, 16});
    }

    @Test
    void new_craftingIsOneCableForEveryEra() {
        assertEquals(new DataLink(DataLine.CRAFTING, HardwareEra.VINTAGE),
                new DataLink(DataLine.CRAFTING, HardwareEra.ADVANCED));
        assertEquals(0, new DataLink(DataLine.CRAFTING, HardwareEra.STANDARD).generation());
    }

    @Test
    void exists_noLineGoesPastTheAdvanced() {
        assertFalse(new DataLink(DataLine.ACCESS, HardwareEra.EXA).exists());
        assertEquals(0L, new DataLink(DataLine.ACCESS, HardwareEra.EXA).throughput());
    }

    @Test
    void straight_onlyTheBackbonesFibre() {
        assertFalse(new DataLink(DataLine.BACKBONE, HardwareEra.TRANSITION).straight());
        assertTrue(new DataLink(DataLine.BACKBONE, HardwareEra.STANDARD).straight());
        assertTrue(new DataLink(DataLine.BACKBONE, HardwareEra.ADVANCED).straight());
        assertFalse(new DataLink(DataLine.LONG_DISTANCE, HardwareEra.ADVANCED).straight());
    }

    @Test
    void betweenTwoEnds_onlyTheLongDistance() {
        assertTrue(new DataLink(DataLine.LONG_DISTANCE, HardwareEra.VINTAGE).betweenTwoEnds());
        assertFalse(new DataLink(DataLine.BACKBONE, HardwareEra.VINTAGE).betweenTwoEnds());
    }

    @Test
    void serializedName_readsBackTheSameLink() {
        for (final DataLine line : DataLine.values()) {
            for (final HardwareEra era : ERAS) {
                final DataLink link = new DataLink(line, era);
                assertEquals(link, DataLink.byName(link.serializedName()), link.serializedName());
            }
        }
        assertEquals("long_distance_legacy", new DataLink(DataLine.LONG_DISTANCE, HardwareEra.LEGACY).serializedName());
        assertEquals("crafting", new DataLink(DataLine.CRAFTING, HardwareEra.LEGACY).serializedName());
    }

    @Test
    void byName_unknownNamesNothing() {
        assertNull(DataLink.byName("t1_ethernet"));
        assertNull(DataLink.byName(""));
        assertNull(DataLink.byName(null));
    }

    @Test
    void of_readsALineIdAndAGeneration() {
        assertEquals(new DataLink(DataLine.BACKBONE, HardwareEra.STANDARD), DataLink.of("jscore:data/backbone", 3));
        assertNull(DataLink.of("jscore:energy", 0));
        assertNull(DataLink.of("jscore:data/access", 99));
    }

    private static void assertTable(final DataLine line, final long[] throughput, final int[] range) {
        for (int i = 0; i < ERAS.length; i++) {
            final DataLink link = new DataLink(line, ERAS[i]);
            assertEquals(throughput[i], link.throughput(), link.serializedName() + " throughput");
            assertEquals(range[i], link.range(), link.serializedName() + " range");
        }
    }
}
