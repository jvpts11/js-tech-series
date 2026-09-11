/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

/**
 * Everything a running program asks of the world outside it.
 *
 * <p>This is the only door. What a program can reach is what a host answers for, and a host that
 * answers for nothing is a perfectly good one: that is what lets the whole language be run and read
 * back with no world around it, which is how it is tested. The machine a program really runs on answers
 * for a great deal more.
 */
public interface IHost {

    /** The tick the server is on. */
    long tick();

    /** How far through the day it is, in ticks. */
    long dayTime();

    /** Which day it is. */
    long day();

    /**
     * What a call on one of the host's own objects gave back.
     *
     * <p>Everything in {@code value} and {@code filled} has to be something a process can be written
     * down with: a number, a piece of text, or one of the kinds in {@link Values}. Nothing opaque, ever,
     * because a program stopped in the middle of one of these has to come back after a reload.
     *
     * <p>{@code cost} is what the call is worth in instructions. Reaching into the world is not free,
     * and this is the knob that says how much it is not free by.
     */
    record Reply(Object value, java.util.List<Object> filled, int cost) {

        public Reply {
            filled = filled == null ? java.util.List.of() : java.util.List.copyOf(filled);
        }

        /** An answer that filled nothing in. */
        public static Reply of(final Object value, final int cost) {
            return new Reply(value, java.util.List.of(), cost);
        }
    }

    /** Whether this host answers for that object at all. */
    default boolean provides(final String owner) {
        return false;
    }

    /**
     * Whether a call of that member is made on one of the host's own objects rather than on the type.
     *
     * <p>The object is then handed over first, ahead of the arguments, so a host that answers for a
     * kind of thing (another computer, say) can tell which one the program means.
     */
    default boolean takesTarget(final String owner, final String member) {
        return false;
    }

    /**
     * Answers a call on one of the host's objects.
     *
     * <p>Only ever asked for an owner {@link #provides} said yes to. Throw {@link Halt} for anything the
     * program did wrong; returning null is a fine answer for a method that gives nothing back.
     *
     * <p>One host serves every program on its machine, so {@code caller} says which of them is asking.
     * Most calls have no use for it; the ones that leave a mark on the world outside do, because a
     * record of who did what is worth nothing if it only ever says "a program".
     *
     * @param caller the name of the class the asking program was started from
     */
    default Reply call(final String owner, final String member, final java.util.List<Object> arguments,
                       final String caller, final int line) {
        throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "this computer cannot reach " + owner);
    }

    /**
     * The same, with the number the machine lists the asking program under.
     *
     * <p>Most calls have no use for it either; the ones about other programs on the machine do, since
     * a program that starts another is its parent and a message has to say who sent it. A host that
     * does not care answers the plain form.
     *
     * @param callerId the machine's number for the asking program, or 0 for a program it never numbered
     */
    default Reply call(final String owner, final String member, final java.util.List<Object> arguments,
                       final String caller, final int callerId, final int line) {
        return this.call(owner, member, arguments, caller, line);
    }


    /** A host for a program that has no world around it, whose clock never moves. */
    static IHost still() {
        return new IHost() {
            @Override
            public long tick() {
                return 0;
            }

            @Override
            public long dayTime() {
                return 0;
            }

            @Override
            public long day() {
                return 0;
            }
        };
    }
}
