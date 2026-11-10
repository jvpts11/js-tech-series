/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.List;

/**
 * One call in progress.
 *
 * <p>The stack holds nulls, because null is a value a program can have and hand around, so it is
 * kept in something that allows one rather than in something that treats it as an absence.
 */
final class Frame {
    final MethodImage method;
    final Object[] slots;
    final List<Object> stack = new ArrayList<>();
    final Object self;
    int at;
    /** Set on all but the last handler of a run, whose answers nobody is waiting for. */
    boolean discard;
    /** Set on a call the machine waits on for its answer, which is kept here when it returns. */
    boolean asked;
    boolean answered;
    Object answer;
    /** The places INTERCAL's ignore made every write to vanish from, or null while there are none. */
    boolean[] ignored;
    /**
     * What each finally running now in this call goes on with when it ends, the innermost last: a way out it is on the
     * way to, or an exception still being thrown. Null while no finally runs.
     */
    List<Unwinder.Pending> pendings;
    /** The addresses of the blocks of raw memory this call made with stackalloc, freed when it returns; or null. */
    List<Long> stackBlocks;

    Frame(final MethodImage method, final Object self) {
        this.method = method;
        this.slots = new Object[Math.max(method.slots(), method.parameters().size())];
        this.self = self;
    }

    /** Makes every later write to that place vanish, or count again. */
    void ignore(final int slot, final boolean on) {
        if (this.ignored == null) {
            if (!on) {
                return;
            }
            this.ignored = new boolean[this.slots.length];
        }
        this.ignored[slot] = on;
    }

    /** Puts a value in a place, unless the place is ignored, where the write vanishes. */
    void store(final int slot, final Object value) {
        if (this.ignored == null || !this.ignored[slot]) {
            this.slots[slot] = value;
        }
    }

    /** The places ignored, for writing the call down. */
    List<Integer> ignoredSlots() {
        final List<Integer> found = new ArrayList<>();
        for (int i = 0; this.ignored != null && i < this.ignored.length; i++) {
            if (this.ignored[i]) {
                found.add(i);
            }
        }
        return found;
    }

    /** The blocks this call made with stackalloc, for writing the call down. */
    List<Long> blocks() {
        return this.stackBlocks == null ? List.of() : List.copyOf(this.stackBlocks);
    }

    /** Makes a block of raw memory that goes when this call returns, and gives its address. */
    long stackBlock(final RawMemory memory, final long bytes, final int line) {
        final long address = memory.allocate(bytes, true, line);
        if (this.stackBlocks == null) {
            this.stackBlocks = new ArrayList<>();
        }
        this.stackBlocks.add(address);
        return address;
    }

    /** Lets go of every block this call made with stackalloc, as it returns or is unwound. */
    void releaseBlocks(final RawMemory memory) {
        if (this.stackBlocks == null) {
            return;
        }
        for (final long address : this.stackBlocks) {
            memory.release(address);
        }
        this.stackBlocks = null;
    }

    void push(final Object value) {
        this.stack.add(value);
    }

    Object pop() {
        return this.stack.isEmpty() ? null : this.stack.remove(this.stack.size() - 1);
    }

    Object peek() {
        return this.stack.isEmpty() ? null : this.stack.getLast();
    }
}
