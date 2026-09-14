/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramEntry;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.SigmaCosts;
import dev.jstech.core.language.ILanguageProcess;
import java.util.ArrayList;
import java.util.List;

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

    private static final int START = SigmaCosts.SUBMIT;
    private static final int LOOK = SigmaCosts.GLANCE;
    private static final int TOUCH = SigmaCosts.GLANCE_NETWORK;
    private static final int READ = SigmaCosts.READ;

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
                final ProgramEntry<IMachineRuntime> one = where == null ? null : where.programs().byId(id(arguments));
                if (one != null) {
                    lines.items().addAll(one.process().console());
                }
                yield IHost.Reply.of(lines, READ + lines.size());
            }
            case "Kill" -> {
                final boolean stopped = where != null && where.programs().stop(id(arguments));
                if (stopped) {
                    where.setChanged();
                }
                yield IHost.Reply.of(stopped, TOUCH);
            }
            case "Send" -> {
                final String text = arguments.size() > 1 ? String.valueOf(arguments.get(1)) : "";
                final long tick = machine.getLevel() == null ? 0L : machine.getLevel().getGameTime();
                yield IHost.Reply.of(machine.programs().send(callerId, id(arguments), text, tick), TOUCH);
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
        final ProgramPriority priority = ProgramPriority.named(arguments.size() > 2 && arguments.get(2) != null
                ? String.valueOf(arguments.get(2)) : null);
        final IProgramParent starter = parent > 0 ? new IProgramParent.Local(parent) : IProgramParent.NONE;
        final ProgramLauncher.Launch launch =
                ProgramLauncher.launch(machine, path, shell::readFile, args, starter, priority, 0);
        if (!launch.ok()) {
            final String why = switch (launch.refusal()) {
                case NO_RUNNER -> path + ": nothing installed runs a program of this kind"
                        + " (compile a source file first)";
                case NO_MEMORY -> path + ": " + launch.roomMb() + " MB will not fit in " + launch.freeMb()
                        + " MB of free memory";
                case UNREADABLE, NOT_STARTED -> launch.message();
            };
            throw new Halt(Halt.Reason.CANNOT_START, line, why);
        }
        // The handle carries the file the program came from; what the program calls itself is its own.
        return HostRemote.handle(launch.id(), launch.name(), "");
    }

    private static boolean running(final AbstractComputerBlockEntity machine, final int id) {
        final ProgramEntry<IMachineRuntime> one = machine.programs().byId(id);
        if (one == null) {
            return false;
        }
        final ILanguageProcess.State state = one.process().state();
        return state == ILanguageProcess.State.RUNNING || state == ILanguageProcess.State.PARKED
                || (state == ILanguageProcess.State.FINISHED && one.process().isService());
    }

    private static int exitCode(final AbstractComputerBlockEntity machine, final int id) {
        final ProgramEntry<IMachineRuntime> one = machine.programs().byId(id);
        return one == null ? 0 : one.process().exitCode();
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
