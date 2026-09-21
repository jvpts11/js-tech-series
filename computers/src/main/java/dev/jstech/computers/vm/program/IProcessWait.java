/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/** Whether a call the program's own process answers has to wait before it can be answered. */
@FunctionalInterface
interface IProcessWait {

    /**
     * Whether the call has to wait. A call that waits has already set up what wakes its thread and has taken nothing
     * off the stack, so asking it again once the thread wakes is the same as asking it once.
     *
     * @param process the process making the call
     * @param frame   the frame making it, whose stack still holds what the call is handed
     * @param count   how many arguments the call takes, which lie on top of the object it is made on
     * @param line    the line making the call, for what the program is told when it goes wrong
     * @return whether the call waits
     */
    boolean waits(Process process, Frame frame, int count, int line);
}
