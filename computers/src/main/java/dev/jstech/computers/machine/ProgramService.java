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
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The programs on a machine, as what runs on the machine reaches them.
 *
 * <p>A program can be started from a compiled file on the machine's disks, asked how it is getting on, read, stopped
 * and sent a line; a program another one started on a computer of the network can be asked after the same way. The
 * machine's list of programs is the only place any of this lives, so what is started here shows in the Task Manager
 * like anything started at the prompt.
 */
@TextHolder
public final class ProgramService {

    private final AbstractComputerBlockEntity machine;
    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The machine's drives, which is where a program is read from before it is started. */
    private final FileService files;
    /** The other computers of the network, for a program asked after by the machine it runs on. */
    private final RemoteComputerService remotes;

    /*
     * Why a program did not start, said alike at the prompt, to a program that asked for another, and to a desktop
     * that opened one.
     */
    static final TextKey NO_RUNNER = TextKey.of("jsc.service.programs.no_runner",
            "%s: nothing installed runs a program of this kind (compile a source file first)");
    public static final TextKey NO_ROOM =
            TextKey.of("jsc.service.programs.no_room", "%s: %s MB will not fit in %s MB of free memory");

    private static final TextKey NOTHING_RUNNING_AS =
            TextKey.of("jsc.service.programs.nothing_running_as", "nothing is running as %s");
    private static final TextKey STOPPED = TextKey.of("jsc.service.programs.stopped", "stopped %s");

    /** How wide a prompt is taken to be for a line run there. */
    private static final int SHELL_WIDTH = 80;

    public ProgramService(final AbstractComputerBlockEntity machine, final IComputerTerminalHost terminal,
                          final ServerLevel level, final FileService files, final RemoteComputerService remotes) {
        this.machine = machine;
        this.terminal = terminal;
        this.level = level;
        this.files = files;
        this.remotes = remotes;
    }

    /**
     * Starts a compiled program from the machine's disks, as the prompt would, without handing it the terminal: what
     * it prints goes to a console of its own that whoever started it can read.
     */
    public ProgramLauncher.Launch start(final String path, final List<String> arguments, final IProgramParent parent,
                                        final ProgramPriority priority) {
        return ProgramLauncher.launch(this.machine, path, this.files::readFile, arguments, parent, priority, 0);
    }

    /**
     * Runs one line at the machine's own prompt and hands back what it printed.
     *
     * <p>The line runs on a shell made for it. What a command sets on a shell, a reboot asked for or the terminal
     * window it speaks for, belongs to whoever sits at that shell, and the one the machine keeps for its programs is
     * one no command ever touches.
     */
    public List<String> shell(final String command) {
        return run(new ServerCliComputer(this.terminal, this.level), command);
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

    /** What the program under that number printed, in English as a program reads it, or nothing when it is gone. */
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

    /**
     * The program sitting in front of the machine's terminal, or null when none is.
     *
     * <p>A machine has one prompt, so it has at most one program in front of it; whoever is at the keyboard is
     * typing at that program until it returns.
     */
    @Nullable
    public MachinePrograms foreground() {
        return this.machine.programs().held() != 0 ? this.machine.programs() : null;
    }

    /**
     * Starts a program from the prompt, which is not the same as starting one for another program.
     *
     * <p>A program started this way that runs at a terminal TAKES it, the way it does on any machine: the prompt is
     * its until it returns. One that is a service does not, and says what it started instead.
     */
    public ICliComputer.OpResult startAtTerminal(final String path, final int heapMb, final List<String> arguments) {
        final ProgramLauncher.Launch launch = ProgramLauncher.launch(this.machine, path, this.files::readFile,
                arguments, IProgramParent.NONE, ProgramPriority.MEDIUM, heapMb);
        if (!launch.ok()) {
            return ICliComputer.OpResult.fail(switch (launch.refusal()) {
                case NO_RUNNER -> NO_RUNNER.with(path);
                case NO_MEMORY -> NO_ROOM.with(Text.literal("sigma"), launch.roomMb(), launch.freeMb());
                case UNREADABLE, NOT_STARTED -> launch.said();
            });
        }
        final ProgramEntry<IMachineRuntime> one = this.machine.programs().byId(launch.id());
        if (one != null && !one.process().isService()) {
            this.machine.programs().hold(launch.id());
            return ICliComputer.OpResult.ok("");
        }
        return ICliComputer.OpResult.ok(launch.said());
    }

    /** Stops the program under that number, in the words the prompt answers with. */
    public ICliComputer.OpResult stop(final int id) {
        if (!this.machine.programs().stop(id)) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(Text.literal("sigma"),
                    NOTHING_RUNNING_AS.with(id)));
        }
        this.machine.setChanged();
        return ICliComputer.OpResult.ok(STOPPED.with(id));
    }

    /** Every program running on this machine, as a prompt lists them. */
    public List<ICliComputer.SigmaProcess> processes() {
        final List<ICliComputer.SigmaProcess> running = new ArrayList<>();
        for (final ProgramView one : this.machine.programs().view()) {
            running.add(new ICliComputer.SigmaProcess(one.id(), one.name(), one.state(), one.heldBytes(),
                    one.heapBytes(), one.file()));
        }
        return running;
    }

    /** Runs one line at a computer's prompt, as that shell sees it, and hands back what it printed. */
    static List<String> run(final ServerCliComputer on, final String command) {
        final CliShell prompt = CliCommands.newShell(SHELL_WIDTH);
        final List<String> lines = new ArrayList<>();
        for (final CliLine printed : prompt.run(command, on).lines()) {
            lines.add(printed.text());
        }
        return lines;
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
        final ServerCliComputer remote = this.remotes.find(host);
        return remote != null && remote.machine() instanceof AbstractComputerBlockEntity there ? there : null;
    }
}
