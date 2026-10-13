/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * What a bus did lately, newest first, which its Activity tab shows: each move it made, as the Operation it was, and
 * each time it held back, with why: the chest keeps what it was set to keep, the network or the chest has no room, a
 * condition does not hold. A hold that goes on is one line that keeps its latest time, so a bus waiting for an hour
 * does not push its moves out of the list.
 */
public final class BusActivity {

    private final Deque<Entry> entries = new ArrayDeque<>();
    private long lastMovedAt = NEVER;

    /** How many lines it keeps. */
    public static final int KEPT = 16;
    /** The last-move time of a bus that has not moved anything. */
    public static final long NEVER = Long.MIN_VALUE;
    public static final byte COMPLETED = 0;
    public static final byte PARTIAL = 1;
    public static final byte WAITING = 2;
    public static final byte LOCKED = 3;
    /** A move into the network or out of it, as the bus faces it. */
    public static final byte MOVED = 0;
    /** The chest keeps what the bus was set to leave in it; the detail is how many. */
    public static final byte KEEPS = 1;
    /** No room where it would go: the network's storage or the chest. */
    public static final byte FULL = 2;
    /** A condition it was set to wait for does not hold. */
    public static final byte HELD = 3;

    /**
     * One line: when, what (an item's id), how many, how it ended, and why.
     *
     * @param time   the game time it happened
     * @param what   the id of what it moved or waited for, empty when it waited for nothing in particular
     * @param amount how many it moved
     * @param status {@link #COMPLETED}, {@link #PARTIAL}, {@link #WAITING} or {@link #LOCKED}
     * @param reason {@link #MOVED}, {@link #KEEPS}, {@link #FULL} or {@link #HELD}
     * @param detail for {@link #KEEPS}, how many the chest keeps
     */
    public record Entry(long time, String what, long amount, byte status, byte reason, long detail) {
    }

    /** A move of {@code amount} of {@code what}, all of it or {@code partial}ly. */
    public void moved(final long time, final String what, final long amount, final boolean partial) {
        lastMovedAt = time;
        add(new Entry(time, what, amount, partial ? PARTIAL : COMPLETED, MOVED, 0L));
    }

    /** A hold on {@code what} for {@code reason}; the same hold as the newest line only moves that line's time. */
    public void held(final long time, final String what, final byte reason, final long detail) {
        final byte status = reason == FULL ? LOCKED : WAITING;
        final Entry newest = entries.peekFirst();
        if (newest != null && newest.status() == status && newest.reason() == reason && newest.what().equals(what)
                && newest.detail() == detail) {
            entries.removeFirst();
        }
        add(new Entry(time, what, 0L, status, reason, detail));
    }

    /** The lines, newest first. */
    public List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    /** Whether the bus has made no move for {@code ticks} up to {@code now}: what another bus waits on it for. */
    public boolean idleFor(final long now, final long ticks) {
        // Kept apart from the log: a run of holds on different items can push the last move out of it.
        return lastMovedAt == NEVER || now - lastMovedAt >= ticks;
    }

    /** The game time of the last move, or {@link #NEVER}; saved with the lines. */
    public long lastMovedAt() {
        return lastMovedAt;
    }

    /** Puts back lines read from a save, oldest first; the last move is taken from them. */
    public void restore(final List<Entry> oldestFirst) {
        long newestMove = NEVER;
        for (final Entry entry : oldestFirst) {
            if (entry.reason() == MOVED) {
                newestMove = entry.time();
            }
        }
        restore(oldestFirst, newestMove);
    }

    /** Puts back lines and the time of the last move read from a save, oldest first. */
    public void restore(final List<Entry> oldestFirst, final long lastMoved) {
        entries.clear();
        lastMovedAt = lastMoved;
        for (final Entry entry : oldestFirst) {
            add(entry);
        }
    }

    private void add(final Entry entry) {
        entries.addFirst(entry);
        while (entries.size() > KEPT) {
            entries.removeLast();
        }
    }
}
