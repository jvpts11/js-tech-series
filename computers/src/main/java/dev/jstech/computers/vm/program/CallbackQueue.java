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
 *
 * <p>The queue holds only so much. A program that falls behind would otherwise keep every click and every alert the
 * world sends it, and the machine would keep them with it; so a call from the world is offered only while there is
 * room for it and for what its arguments hold, and one that finds none is counted.
 */
final class CallbackQueue implements Iterable<Process.Frame> {

    /** The most calls the world may leave waiting at once. */
    static final int MOST_CALLS = 256;

    /** The most bytes the arguments of the waiting calls may hold between them. */
    static final long MOST_BYTES = 64L * 1024;

    /** A waiting call and what its arguments hold, which is given back when the call is taken. */
    private record Waiting(Process.Frame call, long bytes) {
    }

    private final Deque<Waiting> calls = new ArrayDeque<>();
    private long bytes;
    private long dropped;

    /** Whether {@code count} more calls, holding {@code bytes} between them, fit in what is left. */
    boolean fits(final int count, final long bytes) {
        return this.calls.size() + count <= MOST_CALLS && this.bytes + bytes <= MOST_BYTES;
    }

    /** Puts a call at the back of the queue whether or not it fits; asking first is the caller's to do. */
    void add(final Process.Frame call, final long bytes) {
        this.calls.addLast(new Waiting(call, bytes));
        this.bytes += bytes;
    }

    /** Puts a call at the front, ahead of everything waiting. */
    void addFirst(final Process.Frame call, final long bytes) {
        this.calls.addFirst(new Waiting(call, bytes));
        this.bytes += bytes;
    }

    /** Takes the call at the front, or null when none is waiting. */
    Process.Frame poll() {
        final Waiting next = this.calls.pollFirst();
        if (next == null) {
            return null;
        }
        this.bytes -= next.bytes();
        return next.call();
    }

    /** Whether a call of that method is waiting. */
    boolean holds(final String method) {
        for (final Waiting waiting : this.calls) {
            if (waiting.call().method.name().equals(method)) {
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

    /** How many bytes the arguments of the waiting calls hold between them. */
    long bytes() {
        return this.bytes;
    }

    /** Counts a call from the world that found no room and was let go. */
    void drop() {
        this.dropped++;
    }

    /** How many calls from the world found no room and were let go since the program started. */
    long dropped() {
        return this.dropped;
    }

    /** Carries on counting from what a save wrote down. */
    void startFrom(final long dropped) {
        this.dropped = dropped;
    }

    /** Lets go of every waiting call, as a program that ends does; what was dropped stays counted. */
    void clear() {
        this.calls.clear();
        this.bytes = 0;
    }

    /** The waiting calls, front first, for the save. */
    @Override
    public Iterator<Process.Frame> iterator() {
        final Iterator<Waiting> waiting = this.calls.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return waiting.hasNext();
            }

            @Override
            public Process.Frame next() {
                return waiting.next().call();
            }
        };
    }
}
