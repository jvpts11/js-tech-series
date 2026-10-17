/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A dimension's own clock: how long its day lasts, whether it is locked to its star with its sun held still, and how
 * many of its days its year lasts. A dimension with one keeps its own time, apart from the overworld's, which no rule
 * ever touches; one without reads the overworld's clock, as the game's own dimensions do.
 *
 * <pre>{@code
 * "clock": {"day": 25200, "year": 687}          // a day of 21 minutes, a year of 687 of them
 * "clock": {"locked_at": 6000}                    // locked to its star, the sun held at noon
 * }</pre>
 *
 * @param day      how long its day lasts, in ticks: the overworld's lasts 24,000
 * @param lockedAt for a world locked to its star, the time of day its sun is held at, 0 to 23,999; -1 for one that
 *                 turns
 * @param year     how many of its days its year lasts, its seasons a quarter of it each; 0 for the seasons the world's
 *                 balance sets
 */
public record DimensionClock(int day, int lockedAt, int year) {

    /** How long the overworld's day lasts, in ticks: what every day of the game's clock is counted in. */
    public static final int OVERWORLD_DAY = 24_000;
    /** The shortest a day may last: a minute. */
    public static final int SHORTEST_DAY = 1_200;
    /** The longest a day may last: a thousand of the overworld's. */
    public static final int LONGEST_DAY = OVERWORLD_DAY * 1_000;
    /** The longest a year may last, in its days. */
    public static final int LONGEST_YEAR = 100_000;
    /** Not locked: the world turns. */
    public static final int TURNING = -1;
    public static final Codec<DimensionClock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(SHORTEST_DAY, LONGEST_DAY).optionalFieldOf("day", OVERWORLD_DAY)
                    .forGetter(DimensionClock::day),
            Codec.intRange(TURNING, OVERWORLD_DAY - 1).optionalFieldOf("locked_at", TURNING)
                    .forGetter(DimensionClock::lockedAt),
            Codec.intRange(0, LONGEST_YEAR).optionalFieldOf("year", 0).forGetter(DimensionClock::year))
            .apply(instance, DimensionClock::new));

    public DimensionClock {
        day = Math.clamp(day, SHORTEST_DAY, LONGEST_DAY);
        lockedAt = Math.clamp(lockedAt, TURNING, OVERWORLD_DAY - 1);
        year = Math.clamp(year, 0, LONGEST_YEAR);
    }

    /** Whether it is locked to its star, its sun never moving. */
    public boolean locked() {
        return this.lockedAt >= 0;
    }

    /**
     * How far its clock moves a tick, counted as the game's clock is (24,000 to a day): 1 for a day as long as the
     * overworld's, 2 for one half as long, none for a world locked to its star.
     */
    public float perTick() {
        return locked() ? 0F : (float) OVERWORLD_DAY / this.day;
    }
}
