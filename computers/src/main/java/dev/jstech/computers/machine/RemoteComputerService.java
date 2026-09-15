/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The other computers of a machine's network, as what runs on the machine reaches them.
 *
 * <p>A program there can be started, a line run at that computer's prompt, a line sent to one of its programs, and its
 * programs listed. Everything runs on the other computer, out of its own budget and under its own name; whether it
 * takes any of this at all is the other computer's to say.
 */
public final class RemoteComputerService {

    /** How wide the other computer's prompt is taken to be for a line run there. */
    private static final int SHELL_WIDTH = 80;

    private final ServerCliComputer shell;

    RemoteComputerService(final ServerCliComputer shell) {
        this.shell = shell;
    }

    /** The one computer of the network that host name picks out, as its own shell; null when none or several do. */
    @Nullable
    public ServerCliComputer find(final String host) {
        return this.shell.remoteShell(host);
    }

    /**
     * The asking program as the other computer will know it, so that what it starts there is kept for it to read;
     * none for a caller that is not a numbered program on a computer of the network.
     */
    public IProgramParent parentOf(final int callerId) {
        return callerId > 0 && this.shell.machine() instanceof AbstractComputerBlockEntity machine
                && machine.nodeUuid() != null
                ? new IProgramParent.Remote(machine.getBlockPos().asLong(), machine.nodeUuid().value(), callerId)
                : IProgramParent.NONE;
    }

    /** Starts a compiled program from the other computer's disks, on that computer; null when it runs no programs. */
    @Nullable
    public ProgramLauncher.Launch start(final ServerCliComputer remote, final String path, final List<String> arguments,
                                        final IProgramParent parent, final ProgramPriority priority) {
        return remote.machine() instanceof AbstractComputerBlockEntity machine
                ? ProgramLauncher.launch(machine, path, remote::readFile, arguments, parent, priority, 0) : null;
    }

    /** Runs one line at the other computer's prompt and hands back what it printed. */
    public List<String> shell(final ServerCliComputer remote, final String command) {
        final CliShell prompt = CliCommands.newShell(SHELL_WIDTH);
        final List<String> lines = new ArrayList<>();
        for (final CliLine printed : prompt.run(command, remote).lines()) {
            lines.add(printed.text());
        }
        return lines;
    }

    /** Sends a line to a program on the other computer; false when it has no such program or no room for it. */
    public boolean send(final ServerCliComputer remote, final int from, final int to, final String text) {
        final long tick = remote.machine().getLevel() == null ? 0L : remote.machine().getLevel().getGameTime();
        return remote.machine() instanceof AbstractComputerBlockEntity machine
                && machine.programs().send(from, to, text, tick);
    }

    /** The programs the other computer is running. */
    public List<ProgramView> processes(final ServerCliComputer remote) {
        return remote.machine() instanceof AbstractComputerBlockEntity machine ? machine.programs().view() : List.of();
    }
}
