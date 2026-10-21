/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * How a run's client tests are shared out over its shards, the windows that run side by side. Knowing how many ticks
 * each test took last time, the longest go first, each to the shard with the least to do so far, so the shards end
 * together and none waits on the one that drew the long tests; a test never measured counts as the measured ones'
 * average. Knowing nothing, round-robin. Every shard reads the same durations, so each takes its own slice of one
 * split and no test runs twice or not at all.
 *
 * <p>Pure: no game types, so the split is tested on its own.
 */
public final class ClientTestShards {

    private ClientTestShards() {
    }

    /**
     * The slice of {@code all}, in its order, that shard {@code shard} of {@code shards} runs, by the ticks each test
     * took last time, a test known by {@code name}.
     */
    public static <T> List<T> shard(final List<T> all, final int shard, final int shards,
                                    final Map<String, Integer> ticks, final Function<T, String> name) {
        final List<T> mine = new ArrayList<>();
        if (ticks.isEmpty()) {
            for (int i = 0; i < all.size(); i++) {
                if (i % shards == shard) {
                    mine.add(all.get(i));
                }
            }
            return mine;
        }
        final int unknown = averageOf(ticks);
        final List<T> longestFirst = new ArrayList<>(all);
        longestFirst.sort(Comparator.comparingInt((T test) -> -ticks.getOrDefault(name.apply(test), unknown))
                .thenComparing(name));
        final long[] load = new long[shards];
        final Set<T> chosen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final T test : longestFirst) {
            int least = 0;
            for (int other = 1; other < shards; other++) {
                if (load[other] < load[least]) {
                    least = other;
                }
            }
            load[least] += ticks.getOrDefault(name.apply(test), unknown);
            if (least == shard) {
                chosen.add(test);
            }
        }
        for (final T test : all) {
            if (chosen.contains(test)) {
                mine.add(test);
            }
        }
        return mine;
    }

    /** The ticks a slice is expected to take, from the same durations its split was made with. */
    public static <T> long expectedTicks(final List<T> slice, final Map<String, Integer> ticks,
                                         final Function<T, String> name) {
        final int unknown = averageOf(ticks);
        return slice.stream().mapToLong(test -> ticks.getOrDefault(name.apply(test), unknown)).sum();
    }

    private static int averageOf(final Map<String, Integer> ticks) {
        return (int) ticks.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }
}
