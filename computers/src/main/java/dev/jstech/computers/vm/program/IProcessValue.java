/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/** The Java that reads a value the program's own process or the language's core answers. */
@FunctionalInterface
interface IProcessValue {

    /**
     * Reads the value, halting the program the way the runtime halts one when the read is wrong.
     *
     * @param process the process reading it
     * @param target  what it is read from, or null for a value read from the type
     * @param line    the line reading it, for what the program is told when it goes wrong
     * @return the value
     */
    Object read(Process process, Object target, int line);
}
