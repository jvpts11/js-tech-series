/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.cannon.CannonCosts;
import dev.jstech.computers.cannon.run.Halt;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Values;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.ICliComputer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Another computer on the network, as a running program reaches it.
 *
 * <p>A program can start a program there, run a line at its prompt, send a line to one of its programs
 * and list what it is running. Everything runs on the other machine, out of its own budget, under its
 * own name; what comes back here is a handle. The other machine decides whether it takes any of this
 * ({@code config remote off} says no), and being off or gone is an answer too.
 */
public final class HostRemote {

    private static final int START = CannonCosts.SUBMIT;
    private static final int TOUCH = CannonCosts.GLANCE_NETWORK;
    private static final int READ = CannonCosts.READ;

    /** How wide the other machine's prompt is taken to be for a line run there. */
    private static final int SHELL_WIDTH = 80;

    private HostRemote() {
    }

    /** Whether this is one of the calls handled here. */
    public static boolean handles(final String owner) {
        return "RemoteComputer".equals(owner);
    }

    /**
     * Answers one of them. The first argument is the computer the program holds; the rest are the
     * call's own.
     *
     * @param callerId the machine's number for the asking program, named as the sender of a line and as
     *                 the parent of what it starts
     */
    public static IHost.Reply call(final ServerCliComputer shell, final int callerId, final String member,
                                  final List<Object> arguments, final int line) {
        final String host = hostOf(arguments);
        final ServerCliComputer remote = shell.remoteShell(host);
        if (remote == null) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, host + ": no such computer on this network");
        }
        if (!remote.running()) {
            throw new Halt(Halt.Reason.REFUSED, line, host + " is powered off");
        }
        if (!remote.remoteAllowed()) {
            throw new Halt(Halt.Reason.REFUSED, line, host + " does not take programs from other computers");
        }
        return switch (member) {
            case "Start" -> IHost.Reply.of(start(parentOf(shell, callerId), remote, host, arguments, line), START);
            case "Shell" -> IHost.Reply.of(shell(remote, arguments), START);
            case "Send" -> {
                final int id = arguments.size() > 1 && arguments.get(1) instanceof Number number
                        ? number.intValue() : 0;
                final String text = arguments.size() > 2 ? String.valueOf(arguments.get(2)) : "";
                final long tick = remote.machine().getLevel() == null ? 0L
                        : remote.machine().getLevel().getGameTime();
                yield IHost.Reply.of(remote.machine() instanceof AbstractComputerBlockEntity machine
                        && machine.cannon().send(callerId, id, text, tick), TOUCH);
            }
            case "Processes" -> {
                final Values.ListValue all = new Values.ListValue();
                if (remote.machine() instanceof AbstractComputerBlockEntity machine) {
                    for (final MachinePrograms.Live one : machine.cannon().all()) {
                        all.items().add(handle(one.id(), one.file(), host));
                    }
                }
                yield IHost.Reply.of(all, READ + all.size());
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "a computer has no " + member);
        };
    }

    /** The host name the computer the program holds carries. */
    private static String hostOf(final List<Object> arguments) {
        if (!arguments.isEmpty() && arguments.getFirst() instanceof Values.Obj held
                && held.get("Host") instanceof String host) {
            return host;
        }
        return "";
    }

    /**
     * The asking program as the other machine will know it, so that what it starts there is kept for it
     * to read; null for a caller that is not a numbered program on a computer.
     */
    @Nullable
    private static MachinePrograms.RemoteParent parentOf(final ServerCliComputer caller, final int callerId) {
        return callerId > 0 && caller.machine() instanceof AbstractComputerBlockEntity machine
                ? new MachinePrograms.RemoteParent(machine.getBlockPos(), machine.nodeUuid(), callerId)
                : null;
    }

    /** Starts a compiled program from the other machine's disks, on that machine, as the prompt would. */
    private static Values.Obj start(@Nullable final MachinePrograms.RemoteParent parent,
                                    final ServerCliComputer remote, final String host,
                                    final List<Object> arguments, final int line) {
        if (!(remote.machine() instanceof AbstractComputerBlockEntity machine)) {
            throw new Halt(Halt.Reason.CANNOT_START, line, host + " cannot run programs");
        }
        final String path = arguments.size() > 1 ? String.valueOf(arguments.get(1)) : "";
        final List<String> args = new ArrayList<>();
        if (arguments.size() > 2 && arguments.get(2) instanceof Values.ListValue given) {
            for (final Object each : given.items()) {
                args.add(String.valueOf(each));
            }
        }
        final String priority = arguments.size() > 3 && arguments.get(3) != null
                ? String.valueOf(arguments.get(3)).toLowerCase(Locale.ROOT) : MachinePrograms.DEFAULT_PRIORITY;
        final int dot = path.lastIndexOf('.');
        final String extension = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (dev.jstech.core.JsCore.languages().runnerOf(extension) == null) {
            throw new Halt(Halt.Reason.CANNOT_START, line,
                    path + ": nothing installed runs a program of this kind (compile a source file first)");
        }
        final ICliComputer.FsResult read = remote.readFile(path);
        if (!read.ok()) {
            throw new Halt(Halt.Reason.CANNOT_START, line, host + ": " + read.message());
        }
        final int room = MachinePrograms.DEFAULT_HEAP_MB;
        if (!machine.ramLedger().fits(room)) {
            throw new Halt(Halt.Reason.CANNOT_START, line, host + ": " + room + " MB will not fit in "
                    + machine.ramLedger().freeMb() + " MB of free memory");
        }
        final int slash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        final String name = slash < 0 ? path : path.substring(slash + 1);
        final MachinePrograms.Started started = parent == null
                ? machine.cannon().start(name, read.message(), room, machine, args, 0, priority)
                : machine.cannon().startFor(parent, name, read.message(), room, machine, args, priority);
        if (!started.ok()) {
            throw new Halt(Halt.Reason.CANNOT_START, line, host + ": " + started.message());
        }
        machine.setChanged();
        return handle(started.id(), name, host);
    }

    /** Runs one line at the other machine's prompt and hands back what it printed. */
    private static Values.ListValue shell(final ServerCliComputer remote, final List<Object> arguments) {
        final String command = arguments.size() > 1 ? String.valueOf(arguments.get(1)) : "";
        final CliShell prompt = CliCommands.newShell(SHELL_WIDTH);
        final Values.ListValue lines = new Values.ListValue();
        for (final CliLine printed : prompt.run(command, remote).lines()) {
            lines.items().add(printed.text());
        }
        return lines;
    }

    /** A process handle that says which machine the process is on. */
    static Values.Obj handle(final int id, final String name, final String host) {
        final Values.Obj made = new Values.Obj("Process");
        made.set("Id", id);
        made.set("Name", name);
        made.set("Host", host);
        return made;
    }
}
