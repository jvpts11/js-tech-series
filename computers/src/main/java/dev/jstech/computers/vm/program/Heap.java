/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IPureContext;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
@TextHolder
public final class Heap implements IPureContext {

    /** What every object costs before its own contents. */
    public static final int HEADER = 16;

    /** What a reference costs inside an object. */
    public static final int REFERENCE = 8;

    /** How many of the biggest allocating lines are named when a process runs out. */
    private static final int NAMED_LINES = 3;

    private static final TextKey OUT_OF_MEMORY = TextKey.of("jsc.vm.heap.out_of_memory",
            "out of memory: %s of %s bytes are live and %s more were asked for");
    private static final TextKey LINE_HOLDS = TextKey.of("jsc.vm.heap.line_holds", "line %s holds %s bytes");
    /** Joins one more named line onto what running out says, the way the list reads in that language. */
    private static final TextKey AND_THEN = TextKey.of("jsc.vm.heap.and_then", "%s; %s");
    private static final TextKey NOTHING_TO_REACH = TextKey.of("jsc.vm.heap.nothing_to_reach",
            "there is nothing here to reach into");
    private static final TextKey DISPOSED = TextKey.of("jsc.vm.heap.disposed", "this was disposed and cannot be used");

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
    public Text overBudget() {
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
    @Override
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

    /**
     * A fresh piece of text, held here. Each is a thing of its own, so two that read the same are still two things the
     * program can free one of without the other going with it.
     */
    @Override
    public String text(final String value, final int line) {
        return this.allocate(new String(value.toCharArray()), sizeOfText(value), line);
    }

    /** Makes something already held bigger or smaller, as a collection does when it changes. */
    @Override
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

    /**
     * Lets go of something only the runtime ever held, such as a widget's own copy of a text it has replaced or a
     * stroke a cleared canvas no longer draws. Unlike {@link #dispose}, nothing is kept to catch a later use, because
     * nothing the program holds can reach it.
     */
    void release(final Object value) {
        final Entry entry = this.live.remove(value);
        if (entry != null) {
            this.used -= entry.bytes;
        }
    }

    /** Whether this was freed and may no longer be read. */
    public boolean isFreed(final Object value) {
        return this.freed.contains(value);
    }

    /**
     * The value itself, when the program may reach into it. Reaching into nothing halts, and so does reaching into
     * something already freed, since using it again is the mistake freeing exists to catch.
     */
    Object alive(final Object value, final int line) {
        if (value == null || this.freed.contains(value)) {
            throw unreachable(value, line);
        }
        return value;
    }

    /* Built apart from alive, so the check every reach makes stays small enough to be inlined wherever it is called. */
    private static Halt unreachable(final Object value, final int line) {
        return value == null
                ? new Halt(Halt.Reason.NO_OBJECT, line, NOTHING_TO_REACH.text())
                : new Halt(Halt.Reason.USE_AFTER_DISPOSE, line, DISPOSED.text());
    }

    /**
     * Puts a value that came from outside onto the heap, contents and all, so the program may hold it.
     *
     * <p>The one door for everything the world hands a program: an answer from the machine comes through here. A
     * value that skips it is outside the program's RAM and outside its snapshot, which is to say a value that quietly
     * becomes nothing the next time the world is read back. What a program is handed is the program's to hold and to
     * free, and it weighs what it weighs; anything already held is left where it is, so handing back something the
     * program gave in the first place does not charge it twice.
     */
    Object adopt(final Object made, final int line) {
        if (made == null || this.bytesOf(made) > 0) {
            return made;
        }
        switch (made) {
            case String text -> this.allocate(text, sizeOfText(text), line);
            case Values.ListValue list -> {
                for (int i = 0; i < list.items().size(); i++) {
                    list.items().set(i, this.adopt(list.items().get(i), line));
                }
                this.allocate(list, list.bytes(), line);
            }
            case Values.MapValue map -> {
                final Map<Object, Object> adopted = new LinkedHashMap<>();
                for (final Map.Entry<Object, Object> entry : map.entries().entrySet()) {
                    adopted.put(this.adopt(entry.getKey(), line), this.adopt(entry.getValue(), line));
                }
                map.entries().clear();
                map.entries().putAll(adopted);
                this.allocate(map, map.bytes(), line);
            }
            case Values.Obj object -> {
                for (final Map.Entry<String, Object> field : object.all().entrySet()) {
                    object.set(field.getKey(), this.adopt(field.getValue(), line));
                }
                this.allocate(object, HEADER + (long) REFERENCE * object.all().size(), line);
            }
            default -> {
                // A number, a bool or a character: a value, which weighs nothing of its own.
            }
        }
        return made;
    }

    /** Everything still held, biggest first, for the console to show. */
    public List<Text> liveByLine() {
        final Map<Integer, Long> byLine = new LinkedHashMap<>();
        for (final Entry entry : this.live.values()) {
            byLine.merge(entry.line, entry.bytes, Long::sum);
        }
        final List<Map.Entry<Integer, Long>> sorted = new ArrayList<>(byLine.entrySet());
        sorted.sort(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue).reversed());
        final List<Text> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(NAMED_LINES, sorted.size()); i++) {
            lines.add(LINE_HOLDS.with(sorted.get(i).getKey(), sorted.get(i).getValue()));
        }
        return lines;
    }

    private Text outOfMemory(final long wanted) {
        Text message = OUT_OF_MEMORY.with(this.used, this.budget, wanted);
        for (final Text line : this.liveByLine()) {
            message = AND_THEN.with(message, line);
        }
        return message;
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
