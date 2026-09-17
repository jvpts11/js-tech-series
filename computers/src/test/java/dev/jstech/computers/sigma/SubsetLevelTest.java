/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What the smaller language does not have, and the fact that everything it does have is the bigger one too.
 *
 * <p>The second half is the one worth guarding: a subset that quietly meant something else on a newer machine
 * would not be a subset at all. So every source here that the smaller language takes is checked against the
 * bigger one as well, in the same test.
 */
class SubsetLevelTest {

    /**
     * What every file starts with, on one line so the sources keep their line numbers.
     *
     * <p>One using, because the smaller language has one library. That it is also enough for the full language
     * is the point: these sources are handed to both, and the same line has to open the same types in each.
     */
    private static final String PRELUDE = "using Standard.*; namespace Tests; ";

    /**
     * The same, for the sources that reach into the full language's own library.
     *
     * <p>Those are the tests of things the subset does not have at all, and some of them are types rather than
     * shapes, so the source has to be able to name them for the full language to take it. The subset then has two
     * things to say about that source, the library it reached for and the thing it wrote, which is correct.
     */
    private static final String FULL_PRELUDE =
            "using Standard.*; using System.*; using System.Collections.*; namespace Tests; ";

    private static SigmaSemantics.Result subset(final String source) {
        return subset(PRELUDE, source);
    }

    private static SigmaSemantics.Result subset(final String prelude, final String source) {
        return SigmaSemantics.checkProgram(List.of(new SourceFile("Test.sg", prelude + source)),
                LanguageLevel.SIGMA);
    }

    private static SigmaSemantics.Result full(final String source) {
        return full(PRELUDE, source);
    }

    private static SigmaSemantics.Result full(final String prelude, final String source) {
        return SigmaSemantics.checkProgram(List.of(new SourceFile("Test.sgs", prelude + source)));
    }

    /** Takes it in the subset, and proves the same source is the full language too. */
    private static void assertInBoth(final String source) {
        final SigmaSemantics.Result small = subset(source);
        assertTrue(small.ok(), () -> "the subset refused it: " + String.join("\n", small.lines()));
        final SigmaSemantics.Result big = full(source);
        assertTrue(big.ok(), () -> "the full language refused it: " + String.join("\n", big.lines()));
    }

    /** Refused in the subset for being outside it, and accepted by the full language. */
    private static void assertOnlyInTheFullLanguage(final String source) {
        assertOnlyInTheFullLanguage(PRELUDE, source);
    }

    private static void assertOnlyInTheFullLanguage(final String prelude, final String source) {
        final SigmaSemantics.Result small = subset(prelude, source);
        assertTrue(small.diagnostics().stream().anyMatch(d -> "S3052".equals(d.code())),
                () -> "the subset took it: " + String.join("\n", small.lines()));
        final SigmaSemantics.Result big = full(prelude, source);
        assertTrue(big.ok(), () -> "the full language refused it: " + String.join("\n", big.lines()));
    }

    @Test
    void subset_takesAClassWithFieldsMethodsAndALoop() {
        assertInBoth("class M { static void Main() { int total = 0; "
                + "for (int i = 0; i < 4; i = i + 1) { total = total + i; } } }");
    }

    @Test
    void subset_takesAClassStandingOnAnotherWithVirtualAndOverride() {
        assertInBoth("class Base { public virtual int Value() { return 1; } } "
                + "class Below : Base { public override int Value() { return 2; } } "
                + "class M { static void Main() { Base b = new Below(); int v = b.Value(); } }");
    }

    @Test
    void subset_takesAStructAnEnumAndAnArray() {
        assertInBoth("struct Point { public int X; } enum Colour { Red, Blue } "
                + "class M { static void Main() { int[] numbers = new int[4]; numbers[0] = 1; "
                + "Point p = new Point(); Colour c = Colour.Red; } }");
    }

    @Test
    void subset_takesAScriptStandingOnTheBaseClass() {
        assertInBoth("class Watch : Script { public override void OnTick() { } }");
    }

    @Test
    void subset_hasNoInterfaces() {
        assertOnlyInTheFullLanguage("interface IThing { int Value(); } "
                + "class Below : IThing { public int Value() { return 2; } } "
                + "class M { static void Main() { } }");
    }

    @Test
    void subset_hasNoRecords() {
        assertOnlyInTheFullLanguage("record Point(int X, int Y); class M { static void Main() { } }");
    }

    @Test
    void subset_hasNoDelegatesOrEvents() {
        assertOnlyInTheFullLanguage("delegate void Told(); class M { static void Main() { } }");
        assertOnlyInTheFullLanguage(FULL_PRELUDE, "class Bell { public event Action Rang; } "
                + "class M { static void Main() { } }");
    }

