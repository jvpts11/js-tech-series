/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The programs one machine is running, by number, in the order they started.
 *
 * <p>Nothing here knows a language or a world. A program is found by its number directly, and walking them goes in the
 * order they started, which is the order a machine lists them in.
 *
 * <p>A machine looks programs up by number and walks them on every tick, so neither builds anything: the programs are
 * kept twice in plain arrays, once in the order they started and once sorted by number beside the numbers themselves,
 * and finding one is a binary search over those numbers. Starting and ending a program shift the arrays, which happens
 * far less often than a tick.
 *
 * @param <R> what runs each program, as the machine holds it
 */
public final class ProgramTable<R extends IProgramRuntime> {

    private static final int FIRST_ROOM = 8;

    /** The programs in the order they started. */
    private ProgramEntry<R>[] order = entries(FIRST_ROOM);
    /** The same programs sorted by number, each beside its number in {@link #ids}. */
    private ProgramEntry<R>[] sorted = entries(FIRST_ROOM);
    private int[] ids = new int[FIRST_ROOM];
    private int size;
    /** Counts every change, so a walk that finds the table changed under it stops instead of going wrong. */
    private int changes;
    private final Collection<ProgramEntry<R>> running = new Running();
    private int next = 1;

    /**
     * Takes the number the next program is listed under. Numbers count up, wrap past the largest back to one, and pass
     * over any number a program still running holds, so no two programs share one and none is ever below one.
     */
    public int takeId() {
        int id = this.next;
        while (this.indexOf(id) >= 0) {
            id = id == Integer.MAX_VALUE ? 1 : id + 1;
        }
        this.next = id == Integer.MAX_VALUE ? 1 : id + 1;
        return id;
    }

    /**
     * Lists a program under its number, after every program already listed. One already listed under that number is
     * replaced where it stands.
     */
    public void add(final ProgramEntry<R> entry) {
        final int at = this.indexOf(entry.id());
        if (at >= 0) {
            final ProgramEntry<R> before = this.sorted[at];
            this.sorted[at] = entry;
            for (int i = 0; i < this.size; i++) {
                if (this.order[i] == before) {
                    this.order[i] = entry;
                    break;
                }
            }
            this.changes++;
            return;
        }
        if (this.size == this.order.length) {
            this.grow();
        }
        final int insert = -at - 1;
        System.arraycopy(this.ids, insert, this.ids, insert + 1, this.size - insert);
        System.arraycopy(this.sorted, insert, this.sorted, insert + 1, this.size - insert);
        this.ids[insert] = entry.id();
        this.sorted[insert] = entry;
        this.order[this.size] = entry;
        this.size++;
        this.changes++;
    }

    /** The program of that number, or null. */
    public ProgramEntry<R> byId(final int id) {
        final int at = this.indexOf(id);
        return at < 0 ? null : this.sorted[at];
    }

    /** Every program, in the order they started, as a list of its own that later changes to the table leave alone. */
    public List<ProgramEntry<R>> all() {
        return List.of(Arrays.copyOf(this.order, this.size));
    }

    /**
     * Every program, in the order they started, as a view that follows the table. Nothing can be changed through it,
     * and the table must not change while it is walked.
     */
    public Collection<ProgramEntry<R>> running() {
        return this.running;
    }

    /** Takes a program out; false when none was listed under that number. */
    public boolean remove(final int id) {
        final int at = this.indexOf(id);
        if (at < 0) {
            return false;
        }
        final ProgramEntry<R> gone = this.sorted[at];
        final int last = this.size - 1;
        System.arraycopy(this.ids, at + 1, this.ids, at, last - at);
        System.arraycopy(this.sorted, at + 1, this.sorted, at, last - at);
        this.sorted[last] = null;
        for (int i = 0; i <= last; i++) {
            if (this.order[i] == gone) {
                System.arraycopy(this.order, i + 1, this.order, i, last - i);
                break;
            }
        }
        this.order[last] = null;
        this.size = last;
        this.changes++;
        return true;
    }

    /** Takes every one of these programs out. */
    public void removeAll(final Collection<ProgramEntry<R>> gone) {
        for (final ProgramEntry<R> entry : gone) {
            this.remove(entry.id());
        }
    }

    /** Whether nothing is running at all. */
    public boolean isEmpty() {
        return this.size == 0;
    }

    /** How many programs are running. */
    public int size() {
        return this.size;
    }

    /** The megabytes every running program was given, between them. */
    public int heapMb() {
        int sum = 0;
        for (int i = 0; i < this.size; i++) {
            sum += this.order[i].heapMb();
        }
        return sum;
    }

    /** The number the next program will be listed under, for the save. */
    public int nextId() {
        return this.next;
    }

    /** Empties the table and numbers on from {@code next}, for a machine read back out of a save. */
    public void restart(final int next) {
        Arrays.fill(this.order, 0, this.size, null);
        Arrays.fill(this.sorted, 0, this.size, null);
        this.size = 0;
        this.changes++;
        this.next = Math.max(1, next);
    }

    /** Where that number sits among the sorted numbers, or where it would go, as {@link Arrays#binarySearch} says. */
    private int indexOf(final int id) {
        return Arrays.binarySearch(this.ids, 0, this.size, id);
    }

    private void grow() {
        final int room = this.order.length * 2;
        this.order = Arrays.copyOf(this.order, room);
        this.sorted = Arrays.copyOf(this.sorted, room);
        this.ids = Arrays.copyOf(this.ids, room);
    }

    @SuppressWarnings("unchecked")
    private static <R extends IProgramRuntime> ProgramEntry<R>[] entries(final int room) {
        return (ProgramEntry<R>[]) new ProgramEntry<?>[room];
    }

    /** The programs in the order they started, as the table holds them at the moment; nothing changes through it. */
    private final class Running extends AbstractCollection<ProgramEntry<R>> {

        @Override
        public Iterator<ProgramEntry<R>> iterator() {
            return new Walk();
        }

        @Override
        public int size() {
            return ProgramTable.this.size;
        }
    }

    /** One walk over the programs, which stops if the table changes before it is done. */
    private final class Walk implements Iterator<ProgramEntry<R>> {

        private final int expected = ProgramTable.this.changes;
        private int at;

        @Override
        public boolean hasNext() {
            return this.at < ProgramTable.this.size;
        }

        @Override
        public ProgramEntry<R> next() {
            if (ProgramTable.this.changes != this.expected) {
                throw new ConcurrentModificationException();
            }
            if (this.at >= ProgramTable.this.size) {
                throw new NoSuchElementException();
            }
            return ProgramTable.this.order[this.at++];
        }
    }
}
