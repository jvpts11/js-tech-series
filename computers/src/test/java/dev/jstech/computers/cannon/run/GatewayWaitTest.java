/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Waiting for a computer on the other side of a Gateway.
 *
 * <p>A program that asks one of them to do something stops where it stands until the answer comes:
 * it spends nothing while it waits, it carries on with the answer when it lands, and it gives up by
 * itself when nothing ever comes, which is what a computer that was turned off looks like from here.
 */
class GatewayWaitTest {

    private static final long ROOM = 1024L * 1024;
    private static final int PLENTY = 100_000;

    /** A machine that answers for a Gateway and whose clock the test moves by hand. */
    private static final class Fake implements IHost {

        private long now;
        private final List<String> asked = new ArrayList<>();
        private int question;

        @Override
        public long tick() {
            return this.now;
        }

        @Override
        public long dayTime() {
            return this.now;
        }

        @Override
        public long day() {
            return 0;
        }

        @Override
        public boolean provides(final String owner) {
            return "Gateway".equals(owner);
        }

        @Override
        public Reply call(final String owner, final String member, final List<Object> arguments,
                          final String caller, final int line) {
            if ("HasAgent".equals(member)) {
                return Reply.of(Boolean.TRUE, 1);
            }
            // Every question is taken and answered by its number, which is what a Gateway does.
            this.asked.add(member + " " + arguments);
            this.question++;
            return Reply.of(this.question, 1);
        }
    }

    private static final String ASKER = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Network.*;
            namespace Plant;
            class Asker {
                static void Main() {
                    Console.PrintLine("before");
                    Console.PrintLine("read " + Gateway.Read(9, "/startup"));
                    Console.PrintLine("after");
                }
            }
            """;

    private static Process started(final Fake host) {
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("asker.can", ASKER)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final DiagnosticBag bag = new DiagnosticBag("asker.asm");
        final AsmProgram read = new AsmReader(built.assembly(), bag).read();
        assertFalse(bag.hasErrors(), () -> String.join("\n",
                bag.sorted().stream().map(Diagnostic::format).toList()));
        final Loaded program = Loaded.of(read);
        final Process process = new Process(program, ROOM, host);
        process.beginStatic(program.entryPoint(), "Main");
        return process;
    }

    @Test
    void asking_stopsTheProgramUntilTheAnswerComes() {
        final Fake host = new Fake();
        final Process process = started(host);
        process.step(PLENTY);
        assertEquals(List.of("before"), process.console(), "it stops at the question");
        assertEquals(Process.State.PARKED, process.state(), "and is a program that is waiting, not one that ended");
        assertEquals(1, host.asked.size(), "the question was put once");
        assertEquals(0, process.step(PLENTY), "and waiting costs nothing");

        process.answered(1, "shell");
        process.step(PLENTY);
        assertEquals(List.of("before", "read shell", "after"), process.console(), "it carries on with it");
        assertEquals(Process.State.FINISHED, process.state());
        assertEquals(1, host.asked.size(), "and never asked twice");
    }

    @Test
    void asking_givesUpWhenNothingEverAnswers() {
        final Fake host = new Fake();
        final Process process = started(host);
        process.step(PLENTY);
        for (int tick = 0; tick < 99; tick++) {
            host.now++;
            assertEquals(0, process.step(PLENTY), "it is still waiting, still for nothing");
        }
        host.now += 2;
        process.step(PLENTY);
        assertEquals(List.of("before", "read ", "after"), process.console(),
                "the wait ends by itself and the answer is nothing");
        assertEquals(Process.State.FINISHED, process.state());
    }

    @Test
    void asking_ignoresAnAnswerToSomethingElse() {
        final Fake host = new Fake();
        final Process process = started(host);
        process.step(PLENTY);
        assertFalse(process.answered(77, "not for us"), "no one was waiting for that number");
        assertEquals(List.of("before"), process.console(), "and the program is where it was");
        assertTrue(process.answered(1, "ours"), "the one it is waiting for lands");
    }
}
