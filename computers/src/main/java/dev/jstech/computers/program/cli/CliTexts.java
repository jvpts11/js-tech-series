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
