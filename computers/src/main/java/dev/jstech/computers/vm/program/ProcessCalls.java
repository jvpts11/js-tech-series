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
    private static final String INT = "int";
    private static final String LONG = "long";
    /** An address that says nothing of what lies there, as the calls of raw memory take and give it. */
    private static final String VOID_POINTER = "void*";
    /** The type the calls of raw memory are on, beside what the machine's memory answers. */
    private static final String MEMORY = "Memory";
    /** The kinds of variable a read of one value fills in, each a version of Console.Scan of its own. */
    private static final List<String> SCANNED = List.of("int", "long", "double", STRING, "char");
    /** What a watch on the network calls when it goes off. */
    private static final String STOCK_HANDLER = "Action<StockEvent>";
    private static final String SETTLED_HANDLER = "Action<OperationInfo>";
    /** What a kernel's declaration holds at most: its name, its four policies, its slice and its drivers. */
    private static final int KERNEL_FIELDS = 7;
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
        openFile(bindings);
        random(bindings);
        stopwatch(bindings);
        PatternCalls.bind(bindings);
        thread(bindings);
        program(bindings);
        network(bindings);
        gateway(bindings);
        ui(bindings);
        system(bindings);
        rawMemory(bindings);
        return Map.copyOf(bindings);
    }

    /*
     * The program's raw memory, which pointers reach (computers/docs/design/sigma.md). A pointer is the address it
     * holds, a long. Moving, setting and comparing bytes is charged by how many there are.
     */
    private static void rawMemory(final Map<MemberId, Binding> bindings) {
        bind(bindings, MEMORY, "Alloc", null, (process, target, arguments, line) ->
                process.memory().allocate(Numbers.toLong(arguments[0]), false, line), LONG);
        bind(bindings, MEMORY, "AllocZeroed", null, (process, target, arguments, line) ->
                process.memory().allocate(times(arguments[0], arguments[1]), false, line), LONG, LONG);
        bind(bindings, MEMORY, "Resize", null, (process, target, arguments, line) ->
                process.memory().resize(Numbers.toLong(arguments[0]), Numbers.toLong(arguments[1]), line),
                VOID_POINTER, LONG);
        bind(bindings, MEMORY, "Free", null, (process, target, arguments, line) -> {
            process.memory().free(Numbers.toLong(arguments[0]), line);
            return null;
        }, VOID_POINTER);
        bind(bindings, MEMORY, "Copy", null, (process, target, arguments, line) -> {
            chargeBytes(process, arguments[2]);
            process.memory().copy(Numbers.toLong(arguments[1]), Numbers.toLong(arguments[0]),
                    Numbers.toLong(arguments[2]), line);
            return null;
        }, VOID_POINTER, VOID_POINTER, LONG);
        bind(bindings, MEMORY, "Fill", null, (process, target, arguments, line) -> {
            chargeBytes(process, arguments[2]);
            process.memory().fill(Numbers.toLong(arguments[0]), Numbers.toLong(arguments[2]),
                    Numbers.toInt(arguments[1]), line);
            return null;
        }, VOID_POINTER, INT, LONG);
        bind(bindings, MEMORY, "Compare", null, (process, target, arguments, line) -> {
            chargeBytes(process, arguments[2]);
            return process.memory().compare(Numbers.toLong(arguments[0]), Numbers.toLong(arguments[1]),
                    Numbers.toLong(arguments[2]), line);
        }, VOID_POINTER, VOID_POINTER, LONG);
    }

    /* What a count of bytes moved, set or compared costs beyond its call: one for every 64 of them started. */
    private static void chargeBytes(final Process process, final Object bytes) {
        final long count = Numbers.toLong(bytes);
        if (count > 0) {
            final long per = SigmaCosts.RAW_BYTES_PER_INSTRUCTION;
            process.charge((int) Math.min(Integer.MAX_VALUE, (count + per - 1) / per));
        }
    }

    /*
     * The bytes calloc asks for, that many of that size: a count past what a long holds is asked as the most there is,
     * which no budget has room for, and a negative one as the negative it is, which is refused for it.
     */
    private static long times(final Object count, final Object size) {
        final long many = Numbers.toLong(count);
        final long each = Numbers.toLong(size);
        if (many < 0 || each < 0) {
            return Math.min(many, each);
        }
        try {
            return Math.multiplyExact(many, each);
        } catch (final ArithmeticException tooMany) {
            return Long.MAX_VALUE;
        }
    }

    /*
     * What a system a player writes declares its kernel with (computers/docs/design/player-systems.md): one of the
     * mod's by its name, or its own, each policy chosen on a copy of the one before, so a declaration is a value that
     * never changes. And Processes.Yield, which gives the rest of the turn away as Thread.Yield does.
     */
    private static void system(final Map<MemberId, Binding> bindings) {
        bind(bindings, "Kernel", "Of", null, (process, target, arguments, line) ->
                kernel(process, null, "Name", String.valueOf(arguments[0]), line), STRING);
        bind(bindings, "Kernel", "Own", null, (process, target, arguments, line) ->
                kernel(process, null, "Name", "own", line));
        for (final String policy : List.of("Scheduler", "FileSystem", "Drivers", "MemoryRule", "Shell")) {
            bind(bindings, "Kernel", policy, null, (process, target, arguments, line) ->
                    kernel(process, target, policy, String.valueOf(arguments[0]), line), STRING);
        }
        bind(bindings, "Kernel", "Scheduler", null, (process, target, arguments, line) -> {
            final Values.Obj chosen = kernel(process, target, "Scheduler", String.valueOf(arguments[0]), line);
            chosen.set("Slice", Numbers.toInt(arguments[1]));
            return chosen;
        }, STRING, "int");
        bind(bindings, "Processes", "Yield", null, (process, target, arguments, line) -> {
            process.yieldTurn();
            return null;
        });
    }

    /* A kernel's declaration with one more thing said of it: a copy of {@code before}, or a new one. */
    private static Values.Obj kernel(final Process process, final Object before, final String field,
                                     final String value, final int line) {
        final Values.Obj made = new Values.Obj("Kernel");
        if (before instanceof Values.Obj kept) {
            for (final Map.Entry<String, Object> held : kept.fields().entrySet()) {
                made.set(held.getKey(), held.getValue());
            }
        }
        made.set(field, process.heap().text(value, line));
        return process.heap0().allocate(made, Heap.HEADER + KERNEL_FIELDS * Heap.REFERENCE, line);
    }

    /*
     * The console the program writes to and reads from. Reading waits: a program that asks for a line stops until one
     * is typed at the terminal it is in front of, and a line that is not the value asked for stops the program with the
     * text it could not read, as Convert would.
     */
    private static void console(final Map<MemberId, Binding> bindings) {
        // Print leaves the line open for what follows it; PrintLine ends it.
        bind(bindings, "Console", "Print", null, (process, target, arguments, line) -> {
            process.console0().print(String.valueOf(arguments[0]));
            return null;
        }, STRING);
        bind(bindings, "Console", "Print", null, (process, target, arguments, line) -> {
            process.console0().print(String.valueOf((char) Numbers.toInt(arguments[0])));
            return null;
        }, "char");
        bind(bindings, "Console", "PrintLine", null, (process, target, arguments, line) -> {
            process.console0().print(arguments[0] + "\n");
            return null;
        }, STRING);
        bind(bindings, "Console", "Clear", null, (process, target, arguments, line) -> {
            process.console0().clear();
            return null;
        });
        bind(bindings, "Console", "ReadLine", LINE,
                (process, target, arguments, line) -> process.heap().adopt(process.takeInput(), line));
        bind(bindings, "Console", "HasLine", null, (process, target, arguments, line) -> process.hasInput());
        /*
         * Less than a line: one character, the line break at its end included, and one value, of the kind of the
         * variable it goes into. What is left of the line waits for the next read.
         */
        bind(bindings, "Console", "Read", LINE, (process, target, arguments, line) -> (int) process.takeChar());
        for (final String kind : SCANNED) {
            bind(bindings, "Console", "Scan", LINE, (process, target, arguments, line) -> {
                final Object value = process.scan(kind, line);
                arguments[0] = value == null && STRING.equals(kind) ? process.text("", line) : value;
                return value == null ? 0 : 1;
            }, "out " + kind);
        }
        bind(bindings, "Console", "ReadInt", LINE,
                (process, target, arguments, line) -> NumberFunctions.number("ToInt", process.takeInput(), line));
        bind(bindings, "Console", "ReadLong", LINE,
                (process, target, arguments, line) -> NumberFunctions.number("ToLong", process.takeInput(), line));
        bind(bindings, "Console", "ReadDouble", LINE,
                (process, target, arguments, line) -> NumberFunctions.number("ToDouble", process.takeInput(), line));
        bind(bindings, "Console", "ReadBool", LINE,
                (process, target, arguments, line) -> NumberFunctions.truth(process.takeInput(), line));
    }

    /*
     * A file the program opened, which it works on as one of its own objects: reading, writing and moving about in it
     * is the program's own, and only closing it reaches the machine.
     */
    private static void openFile(final Map<MemberId, Binding> bindings) {
        final String file = OpenFile.TYPE;
        bind(bindings, file, "ReadLine", null, (process, target, arguments, line) ->
                OpenFile.readLine(process, (Values.Obj) target, arguments, line), "out " + STRING);
        bind(bindings, file, "Read", null, (process, target, arguments, line) ->
                OpenFile.read((Values.Obj) target, line));
        for (final String kind : SCANNED) {
            bind(bindings, file, "Scan", null, (process, target, arguments, line) -> {
                final Object value = OpenFile.scan(process, (Values.Obj) target, kind, line);
                arguments[0] = value == null && STRING.equals(kind) ? process.text("", line) : value;
                return value == null ? 0 : 1;
            }, "out " + kind);
        }
        bind(bindings, file, "Write", null, (process, target, arguments, line) -> {
            OpenFile.write(process, (Values.Obj) target, String.valueOf(arguments[0]), line);
            return null;
        }, STRING);
        bind(bindings, file, "Write", null, (process, target, arguments, line) -> {
            OpenFile.write(process, (Values.Obj) target, String.valueOf((char) Numbers.toInt(arguments[0])), line);
            return null;
        }, "char");
        bind(bindings, file, "Seek", null, (process, target, arguments, line) -> {
            OpenFile.seek((Values.Obj) target, Numbers.toInt(arguments[0]), line);
            return null;
        }, "int");
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
     * A stopwatch is where the program's clock and its spending stood when it was started or restarted; what it reads
     * is how far each has gone since (ProcessValues).
     */
    private static void stopwatch(final Map<MemberId, Binding> bindings) {
        bind(bindings, Stopwatches.TYPE, "Start", null,
                (process, target, arguments, line) -> Stopwatches.start(process, line));
        bind(bindings, Stopwatches.TYPE, "Restart", null, (process, target, arguments, line) ->
                Stopwatches.restart(process, (Values.Obj) target));
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
            process.setName((String) process.heap().alive(arguments[0], line), line);
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
        // A channel is heard by the program, which tells its machine so; what is sent on it reaches the machine.
        bind(bindings, "Channel", "Listen", null, (process, target, arguments, line) -> {
            process.hearChannel(ChannelNames.checked(String.valueOf(arguments[0]), line),
                    arguments[1] instanceof Values.DelegateValue handler ? handler : null);
            return null;
        }, STRING, "Action<ChannelMessage>");
        bind(bindings, "Channel", "Close", null, (process, target, arguments, line) -> {
            process.hearChannel(ChannelNames.checked(String.valueOf(arguments[0]), line), null);
            return null;
        }, STRING);
        // A file is watched as an item of the network is, by the tick it last changed in, read by the machine.
        bind(bindings, "File", "Watch", null, (process, target, arguments, line) ->
                process.watch(Process.WATCHED_FILE + arguments[0], Process.Watching.CHANGE, 0L,
                        arguments[1] instanceof Values.DelegateValue handler ? handler : null, line),
                STRING, "Action<FileEvent>");
        bind(bindings, "Network", "OnEvent", null, (process, target, arguments, line) -> {
            process.hearNetworkEvents(arguments[0] instanceof Values.DelegateValue handler ? handler : null);
            return null;
        }, "Action<NetworkEvent>");
        // Any Operation of the network settling, or the one with the short id an ask answered with, told once.
        bind(bindings, "Operations", "OnSettled", null, (process, target, arguments, line) -> {
            process.hearSettled(null, arguments[0] instanceof Values.DelegateValue handler ? handler : null);
            return null;
        }, SETTLED_HANDLER);
        bind(bindings, "Operations", "OnSettled", null, (process, target, arguments, line) -> {
            process.hearSettled(String.valueOf(arguments[0]),
                    arguments[1] instanceof Values.DelegateValue handler ? handler : null);
            return null;
        }, STRING, SETTLED_HANDLER);
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
        // What the second version brought: runs of choices, entries and lines, and the widgets that hold others.
        for (final String entries : List.of("RadioGroup", "ComboBox", "ContextMenu", "StatusBar", "LogView",
                "ItemPicker")) {
            drawn(bindings, entries, "Add", STRING);
            drawn(bindings, entries, "Clear");
        }
        drawn(bindings, "TabView", "Add", STRING, "Widget");
        drawn(bindings, "TabView", "Clear");
        drawn(bindings, "Table", "AddColumn", STRING);
        drawn(bindings, "Table", "AddRow", "List<string>");
        drawn(bindings, "Table", "AddRow", STRING);
        drawn(bindings, "Table", "AddRow", STRING, STRING);
        drawn(bindings, "Table", "AddRow", STRING, STRING, STRING);
        drawn(bindings, "Table", "AddRow", STRING, STRING, STRING, STRING);
        drawn(bindings, "Table", "Clear");
        read(bindings, "Table", "Cell", "int", "int");
        drawn(bindings, "TreeView", "Add", STRING);
        drawn(bindings, "TreeView", "Add", STRING, "int");
        drawn(bindings, "TreeView", "Clear");
        read(bindings, "TreeView", "NodeText", "int");
        drawn(bindings, "MenuBar", "Add", STRING, STRING);
        drawn(bindings, "MenuBar", "Clear");
        drawn(bindings, "Chart", "Add", "double");
        drawn(bindings, "Chart", "Clear");
        drawn(bindings, "OperationView", "Show", "OperationInfo");
        // A dialog goes up over a window as a window does, so it costs what opening one does.
        for (final String dialog : List.of("OpenFileDialog", "SaveFileDialog")) {
            bind(bindings, dialog, "Show", null, (process, target, arguments, line) -> {
                final Values.Obj widget = widget(target, dialog, "Show", line);
                process.charge(SigmaCosts.WRITE);
                return process.windows0().mutator().call(widget, "Show", Arrays.asList(arguments), line);
            }, "Window");
        }
        read(bindings, "GenericComponent", "OnAction", "Action<ComponentAction>");
    }

    /** Binds a call on a widget that only reads what it holds, or only says who is to hear it: it costs nothing. */
    private static void read(final Map<MemberId, Binding> bindings, final String owner, final String name,
                             final String... parameters) {
        bind(bindings, owner, name, null, (process, target, arguments, line) -> process.windows0().mutator()
                .call(widget(target, owner, name, line), name, Arrays.asList(arguments), line), parameters);
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

    /** Binds a call the system declares as the process's, once sure it does; the other kits of calls bind here. */
    static void bind(final Map<MemberId, Binding> bindings, final String owner, final String name,
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
