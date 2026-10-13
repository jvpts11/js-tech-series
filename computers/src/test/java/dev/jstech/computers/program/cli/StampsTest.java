/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StampsTest {

    @Test
    void of_readsTheSameDayAndClockAsTheSharedHelpers() {
        final long ticks = 24_000L + 2_250L;

        assertEquals("Day " + Stamps.day(ticks) + "  " + Stamps.clock(ticks), Stamps.of(ticks));
    }

    @Test
    void day_countsFromZeroAtTheFirstMorning() {
        assertEquals(0L, Stamps.day(23_999L));
        assertEquals(1L, Stamps.day(24_000L));
    }

    @Test
    void clock_startsTheDayAtSixInTheMorning() {
        assertEquals("06:00", Stamps.clock(0L));
        assertEquals("08:15", Stamps.clock(2_250L));
        assertEquals("06:00", Stamps.clock(24_000L));
    }

    @Test
    void of_saysDashesForAnUnknownMoment() {
        assertEquals("  --  ", Stamps.of(0L));
    }
}
