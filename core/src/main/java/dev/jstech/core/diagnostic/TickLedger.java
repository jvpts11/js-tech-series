/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * What the server sent in each of its last {@value #TICKS} ticks, tick by tick: every kind of packet sent in a tick,
 * how many of it and how many bytes. A tick older than that is let go as a new one comes, so a ledger holds the same
 * few ticks however long the server runs.
 *
 * <p>Pure: the tick it is now is read from {@code clock}.
 */
public final class TickLedger {

    private final LongSupplier clock;
    private final long[] tickOf = new long[TICKS];
    private final List<Map<String, long[]>> slots = new ArrayList<>(TICKS);

    /** How many ticks the ledger keeps. */
    public static final int TICKS = 100;

    private static final int PACKETS = 0;
    private static final int BYTES = 1;

    /** A ledger whose tick is read from {@code clock}. */
    public TickLedger(final LongSupplier clock) {
        this.clock = clock;
        for (int i = 0; i < TICKS; i++) {
            this.tickOf[i] = Long.MIN_VALUE;
            this.slots.add(new HashMap<>());
        }
    }

    /** Counts a packet of {@code kind}, {@code bytes} long, in the tick it is now. */
    public synchronized void count(final String kind, final long bytes) {
        final long now = this.clock.getAsLong();
        final int slot = Math.floorMod(now, TICKS);
        if (this.tickOf[slot] != now) {
            this.tickOf[slot] = now;
            this.slots.get(slot).clear();
        }
        final long[] counted = this.slots.get(slot).computeIfAbsent(kind, unused -> new long[2]);
        counted[PACKETS]++;
        counted[BYTES] += bytes;
    }

    /** What was sent in the tick {@code tick}, when it is still kept; nothing for a tick in which nothing was sent. */
    public synchronized Optional<Tick> tick(final long tick) {
        final int slot = Math.floorMod(tick, TICKS);
        if (this.tickOf[slot] != tick || tick > this.clock.getAsLong() || tick <= this.clock.getAsLong() - TICKS) {
            return Optional.empty();
        }
        final List<Sent> sent = new ArrayList<>();
        this.slots.get(slot).forEach((kind, counted) -> sent.add(new Sent(kind, counted[PACKETS], counted[BYTES])));
        sent.sort(Comparator.comparingLong(Sent::bytes).reversed().thenComparing(Sent::kind));
        return Optional.of(new Tick(tick, sent));
    }

    /** What was sent in each kept tick that sent anything, the newest first. */
    public synchronized List<Tick> ticks() {
        final long now = this.clock.getAsLong();
        final List<Tick> out = new ArrayList<>();
        for (long tick = now; tick > now - TICKS; tick--) {
            tick(tick).ifPresent(out::add);
        }
        return out;
    }

    /** The kept tick that sent the most bytes, when any sent anything. */
    public Optional<Tick> busiest() {
        return ticks().stream().max(Comparator.comparingLong(Tick::bytes));
    }

    /**
     * One kind of packet sent in a tick.
     *
     * @param kind    a mod's payload's id, or the game's id for a packet's kind
     * @param packets how many were sent
     * @param bytes   how many bytes they took
     */
    public record Sent(String kind, long packets, long bytes) {
    }

    /**
     * What one tick sent.
     *
     * @param tick the server's tick
     * @param sent each kind sent, the most bytes first
     */
    public record Tick(long tick, List<Sent> sent) {

        public Tick {
            sent = List.copyOf(sent);
        }

        /** How many bytes the tick sent in all. */
        public long bytes() {
            long out = 0L;
            for (final Sent each : this.sent) {
                out += each.bytes();
            }
            return out;
        }

        /** How many packets the tick sent in all. */
        public long packets() {
            long out = 0L;
            for (final Sent each : this.sent) {
                out += each.packets();
            }
            return out;
        }
    }
}
