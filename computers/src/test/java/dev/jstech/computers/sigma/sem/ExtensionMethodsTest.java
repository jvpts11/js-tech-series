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

class ExtensionMethodsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;

    @Test
    void extension_isCalledOnTheValueAsIfItWereItsOwn() {
        assertEquals(List.of("6", "HEY!", "hi there"), run(List.of(source("Tool.sgs", """
                using System.*; namespace Tests;
                static class Words {
                    public static string Shout(this string text) { return text.ToUpper() + "!"; }
                    public static string Joined(this string text, params string[] more) {
                        string all = text;
                        for (int i = 0; i < more.Length; i++) { all += " " + more[i]; }
                        return all;
                    }
                }
                static class Numbers { public static int Twice(this int value) { return value * 2; } }
                class Tool {
                    static void Main() {
                        int three = 3;
                        Console.PrintLine("" + three.Twice());
                        Console.PrintLine("hey".Shout());
                        Console.PrintLine("hi".Joined("there"));
                    }
                }
                """))));
    }

    @Test
    void extension_isReachedThroughTheUsingOfItsNamespace() {
        final SourceFile library = source("Words.sgs", """
                using System.*; namespace Tools.Text;
                static class Words { public static string Shout(this string text) { return text + "!"; } }
                """);
        assertEquals(List.of("hey!"), run(List.of(library, source("Tool.sgs", """
                using System.*; using Tools.Text.*; namespace Tests;
                class Tool { static void Main() { Console.PrintLine("hey".Shout()); } }
                """))));
        final SigmaCompiler.Result unreached = SigmaCompiler.compile(List.of(library, source("Tool.sgs", """
                using System.*; namespace Tests;
                class Tool { static void Main() { Console.PrintLine("hey".Shout()); } }
                """)));
        assertTrue(unreached.lines().stream().anyMatch(line -> line.contains("S3004")), unreached.lines()::toString);
    }

    @Test
    void extension_losesToAMethodOfTheValuesOwn() {
        assertEquals(List.of("own"), run(List.of(source("Tool.sgs", """
                using System.*; namespace Tests;
                static class More { public static string Say(this Lamp lamp) { return "extension"; } }
                class Lamp { public string Say() { return "own"; } }
                class Tool { static void Main() { Console.PrintLine(new Lamp().Say()); } }
                """))));
    }

    @Test
    void extension_isAlsoAStaticMethodCalledTheLongWay() {
        assertEquals(List.of("8"), run(List.of(source("Tool.sgs", """
                using System.*; namespace Tests;
                static class Numbers { public static int Twice(this int value) { return value * 2; } }
                class Tool { static void Main() { Console.PrintLine("" + Numbers.Twice(4)); } }
                """))));
    }

    @Test
    void extension_livesInAStaticClassWithThisOnItsFirstParameter() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(source("Tool.sgs", """
                using System.*; namespace Tests;
                class Plain { public static int Twice(this int value) { return value * 2; } }
                static class Numbers {
                    public static int Second(int first, this int value) { return value; }
                    public static int Out(this out int value) { value = 1; return 1; }
                }
                class Tool { Tool(this int value) { } static void Main() { } }
                """)));
        assertEquals(2, built.lines().stream().filter(line -> line.contains("S3092")).count(), built.lines()::toString);
        assertEquals(2, built.lines().stream().filter(line -> line.contains("S3091")).count(), built.lines()::toString);
    }

    @Test
    void extension_isRefusedByAnOlderVersionAndBySigma() {
        final SigmaCompiler.Result older = SigmaCompiler.compile(List.of(source("Tool.sgs", """
                using System.*; namespace Tests;
                class Numbers { public static int Twice(this int value) { return value * 2; } }
                class Tool { static void Main() { } }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertEquals(1, older.lines().stream().filter(line -> line.contains("S3057")).count(), older.lines()::toString);
        final SigmaCompiler.Result sigma = SigmaCompiler.compile(List.of(source("tool.sg", """
                namespace Tests;
                class Numbers { public static int Twice(this int value) { return value * 2; } }
                class Tool { static void Main() { } }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA);
        assertEquals(1, sigma.lines().stream().filter(line -> line.contains("S3052")).count(), sigma.lines()::toString);
    }

    private static SourceFile source(final String name, final String text) {
        return new SourceFile(name, text);
    }

    private static List<String> run(final List<SourceFile> sources) {
        final SigmaCompiler.Result result = SigmaCompiler.compile(sources);
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
