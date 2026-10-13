/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * Things a program does that its host may want to hear of, beyond what it prints: the host decides what they are
 * worth. The virtual machine only says that they happened.
 */
public enum ProgramMilestone {

    /** The program halted for calling deeper than it may. */
    STACK_OVERFLOW,

    /** The program halted dividing a whole number by zero. */
    DIVIDE_BY_ZERO,

    /** The program started a thread besides its first. */
    THREAD_STARTED,

    /** The program opened a window on the machine's desktop. */
    WINDOW_OPENED,

    /** A watch the program set on the network's stock woke it. */
    WATCH_FIRED
}
