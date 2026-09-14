/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CallDispatchTest {

    private static final String SOURCE = "using System.*; namespace Tests; class Monitor : IScript {\n"
            + "    public void OnInit() { }\n"
            + "    public void OnTick() { }\n"
            + "    public void OnDestroy() { }\n"
            + "    public int Add(int a, int b) { return a + b; }\n"
            + "    public bool Find(out int value) { value = 42; return true; }\n"
            + "}\n";

    private static final ProgramImage PROGRAM = load();

    private static ProgramImage load() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Monitor.sgs", SOURCE)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram listing = reader.read();
        assertFalse(reader.hasProblems(), () -> String.join("\n",
                reader.problems().stream().map(ListingProblem::format).toList()));
        return ProgramImage.of(listing);
    }

    private static MethodImage method(final String name, final List<String> parameters) {
        final MethodImage found = PROGRAM.method(PROGRAM.entryPoint(), name, parameters);
        assertNotNull(found, name);
        return found;
    }

    private static Process process() {
        return new Process(PROGRAM, 64L * 1024, IHost.still());
    }

    private static CallDispatch dispatch(final Process process) {
        return new CallDispatch(process, process.heap0(), process.library(), PROGRAM);
    }

    @Test
    void take_skipsThePlacesACallFillsInAndKeepsTheOrder() {
        final Frame written = new Frame(method("OnTick", List.of()), null);
        written.push(1);
        written.push(2);
        final Frame loaded = new Frame(method("OnTick", List.of()), null);
        loaded.push(1);
        loaded.push(2);

        assertEquals(Arrays.asList(1, null, 2), CallDispatch.take(written, List.of("int", "out int", "int")));
        assertEquals(Arrays.asList(1, null, 2), CallDispatch.take(loaded, new boolean[]{false, true, false}));
    }

    @Test
    void push_putsBackTheAnswerThenWhatWasFilledIn() {
        final Frame frame = new Frame(method("OnTick", List.of()), null);

        CallDispatch.push(frame, new IOperand.Method("Console", "Find", List.of("out int"), "bool"),
                new Library.Answer(true, List.of(7)));

        assertEquals(7, frame.pop());
        assertEquals(true, frame.pop());
    }

    @Test
    void push_putsBackNoAnswerForACallThatGivesNothing() {
        final Frame frame = new Frame(method("OnTick", List.of()), null);

        CallDispatch.push(frame, new IOperand.Method("Console", "PrintLine", List.of("string"), "void"),
                Library.Answer.of(99));

        assertTrue(frame.stack.isEmpty());
    }

    @Test
    void enter_startsTheMethodOnTheRunningThreadWithItsArguments() {
        final Process process = process();
        final CallDispatch calls = dispatch(process);
        final MethodImage add = method("Add", List.of("int", "int"));

        calls.enter(add, null, List.of(3, 4), 1);

        final Frame top = process.mainThread().frames.peek();
        assertSame(add, top.method);
        assertEquals(3, top.slots[0]);
        assertEquals(4, top.slots[1]);
    }

    @Test
    void leave_handsTheAnswerAndWhatWasFilledInToTheCaller() {
        final Process process = process();
        final CallDispatch calls = dispatch(process);
        final Frame caller = new Frame(method("OnTick", List.of()), null);
        process.mainThread().frames.push(caller);
        calls.enter(method("Find", List.of("out int")), null, Arrays.asList((Object) null), 1);
        final Frame called = process.mainThread().frames.peek();
        called.slots[0] = 42;

        calls.leave(called, true);

        assertSame(caller, process.mainThread().frames.peek());
        assertEquals(42, caller.pop());
        assertEquals(true, caller.pop());
    }

    @Test
    void enter_haltsOnceAThreadsCallsGoAsDeepAsTheyMay() {
        final Process process = process();
        final CallDispatch calls = dispatch(process);
        final MethodImage tick = method("OnTick", List.of());
        for (int i = 0; i < CallDispatch.DEEPEST; i++) {
            calls.enter(tick, null, List.of(), 1);
        }

        final Halt halt = assertThrows(Halt.class, () -> calls.enter(tick, null, List.of(), 7));

        assertEquals(Halt.Reason.STACK_DEPTH, halt.reason());
        assertEquals(CallDispatch.DEEPEST, process.mainThread().frames.size());
    }
}
