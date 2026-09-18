/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.operation.payload.program.TerminalTools;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

/**
 * A terminal on a machine, typed at the way a player's terminal types at it.
 *
 * <p>Running a line through the shell is not the whole of what typing one does. A line typed while a tool is
 * in front goes to the tool and not to the shell, and a command that leaves a tool running only leaves it
 * running once the machine has been told to put it in front of the terminal. A test that skipped that would
 * be testing a shell no player ever meets, so this does what the machine's own handlers do, in their order.
 */
public final class TerminalAt {

    private final IComputerTerminalHost host;
    private final ServerLevel level;

    public TerminalAt(final IComputerTerminalHost host, final ServerLevel level) {
        this.host = host;
        this.level = level;
    }

    /**
     * Types one line and gives back what was printed at once: what the command said, and the opening of
     * whatever tool it left running. What a tool prints after that goes by on the machine's own tick.
     */
    public String type(final String line) {
        final TerminalTools.Turn inFront = TerminalTools.typed(this.host, this.level, line);
        if (inFront != null) {
            return wire(inFront.lines());
        }
        final ServerCliComputer computer = new ServerCliComputer(this.host, this.level);
        final CliShell.Response response =
                CliCommands.shellFor(computer, TermBuffer.MONITOR_COLUMNS).run(line, computer);
        final StringBuilder out = new StringBuilder();
        for (final CliLine said : response.lines()) {
            out.append(said.text()).append('\n');
        }
        if (response.started() != null) {
            out.append(wire(TerminalTools.started(this.host, this.level, line, response.started()).lines()));
        }
        return out.toString();
    }

    /** Whether a tool is in front of the terminal, which is when the prompt is away. */
    public boolean busy() {
        return this.host.console() != null && this.host.console().foreground().running();
    }

    /** What the tool in front has stopped to ask, or empty when nothing is asking. */
    public String asking() {
        final var tool = this.host.console() == null ? null : this.host.console().foreground().tool();
        return tool == null || tool.asking() == null ? "" : tool.asking().text().text();
    }

    /** The shell's own prompt, which says where the session is standing. */
    public String prompt() {
        return new ServerCliComputer(this.host, this.level).prompt();
    }

    private static String wire(final List<WireLine> lines) {
        final StringBuilder out = new StringBuilder();
        for (final WireLine line : lines) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }
}
