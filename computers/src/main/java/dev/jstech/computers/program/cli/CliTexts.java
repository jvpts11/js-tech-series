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
 * What every family of commands says the same way, declared once so a machine with no system reads alike whichever
 * command found it out.
 */
@TextHolder
public final class CliTexts {

    public static final TextKey NO_SYSTEM = TextKey.of("jsc.cli.no_system", "no system disk or OS installed");

    /** A command's name in front of what it has to say, which is how every Unix tool opens a complaint. */
    public static final TextKey SAID_BY = TextKey.of("jsc.cli.said_by", "%s: %s");

    /** How a command is typed, said when it was typed some other way: its name, then its usage. */
    public static final TextKey USAGE = TextKey.of("jsc.cli.usage", "usage: %s %s");

    /** A word no command answers to, said alike by the prompt and by a script the shell runs. */
    public static final TextKey NOT_FOUND = TextKey.of("jsc.cli.shell.not_found", "command not found: %s");

    /** A command that broke instead of answering, said alike by the prompt and by a script. */
    public static final TextKey FAILED = TextKey.of("jsc.cli.shell.failed", "error running '%s': %s");

    /* The system messages of the DOS prompt, which every command that reaches a drive answers with alike. */
    public static final TextKey BAD_SYNTAX =
            TextKey.of("jsc.cli.dos.bad_syntax", "The syntax of the command is incorrect.");
    public static final TextKey PATH_NOT_FOUND =
            TextKey.of("jsc.cli.dos.path_not_found", "The system cannot find the path specified.");
    public static final TextKey FILE_NOT_FOUND =
            TextKey.of("jsc.cli.dos.file_not_found", "The system cannot find the file specified.");

    private CliTexts() {
    }
}
