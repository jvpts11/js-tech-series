/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.program.cli.CliShell;
import java.util.Locale;

/**
 * Reading what a command-line shell answered in a GameTest, shared so every test matches output the same way.
 */
public final class TestCli {

    private TestCli() {
    }

    /** Whether any output line contains the needle, ignoring case. */
    public static boolean contains(final CliShell.Response response, final String needle) {
        final String lower = needle.toLowerCase(Locale.ROOT);
        return response.lines().stream()
                .anyMatch(line -> line.text().toLowerCase(Locale.ROOT).contains(lower));
    }
}
