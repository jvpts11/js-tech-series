/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/** The Java that answers a call the program's own process answers: its console, its random numbers, its threads. */
@FunctionalInterface
interface IProcessFunction {

    /**
     * Answers one call, halting the program the way the runtime halts one when the call is wrong.
     *
     * @param process   the process making the call
     * @param target    what the call is made on, or null for a call on the type
     * @param arguments what the call hands over, in order
     * @param line      the line making the call, for what the program is told when it goes wrong
     * @return the answer, or null for a call that gives nothing back
     */
    Object call(Process process, Object target, Object[] arguments, int line);
}
