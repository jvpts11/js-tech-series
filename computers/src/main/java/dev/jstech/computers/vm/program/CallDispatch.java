/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.system.IntrinsicSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Making calls: to a method of the program, to one the system answers in Java, through a delegate, or to one the
 * process or the machine answers; and coming back from them with what they give.
 *
 * <p>A call to one of the program's methods is a new frame on the running thread, never a Java call, so a program
 * that calls deep costs frames, not the server's stack, and a thread's calls stop at {@link #DEEPEST}.
 */
final class CallDispatch {

    /** The most calls one thread may have in progress at once; one call more halts the program. */
    static final int DEEPEST = 1_024;

    private final Process process;
    private final Heap heap;
    private final Library library;
    private final ProgramImage program;

    CallDispatch(final Process process, final Heap heap, final Library library, final ProgramImage program) {
        this.process = process;
        this.heap = heap;
        this.library = library;
        this.program = program;
    }

    /** Makes a delegate of the method named, bound to the object on top of the stack. */
    void handler(final Frame frame, final IOperand.Method method, final int line) {
        final Object target = frame.pop();
        final Values.Bound bound = new Values.Bound(target, method.owner(), method.name(),
                method.parameters(), method.returns());
        final Values.DelegateValue made = new Values.DelegateValue(method.owner(), List.of(bound));
        this.heap.allocate(made, made.bytes(), line);
        frame.push(made);
    }

    /**
     * Makes the call the line names.
     *
     * <p>A call that puts a question to a computer on the other side of a Gateway takes nothing off the stack while the
     * question is out: the instruction is rewound, so when the answer lands the call simply runs again, finds it, and
     * takes its arguments off then. That is what makes the wait free (a parked thread is given no budget) and what
     * makes it survive a save.
     */
    void call(final Frame frame, final ProgramImage.CallSite site, final boolean through, final int line) {
        final IOperand.Method named = site.named();
        if (through) {
            this.invoke(frame, take(frame, site.outs()), line);
            return;
        }
        final MethodImage direct = site.direct();
        if (direct == null) {
            if (site.intrinsic() != null) {
                this.answer(frame, site, line);
                return;
            }
            if (site.handled() != null) {
                this.handle(frame, site, line);
                return;
            }
            if ("Process".equals(named.owner())) {
                this.process.processCall(frame, named, line);
                return;
            }
            final List<Object> arguments = take(frame, site.outs());
            final Object self = this.library.takesTarget(named.owner(), named.name())
                    ? this.heap.alive(frame.pop(), line) : null;
            push(frame, named, this.library.call(named, self, arguments, line));
            return;
        }
        final List<Object> arguments = take(frame, site.outs());
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

    /**
     * Answers a call the program's own process takes: the arguments come off the stack as a function's do, then the
     * object the call is made on when it is made on one.
     */
    private void handle(final Frame frame, final ProgramImage.CallSite site, final int line) {
        final ProcessCalls.Binding handled = site.handled();
        final boolean[] outs = site.outs();
        if (handled.waiting() != null && handled.waiting().waits(this.process, frame, outs.length, line)) {
            /*
             * The call cannot be answered yet (nothing has been typed, the thread it joins still runs): it is put back
             * so it is asked again once what it waits for comes, and the thread waits without spending anything. It
             * took nothing off the stack, which is what makes asking it again the same as asking it once.
             */
            frame.at--;
            return;
        }
        final Object[] arguments = takeArray(frame, outs);
        final Object target = handled.onTarget() ? this.heap.alive(frame.pop(), line) : null;
        final Object answer = handled.function().call(this.process, target, arguments, line);
        if (site.gives()) {
            frame.push(answer);
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

    /** Starts a method of the program on the running thread, with those arguments in its first slots. */
    void enter(final MethodImage method, final Object self, final List<Object> arguments, final int line) {
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
        final Deque<Frame> frames = this.process.current().frames;
        if (frames.size() >= DEEPEST) {
            throw new Halt(Halt.Reason.STACK_DEPTH, line, "calls went " + DEEPEST + " deep calling "
                    + frame.method.describe() + ": a method may be calling itself without end");
        }
        frames.push(frame);
    }

    /**
     * Leaves a method, putting back what it gives and then what it filled in, the last of those on
     * top, which is the order the caller stores them in.
     */
    void leave(final Frame frame, final Object answer) {
        final Deque<Frame> frames = this.process.current().frames;
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

    /** Puts back what a call the machine answered gives, then what it filled in. */
    static void push(final Frame frame, final IOperand.Method named, final Library.Answer answer) {
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
    static List<Object> take(final Frame frame, final List<String> parameters) {
        final List<Object> taken = new ArrayList<>(Collections.nCopies(parameters.size(), null));
        for (int i = parameters.size() - 1; i >= 0; i--) {
            if (parameters.get(i).startsWith("out ")) {
                continue;
            }
            taken.set(i, frame.pop());
        }
        return taken;
    }

    /** The same, for a call whose shape was worked out when the program loaded. */
    static List<Object> take(final Frame frame, final boolean[] outs) {
        return Arrays.asList(takeArray(frame, outs));
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

    /** Puts the arguments in the frame's first slots, as a call hands them over. */
    static void fill(final Frame frame, final List<Object> arguments) {
        for (int i = 0; i < arguments.size() && i < frame.slots.length; i++) {
            frame.slots[i] = arguments.get(i);
        }
    }
}
