/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; namespace Tests; ";

    @Test
    void catch_takesWhatIsThrownAndOfTheTypesStandingOnIt() {
        assertEquals(List.of("caught: full", "io: missing", "after"), run("""
                class DiskFull : IOException { public DiskFull(string message) : base(message) { } }
                class Tool {
                    static void Main() {
                        try {
                            throw new Exception("full");
                        } catch (Exception failed) {
                            Console.PrintLine("caught: " + failed.Message);
                        }
                        try {
                            throw new DiskFull("missing");
                        } catch (FormatException wrong) {
                            Console.PrintLine("not this one");
                        } catch (IOException failed) {
                            Console.PrintLine("io: " + failed.Message);
                        }
                        Console.PrintLine("after");
                    }
                }
                """));
    }

    @Test
    void finally_runsOnEveryWayOut() {
        assertEquals(List.of("body", "finally 1", "finally 2", "2", "finally 3", "loop done", "finally 4", "caught"),
                run("""
                class Tool {
                    static int Given() {
                        try { return 2; } finally { Console.PrintLine("finally 2"); }
                    }
                    static void Main() {
                        try { Console.PrintLine("body"); } finally { Console.PrintLine("finally 1"); }
                        Console.PrintLine("" + Given());
                        for (int i = 0; i < 5; i++) {
                            try { break; } finally { Console.PrintLine("finally 3"); }
                        }
                        Console.PrintLine("loop done");
                        try {
                            try { throw new Exception("x"); } finally { Console.PrintLine("finally 4"); }
                        } catch { Console.PrintLine("caught"); }
                    }
                }
                """));
    }

    @Test
    void exception_unwindsThroughTheCallsAndTheirFinallyBlocks() {
        assertEquals(List.of("inner cleanup", "outer cleanup", "Main caught deep"), run("""
                class Tool {
                    static void Deep() { throw new InvalidOperationException("deep"); }
                    static void Inner() { try { Deep(); } finally { Console.PrintLine("inner cleanup"); } }
                    static void Outer() { try { Inner(); } finally { Console.PrintLine("outer cleanup"); } }
                    static void Main() {
                        try { Outer(); } catch (InvalidOperationException failed) {
                            Console.PrintLine("Main caught " + failed.Message);
                        }
                    }
                }
                """));
    }

    @Test
    void throw_bareThrowsOnWhatTheCatchTook() {
        assertEquals(List.of("first", "again: kept"), run("""
                class Tool {
                    static void Main() {
                        try {
                            try { throw new Exception("kept"); } catch (Exception failed) {
                                Console.PrintLine("first");
                                failed = null;
                                throw;
                            }
                        } catch (Exception again) {
                            Console.PrintLine("again: " + again.Message);
                        }
                    }
                }
                """));
    }

    @Test
    void halt_isThrownAsItsExceptionInAProgramThatCatches() {
        assertEquals(List.of("null", "zero", "index", "format", "done"), run("""
                class Box { public int Count; }
                class Tool {
                    static void Main() {
                        Box nothing = null;
                        try { int read = nothing.Count; }
                        catch (NullReferenceException failed) { Console.PrintLine("null"); }
                        int zero = 0;
                        try { int half = 4 / zero; } catch (DivideByZeroException failed) { Console.PrintLine("zero"); }
                        int[] held = new int[2];
                        try { held[5] = 1; } catch (IndexOutOfRangeException failed) { Console.PrintLine("index"); }
                        try { int read = Convert.ToInt("seven"); }
                        catch (FormatException failed) { Console.PrintLine("format"); }
                        Console.PrintLine("done");
                    }
                }
                """));
    }

    @Test
    void exception_saysWhereItWasThrown() {
        assertEquals(List.of("Tool.sgs 3", "true"), run("""
                class Tool {
                    static void Fail() {
                        throw new Exception("here");
                    }
                    static void Main() {
                        try { Fail(); } catch (Exception failed) {
                            Console.PrintLine(failed.File + " " + failed.Line);
                            Console.PrintLine("" + failed.Trace[0].StartsWith("Tests.Tool.Fail"));
                        }
                    }
                }
                """));
    }

    @Test
    void uncaught_stopsTheProgramWithItsMessageAndTrace() {
        final Process process = started("""
                class Tool {
                    static void Fail() { throw new Exception("nobody caught this"); }
                    static void Main() { try { Fail(); } finally { Console.PrintLine("cleaned"); } }
                }
                """);
        process.step(PLENTY);
        assertEquals(Process.State.HALTED, process.state());
        final List<String> said = process.console();
        assertEquals("cleaned", said.getFirst());
        assertTrue(said.contains("nobody caught this"), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("at Tests.Tool.Fail")), said::toString);
    }

    @Test
    void lock_isLetGoWhenAnExceptionLeavesIt() {
        assertEquals(List.of("caught", "taken again"), run("""
                class Tool {
                    static object guard = new object();
                    static void Main() {
                        try { lock (guard) { throw new Exception("inside"); } } catch { Console.PrintLine("caught"); }
                        lock (guard) { Console.PrintLine("taken again"); }
                    }
                }
                """));
    }

    @Test
    void stopping_runsTheFinallyBlocksAndNothingElse() {
        final Process process = started("""
                class Tool {
                    static void Main() {
                        try {
                            while (true) { }
                        } catch { Console.PrintLine("not a catch"); } finally { Console.PrintLine("cleaned up"); }
                    }
                }
                """);
        process.step(500);
        assertTrue(process.stopping());
        process.step(PLENTY);
        assertEquals(List.of("cleaned up"), process.console());
    }

    @Test
    void stopping_doesNothingToAProgramWithoutExceptions() {
        final Process process = started("class Tool { static void Main() { while (true) { } } }");
        process.step(500);
        assertFalse(process.stopping());
    }

    @Test
    void throw_costsFiftyAndFiveForEachCallItLeaves() {
        final int near = this.used("try { throw new Exception(); } catch { }", "");
        final int far = this.used("try { Two(); } catch { }", "static void One() { throw new Exception(); }"
                + " static void Two() { One(); }");
        assertTrue(near >= Unwound.THROW && near < Unwound.THROW + 20, () -> "a throw near costs " + near);
        assertTrue(far - near >= 2 * Unwound.CALL, () -> "a throw two calls down costs " + far + " to " + near);
    }

    @Test
    void finally_runningWhenTheProcessIsPutAwayCarriesOnWhenItIsPutBack() {
        final Process process = started("""
                class Tool {
                    static void Main() {
                        try {
                            try { throw new Exception("kept"); } finally {
                                int turns = 0;
                                while (turns < 400) { turns++; }
                                Console.PrintLine("finally done");
                            }
                        } catch (Exception failed) { Console.PrintLine("caught " + failed.Message); }
                    }
                }
                """);
        process.step(300);
        assertEquals(Process.State.RUNNING, process.state());
        final Process back = Process.restore(process.program(), process.save(), IHost.still());
        back.step(PLENTY);
        assertEquals(Process.State.FINISHED, back.state(), () -> String.valueOf(back.message()));
        assertEquals(List.of("finally done", "caught kept"), back.console());
    }

    @Test
    void program_withoutExceptionsWritesTheSameListingAsBefore() {
        final String listing = built("class Tool { static void Main() { Console.PrintLine(\"hi\"); } }").assembly();
        assertTrue(listing.startsWith(".asm 3"), listing);
        assertFalse(listing.contains(".line") || listing.contains("Exception"), listing);
    }

    @Test
    void exceptions_areRefusedWhereTheyCannotBe() {
        final List<String> said = errors("""
                class Plain { }
                class Tool {
                    static int Back() { try { return 1; } finally { return 2; } }
                    static void Main() {
                        try { } catch (Plain wrong) { }
                        try { } catch (Exception all) { } catch (IOException never) { }
                        throw new Plain();
                        throw;
                        while (true) { try { } finally { break; } }
                    }
                }
                """);
        assertEquals(2, said.stream().filter(line -> line.contains("S3099")).count(), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3097")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3098")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3100")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3101")), said::toString);
    }

    @Test
    void exceptions_areKeptBackByAnOlderVersionAndRefusedBySigma() {
        final SigmaCompiler.Result older = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE
                + "class Tool { static void Main() { try { } finally { } throw null; } }")), AsmProgram.DEFAULT_ISA,
                LanguageLevel.SIGMA_SHARP, 2);
        assertEquals(2, older.lines().stream().filter(line -> line.contains("S2014")).count(), older.lines()::toString);
        final SigmaCompiler.Result sigma = SigmaCompiler.compile(List.of(new SourceFile("tool.sg",
                "namespace Tests; class Tool { static void Main() { try { } finally { } } }")),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA);
        assertEquals(1, sigma.lines().stream().filter(line -> line.contains("S3052")).count(), sigma.lines()::toString);
    }

    /** What the language's spec charges for a throw, and for each call it leaves. */
    private static final class Unwound {
        static final int THROW = 50;
        static final int CALL = 5;
    }

    /* How many instructions a Main of those lines, beside those methods, takes to run to its end. */
    private int used(final String main, final String methods) {
        final Process process = started("class Tool { " + methods + " static void Main() { " + main + " } }");
        final int used = process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        return used;
    }

    private static SigmaCompiler.Result built(final String source) {
        return SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + source)));
    }

    private static List<String> errors(final String source) {
        final SigmaCompiler.Result result = built(source);
        assertFalse(result.ok(), "the program does not compile");
        return result.lines();
    }

    private static Process started(final String source) {
        final SigmaCompiler.Result result = built(source);
        assertTrue(result.ok(), () -> String.join("\n", result.lines()));
        final AsmReader reader = new AsmReader(result.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems(), () -> reader.problems() + "\n" + result.assembly());
        final Process process = new Process(program, ROOM, IHost.still());
        process.beginStatic(program.entryPoint(), "Main");
        return process;
    }

    private static List<String> run(final String source) {
        final Process process = started(source);
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(),
                () -> process.message() + " after " + process.console() + "\n" + built(source).assembly());
        return process.console();
    }
}
