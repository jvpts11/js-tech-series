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
 * The words a later version of the language brings, C#'s try, catch, finally, throw, goto and const and INTERCAL's
 * comefrom, abstainfrom and reinstate, are reserved now: wherever one is written, the program is told so at the word,
 * with what to write until then.
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

    @Test
    void comefrom_isToldToBeALoop() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes(inMain("comefrom again; Console.PrintLine(\"a\");")));
    }

    /** Each is reported once and passed over, so the statements around them still read without more noise. */
    @Test
    void abstainfromAndReinstate_areToldToKeepABool() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE, NOT_IN_THE_LANGUAGE),
                codes(inMain("abstainfrom done; Console.PrintLine(\"a\"); reinstate done;")));
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

    /** The words are kept for the later version that brings them, so no program takes one as a name. */
    @Test
    void theWords_areNoNames() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes(inMain("int try = 1;")));
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes("class Box { public int goto; } "
                + "class M { static void Main() { } }"));
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes(inMain("int reinstate = 1;")));
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes("class Box { public int comefrom; } "
                + "class M { static void Main() { } }"));
    }

    /** C#'s internal is taken as the other words of access are, before a type and before a member. */
    @Test
    void internal_isTakenAsAWordOfAccess() {
        final List<String> found = codes("internal class Box { internal int Size; internal static int Count() {"
                + " return 0; } } class M { static void Main() { Box b = new Box(); b.Size = Box.Count(); } }");
        assertTrue(found.isEmpty(), () -> "internal was refused: " + found);
    }
}
