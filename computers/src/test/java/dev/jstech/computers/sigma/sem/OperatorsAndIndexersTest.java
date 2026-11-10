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

class OperatorsAndIndexersTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; namespace Tests; ";

    /** A value type with the operators a player would give it. */
    private static final String MONEY = """
            struct Money {
                public int Cents;
                public Money(int cents) { Cents = cents; }
                public static Money operator +(Money a, Money b) => new Money(a.Cents + b.Cents);
                public static Money operator -(Money a) => new Money(-a.Cents);
                public static Money operator *(Money a, int times) => new Money(a.Cents * times);
                public static Money operator ++(Money a) => new Money(a.Cents + 1);
                public static bool operator ==(Money a, Money b) => a.Cents == b.Cents;
                public static bool operator !=(Money a, Money b) => a.Cents != b.Cents;
                public static bool operator <(Money a, Money b) => a.Cents < b.Cents;
                public static bool operator >(Money a, Money b) => a.Cents > b.Cents;
            }
            """;

    @Test
    void operator_givesTheTypesValuesTheirMeaning() {
        assertEquals(List.of("350", "-100", "300", "true", "true", "false"), run(MONEY + """
                class Tool {
                    static void Main() {
                        Money one = new Money(100);
                        Money sum = one + new Money(250);
                        Console.PrintLine("" + sum.Cents);
                        Console.PrintLine("" + (-one).Cents);
                        Console.PrintLine("" + (one * 3).Cents);
                        Console.PrintLine("" + (one == new Money(100)));
                        Console.PrintLine("" + (one < sum));
                        Console.PrintLine("" + (one != new Money(100)));
                    }
                }
                """));
    }

    @Test
    void operator_combinesAndStepsAPlace() {
        assertEquals(List.of("300", "301", "301", "302"), run(MONEY + """
                class Tool {
                    static void Main() {
                        Money held = new Money(100);
                        held += new Money(200);
                        Console.PrintLine("" + held.Cents);
                        held++;
                        Console.PrintLine("" + held.Cents);
                        Money before = held++;
                        Console.PrintLine("" + before.Cents);
                        Console.PrintLine("" + held.Cents);
                    }
                }
                """));
    }

    @Test
    void operator_isRefusedOutOfItsShape() {
        final List<String> said = errors("""
                class Point {
                    public int X;
                    public Point operator +(Point a, Point b) => a;
                    public static Point operator *(Point a) => a;
                    public static int operator -(int a, int b) => a;
                    public static bool operator ==(Point a, Point b) => true;
                }
                class Tool { static void Main() { } }
                """);
        assertEquals(2, said.stream().filter(line -> line.contains("S3093")).count(), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3094")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3095")), said::toString);
    }

    @Test
    void indexer_readsAndWritesThroughItsTwoHalves() {
        assertEquals(List.of("two", "TWO", "3", "11"), run("""
                class Shelf {
                    string[] held = new string[4];
                    int[] counts = new int[4];
                    public string this[int at] {
                        get { return held[at]; }
                        set { held[at] = value; }
                    }
                    public int this[string named] => named.Length;
                    public int Count(int at) { return counts[at]; }
                }
                class Tally {
                    int[] counts = new int[2];
                    public int this[int at] { get => counts[at]; set => counts[at] = value; }
                }
                class Tool {
                    static void Main() {
                        Shelf shelf = new Shelf();
                        shelf[1] = "two";
                        Console.PrintLine(shelf[1]);
                        shelf[1] = shelf[1].ToUpper();
                        Console.PrintLine(shelf[1]);
                        Console.PrintLine("" + shelf["abc"]);
                        Tally tally = new Tally();
                        tally[0] = 10;
                        tally[0]++;
                        Console.PrintLine("" + tally[0]);
                    }
                }
                """));
    }

    @Test
    void indexer_withOnlyAReaderIsNotWritten() {
        final List<String> said = errors("""
                class Lens { public int this[int at] => at; }
                class Grid { public int this[int row, int column] => row; }
                class Tool { static void Main() { Lens lens = new Lens(); lens[0] = 1; } }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3016") && line.contains("this[]")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3096")), said::toString);
    }

    @Test
    void operatorsAndIndexers_areRefusedByAnOlderVersionAndBySigma() {
        final String source = """
                class Box {
                    int held;
                    public static Box operator +(Box a, Box b) => a;
                    public static Box operator -(Box a, Box b) => a;
                    public int this[int at] => held;
                }
                class Tool { static void Main() { } }
                """;
        final SigmaCompiler.Result older = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + source)),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertEquals(3, older.lines().stream().filter(line -> line.contains("S3057")).count(), older.lines()::toString);
        final SigmaCompiler.Result sigma = SigmaCompiler.compile(List.of(new SourceFile("tool.sg",
                "namespace Tests;\n" + source)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA);
        assertEquals(3, sigma.lines().stream().filter(line -> line.contains("S3052")).count(), sigma.lines()::toString);
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
        assertFalse(reader.hasProblems(), () -> "the listing reads back:\n" + result.assembly());
        final Process process = new Process(program, ROOM, IHost.still());
        process.beginStatic(program.entryPoint(), "Main");
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(),
                () -> process.message() + " after " + process.console() + "\n" + result.assembly());
        return process.console();
    }
}
