/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The programs the prompt starts on the machine's behalf are real programs, and this is what says so:
 * they compile from their own source in this jar, and they compile ONCE.
 */
class ShellProgramsTest {

    @Test
    void transfer_compilesFromItsOwnSource() {
        final String made = ShellPrograms.transfer();
        assertFalse(made.isEmpty(), () -> "the transfer program is in the jar and compiles: "
                + ShellPrograms.failure());
        assertTrue(made.contains(".start"), "and is an assembly a machine can run; got " + made.lines().findFirst());
        assertTrue(made.contains("Gateway.Read"), "it reads across the bridge");
        assertTrue(made.contains("Gateway.Write"), "and writes across it");
        assertTrue(made.contains("Gateway.Program"), "and a program crosses translated");
    }

    @Test
    void transfer_isCompiledOnceAndKept() {
        assertEquals(ShellPrograms.transfer(), ShellPrograms.transfer());
    }
}
