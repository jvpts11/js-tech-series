/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.time;

/**
 * The world's own calendar, read from the world's clock (its day time, in ticks): the day, the hour and the minute, the
 * day of the week and the week, the season, the day of the season and the year. Everything a player would set a
 * schedule by is the world's: dawn, dusk, the hour the farm is full, the first frost.
 *
 * <p>A day is twenty-four thousand ticks, twenty minutes of play, and starts at six in the morning, which is where the
 * world's clock starts counting; an hour is a thousand ticks. A week is seven days. A year is four seasons of the same
 * number of days, which the world's balance sets. Pure, and the one place the arithmetic lives, so a schedule and a
 * clock on a screen cannot fall out of step.
 *
 * @param daysPerSeason how many days a season lasts
 */
public record GameCalendar(int daysPerSeason) {

    /** Ticks in a day of the world, daylight and dark together. */
    public static final int DAY_TICKS = 24_000;
    /** Ticks in an hour of it. */
    public static final int HOUR_TICKS = 1_000;
    /** The hour a day starts at: the world's clock starts counting at six in the morning. */
    public static final int DAY_STARTS_AT = 6;
    /** Days in a week. */
    public static final int DAYS_PER_WEEK = 7;
    private static final int HOURS = 24;
    private static final int MINUTES = 60;

    public GameCalendar {
        if (daysPerSeason <= 0) {
            throw new IllegalArgumentException("a season lasts a day at least, not " + daysPerSeason);
        }
    }

    /** Which day it is, counted from the world's first, nought. */
    public static long day(final long dayTime) {
        return Math.floorDiv(dayTime, DAY_TICKS);
    }

    /** The hour the clock shows, from midnight: 0 to 23. */
    public static int hourOf(final long dayTime) {
        return (int) ((Math.floorMod(dayTime, DAY_TICKS) / HOUR_TICKS + DAY_STARTS_AT) % HOURS);
    }

    /** The minute of the hour the clock shows: 0 to 59. */
    public static int minuteOf(final long dayTime) {
        return (int) (Math.floorMod(dayTime, HOUR_TICKS) * MINUTES / HOUR_TICKS);
    }

    /** Which day of the week it is, from 0 for the first. */
    public static int dayOfWeek(final long dayTime) {
        return (int) Math.floorMod(day(dayTime), DAYS_PER_WEEK);
    }

    /** Which week it is, counted from the world's first, nought. */
    public static long week(final long dayTime) {
        return Math.floorDiv(day(dayTime), DAYS_PER_WEEK);
    }

    /** How many whole hours passed between two readings of the clock. */
    public static long hoursBetween(final long from, final long to) {
        return (to - from) / HOUR_TICKS;
    }

    /** The season it is. */
    public Season season(final long dayTime) {
        return Season.at(Math.floorDiv(day(dayTime), this.daysPerSeason));
    }

    /** Which day of its season it is, from 0 for the first. */
    public int dayOfSeason(final long dayTime) {
        return (int) Math.floorMod(day(dayTime), this.daysPerSeason);
    }

    /** Which year it is, counted from the world's first, nought. */
    public long year(final long dayTime) {
        return Math.floorDiv(day(dayTime), (long) this.daysPerSeason * Season.perYear());
    }
}
