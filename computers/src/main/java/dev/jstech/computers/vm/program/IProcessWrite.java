/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/** The Java that writes a value the program's own process answers, such as what a widget shows. */
@FunctionalInterface
interface IProcessWrite {

    /**
     * Writes the value, halting the program the way the runtime halts one when the write is wrong.
     *
     * @param process the process writing it
     * @param target  what it is written on
     * @param value   what is written
     * @param line    the line writing it, for what the program is told when it goes wrong
     */
    void write(Process process, Object target, Object value, int line);
}
