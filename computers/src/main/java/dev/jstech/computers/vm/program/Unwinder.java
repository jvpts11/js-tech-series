/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exceptions as a process carries them out (computers/docs/design/sigma.md): one thrown, the protected stretches of
 * the calls searched for what answers it, call by call outwards, the finally blocks on the way run, and a way out of a
 * protected stretch running the finally blocks it leaves.
 *
 * <p>Nothing is searched twice. What a running finally goes on with when it ends is kept in its call, and the search
 * takes up from the stretch after it. A process stopped politely is unwound the same way by something no catch takes,
 * so its finally blocks run and nothing else does.
 */
@TextHolder
final class Unwinder {

    private final Process process;
    private final ProgramImage program;
    private final Heap heap;
    /** What the player was told of each exception a halt became, for the console when nothing catches it. */
    private final Map<Object, Text> told = new IdentityHashMap<>();

    /** What throwing costs, and what it costs more for each call it leaves. */
    static final int THROW_COST = 50;
    static final int CALL_COST = 5;
    /** The most calls an exception's trace keeps. */
    static final int MOST_TRACE = 16;
    /** The type every exception stands on, as a listing names it. */
    static final String EXCEPTION = "Exception";
    /** What unwinds the threads of a process being stopped: no catch takes it, so only finally blocks run. */
    static final Object STOPPING = new Object();
    static final String MESSAGE = "Message";
    static final String FILE = "File";
    static final String LINE = "Line";
    static final String TRACE = "Trace";

    private static final TextKey NOTHING_TO_THROW = TextKey.of("jsc.vm.unwinder.nothing_to_throw",
            "there is nothing here to throw");
    private static final TextKey NOT_CAUGHT = TextKey.of("jsc.vm.unwinder.not_caught",
            "an exception of the type %s was thrown, and nothing caught it");
    private static final TextKey AT = TextKey.of("jsc.vm.unwinder.at", "   at %s");

    /** What a finally running now in a call goes on with when it ends. */
    sealed interface Pending permits Leaving, Throwing {

        /** The handler in the call's list whose finally is running. */
        int running();
    }

    /** A way out to the line {@code target}, with the finally blocks on its way, the one at {@code next} now. */
    record Leaving(int target, List<Integer> finallies, int next) implements Pending {

        Leaving {
            finallies = List.copyOf(finallies);
        }

        @Override
        public int running() {
            return this.finallies.get(this.next);
        }
    }

    /** An exception thrown from the line {@code at}, whose search takes up again after the finally running now. */
    record Throwing(Object exception, int at, int running) implements Pending {
    }

    Unwinder(final Process process, final ProgramImage program, final Heap heap) {
        this.process = process;
        this.program = program;
        this.heap = heap;
    }

    /** Whether the program carries the language's exceptions: their types are in its listing. */
    boolean carried() {
        return this.program.type(EXCEPTION) != null;
    }

    /**
     * Throws in place of a halt, when the program carries exceptions and the halt is one that throws; false when it is
     * not, and the process halts as it always has.
     */
    boolean caught(final ProgramThread thread, final Halt halt) {
        if (halt.thrown() == Halt.Thrown.NONE || thread.frames.isEmpty() || !this.carried()) {
            return false;
        }
        final TypeImage type = this.program.type(halt.thrown().type());
        if (type == null) {
            return false;
        }
        final Values.Obj made = this.heap.allocate(new Values.Obj(type.name()), type.instanceSize(), halt.line());
        made.set(MESSAGE, this.heap.text(halt.getMessage(), halt.line()));
        this.told.put(made, halt.text());
        this.raise(thread, made, true, halt.line());
        return true;
    }

