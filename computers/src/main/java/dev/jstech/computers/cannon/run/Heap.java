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
 * <p>Freeing is the program's word, and from then on the heap keeps only what it needs to catch the
 * thing being used again: it forgets the size and the line, and remembers the thing itself for as long
 * as the program can still reach it (see {@link Tombstones}). A program that allocates and frees for as
 * long as the world runs holds only what it holds.
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

    /** One thing on the heap: how big it is, where it was made, and when. */
    private static final class Entry {
        private long bytes;
        private final int line;
        private final long made;

        Entry(final long bytes, final int line, final long made) {
            this.bytes = bytes;
            this.line = line;
            this.made = made;
        }
    }

    private final Map<Object, Entry> live = new IdentityHashMap<>();
    private final Tombstones freed = new Tombstones();
    private final long budget;
    private long used;
    /** How many things were ever put here, which is the order they were put here in. */
    private long allocations;

    public Heap(final long budget) {
        this.budget = budget;
    }

    /** What running out reads like when an allocation does not fit in what is left. */
    public String overBudget() {
        return this.outOfMemory(0);
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
        if (this.used + bytes > this.budget) {
            throw new Halt(Halt.Reason.OUT_OF_MEMORY, line, this.outOfMemory(bytes));
        }
        this.used += bytes;
        this.live.put(value, new Entry(bytes, line, this.allocations++));
        return value;
    }

    /**
     * Everything still held, in the order it was allocated.
     *
     * <p>The order is what makes two saves of the same program read the same; it is worked out here, when
     * asked, rather than kept up on every allocation and free.
     */
    public List<Object> live() {
        final List<Map.Entry<Object, Entry>> held = new ArrayList<>(this.live.entrySet());
        held.sort(Comparator.comparingLong(entry -> entry.getValue().made));
        final List<Object> things = new ArrayList<>(held.size());
        for (final Map.Entry<Object, Entry> entry : held) {
            things.add(entry.getKey());
        }
        return things;
    }

    /** What that thing costs, or 0 if it is not held (never seen, or freed). */
    public long bytesOf(final Object value) {
        final Entry entry = this.live.get(value);
        return entry == null ? 0 : entry.bytes;
    }

    /** The line that thing was made on, or 0 if it is not held. */
    public int lineOf(final Object value) {
        final Entry entry = this.live.get(value);
        return entry == null ? 0 : entry.line;
    }

    /**
     * Puts something back exactly as it was, for a process being read out of a save.
     *
     * <p>A live thing goes back with the size and the line it had; a freed one goes back freed, so a
     * name that still reaches it is still caught using it.
     */
    public void restore(final Object value, final long bytes, final int line, final boolean freed) {
        if (freed) {
            this.freed.add(value);
            return;
        }
        this.used += bytes;
        this.live.put(value, new Entry(bytes, line, this.allocations++));
    }

    /** Makes something already held bigger or smaller, as a collection does when it changes. */
    public void resize(final Object value, final long bytes, final int line) {
        final Entry entry = this.live.get(value);
        if (entry == null) {
            return;
        }
        final long after = this.used - entry.bytes + bytes;
        if (after > this.budget) {
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
        final Entry entry = this.live.remove(value);
        if (entry == null) {
            return;
        }
        this.used -= entry.bytes;
        this.freed.add(value);
    }

    /** Whether this was freed and may no longer be read. */
    public boolean isFreed(final Object value) {
        return this.freed.contains(value);
    }

    /** Everything still held, biggest first, for the console to show. */
    public List<String> liveByLine() {
        final Map<Integer, Long> byLine = new LinkedHashMap<>();
        for (final Entry entry : this.live.values()) {
            byLine.merge(entry.line, entry.bytes, Long::sum);
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
