/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

/** Who answers a call of the system. */
public enum MemberKind {

    /** The language, in Java, with nothing but what the call hands over: text, collections, numbers. */
    PURE,

    /** The calling program's own process: its threads, its console, its windows, its name. */
    PROCESS,

    /** The machine the program runs on and the world around it: its disks, its network, other computers. */
    WORLD
}
