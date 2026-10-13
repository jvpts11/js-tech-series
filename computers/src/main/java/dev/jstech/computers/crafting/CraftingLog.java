/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * What a crafting part saw lately, newest first, which its Activity tab shows. A Crafting Interface keeps how each
 * job it ran ended, the drain while its recipe changes, and what came out that no pattern listed; a Crafting
 * Receiving Bus keeps each arrival it credited and where it went: to a job, or to the network as unexpected or late.
 */
public final class CraftingLog {

    private final Deque<Entry> entries = new ArrayDeque<>();

    /** How many lines it keeps. */
    public static final int KEPT = 16;
    /** The longest note a line carries: an interface's name. */
    public static final int MAX_NOTE = 32;
    /** How close together, in ticks, arrivals of the same thing are to be one line. */
    public static final long MERGE_TICKS = 100L;
    /** A job made everything it was asked for. */
    public static final byte COMPLETED = 0;
    /** A job made some of what it was asked for before its machine stopped. */
    public static final byte PARTIAL = 1;
    /** A job's machine gave none of the outputs it expected. */
    public static final byte FAILED = 2;
    /** The interface waits for what is still coming out before another recipe goes in. */
    public static final byte DRAINING = 3;
    /** Something came out that no pattern listed, or more than was fed for: it went to the network. */
    public static final byte UNEXPECTED = 4;
    /** An arrival credited to the job that fed it. */
    public static final byte CREDITED = 5;
    /** An arrival for a job that had settled already: it went where the job's outputs go, never to the next job. */
    public static final byte LATE = 6;

    /**
     * One line.
     *
     * @param time   the game time it happened
     * @param what   the id of the thing it is about
     * @param amount how many
     * @param total  for a job, how many it was asked for; else 0
     * @param kind   what happened: {@link #COMPLETED}, {@link #PARTIAL}, {@link #FAILED}, {@link #DRAINING},
     *               {@link #UNEXPECTED}, {@link #CREDITED} or {@link #LATE}
     * @param note   for an arrival, the interface whose job it went to; else empty
     */
    public record Entry(long time, String what, long amount, long total, byte kind, String note) {

        public Entry {
            what = what == null ? "" : what;
            note = note == null ? "" : note.length() > MAX_NOTE ? note.substring(0, MAX_NOTE) : note;
        }
    }

    /**
     * Writes down {@code entry}. A drain that goes on is one line that keeps its latest time, and arrivals of the same
     * thing for the same place close together are one line that adds them up, so a machine paying out a few items a
     * tick does not push everything else out of the list.
     */
    public void add(final Entry entry) {
        final Entry newest = entries.peekFirst();
        if (newest != null && newest.kind() == entry.kind() && newest.what().equals(entry.what())
                && newest.note().equals(entry.note())) {
            if (entry.kind() == DRAINING) {
                entries.removeFirst();
            } else if (arrival(entry.kind()) && entry.time() - newest.time() <= MERGE_TICKS) {
                entries.removeFirst();
                entries.addFirst(new Entry(entry.time(), entry.what(), newest.amount() + entry.amount(), 0L,
                        entry.kind(), entry.note()));
                return;
            }
        }
        entries.addFirst(entry);
        while (entries.size() > KEPT) {
            entries.removeLast();
        }
    }

    /** The lines, newest first. */
    public List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    /** Whether {@code kind} is something arriving at a Receiving Bus. */
    public static boolean arrival(final byte kind) {
        return kind == CREDITED || kind == LATE || kind == UNEXPECTED;
    }

    /** Puts back lines read from a save, oldest first. */
    public void restore(final List<Entry> oldestFirst) {
        entries.clear();
        for (final Entry entry : oldestFirst) {
            entries.addFirst(entry);
            while (entries.size() > KEPT) {
                entries.removeLast();
            }
        }
    }
}
