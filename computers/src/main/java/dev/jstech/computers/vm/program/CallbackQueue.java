/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * The calls waiting for their turn on a process's main thread, in the order they came.
 *
 * <p>A script's set-up and its ticks, the handlers of the events the world hands the program, and the farewell when
 * it is stopped all wait here until the main thread has nothing else to do; then they run one after another, out of
 * the same budget as the rest of the program.
 */
final class CallbackQueue implements Iterable<Process.Frame> {

    private final Deque<Process.Frame> calls = new ArrayDeque<>();

    /** Puts a call at the back of the queue. */
    void add(final Process.Frame call) {
        this.calls.addLast(call);
    }

    /** Takes the call at the front, or null when none is waiting. */
    Process.Frame poll() {
        return this.calls.pollFirst();
    }

    /** Whether a call of that method is waiting. */
    boolean holds(final String method) {
        for (final Process.Frame call : this.calls) {
            if (call.method.name().equals(method)) {
                return true;
            }
        }
        return false;
    }

    /** Whether nothing is waiting. */
    boolean isEmpty() {
        return this.calls.isEmpty();
    }

    /** How many calls are waiting. */
    int size() {
        return this.calls.size();
    }

    /** Lets go of every waiting call, as a program that ends does. */
    void clear() {
        this.calls.clear();
    }

    /** The waiting calls, front first, for the save. */
    @Override
    public Iterator<Process.Frame> iterator() {
        return this.calls.iterator();
    }
}
