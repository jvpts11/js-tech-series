/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EraStorageTest {

    @Test
    void bits_climbFromSixteenToSixtyFourAndStayThere() {
        assertEquals(16, EraStorage.bits(HardwareEra.VINTAGE));
        assertEquals(32, EraStorage.bits(HardwareEra.LEGACY));
        assertEquals(64, EraStorage.bits(HardwareEra.TRANSITION));
        assertEquals(64, EraStorage.bits(HardwareEra.SINGULARITY));
    }

    @Test
    void mbPerItem_isTheLadderOneSixteenTwoHundredFiftySix() {
        assertEquals(1L, EraStorage.mbPerItem(HardwareEra.VINTAGE));
        assertEquals(16L, EraStorage.mbPerItem(HardwareEra.LEGACY));
        assertEquals(256L, EraStorage.mbPerItem(HardwareEra.STANDARD));
        assertEquals(256L, EraStorage.mbPerItem(HardwareEra.EXA));
    }

    @Test
    void itemsFor_roundsAMegabyteSizeUpToWholeItems() {
        assertEquals(4L, EraStorage.itemsFor(HardwareEra.VINTAGE, 4L));
        assertEquals(2L, EraStorage.itemsFor(HardwareEra.LEGACY, 17L));
        assertEquals(0L, EraStorage.itemsFor(HardwareEra.STANDARD, 0L));
    }

    @Test
    void itemsFor_neverTurnsAnEnormousSizeIntoNothing() {
        for (final Era era : HardwareEra.SERIES) {
            assertTrue(EraStorage.itemsFor(era, Long.MAX_VALUE) > 0L, era.id());
        }
    }

    @Test
    void bytesPerMbEq_isAThousandthOfAnItemInBytes() {
        assertEquals(1024L * 1024L / 1000L, EraStorage.bytesPerMbEq(HardwareEra.VINTAGE));
        assertEquals(256L * 1024L * 1024L / 1000L, EraStorage.bytesPerMbEq(HardwareEra.STANDARD));
    }
}
