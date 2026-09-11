/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * The last forty things a Gateway did, newest first: who asked (a CC computer by id, a J's computer by
 * name), what, and how it went. Kept on the block entity the way the Mainframe keeps its operations, and
 * shown by the Gateway Manager and the {@code gateway} command.
 */
public final class GatewayLog {

    public static final int CAPACITY = 40;

    /** How an entry ended, for the colour it is shown in. */
    public enum Tone {
        OK,
        BUSY,
        DENIED;

        public static Tone at(final int index) {
            final Tone[] all = values();
            return all[Math.max(0, Math.min(all.length - 1, index))];
        }
    }

    /**
     * One thing the Gateway did.
     *
     * @param dayTime the world's time of day when it happened, for the clock the log shows
     * @param who     who asked
     * @param what    what was asked
     * @param result  how it went
     * @param tone    the colour of the result
     */
    public record Entry(long dayTime, String who, String what, String result, Tone tone) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    /** Adds an entry as the newest, dropping the oldest past the capacity. */
    public void add(final Entry entry) {
        entries.addFirst(entry);
        while (entries.size() > CAPACITY) {
            entries.removeLast();
        }
    }

    public void add(final long dayTime, final String who, final String what, final String result, final Tone tone) {
        add(new Entry(dayTime, who, what, result, tone));
    }

    /** Every entry, newest first. */
    public List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    /** The newest {@code count} entries. */
    public List<Entry> recent(final int count) {
        final List<Entry> out = new ArrayList<>(Math.min(count, entries.size()));
        for (final Entry e : entries) {
            if (out.size() >= count) {
                break;
            }
            out.add(e);
        }
        return out;
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    /**
     * Restores the log from its saved order, oldest first, the order {@link #entries()} reversed gives.
     */
    public void restore(final List<Entry> oldestFirst) {
        entries.clear();
        for (final Entry e : oldestFirst) {
            add(e);
        }
    }

    /**
     * The world's time of day as a clock: a Minecraft day is 24 000 ticks and starts at six in the
     * morning, so the log reads like a wall clock in the base.
     */
    public static String clock(final long dayTime) {
        final long ofDay = Math.floorMod(dayTime, 24_000L);
        final long hours = (ofDay / 1_000L + 6L) % 24L;
        final long rest = ofDay % 1_000L;
        final long minutes = rest * 60L / 1_000L;
        final long seconds = (rest * 3_600L / 1_000L) % 60L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }
}
