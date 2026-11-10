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

class TuplesTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; namespace Tests; ";

    @Test
    void tuple_isReadByItsNamesAndItsItems() {
        assertEquals(List.of("3 lamps", "3 lamps"), run("""
                class Tool {
                    static (int count, string name) Find() { return (3, "lamps"); }
                    static void Main() {
                        (int count, string name) found = Find();
                        Console.PrintLine(found.count + " " + found.name);
                        Console.PrintLine(found.Item1 + " " + found.Item2);
                    }
                }
                """));
    }

    @Test
    void tuple_readsAsCSharpWritesIt() {
        assertEquals(List.of("(1, two, (3, 4))"), run("""
                class Tool {
                    static void Main() {
                        var nested = (1, "two", (3, 4));
                        Console.PrintLine("" + nested);
                    }
                }
                """));
    }

    @Test
    void tuple_isCopiedAsAStructIs() {
        assertEquals(List.of("1", "9"), run("""
                class Tool {
                    static void Main() {
                        (int a, int b) first = (1, 2);
                        (int a, int b) second = first;
                        second.a = 9;
                        Console.PrintLine("" + first.a);
                        Console.PrintLine("" + second.a);
                    }
                }
                """));
    }

    @Test
    void tuple_takesItsPlacesTypesWhenItIsHandedOne() {
        assertEquals(List.of("5", "2.5"), run("""
                class Tool {
                    static void Main() {
                        (long big, double half) pair = (5, 2.5);
                        long big = pair.big;
                        Console.PrintLine("" + big);
                        Console.PrintLine("" + pair.half);
                    }
                }
                """));
    }

    @Test
    void tuple_ofTheSameTypesIsOneTypeWhateverItsPlacesAreCalled() {
        assertEquals(List.of("4"), run("""
                class Tool {
                    static int Sum((int x, int y) point) { return point.x + point.y; }
                    static void Main() {
                        (int a, int b) given = (1, 3);
                        Console.PrintLine("" + Sum(given));
                    }
                }
                """));
    }

    @Test
    void deconstruct_makesALocalForEachPlace() {
        assertEquals(List.of("3 lamps"), run("""
                class Tool {
                    static (int, string) Find() { return (3, "lamps"); }
                    static void Main() {
                        var (count, name) = Find();
                        Console.PrintLine(count + " " + name);
                    }
                }
                """));
    }

    @Test
    void deconstruct_swapsTwoPlaces() {
        assertEquals(List.of("2 1", "6"), run("""
                class Tool {
                    int field = 1;
                    static void Main() {
                        int a = 1;
                        int b = 2;
                        (a, b) = (b, a);
                        Console.PrintLine(a + " " + b);
                        Tool tool = new Tool();
                        int[] held = new int[1];
                        (tool.field, held[0]) = (5, 6);
                        Console.PrintLine("" + held[0]);
                    }
                }
                """));
    }

    @Test
    void deconstruct_writesALocalALambdaKeeps() {
        assertEquals(List.of("7"), run("""
                class Tool {
                    static void Main() {
                        var (seven, other) = (7, 0);
                        Func<int, int> plus = x => x + seven;
                        Console.PrintLine("" + plus(other));
                    }
                }
                """));
    }

    @Test
    void tuple_refusesWhatDoesNotFit() {
        final List<String> said = errors("""
                class Tool {
                    static void Main() {
                        (int, int, int, int, int, int, int, int, int) wide;
                        var nothing = (null, 1);
                        int a = 0;
                        (a, a) = 5;
                        var (x, y) = (1, 2, 3);
                        (int, int) pair = (1, 2);
                        dispose pair;
                    }
                }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3088")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3089")), said::toString);
        assertEquals(2, said.stream().filter(line -> line.contains("S3090")).count(), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3022")), said::toString);
    }

    @Test
    void tuple_isRefusedByAnOlderVersionAndBySigma() {
        final SigmaCompiler.Result older = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + """
                class Tool {
                    static (int, int) Pair() { return (1, 2); }
                    static void Main() { var (a, b) = Pair(); }
                }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertEquals(3, older.lines().stream().filter(line -> line.contains("S3057")).count(),
                older.lines()::toString);
        final SigmaCompiler.Result sigma = SigmaCompiler.compile(List.of(new SourceFile("tool.sg", """
                namespace Tests;
                class Tool {
                    static (int, int) Pair() { return (1, 2); }
                    static void Main() { var (a, b) = Pair(); }
                }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA);
        assertEquals(3, sigma.lines().stream().filter(line -> line.contains("S3052")).count(),
                sigma.lines()::toString);
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
        assertFalse(reader.hasProblems(), () -> "the listing reads back:\n" + result.assembly());
        final Process process = new Process(program, ROOM, IHost.still());
        process.beginStatic(program.entryPoint(), "Main");
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        return process.console();
    }
}
