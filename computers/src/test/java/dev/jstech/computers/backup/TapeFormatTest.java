/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class TapeFormatTest {

    @Test
    void capacityMb_isEachTapesRealNativeCapacity() {
        assertEquals(150L, TapeFormat.REEL.capacityMb(), "the half-inch reel, 150 MB");
        assertEquals(40L * 1024L, TapeFormat.DLT.capacityMb(), "the DLT, 40 GB");
        assertEquals(400L * 1024L, TapeFormat.LTO_3.capacityMb(), "the LTO-3, 400 GB");
        assertEquals(2_560L * 1024L, TapeFormat.LTO_6.capacityMb(), "the LTO-6, 2.5 TB");
        assertEquals(18L * 1024L * 1024L, TapeFormat.LTO_9.capacityMb(), "the LTO-9, 18 TB");
    }

    @Test
    void windTicks_isTheShareOfAFullWindBetweenTheTwoPlaces() {
        final TapeFormat lto6 = TapeFormat.LTO_6;
        assertEquals(100L * 20L, lto6.windTicks(0L, lto6.capacityItems()), "end to end, its full wind");
        assertEquals(100L * 20L / 2L, lto6.windTicks(lto6.capacityItems(), lto6.capacityItems() / 2L),
                "half of it either way");
        assertEquals(0L, lto6.windTicks(500L, 500L), "nothing where the head already is");
    }

    @Test
    void of_givesEachErasTape() {
        assertEquals(TapeFormat.REEL, TapeFormat.of(HardwareEra.VINTAGE));
        assertEquals(TapeFormat.LTO_9, TapeFormat.of(HardwareEra.ADVANCED));
    }

    @Test
    void label_isTheNameEachFormatGoesBy() {
        assertEquals("9-track", TapeFormat.REEL.label());
        assertEquals("DLT", TapeFormat.DLT.label());
        assertEquals("LTO-6", TapeFormat.LTO_6.label());
    }
}
