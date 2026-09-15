/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.IOperand;
import java.util.ArrayList;
import java.util.List;

/**
 * The part of the library the runtime answers for itself that needs more than a call's arguments.
 *
 * <p>These are the calls that touch the machine: its Gateway, and handing everything else to the machine. It also
 * keeps the console the process writes to and its random numbers, whose calls {@link ProcessCalls} answer, as it
 * does the calls on the program's windows and its watches on the network. The calls that need nothing but their
 * arguments (text, the two collections, numbers) are {@link PureFunctions}.
 */
public final class Library {

    /** What a call gave back: its answer, and whatever it filled in on the way. */
    public record Answer(Object value, List<Object> filled) {

        public Answer {
            filled = filled == null ? List.of() : new ArrayList<>(filled);
        }

        /** An answer with nothing filled in. */
        static Answer of(final Object value) {
            return new Answer(value, List.of());
        }
    }

    private final Heap heap;
    private final IHost host;
    private final ProgramConsole console = new ProgramConsole();
    private final ProgramRandom random = new ProgramRandom();

    /** The class this process was started from, which is how the world knows which program asked. */
    private final String caller;

    public Library(final Heap heap, final IHost host) {
        this(heap, host, "");
    }

    public Library(final Heap heap, final IHost host, final String caller) {
        this.heap = heap;
        this.host = host;
        this.caller = caller == null ? "" : caller;
    }

    /** What the process has written, line by line, oldest of the ones it still keeps first. */
    public List<String> console() {
        return this.console.lines();
    }

    /** How many lines the process has written since it started, the ones already dropped included. */
    public long written() {
        return this.console.written();
    }

    /** Writes a line to the process's console, which keeps what it holds within its limits. */
    public void write(final String line) {
        this.console.write(line);
    }

    /** Empties the process's console, as a program clearing its screen does. */
    void clearConsole() {
        this.console.clear();
    }

    /** The process's own random numbers. */
    ProgramRandom random() {
        return this.random;
    }

    /** Where the process's random numbers have got to, for the save. */
    public long randomState() {
        return this.random.state();
    }

    /** Puts back what a process had written, and where its random numbers were, before it was put away. */
    public void restore(final List<String> lines, final long written, final long random) {
        this.console.restore(lines, written);
        this.random.startFrom(random);
    }

    /** What the last call cost beyond the one instruction every call costs, and clears it. */
    public int drawCost() {
        final int owed = this.owed;
        this.owed = 0;
        return owed;
    }

    private int owed;

    /** Charges the running instruction that much more, for a call the process answers itself. */
    void owe(final int more) {
        this.owed += Math.max(0, more);
    }

    /** The tick the host is on, for what the process times against the world. */
    long now() {
        return this.host.tick();
    }

    long hostTick() {
        return this.host.tick();
    }

    long hostDayTime() {
        return this.host.dayTime();
    }

    long hostDay() {
        return this.host.day();
    }

    /**
     * Whether a call of this needs the thing it is called on to be on the stack under its arguments: the calls on the
     * machine's own objects do. A pure function and a call the process answers say so for themselves.
     */
    public boolean takesTarget(final String owner, final String name) {
        return this.host.takesTarget(owner, name);
    }

    /** Makes one of the things the language brings with it: a collection, a window, a widget. */
    public Object create(final String type, final List<Object> arguments, final int line) {
        final String bare = type.contains("<") ? type.substring(0, type.indexOf('<')) : type;
        final IObjectMaker widget = WidgetObjects.find(bare);
        if (widget != null) {
            return widget.make(this.owner(), arguments, line);
        }
        // Anything else the runtime is asked to make by name is one of the core's collections: a list unless a map.
        return CoreObjects.find("Map".equals(bare) ? "Map" : "List").make(this.owner(), arguments, line);
    }

