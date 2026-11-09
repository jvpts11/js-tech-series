/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class PortableBatteryTest {

    @Test
    void capacity_lastsTwoGameDaysOnAPhoneAndADayAndAHalfOnAPda() {
        // A game day is twenty minutes of the server running; a device on draws a watt.
        assertEquals(2 * 1_200, PortableBattery.secondsLeft(PortableBattery.capacity(PortableModel.QPHONE)));
        assertEquals(1_800, PortableBattery.secondsLeft(PortableBattery.capacity(PortableModel.HIGHTIDE_POCKET)));
    }

    @Test
    void percent_showsOneForANearlyFlatBatteryAndNoneForAFlatOne() {
        final int capacity = PortableBattery.capacity(PortableModel.QPHONE);
        assertEquals(0, PortableBattery.percent(0, capacity));
        assertEquals(1, PortableBattery.percent(1, capacity));
        assertEquals(64, PortableBattery.percent(capacity * 64 / 100, capacity));
        assertEquals(100, PortableBattery.percent(capacity, capacity));
    }

    @Test
    void barWidth_fillsThe13PixelsOfTheGamesBar() {
        assertEquals(0, PortableBattery.barWidth(0.0F));
        assertEquals(13, PortableBattery.barWidth(1.0F));
        assertEquals(7, PortableBattery.barWidth(0.5F));
        assertEquals(13, PortableBattery.barWidth(2.0F));
    }

    @Test
    void level_isGoodAboveHalfLowAboveAFifthCriticalBelow() {
        assertSame(PortableBattery.Level.GOOD, PortableBattery.level(64));
        assertSame(PortableBattery.Level.LOW, PortableBattery.level(50));
        assertSame(PortableBattery.Level.LOW, PortableBattery.level(21));
        assertSame(PortableBattery.Level.CRITICAL, PortableBattery.level(12));
    }

    @Test
    void charger_givesEachErasWatts() {
        assertEquals(5, PortableBattery.chargerWatts(HardwareEra.LEGACY));
        assertEquals(5, PortableBattery.chargerWatts(HardwareEra.TRANSITION));
        assertEquals(10, PortableBattery.chargerWatts(HardwareEra.STANDARD));
        assertEquals(15, PortableBattery.chargerWatts(HardwareEra.ADVANCED));
        assertEquals(250, PortableBattery.chargedATick(HardwareEra.TRANSITION));
    }
}
