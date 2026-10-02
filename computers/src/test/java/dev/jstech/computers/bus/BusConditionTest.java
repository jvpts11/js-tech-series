/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BusConditionTest {

    @Test
    void holdsAt_takesTheHoursOfADay() {
        final BusCondition day = BusCondition.hours(6, 18);

        assertTrue(day.holdsAt(6));
        assertTrue(day.holdsAt(17));
        assertFalse(day.holdsAt(18));
        assertFalse(day.holdsAt(3));
    }

    @Test
    void holdsAt_runsPastMidnight() {
        final BusCondition night = BusCondition.hours(18, 6);

        assertTrue(night.holdsAt(22));
        assertTrue(night.holdsAt(2));
        assertFalse(night.holdsAt(12));
    }

    @Test
    void holdsAt_holdsAllDayWhenTheHoursMeet() {
        assertTrue(BusCondition.hours(5, 5).holdsAt(23));
    }

    @Test
    void hours_wrapsHoursPastTheDay() {
        assertEquals(1, BusCondition.hours(25, 0).fromHour());
    }

    @Test
    void onTag_isAStockOfATag() {
        assertTrue(BusCondition.stock("#c:ores", 4096).onTag());
        assertFalse(BusCondition.stock("minecraft:iron_ore", 512).onTag());
        assertFalse(BusCondition.after("Coal in").onTag());
    }
}
