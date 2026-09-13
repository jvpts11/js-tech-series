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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.List;
import org.junit.jupiter.api.Test;

class CallbackQueueTest {

    private static final String SOURCE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Machine.*; using System.Network.*; using System.Operations.*; "
            + "using System.Execution.*; namespace Tests; class Monitor : IScript {\n"
            + "    public void OnInit() { }\n"
            + "    public void OnTick() { }\n"
            + "    public void OnDestroy() { }\n}\n";

    private static ProgramImage program() {
        final CannonCompiler.Result built = CannonCompiler.compile(List.of(new SourceFile("Monitor.can", SOURCE)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram listing = reader.read();
        assertFalse(reader.hasProblems(), () -> String.join("\n",
                reader.problems().stream().map(ListingProblem::format).toList()));
        return ProgramImage.of(listing);
    }

    private static Process.Frame call(final ProgramImage program, final String method) {
        return new Process.Frame(program.method(program.entryPoint(), method, List.of()), null);
    }

    @Test
    void poll_givesTheCallsInTheOrderTheyCame() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        final Process.Frame init = call(program, "OnInit");
        final Process.Frame tick = call(program, "OnTick");
        queue.add(init);
        queue.add(tick);

        assertSame(init, queue.poll());
        assertSame(tick, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    void holds_saysWhetherACallOfThatMethodIsWaiting() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        queue.add(call(program, "OnTick"));

        assertTrue(queue.holds("OnTick"));
        assertFalse(queue.holds("OnDestroy"));
    }

    @Test
    void clear_leavesNothingWaiting() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        queue.add(call(program, "OnInit"));
        queue.add(call(program, "OnTick"));
        assertEquals(2, queue.size());

        queue.clear();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }
}
