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
 * The words version 3 of the language brought that the versions before it kept back for it, C#'s try, catch, finally,
 * throw, goto and const and INTERCAL's comefrom, abstainfrom and reinstate, are no names: wherever one is written as
 * one, the program is told so at the word. A catch or a finally with no try before it is told it needs one.
 */
class LanguageGapsTest {

    private static final String PRELUDE = "using System.*; using System.IO.*; namespace Tests; ";
    private static final String NOT_IN_THE_LANGUAGE = "S2014";
    private static final String EXPECTED_TOKEN = "S2001";

    private static List<String> codes(final String source) {
        return SigmaSemantics.checkProgram(List.of(new SourceFile("Test.sgs", PRELUDE + source))).diagnostics()
                .stream().map(Diagnostic::code).toList();
    }

    private static String inMain(final String body) {
        return "class M { static void Main() { " + body + " } }";
    }

    @Test
    void catch_withNoTryBeforeItIsToldItNeedsOne() {
        assertEquals(List.of(EXPECTED_TOKEN), codes(inMain("catch (Exception failed) { } finally { }")));
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
