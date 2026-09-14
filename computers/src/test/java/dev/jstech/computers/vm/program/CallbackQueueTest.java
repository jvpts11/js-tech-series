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

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
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
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Monitor.sgs", SOURCE)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram listing = reader.read();
        assertFalse(reader.hasProblems(), () -> String.join("\n",
                reader.problems().stream().map(ListingProblem::format).toList()));
        return ProgramImage.of(listing);
    }

    private static Frame call(final ProgramImage program, final String method) {
        return new Frame(program.method(program.entryPoint(), method, List.of()), null);
    }

    @Test
    void poll_givesTheCallsInTheOrderTheyCame() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        final Frame init = call(program, "OnInit");
        final Frame tick = call(program, "OnTick");
        queue.add(init, 0);
        queue.add(tick, 0);

        assertSame(init, queue.poll());
        assertSame(tick, queue.poll());
        assertNull(queue.poll());
    }

    @Test
    void holds_saysWhetherACallOfThatMethodIsWaiting() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        queue.add(call(program, "OnTick"), 0);

        assertTrue(queue.holds("OnTick"));
        assertFalse(queue.holds("OnDestroy"));
    }

    @Test
    void clear_leavesNothingWaiting() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        queue.add(call(program, "OnInit"), 40);
        queue.add(call(program, "OnTick"), 40);
        assertEquals(2, queue.size());

        queue.clear();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertEquals(0, queue.bytes());
    }

    @Test
    void fits_turnsAwayACallPastTheMostThatMayWait() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        for (int i = 0; i < CallbackQueue.MOST_CALLS; i++) {
            assertTrue(queue.fits(1, 0), "call " + i + " still fits");
            queue.add(call(program, "OnTick"), 0);
        }

        assertFalse(queue.fits(1, 0));
        assertTrue(queue.fits(0, 0), "nothing more always fits");
    }

    @Test
    void fits_turnsAwayArgumentsPastTheMostBytes() {
        final CallbackQueue queue = new CallbackQueue();
        queue.add(call(program(), "OnTick"), CallbackQueue.MOST_BYTES - 10);

        assertTrue(queue.fits(1, 10));
        assertFalse(queue.fits(1, 11));
    }

    @Test
    void poll_givesBackWhatTheCallTakenHeld() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        queue.add(call(program, "OnInit"), 100);
        queue.add(call(program, "OnTick"), 30);

        queue.poll();

        assertEquals(30, queue.bytes());
    }

    @Test
    void addFirst_putsTheCallAheadOfEverythingWaiting() {
        final ProgramImage program = program();
        final CallbackQueue queue = new CallbackQueue();
        final Frame tick = call(program, "OnTick");
        final Frame farewell = call(program, "OnDestroy");
        queue.add(tick, 0);

        queue.addFirst(farewell, 0);

        assertSame(farewell, queue.poll());
        assertSame(tick, queue.poll());
    }

    @Test
    void drop_countsWhatWasLetGoAndClearingDoesNotForgetIt() {
        final CallbackQueue queue = new CallbackQueue();
        queue.drop();
        queue.drop();

        queue.clear();

        assertEquals(2, queue.dropped());
        queue.startFrom(7);
        assertEquals(7, queue.dropped());
    }
}
