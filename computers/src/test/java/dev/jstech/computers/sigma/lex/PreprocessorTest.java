/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.edit.project.ProjectFile;
import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PreprocessorTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;
    /** What every file starts with, on a line of its own so a directive can open the next one. */
    private static final String PRELUDE = "using System.*; namespace Tests;\n";

    @Test
    void if_keepsTheBranchWhoseConditionHolds() {
        assertEquals(List.of("three", "x86"), run("""
                class Tool {
                    static void Main() {
                #if SIGMA_3
                        Console.PrintLine("three");
                #else
                        Console.PrintLine("older");
                #endif
                #if ARM
                        Console.PrintLine("arm");
                #elif X86 && !ARM
                        Console.PrintLine("x86");
                #else
                        Console.PrintLine("other");
                #endif
                    }
                }
                """));
    }

    @Test
    void if_passesOverLinesLeftOutWholeEvenWhenTheyDoNotParse() {
        assertEquals(List.of("kept"), run("""
                class Tool {
                    static void Main() {
                #if NOTHING
                    #if SIGMA_3
                        this is not Sigma at all {{{
                    #else
                        nor is this ;;; )
                    #endif
                #else
                        Console.PrintLine("kept");
                #endif
                    }
                }
                """));
    }

    @Test
    void if_readsTrueFalseBracketsAndComparisons() {
        assertEquals(List.of("a", "b", "c"), run("""
                class Tool {
                    static void Main() {
                #if (SIGMA_3 || false) && true
                        Console.PrintLine("a");
                #endif
                #if NOTHING == false
                        Console.PrintLine("b");
                #endif
                #if SIGMA_3 != NOTHING // a comment closes the line
                        Console.PrintLine("c");
                #endif
                    }
                }
                """));
    }

    @Test
    void define_andUndefChangeTheSymbolsForTheRestOfTheFile() {
        assertEquals(List.of("loud", "quiet"), run("""
                #define LOUD
                class Tool {
                    static void Main() {
                #if LOUD
                        Console.PrintLine("loud");
                #endif
                #undef LOUD
                #if LOUD
                        Console.PrintLine("still loud");
                #else
                        Console.PrintLine("quiet");
                #endif
                    }
                }
                """));
    }

    @Test
    void define_inLinesLeftOutDefinesNothing() {
        assertEquals(List.of("never defined"), run("""
                #if NOTHING
                #define LOUD
                #endif
                class Tool {
                    static void Main() {
                #if LOUD
                        Console.PrintLine("loud");
                #else
                        Console.PrintLine("never defined");
                #endif
                    }
                }
                """));
    }

    @Test
    void compile_definesTheSymbolsItIsHanded() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + """
                class Tool {
                    static void Main() {
                #if MODERN && TRACE_ON
                        Console.PrintLine("both");
                #endif
                    }
                }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, SigmaVersions.NEWEST,
                List.of("MODERN", "TRACE_ON"));
        assertEquals(List.of("both"), ran(built));
    }

    @Test
    void error_stopsTheBuildWithItsWordsAndWarningDoesNot() {
        final List<String> said = errors("""
                #if SIGMA_3
                #error not on this machine
                #endif
                class Tool { static void Main() { } }
                """);
        assertTrue(said.stream().anyMatch(line -> line.contains("S1007") && line.contains("not on this machine")),
                said::toString);
        final SigmaCompiler.Result warned = built("""
                #warning still to do
                class Tool { static void Main() { } }
                """);
        assertTrue(warned.ok(), () -> String.join("\n", warned.lines()));
        assertTrue(warned.lines().stream().anyMatch(line -> line.contains("S1007") && line.contains("still to do")),
                warned.lines()::toString);
    }

    @Test
    void error_inLinesLeftOutSaysNothing() {
        assertTrue(built("""
                #if NOTHING
                #error never read
                #endif
                class Tool { static void Main() { } }
                """).ok());
    }

    @Test
    void directive_refusesWhatItCannotRead() {
        final List<String> said = errors("""
                #pragma warning disable
                #define 9LIVES
                #if SIGMA_3 &&
                #endif
                #if (SIGMA_3
                #endif
                class Tool { static void Main() { } }
                """);
        assertEquals(2, said.stream().filter(line -> line.contains("S1008")).count(), said::toString);
        assertEquals(2, said.stream().filter(line -> line.contains("S1009")).count(), said::toString);
    }

    @Test
    void directive_refusesBranchesOutOfPlace() {
        final List<String> said = errors("""
                #endif
                #else
                #if SIGMA_3
                #else
                #else
                #endif
                #if SIGMA_3
                class Tool { static void Main() { } }
                """);
        assertEquals(2, said.stream().filter(line -> line.contains("S1010")).count(), said::toString);
        assertEquals(1, said.stream().filter(line -> line.contains("S1012")).count(), said::toString);
        assertEquals(1, said.stream().filter(line -> line.contains("S1011")).count(), said::toString);
    }

    @Test
    void directive_isRefusedByAnOlderVersion() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Tool.sgs", PRELUDE + """
                #if X86
                #endif
                class Tool { static void Main() { } }
                """)), AsmProgram.DEFAULT_ISA, LanguageLevel.SIGMA_SHARP, 2);
        assertEquals(2, built.lines().stream().filter(line -> line.contains("S3057")).count(),
                built.lines()::toString);
    }

    @Test
    void sigma3_isNotDefinedForAnOlderVersion() {
        assertEquals(Set.of(), SigmaVersions.defined(2));
        assertEquals(Set.of("SIGMA_3"), SigmaVersions.defined(3));
    }

    @Test
    void symbolOf_isTheIdsPathInCapitals() {
        assertEquals("X86", SigmaVersions.symbolOf("jsc:x86"));
        assertEquals("EARLY_MODERN", SigmaVersions.symbolOf("jscore:early-modern"));
        assertEquals("ARM", SigmaVersions.symbolOf("arm"));
    }

    @Test
    void lexer_recordsDirectivesAndTheLinesLeftOut() {
        final Lexer lexer = new Lexer(new SourceFile("Tool.sgs", """
                int a;
                #if NOTHING
                int b;
                int c;
                #endif
                  #if SIGMA_3
                int d;
                #endif
                """), new DiagnosticBag("Tool.sgs"), SigmaVersions.defined(SigmaVersions.NEWEST));
        final long names = lexer.tokenize().stream().filter(token -> token.kind() == TokenKind.IDENTIFIER).count();
        assertEquals(2, names, "only a and d are read");
        assertEquals(List.of(3, 4), lexer.preprocessor().leftOutLines());
        final List<Preprocessor.Directive> directives = lexer.preprocessor().directives();
        assertEquals(List.of("if", "endif", "if", "endif"),
                directives.stream().map(Preprocessor.Directive::word).toList());
        assertEquals(6, directives.get(2).line());
        assertEquals(3, directives.get(2).column());
    }

    @Test
    void projectFile_keepsItsDefinesThroughTheDisk() {
        final ProjectFile project = new ProjectFile("Tool", ProjectFile.Kind.CONSOLE, "jsc:sigma_sharp",
                List.of("Tool.sgs"), List.of(), ProjectFile.defaultEntry("Tool"), "jsc:x86", 0,
                List.of("TRACE_ON", "LOUD"));
        final ProjectFile read = ProjectFile.read(project.write());
        assertEquals(List.of("TRACE_ON", "LOUD"), read.defines());
        assertEquals(project, read);
        assertEquals(List.of(), ProjectFile.read("name: Old\nkind: console\n").defines());
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
        return ran(built(source));
    }

    private static List<String> ran(final SigmaCompiler.Result result) {
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
