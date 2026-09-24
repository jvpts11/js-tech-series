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
import dev.jstech.computers.vm.system.SigmaCosts;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Arrays;
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
@TextHolder
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
    /** What a watch on the network calls when it goes off. */
    private static final String STOCK_HANDLER = "Action<StockEvent>";
    private static final TextKey NO_WIDGET = TextKey.of("jsc.vm.process_calls.no_widget", "there is no %s here to %s");

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
        program(bindings);
        network(bindings);
        gateway(bindings);
        ui(bindings);
        return Map.copyOf(bindings);
    }

    /*
     * The console the program writes to and reads from. Reading waits: a program that asks for a line stops until one
     * is typed at the terminal it is in front of, and a line that is not the value asked for stops the program with the
     * text it could not read, as Convert would.
     */
    private static void console(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Console", "Print", null, (process, target, arguments, line) -> {
            process.console0().writeLines(String.valueOf(arguments[0]));
            return null;
        }, STRING);
        bind(bindings, "Console", "PrintLine", null, (process, target, arguments, line) -> {
            process.console0().writeLines(String.valueOf(arguments[0]));
            return null;
        }, STRING);
        bind(bindings, "Console", "Clear", null, (process, target, arguments, line) -> {
            process.console0().clear();
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
                process.random().next(Numbers.toInt(arguments[0])), "int");
        bind(bindings, "Random", "NextDouble", null,
                (process, target, arguments, line) -> process.random().nextDouble());
        bind(bindings, "Random", "Seed", null, (process, target, arguments, line) -> {
            process.random().startFrom(Numbers.toLong(arguments[0]));
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

    /*
     * What a program says about itself (its name, its end, who hears the lines sent to it) and waiting for another
     * program to end, which takes nothing off the stack until it is over, the way a join does. Everything else a
     * program asks about other programs is the machine's to answer.
     */
    private static void program(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Program", "SetName", null, (process, target, arguments, line) -> {
            process.setName(String.valueOf(arguments[0]), line);
            return null;
        }, STRING);
        bind(bindings, "Program", "Exit", null, (process, target, arguments, line) -> {
            process.exit(Numbers.toInt(arguments[0]));
            return null;
        }, "int");
        bind(bindings, "Program", "OnMessage", null, (process, target, arguments, line) -> {
            process.hearMessages(arguments[0] instanceof Values.DelegateValue handler ? handler : null);
            return null;
        }, "Action<ProcessMessage>");
        bind(bindings, "Process", "Wait", Process::waitForWaits,
                (process, target, arguments, line) -> process.waitForOver(target, line));
        bind(bindings, "Process", "Wait", Process::waitForWaits,
                (process, target, arguments, line) -> process.waitForOver(target, line), "long");
    }

    /*
     * Asking to be told when what the network holds of something changes, or crosses a line. The watch is the
     * program's: it holds it, is woken by it and takes it across a reload, and the machine is only asked the totals,
     * once a tick, for everything being watched at all. Asking to be told costs nothing, and is meant to.
     */
    private static void network(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Network", "Watch", null, (process, target, arguments, line) ->
                watch(process, Process.Watching.CHANGE, arguments, line), STRING, STOCK_HANDLER);
        bind(bindings, "Network", "WatchBelow", null, (process, target, arguments, line) ->
                watch(process, Process.Watching.BELOW, arguments, line), STRING, "long", STOCK_HANDLER);
        bind(bindings, "Network", "WatchAbove", null, (process, target, arguments, line) ->
                watch(process, Process.Watching.ABOVE, arguments, line), STRING, "long", STOCK_HANDLER);
    }

    /** Sets a watch on an item, the handler being the last thing the call is handed. */
    private static Object watch(final Process process, final Process.Watching kind, final Object[] arguments,
                                final int line) {
        final String item = String.valueOf(arguments[0]);
        final long threshold = kind == Process.Watching.CHANGE ? 0L : Numbers.toLong(arguments[1]);
        final Object last = arguments[arguments.length - 1];
        return process.watch(item, kind, threshold, last instanceof Values.DelegateValue handler ? handler : null,
                line);
    }

    /*
     * Who hears what a ComputerCraft computer says through a Gateway. The listener is the program's, like who hears the
     * lines other programs send it, and comes back with it from a save; the Gateway's own calls are the machine's.
     */
    private static void gateway(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Gateway", "OnMessage", null, (process, target, arguments, line) -> {
            process.hearGateway(arguments[0] instanceof Values.DelegateValue handler ? handler : null);
            return null;
        }, "Action<GatewayMessage>");
    }

    /*
     * The windows a program opens and the widgets in them. Changing what a window shows costs a draw, since the machine
     * has to draw it again for whoever is looking; opening a window or a message box is dearer, since it goes on the
     * machine's desktop and outlives the tick that asked for it. The price is charged before anything changes.
     */
    private static void ui(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Window", "Show", null, (process, target, arguments, line) -> {
            final Values.Obj window = widget(target, "Window", "Show", line);
            process.charge(SigmaCosts.WRITE);
            open(process, window, line);
            return null;
        });
        bind(bindings, "Window", "Close", null, (process, target, arguments, line) -> {
            final Values.Obj window = widget(target, "Window", "Close", line);
            process.charge(SigmaCosts.DRAW);
            process.closeWindow(window);
            return null;
        });
        // A widget put exactly where the program says, for one that lays itself out.
        drawn(bindings, "Window", "Add", "Widget", "int", "int", "int", "int");
        for (final String box : List.of("Row", "Column")) {
            drawn(bindings, box, "Add", "Widget");
            drawn(bindings, box, "Add", "Widget", "int");
            drawn(bindings, box, "Clear");
        }
        drawn(bindings, "ListBox", "Add", STRING);
        drawn(bindings, "ListBox", "Add", STRING, STRING);
        drawn(bindings, "ListBox", "Clear");
        drawn(bindings, "Canvas", "Clear", "int");
        drawn(bindings, "Canvas", "FillRect", "int", "int", "int", "int", "int");
        drawn(bindings, "Canvas", "DrawLine", "int", "int", "int", "int", "int");
        drawn(bindings, "Canvas", "DrawText", STRING, "int", "int", "int");
        drawn(bindings, "Canvas", "SetPixel", "int", "int", "int");
        bind(bindings, "MessageBox", "Show", null, (process, target, arguments, line) -> {
            process.charge(SigmaCosts.WRITE);
            open(process, UiWidgets.message(String.valueOf(arguments[0]), String.valueOf(arguments[1]), line), line);
            return null;
        }, STRING, STRING);
    }

    /** Binds a call on a widget that changes what it shows, charged a draw and made through the windows' one door. */
    private static void drawn(final Map<MemberId, Binding> bindings, final String owner, final String name,
                              final String... parameters) {
        bind(bindings, owner, name, null, (process, target, arguments, line) -> {
            final Values.Obj widget = widget(target, owner, name, line);
            process.charge(SigmaCosts.DRAW);
            return process.windows0().mutator().call(widget, name, Arrays.asList(arguments), line);
        }, parameters);
    }

    /** The window or widget a call is made on, or a halt when what it is made on is none. */
    private static Values.Obj widget(final Object target, final String owner, final String name, final int line) {
        if (target instanceof Values.Obj object && UiWidgets.handles(object.type())) {
            return object;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, NO_WIDGET.with(owner, name));
    }

    /** Puts a window on the machine's desktop; one the runtime made itself is the program's to hold like any other. */
    private static void open(final Process process, final Values.Obj window, final int line) {
        process.heap().adopt(window, line);
        process.openWindow(window, line);
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
