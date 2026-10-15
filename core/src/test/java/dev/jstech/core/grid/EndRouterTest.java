/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class EndRouterTest {

    private static final String WATER = "water";
    private static final String LAVA = "lava";

    @Test
    void route_feedsTheInputsBeforeFillingTheStores() {
        final FakeEnd output = FakeEnd.output(1, WATER, 100);
        final FakeEnd input = FakeEnd.input(2, 60);
        final FakeEnd store = FakeEnd.store(3, 1000);

        final long moved = route(List.of(output, input, store), 1000, 0);

        assertEquals(100, moved);
        assertEquals(60, input.holding(WATER));
        assertEquals(40, store.holding(WATER));
    }

    @Test
    void route_letsTheStoresFeedWhatTheInputsStillWant() {
        final FakeEnd store = FakeEnd.store(1, 1000).with(WATER, 50);
        final FakeEnd input = FakeEnd.input(2, 30);

        route(List.of(store, input), 1000, 0);

        assertEquals(30, input.holding(WATER));
        assertEquals(20, store.holding(WATER));
    }

    @Test
    void route_neverPoursOneStoreIntoAnother() {
        final FakeEnd first = FakeEnd.store(1, 1000).with(WATER, 50);
        final FakeEnd second = FakeEnd.store(2, 1000).with(WATER, 10);

        assertEquals(0, route(List.of(first, second), 1000, 0));
    }

    @Test
    void route_fillsTheHighestPriorityFirst() {
        final FakeEnd output = FakeEnd.output(1, WATER, 50);
        final FakeEnd low = FakeEnd.input(2, 100);
        final FakeEnd high = FakeEnd.input(3, 100).priority(5);

        route(List.of(output, low, high), 1000, 0);

        assertEquals(50, high.holding(WATER));
        assertEquals(0, low.holding(WATER));
    }

    @Test
    void route_letsEndsOfOnePriorityTakeTurns() {
        final FakeEnd output = FakeEnd.output(1, WATER, 1000);
        final FakeEnd first = FakeEnd.input(2, 1000);
        final FakeEnd second = FakeEnd.input(3, 1000);

        route(List.of(output, first, second), 10, 0);
        route(List.of(output, first, second), 10, 1);
        route(List.of(output, first, second), 10, 2);

        assertEquals(20, first.holding(WATER));
        assertEquals(10, second.holding(WATER));
    }

    @Test
    void route_keepsNoMoreThanTheFilterKeepsInTheBlock() {
        final FakeEnd output = FakeEnd.output(1, WATER, 100);
        final FakeEnd input = FakeEnd.input(2, 1000).with(WATER, 25).keeps(WATER, 30);

        route(List.of(output, input), 1000, 0);

        assertEquals(30, input.holding(WATER));
    }

    @Test
    void route_movesNoMoreThroughAnEndThanItsRate() {
        final FakeEnd output = FakeEnd.output(1, WATER, 100).rate(3);
        final FakeEnd input = FakeEnd.input(2, 1000);

        assertEquals(3, route(List.of(output, input), 1000, 0));
    }

    @Test
    void route_movesNoMoreThanTheLineCarries() {
        final FakeEnd output = FakeEnd.output(1, WATER, 100);
        final FakeEnd input = FakeEnd.input(2, 1000);

        assertEquals(7, route(List.of(output, input), 7, 0));
    }

    @Test
    void route_keepsEachChannelApart() {
        final FakeEnd output = FakeEnd.output(1, WATER, 100).channel(2);
        final FakeEnd other = FakeEnd.input(2, 1000);
        final FakeEnd same = FakeEnd.input(3, 1000).channel(2);

        route(List.of(output, other, same), 1000, 0);

        assertEquals(0, other.holding(WATER));
        assertEquals(100, same.holding(WATER));
    }

    @Test
    void route_drawsFromAStoreAnEndPullsFrom() {
        final FakeEnd pulled = FakeEnd.store(1, 1000).with(WATER, 40).mode(EndMode.PULL);
        final FakeEnd store = FakeEnd.store(2, 1000);

        route(List.of(pulled, store), 1000, 0);

        assertEquals(40, store.holding(WATER));
        assertEquals(0, pulled.holding(WATER));
    }

    @Test
    void route_onlyFillsAStoreAnEndPushesInto() {
        final FakeEnd pushed = FakeEnd.store(1, 1000).with(WATER, 40).mode(EndMode.PUSH);
        final FakeEnd input = FakeEnd.input(2, 1000);

        assertEquals(0, route(List.of(pushed, input), 1000, 0));
    }

    @Test
    void route_letsThroughOnlyWhatTheFiltersAllow() {
        final FakeEnd output = FakeEnd.output(1, LAVA, 100);
        final FakeEnd input = FakeEnd.input(2, 1000).denies(LAVA);

        assertEquals(0, route(List.of(output, input), 1000, 0));
    }

    @Test
    void route_neverPassesBetweenTwoEndsOfOneBlock() {
        final FakeEnd output = FakeEnd.output(1, WATER, 100);
        final FakeEnd sameBlock = FakeEnd.input(1, 1000);

        assertEquals(0, route(List.of(output, sameBlock), 1000, 0));
    }

    @Test
    void route_leavesWhatTheLineDoesNotCarry() {
        final FakeEnd output = FakeEnd.output(1, LAVA, 100);
        final FakeEnd input = FakeEnd.input(2, 1000);

        assertEquals(0, EndRouter.route(List.of(output, input), 1000, 0, WATER::equals, FakeEnd::move));
    }

    private static long route(final List<FakeEnd> ends, final long budget, final long turn) {
        return EndRouter.route(ends, budget, turn, resource -> true, FakeEnd::move);
    }

    /* A block an end plugs into: what it holds, whether it gives and takes, and how the end is set. */
    private static final class FakeEnd implements EndRouter.IEnd<String> {

        private final long key;
        private final boolean gives;
        private final boolean takes;
        private final long capacity;
        private final Map<String, Long> held = new TreeMap<>();
        private final Map<String, Long> keeps = new HashMap<>();
        private final Set<String> denied = new HashSet<>();
        private EndMode mode = EndMode.NORMAL;
        private int priority;
        private long rate;
        private int channel;

        private FakeEnd(final long key, final boolean gives, final boolean takes, final long capacity) {
            this.key = key;
            this.gives = gives;
            this.takes = takes;
            this.capacity = capacity;
        }

        static FakeEnd output(final long key, final String resource, final long amount) {
            return new FakeEnd(key, true, false, amount).with(resource, amount);
        }

        static FakeEnd input(final long key, final long capacity) {
            return new FakeEnd(key, false, true, capacity);
        }

        static FakeEnd store(final long key, final long capacity) {
            return new FakeEnd(key, true, true, capacity);
        }

        static long move(final EndRouter.IEnd<String> from, final EndRouter.IEnd<String> to, final String resource,
                         final long amount) {
            final FakeEnd source = (FakeEnd) from;
            final FakeEnd target = (FakeEnd) to;
            final long moved = Math.min(amount, source.holding(resource));
            source.held.merge(resource, -moved, Long::sum);
            target.held.merge(resource, moved, Long::sum);
            return moved;
        }

        FakeEnd with(final String resource, final long amount) {
            this.held.put(resource, amount);
            return this;
        }

        FakeEnd keeps(final String resource, final long amount) {
            this.keeps.put(resource, amount);
            return this;
        }

        FakeEnd denies(final String resource) {
            this.denied.add(resource);
            return this;
        }

        FakeEnd mode(final EndMode to) {
            this.mode = to;
            return this;
        }

        FakeEnd priority(final int to) {
            this.priority = to;
            return this;
        }

        FakeEnd rate(final long to) {
            this.rate = to;
            return this;
        }

        FakeEnd channel(final int to) {
            this.channel = to;
            return this;
        }

        @Override
        public long key() {
            return this.key;
        }

        @Override
        public EndMode mode() {
            return this.mode;
        }

        @Override
        public int priority() {
            return this.priority;
        }

        @Override
        public long rate() {
            return this.rate;
        }

        @Override
        public int channel() {
            return this.channel;
        }

        @Override
        public boolean allows(final String resource) {
            return !this.denied.contains(resource);
        }

        @Override
        public long keep(final String resource) {
            return this.keeps.getOrDefault(resource, 0L);
        }

        @Override
        public EndRouter.Offer<String> offer(final long most, final Predicate<String> wanted) {
            if (!this.gives) {
                return null;
            }
            for (final Map.Entry<String, Long> each : this.held.entrySet()) {
                if (each.getValue() > 0 && wanted.test(each.getKey())) {
                    return new EndRouter.Offer<>(each.getKey(), Math.min(most, each.getValue()));
                }
            }
            return null;
        }

        @Override
        public boolean gives(final String resource) {
            return this.gives && holding(resource) > 0;
        }

        @Override
        public boolean takes(final String resource) {
            return take(resource, 1) > 0;
        }

        @Override
        public long take(final String resource, final long most) {
            if (!this.takes) {
                return 0L;
            }
            long total = 0L;
            for (final long amount : this.held.values()) {
                total += amount;
            }
            return Math.max(0L, Math.min(most, this.capacity - total));
        }

        @Override
        public long holding(final String resource) {
            return this.held.getOrDefault(resource, 0L);
        }
    }
}
