/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

/**
 * How long replacing a network's engine takes, and which of its steps it is on at a moment.
 *
 * <p>A replacement is not instant. The new engine has to find the network's storage and machines and build its
 * indexes before it plans anything, and the more item types the network holds, the longer the indexes take. While it
 * runs the network has no engine: what is in flight carries on with the plans it started with, transfers still work,
 * and anything new that needs planning is refused until the new engine is ready.
 */
public final class EngineSwap {

    /** The least a replacement takes, however little the network holds: two seconds. */
    public static final int BASE_TICKS = 40;
    /** How many item types the new engine indexes in one tick. */
    public static final int TYPES_PER_TICK = 4;
    /** The most a replacement takes, however much the network holds: two minutes. */
    public static final int MAX_TICKS = 2_400;

    private EngineSwap() {
    }

    /**
     * The steps a replacement goes through, in order, each with where it ends in thousandths of the whole. The first
     * four are quick bookkeeping; finding the storage and the machines takes a fifth of the time, and building the
     * indexes most of the rest. The last is reached when it is over, and never ends.
     */
    public enum Step {
        STOP_NEW_PLANS(20),
        KEEP_IN_FLIGHT(40),
        STOP_OLD(60),
        START_NEW(80),
        DISCOVER(300),
        BUILD_INDEXES(900),
        PUBLISH(1_000),
        READY(Integer.MAX_VALUE);

        private final int end;

        Step(final int end) {
            this.end = end;
        }

        /** Whether this step comes before {@code other}. */
        public boolean before(final Step other) {
            return end < other.end;
        }
    }

    /** How many ticks replacing the engine of a network holding {@code itemTypes} kinds of item takes. */
    public static int ticksFor(final int itemTypes) {
        final long ticks = BASE_TICKS + (long) Math.max(0, itemTypes) / TYPES_PER_TICK;
        return (int) Math.min(MAX_TICKS, ticks);
    }

    /** The step a replacement of {@code total} ticks is on {@code elapsed} ticks in. */
    public static Step stepAt(final long elapsed, final int total) {
        final int done = permille(elapsed, total);
        for (final Step step : Step.values()) {
            if (done < step.end) {
                return step;
            }
        }
        return Step.READY;
    }

    /** How far through a replacement of {@code total} ticks it is {@code elapsed} ticks in, in thousandths. */
    public static int permille(final long elapsed, final int total) {
        if (total <= 0) {
            return 1_000;
        }
        return (int) Math.max(0L, Math.min(1_000L, elapsed * 1_000L / total));
    }
}
