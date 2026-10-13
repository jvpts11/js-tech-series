/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GameCalendarTest {

    private static final GameCalendar CALENDAR = new GameCalendar(28);

    @Test
    void hourOf_readsTheWorldsOwnClock() {
        assertEquals(6, GameCalendar.hourOf(0L), "a day starts at six in the morning");
        assertEquals(7, GameCalendar.hourOf(GameCalendar.HOUR_TICKS));
        assertEquals(18, GameCalendar.hourOf(12L * GameCalendar.HOUR_TICKS));
        assertEquals(0, GameCalendar.hourOf(18L * GameCalendar.HOUR_TICKS), "midnight eighteen hours in");
        assertEquals(6, GameCalendar.hourOf((long) GameCalendar.DAY_TICKS), "and round again the next day");
    }

    @Test
    void minuteOf_countsSixtyToTheHour() {
        assertEquals(0, GameCalendar.minuteOf(0L));
        assertEquals(30, GameCalendar.minuteOf(GameCalendar.HOUR_TICKS / 2));
        assertEquals(59, GameCalendar.minuteOf(GameCalendar.HOUR_TICKS - 1));
    }

    @Test
    void dayAndWeek_countFromTheWorldsFirst() {
        assertEquals(0L, GameCalendar.day(GameCalendar.DAY_TICKS - 1));
        assertEquals(1L, GameCalendar.day(GameCalendar.DAY_TICKS));
        assertEquals(6, GameCalendar.dayOfWeek(6L * GameCalendar.DAY_TICKS));
        assertEquals(0, GameCalendar.dayOfWeek(7L * GameCalendar.DAY_TICKS), "a week is seven days");
        assertEquals(2L, GameCalendar.week(15L * GameCalendar.DAY_TICKS));
    }

    @Test
    void season_goesRoundTheYear() {
        assertEquals(Season.SPRING, CALENDAR.season(0L));
        assertEquals(Season.SPRING, CALENDAR.season(27L * GameCalendar.DAY_TICKS));
        assertEquals(Season.SUMMER, CALENDAR.season(28L * GameCalendar.DAY_TICKS));
        assertEquals(Season.WINTER, CALENDAR.season(111L * GameCalendar.DAY_TICKS));
        assertEquals(Season.SPRING, CALENDAR.season(112L * GameCalendar.DAY_TICKS), "a year is four seasons");
        assertEquals(1L, CALENDAR.year(112L * GameCalendar.DAY_TICKS));
        assertEquals(3, CALENDAR.dayOfSeason(31L * GameCalendar.DAY_TICKS));
    }

    @Test
    void hoursBetween_countsWholeHours() {
        assertEquals(0L, GameCalendar.hoursBetween(0L, GameCalendar.HOUR_TICKS - 1));
        assertEquals(25L, GameCalendar.hoursBetween(0L, 25L * GameCalendar.HOUR_TICKS));
    }

    @Test
    void calendar_refusesASeasonOfNoDays() {
        assertThrows(IllegalArgumentException.class, () -> new GameCalendar(0));
    }

    @Test
    void season_hasStableNames() {
        assertEquals("autumn", Season.AUTUMN.serializedName());
        assertEquals(Season.SPRING, Season.at(-4), "round both ways");
    }
}
