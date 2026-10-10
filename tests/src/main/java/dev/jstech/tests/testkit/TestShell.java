/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Running a command on a machine's shell in a GameTest and reading what it printed, shared so every test captures a
 * command's output the same way.
 */
public final class TestShell {

    /** The width of the console the commands run on, in characters. */
    public static final int WIDTH = 80;

    private TestShell() {
    }

    /** The lines the machine's own shell (its OS family's commands, or the live installer's) prints for a command. */
    public static List<String> shell(final GameTestHelper helper, final IComputerTerminalHost on,
                                     final String command) {
        final ServerCliComputer computer = new ServerCliComputer(on, helper.getLevel());
        return texts(CliCommands.shellFor(computer, WIDTH).run(command, computer));
    }

    /** The lines the full command set (every family's verbs) prints for a command. */
    public static List<String> fullShell(final GameTestHelper helper, final IComputerTerminalHost on,
                                         final String command) {
        final ServerCliComputer computer = new ServerCliComputer(on, helper.getLevel());
        return texts(CliCommands.newShell(WIDTH).run(command, computer));
    }

    /** Whether any of the lines contains the text. */
    public static boolean says(final List<String> lines, final String text) {
        return lines.stream().anyMatch(line -> line.contains(text));
    }

    /** A response's lines joined into one string, each ended by a newline. */
    public static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }

    private static List<String> texts(final CliShell.Response response) {
        final List<String> out = new ArrayList<>();
        for (final CliLine line : response.lines()) {
            out.add(line.text());
        }
        return out;
    }
}
