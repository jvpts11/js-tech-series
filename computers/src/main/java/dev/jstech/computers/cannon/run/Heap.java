/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a process has allocated, counted to the byte.
 *
 * <p>The language has no collector on purpose: what a program allocates stays until it says
 * otherwise, and the count is exact so the number the player reads is the number that is true. That
 * is only worth doing if the arithmetic is the same everywhere, so the sizes live here and nowhere
 * else.
 *
 * <p>The heap also remembers which line each allocation came from. That costs a little and buys the
 * one thing a player needs when a process runs out: the three lines that asked for the most.
 */
public final class Heap {

    /** What every object costs before its own contents. */
    public static final int HEADER = 16;

    /** What a reference costs inside an object. */
    public static final int REFERENCE = 8;

    /** How many of the biggest allocating lines are named when a process runs out. */
    private static final int NAMED_LINES = 3;

    /** One thing on the heap: how big it is, where it was made, and whether it has been freed. */
    private static final class Entry {
        private long bytes;
        private final int line;
        private boolean freed;

        Entry(final long bytes, final int line) {
            this.bytes = bytes;
            this.line = line;
        }
    }

    private final Map<Object, Entry> live = new IdentityHashMap<>();
    /*
     * Kept beside the map because two objects are told apart by being themselves, and a map that does
     * that has no order of its own. Writing a process down needs one, so this is it.
     */
    private final List<Object> order = new ArrayList<>();
    private final long budget;
    private long used;
    /*
     * The collector, for the programs that have one. It is asked when an allocation would not fit, and
     * again each time the heap has grown past a mark that moves up with what survived the last
     * collection, so a program that keeps a lot is not swept over and over for the little it makes.
     */
    private java.util.function.LongSupplier collector;
    private long collectAt;

    public Heap(final long budget) {
        this.budget = budget;
    }

    /** Gives the heap a collector: something that frees whatever the program can no longer reach. */
    public void collectWith(final java.util.function.LongSupplier collector) {
        this.collector = collector;
        this.collectAt = Math.max(this.budget / 4, this.used * 2);
    }

    /** Whether this heap has a collector. */
    public boolean collects() {
        return this.collector != null;
    }

    /** Whether the process should collect before its next instruction: it has grown, or gone past its budget. */
    public boolean wantsCollection() {
        return this.collector != null && (this.used > this.collectAt || this.used > this.budget);
    }

    /** What running out reads like when a collection could not bring the heap back under its budget. */
    public String overBudget() {
        return this.outOfMemory(0);
    }

    /** Runs the collector now, where there is one, and says how many bytes it freed. */
    public long collectNow() {
        if (this.collector == null) {
            return 0;
        }
        final long freed = this.collector.getAsLong();
        this.collectAt = Math.max(this.budget / 4, this.used * 2);
        return freed;
    }

    /**
     * Lets go of everything not in the set given, and says how many bytes that was.
     *
     * <p>Something freed by the program but still in the set stays, so that reaching into it still
     * says it was freed; something freed and no longer reachable is simply forgotten.
     */
    public long sweep(final java.util.Set<Object> kept) {
        long freed = 0;
        final List<Object> remaining = new ArrayList<>(kept.size());
        for (final Object thing : this.order) {
            if (kept.contains(thing)) {
                remaining.add(thing);
                continue;
            }
            final Entry entry = this.live.remove(thing);
            if (entry != null && !entry.freed) {
                this.used -= entry.bytes;
                freed += entry.bytes;
            }
        }
        this.order.clear();
        this.order.addAll(remaining);
        return freed;
    }

    /** How many bytes this process may hold at once. */
    public long budget() {
        return this.budget;
    }

    /** How many bytes it is holding. */
    public long used() {
        return this.used;
    }

    /** How many it could still take. */
    public long free() {
        return Math.max(0, this.budget - this.used);
    }

