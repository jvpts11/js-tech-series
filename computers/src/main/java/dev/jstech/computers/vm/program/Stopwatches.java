/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * A program's stopwatches: each an object of its own that remembers the world's tick and the credits the program had
 * spent when it was started or restarted, so what it reads is how far each has gone since. It holds only numbers, and
 * is saved with the program like any object.
 */
final class Stopwatches {

    /** The type a program writes. */
    static final String TYPE = "Stopwatch";
    private static final String FROM_TICK = "FromTick";
    private static final String FROM_CREDITS = "FromCredits";
    /** What one weighs: the header and its two numbers. */
    private static final long BYTES = Heap.HEADER + 2L * Heap.REFERENCE;

    private Stopwatches() {
    }

    /** A stopwatch started now. */
    static Values.Obj start(final Process process, final int line) {
        final Values.Obj made = new Values.Obj(TYPE);
        restart(process, made);
        return process.heap0().allocate(made, BYTES, line);
    }

    /** Starts {@code watch} again from now, and hands it back. */
    static Values.Obj restart(final Process process, final Values.Obj watch) {
        watch.set(FROM_TICK, process.host().tick());
        watch.set(FROM_CREDITS, process.spent());
        return watch;
    }

    /** The ticks gone by since {@code watch} was started. */
    static long ticks(final Process process, final Values.Obj watch) {
        return process.host().tick() - number(watch, FROM_TICK);
    }

    /** The credits the program spent since {@code watch} was started. */
    static long credits(final Process process, final Values.Obj watch) {
        return process.spent() - number(watch, FROM_CREDITS);
    }

    private static long number(final Values.Obj watch, final String field) {
        return watch.get(field) instanceof Number value ? value.longValue() : 0L;
    }
}
