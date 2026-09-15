/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.MemberKind;
import dev.jstech.computers.vm.system.SystemApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The calls the program's own process answers, each bound to the call the system declares for it.
 *
 * <p>A program reaches them through the call it loaded with, never by their names while it runs. Every binding names a
 * call the system declares as the process's to answer, and that is checked when the bindings are made, so what the
 * process answers cannot drift from what the compiler lets a program write.
 */
final class ProcessCalls {

    /**
     * One call the process answers.
     *
     * @param id       the call it answers
     * @param function the Java that answers it
     * @param waiting  what says whether the call has to wait before it is answered, or null when it never does
     * @param onTarget whether the call is made on an object, which comes off the stack after the arguments
     */
    record Binding(MemberId id, IProcessFunction function, IProcessWait waiting, boolean onTarget) {
    }

    private static final String STRING = "string";

    /** A read has to wait while nothing has been typed at the terminal the program is in front of. */
    private static final IProcessWait LINE = (process, frame, count, line) -> {
        if (process.hasInput()) {
            return false;
        }
        process.park();
        return true;
    };

    private static final Map<MemberId, Binding> BINDINGS = build();

    private ProcessCalls() {
    }

    /** The binding for that call, or null when the process does not answer it this way. */
    static Binding find(final MemberId id) {
        return BINDINGS.get(id);
    }

    /** Every binding. */
    static Iterable<Binding> all() {
        return BINDINGS.values();
    }

    private static Map<MemberId, Binding> build() {
        final Map<MemberId, Binding> bindings = new HashMap<>();
        console(bindings);
        random(bindings);
        thread(bindings);
        return Map.copyOf(bindings);
    }

    /*
     * The console the program writes to and reads from. Reading waits: a program that asks for a line stops until one
     * is typed at the terminal it is in front of, and a line that is not the value asked for stops the program with the
     * text it could not read, as Convert would.
     */
    private static void console(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Console", "Print", null, (process, target, arguments, line) -> {
            process.library().write(String.valueOf(arguments[0]));
            return null;
        }, STRING);
        bind(bindings, "Console", "PrintLine", null, (process, target, arguments, line) -> {
            process.library().write(String.valueOf(arguments[0]));
            return null;
        }, STRING);
        bind(bindings, "Console", "Clear", null, (process, target, arguments, line) -> {
            process.library().clearConsole();
            return null;
        });
        bind(bindings, "Console", "ReadLine", LINE,
                (process, target, arguments, line) -> process.heap().adopt(process.takeInput(), line));
        bind(bindings, "Console", "HasLine", null, (process, target, arguments, line) -> process.hasInput());
        bind(bindings, "Console", "ReadInt", LINE,
                (process, target, arguments, line) -> NumberFunctions.number("ToInt", process.takeInput(), line));
        bind(bindings, "Console", "ReadLong", LINE,
                (process, target, arguments, line) -> NumberFunctions.number("ToLong", process.takeInput(), line));
        bind(bindings, "Console", "ReadDouble", LINE,
                (process, target, arguments, line) -> NumberFunctions.number("ToDouble", process.takeInput(), line));
        bind(bindings, "Console", "ReadBool", LINE,
                (process, target, arguments, line) -> NumberFunctions.truth(process.takeInput(), line));
    }

    /** The program's own random numbers, which a program may start again from a number of its choosing. */
    private static void random(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Random", "Next", null, (process, target, arguments, line) ->
                process.library().random().next(Numbers.toInt(arguments[0])), "int");
        bind(bindings, "Random", "NextDouble", null,
                (process, target, arguments, line) -> process.library().random().nextDouble());
        bind(bindings, "Random", "Seed", null, (process, target, arguments, line) -> {
            process.library().random().startFrom(Numbers.toLong(arguments[0]));
            return null;
        }, "long");
    }

    /*
     * More than one thing at once inside one program. Waiting for a thread to end takes nothing off the stack until it
     * is over or the time given has run out, the same as waiting for a line.
     */
    private static void thread(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Thread", "Start", null,
                (process, target, arguments, line) -> process.spawn(arguments[0], line), "Action");
        bind(bindings, "Thread", "Sleep", null, (process, target, arguments, line) -> {
            process.sleep(Numbers.toLong(arguments[0]));
            return null;
        }, "long");
        bind(bindings, "Thread", "Yield", null, (process, target, arguments, line) -> {
            process.yieldTurn();
            return null;
        });
        bind(bindings, "Thread", "Join", Process::joinWaits,
                (process, target, arguments, line) -> process.joinOver(target, line));
        bind(bindings, "Thread", "Join", Process::joinWaits,
                (process, target, arguments, line) -> process.joinOver(target, line), "long");
        bind(bindings, "Thread", "Stop", null, (process, target, arguments, line) -> {
            process.stop(target, line);
            return null;
        });
    }

    private static void bind(final Map<MemberId, Binding> bindings, final String owner, final String name,
                             final IProcessWait wait, final IProcessFunction function, final String... parameters) {
        final MemberId id = new MemberId(owner, name, List.of(parameters));
        final IMemberSpec declared = SystemApi.member(owner, name, id.parameters());
        if (declared == null || declared.kind() != MemberKind.PROCESS) {
            throw new IllegalStateException(id.describe() + " is not a call the system declares as the process's");
        }
        if (bindings.putIfAbsent(id, new Binding(id, function, wait, !declared.isStatic())) != null) {
            throw new IllegalStateException(id.describe() + " is bound twice");
        }
    }
}