    @Test
    void subset_hasNoProperties() {
        assertOnlyInTheFullLanguage("class Box { public int Size { get; private set; } } "
                + "class M { static void Main() { } }");
    }

    @Test
    void subset_hasNoAbstractClassesOrMethods() {
        assertOnlyInTheFullLanguage("abstract class Shape { public abstract int Sides(); } "
                + "class Square : Shape { public override int Sides() { return 4; } } "
                + "class M { static void Main() { } }");
    }

    @Test
    void subset_hasNoForeach() {
        assertOnlyInTheFullLanguage("class M { static void Main() { int[] n = new int[2]; "
                + "foreach (int x in n) { } } }");
    }

    @Test
    void subset_hasNoLambdas() {
        assertOnlyInTheFullLanguage(FULL_PRELUDE, "class M { static void Main() { Action a = () => { }; } }");
    }

    @Test
    void subset_hasNoListOrMap() {
        assertOnlyInTheFullLanguage(FULL_PRELUDE,
                "class M { static void Main() { List<int> n = new List<int>(); } }");
    }

    @Test
    void subset_hasNoVar() {
        assertOnlyInTheFullLanguage("class M { static void Main() { var n = 1; } }");
    }

    @Test
    void subset_hasNoLock() {
        assertOnlyInTheFullLanguage("class M { static void Main() { object o = null; lock (o) { } } }");
    }

    /**
     * The one the checker cannot see, so it is refused while the string is still a string.
     *
     * <p>A string with holes in it is read into the same additions somebody would have written by hand, so by
     * the time there is a tree to walk there is nothing left that says which it was.
     */
    @Test
    void subset_hasNoStringsWithHolesInThem() {
        assertOnlyInTheFullLanguage("class M { static void Main() { int n = 1; string s = $\"n is {n}\"; } }");
    }

    /** One using opens the whole of the subset's library, and the same line opens it in the full language too. */
    @Test
    void subset_reachesItsLibraryWithTheOneUsing() {
        assertInBoth("class M { static void Main() { Console.PrintLine(\"hello\"); } }");
    }

    /** The library is the same types the full language has, so the one call compiles the same on either side. */
    @Test
    void subset_callsTheSameLibraryTheFullLanguageDoes() {
        final String source = "class M { static void Main() { Console.PrintLine(Convert.ToString(Math.Abs(-2))); } }";
        assertInBoth(source);
    }

    @Test
    void subset_hasOnlyPartOfEachTypeItHas() {
        assertOnlyInTheFullLanguage("class M { static void Main() { long n = Console.ReadLong(); } }");
        assertOnlyInTheFullLanguage("class M { static void Main() { double n = Math.Round(1.5); } }");
    }

    @Test
    void subset_namesWhatItsOwnVersionOfATypeHas() {
        final SigmaSemantics.Result result = subset(
                "class M { static void Main() { long n = Console.ReadLong(); } }");
        assertTrue(result.lines().stream().anyMatch(line -> line.contains("PrintLine")),
                () -> "the message does not say what Console has: " + String.join("\n", result.lines()));
    }

    @Test
    void subset_cannotReachTheFullLanguagesLibraryAtAll() {
        final SigmaSemantics.Result result = subset(FULL_PRELUDE, "class M { static void Main() { } }");
        assertTrue(result.lines().stream().anyMatch(line -> line.contains("Standard")),
                () -> "the message does not point at the one library: " + String.join("\n", result.lines()));
    }

    /** Writing the name out in full walks past the refused using, so it is caught where the chain starts. */
    @Test
    void subset_cannotReachTheFullLibraryByWritingItsNameOutInFull() {
        final SigmaSemantics.Result result = subset(
                "class M { static void Main() { int n = System.Utils.Random.Next(4); } }");
        assertTrue(result.diagnostics().stream().anyMatch(d -> "S3052".equals(d.code())),
                () -> "it went through: " + String.join("\n", result.lines()));
    }

    @Test
    void subset_saysWhatToWriteInstead() {
        final SigmaSemantics.Result result = subset(
                "class M { static void Main() { int[] n = new int[2]; foreach (int x in n) { } } }");
        assertTrue(result.lines().stream().anyMatch(line -> line.contains("Length")),
                () -> "the message does not say what to write instead: " + String.join("\n", result.lines()));
    }

    /** The full language is not narrowed by any of this: it goes on taking everything it took before. */
    @Test
    void fullLanguage_isLeftAlone() {
        final SigmaSemantics.Result result = full(FULL_PRELUDE,
                "class M { static void Main() { List<int> n = new List<int>(); "
                        + "var total = 0; foreach (int x in n) { total = total + x; } } }");
        assertEquals(List.of(), result.diagnostics().stream().map(Diagnostic::code).toList());
    }
}
