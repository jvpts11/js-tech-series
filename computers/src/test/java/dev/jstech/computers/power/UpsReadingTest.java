/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpsReadingTest {

    @Test
    void none_isNoUps() {
        assertFalse(UpsReading.NONE.present());
        assertFalse(UpsReading.NONE.onBattery());
        assertEquals(0, UpsReading.NONE.percent());
    }

    @Test
    void constructor_keepsItsFiguresInRange() {
        final UpsReading reading = new UpsReading(null, 1_500, -4L);
        assertEquals(UpsReading.Mode.NONE, reading.mode());
        assertEquals(1_000, reading.permille());
        assertEquals(0L, reading.minutesLeft());
        assertEquals(0, new UpsReading(UpsReading.Mode.ON_LINE, -3, 0L).permille());
    }

    @Test
    void percent_isTheWholePercent() {
        assertEquals(64, new UpsReading(UpsReading.Mode.ON_BATTERY, 649, 70L).percent());
        assertEquals(100, new UpsReading(UpsReading.Mode.ON_LINE, 1_000, 0L).percent());
    }

    @Test
    void onBattery_onlyWhileTheBatteryCarriesTheLoad() {
        assertTrue(new UpsReading(UpsReading.Mode.ON_BATTERY, 500, 3L).onBattery());
        assertFalse(new UpsReading(UpsReading.Mode.BYPASS, 500, 3L).onBattery());
        assertTrue(new UpsReading(UpsReading.Mode.BYPASS, 500, 3L).present());
    }
}
