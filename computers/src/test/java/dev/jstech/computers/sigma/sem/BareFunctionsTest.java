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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.computers.vm.program.Values;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The calls written with no type in front of them: each old name is the library's own call written the short way,
 * the listing is the long way's to the byte, and a name the program has for itself stays the program's.
 */
class BareFunctionsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;

    private static final String PRELUDE = "using Standard.*; namespace Tests; ";
    private static final String SHARP_PRELUDE = "using System.*; using Standard.*; namespace Tests; ";

    /** Each old name beside the call a player would have written the long way, in a method body. */
    private static final List<List<String>> SAME_LISTINGS = List.of(
            List.of("puts(\"hi\");", "Console.PrintLine(\"hi\");"),
            List.of("string s = gets();", "string s = Console.ReadLine();"),
            List.of("exit(1);", "Program.Exit(1);"),
            List.of("int a = abs(-3); double b = abs(-2.5);", "int a = Math.Abs(-3); double b = Math.Abs(-2.5);"),
            List.of("double r = sqrt(16);", "double r = Math.Sqrt(16);"),
            List.of("double p = pow(2, 10);", "double p = Math.Pow(2, 10);"),
            List.of("double f = floor(2.7);", "double f = Math.Floor(2.7);"),
            List.of("int m = min(3, 4); double n = max(1.5, 2);",
                    "int m = Math.Min(3, 4); double n = Math.Max(1.5, 2);"),
            List.of("double d = atof(\"2.5\");", "double d = Convert.ToDouble(\"2.5\");"),
            List.of("int n = 42; string t = itoa(n);", "int n = 42; string t = Convert.ToString(n);"),
            List.of("srand(7);", "Random.Seed(7);"),
            List.of("int r = rand();", "int r = Random.Next(32768);"),
            List.of("string s = \"abc\"; int n = strlen(s);", "string s = \"abc\"; int n = s.Length;"),
            List.of("string s = \"abc\"; int i = strstr(s, \"b\");", "string s = \"abc\"; int i = s.IndexOf(\"b\");"),
            List.of("int n = 3; string t = sprintf(\"%d items\", n);", "int n = 3; string t = \"\" + n + \" items\";"));

    private static SigmaCompiler.Result built(final String members, final String body, final LanguageLevel level,
                                              final int version) {
        final String source = (level.full() ? SHARP_PRELUDE : PRELUDE) + "class Says : Script { " + members
                + " public override void OnTick() { " + body + " } }";
        return SigmaCompiler.compile(List.of(new SourceFile(level.full() ? "Says.sgs" : "Says.sg", source)),
                level.full() ? AsmProgram.DEFAULT_ISA : "jsc:x86_16", level, version);
    }

    private static String listing(final String body, final LanguageLevel level) {
        final SigmaCompiler.Result built = built("", body, level, SigmaVersions.NEWEST);
        assertTrue(built.ok(), () -> body + "\n" + String.join("\n", built.lines()));
        return built.assembly();
    }

    /** What the program printed, a line to an entry. */
    private static List<String> printed(final String members, final String body, final LanguageLevel level,
                                        final int version) {
        final SigmaCompiler.Result built = built(members, body, level, version);
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems());
        final Process process = new Process(program, ROOM, IHost.still());
        final Values.Obj self = process.create(program.entryPoint());
        assertNotNull(self);
        process.begin(self, "OnTick");
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        return process.console();
    }

    private static String refusal(final String body, final LanguageLevel level, final int version) {
        final SigmaCompiler.Result built = built("", body, level, version);
        assertFalse(built.ok(), "it was taken");
        return String.join("\n", built.lines());
    }

    @Test
    void all_printfCameWithTheLanguagesAndTheOldNamesInTheSecondVersion() {
        for (final BareFunctions.Function function : BareFunctions.all()) {
            assertFalse(function.forms().isEmpty(), function.name());
            assertTrue(function.in(LanguageLevel.SIGMA) && function.in(LanguageLevel.SIGMA_SHARP), function.name());
            assertEquals("printf".equals(function.name()) ? SigmaVersions.FIRST : 2, function.since(),
                    function.name());
        }
    }

    @Test
    void named_findsOneByItsNameAndNothingForAnyOtherName() {
        assertEquals(BareFunctions.Shape.READ_OFF_THE_FIRST, BareFunctions.named("strlen").shape());
        assertNull(BareFunctions.named("Strlen"));
        assertEquals(List.of("printf", "puts", "pow"),
                BareFunctions.startingWith("p").stream().map(BareFunctions.Function::name).toList());
    }

    /** Nothing of the short way is left in what is written down, in either language. */
    @Test
    void everyOldName_writesTheListingTheLongWayWrites() {
        for (final LanguageLevel level : LanguageLevel.values()) {
            for (final List<String> pair : SAME_LISTINGS) {
                assertEquals(listing(pair.get(1), level), listing(pair.getFirst(), level),
                        () -> level + ": " + pair.getFirst());
            }
        }
    }

    @Test
    void theOldNames_doWhatTheLongWayDoes() {
        final List<String> out = printed("", "puts(itoa(strlen(\"abcd\") + strstr(\"hello\", \"ll\"))); "
                + "puts(sprintf(\"%s=%d\", \"n\", abs(-4))); srand(7); int r = rand(); "
                + "if (r >= 0 && r < 32768) { puts(\"in range\"); }", LanguageLevel.SIGMA, SigmaVersions.NEWEST);
        assertEquals(List.of("6", "n=4", "in range"), out);
    }

    /** A program that had a method or a variable under one of the names before they came keeps calling its own. */
    @Test
    void aNameTheProgramHasForItself_staysTheProgramsAtEveryVersion() {
        assertEquals(List.of("7"), printed("static int abs(int n) { return 7; }", "printf(\"%d\\n\", abs(-3));",
                LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST));
        assertEquals(List.of("9"), printed("", "Func<int, int> abs = n => 9; printf(\"%d\\n\", abs(1));",
                LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST));
    }

    @Test
    void anOldName_atTheFirstVersion_needsTheSecond() {
        assertTrue(refusal("puts(\"x\");", LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST)
                .contains("'puts' needs Σ# 2; this project is Σ# 1"));
        assertTrue(refusal("int r = rand();", LanguageLevel.SIGMA, SigmaVersions.FIRST)
                .contains("'rand' needs Σ 2; this project is Σ 1"));
        assertEquals(List.of("1"), printed("", "printf(\"%d\\n\", 1);", LanguageLevel.SIGMA, SigmaVersions.FIRST));
    }

    /** A value the name does not take is reported under the name the player wrote. */
    @Test
    void anOldName_givenWhatItDoesNotTake_isRefusedUnderItsOwnName() {
        assertTrue(refusal("int a = abs(\"x\");", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("no version of 'abs' takes those arguments"));
        assertTrue(refusal("int r = rand(5);", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("no version of 'rand' takes those arguments"));
        assertTrue(refusal("string t = sprintf(\"%d\", 1, 2);", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("sprintf: the format has 1 hole and the call gives 2 values"));
    }

    /** The smaller language gained Random with rand, and only the two calls rand and srand come down to. */
    @Test
    void random_isInTheSmallerLanguagesLibraryFromTheSecondVersion() {
        assertTrue(refusal("int r = Random.Next(6);", LanguageLevel.SIGMA, SigmaVersions.FIRST)
                .contains("'Random' needs Σ 2; this project is Σ 1"));
        assertTrue(built("", "int r = Random.Next(6);", LanguageLevel.SIGMA_SHARP, SigmaVersions.FIRST).ok(),
                "the full language always had it");
        assertTrue(refusal("double d = Random.NextDouble();", LanguageLevel.SIGMA, SigmaVersions.NEWEST)
                .contains("S3052"));
    }
}