    /**
     * Records something new and gives it back. Halts when it would not fit, naming what was live and
     * the lines that asked for the most, because that is what tells a player where to look.
     */
    public <T> T allocate(final T value, final long bytes, final int line) {
        /*
         * A heap with a collector lets an allocation run past the budget, up to twice over, and
         * leaves it to the process to collect before its next instruction: collecting here, in the
         * middle of one, could let go of something the runtime has just made and not yet handed over.
         */
        final long ceiling = this.collector == null ? this.budget : 2 * this.budget;
        if (this.used + bytes > ceiling) {
            throw new Halt(Halt.Reason.OUT_OF_MEMORY, line, this.outOfMemory(bytes));
        }
        this.used += bytes;
        this.live.put(value, new Entry(bytes, line));
        this.order.add(value);
        return value;
    }

    /** Everything ever allocated, in the order it was, freed things included. */
    public List<Object> everything() {
        return List.copyOf(this.order);
    }

    /** What that thing costs, or 0 if the heap never saw it. */
    public long bytesOf(final Object value) {
        final Entry entry = this.live.get(value);
        return entry == null ? 0 : entry.bytes;
    }

    /** The line that thing was made on, or 0. */
    public int lineOf(final Object value) {
        final Entry entry = this.live.get(value);
        return entry == null ? 0 : entry.line;
    }

    /**
     * Puts something back exactly as it was, for a process being read out of a save.
     *
     * <p>It goes back with the size and the line it had, and freed if it was freed, so what the player
     * sees after the world comes back is what they saw before it went away.
     */
    public void restore(final Object value, final long bytes, final int line, final boolean freed) {
        final Entry entry = new Entry(bytes, line);
        entry.freed = freed;
        this.live.put(value, entry);
        this.order.add(value);
        if (!freed) {
            this.used += bytes;
        }
    }

    /** Makes something already recorded bigger or smaller, as a collection does when it changes. */
    public void resize(final Object value, final long bytes, final int line) {
        final Entry entry = this.live.get(value);
        if (entry == null) {
            return;
        }
        final long after = this.used - entry.bytes + bytes;
        if (after > (this.collector == null ? this.budget : 2 * this.budget)) {
            throw new Halt(Halt.Reason.OUT_OF_MEMORY, line, this.outOfMemory(bytes - entry.bytes));
        }
        this.used = after;
        entry.bytes = bytes;
    }

    /**
     * Frees something. A second free of the same thing is not a mistake in itself, because the
     * reference that named it is set to null by the same statement; using a freed one is.
     */
    public void dispose(final Object value, final int line) {
        final Entry entry = this.live.get(value);
        if (entry == null || entry.freed) {
            return;
        }
        entry.freed = true;
        this.used -= entry.bytes;
    }

    /** Whether this was freed and may no longer be read. */
    public boolean isFreed(final Object value) {
        final Entry entry = this.live.get(value);
        return entry != null && entry.freed;
    }

    /** Everything still held, biggest first, for the console to show. */
    public List<String> liveByLine() {
        final Map<Integer, Long> byLine = new LinkedHashMap<>();
        for (final Entry entry : this.live.values()) {
            if (!entry.freed) {
                byLine.merge(entry.line, entry.bytes, Long::sum);
            }
        }
        final List<Map.Entry<Integer, Long>> sorted = new ArrayList<>(byLine.entrySet());
        sorted.sort(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue).reversed());
        final List<String> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(NAMED_LINES, sorted.size()); i++) {
            lines.add("line " + sorted.get(i).getKey() + " holds " + sorted.get(i).getValue() + " bytes");
        }
        return lines;
    }

    private String outOfMemory(final long wanted) {
        final StringBuilder message = new StringBuilder("out of memory: ")
                .append(this.used).append(" of ").append(this.budget)
                .append(" bytes are live and ").append(wanted).append(" more were asked for");
        for (final String line : this.liveByLine()) {
            message.append("; ").append(line);
        }
        return message.toString();
    }

    /** What a value of that type costs inside an object or an array. */
    public static int sizeOf(final String type) {
        return switch (type) {
            case "int", "float", "bool", "char" -> 4;
            case "long", "double" -> 8;
            default -> REFERENCE;
        };
    }

    /** What a piece of text costs: its header and two bytes for every character. */
    public static long sizeOfText(final String value) {
        return HEADER + 2L * value.length();
    }
}
