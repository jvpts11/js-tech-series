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
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.language.ILanguageProcess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The other programs on the machine, as a running program reaches them.
 *
 * <p>A program can start another from a compiled file on the same disks, ask how it is getting on,
 * read what it printed, stop it, and send it a line. The one that starts another is its parent: the
 * child keeps its exit code and its output for the parent to read until the parent is gone, and runs on
 * if the parent goes first. The machine's list of programs is the only place any of this lives, so what
 * a program starts shows in the Task Manager like anything started at the prompt.
 */
public final class HostProgram {

    private static final int START = CannonCosts.SUBMIT;
    private static final int LOOK = CannonCosts.GLANCE;
    private static final int TOUCH = CannonCosts.GLANCE_NETWORK;
    private static final int READ = CannonCosts.READ;

    /** What a program is started with when it does not say: the middle, like anything at the prompt. */
    private static final String DEFAULT_PRIORITY = "medium";

    private HostProgram() {
    }

    /** Whether this is one of the calls handled here. */
    public static boolean handles(final String owner) {
        return "Program".equals(owner);
    }

    /**
     * Answers one of them against a real machine.
     *
     * @param callerId the machine's number for the asking program, which is the parent of what it starts
     *                 and the sender of what it sends
     */
    public static IHost.Reply call(final AbstractComputerBlockEntity machine, final ICliComputer shell,
                                  final int callerId, final String member, final List<Object> arguments,
                                  final int line) {
        /*
         * A handle may point at a program on another machine of the network; the machine asked is then
         * that one, found through this machine's shell. Gone or unreachable reads as not running.
         */
        final AbstractComputerBlockEntity where = machineFor(machine, shell, arguments);
        return switch (member) {
            case "Start" -> IHost.Reply.of(start(machine, shell, callerId, arguments, line), START);
            case "Running" -> IHost.Reply.of(where != null && running(where, id(arguments)), LOOK);
            case "ExitCode" -> IHost.Reply.of(where == null ? 0 : exitCode(where, id(arguments)), LOOK);
            case "Output" -> {
                final Values.ListValue lines = new Values.ListValue();
                final MachinePrograms.Live one = where == null ? null : where.cannon().byId(id(arguments));
                if (one != null) {
                    lines.items().addAll(one.process().console());
                }
                yield IHost.Reply.of(lines, READ + lines.size());
            }
            case "Kill" -> {
                final boolean stopped = where != null && where.cannon().stop(id(arguments));
                if (stopped) {
                    where.setChanged();
                }
                yield IHost.Reply.of(stopped, TOUCH);
            }
            case "Send" -> {
                final String text = arguments.size() > 1 ? String.valueOf(arguments.get(1)) : "";
                final long tick = machine.getLevel() == null ? 0L : machine.getLevel().getGameTime();
                yield IHost.Reply.of(machine.cannon().send(callerId, id(arguments), text, tick), TOUCH);
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "Program has no " + member);
        };
    }

    /**
     * Starts a compiled program from the machine's disks, as the prompt would, without handing it the
     * terminal: what a program starts prints to a console of its own that the parent can read.
     */
    private static Values.Obj start(final AbstractComputerBlockEntity machine, final ICliComputer shell,
                                    final int parent, final List<Object> arguments, final int line) {
        final String path = arguments.isEmpty() ? "" : String.valueOf(arguments.getFirst());
        final List<String> args = new ArrayList<>();
        if (arguments.size() > 1 && arguments.get(1) instanceof Values.ListValue given) {
            for (final Object each : given.items()) {
                args.add(String.valueOf(each));
            }
        }
        final String priority = arguments.size() > 2 && arguments.get(2) != null
                ? String.valueOf(arguments.get(2)).toLowerCase(Locale.ROOT) : DEFAULT_PRIORITY;
        final int dot = path.lastIndexOf('.');
        final String extension = dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (dev.jstech.core.JsCore.languages().runnerOf(extension) == null) {
            throw new Halt(Halt.Reason.CANNOT_START, line,
                    path + ": nothing installed runs a program of this kind (compile a source file first)");
        }
        final ICliComputer.FsResult read = shell.readFile(path);
        if (!read.ok()) {
            throw new Halt(Halt.Reason.CANNOT_START, line, read.message());
        }
        final int room = MachinePrograms.DEFAULT_HEAP_MB;
        if (!machine.ramLedger().fits(room)) {
            throw new Halt(Halt.Reason.CANNOT_START, line, path + ": " + room + " MB will not fit in "
                    + machine.ramLedger().freeMb() + " MB of free memory");
        }
        final int slash = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
        final String name = slash < 0 ? path : path.substring(slash + 1);
        final MachinePrograms.Started started =
                machine.cannon().start(name, read.message(), room, machine, args, parent, priority);
        if (!started.ok()) {
            throw new Halt(Halt.Reason.CANNOT_START, line, started.message());
        }
        machine.setChanged();
        // The handle carries the file the program came from; what the program calls itself is its own.
        return HostRemote.handle(started.id(), name, "");
    }

    private static boolean running(final AbstractComputerBlockEntity machine, final int id) {
        final MachinePrograms.Live one = machine.cannon().byId(id);
        if (one == null) {
            return false;
        }
        final ILanguageProcess.State state = one.process().state();
        return state == ILanguageProcess.State.RUNNING || state == ILanguageProcess.State.PARKED
                || (state == ILanguageProcess.State.FINISHED && one.process().isService());
    }

    private static int exitCode(final AbstractComputerBlockEntity machine, final int id) {
        final MachinePrograms.Live one = machine.cannon().byId(id);
        if (one == null) {
            return 0;
        }
        if (one.process() instanceof CannonProgram cannon) {
            return cannon.process().exitCode();
        }
        return one.process().state() == ILanguageProcess.State.HALTED ? 1 : 0;
    }

    private static int id(final List<Object> arguments) {
        return arguments.isEmpty() || !(arguments.getFirst() instanceof Number number) ? 0 : number.intValue();
    }

    /**
     * The machine a handle's program is on: this one, or the one the handle names after the number.
     * Null when that machine is not on the network any more, or cannot run programs at all.
     */
    @org.jetbrains.annotations.Nullable
    private static AbstractComputerBlockEntity machineFor(final AbstractComputerBlockEntity machine,
                                                          final ICliComputer shell, final List<Object> arguments) {
        final String host = arguments.size() > 1 && arguments.get(1) instanceof String named ? named : "";
        if (host.isEmpty()) {
            return machine;
        }
        if (!(shell instanceof dev.jstech.computers.program.ServerCliComputer own)) {
            return null;
        }
        final dev.jstech.computers.program.ServerCliComputer remote = own.remoteShell(host);
        return remote != null && remote.machine() instanceof AbstractComputerBlockEntity there ? there : null;
    }
}
