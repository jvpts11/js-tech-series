/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.Locale;

/**
 * When a file was last written, said the way a person reads it.
 *
 * <p>There is one clock in this world and every listing tells it the same way, whichever family's verb asked
 * for the listing. It lives on its own so that stays true: a stamp written by one shell and read at another
 * is the same stamp, not two spellings of one.
 */
final class Stamps {

    /** How many ticks a day is here. */
    private static final long DAY = 24_000L;

    /** How many a single hour is. */
    private static final long HOUR = 1_000L;

    /** The hour the world's day begins at, since tick zero is six in the morning. */
    private static final long DAWN = 6L;

    private Stamps() {
    }

    /**
     * A file's world-time stamp as a day and a clock, for example {@code Day 12  08:15}.
     *
     * <p>A stamp of nought is a file whose hour nobody knows: one the system itself put there, or one
     * written before any of them were stamped. It says so rather than claiming the first morning.
     */
    static String of(final long ticks) {
        if (ticks <= 0L) {
            return "  --  ";
        }
        final long timeOfDay = ticks % DAY;
        final long hour = ((timeOfDay / HOUR) + DAWN) % 24L;
        final long minute = (timeOfDay % HOUR) * 60L / HOUR;
        return String.format(Locale.ROOT, "Day %d  %02d:%02d", ticks / DAY, hour, minute);
    }
}
