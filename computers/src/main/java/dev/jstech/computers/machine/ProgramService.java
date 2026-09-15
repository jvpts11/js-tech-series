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
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.computers.vm.program.ProgramPriority;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The programs on a machine, as what runs on the machine reaches them.
 *
 * <p>A program can be started from a compiled file on the machine's disks, asked how it is getting on, read, stopped
 * and sent a line; a program another one started on a computer of the network can be asked after the same way. The
 * machine's list of programs is the only place any of this lives, so what is started here shows in the Task Manager
 * like anything started at the prompt.
 */
public final class ProgramService {

    private final AbstractComputerBlockEntity machine;
    private final ServerCliComputer shell;

    ProgramService(final AbstractComputerBlockEntity machine, final ServerCliComputer shell) {
        this.machine = machine;
        this.shell = shell;
    }

    /**
     * Starts a compiled program from the machine's disks, as the prompt would, without handing it the terminal: what
     * it prints goes to a console of its own that whoever started it can read.
     */
    public ProgramLauncher.Launch start(final String path, final List<String> arguments, final IProgramParent parent,
                                        final ProgramPriority priority) {
        return ProgramLauncher.launch(this.machine, path, this.shell::readFile, arguments, parent, priority, 0);
    }

    /** Whether the program under that number, on this machine or on the computer host names, is still going. */
    public boolean running(final int id, final String host) {
        final ProgramEntry<IMachineRuntime> one = this.find(id, host);
        return one != null && MachinePrograms.running(one.process());
    }

    /** The code the program under that number ended with, or 0 when it has none or is gone. */
    public int exitCode(final int id, final String host) {
        final ProgramEntry<IMachineRuntime> one = this.find(id, host);
        return one == null ? 0 : one.process().exitCode();
    }

    /** What the program under that number printed, or nothing when it is gone. */
    public List<String> output(final int id, final String host) {
        final ProgramEntry<IMachineRuntime> one = this.find(id, host);
        return one == null ? List.of() : one.process().console();
    }

    /** Stops the program under that number; false when there was none to stop. */
    public boolean kill(final int id, final String host) {
        final AbstractComputerBlockEntity where = this.machineFor(host);
        final boolean stopped = where != null && where.programs().stop(id);
        if (stopped) {
            where.setChanged();
        }
        return stopped;
    }

    /** Sends a line from one program on the machine to another; false when that one has no room for it or is gone. */
    public boolean send(final int from, final int to, final String text) {
        final long tick = this.machine.getLevel() == null ? 0L : this.machine.getLevel().getGameTime();
        return this.machine.programs().send(from, to, text, tick);
    }

    @Nullable
    private ProgramEntry<IMachineRuntime> find(final int id, final String host) {
        final AbstractComputerBlockEntity where = this.machineFor(host);
        return where == null ? null : where.programs().byId(id);
    }

    /**
     * The machine a program's number is on: this one when no host is named, or the one of the network the name picks
     * out. Null when that one is not on the network any more, or cannot run programs at all.
     */
    @Nullable
    private AbstractComputerBlockEntity machineFor(final String host) {
        if (host == null || host.isEmpty()) {
            return this.machine;
        }
        final ServerCliComputer remote = this.shell.remoteShell(host);
        return remote != null && remote.machine() instanceof AbstractComputerBlockEntity there ? there : null;
    }
}