    /**
     * Throws what the call on top of the thread hands over: marked where it was thrown when it is thrown fresh, left as
     * it was when it is thrown on from a catch. Nothing at all is a null reference, as in C#.
     */
    void raise(final ProgramThread thread, final Object thrown, final boolean fresh, final int line) {
        if (thrown == null) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, NOTHING_TO_THROW.text());
        }
        this.process.charge(THROW_COST);
        if (fresh && thrown instanceof Values.Obj exception) {
            this.stamp(thread, exception, line);
        }
        this.unwind(thread, thrown, thread.frames.peek().at - 1, 0);
    }

    /**
     * Leaves the protected stretches around the line {@code at} for the line {@code target}, running on the way the
     * finally blocks of every one it leaves, the innermost first.
     */
    void leave(final Frame frame, final int at, final int target) {
        final List<Integer> finallies = new ArrayList<>();
        for (int i = 0; i < frame.method.handlerCount(); i++) {
            final MethodImage.Handler handler = frame.method.handler(i);
            if (handler.cleanup() && handler.covers(at) && !handler.covers(target)) {
                finallies.add(i);
            }
        }
        frame.stack.clear();
        if (finallies.isEmpty()) {
            frame.at = target;
            return;
        }
        pend(frame, new Leaving(target, finallies, 0));
        frame.at = frame.method.handler(finallies.getFirst()).handler();
    }

    /** Ends a finally: what it was run for goes on, the next finally of a way out or the search of an exception. */
    void endFinally(final ProgramThread thread, final Frame frame) {
        if (frame.pendings == null || frame.pendings.isEmpty()) {
            return;
        }
        final Pending pending = frame.pendings.removeLast();
        switch (pending) {
            case Leaving leaving -> {
                final int next = leaving.next() + 1;
                if (next < leaving.finallies().size()) {
                    pend(frame, new Leaving(leaving.target(), leaving.finallies(), next));
                    frame.at = frame.method.handler(leaving.finallies().get(next)).handler();
                } else {
                    frame.at = leaving.target();
                }
            }
            case Throwing throwing -> this.unwind(thread, throwing.exception(), throwing.at(), throwing.running() + 1);
        }
    }

    /**
     * Unwinds a thread of a process being stopped politely: every finally block it is inside runs, the innermost first,
     * and no catch takes the unwinding, so the thread ends once they have. Whatever it was waiting for is let go.
     */
    void stop(final ProgramThread thread) {
        if (thread.frames.isEmpty()) {
            return;
        }
        thread.wait = IWait.NONE;
        this.unwind(thread, STOPPING, thread.frames.peek().at - 1, 0);
    }

    /*
     * Searches the thread's calls for what answers the exception, from the line {@code at} of the top call and the
     * handler {@code from} of its list: a catch that takes it, or a finally on the way, which runs and takes the search
     * up again when it ends. A call where nothing answers is left, at the cost of a call more.
     */
    private void unwind(final ProgramThread thread, final Object exception, final int at, final int from) {
        int line = at;
        int start = from;
        while (!thread.frames.isEmpty()) {
            final Frame frame = thread.frames.peek();
            for (int i = start; i < frame.method.handlerCount(); i++) {
                final MethodImage.Handler handler = frame.method.handler(i);
                if (!handler.covers(line) || !(handler.cleanup() || this.takes(handler, exception))) {
                    continue;
                }
                abandon(frame, handler);
                frame.stack.clear();
                if (handler.cleanup()) {
                    pend(frame, new Throwing(exception, line, i));
                } else {
                    frame.push(exception);
                }
                frame.at = handler.handler();
                return;
            }
            thread.frames.pop().releaseBlocks(this.process.memory());
            this.process.charge(CALL_COST);
            final Frame caller = thread.frames.peek();
            // A caller stands one past the call it is waiting on; one not started yet stands nowhere a stretch covers.
            line = caller == null ? -1 : caller.at - 1;
            start = 0;
        }
        this.uncaught(exception);
    }

    /** Whether a catch takes that exception: one of its type, or of a type standing on it. */
    private boolean takes(final MethodImage.Handler handler, final Object exception) {
        return exception instanceof Values.Obj object && this.program.isA(object.type(), handler.type());
    }

    /* An exception nothing caught stops the program, saying what it was and the calls it was thrown through. */
    private void uncaught(final Object exception) {
        if (exception == STOPPING || !(exception instanceof Values.Obj object)) {
            return;
        }
        final Object message = object.get(MESSAGE);
        final Text said = this.told.getOrDefault(object, message == null ? NOT_CAUGHT.with(object.type())
                : Text.literal(String.valueOf(message)));
        final List<Text> trace = new ArrayList<>();
        if (object.get(TRACE) instanceof Values.Arr calls) {
            for (int i = 0; i < calls.length(); i++) {
                trace.add(AT.with(String.valueOf(calls.get(i, 0))));
            }
        }
        this.process.uncaught(said, trace);
    }

    /* Marks where an exception was thrown: the file and the line, and the calls it was thrown through. */
    private void stamp(final ProgramThread thread, final Values.Obj exception, final int line) {
        final Frame top = thread.frames.peek();
        exception.set(FILE, this.heap.text(this.fileOf(top), line));
        exception.set(LINE, top.method.positionOf(top.at - 1));
        final List<String> calls = new ArrayList<>();
        for (final Frame frame : thread.frames) {
            if (calls.size() == MOST_TRACE) {
                break;
            }
            final int position = frame.method.positionOf(frame.at - 1);
            final String file = this.fileOf(frame);
            final String place = frame.method.owner() + "." + frame.method.name();
            calls.add(position > 0 && !file.isEmpty() ? place + " (" + file + ":" + position + ")" : place);
        }
        final Values.Arr trace = this.heap.allocate(new Values.Arr("string", calls.size()),
                Heap.HEADER + (long) Heap.sizeOf("string") * calls.size(), line);
        for (int i = 0; i < calls.size(); i++) {
            trace.set(i, this.heap.text(calls.get(i), line), line);
        }
        exception.set(TRACE, trace);
    }

    /** The file of the source the call's method was written in, or empty when the listing does not say. */
    private String fileOf(final Frame frame) {
        final TypeImage owner = this.program.type(frame.method.owner());
        return owner == null ? "" : owner.source();
    }

    /* Keeps what a finally starting now goes on with when it ends. */
    private static void pend(final Frame frame, final Pending pending) {
        if (frame.pendings == null) {
            frame.pendings = new ArrayList<>();
        }
        frame.pendings.add(pending);
    }

    /*
     * Lets go of what the finally blocks running inside the stretch about to answer were run for: an exception thrown
     * out of a finally takes the place of whatever it was running for, as in C#.
     */
    private static void abandon(final Frame frame, final MethodImage.Handler answering) {
        while (frame.pendings != null && !frame.pendings.isEmpty()) {
            final MethodImage.Handler running = frame.method.handler(frame.pendings.getLast().running());
            if (running.handler() < answering.start() || running.through() > answering.end()) {
                return;
            }
            frame.pendings.removeLast();
        }
    }
}
