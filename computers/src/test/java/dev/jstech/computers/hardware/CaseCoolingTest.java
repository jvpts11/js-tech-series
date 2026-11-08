/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CaseCoolingTest {

    private static final CoolerSpec STOCK = new CoolerSpec(HardwareEra.STANDARD, CoolerSpec.Kind.STOCK, 150);
    private static final CoolerSpec TOWER = new CoolerSpec(HardwareEra.STANDARD, CoolerSpec.Kind.TOWER, 250);
    private static final CoolerSpec AIO = new CoolerSpec(HardwareEra.STANDARD, CoolerSpec.Kind.WATER, 300);
    private static final CaseFanSpec FAN = new CaseFanSpec(HardwareEra.STANDARD, 120, 50);

    @Test
    void fans_countsEverySeat() {
        assertEquals(1, CaseCooling.VINTAGE.fans());
        assertEquals(6, CaseCooling.HIGH_PERFORMANCE.fans());
        assertEquals(0, CaseCooling.MAINFRAME.fans());
    }

    @Test
    void fanSlots_keepsTheMostAnyCaseHasOrNone() {
        assertEquals(CaseCooling.MOST_FANS, CaseCooling.VINTAGE.fanSlots());
        assertEquals(CaseCooling.MOST_FANS, CaseCooling.HIGH_PERFORMANCE.fanSlots());
        assertEquals(0, CaseCooling.MAINFRAME.fanSlots());
        assertEquals(0, CaseCooling.NONE.fanSlots());
    }

    @Test
    void side_numbersTheSeatsRearTopFront() {
        assertEquals(CaseCooling.Side.REAR, CaseCooling.HIGH_PERFORMANCE.side(0));
        assertEquals(CaseCooling.Side.TOP, CaseCooling.HIGH_PERFORMANCE.side(2));
        assertEquals(CaseCooling.Side.FRONT, CaseCooling.HIGH_PERFORMANCE.side(3));
        assertEquals(CaseCooling.Side.FRONT, CaseCooling.VINTAGE.side(0));
    }

    @Test
    void radiatorPairs_takesTheTopFirstThenTheFront() {
        assertEquals(List.of(1, 3), CaseCooling.HIGH_PERFORMANCE.radiatorPairs());
        assertEquals(List.of(1), CaseCooling.NEUTRAL.radiatorPairs());
        assertEquals(List.of(2), CaseCooling.AESTHETIC.radiatorPairs());
        assertTrue(CaseCooling.LEGACY.radiatorPairs().isEmpty(), "no pair of seats in the Legacy tower");
    }

    @Test
    void radiatorSeats_giveEachWaterCoolerTheNextPair() {
        assertEquals(Set.of(1, 2), CaseCooling.HIGH_PERFORMANCE.radiatorSeats(List.of(AIO)));
        assertEquals(Set.of(1, 2, 3, 4), CaseCooling.HIGH_PERFORMANCE.radiatorSeats(List.of(AIO, AIO)));
        assertEquals(Set.of(1, 2), CaseCooling.NEUTRAL.radiatorSeats(Arrays.asList(null, AIO)));
        assertTrue(CaseCooling.NEUTRAL.radiatorSeats(List.of(TOWER)).isEmpty());
    }

    @Test
    void radiatorSide_followsThePairEachWaterCoolerTakes() {
        assertEquals(CaseCooling.Side.TOP, CaseCooling.HIGH_PERFORMANCE.radiatorSide(List.of(AIO, AIO), 0));
        assertEquals(CaseCooling.Side.FRONT, CaseCooling.HIGH_PERFORMANCE.radiatorSide(List.of(AIO, AIO), 1));
        // A tower before it takes no pair: the water cooler over the second socket still takes the first one.
        assertEquals(CaseCooling.Side.TOP, CaseCooling.HIGH_PERFORMANCE.radiatorSide(List.of(TOWER, AIO), 1));
        assertEquals(CaseCooling.Side.FRONT, CaseCooling.AESTHETIC.radiatorSide(Arrays.asList(null, AIO), 1));
        assertNull(CaseCooling.NEUTRAL.radiatorSide(List.of(TOWER), 0));
        assertNull(CaseCooling.NEUTRAL.radiatorSide(List.of(AIO, AIO), 1), "one pair, taken by the first");
    }

    @Test
    void budgetWatts_addsTheCaseTheCoolersAndTheFans() {
        // Neutral, a stock cooler and three fans: 500 W.
        assertEquals(500, CaseCooling.NEUTRAL.budgetWatts(List.of(STOCK), List.of(FAN, FAN, FAN)));
        // High Performance, a 240 mm water cooler on the top pair, a rear and three front fans: 800 W.
        assertEquals(800, CaseCooling.HIGH_PERFORMANCE.budgetWatts(List.of(AIO),
                Arrays.asList(FAN, null, null, FAN, FAN, FAN)));
        // No cooler and no fans: the case's own 200 W.
        assertEquals(200, CaseCooling.NEUTRAL.budgetWatts(Arrays.asList((CoolerSpec) null), List.of()));
    }

    @Test
    void constructor_refusesLessThanNothing() {
        assertThrows(IllegalArgumentException.class, () -> new CaseCooling(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new CaseCooling(10, 0, -1, 0));
    }
}
