/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class DataLineTest {

    @Test
    void lineId_isTheCoresDataNamespace() {
        assertEquals("jscore:data/long_distance", DataLine.LONG_DISTANCE.lineId());
    }

    @Test
    void ofLineId_readsTheLineBack() {
        for (final DataLine line : DataLine.values()) {
            assertEquals(line, DataLine.ofLineId(line.lineId()));
        }
        assertNull(DataLine.ofLineId("jscore:data/t1_ethernet"));
    }

    @Test
    void generation_isTheEraExceptForCrafting() {
        assertEquals(3, DataLine.ACCESS.generation(HardwareEra.STANDARD));
        assertEquals(0, DataLine.CRAFTING.generation(HardwareEra.ADVANCED));
    }

    @Test
    void byName_readsEveryLine() {
        for (final DataLine line : DataLine.values()) {
            assertEquals(line, DataLine.byName(line.serializedName()));
        }
    }
}
