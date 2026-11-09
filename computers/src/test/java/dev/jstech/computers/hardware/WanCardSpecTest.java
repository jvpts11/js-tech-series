/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.os.HostScope;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WanCardSpecTest {

    private static final WanCardSpec T3 = new WanCardSpec(HardwareEra.TRANSITION, PcieGeneration.PCIE_2_0, 12);

    @Test
    void takes_theLongDistanceLineOfItsEraAndTheOlderOnes() {
        assertTrue(T3.takes(new DataLink(DataLine.LONG_DISTANCE, HardwareEra.TRANSITION)));
        assertTrue(T3.takes(new DataLink(DataLine.LONG_DISTANCE, HardwareEra.LEGACY)));
        assertTrue(T3.takes(new DataLink(DataLine.LONG_DISTANCE, HardwareEra.VINTAGE)));
    }

    @Test
    void takes_noNewerLineNorAnotherLineNorNone() {
        assertFalse(T3.takes(new DataLink(DataLine.LONG_DISTANCE, HardwareEra.STANDARD)));
        assertFalse(T3.takes(new DataLink(DataLine.BACKBONE, HardwareEra.TRANSITION)));
        assertFalse(T3.takes(null));
    }

    @Test
    void itemsPerTick_isItsOwnLinesSpeed() {
        assertEquals(1L, new WanCardSpec(HardwareEra.VINTAGE, PcieGeneration.ISA, 3).itemsPerTick());
        assertEquals(24L, T3.itemsPerTick());
        assertEquals(256L, new WanCardSpec(HardwareEra.ADVANCED, PcieGeneration.PCIE_4_0, 25).itemsPerTick());
    }

    @Test
    void kind_goesOnlyInAWanGateway() {
        assertEquals(ExpansionCardKind.WAN_INTERFACE, T3.kind());
        assertTrue(T3.kind().fitsIn(HostScope.WAN_GATEWAY_COMPUTER));
        assertFalse(T3.kind().fitsIn(HostScope.MAINFRAME));
        assertFalse(T3.kind().fitsIn(HostScope.PERSONAL_COMPUTER));
    }

    @Test
    void constructor_refusesANegativeDraw() {
        assertThrows(IllegalArgumentException.class,
                () -> new WanCardSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, -1));
    }
}
