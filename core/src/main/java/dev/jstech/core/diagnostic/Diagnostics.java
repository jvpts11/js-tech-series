/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

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
 */
public final class Diagnostics {

    private static final Timings TIMINGS = new Timings();

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
}
