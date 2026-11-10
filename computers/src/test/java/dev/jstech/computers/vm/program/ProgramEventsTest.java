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

class ProgramEventsTest {

    private static final String SOURCE = "using System.*; namespace Tests; class Monitor : IScript {\n"
            + "    public void OnInit() { }\n"
            + "    public void OnTick() { }\n"
            + "    public void OnDestroy() { }\n"
            + "    public void Hear(object said) { }\n"
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

    private static Process process() {
        return new Process(PROGRAM, 64L * 1024, IHost.still());
    }

    @Test
    void handlerFor_bindsTheMethodOfThatNameToTheObject() {
        final Process process = process();
        final Values.Obj self = process.create(PROGRAM.entryPoint());

        final Values.DelegateValue hear = process.events().handlerFor(self, "Hear");

        assertEquals(1, hear.chain().size());
        assertSame(self, hear.chain().getFirst().target());
        assertEquals("Hear", hear.chain().getFirst().method());
        assertNull(process.events().handlerFor(self, "Missing"));
    }

    @Test
    void deliverMessage_queuesACallOnlyForAProgramThatGaveAHandler() {
        final Process process = process();
        final Values.Obj self = process.create(PROGRAM.entryPoint());
        final ProgramEvents events = process.events();
        final int before = process.waiting();

        assertTrue(events.deliverMessage(4, "hi", 20L),
                "a program with no handler takes the message and hears nothing");
        assertEquals(before, process.waiting());

        process.listeners().hearMessages(events.handlerFor(self, "Hear"));
        assertTrue(events.deliverMessage(4, "hi", 20L));
        assertEquals(before + 1, process.waiting());
    }

    @Test
    void deliverMessage_toAProgramTooFullToHoldItIsTurnedAwayNotThrown() {
        // Built outside any instruction, the message ran the heap out and stopped the tick of the machine running it.
        final Process process = process();
        final Values.Obj self = process.create(PROGRAM.entryPoint());
        final ProgramEvents events = process.events();
        process.listeners().hearMessages(events.handlerFor(self, "Hear"));
        process.heap0().allocate(new Object(), process.heap0().free() - 1, 0);
        final int before = process.waiting();

        assertFalse(events.deliverMessage(4, "a message longer than the one byte left", 20L));
        assertEquals(before, process.waiting());
    }

    @Test
    void deliverSettled_queuesTheOneWaitingOnItOnceAndTheOneWaitingOnAllEachTime() {
        final Process process = process();
        final Values.Obj self = process.create(PROGRAM.entryPoint());
        final ProgramEvents events = process.events();
        final Values.DelegateValue hear = events.handlerFor(self, "Hear");
        process.listeners().hearSettled("A1B2", hear);
        process.listeners().hearSettled(hear);
        final int before = process.waiting();

        assertTrue(events.deliverSettled(settled("a1b2")));
        assertEquals(before + 2, process.waiting(), "the one waiting on it, and the one waiting on all");
        assertTrue(events.deliverSettled(settled("a1b2")));
        assertEquals(before + 3, process.waiting(), "the one waiting on it was told once");
    }

    @Test
    void deliverNetworkEvent_queuesACallOnlyForAProgramThatGaveAHandler() {
        final Process process = process();
        final Values.Obj self = process.create(PROGRAM.entryPoint());
        final ProgramEvents events = process.events();
        final int before = process.waiting();

        assertTrue(events.deliverNetworkEvent("warn", "UPS-1", "on battery", 40L));
        assertEquals(before, process.waiting());
        assertFalse(process.listensToNetwork());

        process.listeners().hearEvents(events.handlerFor(self, "Hear"));
        assertTrue(process.listensToNetwork());
        assertTrue(events.deliverNetworkEvent("warn", "UPS-1", "on battery", 40L));
        assertEquals(before + 1, process.waiting());
    }

    @Test
    void networkHandlers_hearWhatHappenedAndTellTheMachineTheyWait() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Watcher.sgs",
                "using System.*; using System.Network.*; using System.Operations.*; namespace Tests;\n"
                        + "class Watcher : IScript {\n"
                        + "    public void OnInit() {\n"
                        + "        Network.OnEvent(e => Console.PrintLine(e.Level + \" \" + e.Machine + \" \""
                        + " + e.Text + \" \" + e.Tick));\n"
                        + "        Operations.OnSettled(o => Console.PrintLine(o.Id + \" \" + o.Status + \" \""
                        + " + o.Moved));\n"
                        + "    }\n"
                        + "    public void OnTick() { }\n"
                        + "    public void OnDestroy() { }\n"
                        + "}\n")));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final ProgramImage program = ProgramImage.of(new AsmReader(built.assembly()).read());
        final int[] told = new int[1];
        final Process process = new Process(program, 64L * 1024, new IHost() {
            @Override
            public long tick() {
                return 0;
            }

            @Override
            public long dayTime() {
                return 0;
            }

            @Override
            public long day() {
                return 0;
            }

            @Override
            public void listensToNetwork() {
                told[0]++;
            }
        });
        final Values.Obj script = process.create(program.entryPoint());
        process.begin(script, "OnInit");
        process.step(100_000);
        assertTrue(told[0] > 0, "the machine is told the program waits on its network");

        process.deliverNetworkEvent("warn", "UPS-1", "on battery", 40L);
        process.deliverSettled(settled("a1b2"));
        process.step(100_000);
        assertEquals(List.of("warn UPS-1 on battery 40", "a1b2 done 64"), process.console());
    }

    private static SettledOperation settled(final String id) {
        return new SettledOperation(id, "craft", "minecraft:oak_planks", 64, 64, "done", "medium");
    }

    @Test
    void deliverUiEvent_refusesAWindowTheProgramDoesNotHave() {
        assertFalse(process().events().deliverUiEvent(99L, 1L, "click", List.of()));
    }

    @Test
    void post_dropsAndCountsACallThatFindsNoRoom() {
        final Process process = process();
        final Values.Obj self = process.create(PROGRAM.entryPoint());
        final Values.DelegateValue hear = process.events().handlerFor(self, "Hear");
        for (int i = 0; i <= CallbackQueue.MOST_CALLS; i++) {
            process.events().post(hear, List.of(i));
        }

        assertFalse(process.events().post(hear, List.of(-1)), "the queue is full");
        assertTrue(process.droppedEvents() >= 2, "every call turned away is counted");
    }
}
