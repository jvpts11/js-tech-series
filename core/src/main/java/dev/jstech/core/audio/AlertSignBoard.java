/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * The signs a player who asked for alerts on screen sees at the top of it: one per alert, newest first, a few at a
 * time. A sign blinks three times as it comes up, stays four seconds, and points where the alert comes from. The
 * same alert again brings its sign back to the top rather than adding another.
 */
public final class AlertSignBoard {

    private final List<Sign> signs = new ArrayList<>();

    /** How long a sign stays, in ticks. */
    public static final int LIFETIME_TICKS = 80;
    /** How many times it blinks as it comes up. */
    public static final int BLINKS = 3;
    /** How long each half of a blink lasts, in ticks. */
    public static final int BLINK_TICKS = 5;
    /** How many signs show at once. */
    public static final int MOST = 3;
    /** How far ahead a sound must be for its sign to point nowhere, as the game's subtitles judge it. */
    private static final double AHEAD = 0.5;

    /**
     * One alert's sign.
     *
     * @param label  what it says: the alert's subtitle
     * @param x      where the alert comes from
     * @param y      where the alert comes from
     * @param z      where the alert comes from
     * @param placed whether it comes from a place in the world, and so has a direction
     * @param raised the tick it came up
     */
    public record Sign(String label, double x, double y, double z, boolean placed, long raised) {
    }

    /** Puts a sign up for an alert that has just started. */
    public synchronized void raise(final String label, final double x, final double y, final double z,
                                   final boolean placed, final long now) {
        signs.removeIf(sign -> sign.label().equals(label));
        signs.addFirst(new Sign(label, x, y, z, placed, now));
        while (signs.size() > MOST) {
            signs.removeLast();
        }
    }

    /** The signs up at that tick, newest first; the ones whose time is over are taken down. */
    public synchronized List<Sign> showing(final long now) {
        signs.removeIf(sign -> now - sign.raised() >= LIFETIME_TICKS);
        return List.copyOf(signs);
    }

    /** Takes every sign down, as when the player leaves a world. */
    public synchronized void clear() {
        signs.clear();
    }

    /** Whether a sign is lit at that tick: on and off three times as it comes up, then on until it goes. */
    public static boolean lit(final Sign sign, final long now) {
        final long age = now - sign.raised();
        return age >= 2L * BLINKS * BLINK_TICKS || (age / BLINK_TICKS) % 2 == 0;
    }

    /**
     * Where a sign points, from how much the sound lies ahead of the listener and to their right (dot products of
     * the listener's forward and right with the way to the sound): 0 when it is ahead, 1 right, -1 left.
     */
    public static int direction(final double ahead, final double right) {
        if (ahead > AHEAD) {
            return 0;
        }
        return right > 0 ? 1 : right < 0 ? -1 : 0;
    }
}
