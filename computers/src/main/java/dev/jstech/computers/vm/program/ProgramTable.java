/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The programs one machine is running, by number, in the order they started.
 *
 * <p>Nothing here knows a language or a world. A program is found by its number directly, and walking them goes in the
 * order they started, which is the order a machine lists them in.
 *
 * @param <R> what runs each program, as the machine holds it
 */
public final class ProgramTable<R extends IProgramRuntime> {

    private final Map<Integer, ProgramEntry<R>> entries = new LinkedHashMap<>();
    private final Collection<ProgramEntry<R>> running = Collections.unmodifiableCollection(this.entries.values());
    private int next = 1;

    /** Takes the number the next program is listed under. */
    public int takeId() {
        return this.next++;
    }

    /** Lists a program under its number, after every program already listed. */
    public void add(final ProgramEntry<R> entry) {
        this.entries.put(entry.id(), entry);
    }

    /** The program of that number, or null. */
    public ProgramEntry<R> byId(final int id) {
        return this.entries.get(id);
    }

    /** Every program, in the order they started, as a list of its own that later changes to the table leave alone. */
    public List<ProgramEntry<R>> all() {
        return List.copyOf(this.entries.values());
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
        return this.entries.remove(id) != null;
    }

    /** Takes every one of these programs out. */
    public void removeAll(final Collection<ProgramEntry<R>> gone) {
        for (final ProgramEntry<R> entry : gone) {
            this.entries.remove(entry.id());
        }
    }

    /** Whether nothing is running at all. */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /** How many programs are running. */
    public int size() {
        return this.entries.size();
    }

    /** The megabytes every running program was given, between them. */
    public int heapMb() {
        int sum = 0;
        for (final ProgramEntry<R> entry : this.entries.values()) {
            sum += entry.heapMb();
        }
        return sum;
    }

    /** The number the next program will be listed under, for the save. */
    public int nextId() {
        return this.next;
    }

    /** Empties the table and numbers on from {@code next}, for a machine read back out of a save. */
    public void restart(final int next) {
        this.entries.clear();
        this.next = Math.max(1, next);
    }
}
