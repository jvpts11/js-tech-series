/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnergyLossTest {

    @Test
    void delivered_isAllOfItOverALosslessWay() {
        assertEquals(500L, EnergyLoss.delivered(500L, 0));
    }

    @Test
    void lost_takesItsMillionthsRoundedUp() {
        assertEquals(50L, EnergyLoss.lost(1_000L, 50_000));
        // 7 millionths of 100 000 is 0.7, which rounds up to one lost.
        assertEquals(1L, EnergyLoss.lost(100_000L, 7));
        assertEquals(99_999L, EnergyLoss.delivered(100_000L, 7));
    }

    @Test
    void lost_isAHundredthOfAPercentOverAHundredBlocksOfOneMillionth() {
        int way = 0;
        for (int block = 0; block < 100; block++) {
            way = EnergyLoss.along(way, 1);
        }

        assertEquals(100, way);
        assertEquals(1_000L, EnergyLoss.lost(10_000_000L, way));
    }

    @Test
    void lost_isEverythingOverAWayThatLosesTheWhole() {
        assertEquals(0L, EnergyLoss.delivered(1_234L, EnergyLoss.WHOLE));
    }

    @Test
    void lost_neverOverflowsForTheLargestAmount() {
        final long lost = EnergyLoss.lost(Long.MAX_VALUE, EnergyLoss.WHOLE - 1);

        assertTrue(lost > 0 && lost <= Long.MAX_VALUE);
        assertTrue(EnergyLoss.delivered(Long.MAX_VALUE, EnergyLoss.WHOLE - 1) >= 0);
    }

    @Test
    void along_addsTheBlocksLossesUpToTheWhole() {
        assertEquals(30, EnergyLoss.along(10, 20));
        assertEquals(EnergyLoss.WHOLE, EnergyLoss.along(600_000, 700_000));
    }

    @Test
    void toSend_isWhatArrivesAsWantedAfterTheLoss() {
        final long send = EnergyLoss.toSend(1_000L, 100_000);

        assertTrue(EnergyLoss.delivered(send, 100_000) >= 1_000L);
        assertTrue(EnergyLoss.delivered(send - 1, 100_000) < 1_000L);
        assertEquals(1_000L, EnergyLoss.toSend(1_000L, 0));
    }

    @Test
    void losses_refuseMillionthsOutsideTheWhole() {
        assertThrows(IllegalArgumentException.class, () -> EnergyLoss.lost(10L, -1));
        assertThrows(IllegalArgumentException.class, () -> EnergyLoss.lost(10L, EnergyLoss.WHOLE + 1));
    }
}
