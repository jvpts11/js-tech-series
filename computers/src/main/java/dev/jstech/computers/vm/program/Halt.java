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
import dev.jstech.core.text.Text;

/**
 * The end of a process that did not finish on its own.
 *
 * <p>The language has no exceptions, so nothing here can be caught: a halt stops the process where
 * it stands, and the message is what the player reads in its console. That is the whole of the error
 * handling a v1 program has, which is why the message has to say enough to act on.
 *
 * <p>The message is a sentence, read in the player's language on the glass; as a Java exception it reads in English,
 * the language the machine keeps its logs in.
 */
public final class Halt extends RuntimeException {

    private final transient Reason reason;
    private final int line;
    private final transient Text text;

    private static final long serialVersionUID = 1L;

    public Halt(final Reason reason, final int line, final Text message) {
        super(message.english());
        this.reason = reason;
        this.line = line;
        this.text = message;
    }

    /** Why the process stopped. */
    public Reason reason() {
        return this.reason;
    }

    /** The line of the assembly it stopped on, or 0 when it was not inside one. */
    public int line() {
        return this.line;
    }

    /** What the player is told, in their own language. */
    public Text text() {
        return this.text;
    }

    /**
     * What went wrong, with a number of its own that a reason is written down by, so the order the reasons are
     * declared in never decides what a written number means.
     */
    public enum Reason implements IStableId {
        DIVIDE_BY_ZERO(1),
        NO_OBJECT(2),
        USE_AFTER_DISPOSE(3),
        OUT_OF_MEMORY(4),
        BAD_CAST(5),
        OUT_OF_RANGE(6),
        NO_SUCH_MEMBER(7),
        NO_NETWORK(8),
        NOT_LOCKED(9),
        CANNOT_START(10),
        REFUSED(11),
        /** A thread's calls went deeper than they may: most often a method that calls itself without end. */
        STACK_DEPTH(12),
        /** The runtime itself failed on an instruction: a fault of the machine, kept to the one process. */
        FAULT(13);

        private static final StableIds<Reason> IDS = StableIds.of(Reason.class);

        private final int id;

        Reason(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return this.id;
        }

        /** The reason that declares {@code id}; an id no reason declares reads as a fault of the runtime. */
        public static Reason byId(final int id) {
            return IDS.byId(id, FAULT);
        }
    }
}
