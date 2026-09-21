/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.MemberId;

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
     * What answers a call the system declares as the world's, or null when this host does not answer it.
     *
     * <p>Asked once for each such call a program makes, when its process is made or read back, never while it runs.
     */
    default IWorldFunction bind(final MemberId id) {
        return null;
    }

    /** Whether the machine has a desktop a program can open its windows on. */
    default boolean hasDesktop() {
        return false;
    }

    /**
     * Whether the program the machine lists under that number is still going: on this machine when {@code host} is
     * empty, or on the computer of the network it names. Asked for nothing, between slices, by a program waiting on
     * it; a host with no programs to speak of knows of none.
     */
    default boolean programRunning(final int program, final String host) {
        return false;
    }

    /**
     * Told when the runtime itself failed while running one of this host's programs.
     *
     * <p>The program has already been stopped with a message of its own; this is where the fault is
     * written down for whoever has to fix it. A host with nowhere to write it lets it go.
     *
     * @param process the name the program is listed under
     * @param line    the instruction it was on, or 0 when it was between instructions
     */
    default void fault(final String process, final int line, final RuntimeException cause) {
    }

    /**
     * Told once when one of this host's programs has ended for good: returned, exited or halted. A host that runs
     * several programs tells the others, so whatever of them waits on that program runs again.
     *
     * @param program the machine's number for the program that ended
     */
    default void programEnded(final int program) {
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
