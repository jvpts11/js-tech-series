/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.AsmType;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.Opcode;
import dev.jstech.computers.vm.listing.Shape;
import dev.jstech.computers.vm.system.IntrinsicSpec;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One running program.
 *
 * <p>A process runs in slices. It is given a budget of instructions, spends what it can, and stops
 * where it stands with everything it needs to carry on next time: its frames, its stack and where it
 * was in each of them. That is what lets a loop that would take a minute span a minute of ticks
 * without the game waiting on it, and it is why nothing here ever blocks.
 *
 * <p>A process can have more than one thread. Each is a stack of calls of its own over the one heap,
 * and a slice is dealt out between them a few instructions at a time, so nothing ever runs at the same
 * instant and yet two things get on side by side. A thread waits, when it waits, by being left out of
 * the deal until what it waits for has come: a line typed, a tick, another thread's end, an object's
 * lock.
 *
 * <p>Nothing it does reaches outside itself except through the console it writes to and the host it
 * asks the time of, so a whole program can be run and read back with no world around it.
 */
public final class Process {

    /** Where a process is up to. A snapshot writes it by its name. */
    public enum State implements IStableName {
        /** It has instructions left to run. */
        RUNNING("running"),
        /** It is waiting for something outside it and will not spend budget until that settles. */
        PARKED("parked"),
        /** It ran to the end. */
        FINISHED("finished"),
        /** It stopped on a mistake. */
        HALTED("halted");

        private final String serializedName;

        State(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }

    /** The most instructions one thread runs before another of the same process has its turn. */
    public static final int SLICE = 64;

    /** What starting a thread costs beyond the call itself: a stack of its own is not a small thing. */
    private static final int START_COST = 49;

    /** What an event handed to a handler holds before its text: a header and its three fields. */
    private static final long EVENT_BYTES = Heap.HEADER + 3L * Heap.REFERENCE;

    /** The most calls one thread may have in progress at once; one call more halts the program. */
    private static final int DEEPEST = 1_024;

    /** Who holds an object's lock, and how many times over it took it. */
    private static final class Monitor {
        private int owner;
        private int count;
    }

    private final ProgramImage program;
    private final Heap heap;
    private final Library library;
    /** The process's threads, whose turn it is, and which of the waiting ones may run again. */
    private final ThreadScheduler scheduler = new ThreadScheduler();
    private final ProgramThread main = this.scheduler.main();
    private ProgramThread current = this.main;
    /** Whether a thread can be given instructions, asked of every thread once a round. */
    private final Predicate<ProgramThread> canRun = this::runnable;
    /** What waking the threads asks of the world: the tick, the locks, the machine's programs, the typed lines. */
    private final ThreadScheduler.IWorld world = new ThreadScheduler.IWorld() {
        @Override
        public long now() {
            return Process.this.library.now();
        }

        @Override
        public boolean locked(final Object target) {
            return Process.this.monitors.containsKey(target);
        }

        @Override
        public boolean running(final int program, final String host) {
            return Process.this.library.programRunning(program, host);
        }

        @Override
        public boolean typed() {
            return Process.this.input.has();
        }
    };
    private final Map<Object, Monitor> monitors = new IdentityHashMap<>();
    private final CallbackQueue waiting = new CallbackQueue();
    private final Map<String, Values.Obj> statics = new LinkedHashMap<>();
    private Values.Obj script;
    private final ProgramIdentity identity = new ProgramIdentity();
    /** Who the program tells when something is said to it, and the Gateway it chose to reach through. */
    private final ProgramListeners listeners = new ProgramListeners();
    private Values.Obj self;
    /** The windows this program has open on the machine's desktop, and the numbers its windows and widgets get. */
    private final ProgramWindows windows = new ProgramWindows();

    /** The number the next widget this program makes is known by. */
    long nextWidgetId() {
        return this.windows.nextWidgetId();
    }

    Heap heap0() {
        return this.heap;
    }

    Library library() {
        return this.library;
    }

    ProgramThread current() {
        return this.current;
    }

    List<ProgramThread> threads0() {
        return this.scheduler.threads();
    }

    ProgramThread mainThread() {
        return this.main;
    }

    Values.Obj staticsOf(final String owner) {
        return this.statics(owner);
    }

    void charge(final int more) {
        this.library.owe(more);
    }

    /** Ends a thread other than the main one, as the runtime's coroutines do when theirs is over. */
    void endThread(final ProgramThread thread) {
        this.end(thread);
    }

    public Process(final ProgramImage program, final long heapBytes, final IHost host) {
        this(program, heapBytes, host, true);
    }

    private Process(final ProgramImage program, final long heapBytes, final IHost host, final boolean fresh) {
        this.program = program;
        this.heap = new Heap(heapBytes);
        this.library = new Library(this.heap, host, program.entryPoint());
        this.library.serves(this);
        if (!fresh) {
            return;
        }
        /*
         * Putting the starting values in a type's own fields is the program's work like any other, so
         * it waits its turn and is paid for out of the budget rather than run on the spot.
         */
        for (final TypeImage type : program.types()) {
            if (type.setUp() != null) {
                this.waiting.add(new Frame(type.setUp(), null), 0);
            }
        }
    }

    /**
     * Where the process is up to.
     *
     * <p>It is read off the threads rather than kept: finished when the main thread has nothing left to
     * do, whatever the others are at (a program that stays up is asked again next tick, and one that
     * runs at a terminal is over); parked when everything with work to do is waiting; running otherwise.
     */
    public State state() {
        if (this.identity.halted()) {
            return State.HALTED;
        }
        if (this.main.frames.isEmpty() && this.waiting.isEmpty()) {
            return State.FINISHED;
        }
        for (final ProgramThread thread : this.scheduler.threads()) {
            if (this.runnable(thread)) {
                return State.RUNNING;
            }
        }
        return State.PARKED;
    }

    /** The program it is running, for reading it back after it has been written down. */
    public ProgramImage program() {
        return this.program;
    }

    /** What it said when it stopped, or null while it is still going. */
    public String message() {
        return this.identity.message();
    }

    /** What it has written to its own console. */
    public List<String> console() {
        return this.library.console();
    }

    /** How many lines it has written since it started, the ones no longer kept included. */
    public long written() {
        return this.library.written();
    }

    /** What it is holding, to the byte. */
    public Heap heap() {
        return this.heap;
    }

    /** How many instructions it has run since it started. */
    public long spent() {
        return this.identity.spent();
    }

    /** How many threads it has, the main one counted. */
    public int threads() {
        return this.scheduler.threads().size();
    }

    /** What the program was started with, as its {@code Program.Args} reads them. */
    public void setArgs(final List<String> arguments) {
        this.identity.startWith(arguments);
    }

    public List<String> args() {
        return this.identity.args();
    }

    /** Tells the process the number the machine lists it under, which is what it calls itself by. */
    public void identify(final int id) {
        this.identity.identify(id);
    }

    public int machineId() {
        return this.identity.machineId();
    }

    // the Gateways to ComputerCraft

    /** Which Gateway this program's calls go through; empty for whichever the machine lists first. */
    public String gatewayName() {
        return this.listeners.gateway();
    }

    /** Remembers the Gateway the program chose, which it keeps across a reload like anything else. */
    public void chooseGateway(final String name) {
        this.listeners.chooseGateway(name);
    }

    /** Who to tell when a ComputerCraft computer says something; null takes the listener away. */
    public void hearGateway(@org.jetbrains.annotations.Nullable final Values.DelegateValue handler) {
        this.listeners.hearGateway(handler);
    }

    /**
     * Hands the program what a ComputerCraft computer said through a Gateway.
     *
     * <p>It is queued for the handler the program gave {@code Gateway.OnMessage}, on the main thread, in
     * its turn. A program that gave none does not hear it, and neither does one with too many calls
     * already waiting to take it; false says so.
     */
    public boolean deliverGatewayMessage(final int from, final String text, final long tick) {
        final Values.DelegateValue handler = this.listeners.onGatewayMessage();
        if (this.identity.over() || handler == null) {
            return false;
        }
        final String said = text == null ? "" : text;
        return this.offer(handler, EVENT_BYTES + Heap.sizeOfText(said),
                () -> List.of(this.gatewayMessageOf(from, said, tick)));
    }

    /** What a Gateway message is as a value the program holds. */
    private Values.Obj gatewayMessageOf(final int from, final String text, final long tick) {
        final Values.Obj made = new Values.Obj("GatewayMessage");
        made.set("From", (long) from);
        made.set("Text", this.text(text == null ? "" : text, 0));
        made.set("Tick", tick);
        this.heap.allocate(made, EVENT_BYTES, 0);
        return made;
    }

    /** Whether the program ended itself with {@code Program.Exit}. */
    public boolean exited() {
        return this.identity.exited();
    }

    /** How the program ended: what it said with {@code Program.Exit}, one for a halt, zero otherwise. */
    public int exitCode() {
        return this.identity.exitCode();
    }

    /**
     * Hands the process a line from another program.
     *
     * <p>It is queued for the handler the program gave {@code Program.OnMessage}, on the main thread,
     * in its turn; a program that gave none simply does not hear it. False when the program is over, or
     * when it has too many calls already waiting to take this one, so the sender knows it was not heard.
     */
    public boolean deliverMessage(final int from, final String text, final long tick) {
        if (this.identity.over()) {
            return false;
        }
        final String said = text == null ? "" : text;
        return this.offer(this.listeners.onMessage(), EVENT_BYTES + Heap.sizeOfText(said),
                () -> List.of(this.messageOf(from, said, tick)));
    }

