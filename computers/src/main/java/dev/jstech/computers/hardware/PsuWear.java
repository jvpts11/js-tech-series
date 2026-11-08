/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;

/**
 * How a power supply wears while it is on: by its load, the parts' watts over its rating. Up to half its rating it
 * lives eight times its full-load life, up to four fifths three times, and above that its full-load life; a redundant
 * supply's working units share the load, so each lives longer until one fails and the rest carry it all. From
 * {@link #DEGRADED} worn a unit warns, and at 1 it fails. The time is worked out when a machine changes state or once a
 * game minute, never each tick. Every number is a first estimate.
 */
@ApiStatus.Experimental
public final class PsuWear {

    /** The wear from which a unit is degraded and warns. */
    public static final float DEGRADED = 0.8f;
    /** A game day, in ticks. */
    public static final long TICKS_A_DAY = 24_000L;
    /** The shortest and longest a supply's own life is, against its line's. */
    public static final float SHORTEST_LIFE = 0.8f;
    public static final float LONGEST_LIFE = 1.2f;
    private static final double LIGHT_LOAD = 0.5;
    private static final double MEDIUM_LOAD = 0.8;
    private static final int LIGHT_LOAD_TIMES = 8;
    private static final int MEDIUM_LOAD_TIMES = 3;

    private PsuWear() {
    }

    /** How many times its full-load life a supply lasts at {@code load}, its parts' watts over its rating. */
    public static int lifeTimes(final double load) {
        if (load <= LIGHT_LOAD) {
            return LIGHT_LOAD_TIMES;
        }
        return load <= MEDIUM_LOAD ? MEDIUM_LOAD_TIMES : 1;
    }

    /** The load each of {@code working} units of {@code spec} carries for {@code partsWatts}. */
    public static double unitLoad(final PsuSpec spec, final int partsWatts, final int working) {
        return working <= 0 ? 0.0 : (double) partsWatts / spec.wattage() / working;
    }

    /** The load each working unit of {@code spec} in {@code condition} carries for {@code partsWatts}. */
    public static double unitLoad(final PsuSpec spec, final PsuCondition condition, final int partsWatts) {
        return unitLoad(spec, partsWatts, spec.units() - condition.failedUnits(spec));
    }

    /** Its own life drawn from {@code roll}, a number from 0 to 1: between the shortest and the longest. */
    public static float drawLife(final double roll) {
        final double clamped = Math.max(0.0, Math.min(1.0, roll));
        return (float) (SHORTEST_LIFE + (LONGEST_LIFE - SHORTEST_LIFE) * clamped);
    }

    /**
     * {@code condition} after {@code ticks} on giving {@code partsWatts}, every life stretched by {@code multiplier}
     * (the server's setting, halved in a rack over its thermal budget): each working unit wears by the time over its
     * life at the load it carries.
     */
    public static PsuCondition worn(final PsuCondition condition, final PsuSpec spec, final int partsWatts,
                                    final long ticks, final double multiplier) {
        if (ticks <= 0L || multiplier <= 0.0 || condition.failed(spec)) {
            return condition;
        }
        final double load = unitLoad(spec, condition, partsWatts);
        final double life = spec.lifeDays() * (double) TICKS_A_DAY * lifeTimes(load)
                * (condition.life() > 0f ? condition.life() : 1.0) * multiplier;
        final List<Float> wear = new ArrayList<>(spec.units());
        for (int unit = 0; unit < spec.units(); unit++) {
            final float now = condition.wearOf(unit);
            wear.add(now >= 1f ? 1f : (float) Math.min(1.0, now + ticks / life));
        }
        return new PsuCondition(condition.life(), wear);
    }
}
