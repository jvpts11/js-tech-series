/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.Diagnostic;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaSemantics;
import dev.jstech.computers.sigma.SourceFile;
import java.util.List;
import org.junit.jupiter.api.Test;

class VersionRulesTest {

    private static final String PRELUDE = "using System.*; using System.IO.*; namespace Tests; ";
    private static final String NOT_IN_THE_LANGUAGE = "S2014";

    /** A const local is reported and then declared all the same, so the lines after it read it without more noise. */
    @Test
    void check_refusesAConstLocalTheWayVersionTwoDid() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE),
                codes(2, inMain("const int most = 5; Console.PrintLine(\"\" + most);")));
    }

    @Test
    void check_refusesAConstFieldTheWayVersionTwoDid() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE),
                codes(2, "class Box { public const int Most = 5; } class M { static void Main() { } }"));
    }

    @Test
    void check_refusesGotoAndComefromTheWayVersionTwoDid() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes(2, inMain("goto again;")));
        assertEquals(List.of(NOT_IN_THE_LANGUAGE), codes(2, inMain("comefrom again; Console.PrintLine(\"a\");")));
    }

    /** Each is reported once, so the statements around them still read without more noise. */
    @Test
    void check_refusesAbstainfromAndReinstateTheWayVersionTwoDid() {
        assertEquals(List.of(NOT_IN_THE_LANGUAGE, NOT_IN_THE_LANGUAGE),
                codes(2, inMain("abstainfrom done; Console.PrintLine(\"a\"); reinstate done;")));
    }

    @Test
    void check_acceptsWhatVersionThreeBrings() {
        final List<String> found = codes(3, "class Box { public const int Most = 5; } class M {"
                + " static void Main() { const int most = Box.Most; Console.PrintLine(\"\" + most); } }");
        assertTrue(found.isEmpty(), found::toString);
    }

    private static List<String> codes(final int version, final String source) {
        return SigmaSemantics.checkProgram(List.of(new SourceFile("Test.sgs", PRELUDE + source)),
                LanguageLevel.SIGMA_SHARP, version).diagnostics().stream().map(Diagnostic::code).toList();
    }

    private static String inMain(final String body) {
        return "class M { static void Main() { " + body + " } }";
    }
}
