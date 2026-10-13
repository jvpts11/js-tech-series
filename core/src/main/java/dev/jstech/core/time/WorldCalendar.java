/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.time;

import net.minecraft.world.level.Level;

/**
 * The calendar of a world as it stands: the moment its clock shows, read with the season length the world's balance
 * sets. Every side reads the same clock, so a machine's schedule and a screen's clock agree.
 */
public final class WorldCalendar {

    /** How many days a season lasts unless the world's balance says otherwise. */
    public static final int DEFAULT_DAYS_PER_SEASON = 28;
    /** The longest a season may be set to last, in days. */
    public static final int MOST_DAYS_PER_SEASON = 365;

    private static volatile int daysPerSeason = DEFAULT_DAYS_PER_SEASON;

    private WorldCalendar() {
    }

    /** The calendar the world's balance sets. */
    public static GameCalendar calendar() {
        return new GameCalendar(daysPerSeason);
    }

    /** Sets how many days a season lasts, as the world's balance file says. */
    public static void setDaysPerSeason(final int days) {
        daysPerSeason = Math.clamp(days, 1, MOST_DAYS_PER_SEASON);
    }

    /** The moment {@code level}'s clock shows. */
    public static Moment now(final Level level) {
        return at(level.getDayTime());
    }

    /** The moment the clock shows at {@code dayTime}. */
    public static Moment at(final long dayTime) {
        final GameCalendar calendar = calendar();
        return new Moment(GameCalendar.day(dayTime), GameCalendar.hourOf(dayTime), GameCalendar.minuteOf(dayTime),
                GameCalendar.dayOfWeek(dayTime), GameCalendar.week(dayTime), calendar.season(dayTime),
                calendar.dayOfSeason(dayTime), calendar.year(dayTime));
    }

    /**
     * A moment of the world's calendar.
     *
     * @param day         the day, from the world's first, nought
     * @param hour        the hour, from midnight
     * @param minute      the minute of the hour
     * @param dayOfWeek   the day of the week, from 0
     * @param week        the week, from the world's first, nought
     * @param season      the season
     * @param dayOfSeason the day of the season, from 0
     * @param year        the year, from the world's first, nought
     */
    public record Moment(long day, int hour, int minute, int dayOfWeek, long week, Season season, int dayOfSeason,
                         long year) {
    }
}
