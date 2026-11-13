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

class ConstantsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; namespace Tests; ";

    @Test
    void const_isWrittenIntoEveryPlaceThatReadsIt() {
        final String source = """
                class Tool {
                    const int Limit = 40;
                    static void Main() {
                        const int More = Limit + 2;
                        Console.PrintLine("" + More);
                    }
                }
                """;
        assertEquals(List.of("42"), run(source));
        final String listing = built(source).assembly();
        assertFalse(listing.contains("Limit"), () -> "the listing keeps no field for a const:\n" + listing);
        assertFalse(listing.contains("ldsfld"), () -> "nothing reads a const as a field:\n" + listing);
    }

    @Test
    void const_mayBeWorkedOutFromOneDeclaredFurtherDown() {
        assertEquals(List.of("42"), run("""
                class Tool {
                    const int Twice = Half * 2;
                    const int Half = 21;
                    static void Main() { Console.PrintLine("" + Twice); }
                }
                """));
    }

    @Test
    void const_wrapsAWholeNumberAsTheProgramWould() {
        // A bool const reads as the bool written in the source does, which runs as a one.
        assertEquals(List.of("-2147483648", "10", "2.5", "x", "hello there", "1"), run("""
                class Tool {
                    const int Big = 2147483647 + 1;
                    const long Wide = 10;
                    const double Half = 5 / 2.0;
                    const char Letter = 'x';
                    const string Greeting = "hello" + " there";
                    const bool Yes = Big < 0 && Wide == 10L;
                    static void Main() {
                        Console.PrintLine("" + Big);
                        Console.PrintLine("" + Wide);
                        Console.PrintLine("" + Half);
                        Console.PrintLine("" + Letter);
                        Console.PrintLine(Greeting);
                        Console.PrintLine("" + Yes);
                    }
                }
                """));
    }

    @Test
    void const_holdsAnEnumsValue() {
        assertEquals(List.of("true"), run("""
                enum Colour { Red, Green = 5, Blue }
                class Tool {
                    const Colour Chosen = Colour.Blue;
                    static void Main() { Console.PrintLine("" + (Chosen == Colour.Blue)); }
                }
                """));
    }

    @Test
    void const_refusesBeingWrittenTo() {
        assertTrue(errors("""
                class Tool {
                    const int Limit = 1;
                    static void Main() {
                        const int Local = 2;
                        Limit = 3;
                        Local++;
                    }
                }
                """).stream().filter(line -> line.contains("S3066")).count() == 2);
    }

    @Test
    void const_refusesAValueNotKnownWhenCompiling() {
        assertTrue(errors("""
                class Tool {
                    const int Read = Twice(2);
                    static int Twice(int x) { return x * 2; }
                    static void Main() { }
                }
                """).stream().anyMatch(line -> line.contains("S3067")));
    }

    @Test
    void const_refusesOneWorkedOutFromItself() {
        assertTrue(errors("""
                class Tool {
                    const int A = B + 1;
                    const int B = A + 1;
                    static void Main() { }
                }
                """).stream().filter(line -> line.contains("S3068")).count() == 2);
    }

    @Test
    void const_sayingCircularOnlyOfTheOnesRoundTheLoop() {
        final List<String> said = errors("""
                class Tool {
                    const int A = B;
                    const int B = B + 1;
                    static void Main() { }
                }
                """);
        assertEquals(1, said.stream().filter(line -> line.contains("S3068")).count(), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3068") && line.contains("'B'")), said::toString);
    }

    @Test
    void const_refusesOneWithoutAValueOrOfAnotherKind() {
        final List<String> said = errors("""
                class Tool {
                    const int Missing;
                    const object Thing = "x";
                    static const int Twice = 2;
                    static void Main() { }
                }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3069")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3070")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3051")), said::toString);
    }

    @Test
    void const_isRefusedByAnOlderVersionAsItAlwaysWas() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE
                        + "class Tool { const int Limit = 1; static void Main() { const int Local = 2; } }")),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertEquals(2, built.lines().stream().filter(line -> line.contains("S2014")).count(),
                built.lines()::toString);
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
