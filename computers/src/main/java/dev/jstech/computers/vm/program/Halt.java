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
 * <p>A program written without exceptions cannot catch anything: a halt stops the process where it stands, and the
 * message is what the player reads in its console. A program that carries the language's exceptions is thrown one in
 * its place instead, of the type {@link Thrown} names, which it may catch; uncaught, it stops the program the same.
 * Running out of memory and calling too deep are thrown nothing, as .NET's stack overflow is not: catching the lack of
 * memory would need memory.
 *
 * <p>The message is a sentence, read in the player's language on the glass; as a Java exception it reads in English,
 * the language the machine keeps its logs in.
 */
public final class Halt extends RuntimeException {

    private final transient Reason reason;
    private final int line;
    private final transient Text text;
    private final transient Thrown thrown;

    private static final long serialVersionUID = 1L;

    public Halt(final Reason reason, final int line, final Text message) {
        this(reason, line, message, Thrown.of(reason));
    }

    /** A halt that throws a program that catches an exception of its own kind, rather than its reason's. */
    public Halt(final Reason reason, final int line, final Text message, final Thrown thrown) {
        super(message.english());
        this.reason = reason;
        this.line = line;
        this.text = message;
        this.thrown = thrown;
    }

    /** The exception a program that carries exceptions is thrown in place of the halt. */
    public Thrown thrown() {
        return this.thrown;
    }

    /**
     * The exceptions halts are thrown as, by the name each type has in a listing; {@link #NONE} for the halts that are
     * thrown nothing.
     */
    public enum Thrown {
        NONE(""),
        NULL_REFERENCE("NullReferenceException"),
        OBJECT_DISPOSED("ObjectDisposedException"),
        DIVIDE_BY_ZERO("DivideByZeroException"),
        INDEX_OUT_OF_RANGE("IndexOutOfRangeException"),
        ARGUMENT("ArgumentException"),
        ARGUMENT_OUT_OF_RANGE("ArgumentOutOfRangeException"),
        INVALID_OPERATION("InvalidOperationException"),
        FORMAT("FormatException"),
        IO("IOException"),
        FILE_NOT_FOUND("FileNotFoundException"),
        NETWORK("NetworkException"),
        ACCESS_VIOLATION("AccessViolationException");

        private final String type;

        Thrown(final String type) {
            this.type = type;
        }

        /** The type the exception is made of, as the listing names it. */
        public String type() {
            return this.type;
        }

        /** What a halt of that reason throws, where the place that halts does not say otherwise. */
        public static Thrown of(final Reason reason) {
            return switch (reason) {
                case DIVIDE_BY_ZERO -> DIVIDE_BY_ZERO;
                case NO_OBJECT -> NULL_REFERENCE;
                case USE_AFTER_DISPOSE -> OBJECT_DISPOSED;
                case OUT_OF_RANGE -> ARGUMENT_OUT_OF_RANGE;
                case NO_NETWORK -> NETWORK;
                case ACCESS_VIOLATION -> ACCESS_VIOLATION;
                case BAD_CAST, NO_SUCH_MEMBER, NOT_LOCKED, CANNOT_START, REFUSED -> INVALID_OPERATION;
                case OUT_OF_MEMORY, STACK_DEPTH, FAULT, UNCAUGHT -> NONE;
            };
        }
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
        FAULT(13),
        /** An exception the program threw that nothing caught. */
        UNCAUGHT(14),
        /** A read or a write through a pointer outside every live block: freed, past its end, or null. */
        ACCESS_VIOLATION(15);

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
