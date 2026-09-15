/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.IProgramParent;
import dev.jstech.computers.vm.program.ProgramPriority;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The other programs on the machine, as a running program reaches them through its computer's {@link ProgramService}.
 *
 * <p>A program can start another from a compiled file on the same disks, ask how it is getting on, read what it
 * printed, stop it, and send it a line. The one that starts another is its parent: the child keeps its exit code and
 * its output for the parent to read, and runs on if the parent goes first. A handle names the machine its program is
 * on, so a program another computer runs is asked after the same way.
 */
final class ProgramCalls {

    private static final String STRING = "string";
    private static final String STRINGS = "List<string>";

    private ProgramCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        final MachineCalls.IServiceFunction<ProgramService> start = (programs, call, target, arguments, line) -> {
            final String path = text(arguments, 0);
            final IProgramParent parent =
                    call.callerId() > 0 ? new IProgramParent.Local(call.callerId()) : IProgramParent.NONE;
            final ProgramLauncher.Launch launch =
                    programs.start(path, strings(arguments, 1), parent, priority(arguments));
            if (!launch.ok()) {
                throw new Halt(Halt.Reason.CANNOT_START, line, refusal(path, "", launch));
            }
            // The handle carries the file the program came from; what the program calls itself is its own.
            return handle(launch.id(), launch.name(), "");
        };
        program(bindings, "Start", start, STRING);
        program(bindings, "Start", start, STRING, STRINGS);
        program(bindings, "Start", start, STRING, STRINGS, STRING);
        program(bindings, "Shell", (programs, call, target, arguments, line) -> {
            final Values.ListValue lines = new Values.ListValue();
            lines.items().addAll(programs.shell(text(arguments, 0)));
            return lines;
        }, STRING);
        process(bindings, "Running",
                (programs, call, target, arguments, line) -> programs.running(idOf(target, line), hostOf(target)));
        process(bindings, "ExitCode",
                (programs, call, target, arguments, line) -> programs.exitCode(idOf(target, line), hostOf(target)));
        process(bindings, "Kill",
                (programs, call, target, arguments, line) -> programs.kill(idOf(target, line), hostOf(target)));
        process(bindings, "Output", (programs, call, target, arguments, line) -> {
            final Values.ListValue lines = new Values.ListValue();
            lines.items().addAll(programs.output(idOf(target, line), hostOf(target)));
            return lines;
        });
        process(bindings, "Send", (programs, call, target, arguments, line) -> programs.send(call.callerId(),
                number(arguments, 0), text(arguments, 1)), "int", STRING);
    }

    private static void program(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                final MachineCalls.IServiceFunction<ProgramService> function,
                                final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::programs, "Program", name, function, parameters);
    }

    private static void process(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                final MachineCalls.IServiceFunction<ProgramService> function,
                                final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::programs, "Process", name, function, parameters);
    }

    /** A process handle that says which machine its program is on: {@code ""} for this one. */
    static Values.Obj handle(final int id, final String name, final String host) {
        final Values.Obj made = new Values.Obj("Process");
        made.set("Id", id);
        made.set("Name", name);
        made.set("Host", host);
        return made;
    }

    /** Why a program could not be started, from this machine or, when a host is named, on that one. */
    static String refusal(final String path, final String host, final ProgramLauncher.Launch launch) {
        final String where = host.isEmpty() ? path : host;
        return switch (launch.refusal()) {
            case NO_RUNNER -> path + ": nothing installed runs a program of this kind (compile a source file first)";
            case NO_MEMORY -> where + ": " + launch.roomMb() + " MB will not fit in " + launch.freeMb()
                    + " MB of free memory";
            case UNREADABLE, NOT_STARTED -> host.isEmpty() ? launch.message() : host + ": " + launch.message();
        };
    }

    /** The priority a start asked for, as its third argument names it, or the default one when it names none. */
    static ProgramPriority priority(final Object[] arguments) {
        final String named = arguments.length > 2 && arguments[2] != null ? String.valueOf(arguments[2]) : null;
        return ProgramPriority.named(named);
    }

    /** The texts of a list argument, or none when the call was not handed one there. */
    static List<String> strings(final Object[] arguments, final int index) {
        final List<String> given = new ArrayList<>();
        if (arguments.length > index && arguments[index] instanceof Values.ListValue list) {
            for (final Object each : list.items()) {
                given.add(String.valueOf(each));
            }
        }
        return given;
    }

    static String text(final Object[] arguments, final int index) {
        return arguments.length > index ? String.valueOf(arguments[index]) : "";
    }

    static int number(final Object[] arguments, final int index) {
        return arguments.length > index && arguments[index] instanceof Number number ? number.intValue() : 0;
    }

    /** The number of the program a handle points at, or a halt when what the call is made on is no handle. */
    private static int idOf(final Object target, final int line) {
        if (target instanceof Values.Obj handle && handle.get("Id") instanceof Integer id) {
            return id;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no process here");
    }

    /** The machine a handle's program is on: what its {@code Host} says, or this one when it says nothing. */
    private static String hostOf(final Object target) {
        return target instanceof Values.Obj handle && handle.get("Host") instanceof String host ? host : "";
    }
}
