/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * The end of a process that did not finish on its own.
 *
 * <p>The language has no exceptions, so nothing here can be caught: a halt stops the process where
 * it stands, and the message is what the player reads in its console. That is the whole of the error
 * handling a v1 program has, which is why the message has to say enough to act on.
 */
public final class Halt extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * What went wrong, in the words the console uses, with a number of its own that a reason is written down by, so the
     * order the reasons are declared in never decides what a written number means.
     */
    public enum Reason implements IStableId {
        DIVIDE_BY_ZERO(1, "divided by zero"),
        NO_OBJECT(2, "reached into nothing"),
        USE_AFTER_DISPOSE(3, "used after dispose"),
        OUT_OF_MEMORY(4, "out of memory"),
        BAD_CAST(5, "not of that type"),
        OUT_OF_RANGE(6, "outside the collection"),
        NO_SUCH_MEMBER(7, "no such member"),
        NO_NETWORK(8, "no network"),
        NOT_LOCKED(9, "not holding the lock"),
        CANNOT_START(10, "could not start"),
        REFUSED(11, "refused"),
        /** A thread's calls went deeper than they may: most often a method that calls itself without end. */
        STACK_DEPTH(12, "called too deep"),
        /** The runtime itself failed on an instruction: a fault of the machine, kept to the one process. */
        FAULT(13, "failed inside the runtime");

        private static final StableIds<Reason> IDS = StableIds.of(Reason.class);

        private final int id;
        private final String text;

        Reason(final int id, final String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public int id() {
            return this.id;
        }

        /** How the reason reads on its own. */
        public String text() {
            return this.text;
        }

        /** The reason that declares {@code id}; an id no reason declares reads as a fault of the runtime. */
        public static Reason byId(final int id) {
            return IDS.byId(id, FAULT);
        }
    }

    private final transient Reason reason;
    private final int line;

    public Halt(final Reason reason, final int line, final String message) {
        super(message);
        this.reason = reason;
        this.line = line;
    }

    /** Why the process stopped. */
    public Reason reason() {
        return this.reason;
    }

    /** The line of the assembly it stopped on, or 0 when it was not inside one. */
    public int line() {
        return this.line;
    }
}
