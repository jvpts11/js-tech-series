/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import java.util.Locale;

/**
 * The world's time and day as the portables show them (computers/docs/design/wireless.md): the time on the
 * twenty-four hour clock, the world's day starting at six in the morning, and the date as the day's number, counting
 * from one.
 *
 * <p>Pure: no part of the game.
 */
public final class PortableClock {

    private static final long TICKS_A_DAY = 24_000L;
    private static final long TICKS_AN_HOUR = 1_000L;
    private static final int MINUTES_AN_HOUR = 60;
    private static final int MINUTES_A_DAY = 24 * MINUTES_AN_HOUR;
    /* The world's day starts at six in the morning. */
    private static final int DAWN = 6 * MINUTES_AN_HOUR;

    private PortableClock() {
    }

    /** The time of day {@code dayTime} as {@code 07:42}. */
    public static String time(final long dayTime) {
        final long into = Math.floorMod(dayTime, TICKS_A_DAY);
        final int minutes = (int) ((into * MINUTES_AN_HOUR / TICKS_AN_HOUR + DAWN) % MINUTES_A_DAY);
        return String.format(Locale.ROOT, "%02d:%02d", minutes / MINUTES_AN_HOUR, minutes % MINUTES_AN_HOUR);
    }

    /** The day of {@code dayTime}, counting from one. */
    public static long day(final long dayTime) {
        return Math.floorDiv(dayTime, TICKS_A_DAY) + 1;
    }
}
