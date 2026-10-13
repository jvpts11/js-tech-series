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
import dev.jstech.computers.sigma.SigmaSemantics;
import dev.jstech.computers.sigma.SourceFile;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The words of access hold as C# holds them: a private member, which is what a member written with no word is, reached
 * only from its own type and the types nested in it, a protected one from the types that stand on it as well, a public
 * or an internal one from anywhere in the program.
 */
class MemberCheckerTest {

    private static final String PRELUDE = "using System.*; using System.IO.*; namespace Tests; ";
    private static final String NOT_ACCESSIBLE = "S3060";

    private static List<String> codes(final String source) {
        return SigmaSemantics.check(List.of(new SourceFile("Test.sgs", PRELUDE + source))).diagnostics().stream()
                .map(Diagnostic::code).toList();
    }

    private static void assertRefused(final String source) {
        final List<String> found = codes(source);
        assertTrue(found.contains(NOT_ACCESSIBLE), () -> "it was reached: " + found);
    }

    private static void assertClean(final String source) {
        assertEquals(List.of(), codes(source));
    }

    @Test
    void privateField_isNotReachedFromAnotherClass() {
        assertRefused("class Box { private int size; } "
                + "class M { static void Main() { Box b = new Box(); int n = b.size; } }");
    }

    @Test
    void memberWithNoWord_isPrivateAsInCSharp() {
        assertRefused("class Box { int size; } class M { static void Main() { Box b = new Box(); int n = b.size; } }");
        assertRefused("class Box { void Shake() { } } class M { static void Main() { new Box().Shake(); } }");
    }

    @Test
    void privateMember_isReachedFromItsOwnClassAndTheTypesNestedInIt() {
        assertClean("class Box { private int size; public int Twice() { return size * 2; } "
                + "class Lid { public int Of(Box b) { return b.size; } } } "
                + "class M { static void Main() { int n = new Box().Twice(); } }");
    }

    @Test
    void protectedMember_isReachedFromADerivedClassOnly() {
        assertClean("class Base { protected int level; } "
                + "class Below : Base { public int Read() { return level; } } "
                + "class M { static void Main() { int n = new Below().Read(); } }");
        assertRefused("class Base { protected int level; } "
                + "class M { static void Main() { Base b = new Base(); int n = b.level; } }");
    }

    @Test
    void publicAndInternalMembers_areReachedFromAnywhere() {
        assertClean("class Box { public int size; internal int depth; } "
                + "class M { static void Main() { Box b = new Box(); int n = b.size + b.depth; } }");
    }

    @Test
    void privateConstructor_keepsTheTypeFromBeingMadeElsewhere() {
        assertRefused("class Box { private Box() { } } class M { static void Main() { Box b = new Box(); } }");
        assertClean("class Box { public Box() { } } class M { static void Main() { Box b = new Box(); } }");
    }

    @Test
    void overloads_areChosenAmongThoseReachable() {
        assertClean("class Box { private void Put(int n) { } public void Put(string s) { } } "
                + "class M { static void Main() { new Box().Put(\"a\"); } }");
    }
}