    private Values.Obj messageOf(final int from, final String text, final long tick) {
        final Values.Obj made = new Values.Obj("ProcessMessage");
        made.set("From", from);
        made.set("Text", this.text(text == null ? "" : text, 0));
        made.set("Tick", tick);
        this.heap.allocate(made, EVENT_BYTES, 0);
        return made;
    }

    /** Ends the program where it stands with that code: every thread stops and nothing is asked again. */
    private void exit(final int code) {
        this.identity.exit(code);
        this.main.frames.clear();
        this.waiting.clear();
        for (final ProgramThread other : List.copyOf(this.scheduler.threads())) {
            if (other != this.main) {
                this.end(other);
            }
        }
    }

    /** What the program holds itself by: its number on the machine and its name, made on first use. */
    private Values.Obj selfToken(final int line) {
        if (this.self == null) {
            final Values.Obj token = new Values.Obj("Process");
            token.set("Id", this.identity.machineId());
            token.set("Name", this.text(this.identity.name(), line));
            token.set("Host", this.text("", line));
            this.heap.allocate(token, Heap.HEADER + 3L * Heap.REFERENCE, line);
            this.self = token;
        }
        return this.self;
    }

    /** A fresh list of the arguments, the program's to hold and to free like anything else. */
    private Values.ListValue argsList(final int line) {
        final Values.ListValue made = new Values.ListValue();
        for (final String arg : this.identity.args()) {
            made.items().add(this.text(arg, line));
        }
        this.heap.allocate(made, made.bytes(), line);
        return made;
    }

