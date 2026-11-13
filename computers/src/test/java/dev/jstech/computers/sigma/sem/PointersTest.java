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
import dev.jstech.core.text.Text;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pointers and raw memory, as version 3 of the language has them: unsafe code in Sigma#, everywhere in Sigma.
 */
class PointersTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; namespace Tests; ";
    /** The words a machine running a Unix stops a program with that read outside its memory. */
    private static final String UNIX_WORDS = "Segmentation fault (core dumped)";

    @Test
    void pointer_readsAndWritesTheVariableItPointsAt() {
        assertEquals(List.of("7", "10", "12", "5"), run("""
                struct Point { public int X; public int Y; }
                class Tool {
                    unsafe static void Twice(int n) {
                        int* p = &n;
                        *p = *p * 2;
                        Console.PrintLine("" + n);
                    }
                    unsafe static void Main() {
                        int x = 5;
                        int* p = &x;
                        *p = 7;
                        Console.PrintLine("" + x);
                        *p += 3;
                        Console.PrintLine("" + *p);
                        Twice(6);
                        Point spot;
                        int* across = &spot.X;
                        *across = 5;
                        Console.PrintLine("" + spot.X);
                    }
                }
                """));
    }

    @Test
    void memory_isTakenMovedThroughAndGivenBack() {
        assertEquals(List.of("4 9 2", "1 4 9", "0 1 4 0", "same 1 0"), run("""
                class Tool {
                    unsafe static void Main() {
                        int* numbers = (int*) Memory.Alloc(5 * sizeof(int));
                        for (int i = 0; i < 5; i++) { numbers[i] = i * i; }
                        int* third = numbers + 2;
                        Console.PrintLine(*third + " " + third[1] + " " + (third - numbers));
                        int* walk = numbers;
                        walk++;
                        Console.PrintLine(*walk + " " + walk[1] + " " + *(walk + 2));
                        numbers = (int*) Memory.Resize(numbers, 6 * sizeof(int));
                        Console.PrintLine(numbers[0] + " " + numbers[1] + " " + numbers[2] + " " + numbers[5]);
                        byte* bytes = (byte*) Memory.AllocZeroed(2, 4);
                        Memory.Fill(bytes, 255, 8);
                        byte* other = (byte*) Memory.Alloc(8);
                        Memory.Copy(other, bytes, 8);
                        int same = Memory.Compare(other, bytes, 8);
                        other[0] = 0;
                        Console.PrintLine((same == 0 ? "same " : "not ") + Memory.Compare(bytes, other, 8) + " "
                                + other[0]);
                        Memory.Free(numbers);
                        Memory.Free(bytes);
                        Memory.Free(other);
                        Memory.Free(null);
                    }
                }
                """));
    }

    @Test
    void struct_withNothingInItStillTakesAByte() {
        assertEquals(List.of("1"), run("""
                struct Marker { }
                class Tool {
                    unsafe static void Main() {
                        Console.PrintLine("" + sizeof(Marker));
                    }
                }
                """));
    }

    @Test
    void struct_liesInMemoryFieldByField() {
        assertEquals(List.of("32", "4 40000000000 200 1.5", "3", "2"), run("""
                struct Point { public int X; public long Y; public byte Z; public double W; }
                struct Counter {
                    public int Count;
                    public void Bump() { Count++; }
                }
                class Tool {
                    unsafe static void Main() {
                        Console.PrintLine("" + sizeof(Point));
                        Point* p = (Point*) Memory.AllocZeroed(2, sizeof(Point));
                        p->X = 3;
                        p->Y = 40000000000L;
                        p->Z = 200;
                        p->W = 1.5;
                        p[1] = *p;
                        p[1].X += 1;
                        Point copy = p[1];
                        Console.PrintLine(copy.X + " " + copy.Y + " " + copy.Z + " " + copy.W);
                        Console.PrintLine("" + p->X);
                        Counter* counter = (Counter*) Memory.AllocZeroed(1, sizeof(Counter));
                        counter->Bump();
                        counter->Bump();
                        Console.PrintLine("" + counter->Count);
                    }
                }
                """));
    }

    @Test
    void pointers_compareByAddressAndNullIsNone() {
        assertEquals(List.of("none", "before", "same"), run("""
                class Tool {
                    unsafe static void Main() {
                        int* none = null;
                        if (none == null) { Console.PrintLine("none"); }
                        int* block = stackalloc int[4];
                        if (block < block + 1 && block + 3 > block) { Console.PrintLine("before"); }
                        void* any = block;
                        if ((int*) any == block) { Console.PrintLine("same"); }
                    }
                }
                """));
    }

    @Test
    void stackalloc_isFreedWhenItsCallReturns() {
        assertEquals(List.of("9", "violation"), run("""
                class Tool {
                    unsafe static int* Kept() {
                        int* kept = stackalloc int[4];
                        kept[0] = 9;
                        Console.PrintLine("" + kept[0]);
                        return kept;
                    }
                    unsafe static void Main() {
                        int* gone = Kept();
                        try { Console.PrintLine("read " + *gone); }
                        catch (AccessViolationException failed) { Console.PrintLine("violation"); }
                    }
                }
                """));
    }

    @Test
    void violation_isAnExceptionInSigmaSharp() {
        assertEquals(List.of("past the end", "freed", "null", "after"), run("""
                class Tool {
                    unsafe static void Main() {
                        int* four = (int*) Memory.Alloc(4 * sizeof(int));
                        try { four[4] = 1; }
                        catch (AccessViolationException failed) { Console.PrintLine("past the end"); }
                        Memory.Free(four);
                        try { int read = four[0]; }
                        catch (AccessViolationException failed) { Console.PrintLine("freed"); }
                        int* none = null;
                        try { *none = 1; } catch (Exception failed) { Console.PrintLine("null"); }
                        Console.PrintLine("after");
                    }
                }
                """));
    }

    @Test
    void violation_inSigmaStopsTheProcessWithItsSystemsWords() {
        final IHost unix = new StillHost(UNIX_WORDS);
        final Process process = sigma("""
                namespace Tests;
                class Tool {
                    static void Main() {
                        int* p = malloc(2 * sizeof(int));
                        p[1] = 3;
                        printf("%d\\n", p[1]);
                        free(p);
                        int read = p[1];
                        printf("never\\n");
                    }
                }
                """, unix);
        process.step(PLENTY);
        assertEquals(Process.State.HALTED, process.state());
        assertEquals(List.of("3", UNIX_WORDS), process.console());
    }

    @Test
    void sigma_takesPointersAndTheMemoryCallsAsC() {
        final Process process = sigma("""
                namespace Tests;
                struct Node { public int Value; public Node* Next; }
                class Tool {
                    static void Main() {
                        Node* first = null;
                        for (int i = 1; i <= 3; i++) {
                            Node* made = calloc(1, sizeof(Node));
                            made->Value = i;
                            made->Next = first;
                            first = made;
                        }
                        int total = 0;
                        for (Node* at = first; at != null; at = at->Next) { total += at->Value; }
                        printf("%d\\n", total);
                        int* both = realloc(null, 8);
                        memset(both, 0, 8);
                        both[1] = 4;
                        int* copied = malloc(8);
                        memcpy(copied, both, 8);
                        printf("%d %d\\n", copied[1], memcmp(copied, both, 8));
                    }
                }
                """, new StillHost(UNIX_WORDS));
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals(List.of("6", "4 0"), process.console());
    }

    @Test
    void bytes_countInTheProgramsMemoryAndRunOutAsObjectsDo() {
        final Process process = started("""
                class Tool {
                    unsafe static void Main() {
                        void* kept = Memory.Alloc(1000);
                        Memory.Free(Memory.Alloc(100000));
                    }
                }
                """);
        process.step(PLENTY);
        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().english().contains("out of memory"), () -> process.message().english());
        assertTrue(process.heap().used() >= 1000, () -> "holds " + process.heap().used());
    }

    @Test
    void copy_costsOneForEverySixtyFourBytes() {
        final int small = this.used("Memory.Copy(a, b, 64);");
        final int large = this.used("Memory.Copy(a, b, 6400);");
        assertEquals(99, large - small);
    }

    @Test
    void bytes_holdFromZeroToTwoHundredAndFiftyFive() {
        assertEquals(List.of("44 255 44 0"), run("""
                class Tool {
                    static void Main() {
                        byte cut = (byte) 300;
                        byte full = 255;
                        byte added = 200;
                        added += 100;
                        byte stepped = 255;
                        stepped++;
                        Console.PrintLine(cut + " " + full + " " + added + " " + stepped);
                    }
                }
                """));
    }

    @Test
    void process_putAwayAndBackKeepsItsRawMemory() {
        final Process process = started("""
                class Tool {
                    unsafe static void Main() {
                        int* kept = (int*) Memory.Alloc(8);
                        kept[1] = 41;
                        int* onStack = stackalloc int[2];
                        onStack[0] = 1;
                        int turns = 0;
                        while (turns < 400) { turns++; }
                        Console.PrintLine("" + (kept[1] + onStack[0]));
                        int* fresh = (int*) Memory.Alloc(4);
                        Console.PrintLine(fresh > kept + 2 ? "past" : "reused");
                    }
                }
                """);
        process.step(300);
        assertEquals(Process.State.RUNNING, process.state());
        final Process back = Process.restore(process.program(), process.save(), IHost.still());
        back.step(PLENTY);
        assertEquals(Process.State.FINISHED, back.state(), () -> String.valueOf(back.message()));
        assertEquals(List.of("42", "past"), back.console());
    }

    @Test
    void program_withPointersIsWrittenInTheNewestFormat() {
        final String listing = built("class Tool { unsafe static void Main() { int* p = stackalloc int[1]; *p = 1; } }")
                .assembly();
        assertTrue(listing.startsWith(".asm 4"), listing);
        assertTrue(listing.contains("localloc") && listing.contains("stind.i4"), listing);
    }

    @Test
    void reader_refusesTheInstructionsOfRawMemoryInAnOlderListing() {
        final String listing = built("class Tool { unsafe static void Main() { int* p = stackalloc int[1]; *p = 1; } }")
                .assembly().replaceFirst(".asm " + AsmProgram.VERSION, ".asm " + AsmProgram.BASE_VERSION);
        final AsmReader reader = new AsmReader(listing);
        reader.read();
        for (final String instruction : List.of("'localloc'", "'stind.i4'")) {
            assertTrue(reader.problems().stream().anyMatch(problem -> problem.code().equals("A4019")
                    && problem.format().contains(instruction)), () -> reader.problems().toString());
        }
    }

    @Test
    void pointers_areRefusedWhereTheyCannotBe() {
        final List<String> said = errors("""
                class Plain { public int Count; }
                class Tool {
                    static void Safe() { int* p = null; }
                    static void Main() { }
                    unsafe static void Check(int* given) {
                        double* wrong = null;
                        int* none = &3;
                        Plain plain = new Plain();
                        int* field = &plain.Count;
                        int kept = 1;
                        int* p = &kept;
                        Action keeps = () => Console.PrintLine("" + kept);
                        int sized = sizeof(string);
                        void* any = null;
                        int read = *any;
                    }
                }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3102")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3103")), said::toString);
        assertEquals(2, said.stream().filter(line -> line.contains("S3104")).count(), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3105")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3107")), said::toString);
        assertTrue(said.stream().anyMatch(line -> line.contains("S3008")), said::toString);
    }

    @Test
    void ignore_ofAVariableInRawMemoryIsRefused() {
        final List<String> said = errors(
                "class Tool { unsafe static void Main() { int x = 1; int* p = &x; ignore x; x = 2; } }");
        assertTrue(said.stream().anyMatch(line -> line.contains("S3106")), said::toString);
    }

    @Test
    void pointers_areKeptBackByAnOlderVersionAndUnsafeAndStackallocRefusedBySigma() {
        final SigmaCompiler.Result older = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE
                + "class Tool { unsafe static void Main() { byte b = 1; int* p = stackalloc int[1];"
                + " int n = sizeof(int); } }")), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertTrue(older.lines().stream().filter(line -> line.contains("S3057")).count() >= 4, older.lines()::toString);
        final SigmaCompiler.Result sigma = SigmaCompiler.compile(List.of(new SourceFile("tool.sg",
                "namespace Tests; class Tool { unsafe static void Main() { int* p = stackalloc int[1];"
                        + " unsafe { } } }")),
                AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA);
        assertEquals(3, sigma.lines().stream().filter(line -> line.contains("S3052")).count(), sigma.lines()::toString);
    }

    @Test
    void theNewWords_areStillNamesInAnOlderProgram() {
        final SigmaCompiler.Result older = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + """
                class Tool {
                    static int sizeof(int n) { return n * 2; }
                    static void Main() {
                        int unsafe = 1;
                        int stackalloc = 2;
                        int count = 3;
                        Console.PrintLine("" + (unsafe + stackalloc + sizeof(count)));
                    }
                }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertTrue(older.ok(), older.lines()::toString);
    }

    /* How many instructions a Main with two blocks of 6400 bytes, a and b, takes to run those lines. */
    private int used(final String lines) {
        final Process process = started("class Tool { unsafe static void Main() { byte* a = (byte*) Memory.Alloc(6400);"
                + " byte* b = (byte*) Memory.Alloc(6400); " + lines + " } }");
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
        return begun(built(source), IHost.still());
    }

    private static Process sigma(final String source, final IHost host) {
        return begun(SigmaCompiler.compile(List.of(new SourceFile("tool.sg", source)), AsmProgram.DEFAULT_ISA,
                LanguageLevel.SIGMA), host);
    }

    private static Process begun(final SigmaCompiler.Result result, final IHost host) {
        assertTrue(result.ok(), () -> String.join("\n", result.lines()));
        final AsmReader reader = new AsmReader(result.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems(), () -> reader.problems() + "\n" + result.assembly());
        final Process process = new Process(program, ROOM, host);
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

    /** A host with no world around it, whose system says those words for a read outside a program's memory. */
    private record StillHost(String words) implements IHost {

        @Override
        public long tick() {
            return 0;
        }

        @Override
        public long dayTime() {
            return 0;
        }

        @Override
        public long day() {
            return 0;
        }

        @Override
        public Text accessViolation(final Text plain) {
            return Text.literal(this.words);
        }
    }
}
