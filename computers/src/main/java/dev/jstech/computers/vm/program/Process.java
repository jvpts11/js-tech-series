/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Shape;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
    private static final int START_COST = dev.jstech.computers.vm.system.SigmaCosts.THREAD_START;

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
            return Process.this.locks.held(target);
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
    /** The locks the threads hold, and the threads waiting for each. */
    private final MonitorTable locks = new MonitorTable();
    private final CallbackQueue waiting = new CallbackQueue();
    /** An object's fields and the fields each type keeps for itself. */
    private final FieldAccess fieldAccess;
    /** Calls to the program's methods, to the system and through delegates, and coming back from them. */
    private final CallDispatch calls;
    /** The program's objects, arrays and struct copies. */
    private final ObjectMaking objects;
    /** What each instruction does, carried out on the thread whose turn it is. */
    private final InstructionExecutor executor;
    /** What the world says to the program, turned into calls waiting their turn. */
    private final ProgramEvents events;
    private Values.Obj script;
    private final ProgramIdentity identity = new ProgramIdentity();
    /** The machine around the program, told once when the program has ended for good. */
    private final IHost host;
    /** Whether the host has been told the program ended, so it is told once. */
    private boolean told;
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

    ThreadScheduler scheduler() {
        return this.scheduler;
    }

    MonitorTable locks() {
        return this.locks;
    }

    CallbackQueue callbacks() {
        return this.waiting;
    }

    FieldAccess fieldAccess() {
        return this.fieldAccess;
    }

    ProgramWatches watches() {
        return this.watches;
    }

    ProgramListeners listeners() {
        return this.listeners;
    }

    ProgramWindows windows0() {
        return this.windows;
    }

    ProgramIdentity identity() {
        return this.identity;
    }

    ProgramInput input() {
        return this.input;
    }

    ProgramEvents events() {
        return this.events;
    }

    /** Puts back the object the script runs on, for a process read out of a save. */
    void restoreScript(final Values.Obj restored) {
        this.script = restored;
    }

    Values.Obj staticsOf(final String owner) {
        return this.fieldAccess.statics(owner);
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

    Process(final ProgramImage program, final long heapBytes, final IHost host, final boolean fresh) {
        this.program = program;
        this.host = host;
        this.heap = new Heap(heapBytes);
        this.library = new Library(this.heap, host, program.entryPoint());
        this.library.serves(this);
        this.fieldAccess = new FieldAccess(this, this.heap, this.library, program);
        this.calls = new CallDispatch(this, this.heap, this.library, program);
        this.objects = new ObjectMaking(this.heap, this.library, program, this.calls);
        this.executor = new InstructionExecutor(program, this.heap, this.scheduler, this.locks, this.fieldAccess,
                this.calls, this.objects);
        this.events = new ProgramEvents(this);
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
    public void hearGateway(final Values.DelegateValue handler) {
        this.listeners.hearGateway(handler);
    }

    /** Hands the program what a ComputerCraft computer said through a Gateway; false when it was not heard. */
    public boolean deliverGatewayMessage(final int from, final String text, final long tick) {
        return this.events.deliverGatewayMessage(from, text, tick);
    }

    /** Tells the process that another program has ended, so whatever of it waits on that program runs again. */
    public void programEnded(final int program) {
        this.scheduler.programEnded(program);
    }

    /** Whether the program ended itself with {@code Program.Exit}. */
    public boolean exited() {
        return this.identity.exited();
    }

    /** How the program ended: what it said with {@code Program.Exit}, one for a halt, zero otherwise. */
    public int exitCode() {
        return this.identity.exitCode();
    }

    /** Hands the process a line from another program; false when the program is over or has no room for it. */
    public boolean deliverMessage(final int from, final String text, final long tick) {
        return this.events.deliverMessage(from, text, tick);
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
    Values.Obj selfToken(final int line) {
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
    Values.ListValue argsList(final int line) {
        final Values.ListValue made = new Values.ListValue();
        for (final String arg : this.identity.args()) {
            made.items().add(this.text(arg, line));
        }
        this.heap.allocate(made, made.bytes(), line);
        return made;
    }

    /** The calls on {@code Program} the process answers itself: the ones about this very program. */
    boolean programCall(final Frame frame, final IOperand.Method named, final int line) {
        switch (named.name()) {
            case "Exit" -> {
                final List<Object> arguments = CallDispatch.take(frame, named.parameters());
                this.exit(arguments.isEmpty() ? 0 : Numbers.toInt(arguments.getFirst()));
                return true;
            }
            case "OnMessage" -> {
                final List<Object> arguments = CallDispatch.take(frame, named.parameters());
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
    void processCall(final Frame frame, final IOperand.Method named, final int line) {
        switch (named.name()) {
            case "Wait" -> this.waitFor(frame, named, line);
            case "Send" -> {
                final List<Object> arguments = CallDispatch.take(frame, named.parameters());
                CallDispatch.push(frame, named, this.library.call(new IOperand.Method("Program", "Send",
                        named.parameters(), named.returns()), null, arguments, line));
            }
            case "Kill", "Output" -> {
                CallDispatch.take(frame, named.parameters());
                final Object token = frame.pop();
                final Integer id = this.processId(token, line);
                CallDispatch.push(frame, named, this.library.call(new IOperand.Method("Program", named.name(),
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
    static String processHost(final Object token) {
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
            CallDispatch.take(frame, named.parameters());
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
        final Object made = this.objects.instance(type, List.of(), 0);
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
        if (!this.told && this.endedForGood()) {
            this.told = true;
            this.host.programEnded(this.machineId());
        }
        return used;
    }

    /**
     * Whether the program is over for good: halted, exited, or a program that runs at a terminal whose main thread has
     * returned with nothing left waiting. A script between its turns is not; it is asked again next tick.
     */
    private boolean endedForGood() {
        return this.identity.over() || (this.program.shape() != Shape.SCRIPT && this.main.frames.isEmpty()
                && this.waiting.isEmpty());
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
                    this.executor.one(thread);
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
     * Puts a handler in the queue, to run on the main thread when it next has nothing else to do; false when the event
     * found no room and was dropped.
     */
    public boolean post(final Values.DelegateValue handler, final List<Object> arguments) {
        return this.events.post(handler, arguments);
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

    /** Tells the program what a player did to one of its widgets; false when no part of the program heard it. */
    public boolean deliverUiEvent(final long window, final long widget, final String kind,
                                  final List<Object> values) {
        return this.events.deliverUiEvent(window, widget, kind, values);
    }

    // threads

    /** The thread of that number, or null once it is over. */
    private ProgramThread thread(final Object id) {
        return this.scheduler.thread(id);
    }

    /**
     * What the program holds a thread by, made the first time it is asked for.
     *
     * <p>{@code Running} is kept true to life on the object itself, so reading it is reading a field
     * like any other and costs what that costs.
     */
    Values.Obj tokenFor(final ProgramThread thread, final int line) {
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
    Values.Obj spawn(final Object body, final int line) {
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
        this.locks.releaseAll(thread.id);
    }

    /** Puts the running thread to sleep for that many ticks; asking for none is asking for nothing. */
    void sleep(final long ticks) {
        if (ticks > 0) {
            this.scheduler.await(this.current, new IWait.Sleep(this.library.now() + ticks));
        }
    }

    /** Gives up the rest of the running thread's turn, so another thread has one sooner. */
    void yieldTurn() {
        this.current.yielded = true;
    }

    /** Stops the thread a program holds: stopping the main one ends the program's run, another one simply ends. */
    void stop(final Object token, final int line) {
        final ProgramThread target = this.thread(this.threadId(token, line));
        if (target == this.main) {
            this.main.frames.clear();
            this.waiting.clear();
            this.ended(this.main);
        } else if (target != null) {
            this.end(target);
        }
    }

    private Integer threadId(final Object token, final int line) {
        if (this.heap.alive(token, line) instanceof Values.Obj object && object.get("Id") instanceof Integer id) {
            return id;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no thread here");
    }

    /**
     * Whether a join has to wait for another thread to end, setting up what wakes it when it does.
     *
     * <p>Nothing is taken off the stack until the wait is over, so the call can be asked again once the thread has
     * ended or the time given has run out, and asking it again is the same as asking it once. It does not wait for a
     * thread that is over or is the one asking, nor once its time has run out or when it was given none.
     */
    boolean joinWaits(final Frame frame, final int count, final int line) {
        final Object token = frame.stack.size() > count ? frame.stack.get(frame.stack.size() - 1 - count) : null;
        final ProgramThread target = this.thread(this.threadId(token, line));
        final long ticks = count == 1 ? Numbers.toLong(frame.peek()) : 0L;
        // Read before the test, so it is forgotten whenever the call answers, as it always was.
        final boolean gaveUp = this.current.takeGaveUp();
        if (target == null || target == this.current || gaveUp || (count == 1 && ticks <= 0)) {
            return false;
        }
        this.scheduler.await(this.current, new IWait.Join(target.id, ticks > 0 ? this.library.now() + ticks : 0L));
        return true;
    }

    /** Whether the thread a join asked about is over or is the one asking, which is what a join given time answers. */
    boolean joinOver(final Object token, final int line) {
        final ProgramThread target = this.thread(this.threadId(token, line));
        return target == null || target == this.current;
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
        this.events.deliver(totals);
    }

    /** The program's method of that name bound to {@code self}, as a handler, or null when its type has none. */
    public Values.DelegateValue handlerFor(final Values.Obj self, final String name) {
        return this.events.handlerFor(self, name);
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

    void halt(final Halt halt) {
        this.identity.halt(halt.getMessage());
        this.library.write(halt.getMessage());
        for (final ProgramThread thread : this.scheduler.threads()) {
            thread.frames.clear();
        }
        this.locks.clear();
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
        return ProcessSnapshotWriter.write(this);
    }

    /** Reads a process back out of what {@link #save()} wrote, ready to carry on where it stopped. */
    public static Process restore(final ProgramImage program, final Snapshot shot, final IHost host) {
        return ProcessSnapshotReader.read(program, shot, host);
    }

    // odds and ends

    private String text(final String value, final int line) {
        return this.heap.text(value, line);
    }
}
