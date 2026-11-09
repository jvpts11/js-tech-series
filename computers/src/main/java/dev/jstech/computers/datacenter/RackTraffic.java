/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datacenter;

/**
 * What a Server Rack's servers did for the network since its section's router last read it: the items they moved, and
 * how long each read or write waited for its server to answer. The router reads it once a second and starts it over.
 *
 * <p>A rack in no section is never read, so what it counts would only grow: a count left unread for longer than a
 * reading takes is started over before it is added to.
 */
public final class RackTraffic {

    private long items;
    private long waitedTicks;
    private int answers;
    // Never read yet: far enough back to count as stale, not so far that taking it from a game time overflows.
    private long readAt = Long.MIN_VALUE / 2;

    /* How long a count may go unread before it is taken as no router's: two readings' worth. */
    private static final long STALE_TICKS = 40L;

    /** {@code moved} items crossed one of the rack's servers at {@code now}. */
    public void moved(final long now, final long moved) {
        freshen(now);
        items += Math.max(0L, moved);
    }

    /** One of the rack's servers answered a read or a write at {@code now}, {@code ticks} after it was asked. */
    public void answered(final long now, final long ticks) {
        freshen(now);
        waitedTicks += Math.max(0L, ticks);
        answers++;
    }

    /** What was counted since the last reading, which it starts over. */
    public Sample read(final long now) {
        final Sample sample = now - readAt > STALE_TICKS ? Sample.NONE : new Sample(items, waitedTicks, answers);
        items = 0L;
        waitedTicks = 0L;
        answers = 0;
        readAt = now;
        return sample;
    }

    /* A count nobody read for longer than a reading takes is no router's: it starts over. */
    private void freshen(final long now) {
        if (now - readAt > STALE_TICKS) {
            items = 0L;
            waitedTicks = 0L;
            answers = 0;
            readAt = now;
        }
    }

    /**
     * One reading of a rack, or of a section summed over its racks.
     *
     * @param items       the items its servers moved
     * @param waitedTicks the ticks its reads and writes waited for their servers, summed
     * @param answers     how many reads and writes were answered
     */
    public record Sample(long items, long waitedTicks, int answers) {

        public static final Sample NONE = new Sample(0L, 0L, 0);

        /** The two readings together. */
        public Sample plus(final Sample other) {
            return new Sample(items + other.items, waitedTicks + other.waitedTicks, answers + other.answers);
        }
    }
}
