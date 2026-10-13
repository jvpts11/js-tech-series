/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What a command is for, which is the heading a machine that lists everything it can do puts it under.
 *
 * <p>Each command says so for itself, so one an add-on registers can stand under the right heading as well. A
 * command that says nothing is software, which is what most of what a player adds to a machine is. The order
 * here is the order a person meets them in.
 */
@TextHolder
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
    SOFTWARE,

    /** Writing programs: the editors, the compiler and the tools that package what it makes. */
    PROGRAMMING,

    /** Finding out what a machine can do: the manual and what searches it. */
    HELP;

    private static final TextKey FILES_TITLE = TextKey.of("jsc.cli.group.files", "Files");
    private static final TextKey TEXT_TITLE = TextKey.of("jsc.cli.group.text", "Text");
    private static final TextKey MACHINE_TITLE = TextKey.of("jsc.cli.group.machine", "The Machine");
    private static final TextKey NETWORK_TITLE = TextKey.of("jsc.cli.group.network", "The Network");
    private static final TextKey SOFTWARE_TITLE = TextKey.of("jsc.cli.group.software", "Software");
    private static final TextKey PROGRAMMING_TITLE = TextKey.of("jsc.cli.group.programming", "Writing programs");
    private static final TextKey HELP_TITLE = TextKey.of("jsc.cli.group.help", "Finding your way");

    /** What the heading says. */
    public TextKey title() {
        return switch (this) {
            case FILES -> FILES_TITLE;
            case TEXT -> TEXT_TITLE;
            case MACHINE -> MACHINE_TITLE;
            case NETWORK -> NETWORK_TITLE;
            case SOFTWARE -> SOFTWARE_TITLE;
            case PROGRAMMING -> PROGRAMMING_TITLE;
            case HELP -> HELP_TITLE;
        };
    }
}
