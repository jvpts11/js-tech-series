/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.Diagnostic;
import dev.jstech.computers.sigma.SigmaSemantics;
import dev.jstech.computers.sigma.SourceFile;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What C# has and the language has not, written as C# writes it, is told for what it is; the same words used as
 * names stay names.
 */
class LanguageGapsTest {

    private static final String PRELUDE = "using System.*; using System.IO.*; namespace Tests; ";
    private static final String NOT_IN_THE_LANGUAGE = "S2014";

    private static List<String> codes(final String source) {
        return SigmaSemantics.checkProgram(List.of(new SourceFile("Test.sgs", PRELUDE + source))).diagnostics()
                .stream().map(Diagnostic::code).toList();
    }

    private static String inMain(final String body) {
        return "class M { static void Main() { " + body + " } }";
    }

    @Test
    void tryAndCatch_areToldThereAreNoExceptions() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE),
                codes(inMain("try { Console.PrintLine(\"a\"); } catch (Failure e) { } finally { }")));
    }

    @Test
    void throw_isToldThereAreNoExceptions() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes(inMain("throw new Failure(\"no\");")));
    }

    @Test
    void goto_isToldToBeALoop() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes(inMain("goto again;")));
    }

    /** A const local is reported and then declared all the same, so the lines after it read it without more noise. */
    @Test
    void constLocal_isReportedAndStillDeclared() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE),
                codes(inMain("const int most = 5; Console.PrintLine(\"\" + most);")));
    }

    @Test
    void constField_isToldToBeStaticReadonly() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE),
                codes("class Box { public const int Most = 5; } class M { static void Main() { } }"));
    }

    /** The words are not the language's own, so they still name things where C# would not have them. */
    @Test
    void theWords_stayNamesElsewhere() {
        assertEquals(List.of(), codes(inMain("int try = 1; try = try + 1; int goto = try; int const = goto;")));
    }

    /** C#'s internal is taken as the other words of access are, before a type and before a member. */
    @Test
    void internal_isTakenAsAWordOfAccess() {
        final List<String> found = codes("internal class Box { internal int Size; internal static int Count() {"
                + " return 0; } } class M { static void Main() { Box b = new Box(); b.Size = Box.Count(); } }");
        assertTrue(found.isEmpty(), () -> "internal was refused: " + found);
    }
}