    /** Reads one of the values the runtime keeps on a type of its own rather than on an object. */
    public Object readStatic(final String owner, final String name, final int line) {
        if ("Time".equals(owner)) {
            return switch (name) {
                case "Tick" -> this.host.tick();
                case "DayTime" -> this.host.dayTime();
                case "Day" -> this.host.day();
                default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "Time has no " + name);
            };
        }
        if (this.host.provides(owner)) {
            /*
             * To the machine, being asked for a value and being asked to do something are the same
             * question with different names, so a property goes out as a call that takes nothing. A
             * Gateway is the one thing that takes something even so: which Gateway the program chose.
             */
            final List<Object> asked = UiWidgets.WINDOW.equals(owner) || !"Gateway".equals(owner) ? List.of()
                    : List.of(this.owner == null ? "" : this.owner.gatewayName());
            final IHost.Reply reply = this.host.call(owner, name, asked, this.caller, this.callerId(), line);
            this.owed += Math.max(0, reply.cost());
            return this.heap.adopt(reply.value(), line);
        }
        throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, owner + " has no " + name);
    }

    /** Runs one of the calls the runtime answers for. */
    public Answer call(final IOperand.Method named, final Object self, final List<Object> arguments,
                       final int line) {
        return switch (named.owner()) {
            case "Program" -> this.program(named, arguments, line);
            case "Gateway" -> this.gateway(named, arguments, line);
            default -> this.outwardOn(named, self, arguments, line);
        };
    }

    /**
     * A call on one of the machine's Gateways.
     *
     * <p>Which Gateway is the program's own choice and stays with the program, so every call carries it
     * to the machine in front of whatever else it takes. Listening for what the other side says is the
     * program's too, and is kept here rather than asked of the machine.
     */
    private Answer gateway(final IOperand.Method named, final List<Object> arguments, final int line) {
        if ("OnMessage".equals(named.name())) {
            if (this.owner != null) {
                this.owner.hearGateway(arguments.isEmpty()
                        || !(arguments.getFirst() instanceof Values.DelegateValue handler) ? null : handler);
            }
            return Answer.of(null);
        }
        final boolean choosing = "Select".equals(named.name());
        final String chosen = choosing && !arguments.isEmpty() ? String.valueOf(arguments.getFirst())
                : (this.owner == null ? "" : this.owner.gatewayName());
        final List<Object> passed = new ArrayList<>();
        passed.add(chosen);
        passed.addAll(arguments);
        final Answer answered = this.outward(named, passed, line);
        if (choosing && this.owner != null && Boolean.TRUE.equals(answered.value())) {
            this.owner.chooseGateway(chosen);
        }
        return answered;
    }

    private Process owner() {
        if (this.owner == null) {
            throw new Halt(Halt.Reason.CANNOT_START, 0, "this program has no machine to open a window on");
        }
        return this.owner;
    }

    /** The process this library serves, for the few calls that are about the program rather than the world. */
    private Process owner;

    void serves(final Process process) {
        this.owner = process;
    }

    /** Hands a call to the machine; a call on one of the machine's own objects hands the object over first. */
    private Answer outwardOn(final IOperand.Method named, final Object self, final List<Object> arguments,
                             final int line) {
        if (self == null) {
            return this.outward(named, arguments, line);
        }
        final List<Object> withSelf = new ArrayList<>();
        withSelf.add(self);
        withSelf.addAll(arguments);
        return this.outward(named, withSelf, line);
    }

    /**
     * Hands a call to the machine, and takes what comes back onto the program's heap.
     *
     * <p>The machine knows nothing of heaps or budgets: it answers with plain values and says what the
     * answer was worth. Everything else about it is settled here, in the one place that already knows how
     * much a thing costs to hold.
     */
    private Answer outward(final IOperand.Method named, final List<Object> arguments, final int line) {
        if (!this.host.provides(named.owner())) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line,
                    "the runtime does not answer for " + named.owner());
        }
        final IHost.Reply reply =
                this.host.call(named.owner(), named.name(), arguments, this.caller, this.callerId(), line);
        this.owed += Math.max(0, reply.cost());
        final List<Object> filled = new ArrayList<>();
        for (final Object one : reply.filled()) {
            filled.add(this.heap.adopt(one, line));
        }
        return new Answer(this.heap.adopt(reply.value(), line), filled);
    }

    /** Hands a failure of the runtime itself to the machine, which is where it gets written down. */
    void fault(final String process, final int line, final RuntimeException cause) {
        this.host.fault(process, line, cause);
    }

    /**
     * What a program asks of the machine about the other programs on it: starting one, or running a line at the
     * machine's own prompt.
     *
     * <p>That is the machine's business, and a machine that has no other programs to speak of (there is none around the
     * tests) says so. What a program says about itself is the process's, and is bound with its other calls.
     */
    private Answer program(final IOperand.Method named, final List<Object> arguments, final int line) {
        if (this.host.provides(named.owner())) {
            return this.outward(named, arguments, line);
        }
        throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "this computer cannot reach other programs");
    }

    /** The number the machine lists the served process under, or 0 off any machine. */
    private int callerId() {
        return this.owner == null ? 0 : this.owner.machineId();
    }

    /**
     * Asks the machine something without charging for it, for what the process checks on its own
     * account between slices; nothing when the machine does not answer for it.
     */
    Object peek(final String owner, final String member, final List<Object> arguments) {
        if (!this.host.provides(owner)) {
            return null;
        }
        try {
            return this.host.call(owner, member, arguments, this.caller, this.callerId(), 0).value();
        } catch (final Halt refused) {
            return null;
        }
    }

    /**
     * Whether the program listed under that number, on this machine or on the named one, is still
     * going.
     */
    boolean programRunning(final Object id, final Object host) {
        return Boolean.TRUE.equals(this.peek("Program", "Running", whereabouts(id, host)));
    }

    /**
     * Reads one of the things only the machine knows about another program: whether it still runs
     * and how it ended. Off any machine the only program there is is this one.
     */
    Object programField(final Object id, final Object host, final String name, final int line) {
        if (this.host.provides("Program")) {
            return this.outward(new IOperand.Method("Program", name, List.of("int", "string"),
                    "Running".equals(name) ? "bool" : "int"), whereabouts(id, host), line).value();
        }
        if ("Running".equals(name)) {
            return this.owner != null && Integer.valueOf(this.owner.machineId()).equals(id);
        }
        return 0;
    }

    /** A program's number and the machine it is on ({@code ""} for this one), as the machine is asked. */
    static List<Object> whereabouts(final Object id, final Object host) {
        final List<Object> where = new ArrayList<>();
        where.add(id);
        where.add(host == null ? "" : String.valueOf(host));
        return where;
    }
}
