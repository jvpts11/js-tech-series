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
     * Whether the last timed join or wait on another program gave up, which the call that waited answers when it is
     * asked again. It is the answer of a wait that is over, not a wait, so it is kept apart from {@link #wait}.
     */
    private boolean timedOut;
    boolean yielded;

    ProgramThread(final int id) {
        this.id = id;
    }

    /** A timed wait ran out before what it waited for came. */
    void gaveUp() {
        this.timedOut = true;
    }

    /** Whether the last timed wait gave up; once asked, it is forgotten, so the answer is given only once. */
    boolean takeGaveUp() {
        final boolean was = this.timedOut;
        this.timedOut = false;
        return was;
    }

    /** Whether a timed wait gave up and the call that waited has still to hear it, for the save. */
    boolean givenUp() {
        return this.timedOut;
    }

    /** Puts back what the save wrote down. */
    void restoreGivenUp(final boolean timedOut) {
        this.timedOut = timedOut;
    }

    /** Whether the thread is waiting for something, and so is given no budget. */
    boolean waiting() {
        return this.wait != IWait.NONE;
    }
}
