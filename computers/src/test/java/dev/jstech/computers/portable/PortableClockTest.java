/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PortableClockTest {

    @Test
    void time_startsTheWorldsDayAtSixInTheMorning() {
        assertEquals("06:00", PortableClock.time(0L));
        assertEquals("12:00", PortableClock.time(6_000L));
        assertEquals("00:00", PortableClock.time(18_000L));
        assertEquals("07:42", PortableClock.time(1_700L));
    }

    @Test
    void day_countsFromOne() {
        assertEquals(1L, PortableClock.day(0L));
        assertEquals(1L, PortableClock.day(23_999L));
        assertEquals(128L, PortableClock.day(127L * 24_000L + 5L));
    }
}
