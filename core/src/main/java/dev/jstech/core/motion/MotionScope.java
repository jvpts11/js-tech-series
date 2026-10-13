/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.motion;

import java.util.Objects;
import java.util.function.Function;

/**
 * Whose motions the things being drawn right now move by. A desktop draws everything on it within its own system's
 * motions, so a part deep inside one of its windows (a progress bar, a text cursor, a bar for a wait) moves the way
 * that system moves without anyone handing it the system. Outside any scope nothing moves.
 *
 * <p>Drawing happens on one thread, the game's, and a scope ends when the drawing inside it does, so scopes nest: a
 * picture drawn for a monitor's face in the middle of a frame moves by its own and hands the frame back after.
 */
public final class MotionScope {

    private static Function<String, MotionSpec> current = kind -> MotionSpec.NONE;

    private MotionScope() {
    }

    /** Runs {@code paint} with every kind of motion read from {@code specs}, then goes back to what it was. */
    public static void within(final Function<String, MotionSpec> specs, final Runnable paint) {
        final Function<String, MotionSpec> before = current;
        current = Objects.requireNonNull(specs, "specs");
        try {
            paint.run();
        } finally {
            current = before;
        }
    }

    /** How that kind of thing moves in the scope being drawn: nothing outside any scope. */
    public static MotionSpec spec(final String kind) {
        final MotionSpec spec = current.apply(kind);
        return spec == null ? MotionSpec.NONE : spec;
    }
}
