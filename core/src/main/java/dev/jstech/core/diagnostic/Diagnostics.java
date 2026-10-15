/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/**
 * The game's register of how long the series' work takes, read on the debug screen (F3) of a player's game. A piece of
 * work is timed by its id, around the work, where it runs:
 *
 * <pre>{@code
 * long start = System.nanoTime();
 * ...
 * Diagnostics.record(ENERGY_GRID, start);
 * }</pre>
 *
 * <p>One register serves the whole game: on a player's own world it holds the world's work as well as the screen's;
 * on a server it holds the server's, which a player's game does not see.
 *
 * <p>A {@link Section} is timed here by itself as it runs, and is named in profilers, watched for stalls and kept to
 * its budget besides: the way to declare a system's work.
 */
public final class Diagnostics {

    private static final Timings TIMINGS = new Timings();
    private static final double NANOS_PER_MILLI = 1_000_000.0;

    private Diagnostics() {
    }

    /** Every piece of work timed. */
    public static Timings timings() {
        return TIMINGS;
    }

    /** Keeps that {@code section} ran from {@code startNanos}, read from {@link System#nanoTime()}, until now. */
    public static void record(final ResourceLocation section, final long startNanos) {
        TIMINGS.record(section.toString(), System.nanoTime() - startNanos);
    }

    /**
     * A time in milliseconds to three places, the way the server writes it into its log, its files and its commands'
     * answers: {@code 0.420}.
     */
    public static String millis(final long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / NANOS_PER_MILLI);
    }
}
