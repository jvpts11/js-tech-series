/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.connect.Neighbours;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Where what a part of a line moves goes, wire by wire: each move from one end to another is followed along the
 * shortest way between their wires, and each wire on it counts what crossed it and toward which face, over a window of
 * ticks, so a pipe can show its fluid and its level, a tube its items, and a wrench which way and how much. The ways
 * are found once for each pair of ends that moves, until the part's shape changes.
 */
public final class FlowTrace {

    private final Set<Long> wires;
    private final LongFunction<Set<Long>> neighbours;
    private final LongFunction<BlockPos> positions;
    private final Map<Long, Map<Long, long[]>> ways = new HashMap<>();
    private final Map<Long, Map<Way, Long>> counted = new HashMap<>();

    /** How many ticks a window counts over before it is read. */
    public static final int WINDOW = 10;

    /**
     * The flow of a part whose wires are {@code wires}, each a number of its grid, joined as {@code neighbours} says and
     * standing where {@code positions} says.
     */
    public FlowTrace(final Set<Long> wires, final LongFunction<Set<Long>> neighbours,
                     final LongFunction<BlockPos> positions) {
        this.wires = Set.copyOf(wires);
        this.neighbours = neighbours;
        this.positions = positions;
    }

    /**
     * Counts {@code amount} of {@code what} moved from the end of the wire {@code fromWire} to the end of
     * {@code toWire}, where it went into a block through the face {@code into} of that wire's cable block.
     */
    public void moved(final long fromWire, final long toWire, final Direction into, final String what,
                      final long amount) {
        if (amount <= 0) {
            return;
        }
        final long[] way = way(fromWire, toWire);
        for (int i = 0; i < way.length; i++) {
            final Direction toward = i + 1 < way.length ? Neighbours.faceTowards(this.positions.apply(way[i]),
                    this.positions.apply(way[i + 1])) : into;
            if (toward != null) {
                this.counted.computeIfAbsent(way[i], key -> new HashMap<>()).merge(new Way(what, toward), amount,
                        Long::sum);
            }
        }
    }

    /**
     * What each wire carried over the window, the most of it one thing toward one face, a tick on average; the window
     * starts again. A wire that carried nothing is not named.
     */
    public Map<Long, Flow> read() {
        final Map<Long, Flow> out = new HashMap<>();
        for (final Map.Entry<Long, Map<Way, Long>> wire : this.counted.entrySet()) {
            Way most = null;
            long amount = 0L;
            for (final Map.Entry<Way, Long> each : wire.getValue().entrySet()) {
                if (each.getValue() > amount) {
                    most = each.getKey();
                    amount = each.getValue();
                }
            }
            if (most != null) {
                out.put(wire.getKey(), new Flow(most.what(), most.toward(), Math.max(1L, amount / WINDOW)));
            }
        }
        this.counted.clear();
        return out;
    }

    /* The shortest way between two wires of the part, found once; just the one wire when they are the same. */
    private long[] way(final long from, final long to) {
        if (from == to) {
            return new long[] {from};
        }
        return this.ways.computeIfAbsent(from, key -> new HashMap<>()).computeIfAbsent(to, key -> search(from, to));
    }

    private long[] search(final long from, final long to) {
        final Map<Long, Long> came = new HashMap<>();
        final Deque<Long> open = new ArrayDeque<>();
        open.add(from);
        came.put(from, from);
        while (!open.isEmpty()) {
            final long at = open.poll();
            if (at == to) {
                break;
            }
            for (final long next : this.neighbours.apply(at)) {
                if (this.wires.contains(next) && !came.containsKey(next)) {
                    came.put(next, at);
                    open.add(next);
                }
            }
        }
        if (!came.containsKey(to)) {
            return new long[] {to};
        }
        final List<Long> back = new ArrayList<>();
        for (long at = to; at != from; at = came.get(at)) {
            back.add(at);
        }
        back.add(from);
        Collections.reverse(back);
        final long[] way = new long[back.size()];
        for (int i = 0; i < way.length; i++) {
            way[i] = back.get(i);
        }
        return way;
    }

    /**
     * What a wire carried over a window: one thing, toward one face, how much a tick.
     *
     * @param what    the id of what it carried
     * @param toward  the face of its cable block it went toward
     * @param perTick how much a tick, on average over the window
     */
    public record Flow(String what, Direction toward, long perTick) {
    }

    /* One thing toward one face. */
    private record Way(String what, Direction toward) {
    }
}
