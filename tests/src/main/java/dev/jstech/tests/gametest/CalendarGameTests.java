/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.time.GameCalendar;
import dev.jstech.core.time.Season;
import dev.jstech.core.time.WorldCalendar;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The world's calendar: the moment a level's clock shows, read the same way a schedule reads it, and the season the
 * world's balance sets.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CalendarGameTests {

    private static final String ARENA = "empty";

    private CalendarGameTests() {
    }

    @GameTest(template = ARENA)
    public static void worldCalendar_readsTheLevelsClockAsASchedulesDoes(final GameTestHelper helper) {
        final long dayTime = helper.getLevel().getDayTime();
        final WorldCalendar.Moment now = WorldCalendar.at(dayTime);

        helper.assertTrue(now.hour() == GameCalendar.hourOf(dayTime) && now.minute() == GameCalendar.minuteOf(dayTime),
                "the hour and minute a screen shows are the ones a schedule reads");
        helper.assertTrue(now.day() == GameCalendar.day(dayTime) && now.dayOfWeek() == GameCalendar.dayOfWeek(dayTime),
                "and so are the day and its day of the week");
        helper.assertTrue(now.season() == WorldCalendar.calendar().season(dayTime), "the season is the calendar's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void worldCalendar_takesItsSeasonLengthFromTheBalance(final GameTestHelper helper) {
        WorldCalendar.setDaysPerSeason(2);
        try {
            helper.assertTrue(WorldCalendar.at(0L).season() == Season.SPRING, "the world begins in spring");
            helper.assertTrue(WorldCalendar.at(2L * GameCalendar.DAY_TICKS).season() == Season.SUMMER,
                    "two days on it is summer, when a season lasts two days");
            WorldCalendar.setDaysPerSeason(0);
            helper.assertTrue(WorldCalendar.calendar().daysPerSeason() == 1, "a season lasts a day at least");
        } finally {
            WorldCalendar.setDaysPerSeason(WorldCalendar.DEFAULT_DAYS_PER_SEASON);
        }
        helper.succeed();
    }
}
