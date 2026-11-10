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
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class LabelsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; namespace Tests; ";

    @Test
    void goto_jumpsBackAndForward() {
        assertEquals(List.of("3", "after"), run(inMain("""
                int i = 0;
                again: i++;
                if (i < 3) goto again;
                Console.PrintLine("" + i);
                goto past;
                Console.PrintLine("skipped");
                past: Console.PrintLine("after");
                """)));
    }

    @Test
    void goto_leavesABlockAndALock() {
        assertEquals(List.of("out", "again"), run("""
                class Tool {
                    static void Main() {
                        object held = "held";
                        lock (held) {
                            { goto done; }
                        }
                        done: Console.PrintLine("out");
                        lock (held) { Console.PrintLine("again"); }
                    }
                }
                """));
    }

    @Test
    void gotoCase_andGotoDefault_jumpInsideTheirSwitch() {
        assertEquals(List.of("one", "two", "other"), run(inMain("""
                switch (1) {
                    case 1:
                        Console.PrintLine("one");
                        goto case 2;
                    case 2:
                        Console.PrintLine("two");
                        goto default;
                    default:
                        Console.PrintLine("other");
                        break;
                }
                """)));
    }

    @Test
    void goto_refusesWhatItCannotReach() {
        assertTrue(codes(inMain("goto inside; { inside: Console.PrintLine(\"x\"); }")).contains("S3073"));
        assertTrue(codes(inMain("goto nowhere;")).contains("S3072"));
        assertTrue(codes(inMain("twice: Console.PrintLine(\"a\"); twice: Console.PrintLine(\"b\");"))
                .contains("S3071"));
        assertTrue(codes(inMain("goto case 3;")).contains("S3075"));
        assertTrue(codes(inMain("switch (1) { case 1: goto case 9; }")).contains("S3074"));
    }

    @Test
    void comefrom_takesTheWayOutOfItsLabel() {
        assertEquals(List.of("a", "b", "d"), run(inMain("""
                Console.PrintLine("a");
                mark: Console.PrintLine("b");
                Console.PrintLine("c");
                comefrom mark;
                Console.PrintLine("d");
                """)));
    }

    @Test
    void comefrom_refusesASecondOneAndOneOutOfReach() {
        assertTrue(codes(inMain("mark: Console.PrintLine(\"b\"); comefrom mark; comefrom mark;"))
                .contains("S3076"));
        assertTrue(codes(inMain("mark: Console.PrintLine(\"b\"); { comefrom mark; }")).contains("S3077"));
    }

    @Test
    void abstainfrom_passesOverTheRegionToTheEndOfItsBlock_untilReinstate() {
        assertEquals(List.of("x", "y", "z", "z", "x", "y", "z"), run("""
                class Tool {
                    static void Main() { Step(false); Step(true); Step(false); }
                    static void Step(bool off) {
                        if (off) abstainfrom part; else reinstate part;
                        { part: Console.PrintLine("x"); Console.PrintLine("y"); }
                        Console.PrintLine("z");
                    }
                }
                """));
        assertTrue(codes("class Tool { static void Main() { abstainfrom part; }"
                + " static void Other() { part: Console.PrintLine(\"x\"); } }").contains("S3072"),
                "an abstainfrom sits in its label's method");
    }

    @Test
    void abstainfrom_holdsAcrossLaterCallsOfTheMethod() {
        assertEquals(List.of("in 1", "out 1", "out 2"), run("""
                class Tool {
                    static void Main() { Step(1); Step(2); }
                    static void Step(int n) {
                        { part: Console.PrintLine("in " + n); }
                        Console.PrintLine("out " + n);
                        if (n == 1) abstainfrom part;
                    }
                }
                """));
    }

    @Test
    void comefrom_doesNotFireFromInsideASwitchedOffRegion() {
        final String body = """
                mark: Console.PrintLine("b");
                Console.PrintLine("c");
                region: Console.PrintLine("r");
                comefrom mark;
                Console.PrintLine("d");
                """;
        assertEquals(List.of("b", "d"), run(inMain(body)));
        assertEquals(List.of("b", "c"), run(inMain("abstainfrom region;\n" + body)));
    }

    @Test
    void ignore_makesWritesVanish_untilRemember() {
        assertEquals(List.of("1", "6", "10", "30"), run("""
                class Tool {
                    int count = 10;
                    static int total = 30;
                    static void Main() {
                        int x = 1;
                        ignore x;
                        x = 5;
                        Console.PrintLine("" + x);
                        remember x;
                        x = 6;
                        Console.PrintLine("" + x);
                        Tool t = new Tool();
                        ignore t.count;
                        t.count = 99;
                        Console.PrintLine("" + t.count);
                        ignore total;
                        total++;
                        Console.PrintLine("" + total);
                    }
                }
                """));
    }

    @Test
    void ignore_refusesWhatIsNoPlace() {
        assertTrue(codes(inMain("int[] a = new int[2]; ignore a[0];")).contains("S3078"));
    }

    @Test
    void listing_isWrittenInTheNewerFormatOnlyWhenItHoldsANewerInstruction() {
        assertTrue(built(inMain("again: goto again;")).assembly().startsWith(".asm 3"),
                "a goto is a branch, which every runtime knows");
        assertTrue(built(inMain("part: Console.PrintLine(\"x\"); abstainfrom part;")).assembly()
                .startsWith(".asm " + AsmProgram.VERSION), "abstain needs the newer format");
    }

    @Test
    void reader_refusesANewerInstructionInAnOlderListing() {
        final String listing = built(inMain("part: Console.PrintLine(\"x\"); abstainfrom part;")).assembly()
                .replaceFirst(".asm " + AsmProgram.VERSION, ".asm " + AsmProgram.BASE_VERSION);
        final AsmReader reader = new AsmReader(listing);
        reader.read();
        assertTrue(reader.problems().stream().map(ListingProblem::code).anyMatch("A4019"::equals),
                () -> reader.problems().toString());
    }

    @Test
    void theNewWords_areRefusedByAnOlderVersion() {
        final List<String> found = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + inMain(
                        "a: goto a; comefrom a; abstainfrom a; reinstate a; int x = 1; ignore x;"))),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2).lines();
        assertEquals(4, found.stream().filter(line -> line.contains("S2014")).count(), found::toString);
        assertEquals(2, found.stream().filter(line -> line.contains("S3057")).count(), found::toString);
    }

    @Test
    void sigma_hasGotoToALabelButNoGotoCase() {
        final List<String> found = SigmaCompiler.compile(List.of(new SourceFile("Tool.sg", "namespace Tests; "
                        + "class Tool { static void Main() { again: goto again;"
                        + " switch (1) { case 1: goto case 1; } } }")),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA).lines();
        assertEquals(1, found.stream().filter(line -> line.contains("S3052")).count(), found::toString);
    }

    private static String inMain(final String body) {
        return "class Tool { static void Main() {\n" + body + "\n} }";
    }

    private static SigmaCompiler.Result built(final String source) {
        return SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + source)));
    }

    private static List<String> codes(final String source) {
        final SigmaCompiler.Result result = built(source);
        assertFalse(result.ok(), "the program does not compile");
        return result.diagnostics().stream().map(diagnostic -> diagnostic.code()).toList();
    }

    private static List<String> run(final String source) {
        final SigmaCompiler.Result result = built(source);
        assertTrue(result.ok(), () -> String.join("\n", result.lines()));
        final AsmReader reader = new AsmReader(result.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems(), () -> reader.problems().toString());
        final Process process = new Process(program, ROOM, IHost.still());
        process.beginStatic(program.entryPoint(), "Main");
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        return process.console();
    }
}
