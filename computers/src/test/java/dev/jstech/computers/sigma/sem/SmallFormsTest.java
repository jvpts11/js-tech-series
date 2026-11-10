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

class SmallFormsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; namespace Tests; ";

    @Test
    void sealed_classRunsAndNothingStandsOnIt() {
        assertEquals(List.of("leaf"), run("""
                sealed class Leaf { public string Name() { return "leaf"; } }
                class Tool { static void Main() { Console.PrintLine(new Leaf().Name()); } }
                """));
        assertTrue(errors("""
                sealed class Leaf { }
                class Twig : Leaf { }
                class Tool { static void Main() { } }
                """).stream().anyMatch(line -> line.contains("S3079")));
    }

    @Test
    void sealed_overrideStopsTheReplacingThere() {
        assertEquals(List.of("middle"), run("""
                class Top { public virtual string Say() { return "top"; } }
                class Middle : Top { public sealed override string Say() { return "middle"; } }
                class Bottom : Middle { }
                class Tool { static void Main() { Top seen = new Bottom(); Console.PrintLine(seen.Say()); } }
                """));
        assertTrue(errors("""
                class Top { public virtual string Say() { return "top"; } }
                class Middle : Top { public sealed override string Say() { return "middle"; } }
                class Bottom : Middle { public override string Say() { return "bottom"; } }
                class Tool { static void Main() { } }
                """).stream().anyMatch(line -> line.contains("S3079")));
    }

    @Test
    void sealed_isRefusedWhereItMeansNothing() {
        final List<String> said = errors("""
                class Top { public sealed string Alone() { return "x"; } }
                sealed struct Point { public int X; }
                abstract sealed class Shape { }
                interface IShape { }
                sealed enum Colour { Red }
                class Tool { sealed int count; static void Main() { } }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3080")), said::toString);
        assertEquals(4, said.stream().filter(line -> line.contains("S3051")).count(), said::toString);
    }

    @Test
    void sealed_isStillANameWhereNoModifierStands() {
        assertEquals(List.of("3"), run("""
                class Tool {
                    static void Main() {
                        int sealed = 3;
                        Console.PrintLine("" + sealed);
                    }
                }
                """));
    }

    @Test
    void staticClass_holdsStaticMembersAndIsNeverMade() {
        assertEquals(List.of("6"), run("""
                static class Numbers {
                    const int Base = 2;
                    public static int Times(int x) { return x * Base; }
                }
                class Tool { static void Main() { Console.PrintLine("" + Numbers.Times(3)); } }
                """));
        final List<String> said = errors("""
                interface IThing { }
                static class Numbers : IThing {
                    public int Count;
                    public int Twice(int x) { return x * 2; }
                }
                class More : Numbers { }
                class Tool { static void Main() { Numbers made = new Numbers(); } }
                """);
        assertEquals(2, said.stream().filter(line -> line.contains("S3081")).count(), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3082")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3083")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3079")), said::toString);
    }

    @Test
    void params_takesTheValuesOneByOneOrAsAnArray() {
        assertEquals(List.of("6", "0", "a-b-c", "9"), run("""
                class Tool {
                    static int Sum(params int[] values) {
                        int total = 0;
                        for (int i = 0; i < values.Length; i++) { total += values[i]; }
                        return total;
                    }
                    static string Join(string between, params string[] parts) {
                        string joined = "";
                        for (int i = 0; i < parts.Length; i++) { joined += (i > 0 ? between : "") + parts[i]; }
                        return joined;
                    }
                    static void Main() {
                        Console.PrintLine("" + Sum(1, 2, 3));
                        Console.PrintLine("" + Sum());
                        Console.PrintLine(Join("-", "a", "b", "c"));
                        int[] held = new int[2];
                        held[0] = 4;
                        held[1] = 5;
                        Console.PrintLine("" + Sum(held));
                    }
                }
                """));
    }

    @Test
    void params_losesToAVersionThatTakesTheValueAsWritten() {
        assertEquals(List.of("one", "many", "many"), run("""
                class Tool {
                    static string Pick(int value) { return "one"; }
                    static string Pick(params int[] values) { return "many"; }
                    static void Main() {
                        Console.PrintLine(Pick(1));
                        Console.PrintLine(Pick(1, 2));
                        Console.PrintLine(Pick());
                    }
                }
                """));
    }

    @Test
    void params_isTheLastParameterAndAnArray() {
        final List<String> said = errors("""
                class Tool {
                    static void First(params int[] values, int last) { }
                    static void Plain(params int value) { }
                    static void Main() { }
                }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3084")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3085")), said::toString);
    }

    @Test
    void params_givesAWrongValueItsOwnMessage() {
        assertTrue(errors("""
                class Tool {
                    static int Sum(params int[] values) { return 0; }
                    static void Main() { Sum(1, "two"); }
                }
                """).stream().anyMatch(line -> line.contains("S3003") && line.contains("'int'")));
    }

    @Test
    void nameof_isTheNameAsText() {
        assertEquals(List.of("count", "Limit", "Tool", "Length", "Limit"), run("""
                class Tool {
                    const int Limit = 3;
                    const string Named = nameof(Limit);
                    static void Main() {
                        int count = 0;
                        Console.PrintLine(nameof(count));
                        Console.PrintLine(nameof(Tool.Limit));
                        Console.PrintLine(nameof(Tool));
                        string text = "abc";
                        Console.PrintLine(nameof(text.Length));
                        Console.PrintLine(Named);
                    }
                }
                """));
        final String listing = built("""
                class Tool { static void Main() { int count = 0; Console.PrintLine(nameof(count)); } }
                """).assembly();
        assertTrue(listing.contains("\"count\""), listing);
    }

    @Test
    void nameof_refusesWhatNamesNothing() {
        final List<String> said = errors("""
                class Tool {
                    static void Main() {
                        Console.PrintLine(nameof(missing));
                        Console.PrintLine(nameof(1));
                        Console.PrintLine(nameof(Tool.Nothing));
                    }
                }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3001")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3086")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3004")), said::toString);
    }

    @Test
    void nameof_isTheProgramsOwnMethodWhenItHasOne() {
        assertEquals(List.of("mine"), run("""
                class Tool {
                    static string nameof(int x) { return "mine"; }
                    static void Main() { Console.PrintLine(nameof(1)); }
                }
                """));
    }

    @Test
    void smallForms_areRefusedByAnOlderVersion() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + """
                sealed class Leaf { }
                static class Numbers { }
                class Tool {
                    static int Sum(params int[] values) { return 0; }
                    static void Main() {
                        int Twice(int x) => x * 2;
                    }
                }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertEquals(4, built.lines().stream().filter(line -> line.contains("S3057")).count(),
                built.lines()::toString);
        // nameof is known for one only once the program has said it calls nothing of its own so.
        final SigmaCompiler.Result named = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE
                        + "class Tool { static void Main() { Console.PrintLine(nameof(Main)); } }")),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertEquals(1, named.lines().stream().filter(line -> line.contains("S3057")).count(), named.lines()::toString);
    }

    @Test
    void smallForms_areNotSigmas() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("tool.sg", """
                namespace Tests;
                sealed class Leaf { }
                static class Numbers { }
                class Tool {
                    static int Sum(params int[] values) { return 0; }
                    static void Main() {
                        int Twice(int x) { return x * 2; }
                    }
                }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA);
        assertEquals(4, built.lines().stream().filter(line -> line.contains("S3052")).count(),
                built.lines()::toString);
        final SigmaCompiler.Result named = SigmaCompiler.compile(List.of(new SourceFile("tool.sg",
                "namespace Tests; class Tool { static void Main() { string text = nameof(Main); } }")),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA);
        assertEquals(1, named.lines().stream().filter(line -> line.contains("S3052")).count(), named.lines()::toString);
    }

    private static SigmaCompiler.Result built(final String source) {
        return SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + source)));
    }

    private static List<String> errors(final String source) {
        final SigmaCompiler.Result result = built(source);
        assertFalse(result.ok(), "the program does not compile");
        return result.lines();
    }

    private static List<String> run(final String source) {
        final SigmaCompiler.Result result = built(source);
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
