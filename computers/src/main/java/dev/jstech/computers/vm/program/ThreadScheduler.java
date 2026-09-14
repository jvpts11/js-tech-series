/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * The threads of a process and whose turn it is: the numbers they are known by, which of them can be given
 * instructions in a round and in what order, and which of the waiting ones may run again.
 *
 * <p>The main thread is always there and is always number one. Running the instructions is the process's business;
 * this only chooses who runs.
 */
final class ThreadScheduler {

    /** What waking the threads has to ask of the world outside them. */
    interface IWorld {

        /** The tick it is now. */
        long now();

        /** Whether that object's lock is held by some thread. */
        boolean locked(Object target);

        /** Whether the program of that number still runs on that machine, empty for this one. */
        boolean running(int program, String host);

        /** Whether a typed line is waiting to be read. */
        boolean typed();
    }

    private final List<ProgramThread> threads = new ArrayList<>();
    private final List<ProgramThread> view = Collections.unmodifiableList(this.threads);
    private final ProgramThread main = new ProgramThread(1);
    /** The threads that can run in the current round, kept so a round does not make a list of its own. */
    private final List<ProgramThread> ready = new ArrayList<>();
    private int nextId = 2;
    private int turn;

    ThreadScheduler() {
        this.threads.add(this.main);
    }

    /** The thread the process began with. */
    ProgramThread main() {
        return this.main;
    }

    /** Every thread there is, the main one first and the rest in the order they were started. */
    List<ProgramThread> threads() {
        return this.view;
    }

    /** A new thread under the next number, already among the threads. */
    ProgramThread start() {
        final ProgramThread made = new ProgramThread(this.nextId++);
        this.threads.add(made);
        return made;
    }

    /** The thread of that number a save wrote down: the main one, or a new one put among the threads. */
    ProgramThread restore(final int id) {
        this.nextId = Math.max(this.nextId, id + 1);
        if (id == this.main.id) {
            return this.main;
        }
        final ProgramThread made = new ProgramThread(id);
        this.threads.add(made);
        return made;
    }

    /** Carries on numbering from what a save wrote down; a number already given out is never given again. */
    void startFrom(final int nextId) {
        this.nextId = Math.max(this.nextId, nextId);
    }

    /** The number the next thread started gets, for the save. */
    int nextId() {
        return this.nextId;
    }

    /** Takes a thread that is over out of the process and lets whoever joined it run again. */
    void remove(final ProgramThread thread) {
        this.threads.remove(thread);
        for (final ProgramThread other : this.threads) {
            if (other.wait instanceof IWait.Join join && join.thread() == thread.id) {
                other.wait = IWait.NONE;
            }
        }
    }

    /** The thread of that number, or null when there is none or it is over. */
    ProgramThread thread(final Object id) {
        return id instanceof Integer number ? this.find(number) : null;
    }

    /** The threads that can be given instructions in this round, in the order they were started. */
    List<ProgramThread> ready(final Predicate<ProgramThread> runnable) {
        this.ready.clear();
        for (final ProgramThread thread : this.threads) {
            if (runnable.test(thread)) {
                this.ready.add(thread);
            }
        }
        return this.ready;
    }

    /** Where a round of {@code count} ready threads starts: one further on than the round before it started. */
    int firstTurn(final int count) {
        return Math.floorMod(this.turn, count);
    }

    /** Notes where the round that just ran started, so the next one starts after it. */
    void turned(final int first) {
        this.turn = first + 1;
    }

    /** Lets every thread whose wait is over run again. */
    void wake(final IWorld world) {
        final long now = world.now();
        for (final ProgramThread thread : this.threads) {
            switch (thread.wait) {
                case IWait.Sleep sleep -> {
                    if (now >= sleep.until()) {
                        thread.wait = IWait.NONE;
                    }
                }
                case IWait.Join join -> {
                    if (this.find(join.thread()) == null) {
                        thread.wait = IWait.NONE;
                    } else if (join.until() > 0 && now >= join.until()) {
                        thread.wait = IWait.NONE;
                        thread.gaveUp();
                    }
                }
                case IWait.Lock lock -> {
                    if (!world.locked(lock.target())) {
                        thread.wait = IWait.NONE;
                    }
                }
                case IWait.Child child -> {
                    if (!world.running(child.program(), child.host())) {
                        thread.wait = IWait.NONE;
                    } else if (child.until() > 0 && now >= child.until()) {
                        thread.wait = IWait.NONE;
                        thread.gaveUp();
                    }
                }
                case IWait.Input typed -> {
                    if (world.typed()) {
                        thread.wait = IWait.NONE;
                    }
                }
                case IWait.None none -> { }
            }
        }
    }

    /** Lets every thread waiting for the lock of {@code target} ask for it again. */
    void wakeLocked(final Object target) {
        for (final ProgramThread thread : this.threads) {
            if (thread.wait instanceof IWait.Lock lock && lock.target() == target) {
                thread.wait = IWait.NONE;
            }
        }
    }

    /** Lets every thread waiting for a typed line run again. */
    void wakeReaders() {
        for (final ProgramThread thread : this.threads) {
            if (thread.wait instanceof IWait.Input) {
                thread.wait = IWait.NONE;
            }
        }
    }

    /** Whether some thread waits for a typed line. */
    boolean anyReader() {
        for (final ProgramThread thread : this.threads) {
            if (thread.wait instanceof IWait.Input) {
                return true;
            }
        }
        return false;
    }

    private ProgramThread find(final int id) {
        for (final ProgramThread thread : this.threads) {
            if (thread.id == id) {
                return thread;
            }
        }
        return null;
    }
}
