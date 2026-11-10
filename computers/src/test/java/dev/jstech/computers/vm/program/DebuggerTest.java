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
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.AsmWriter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DebuggerTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** A program like the one the debugger is shown with, every line numbered as the source has it. */
    private static final String PLANT = String.join("\n",
            "using System.*; namespace Plant;",
            "class Plant {",
            "    static int Twice(int n) {",
            "        int doubled = n * 2;",
            "        return doubled;",
            "    }",
            "    static void Main() {",
            "        long logs = 41;",
            "        Console.PrintLine(\"Logs: \" + logs);",
            "        if (logs < 64) {",
            "            int more = Twice(21);",
            "            Console.PrintLine(\"\" + more);",
            "        }",
            "    }",
            "}");

    @Test
    void build_forTheDebuggerWritesColumnsAndNamesAndReadsThemBack() {
        final String listing = built(true);
        assertTrue(listing.contains(".local param slot 0 n int"), listing);
        assertTrue(listing.contains(".local slot 0 logs long"), listing);
        assertTrue(listing.contains(".line 10 9"), listing);
        assertTrue(listing.startsWith(".asm " + AsmProgram.VERSION), listing);
        final AsmProgram read = new AsmReader(listing).read();
        assertEquals(listing, AsmWriter.write(read));
        final AsmMethod main = read.type("Plant.Plant").methods().stream()
                .filter(method -> method.name().equals("Main")).findFirst().orElseThrow();
        assertEquals(List.of("logs", "more"), main.debug().locals().stream().map(AsmMethod.Local::name).toList());
    }

    @Test
    void build_withoutTheDebuggerWritesNoneOfIt() {
        final String listing = built(false);
        assertFalse(listing.contains(".local"), listing);
        assertFalse(listing.contains(".line"), listing);
        assertFalse(Debugger.hasLines(debugged(false).program()));
        assertTrue(Debugger.hasLines(debugged(true).program()));
    }

    @Test
    void breakpoint_stopsBeforeItsLineAndShowsWhereAndTheLocals() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        assertEquals(new Debugger.Breakpoint("Plant.sgs", 10), debugger.breakAt("Plant.sgs", 10).orElseThrow());
        process.step(PLENTY);
        assertTrue(debugger.paused());
        assertEquals(Debugger.Reason.BREAKPOINT, debugger.stop());
        assertEquals(List.of("Logs: 41"), process.console());
        assertEquals(new Debugger.Place("Plant.Main", "Plant.sgs", 10, 9, List.of()), debugger.stack().getFirst());
        assertTrue(debugger.locals(0).contains(new Debugger.Variable("logs", "long", "41")), debugger.locals(0)
                .toString());
    }

    @Test
    void stopped_spendsNothingAndNothingRuns() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        debugger.breakAt("Plant.sgs", 10);
        process.step(PLENTY);
        final long spent = process.spent();
        assertEquals(0, process.step(PLENTY));
        assertEquals(spent, process.spent());
        assertEquals(List.of("Logs: 41"), process.console());
    }

    @Test
    void stepInto_entersTheCallWithItsArgumentsAndStepOutComesBack() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        debugger.breakAt("Plant.sgs", 11);
        process.step(PLENTY);
        debugger.stepInto();
        process.step(PLENTY);
        assertEquals(Debugger.Reason.STEP, debugger.stop());
        final List<Debugger.Place> stack = debugger.stack();
        assertEquals(2, stack.size());
        assertEquals(new Debugger.Place("Plant.Twice", "Plant.sgs", 4, 9,
                List.of(new Debugger.Variable("n", "int", "21"))), stack.getFirst());
        assertEquals(11, stack.get(1).line());
        debugger.stepOut();
        process.step(PLENTY);
        assertEquals("Plant.Main", debugger.stack().getFirst().method());
        assertEquals(1, debugger.stack().size());
    }

    @Test
    void stepOver_runsTheCallThroughAndStopsAtTheNextLine() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        debugger.breakAt("Plant.sgs", 11);
        process.step(PLENTY);
        debugger.stepOver();
        process.step(PLENTY);
        assertEquals(12, debugger.stack().getFirst().line());
        assertTrue(debugger.locals(0).contains(new Debugger.Variable("more", "int", "42")));
    }

    @Test
    void breakpoint_onALineWithNoCodeLandsOnTheNextOneThatHasSome() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        assertEquals(8, debugger.breakAt("Plant.sgs", 7).orElseThrow().line());
        assertEquals(Optional.empty(), debugger.breakAt("Plant.sgs", 40));
        assertEquals(Optional.empty(), debugger.breakAt("Other.sgs", 8));
        assertEquals(new Debugger.Breakpoint("Plant.sgs", 4),
                Debugger.landing(process.program(), "plant.SGS", 3).orElseThrow());
        assertEquals("Plant.sgs", Debugger.startFile(process.program()));
    }

    @Test
    void delete_takesTheBreakpointAwaySoTheProgramRunsThrough() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        final Debugger.Breakpoint at = debugger.breakAt("Plant.sgs", 10).orElseThrow();
        assertTrue(debugger.delete(at));
        assertFalse(debugger.delete(at));
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state());
    }

    @Test
    void find_looksInTheCallersWhenTheCallStoppedInHasNoSuchName() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        debugger.breakAt("Plant.sgs", 4);
        process.step(PLENTY);
        assertEquals(Optional.of(new Debugger.Variable("logs", "long", "41")), debugger.find("logs", 0));
        assertEquals(Optional.of(new Debugger.Variable("n", "int", "21")), debugger.find("n", 0));
        assertEquals(Optional.empty(), debugger.find("nothing", 0));
    }

    @Test
    void resume_runsToTheEndAndTheCreditsAddUp() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        debugger.breakAt("Plant.sgs", 9);
        process.step(PLENTY);
        assertTrue(debugger.paused());
        debugger.deleteAll();
        debugger.resume();
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state());
        assertEquals(List.of("Logs: 41", "42"), process.console());
        final long byCalls = debugger.calls().values().stream().mapToLong(Long::longValue).sum();
        assertEquals(process.spent(), byCalls + debugger.steps());
    }

    @Test
    void interrupt_stopsAtOnceAndResumeGoesOn() {
        final Process process = debugged(true);
        final Debugger debugger = process.debug();
        debugger.interrupt();
        assertEquals(Debugger.Reason.INTERRUPTED, debugger.stop());
        assertEquals(0, process.step(PLENTY));
        assertNotNull(debugger.stack());
        debugger.resume();
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state());
    }

    private static String built(final boolean forDebugger) {
        final SigmaCompiler.Result result = SigmaCompiler.compile(List.of(new SourceFile("Plant.sgs", PLANT)),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, SigmaVersions.NEWEST,
                forDebugger ? List.of(SigmaVersions.DEBUG) : List.of());
        assertTrue(result.ok(), () -> String.join("\n", result.lines()));
        return result.assembly();
    }

    private static Process debugged(final boolean forDebugger) {
        final AsmReader reader = new AsmReader(built(forDebugger));
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems(), () -> reader.problems().toString());
        final Process process = new Process(program, ROOM, IHost.still());
        process.beginStatic(program.entryPoint(), "Main");
        return process;
    }
}
