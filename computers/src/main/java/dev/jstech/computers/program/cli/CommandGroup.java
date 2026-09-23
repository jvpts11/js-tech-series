/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

/**
 * What a command is for, which is the heading a machine that lists everything it can do puts it under.
 *
 * <p>Each command says so for itself, so one an add-on registers can stand under the right heading as well. A
 * command that says nothing is software, which is what most of what a player adds to a machine is. The order
 * here is the order a person meets them in.
 */
public enum CommandGroup {

    /** Reading, writing and moving files. */
    FILES,

    /** Working on the lines of a text. */
    TEXT,

    /** The machine itself: its memory, its tasks, its name and its clock. */
    MACHINE,

    /** Reaching other machines and what the network holds. */
    NETWORK,

    /** The programs on the machine and the work it is left with. */
    SOFTWARE
}
