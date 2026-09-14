/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * What a thread is waiting for, if anything.
 *
 * <p>Each kind carries only what its own wait needs: a sleep the tick it ends on, a join the thread and when it gives
 * up, a lock the object, a wait on another program which program on which machine and when it gives up. A tick of
 * zero is never. A thread that waits is given no budget until its wait is over, so waiting costs nothing.
 */
sealed interface IWait {

    /** Not waiting. */
    None NONE = new None();

    /** Waiting for a line to be typed at the terminal. */
    Input INPUT = new Input();

    /** Not waiting: the thread runs when it is given budget. */
    record None() implements IWait {
    }

    /** Waiting for a line to be typed at the terminal. */
    record Input() implements IWait {
    }

    /** Waiting for the tick {@code until}. */
    record Sleep(long until) implements IWait {
    }

    /** Waiting for the thread numbered {@code thread} to end, or for the tick {@code until} when it is not zero. */
    record Join(int thread, long until) implements IWait {
    }

    /** Waiting for another thread to let go of the lock of {@code target}. */
    record Lock(Object target) implements IWait {
    }

    /**
     * Waiting for the program numbered {@code program} to end on the machine {@code host} (empty for this one), or for
     * the tick {@code until} when it is not zero.
     */
    record Child(int program, String host, long until) implements IWait {
    }

    // the names and numbers a snapshot writes a wait as

    /** The name a snapshot writes the wait under. */
    static String kindOf(final IWait wait) {
        return switch (wait) {
            case None none -> "none";
            case Input input -> "input";
            case Sleep sleep -> "sleep";
            case Join join -> "join";
            case Lock lock -> "lock";
            case Child child -> "child";
        };
    }

    /** The tick the wait ends or gives up on, zero for never. */
    static long untilOf(final IWait wait) {
        return switch (wait) {
            case Sleep sleep -> sleep.until();
            case Join join -> join.until();
            case Child child -> child.until();
            case None none -> 0L;
            case Input input -> 0L;
            case Lock lock -> 0L;
        };
    }

    /** What the wait is on: the thread's or the program's number, or the locked object; null for the rest. */
    static Object onOf(final IWait wait) {
        return switch (wait) {
            case Join join -> join.thread();
            case Lock lock -> lock.target();
            case Child child -> child.program();
            case None none -> null;
            case Input input -> null;
            case Sleep sleep -> null;
        };
    }

    /** The machine a wait on another program looks at, empty for this one and for every other kind. */
    static String hostOf(final IWait wait) {
        return wait instanceof Child child ? child.host() : "";
    }

    /**
     * The wait a snapshot wrote.
     *
     * @throws IllegalArgumentException for a name no wait declares, which only a damaged snapshot holds
     */
    static IWait read(final String kind, final long until, final Object on, final String host) {
        return switch (kind) {
            case "none" -> NONE;
            case "input" -> INPUT;
            case "sleep" -> new Sleep(until);
            case "join" -> new Join(on instanceof Integer thread ? thread : 0, until);
            case "lock" -> new Lock(on);
            case "child" -> new Child(on instanceof Integer program ? program : 0, host == null ? "" : host, until);
            default -> throw new IllegalArgumentException("no thread wait is named '" + kind + "'");
        };
    }
}
