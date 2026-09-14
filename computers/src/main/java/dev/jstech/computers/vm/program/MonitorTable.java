/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The locks a process's threads hold: for each locked object, the thread that holds it, how many times over, and the
 * threads waiting for it.
 *
 * <p>Letting go of a lock wakes the threads queued for that lock and no others, so a release costs what that lock's
 * waiters cost rather than a look at every thread. An entry let go of is kept as a spare for the next lock taken, so a
 * program that takes and lets go of a lock in a loop makes nothing new each time round.
 */
final class MonitorTable {

    /** Told of each lock held, for the save. */
    @FunctionalInterface
    interface IHeldLock {
        void held(Object target, int owner, int count);
    }

    /** One held lock. */
    private static final class Held {
        private int owner;
        private int count;
        /** The threads that found it taken, in the order they came. */
        private final List<ProgramThread> queue = new ArrayList<>();
    }

    private final Map<Object, Held> held = new IdentityHashMap<>();
    /** An entry let go of, kept for the next lock taken. */
    private Held spare;

    /**
     * Takes the lock of {@code target} for {@code thread}. True when it was free, or already the thread's own and is
     * taken once more; false when another thread holds it, in which case the thread is queued to be told when it is let
     * go of.
     */
    boolean enter(final ProgramThread thread, final Object target) {
        final Held lock = this.held.get(target);
        if (lock == null) {
            final Held made = this.spare != null ? this.spare : new Held();
            this.spare = null;
            made.owner = thread.id;
            made.count = 1;
            this.held.put(target, made);
            return true;
        }
        if (lock.owner == thread.id) {
            lock.count++;
            return true;
        }
        if (!lock.queue.contains(thread)) {
            lock.queue.add(thread);
        }
        return false;
    }

    /**
     * Lets go of the lock of {@code target} once; false when {@code thread} does not hold it. Let go of as many times
     * as it was taken, the lock is free and the threads queued for it may ask again.
     */
    boolean exit(final ProgramThread thread, final Object target) {
        final Held lock = this.held.get(target);
        if (lock == null || lock.owner != thread.id) {
            return false;
        }
        if (--lock.count == 0) {
            this.held.remove(target);
            this.free(lock, target);
        }
        return true;
    }

    /** Lets go of every lock the thread of that number holds, as its end does, and wakes each lock's queue. */
    void releaseAll(final int owner) {
        final Iterator<Map.Entry<Object, Held>> each = this.held.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<Object, Held> entry = each.next();
            if (entry.getValue().owner == owner) {
                // An entry the iterator has removed can no longer be read, so both halves are taken first.
                final Object target = entry.getKey();
                final Held lock = entry.getValue();
                each.remove();
                this.free(lock, target);
            }
        }
    }

    /** Whether some thread holds the lock of {@code target}. */
    boolean held(final Object target) {
        return this.held.containsKey(target);
    }

    /** Forgets every lock, as a program that halts does. */
    void clear() {
        this.held.clear();
    }

    /** Tells {@code visitor} of every lock held, for the save. */
    void forEach(final IHeldLock visitor) {
        for (final Map.Entry<Object, Held> entry : this.held.entrySet()) {
            visitor.held(entry.getKey(), entry.getValue().owner, entry.getValue().count);
        }
    }

    /** Puts back a lock the save wrote down; its queue is rebuilt by {@link #requeue}. */
    void restore(final Object target, final int owner, final int count) {
        final Held lock = new Held();
        lock.owner = owner;
        lock.count = count;
        this.held.put(target, lock);
    }

    /**
     * Queues again the threads that came back from a save waiting for a lock that is held, so its release still tells
     * them. A thread waiting for a lock nobody holds any more is left to the scheduler's first look.
     */
    void requeue(final List<ProgramThread> threads) {
        for (final ProgramThread thread : threads) {
            if (thread.wait instanceof IWait.Lock waiting) {
                final Held lock = this.held.get(waiting.target());
                if (lock != null && !lock.queue.contains(thread)) {
                    lock.queue.add(thread);
                }
            }
        }
    }

    /** Wakes the threads still waiting for the lock of {@code target}, and keeps the entry for the next lock taken. */
    private void free(final Held lock, final Object target) {
        for (final ProgramThread thread : lock.queue) {
            if (thread.wait instanceof IWait.Lock waiting && waiting.target() == target) {
                thread.wait = IWait.NONE;
            }
        }
        lock.queue.clear();
        this.spare = lock;
    }
}
