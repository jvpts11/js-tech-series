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
