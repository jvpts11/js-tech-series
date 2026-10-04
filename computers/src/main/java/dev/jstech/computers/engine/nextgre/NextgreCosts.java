/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.nextgre;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What NextgreIQL reckons a plan will take, in game ticks, which is the cost it weighs plans by.
 *
 * <p>A bench step takes the work of its runs at the speed of the computers that craft it; a machine step takes its
 * runs at the time each run was measured to take on this network, or a furnace's time when nothing was measured; a
 * pull takes the store's latency and then its items at what the store and the cable to it carry. A step can only
 * begin once what it is made of is there, and the steps beneath it run side by side on as many lanes as the craft
 * may use at once. Pure arithmetic, so its tests run without the game.
 */
public final class NextgreCosts {

    /** A machine run's time when the network never measured that recipe: what a furnace takes to smelt one item. */
    public static final long DEFAULT_MACHINE_TICKS = 200L;

    /** What a store is taken to carry a tick when nothing on the way says how much. */
    public static final long DEFAULT_ITEMS_PER_TICK = 64L;

    private NextgreCosts() {
    }

    /** {@code runs} of a bench recipe whose run handles {@code unitsPerRun} units, at {@code unitsPerTick}. */
    public static long bench(final long runs, final long unitsPerRun, final long unitsPerTick) {
        return ceilDiv(Math.max(0L, runs) * Math.max(1L, unitsPerRun), Math.max(1L, unitsPerTick));
    }

    /** {@code runs} of a machine recipe of {@code ticksPerRun} each, on {@code lanes} machines at once. */
    public static long machine(final long runs, final long ticksPerRun, final int lanes) {
        return ceilDiv(Math.max(0L, runs), Math.max(1, lanes)) * Math.max(1L, ticksPerRun);
    }

    /** {@code amount} items from a store that answers after {@code latency} ticks and carries {@code perTick}. */
    public static long pull(final long amount, final long perTick, final int latency) {
        if (amount <= 0L) {
            return 0L;
        }
        return Math.max(0, latency) + ceilDiv(amount, Math.max(1L, perTick));
    }

    /**
     * How long it takes for every one of {@code children} to be done when at most {@code lanes} run at once: each
     * goes to the lane that frees up first, the longest first, which is how a careful foreman hands out work.
     */
    public static long makespan(final List<Long> children, final int lanes) {
        if (children.isEmpty()) {
            return 0L;
        }
        final List<Long> longestFirst = new ArrayList<>(children);
        longestFirst.sort(Comparator.reverseOrder());
        final long[] busy = new long[Math.max(1, Math.min(lanes, longestFirst.size()))];
        for (final long child : longestFirst) {
            int freest = 0;
            for (int i = 1; i < busy.length; i++) {
                if (busy[i] < busy[freest]) {
                    freest = i;
                }
            }
            busy[freest] += Math.max(0L, child);
        }
        long most = 0L;
        for (final long lane : busy) {
            most = Math.max(most, lane);
        }
        return most;
    }

    /** A step that takes {@code own} once {@code children} are done, on {@code lanes} lanes. */
    public static long node(final long own, final List<Long> children, final int lanes) {
        return Math.max(0L, own) + makespan(children, lanes);
    }

    private static long ceilDiv(final long a, final long b) {
        return a <= 0L ? 0L : (a + b - 1L) / b;
    }
}
