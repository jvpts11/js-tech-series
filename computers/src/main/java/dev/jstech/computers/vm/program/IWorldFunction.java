/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * The Java that answers a call the system declares as the world's, as the machine a program runs on hands it over.
 *
 * <p>The runtime does everything around it: it takes the arguments off the stack, charges what the call is declared to
 * cost, and makes what comes back, and whatever the call fills in, the program's to hold. What it gives has to be
 * something a process can be written down with (a number, a piece of text, or one of the kinds in {@link Values}),
 * because a program stopped just after the call has to come back after a reload.
 */
@FunctionalInterface
public interface IWorldFunction {

    /**
     * Answers the call. Throw {@link Halt} for anything the program did wrong.
     *
     * @param target    the object the call is made on, or null for a call on a type
     * @param arguments what the call was handed, in order; a place the call fills in is empty, and is where the
     *                  function puts what it fills in
     * @return the answer, or null for a call that gives nothing
     */
    Object call(Object target, Object[] arguments, int line);
}
