/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.time;

import dev.jstech.core.dimension.DayClocks;
import dev.jstech.core.dimension.DimensionClock;
import net.minecraft.world.level.Level;

/**
 * The calendar of a world as it stands: the moment its clock shows, read with the season length the world's balance
 * sets, or by the year of the dimension's own clock, and where its world stands on its orbit. Every side reads the
 * same clock, so a machine's schedule and a screen's clock agree.
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

    /**
     * The moment {@code level}'s clock shows, by its own calendar: a dimension whose clock has a year of its own has
     * seasons a quarter of it each ({@link #calendarOf}).
     */
    public static Moment now(final Level level) {
        return at(level.getDayTime(), calendarOf(level));
    }

    /** The moment the clock shows at {@code dayTime}, by the calendar the world's balance sets. */
    public static Moment at(final long dayTime) {
        return at(dayTime, calendar());
    }

    /** The moment the clock shows at {@code dayTime}, by {@code calendar}. */
    public static Moment at(final long dayTime, final GameCalendar calendar) {
        return new Moment(GameCalendar.day(dayTime), GameCalendar.hourOf(dayTime), GameCalendar.minuteOf(dayTime),
                GameCalendar.dayOfWeek(dayTime), GameCalendar.week(dayTime), calendar.season(dayTime),
                calendar.dayOfSeason(dayTime), calendar.year(dayTime));
    }

    /**
     * The calendar of {@code level}: for a dimension whose clock has a year of its own, seasons of a quarter of it
     * each, to the nearest day; for any other, the one the world's balance sets.
     */
    public static GameCalendar calendarOf(final Level level) {
        final int year = yearOf(level);
        return year > 0 ? new GameCalendar(Math.max(1, Math.round(year / (float) Season.perYear()))) : calendar();
    }

    /**
     * Where {@code level}'s world stands on its orbit round its star: 0 as its year begins, a half halfway round, by
     * its own year when its clock has one, else by the year of four seasons the world's balance sets. Pure arithmetic
     * on the clock, so every side reads the same.
     */
    public static double orbit(final Level level) {
        final int year = yearOf(level);
        final long days = year > 0 ? year : (long) calendar().daysPerSeason() * Season.perYear();
        final long ticks = days * GameCalendar.DAY_TICKS;
        return Math.floorMod(level.getDayTime(), ticks) / (double) ticks;
    }

    /* The days of {@code level}'s own year, or 0 when its clock has none. */
    private static int yearOf(final Level level) {
        return DayClocks.of(level).map(DimensionClock::year).orElse(0);
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
