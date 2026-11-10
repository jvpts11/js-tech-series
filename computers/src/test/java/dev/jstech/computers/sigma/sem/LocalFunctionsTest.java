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

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalFunctionsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; namespace Tests; ";

    @Test
    void localFunction_isCalledAsAMethodIs() {
        assertEquals(List.of("6", "hi there"), run("""
                class Tool {
                    static void Main() {
                        int Twice(int x) => x * 2;
                        string Greet(string who) { return "hi " + who; }
                        Console.PrintLine("" + Twice(3));
                        Console.PrintLine(Greet("there"));
                    }
                }
                """));
    }

    @Test
    void localFunction_mayBeCalledAboveWhereItIsWritten() {
        assertEquals(List.of("10"), run("""
                class Tool {
                    static void Main() {
                        Console.PrintLine("" + Add(4, 6));
                        int Add(int a, int b) { return a + b; }
                    }
                }
                """));
    }

    @Test
    void localFunction_callsItself() {
        assertEquals(List.of("120"), run("""
                class Tool {
                    static void Main() {
                        int Factorial(int n) { return n <= 1 ? 1 : n * Factorial(n - 1); }
                        Console.PrintLine("" + Factorial(5));
                    }
                }
                """));
    }

    @Test
    void localFunction_readsAndWritesTheLocalsAroundIt() {
        assertEquals(List.of("3", "13"), run("""
                class Tool {
                    int start = 10;
                    void Run() {
                        int count = 0;
                        void Bump() { count++; }
                        Bump();
                        Bump();
                        Bump();
                        Console.PrintLine("" + count);
                        int FromHere() { return this.start + count; }
                        Console.PrintLine("" + FromHere());
                    }
                    static void Main() { new Tool().Run(); }
                }
                """));
    }

    @Test
    void localFunction_goesWhereADelegateOfItsShapeIsWanted() {
        assertEquals(List.of("9"), run("""
                class Tool {
                    static int Apply(Func<int, int> step, int value) { return step(value); }
                    static void Main() {
                        int Square(int x) { return x * x; }
                        Console.PrintLine("" + Apply(Square, 3));
                    }
                }
                """));
    }

    @Test
    void localFunction_givesBackOnEveryWayOut() {
        assertTrue(errors("""
                class Tool {
                    static void Main() {
                        int Half(int x) { if (x > 0) { return x / 2; } }
                    }
                }
                """).stream().anyMatch(line -> line.contains("S3012")));
    }

    @Test
    void localFunction_isNeverWrittenTo() {
        final List<String> said = errors("""
                class Tool {
                    static void Main() {
                        int One() => 1;
                        int Two() => 2;
                        One = Two;
                        int One() => 3;
                    }
                }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3087")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3002")), said::toString);
    }

    private static List<String> errors(final String source) {
        final SigmaCompiler.Result result =
                SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + source)));
        assertFalse(result.ok(), "the program does not compile");
        return result.lines();
    }

    private static List<String> run(final String source) {
        final SigmaCompiler.Result result =
                SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + source)));
        assertTrue(result.ok(), () -> String.join("\n", result.lines()));
        final AsmReader reader = new AsmReader(result.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems(), "the listing reads back");
        final Process process = new Process(program, ROOM, IHost.still());
        process.beginStatic(program.entryPoint(), "Main");
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        return process.console();
    }
}
