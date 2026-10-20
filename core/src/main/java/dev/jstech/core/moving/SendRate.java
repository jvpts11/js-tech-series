/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

/**
 * How often a player is told where a moving structure is, by how far away they are: every tick near it, where a
 * deck is walked on and every wobble shows, and less often the farther it is, where a player's game fills the gaps
 * from its speed. Pure: no part of the game.
 */
public final class SendRate {

    /** Within this many blocks a player is told every tick. */
    public static final double NEAR = 64;
    /** The longest a player far away waits between two poses, in ticks. */
    public static final int LONGEST = 8;

    private SendRate() {
    }

    /** Every how many ticks a player {@code distance} blocks from a moving structure is told its pose. */
    public static int every(final double distance) {
        int every = 1;
        double reach = NEAR;
        while (distance > reach && every < LONGEST) {
            every *= 2;
            reach *= 2;
        }
        return every;
    }
}
