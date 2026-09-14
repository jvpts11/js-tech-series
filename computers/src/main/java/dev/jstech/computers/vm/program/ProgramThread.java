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

/** One flow of control in a process: its own stack of calls over the process's shared heap. */
final class ProgramThread {
    final int id;
    final Deque<Frame> frames = new ArrayDeque<>();
    /** What the program holds the thread by, made the first time it asks for one. */
    Values.Obj token;
    /** What the thread is waiting for; it is given budget only while this is an {@link IWait.None}. */
    IWait wait = IWait.NONE;
    /**
     * Set when a timed join or wait on another program gave up, so the call that waited, asked again, answers that
     * it gave up. It is the answer of a wait that is over, not a wait.
     */
    boolean timedOut;
    boolean yielded;

    ProgramThread(final int id) {
        this.id = id;
    }

    /** Whether the thread is waiting for something, and so is given no budget. */
    boolean waiting() {
        return this.wait != IWait.NONE;
    }
}
