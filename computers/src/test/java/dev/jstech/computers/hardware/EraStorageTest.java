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
    void weightOfBytes_ofAnItemsBytesIsExactlyAnItem() {
        for (final Era era : HardwareEra.SERIES) {
            assertEquals(1000L, EraStorage.weightOfBytes(era, EraStorage.bytesPerItem(era)), era.id());
            assertEquals(2000L, EraStorage.weightOfBytes(era, 2L * EraStorage.bytesPerItem(era)), era.id());
        }
    }

    @Test
    void weightOfBytes_roundsUpAndWeighsNothingForNothing() {
        assertEquals(0L, EraStorage.weightOfBytes(HardwareEra.VINTAGE, 0L));
        assertEquals(1L, EraStorage.weightOfBytes(HardwareEra.VINTAGE, 1L));
        assertEquals(1001L, EraStorage.weightOfBytes(HardwareEra.VINTAGE, 1024L * 1024L + 1L));
    }

    @Test
    void weightOfBytes_neverRunsPastTheEndForTheLargestSize() {
        for (final Era era : HardwareEra.SERIES) {
            assertTrue(EraStorage.weightOfBytes(era, Long.MAX_VALUE) > 0L, era.id());
        }
    }

    @Test
    void bytesOfWeight_comesBackToTheSameWeight() {
        for (final Era era : HardwareEra.SERIES) {
            for (final long weight : new long[] {1L, 999L, 1000L, 1001L, 123_456L}) {
                assertEquals(weight, EraStorage.weightOfBytes(era, EraStorage.bytesOfWeight(era, weight)),
                        era.id() + " " + weight);
            }
        }
    }

    @Test
    void weightHeldBy_aFloppyIsAnItemAndAFractionOnVintageTerms() {
        assertEquals(1406L, EraStorage.weightHeldBy(HardwareEra.VINTAGE, 1_474_560L));
        assertEquals(1000L, EraStorage.weightHeldBy(HardwareEra.LEGACY, 16L * 1024L * 1024L));
        assertEquals(0L, EraStorage.weightHeldBy(HardwareEra.STANDARD, 0L));
    }
}
