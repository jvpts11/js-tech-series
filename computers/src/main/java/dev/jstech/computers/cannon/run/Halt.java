/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

/**
 * The end of a process that did not finish on its own.
 *
 * <p>The language has no exceptions, so nothing here can be caught: a halt stops the process where
 * it stands, and the message is what the player reads in its console. That is the whole of the error
 * handling a v1 program has, which is why the message has to say enough to act on.
 */
public final class Halt extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** What went wrong, in the words the console uses. */
    public enum Reason {
        DIVIDE_BY_ZERO("divided by zero"),
        NO_OBJECT("reached into nothing"),
        USE_AFTER_DISPOSE("used after dispose"),
        OUT_OF_MEMORY("out of memory"),
        BAD_CAST("not of that type"),
        OUT_OF_RANGE("outside the collection"),
        NO_SUCH_MEMBER("no such member"),
        NO_NETWORK("no network"),
        /** A Lua program raised an error, which carries any value and can be caught by pcall. */
        RAISED("error"),
        NOT_LOCKED("not holding the lock"),
        CANNOT_START("could not start"),
        REFUSED("refused");

        private final String text;

        Reason(final String text) {
            this.text = text;
        }

        /** How the reason reads on its own. */
        public String text() {
            return this.text;
        }
    }

    private final transient Reason reason;
    private final int line;
    private final transient Object value;

    public Halt(final Reason reason, final int line, final String message) {
        this(reason, line, message, null);
    }

    /** A halt that carries a value with it, which is what a Lua error is: pcall hands it back. */
    public Halt(final Reason reason, final int line, final String message, final Object value) {
        super(message);
        this.reason = reason;
        this.line = line;
        this.value = value;
    }

    /** Why the process stopped. */
    public Reason reason() {
        return this.reason;
    }

    /** What was raised, for a halt that was raised with a value; null for the others. */
    public Object value() {
        return this.value;
    }

    /** The line of the assembly it stopped on, or 0 when it was not inside one. */
    public int line() {
        return this.line;
    }
}
