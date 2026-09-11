/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import java.util.Arrays;

/**
 * What a Gateway served in the last minute: calls answered, operations started, files touched. Counted in
 * twelve buckets of five seconds each, so "this minute" is a rolling window rather than a counter that
 * drops to zero on the minute.
 */
public final class GatewayStats {

    /** What was served. */
    public enum Kind {
        CALL,
        OPERATION,
        FILE
    }

    private static final int BUCKETS = 12;
    private static final long BUCKET_TICKS = 100L;

    private final int[][] counts = new int[Kind.values().length][BUCKETS];
    private final long[] bucketStart = new long[BUCKETS];

    public GatewayStats() {
        Arrays.fill(bucketStart, Long.MIN_VALUE);
    }

    /** Counts one served {@code kind} at {@code tick}. */
    public void count(final Kind kind, final long tick) {
        final int bucket = bucketOf(tick);
        final long start = Math.floorDiv(tick, BUCKET_TICKS) * BUCKET_TICKS;
        if (bucketStart[bucket] != start) {
            // The bucket last held a slice of an older minute: it starts over for this one.
            bucketStart[bucket] = start;
            for (final int[] row : counts) {
                row[bucket] = 0;
            }
        }
        counts[kind.ordinal()][bucket]++;
    }

    /** How many of {@code kind} were served in the minute ending at {@code tick}. */
    public int lastMinute(final Kind kind, final long tick) {
        final long oldest = Math.floorDiv(tick, BUCKET_TICKS) * BUCKET_TICKS - BUCKET_TICKS * (BUCKETS - 1);
        int sum = 0;
        for (int i = 0; i < BUCKETS; i++) {
            if (bucketStart[i] >= oldest && bucketStart[i] <= tick) {
                sum += counts[kind.ordinal()][i];
            }
        }
        return sum;
    }

    public void reset() {
        for (final int[] row : counts) {
            Arrays.fill(row, 0);
        }
        Arrays.fill(bucketStart, Long.MIN_VALUE);
    }

    private static int bucketOf(final long tick) {
        return (int) Math.floorMod(Math.floorDiv(tick, BUCKET_TICKS), (long) BUCKETS);
    }
}