    /** The calls on {@code Program} the process answers itself: the ones about this very program. */
    private boolean programCall(final Frame frame, final IOperand.Method named, final int line) {
        switch (named.name()) {
            case "Exit" -> {
                final List<Object> arguments = this.take(frame, named.parameters());
                this.exit(arguments.isEmpty() ? 0 : Numbers.toInt(arguments.getFirst()));
                return true;
            }
            case "OnMessage" -> {
                final List<Object> arguments = this.take(frame, named.parameters());
                this.listeners.hearMessages(arguments.isEmpty()
                        || !(arguments.getFirst() instanceof Values.DelegateValue handler) ? null : handler);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * The calls on a {@code Process}: waiting is the process's own business, the rest is the machine's,
     * asked under {@code Program} with the other program's number.
     */
    private void processCall(final Frame frame, final IOperand.Method named, final int line) {
        switch (named.name()) {
            case "Wait" -> this.waitFor(frame, named, line);
            case "Send" -> {
                final List<Object> arguments = this.take(frame, named.parameters());
                this.push(frame, named, this.library.call(new IOperand.Method("Program", "Send",
                        named.parameters(), named.returns()), null, arguments, line));
            }
            case "Kill", "Output" -> {
                this.take(frame, named.parameters());
                final Object token = frame.pop();
                final Integer id = this.processId(token, line);
                this.push(frame, named, this.library.call(new IOperand.Method("Program", named.name(),
                        List.of("int", "string"), named.returns()), null,
                        Library.whereabouts(id, processHost(token)), line));
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "a process has no " + named.name());
        }
    }

    private Integer processId(final Object token, final int line) {
        if (this.heap.alive(token, line) instanceof Values.Obj object && object.get("Id") instanceof Integer id) {
            return id;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no process here");
    }

    /** The machine a process handle points at: what its {@code Host} says, or this one when it says nothing. */
    private static String processHost(final Object token) {
        return token instanceof Values.Obj object && object.get("Host") instanceof String host ? host : "";
    }

    /** Waits for another program to end, the way a join waits for a thread. */
    private void waitFor(final Frame frame, final IOperand.Method named, final int line) {
        final int count = named.parameters().size();
        final Object token = frame.stack.size() > count ? frame.stack.get(frame.stack.size() - 1 - count) : null;
        final Integer id = this.processId(token, line);
        final String host = processHost(token);
        final boolean over = !this.library.programRunning(id, host);
        final long ticks = count == 1 ? Numbers.toLong(frame.peek()) : 0L;
        // Read before the test, so it is forgotten whenever the call answers, as it always was.
        final boolean gaveUp = this.current.takeGaveUp();
        if (over || gaveUp || (count == 1 && ticks <= 0)) {
            this.take(frame, named.parameters());
            frame.pop();
            if (count == 1) {
                frame.push(over);
            }
            return;
        }
        frame.at--;
        this.scheduler.await(this.current, new IWait.Child(id, host, ticks > 0 ? this.library.now() + ticks : 0L));
    }

    /**
     * Makes an instance of a class the program declares.
     *
     * <p>The object comes back at once and its constructor waits its turn, so making one costs the
     * budget like everything else and a constructor that never ends cannot hold up the tick.
     */
    public Values.Obj create(final String type) {
        final Object made = this.instance(type, List.of(), 0);
        if (!(made instanceof Values.Obj object)) {
            return null;
        }
        if (this.script == null) {
            this.script = object;
        }
        return object;
    }

    /**
     * The script this process is running, which is the first object it was asked to make.
     *
     * <p>Whatever runs the process needs it back to call the script again on the next tick, and needs it
     * back after a reload as well, so it is remembered here rather than by the caller.
     */
    public Values.Obj script() {
        return this.script;
    }

    /** Whether this is a program that runs at a terminal or one that stays up. */
    public Shape shape() {
        return this.program.shape();
    }

    /**
     * Puts a call on a type itself in the queue, for a program that starts at a static method and so
     * never has an instance of anything to be called on.
     */
    public void beginStatic(final String owner, final String method) {
        final MethodImage found = this.program.method(owner, method, List.of());
        if (found == null) {
            this.halt(new Halt(Halt.Reason.NO_SUCH_MEMBER, 0, owner + " has no " + method + " to run"));
            return;
        }
        this.waiting.add(new Frame(found, null), 0);
    }

    /**
     * Whether the program can be given another turn of that method right now.
     *
     * <p>It cannot while one is already queued and has not had its chance, while the thread those turns
     * run on is still busy with an earlier turn or a handler, and while that thread is waiting for
     * something: a line to be typed, a lock, an answer from the other side of a Gateway. A tick that
     * comes while a program works or waits is missed, rather than piling up to be run the moment it is
     * free, which is neither what a tick means nor something the machine can afford in one go.
     */
    public boolean readyForTurn(final String method) {
        return !this.main.waiting() && this.main.frames.isEmpty() && !this.waiting.holds(method);
    }

    /** Puts a call on that object in the queue, to be run by the slices that follow. */
    public void begin(final Values.Obj self, final String method) {
        final MethodImage found = this.program.method(self.type(), method, List.of());
        if (found == null) {
            this.halt(new Halt(Halt.Reason.NO_SUCH_MEMBER, 0,
                    self.type() + " has no " + method + " to run"));
            return;
        }
        this.waiting.add(new Frame(found, self), 0);
    }

    /**
     * Puts a call on that object ahead of everything waiting, for a script being stopped: its farewell has
     * the little time left to itself before any handler still queued.
     */
    public void beginFirst(final Values.Obj self, final String method) {
        final MethodImage found = this.program.method(self.type(), method, List.of());
        if (found == null) {
            this.halt(new Halt(Halt.Reason.NO_SUCH_MEMBER, 0,
                    self.type() + " has no " + method + " to run"));
            return;
        }
        this.waiting.addFirst(new Frame(found, self), 0);
    }

    /**
     * Runs up to {@code budget} instructions and says how many it used.
     *
     * <p>The budget is dealt out between the threads that can run, in turns of a slice each, so that
     * none of them finishes a tick's worth before another has begun. It stops early when nothing is
     * left that can run: the program finished, halted, or every thread is waiting. Whatever it did not
     * use is left, because a process that has nothing to do should not be charged for the tick.
     */
    public int step(final int budget) {
        int used = 0;
        try {
            this.wake();
            while (used < budget && !this.identity.halted()) {
                final List<ProgramThread> ready = this.scheduler.ready(this.canRun);
                if (ready.isEmpty()) {
                    break;
                }
                final int slice = Math.max(1, Math.min(SLICE, (budget - used) / ready.size()));
                final int first = this.scheduler.firstTurn(ready.size());
                int moved = 0;
                for (int k = 0; k < ready.size() && used < budget && !this.identity.halted(); k++) {
                    final int ran = this.run(ready.get((first + k) % ready.size()),
                            Math.min(slice, budget - used));
                    used += ran;
                    moved += ran;
                }
                this.scheduler.turned(first);
                if (moved == 0) {
                    break;
                }
            }
            /*
             * The last window was shut and the program has had its say about it: a program whose windows
             * are gone has nothing left to be looked at, and ends.
             */
            if (this.waiting.isEmpty() && this.windows.endsNow()) {
                this.exit(0);
            }
        } catch (final Halt halt) {
            this.halt(halt);
        } catch (final RuntimeException fault) {
            // What goes wrong between instructions (waking a thread, asking the machine) ends here too.
            this.halt(this.fault(fault, 0));
        }
        return used;
    }

    /** Whether a thread can be given instructions right now. */
    private boolean runnable(final ProgramThread thread) {
        if (thread.waiting()) {
            return false;
        }
        return !thread.frames.isEmpty() || (thread == this.main && !this.waiting.isEmpty());
    }

    /** Runs one thread for up to {@code allowance} instructions, or until it stops on its own. */
    private int run(final ProgramThread thread, final int allowance) {
        this.current = thread;
        thread.yielded = false;
        int used = 0;
        try {
            while (used < allowance && !this.identity.halted() && !thread.waiting() && !thread.yielded) {
                if (thread.frames.isEmpty() && (thread != this.main || !this.take())) {
                    break;
                }
                used++;
                try {
                    this.one();
                } catch (final Halt halt) {
                    this.fail(thread, halt);
                } catch (final RuntimeException fault) {
                    final Frame top = thread.frames.peek();
                    this.fail(thread, this.fault(fault, top == null ? 0 : top.at));
                }
                /*
                 * Reaching into the machine costs more than moving a number about, and the difference is
                 * charged to this tick rather than hidden, so a program that talks to the world all the time
                 * gets through less of itself than one that does its own arithmetic.
                 */
                used += this.library.drawCost();
                if (thread.frames.isEmpty()) {
                    this.ended(thread);
                }
            }
        } finally {
            /*
             * Counted once a turn rather than once an instruction: nothing reads the count in the middle of a
             * turn, and writing it to another object after every instruction is a measurable share of what an
             * instruction costs.
             */
            this.identity.spend(used);
        }
        return used;
    }

    /**
     * A failure of the runtime itself, made into the end of the one process it happened in.
     *
     * <p>A program cannot make the runtime throw by being wrong: every mistake it can make is checked and
     * halts with a message of its own. What arrives here is a fault of the machine, or of a listing
     * written by hand that no compiler would produce. Either way the server's tick is no place for it, so
     * the process stops and says so, and the machine is told so it can write down what is needed to find
     * the fault.
     */
    private Halt fault(final RuntimeException cause, final int line) {
        this.library.fault(this.name(), line, cause);
        return new Halt(Halt.Reason.FAULT, line,
                "the runtime could not carry this out (" + cause.getClass().getSimpleName() + ")");
    }

    /**
     * A thread whose last call has returned.
     *
     * <p>Any thread but the main one is simply over. When the main thread of a program that runs at a
     * terminal returns, the program is over, and it takes its threads with it; a program that stays up
     * keeps its threads between ticks, since its main thread returns every tick by design.
     */
    private void ended(final ProgramThread thread) {
        if (thread != this.main) {
            this.end(thread);
            return;
        }
        if (this.waiting.isEmpty() && this.program.shape() != Shape.SCRIPT) {
            for (final ProgramThread other : List.copyOf(this.scheduler.threads())) {
                if (other != this.main) {
                    this.end(other);
                }
            }
        }
    }

    /** Lets every thread whose wait is over run again. */
    private void wake() {
        this.scheduler.wake(this.world);
    }

    /**
     * Waits for a line to be typed, spending nothing until it comes.
     *
     * <p>Nothing here blocks a thread: a thread that is waiting simply stops being given budget, and
     * the rest of the program carries on without it.
     */
    public void park() {
        this.scheduler.await(this.current, IWait.INPUT);
    }

    /** Lets every thread waiting on a typed line have budget again. */
    public void resume() {
        this.scheduler.wakeReaders();
    }

    /** The lines typed at the terminal this process is in front of, waiting for the program to read them. */
    private final ProgramInput input = new ProgramInput();

    /** Hands the process a typed line; a thread stopped on a read carries on with it. */
    public void offerInput(final String line) {
        this.input.offer(line);
        if (this.waitingForInput()) {
            this.resume();
        }
    }

    /**
     * Whether the process is stopped on a read: a thread waiting for a typed line, which is how a read that finds
     * nothing typed waits. The wait is written down with each thread, so a process put away mid-read and brought
     * back is still seen to be waiting.
     */
    public boolean waitingForInput() {
        return this.scheduler.anyReader();
    }

    /**
     * Names the program, as its own call to {@code Program.SetName} does; blank means no name. The object the program
     * holds itself by takes the new name too, since {@code Program.Current} hands back that same object every time.
     */
    void setName(final String value, final int line) {
        this.identity.rename(value);
        if (this.self != null) {
            this.self.set("Name", this.text(this.identity.name(), line));
        }
    }

    /** The name the program gave itself, or empty when it gave none. */
    public String name() {
        return this.identity.name();
    }

    /** The next line typed, or an empty string when none has been. */
    String takeInput() {
        return this.input.take();
    }

    /** Whether a typed line is waiting to be read. */
    boolean hasInput() {
        return this.input.has();
    }

    /**
     * Puts a handler in the queue, to run on the main thread when it next has nothing else to do.
     *
     * <p>Handlers run in the order they arrived, in the same slice as the work that was already
     * there, out of the same budget. So a process that fires a great many of them does not get more
     * of the tick than one that fires none. A delegate joined from several handlers queues each of
     * them, in the order they were joined and with the same arguments, so every listener hears the
     * event; one whose method cannot be found is passed over and the rest still run.
     *
     * <p>An event that finds no room for its calls, or for what their arguments hold, is dropped and
     * counted, which is what false says: the widget or the watch behind it already holds the latest
     * value, so the next handler that runs reads what is true now.
     */
    public boolean post(final Values.DelegateValue handler, final List<Object> arguments) {
        if (this.offer(handler, this.weigh(arguments), () -> arguments)) {
            return true;
        }
        this.waiting.drop();
        return false;
    }

    /**
     * Queues a handler's calls when there is room for them and for what their arguments hold, each call
     * counted with all of them, and only then builds the arguments, so a call turned away leaves nothing
     * behind on the heap. False when there was no room; a handler with nothing to call always fits.
     */
    private boolean offer(final Values.DelegateValue handler, final long bytes,
                          final Supplier<List<Object>> arguments) {
        final List<Frame> calls = this.callsOf(handler);
        if (calls.isEmpty()) {
            return true;
        }
        if (!this.waiting.fits(calls.size(), bytes * calls.size())) {
            return false;
        }
        final List<Object> handed = arguments.get();
        for (final Frame call : calls) {
            fill(call, handed);
            this.waiting.add(call, bytes);
        }
        return true;
    }

    /** Puts a handler's calls ahead of everything waiting, in their own order; nothing turns these away. */
    private void ahead(final Values.DelegateValue handler) {
        final List<Frame> calls = this.callsOf(handler);
        for (int i = calls.size() - 1; i >= 0; i--) {
            this.waiting.addFirst(calls.get(i), 0);
        }
    }

    /* One call for each method joined to the handler, in order, passing over any that cannot be found. */
    private List<Frame> callsOf(final Values.DelegateValue handler) {
        if (handler == null) {
            return List.of();
        }
        final List<Frame> calls = new ArrayList<>(handler.chain().size());
        for (final Values.Bound bound : handler.chain()) {
            final MethodImage method =
                    this.program.method(bound.owner(), bound.method(), bound.parameters());
            if (method != null && method.hasCode()) {
                calls.add(new Frame(method, bound.target()));
            }
        }
        return calls;
    }

    /* What the arguments handed to a call hold on the heap: each one, and whatever its fields hold. */
    private long weigh(final List<Object> arguments) {
        long bytes = 0;
        for (final Object argument : arguments) {
            bytes += this.heap.bytesOf(argument);
            if (argument instanceof Values.Obj object) {
                for (final Object field : object.all().values()) {
                    bytes += this.heap.bytesOf(field);
                }
            }
        }
        return bytes;
    }

    /* The same, for a call read back out of a save: what it was handed sits in the first slots of its frame. */
    private long weigh(final Frame call) {
        final int handed = Math.min(call.method.parameters().size(), call.slots.length);
        return this.weigh(Arrays.asList(call.slots).subList(0, handed));
    }

    /** How many calls are still waiting their turn, handlers among them. */
    public int waiting() {
        return this.waiting.size();
    }

    /** How many clicks and watch alerts found no room among the calls waiting and were let go. */
    long droppedEvents() {
        return this.waiting.dropped();
    }

    // windows

    /**
     * Opens a window on the machine's desktop.
     *
     * <p>A machine with no desktop has nowhere to put it and says so, which is the whole of what a
     * program needs to be told: a window is a thing a system with a desktop has.
     */
    void openWindow(final Values.Obj window, final int line) {
        if (!Boolean.TRUE.equals(this.library.peek("Computer", "Desktop", List.of()))) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "this computer has no desktop to open a window on");
        }
        this.windows.open(window, line);
    }

    /** Closes a window, which takes it off the desktop; closing one that is not open is nothing at all. */
    void closeWindow(final Values.Obj window) {
        this.windows.close(window);
    }

    /** The windows this program has open, in the order it opened them. */
    public List<Values.Obj> windows() {
        return this.windows.all();
    }

    /** The window of that number, or null when the program has no such window open. */
    public Values.Obj windowOf(final long id) {
        return this.windows.of(id);
    }

    /**
     * Tells the program what a player did to one of its widgets.
     *
     * <p>What the widget holds is changed first, the way the player changed it (a box is ticked, a line
     * is typed), and only then is the program's handler queued: a handler that reads the widget reads
     * what the player sees. A widget with no handler still changes, and so does one whose handler finds
     * no room among the calls waiting and is dropped. Closing a window always gets in, ahead of the rest.
     */
    public boolean deliverUiEvent(final long window, final long widget, final String kind,
                                  final List<Object> values) {
        if (this.identity.over()) {
            return false;
        }
        final Values.Obj open = this.windowOf(window);
        if (open == null) {
            return false;
        }
        if ("close".equals(kind)) {
            this.closeWindow(open);
            this.ahead(handlerOf(open, "OnClose"));
            /*
             * Shutting the last window of a program is how a person ends it, as it is on any desktop. The
             * program hears it first, and one that opens another window in its OnClose carries on.
             */
            this.windows.closedByPerson();
            return true;
        }
        final Values.Obj found = UiWidgets.widgetOf(open, widget);
        if (found == null || !Boolean.TRUE.equals(found.get(UiWidgets.ENABLED))
                || !Boolean.TRUE.equals(found.get(UiWidgets.VISIBLE))) {
            return false;
        }
        final String handler;
        try {
            handler = this.library.ui().accept(found, kind, values);
        } catch (final Halt halt) {
            this.halt(halt);
            return false;
        }
        if (handler == null) {
            return false;
        }
        this.post(handlerOf(found, handler), List.of());
        return true;
    }

    private static Values.DelegateValue handlerOf(final Values.Obj widget, final String name) {
        return widget.get(name) instanceof Values.DelegateValue handler ? handler : null;
    }

    // threads

    /** The thread of that number, or null once it is over. */
    private ProgramThread thread(final Object id) {
        return this.scheduler.thread(id);
    }

    /** The thread of that number. */
    ProgramThread threadById(final Object id) {
        return this.thread(id);
    }

    /**
     * What the program holds a thread by, made the first time it is asked for.
     *
     * <p>{@code Running} is kept true to life on the object itself, so reading it is reading a field
     * like any other and costs what that costs.
     */
    private Values.Obj tokenFor(final ProgramThread thread, final int line) {
        if (thread.token == null) {
            final Values.Obj token = new Values.Obj("Thread");
            token.set("Id", thread.id);
            token.set("Running", true);
            this.heap.allocate(token, Heap.HEADER + 2L * Heap.REFERENCE, line);
            thread.token = token;
        }
        return thread.token;
    }

    /** Starts a thread on the body a delegate holds, and hands back what the program holds it by. */
    private Values.Obj spawn(final Object body, final int line) {
        if (!(body instanceof Values.DelegateValue delegate) || delegate.chain().isEmpty()) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no body to run on the thread");
        }
        final Values.Bound bound = delegate.chain().getFirst();
        final MethodImage method =
                this.program.method(bound.owner(), bound.method(), bound.parameters());
        if (method == null || !method.hasCode()) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line,
                    "there is no " + bound.method() + " to run on the thread");
        }
        final ProgramThread made = this.scheduler.start();
        made.frames.push(new Frame(method, bound.target()));
        this.library.owe(START_COST);
        return this.tokenFor(made, line);
    }

