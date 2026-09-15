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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstructionExecutorTest {

    private static final String SOURCE = "using System.*; namespace Tests; class Monitor : IScript {\n"
            + "    public void OnInit() { }\n"
            + "    public void OnTick() { }\n"
            + "    public void OnDestroy() { }\n"
            + "    public int Add(int a, int b) { return a + b; }\n"
            + "    public int Larger(int a, int b) { if (a > b) { return a; } return b; }\n"
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

    /** A process, and an executor made of parts that work on it, with locks and a scheduler of their own. */
    private record Parts(Process process, CallDispatch calls, MonitorTable locks, ThreadScheduler scheduler,
                         InstructionExecutor executor) {
    }

    private static Parts parts() {
        final Process process = new Process(PROGRAM, 64L * 1024, IHost.still());
        final CallDispatch calls = new CallDispatch(process, process.heap0(), process.library(), PROGRAM);
        final FieldAccess fields = new FieldAccess(process, process.heap0(), process.library(), PROGRAM);
        final ObjectMaking objects = new ObjectMaking(process, process.heap0(), process.library(), PROGRAM, calls);
        final MonitorTable locks = new MonitorTable();
        final ThreadScheduler scheduler = new ThreadScheduler();
        return new Parts(process, calls, locks, scheduler,
                new InstructionExecutor(PROGRAM, process.heap0(), scheduler, locks, fields, calls, objects));
    }

    /** Runs the main thread until the frame it began in is on top again. */
    private static void runBackTo(final Parts parts, final Frame caller) {
        final ProgramThread main = parts.process().mainThread();
        for (int i = 0; main.frames.peek() != caller; i++) {
            assertTrue(i < 1_000, "the method never came back");
            parts.executor().one(main);
        }
    }

    private static Frame frame() {
        return new Frame(method("OnTick", List.of()), null);
    }

    @Test
    void one_runsAMethodThroughToItsAnswer() {
        final Parts parts = parts();
        final Frame caller = frame();
        parts.process().mainThread().frames.push(caller);
        parts.calls().enter(method("Add", List.of("int", "int")), null, List.of(3, 4), 1);

        runBackTo(parts, caller);

        assertEquals(7, caller.pop());
    }

    @Test
    void one_takesTheBranchTheComparisonChooses() {
        final Parts parts = parts();
        final Frame caller = frame();
        parts.process().mainThread().frames.push(caller);

        parts.calls().enter(method("Larger", List.of("int", "int")), null, List.of(2, 9), 1);
        runBackTo(parts, caller);
        parts.calls().enter(method("Larger", List.of("int", "int")), null, List.of(9, 2), 1);
        runBackTo(parts, caller);

        assertEquals(9, caller.pop());
        assertEquals(9, caller.pop());
    }

    @Test
    void one_leavesAMethodThatHasRunOffItsEnd() {
        final Parts parts = parts();
        final ProgramThread main = parts.process().mainThread();
        final Frame caller = frame();
        main.frames.push(caller);
        parts.calls().enter(method("OnTick", List.of()), null, List.of(), 1);
        final Frame called = main.frames.peek();
        called.at = called.method.length();

        parts.executor().one(main);

        assertSame(caller, main.frames.peek());
        assertTrue(caller.stack.isEmpty(), "a method that gives nothing hands nothing back");
    }

    @Test
    void run_monitorEnterTakesAFreeLockAndTheObjectOffTheStack() {
        final Parts parts = parts();
        final ProgramThread thread = parts.scheduler().main();
        final Frame frame = frame();
        final Values.Obj target = new Values.Obj(PROGRAM.entryPoint());
        frame.push(target);
        frame.at = 1;

        parts.executor().run(thread, frame, Instruction.of(Opcode.MONITOR_ENTER), 1);

        assertTrue(frame.stack.isEmpty());
        assertTrue(parts.locks().held(target));
        assertFalse(thread.waiting());
    }

    @Test
    void run_monitorEnterWaitsOnALockAnotherThreadHoldsAndLeavesTheObjectWhereItIs() {
        final Parts parts = parts();
        final ProgramThread holder = parts.scheduler().main();
        final ProgramThread asker = parts.scheduler().start();
        final Frame frame = frame();
        final Values.Obj target = new Values.Obj(PROGRAM.entryPoint());
        assertTrue(parts.locks().enter(holder, target));
        frame.push(target);
        frame.at = 5;

        parts.executor().run(asker, frame, Instruction.of(Opcode.MONITOR_ENTER), 5);

        assertSame(target, frame.peek());
        assertEquals(4, frame.at, "it asks again from the same instruction");
        assertInstanceOf(IWait.Lock.class, asker.wait);
    }

    @Test
    void run_monitorExitHaltsOnALockTheThreadDoesNotHold() {
        final Parts parts = parts();
        final ProgramThread thread = parts.scheduler().main();
        final Frame frame = frame();
        frame.push(new Values.Obj(PROGRAM.entryPoint()));

        final Halt halt = assertThrows(Halt.class,
                () -> parts.executor().run(thread, frame, Instruction.of(Opcode.MONITOR_EXIT), 3));

        assertEquals(Halt.Reason.NOT_LOCKED, halt.reason());
        assertEquals("this thread is letting go of a lock it does not hold", halt.getMessage());
    }
}
