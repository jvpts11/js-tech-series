/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ClientTestShardsTest {

    private static final Function<String, String> NAME = Function.identity();

    @Test
    void shard_withNoDurationsDealsTheTestsRoundRobin() {
        final List<String> all = List.of("a", "b", "c", "d", "e");

        assertEquals(List.of("a", "c", "e"), ClientTestShards.shard(all, 0, 2, Map.of(), NAME));
        assertEquals(List.of("b", "d"), ClientTestShards.shard(all, 1, 2, Map.of(), NAME));
    }

    @Test
    void shard_givesTheLongestTestsToTheShardWithTheLeastToDo() {
        final List<String> all = List.of("long", "mid", "short1", "short2");
        final Map<String, Integer> ticks = Map.of("long", 900, "mid", 500, "short1", 300, "short2", 200);

        final List<String> first = ClientTestShards.shard(all, 0, 2, ticks, NAME);
        final List<String> second = ClientTestShards.shard(all, 1, 2, ticks, NAME);

        assertEquals(List.of("long"), first);
        assertEquals(List.of("mid", "short1", "short2"), second);
        assertEquals(900L, ClientTestShards.expectedTicks(first, ticks, NAME));
        assertEquals(1000L, ClientTestShards.expectedTicks(second, ticks, NAME));
    }

    @Test
    void shard_runsEveryTestOnceAcrossTheShardsInTheOrderGiven() {
        final List<String> all = new ArrayList<>();
        for (int i = 0; i < 37; i++) {
            all.add("test" + i);
        }
        final Map<String, Integer> ticks = Map.of("test3", 4000, "test10", 50);
        final Set<String> seen = new HashSet<>();
        int total = 0;
        for (int shard = 0; shard < 4; shard++) {
            final List<String> slice = ClientTestShards.shard(all, shard, 4, ticks, NAME);
            for (int i = 1; i < slice.size(); i++) {
                assertTrue(all.indexOf(slice.get(i - 1)) < all.indexOf(slice.get(i)), "kept in order: " + slice);
            }
            seen.addAll(slice);
            total += slice.size();
        }

        assertEquals(37, total);
        assertEquals(37, seen.size());
    }

    @Test
    void expectedTicks_countsATestNeverMeasuredAsTheAverageOfTheMeasured() {
        final Map<String, Integer> ticks = Map.of("a", 100, "b", 300);

        assertEquals(100L + 300L + 200L, ClientTestShards.expectedTicks(List.of("a", "b", "c"), ticks, NAME));
    }
}
