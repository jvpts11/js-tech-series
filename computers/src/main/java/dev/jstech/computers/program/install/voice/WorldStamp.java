/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import java.util.Locale;

/**
 * The date and time a tool stamps on what it does, read off the world it is running in.
 *
 * <p>The real tools stamp a calendar date. This world has no calendar, but it has the two things a calendar is
 * made of: a count of days since the first one, and a time of day. So a stamp here reads {@code Day 214
 * 15:01:12}, which is true, where any date would have been made up.
 *
 * @param day    how many days the world has seen, the first being day 0, turning over at midnight
 * @param second how far into that day it is, in the world's own seconds
 */
public record WorldStamp(long day, int second) {

    /** How many ticks a day of the world lasts. */
    private static final long TICKS_A_DAY = 24_000L;

    /** How far past midnight the world's day-time counter starts: its zero is six in the morning. */
    private static final long TICKS_TO_DAWN = 6_000L;

    private static final int SECONDS_A_DAY = 86_400;

    /**
     * The stamp for a reading of the world's day-time counter, the one that keeps counting across days.
     */
    public static WorldStamp of(final long dayTime) {
        final long sinceMidnight = Math.max(0L, dayTime) + TICKS_TO_DAWN;
        return new WorldStamp(sinceMidnight / TICKS_A_DAY,
                (int) (sinceMidnight % TICKS_A_DAY * SECONDS_A_DAY / TICKS_A_DAY));
    }

    /** The stamp that many game ticks later, which is when something started now will have ended. */
    public WorldStamp after(final long ticks) {
        final long seconds = this.second + Math.max(0L, ticks) * SECONDS_A_DAY / TICKS_A_DAY;
        return new WorldStamp(this.day + seconds / SECONDS_A_DAY, (int) (seconds % SECONDS_A_DAY));
    }

    /** The time alone, as a clock shows it. */
    public String time() {
        return String.format(Locale.ROOT, "%02d:%02d:%02d",
                this.second / 3600, this.second / 60 % 60, this.second % 60);
    }

    /** The day and the time, which is as much of a date as the world has. */
    public String dated() {
        return "Day " + this.day + " " + this.time();
    }
}