    /** Ends a thread other than the main one: it lets go of what it locked and wakes whoever joined it. */
    private void end(final ProgramThread thread) {
        thread.frames.clear();
        thread.wait = IWait.NONE;
        if (thread.token != null) {
            thread.token.set("Running", false);
        }
        this.scheduler.remove(thread);
        this.release(thread.id);
    }

    /** Answers a call on {@code Thread}, which is the process's own business rather than the library's. */
    private void threadCall(final Frame frame, final IOperand.Method named, final int line) {
        switch (named.name()) {
            case "Start" -> {
                final List<Object> arguments = this.take(frame, named.parameters());
                frame.push(this.spawn(arguments.isEmpty() ? null : arguments.getFirst(), line));
            }
            case "Sleep" -> {
                final long ticks = Numbers.toLong(this.take(frame, named.parameters()).getFirst());
                if (ticks > 0) {
                    this.scheduler.await(this.current, new IWait.Sleep(this.library.now() + ticks));
                }
            }
            case "Yield" -> {
                this.take(frame, named.parameters());
                this.current.yielded = true;
            }
            case "Join" -> this.join(frame, named, line);
            case "Stop" -> {
                this.take(frame, named.parameters());
                final ProgramThread target = this.thread(this.threadId(frame.pop(), line));
                if (target == this.main) {
                    this.main.frames.clear();
                    this.waiting.clear();
                    this.ended(this.main);
                } else if (target != null) {
                    this.end(target);
                }
            }
            default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "Thread has no " + named.name());
        }
    }

    private Integer threadId(final Object token, final int line) {
        if (this.heap.alive(token, line) instanceof Values.Obj object && object.get("Id") instanceof Integer id) {
            return id;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no thread here");
    }

    /**
     * Waits for another thread to end.
     *
     * <p>Nothing is taken off the stack until the wait is over, so the call can be asked again once the
     * thread has ended or the time given has run out, and asking it again is the same as asking it once.
     */
    private void join(final Frame frame, final IOperand.Method named, final int line) {
        final int count = named.parameters().size();
        final Object token = frame.stack.size() > count ? frame.stack.get(frame.stack.size() - 1 - count) : null;
        final ProgramThread target = this.thread(this.threadId(token, line));
        final boolean over = target == null || target == this.current;
        final long ticks = count == 1 ? Numbers.toLong(frame.peek()) : 0L;
        // Read before the test, so it is forgotten whenever the call answers, as it always was.
        final boolean gaveUp = this.current.takeGaveUp();
        if (over || gaveUp || (count == 1 && ticks <= 0)) {
            this.take(frame, named.parameters());
            frame.pop();
            if (count == 1) {
                frame.push(over);
            }
            return;
        }
        frame.at--;
        this.scheduler.await(this.current, new IWait.Join(target.id, ticks > 0 ? this.library.now() + ticks : 0L));
    }

    // locks

    /**
     * Takes the lock of the object on top of the stack.
     *
     * <p>A thread that already holds it takes it once more, and has to let go as many times. One that
     * finds it held by another leaves the object where it is and waits, to ask again when it is free.
     */
    private void enterMonitor(final Frame frame, final int line) {
        final Object target = this.heap.alive(frame.peek(), line);
        final Monitor held = this.monitors.get(target);
        if (held == null) {
            final Monitor made = new Monitor();
            made.owner = this.current.id;
            made.count = 1;
            this.monitors.put(target, made);
            frame.pop();
            return;
        }
        if (held.owner == this.current.id) {
            held.count++;
            frame.pop();
            return;
        }
        frame.at--;
        this.scheduler.await(this.current, new IWait.Lock(target));
    }

    private void exitMonitor(final Frame frame, final int line) {
        final Object target = this.heap.alive(frame.pop(), line);
        final Monitor held = this.monitors.get(target);
        if (held == null || held.owner != this.current.id) {
            throw new Halt(Halt.Reason.NOT_LOCKED, line, "this thread is letting go of a lock it does not hold");
        }
        if (--held.count == 0) {
            this.monitors.remove(target);
            this.scheduler.wakeLocked(target);
        }
    }

    /** Lets go of every lock a thread holds, as its end does. */
    private void release(final int owner) {
        final Iterator<Map.Entry<Object, Monitor>> each = this.monitors.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<Object, Monitor> entry = each.next();
            if (entry.getValue().owner == owner) {
                // An entry the iterator has removed can no longer be read, so the locked object is taken first.
                final Object target = entry.getKey();
                each.remove();
                this.scheduler.wakeLocked(target);
            }
        }
    }

    // watching the world

    /** What a watch is waiting for. A snapshot writes it by its name. */
    public enum Watching implements IStableName {
        /** Any change at all in what the network holds of that thing. */
        CHANGE("change"),
        /** The moment it falls to or below a number, and not again until it has gone back above. */
        BELOW("below"),
        /** The moment it rises to or above a number, and not again until it has gone back below. */
        ABOVE("above");

        private static final StableNames<Watching> NAMES = StableNames.of(Watching.class);

        private final String serializedName;

        Watching(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }

        /**
         * The watch a snapshot names.
         *
         * @throws IllegalArgumentException for a name no watch declares, which only a damaged snapshot holds
         */
        static Watching named(final String name) {
            final Watching found = NAMES.find(name);
            if (found == null) {
                throw new IllegalArgumentException("no watch is named '" + name + "'");
            }
            return found;
        }
    }

    private final ProgramWatches watches = new ProgramWatches();

    /**
     * Asks to be told when what the network holds of something changes.
     *
     * <p>The program is handed a token. Disposing it stops the watch, which is the same gesture that
     * frees anything else, so there is nothing new to learn: what a program stops holding, it stops
     * paying for, and here it also stops being woken by.
     */
    public Values.Obj watch(final String item, final Watching kind, final long threshold,
                            final Values.DelegateValue handler, final int line) {
        if (handler == null || handler.chain().isEmpty()) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no handler to call for " + item);
        }
        final Values.Obj token = new Values.Obj("Subscription");
        token.set("Id", this.watches.nextId());
        token.set("Item", item);
        this.heap.allocate(token, Heap.HEADER + 2L * Heap.REFERENCE, line);
        this.watches.add(item, kind, threshold, handler, token);
        return token;
    }

    /**
     * Everything this process is watching, so whatever has the world can look each up once and no more,
     * however many watches are waiting on the same thing.
     */
    public List<String> watching() {
        return this.watches.watching();
    }

    /**
     * Hands in what the world now holds, and queues a call for every watch that was waiting for it.
     *
     * <p>A watch whose token the program has thrown away is dropped here rather than fired: stopping is
     * the program's to decide, and it decided. A call that finds no room among the calls waiting is
     * dropped and counted; the watch already holds the new number, so the next one to fire reads it.
     */
    public void deliver(final Map<String, Long> totals) {
        this.watches.deliver(totals, this.heap::isFreed, this::fired);
    }

    private void fired(final ProgramWatches.Watch watch, final long before, final long now, final boolean first) {
        if (!this.offer(watch.handler(), EVENT_BYTES,
                () -> List.of(this.stockEvent(watch.item(), before, now, first)))) {
            this.waiting.drop();
        }
    }

    private Values.Obj stockEvent(final String item, final long before, final long now,
                                  final boolean first) {
        final Values.Obj made = new Values.Obj("StockEvent");
        made.set("Item", item);
        made.set("Total", now);
        made.set("Previous", first ? now : before);
        this.heap.allocate(made, EVENT_BYTES, 0);
        return made;
    }

    public Values.DelegateValue handlerFor(final Values.Obj self, final String name) {
        final TypeImage type = this.program.type(self.type());
        for (final MethodImage method : type.methods().values()) {
            if (method.name().equals(name)) {
                return new Values.DelegateValue(self.type(), List.of(new Values.Bound(self,
                        method.owner(), method.name(), method.parameters(), method.returns())));
            }
        }
        return null;
    }

    // Starts the next call that was waiting on the main thread, if there is one and it has nothing else to do.
    private boolean take() {
        final Frame next = this.waiting.poll();
        if (next == null) {
            return false;
        }
        this.main.frames.push(next);
        return true;
    }

    /**
     * A halt in a thread is the end of the process.
     */
    private void fail(final ProgramThread thread, final Halt first) {
        this.halt(first);
    }

    private void halt(final Halt halt) {
        this.identity.halt(halt.getMessage());
        this.library.write(halt.getMessage());
        for (final ProgramThread thread : this.scheduler.threads()) {
            thread.frames.clear();
        }
        this.monitors.clear();
    }

    // putting it away and back

    /**
     * Writes the whole process down: what it has allocated, what each thread was doing, and where each
     * of its calls had got to.
     *
     * <p>Two names for one object come back as two names for one object, because everything still held,
     * and anything freed that something written still reaches, is written under a number and every
     * reference is written as that number.
     */
    public Snapshot save() {
        final HeldNumbers numbers = new HeldNumbers(this.heap);
        final List<Snapshot.ThreadShot> running = new ArrayList<>();
        for (final ProgramThread thread : this.scheduler.threads()) {
            running.add(freeze(thread, numbers));
        }
        final List<Snapshot.FrameShot> queued = new ArrayList<>();
        for (final Frame frame : this.waiting) {
            queued.add(freeze(frame, numbers));
        }
        final Map<String, Map<String, Snapshot.IValue>> kept = new LinkedHashMap<>();
        for (final Map.Entry<String, Values.Obj> entry : this.statics.entrySet()) {
            kept.put(entry.getKey(), fields(entry.getValue(), numbers));
        }
        final List<Snapshot.WatchShot> watching = new ArrayList<>();
        for (final ProgramWatches.Watch watch : this.watches.all()) {
            watching.add(new Snapshot.WatchShot(watch.id(), watch.item(), watch.kind().serializedName(),
                    watch.threshold(), value(watch.handler(), numbers), value(watch.token(), numbers), watch.last(),
                    watch.armed(), watch.seen()));
        }
        final List<Snapshot.MonitorShot> locked = new ArrayList<>();
        for (final Map.Entry<Object, Monitor> entry : this.monitors.entrySet()) {
            locked.add(new Snapshot.MonitorShot(value(entry.getKey(), numbers), entry.getValue().owner,
                    entry.getValue().count));
        }
        final Snapshot.IValue scriptShot = value(this.script, numbers);
        final Snapshot.IValue onMessageShot = value(this.listeners.onMessage(), numbers);
        final List<Snapshot.IValue> windowShots = values(this.windows.held(), numbers);
        final Snapshot.IValue onGatewayShot = value(this.listeners.onGatewayMessage(), numbers);
        /*
         * The objects are written last: writing anything else down can number a freed thing it still
         * reaches, and so can writing an object, so the numbers are walked while they grow.
         */
        final List<Snapshot.IHeld> held = new ArrayList<>();
        for (int number = 0; number < numbers.size(); number++) {
            held.add(this.freeze(numbers.thing(number), number, numbers));
        }
        return new Snapshot(this.heap.budget(), held, running, queued, kept, scriptShot,
                watching, this.library.console(), this.library.written(), this.library.randomState(),
                this.input.lines(), this.waiting.dropped(), this.state().serializedName(),
                this.identity.message() == null ? "" : this.identity.message(),
                this.identity.spent(), this.identity.name(), locked, this.scheduler.nextId(), this.identity.args(),
                this.identity.machineId(), this.identity.exited(), this.identity.givenExitCode(), onMessageShot,
                windowShots, this.windows.nextWindow(), this.windows.nextWidget(), this.windows.endWithWindows(),
                onGatewayShot, this.listeners.gateway());
    }

    /** Reads a process back out of what {@link #save()} wrote, ready to carry on where it stopped. */
    public static Process restore(final ProgramImage program, final Snapshot shot, final IHost host) {
        final Process process = new Process(program, shot.heapBudget(), host, false);
        process.identity.rename(shot.name());
        final Map<Integer, Object> byNumber = new LinkedHashMap<>();
        for (final Snapshot.IHeld written : shot.held()) {
            byNumber.put(written.id(), shell(written));
        }
        /*
         * Handlers are settled before anything is filled in, because one cannot be changed after it is
         * made and whatever points at one has to point at the one that stays.
         */
        for (final Snapshot.IHeld written : shot.held()) {
            if (written instanceof Snapshot.IHeld.Handler handler) {
                final List<Values.Bound> chain = new ArrayList<>();
                for (final Snapshot.BoundShot bound : handler.chain()) {
                    chain.add(new Values.Bound(value(bound.target(), byNumber), bound.owner(),
                            bound.method(), bound.parameters(), bound.returns()));
                }
                byNumber.put(handler.id(), new Values.DelegateValue(handler.type(), chain));
            }
        }
        for (final Snapshot.IHeld written : shot.held()) {
            fill(written, byNumber);
            process.heap.restore(byNumber.get(written.id()), written.bytes(), written.line(),
                    written.freed());
        }
        for (final Snapshot.ThreadShot written : shot.threads()) {
            final ProgramThread thread = process.scheduler.restore(written.id());
            for (final Snapshot.FrameShot each : written.frames()) {
                final Frame frame = thaw(program, each, byNumber);
                if (frame != null) {
                    thread.frames.push(frame);
                }
            }
            thread.wait = IWait.read(written.parked(), written.until(), value(written.on(), byNumber),
                    written.onHost());
            thread.restoreGivenUp(written.timedOut());
            if (value(written.token(), byNumber) instanceof Values.Obj token) {
                thread.token = token;
            }
        }
        process.scheduler.startFrom(shot.nextThread());
        for (final Snapshot.MonitorShot written : shot.monitors()) {
            final Object target = value(written.target(), byNumber);
            if (target != null) {
                final Monitor held = new Monitor();
                held.owner = written.owner();
                held.count = written.count();
                process.monitors.put(target, held);
            }
        }
        for (final Snapshot.FrameShot written : shot.waiting()) {
            final Frame frame = thaw(program, written, byNumber);
            if (frame != null) {
                process.waiting.add(frame, process.weigh(frame));
            }
        }
        for (final Map.Entry<String, Map<String, Snapshot.IValue>> entry : shot.statics().entrySet()) {
            final Values.Obj holder = process.statics(entry.getKey());
            for (final Map.Entry<String, Snapshot.IValue> field : entry.getValue().entrySet()) {
                holder.set(field.getKey(), value(field.getValue(), byNumber));
            }
        }
        if (value(shot.script(), byNumber) instanceof Values.Obj script) {
            process.script = script;
        }
        for (final Snapshot.WatchShot written : shot.watches()) {
            if (value(written.handler(), byNumber) instanceof Values.DelegateValue handler
                    && value(written.token(), byNumber) instanceof Values.Obj token) {
                process.watches.restore(written, handler, token);
            }
        }
        process.library.restore(shot.console(), shot.written(), shot.random());
        process.input.restore(shot.input());
        process.waiting.startFrom(shot.dropped());
        process.identity.restore(shot.args(), shot.machineId(), shot.spent(), shot.exited(), shot.exitCode(),
                State.HALTED.serializedName().equals(shot.state()), shot.message().isEmpty() ? null : shot.message());
        // The windows the program had open come back open, with everything they were showing.
        for (final Snapshot.IValue written : shot.windows()) {
            if (value(written, byNumber) instanceof Values.Obj window) {
                process.windows.restoreOpen(window);
            }
        }
        process.windows.startFrom(shot.nextWindow(), shot.nextWidget());
        process.windows.restoreEnding(shot.endWithWindows());
        process.listeners.restore(
                value(shot.onMessage(), byNumber) instanceof Values.DelegateValue handler ? handler : null,
                value(shot.onGatewayMessage(), byNumber) instanceof Values.DelegateValue listening ? listening : null,
                shot.gateway());
        return process;
    }

    private Snapshot.IHeld freeze(final Object thing, final int number, final HeldNumbers numbers) {
        final long bytes = this.heap.bytesOf(thing);
        final int line = this.heap.lineOf(thing);
        final boolean freed = this.heap.isFreed(thing);
        if (thing instanceof String text) {
            return new Snapshot.IHeld.Text(number, bytes, line, freed, text);
        }
        if (thing instanceof Values.Obj object) {
            return new Snapshot.IHeld.Object(number, bytes, line, freed, object.type(),
                    fields(object, numbers));
        }
        if (thing instanceof Values.Arr array) {
            return new Snapshot.IHeld.Array(number, bytes, line, freed, array.element(),
                    values(array.all(), numbers));
        }
        if (thing instanceof Values.ListValue list) {
            return new Snapshot.IHeld.Listing(number, bytes, line, freed, values(list.items(), numbers));
        }
        if (thing instanceof Values.MapValue map) {
            return new Snapshot.IHeld.Keyed(number, bytes, line, freed,
                    values(new ArrayList<>(map.entries().keySet()), numbers),
                    values(new ArrayList<>(map.entries().values()), numbers));
        }
        final Values.DelegateValue delegate = (Values.DelegateValue) thing;
        final List<Snapshot.BoundShot> chain = new ArrayList<>();
        for (final Values.Bound bound : delegate.chain()) {
            chain.add(new Snapshot.BoundShot(value(bound.target(), numbers), bound.owner(),
                    bound.method(), bound.parameters(), bound.returns()));
        }
        return new Snapshot.IHeld.Handler(number, bytes, line, freed, delegate.type(), chain);
    }

    private static Object shell(final Snapshot.IHeld written) {
        return switch (written) {
            case Snapshot.IHeld.Text text -> new String(text.value().toCharArray());
            case Snapshot.IHeld.Object object -> new Values.Obj(object.type());
            case Snapshot.IHeld.Array array -> new Values.Arr(array.element(), array.values().size());
            case Snapshot.IHeld.Listing ignored -> new Values.ListValue();
            case Snapshot.IHeld.Keyed ignored -> new Values.MapValue();
            case Snapshot.IHeld.Handler handler -> new Values.DelegateValue(handler.type(), List.of());
        };
    }

    /*
     * A later pass, because two things can point at each other and neither can be filled in until both
     * exist.
     */
    private static void fill(final Snapshot.IHeld written, final Map<Integer, Object> byNumber) {
        final Object thing = byNumber.get(written.id());
        switch (written) {
            case Snapshot.IHeld.Object object -> {
                for (final Map.Entry<String, Snapshot.IValue> field : object.fields().entrySet()) {
                    ((Values.Obj) thing).set(field.getKey(), value(field.getValue(), byNumber));
                }
            }
            case Snapshot.IHeld.Array array -> {
                for (int i = 0; i < array.values().size(); i++) {
                    ((Values.Arr) thing).set(i, value(array.values().get(i), byNumber), 0);
                }
            }
            case Snapshot.IHeld.Listing list -> {
                for (final Snapshot.IValue item : list.items()) {
                    ((Values.ListValue) thing).items().add(value(item, byNumber));
                }
            }
            case Snapshot.IHeld.Keyed keyed -> {
                for (int i = 0; i < keyed.keys().size(); i++) {
                    ((Values.MapValue) thing).entries().put(value(keyed.keys().get(i), byNumber),
                            value(keyed.values().get(i), byNumber));
                }
            }
            default -> { }
        }
    }

    /*
     * The frames are a stack, so they come out top first; they are written bottom first, which is the
     * order they have to be put back in.
     */
    private static Snapshot.ThreadShot freeze(final ProgramThread thread, final HeldNumbers numbers) {
        final List<Frame> stack = new ArrayList<>(thread.frames);
        java.util.Collections.reverse(stack);
        final List<Snapshot.FrameShot> frames = new ArrayList<>();
        for (final Frame frame : stack) {
            frames.add(freeze(frame, numbers));
        }
        return new Snapshot.ThreadShot(thread.id, frames, IWait.kindOf(thread.wait), IWait.untilOf(thread.wait),
                value(IWait.onOf(thread.wait), numbers), value(thread.token, numbers), thread.givenUp(),
                IWait.hostOf(thread.wait));
    }

    private static Snapshot.FrameShot freeze(final Frame frame, final HeldNumbers numbers) {
        return new Snapshot.FrameShot(frame.method.owner(), frame.method.name(),
                frame.method.parameters(), frame.at, value(frame.self, numbers),
                values(java.util.Arrays.asList(frame.slots), numbers), values(frame.stack, numbers),
                frame.discard);
    }

    private static Frame thaw(final ProgramImage program, final Snapshot.FrameShot written,
                              final Map<Integer, Object> byNumber) {
        final MethodImage method = found(program, written);
        if (method == null) {
            return null;
        }
        final Frame frame = new Frame(method, value(written.self(), byNumber));
        for (int i = 0; i < written.slots().size() && i < frame.slots.length; i++) {
            frame.slots[i] = value(written.slots().get(i), byNumber);
        }
        for (final Snapshot.IValue held : written.stack()) {
            frame.push(value(held, byNumber));
        }
        frame.at = written.at();
        frame.discard = written.discard();
        return frame;
    }

    /**
     * The method a frame was in.
     *
     * <p>The one that puts a type's own starting values in place is not among the methods that can be
     * called by name, so it is asked for separately: a process put away before it ran would otherwise
     * come back without it.
     */
    private static MethodImage found(final ProgramImage program, final Snapshot.FrameShot written) {
        final MethodImage named =
                program.method(written.owner(), written.name(), written.parameters());
        if (named != null) {
            return named;
        }
        final TypeImage type = program.type(written.owner());
        if (type == null || type.setUp() == null) {
            return null;
        }
        final MethodImage setUp = type.setUp();
        return setUp.name().equals(written.name()) && setUp.parameters().equals(written.parameters())
                ? setUp : null;
    }

    private static Map<String, Snapshot.IValue> fields(final Values.Obj object, final HeldNumbers numbers) {
        final Map<String, Snapshot.IValue> written = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> field : object.all().entrySet()) {
            written.put(field.getKey(), value(field.getValue(), numbers));
        }
        return written;
    }

    private static List<Snapshot.IValue> values(final List<Object> things, final HeldNumbers numbers) {
        final List<Snapshot.IValue> written = new ArrayList<>();
        for (final Object thing : things) {
            written.add(value(thing, numbers));
        }
        return written;
    }

    private static Snapshot.IValue value(final Object thing, final HeldNumbers numbers) {
        return switch (thing) {
            case null -> new Snapshot.IValue.Nothing();
            case Integer number -> new Snapshot.IValue.I4(number);
            case Long number -> new Snapshot.IValue.I8(number);
            case Float number -> new Snapshot.IValue.R4(number);
            case Double number -> new Snapshot.IValue.R8(number);
            case Boolean flag -> new Snapshot.IValue.Bool(flag);
            case Character letter -> new Snapshot.IValue.Ch(letter);
            default -> {
                final Integer number = numbers.numberOf(thing);
                yield number == null ? new Snapshot.IValue.Nothing() : new Snapshot.IValue.Ref(number);
            }
        };
    }

    private static Object value(final Snapshot.IValue written,
                                          final Map<Integer, Object> byNumber) {
        return switch (written) {
            case Snapshot.IValue.Nothing ignored -> null;
            case Snapshot.IValue.I4 number -> number.value();
            case Snapshot.IValue.I8 number -> number.value();
            case Snapshot.IValue.R4 number -> number.value();
            case Snapshot.IValue.R8 number -> number.value();
            case Snapshot.IValue.Bool flag -> flag.value();
            case Snapshot.IValue.Ch letter -> letter.value();
            case Snapshot.IValue.Ref reference -> byNumber.get(reference.id());
        };
    }

    // one instruction

    private void one() {
        final Frame frame = this.current.frames.peek();
        if (frame.at >= frame.method.length()) {
            this.leave(frame, null);
            return;
        }
        final Instruction instruction = frame.method.instruction(frame.at);
        frame.at++;
        this.run(frame, instruction, frame.at);
    }

    private void run(final Frame frame, final Instruction instruction, final int line) {
        switch (instruction.opcode()) {
            case LDC_I4 -> frame.push(((IOperand.I4) instruction.operand()).value());
            case LDC_I8 -> frame.push(((IOperand.I8) instruction.operand()).value());
            case LDC_R4 -> frame.push(((IOperand.R4) instruction.operand()).value());
            case LDC_R8 -> frame.push(((IOperand.R8) instruction.operand()).value());
            case LDNULL -> frame.push(null);
            case LDSTR -> frame.push(this.text(((IOperand.Text) instruction.operand()).value(), line));
            case LDTHIS -> frame.push(frame.self);
            case LDLOC -> frame.push(frame.slots[((IOperand.Slot) instruction.operand()).index()]);
            case STLOC -> frame.slots[((IOperand.Slot) instruction.operand()).index()] = frame.pop();
            case POP -> frame.pop();
            case COPY -> frame.push(this.copyOf(frame.pop(), line));
            case DUP -> frame.push(frame.peek());
            case LDFLD -> this.loadField(frame, (IOperand.Field) instruction.operand(), line);
            case STFLD -> this.storeField(frame, (IOperand.Field) instruction.operand(), line);
            case LDSFLD -> this.loadStatic(frame, (IOperand.Field) instruction.operand(), line);
            case STSFLD -> this.storeStatic(frame, (IOperand.Field) instruction.operand(), line);
            case ADD, SUB, MUL, DIV, REM, AND, OR, XOR, SHL, SHR ->
                    this.arithmetic(frame, instruction.opcode(), line);
            case NEG -> frame.push(Numbers.negate(frame.pop()));
            case NOT -> frame.push(Numbers.complement(frame.pop()));
            case CONV_I4 -> frame.push(Numbers.toInt(frame.pop()));
            case CONV_I8 -> frame.push(Numbers.toLong(frame.pop()));
            case CONV_R4 -> frame.push(Numbers.toFloat(frame.pop()));
            case CONV_R8 -> frame.push(Numbers.toDouble(frame.pop()));
            case CEQ, CLT, CGT -> this.compare(frame, instruction.opcode());
            case BR -> frame.at = frame.method.jump(line - 1);
            case BRTRUE -> this.jumpIf(frame, line, truth(frame.pop()));
            case BRFALSE -> this.jumpIf(frame, line, !truth(frame.pop()));
            case BEQ, BNE, BLT, BLE, BGT, BGE -> this.jumpCompare(frame, instruction.opcode(), line);
            case NEWOBJ -> this.newObject(frame, frame.method.creation(line - 1), line);
            case NEWARR -> this.newArray(frame, (IOperand.Type) instruction.operand(), line);
            case LDELEM -> this.loadElement(frame, line);
            case STELEM -> this.storeElement(frame, line);
            case LDLEN -> frame.push(this.array(frame.pop(), line).length());
            case DISPOSE -> this.heap.dispose(frame.pop(), line);
            case MONITOR_ENTER -> this.enterMonitor(frame, line);
            case MONITOR_EXIT -> this.exitMonitor(frame, line);
            case CASTCLASS -> this.cast(frame, ((IOperand.Type) instruction.operand()).name(), line);
            case ISINST -> this.isInstance(frame, ((IOperand.Type) instruction.operand()).name());
            case LDFN -> this.handler(frame, (IOperand.Method) instruction.operand(), line);
            case CALL, CALLVIRT -> this.call(frame, frame.method.call(line - 1),
                    instruction.opcode() == Opcode.CALLVIRT, line);
            case SYS -> throw new Halt(Halt.Reason.NO_NETWORK, line,
                    "this computer is not on a network");
            case RET -> this.leave(frame, frame.method.gives() ? frame.pop() : null);
            default -> { }
        }
    }

    /** Branches when {@code go}; {@code line} is where the frame already stands, one past the branch. */
    private void jumpIf(final Frame frame, final int line, final boolean go) {
        if (go) {
            frame.at = frame.method.jump(line - 1);
        }
    }

    private void jumpCompare(final Frame frame, final Opcode opcode, final int line) {
        final Object right = frame.pop();
        final Object left = frame.pop();
        final boolean go = switch (opcode) {
            case BEQ -> same(left, right);
            case BNE -> !same(left, right);
            case BLT -> Numbers.compare(left, right) < 0;
            case BLE -> Numbers.compare(left, right) <= 0;
            case BGT -> Numbers.compare(left, right) > 0;
            default -> Numbers.compare(left, right) >= 0;
        };
        this.jumpIf(frame, line, go);
    }

    private void compare(final Frame frame, final Opcode opcode) {
        final Object right = frame.pop();
        final Object left = frame.pop();
        frame.push(switch (opcode) {
            case CEQ -> this.same(left, right);
            case CLT -> Numbers.compare(left, right) < 0;
            default -> Numbers.compare(left, right) > 0;
        });
    }

    private void arithmetic(final Frame frame, final Opcode opcode, final int line) {
        final Object right = frame.pop();
        final Object left = frame.pop();
        frame.push(Numbers.apply(opcode, left, right, line));
    }

    // fields

    private void loadField(final Frame frame, final IOperand.Field field, final int line) {
        final Object target = this.heap.alive(frame.pop(), line);
        if (target instanceof Values.Obj object) {
            if ("Process".equals(object.type())
                    && ("Running".equals(field.name()) || "ExitCode".equals(field.name()))) {
                // Whether another program still runs is the machine's to say, not a field to go stale.
                frame.push(this.library.programField(object.get("Id"), processHost(object), field.name(), line));
                return;
            }
            final Object held = object.get(field.name());
            // A widget's texts are its own, so the program is handed a copy that stays the program's.
            frame.push(held instanceof String said && UiWidgets.handles(object.type())
                    ? this.heap.text(said, line) : held);
            return;
        }
        frame.push(this.library.read(target, field.name(), line));
    }

    private void storeField(final Frame frame, final IOperand.Field field, final int line) {
        final Object value = frame.pop();
        final Object target = this.heap.alive(frame.pop(), line);
        if (!(target instanceof Values.Obj object)) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no object to write " + field.name() + " on");
        }
        if (UiWidgets.handles(object.type())) {
            // What a window shows is the machine's to draw again, so writing on a widget is paid for.
            this.library.uiWrite(object, field.name(), value, line);
            return;
        }
        object.set(field.name(), value);
    }

    private void loadStatic(final Frame frame, final IOperand.Field field, final int line) {
        if ("Thread".equals(field.owner()) && "Current".equals(field.name())) {
            frame.push(this.tokenFor(this.current, line));
            return;
        }
        if ("Program".equals(field.owner()) && "Args".equals(field.name())) {
            frame.push(this.argsList(line));
            return;
        }
        if ("Program".equals(field.owner()) && "Current".equals(field.name())) {
            frame.push(this.selfToken(line));
            return;
        }
        final TypeImage type = this.program.type(field.owner());
        if (type == null) {
            frame.push(this.library.readStatic(field.owner(), field.name(), line));
            return;
        }
        if (type.kind() == AsmType.Kind.ENUM) {
            frame.push(type.values().get(field.name()));
            return;
        }
        frame.push(this.statics(field.owner()).get(field.name()));
    }

    private void storeStatic(final Frame frame, final IOperand.Field field, final int line) {
        this.statics(field.owner()).set(field.name(), frame.pop());
    }

    private Values.Obj statics(final String owner) {
        return this.statics.computeIfAbsent(owner, Values.Obj::new);
    }

    // objects

    private void newObject(final Frame frame, final ProgramImage.Creation creation, final int line) {
        final List<Object> arguments = this.take(frame, creation.outs());
        frame.push(creation.type() == null
                ? this.library.create(creation.made().owner(), arguments, line)
                : this.instance(creation.type(), creation.constructor(), arguments, line));
    }

    /**
     * A copy of a struct: a new object holding what the old one holds, counted like any other. A value
     * that is not a struct, null included, is handed back as it is, since there is nothing to copy.
     */
    private Object copyOf(final Object value, final int line) {
        if (!(value instanceof Values.Obj original)) {
            return value;
        }
        final TypeImage known = this.program.type(original.type());
        if (known == null || known.kind() != AsmType.Kind.STRUCT) {
            return value;
        }
        final Values.Obj made = new Values.Obj(original.type());
        this.heap.allocate(made, known.instanceSize(), line);
        for (final Map.Entry<String, Object> field : original.all().entrySet()) {
            made.set(field.getKey(), this.copyOf(field.getValue(), line));
        }
        return made;
    }

    private Object instance(final String type, final List<Object> arguments, final int line) {
        final TypeImage known = this.program.type(type);
        if (known == null) {
            return this.library.create(type, arguments, line);
        }
        return this.instance(known, known.constructor(arguments.size()), arguments, line);
    }

    private Values.Obj instance(final TypeImage type, final MethodImage constructor, final List<Object> arguments,
                                final int line) {
        final Values.Obj made = new Values.Obj(type.name());
        this.heap.allocate(made, type.instanceSize(), line);
        if (constructor != null) {
            this.enter(constructor, made, arguments, line);
        }
        return made;
    }

    private void newArray(final Frame frame, final IOperand.Type element, final int line) {
        final int length = Numbers.toInt(frame.pop());
        if (length < 0) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, "an array cannot have " + length + " places");
        }
        final Values.Arr made = new Values.Arr(element.name(), length);
        this.heap.allocate(made, Heap.HEADER + (long) Heap.sizeOf(element.name()) * length, line);
        frame.push(made);
    }

    private void loadElement(final Frame frame, final int line) {
        final int index = Numbers.toInt(frame.pop());
        frame.push(this.array(frame.pop(), line).get(index, line));
    }

    private void storeElement(final Frame frame, final int line) {
        final Object value = frame.pop();
        final int index = Numbers.toInt(frame.pop());
        this.array(frame.pop(), line).set(index, value, line);
    }

    private Values.Arr array(final Object value, final int line) {
        if (this.heap.alive(value, line) instanceof Values.Arr array) {
            return array;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no array here");
    }

    private void cast(final Frame frame, final String type, final int line) {
        final Object value = frame.pop();
        final PrimitiveKind primitive = PrimitiveKind.of(type);
        if (primitive != null) {
            // An object holding a number gives it back as the kind asked for, whichever kind it holds.
            if (value instanceof Number || value instanceof Character) {
                frame.push(primitive.convert(value));
                return;
            }
            if (primitive == PrimitiveKind.BOOL && value instanceof Boolean) {
                frame.push(value);
                return;
            }
            throw new Halt(Halt.Reason.BAD_CAST, line, value == null ? "there is nothing here to make a " + type
                    : "this is not a " + type);
        }
        if (value == null || this.isOfType(value, type)) {
            frame.push(value);
            return;
        }
        throw new Halt(Halt.Reason.BAD_CAST, line, "this is not a " + type);
    }

    private void isInstance(final Frame frame, final String type) {
        final Object value = frame.pop();
        frame.push(value != null && this.isOfType(value, type));
    }

    private boolean isOfType(final Object value, final String type) {
        if (value instanceof Values.Obj object) {
            return this.program.isA(object.type(), type);
        }
        if (value instanceof Values.DelegateValue delegate) {
            return delegate.type().equals(type);
        }
        return switch (type) {
            case "string" -> value instanceof String;
            case "object" -> true;
            case "int" -> value instanceof Integer;
            case "long" -> value instanceof Long;
            case "float" -> value instanceof Float;
            case "double" -> value instanceof Double;
            case "bool" -> value instanceof Boolean;
            case "char" -> value instanceof Character;
            default -> value instanceof Values.Arr array && type.equals(array.element() + "[]")
                    || value instanceof Values.ListValue && type.startsWith("List<")
                    || value instanceof Values.MapValue && type.startsWith("Map<");
        };
    }

    /** The kinds of value a cast can take a number out of an object as. */
    private enum PrimitiveKind {
        INT, LONG, FLOAT, DOUBLE, CHAR, BOOL;

        static PrimitiveKind of(final String written) {
            return switch (written) {
                case "int" -> INT;
                case "long" -> LONG;
                case "float" -> FLOAT;
                case "double" -> DOUBLE;
                case "char" -> CHAR;
                case "bool" -> BOOL;
                default -> null;
            };
        }

        Object convert(final Object value) {
            return switch (this) {
                case INT -> Numbers.toInt(value);
                case LONG -> Numbers.toLong(value);
                case FLOAT -> Numbers.toFloat(value);
                case DOUBLE -> Numbers.toDouble(value);
                case CHAR -> (char) Numbers.toInt(value);
                case BOOL -> value;
            };
        }
    }

    // calls

    private void handler(final Frame frame, final IOperand.Method method, final int line) {
        final Object target = frame.pop();
        final Values.Bound bound = new Values.Bound(target, method.owner(), method.name(),
                method.parameters(), method.returns());
        final Values.DelegateValue made = new Values.DelegateValue(method.owner(), List.of(bound));
        this.heap.allocate(made, made.bytes(), line);
        frame.push(made);
    }

    /**
     * Puts a question to a computer on the other side of a Gateway and waits for its answer.
     *
     * <p>Nothing comes off the stack while the question is out: the instruction is rewound, so when the
     * answer lands the call simply runs again, finds it, and takes its arguments off then. That is what
     * makes the wait free (a parked thread is given no budget) and what makes it survive a save.
     */
    private void call(final Frame frame, final ProgramImage.CallSite site, final boolean through, final int line) {
        final IOperand.Method named = site.named();
        if (through) {
            this.invoke(frame, this.take(frame, site.outs()), line);
            return;
        }
        final MethodImage direct = site.direct();
        if (direct == null) {
            if (site.intrinsic() != null) {
                this.answer(frame, site, line);
                return;
            }
            if ("Thread".equals(named.owner())) {
                this.threadCall(frame, named, line);
                return;
            }
            if ("Program".equals(named.owner()) && this.programCall(frame, named, line)) {
                return;
            }
            if ("Process".equals(named.owner())) {
                this.processCall(frame, named, line);
                return;
            }
            if (Library.readsLine(named) && !this.input.has()) {
                /*
                 * Nothing has been typed: the call is put back so it is asked again once a line comes,
                 * and the thread waits without spending anything. The read takes nothing off the
                 * stack, which is what makes asking it again the same as asking it once.
                 */
                frame.at--;
                this.park();
                return;
            }
            final List<Object> arguments = this.take(frame, site.outs());
            final Object self = this.library.takesTarget(named.owner(), named.name())
                    ? this.heap.alive(frame.pop(), line) : null;
            this.push(frame, named, this.library.call(named, self, arguments, line));
            return;
        }
        final List<Object> arguments = this.take(frame, site.outs());
        final Object self = direct.isStatic() ? null : this.heap.alive(frame.pop(), line);
        this.enter(this.onItsOwnType(site, self), self, arguments, line);
    }

    /**
     * Answers a call the system takes in Java: the arguments come off the stack, then the object the call is made on
     * when it is made on one, and the answer goes back on followed by whatever the call filled in.
     */
    private void answer(final Frame frame, final ProgramImage.CallSite site, final int line) {
        final IntrinsicSpec intrinsic = site.intrinsic();
        final boolean[] outs = site.outs();
        final Object[] arguments = takeArray(frame, outs);
        final Object target = intrinsic.onTarget() ? this.heap.alive(frame.pop(), line) : null;
        final Object answer = intrinsic.function().call(this.heap, target, arguments, line);
        if (site.gives()) {
            frame.push(answer);
        }
        for (int i = 0; i < outs.length; i++) {
            if (outs[i]) {
                frame.push(arguments[i] == null ? site.defaults()[i] : arguments[i]);
            }
        }
    }

    /*
     * A call through an interface names the interface, but the object knows which class it is, and
     * that is the one whose lines should run.
     */
    private MethodImage onItsOwnType(final ProgramImage.CallSite site, final Object self) {
        // A constructor runs on the type it names: a class chaining to its base must not land in one of its own.
        if (site.constructs() || !(self instanceof Values.Obj object) || object.type().equals(site.named().owner())) {
            return site.direct();
        }
        final TypeImage own = this.program.type(object.type());
        final MethodImage found = own == null ? null : own.method(site.signature());
        return found != null && found.hasCode() ? found : site.direct();
    }

    /**
     * Calls what a delegate holds.
     *
     * <p>A delegate can hold a run of handlers, and all of them are called, in the order they were
     * joined. They are stacked up rather than run one after another on the spot, so a run of a
     * hundred handlers costs the budget the same as a hundred calls written out and cannot take the
     * tick away from anything else. Only the last of them leaves an answer, which is what the caller
     * is waiting for.
     */
    private void invoke(final Frame frame, final List<Object> arguments, final int line) {
        final Object value = this.heap.alive(frame.pop(), line);
        if (!(value instanceof Values.DelegateValue delegate) || delegate.chain().isEmpty()) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no handler to call");
        }
        final List<Values.Bound> chain = delegate.chain();
        for (int i = chain.size() - 1; i >= 0; i--) {
            final Values.Bound bound = chain.get(i);
            final MethodImage method =
                    this.program.method(bound.owner(), bound.method(), bound.parameters());
            if (method == null || !method.hasCode()) {
                if (i == chain.size() - 1) {
                    throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line,
                            "there is no " + bound.method() + " to call");
                }
                continue;
            }
            final Frame made = new Frame(method, bound.target());
            fill(made, arguments);
            made.discard = i < chain.size() - 1;
            this.pushCall(made, line);
        }
    }

    private void enter(final MethodImage method, final Object self, final List<Object> arguments,
                       final int line) {
        if (!method.hasCode()) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, method.describe() + " has no body to run");
        }
        final Frame frame = new Frame(method, self);
        fill(frame, arguments);
        this.pushCall(frame, line);
    }

    /**
     * Starts a call the running thread makes, unless its calls already go as deep as a thread's may.
     *
     * <p>A method that calls itself without end would otherwise keep adding calls, each held in the server's
     * memory, for as long as the program runs; no program that ends is anywhere near this deep.
     */
    private void pushCall(final Frame frame, final int line) {
        if (this.current.frames.size() >= DEEPEST) {
            throw new Halt(Halt.Reason.STACK_DEPTH, line, "calls went " + DEEPEST + " deep calling "
                    + frame.method.describe() + ": a method may be calling itself without end");
        }
        this.current.frames.push(frame);
    }

    private static void fill(final Frame frame, final List<Object> arguments) {
        for (int i = 0; i < arguments.size() && i < frame.slots.length; i++) {
            frame.slots[i] = arguments.get(i);
        }
    }

    /**
     * Leaves a method, putting back what it gives and then what it filled in, the last of those on
     * top, which is the order the caller stores them in.
     */
    private void leave(final Frame frame, final Object answer) {
        final Deque<Frame> frames = this.current.frames;
        frames.pop();
        if (frames.isEmpty()) {
            return;
        }
        if (frame.discard) {
            return;
        }
        final Frame caller = frames.peek();
        if (frame.method.gives()) {
            caller.push(answer);
        }
        for (int i = 0; i < frame.method.parameters().size(); i++) {
            if (frame.method.fillsIn(i)) {
                caller.push(frame.slots[i]);
            }
        }
    }

    private void push(final Frame frame, final IOperand.Method named, final Library.Answer answer) {
        if (!"void".equals(named.returns())) {
            frame.push(answer.value());
        }
        for (final Object filled : answer.filled()) {
            frame.push(filled);
        }
    }

    /**
     * Takes the arguments off the stack. They were pushed in order, so they come off backwards, and
     * they may be null, which is why the list is one that allows it.
     *
     * <p>A place the method fills in was never pushed: the caller hands over somewhere to write, not
     * a value, so that place is left empty here and holds what the method put there when it returns.
     */
    private List<Object> take(final Frame frame, final List<String> parameters) {
        final List<Object> taken = new ArrayList<>(java.util.Collections.nCopies(parameters.size(), null));
        for (int i = parameters.size() - 1; i >= 0; i--) {
            if (parameters.get(i).startsWith("out ")) {
                continue;
            }
            taken.set(i, frame.pop());
        }
        return taken;
    }

    /** The same, for a call whose shape was worked out when the program loaded. */
    private List<Object> take(final Frame frame, final boolean[] outs) {
        return java.util.Arrays.asList(takeArray(frame, outs));
    }

    private static Object[] takeArray(final Frame frame, final boolean[] outs) {
        final Object[] taken = new Object[outs.length];
        for (int i = outs.length - 1; i >= 0; i--) {
            if (!outs[i]) {
                taken[i] = frame.pop();
            }
        }
        return taken;
    }

    // odds and ends

    private String text(final String value, final int line) {
        return this.heap.text(value, line);
    }

    /**
     * Whether a value counts as true where a branch asks.
     *
     * <p>The assembly has no constant for a bool: true and false are written as a one and a zero, so
     * a number stands for a bool wherever one was meant, and zero is the false one.
     */
    private static boolean truth(final Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof Number number) {
            return number.longValue() != 0;
        }
        return value != null;
    }

    /*
     * Two values are the same when they say the same thing, which for a bool and the number that
     * stands for it means comparing what they both mean rather than what they are. Two structs or two
     * records are the same when everything they hold is, field by field, however far down that goes.
     */
    private boolean same(final Object left, final Object right) {
        if (left == null || right == null) {
            return left == right;
        }
        if (left instanceof Boolean || right instanceof Boolean) {
            return truth(left) == truth(right);
        }
        if (left instanceof String || right instanceof String) {
            return left.equals(right);
        }
        if (left instanceof Values.Obj one && right instanceof Values.Obj other && one.type().equals(other.type())) {
            final TypeImage kind = this.program.type(one.type());
            if (kind != null && kind.kind().byValue()) {
                final Map<String, Object> mine = one.all();
                final Map<String, Object> theirs = other.all();
                if (!mine.keySet().equals(theirs.keySet())) {
                    return false;
                }
                for (final Map.Entry<String, Object> field : mine.entrySet()) {
                    if (!this.same(field.getValue(), theirs.get(field.getKey()))) {
                        return false;
                    }
                }
                return true;
            }
        }
        if (left instanceof Number && right instanceof Number) {
            return Numbers.compare(left, right) == 0;
        }
        return left.equals(right);
    }
}
