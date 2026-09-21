/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.computers.vm.program.Values;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * printf, the one call written with no type in front of it: read while the program is compiled, in both
 * languages, and gone by the time it runs, where it is the console's own call and nothing else.
 */
class PrintfTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;

    private static final String PRELUDE = "using Standard.*; namespace Tests; ";

    private static SigmaCompiler.Result built(final String body, final LanguageLevel level) {
        final String source = PRELUDE + "class Says : Script { public override void OnTick() { " + body + " } }";
        return SigmaCompiler.compile(List.of(new SourceFile(level.full() ? "Says.sgs" : "Says.sg", source)),
                level.full() ? AsmProgram.DEFAULT_ARCHITECTURE : "jsc:x86_16", level);
    }

    /** What the program printed, a line to an entry, in the smaller language. */
    private static List<String> printed(final String body) {
        final SigmaCompiler.Result built = built(body, LanguageLevel.SIGMA);
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

    private static String refusal(final String body) {
        final SigmaCompiler.Result built = built(body, LanguageLevel.SIGMA);
        assertFalse(built.ok(), "it was taken");
        return String.join("\n", built.lines());
    }

    @Test
    void printf_printsItsFormatAndALineBreakEndsTheLine() {
        assertEquals(List.of("hello"), printed("printf(\"hello\\n\");"));
    }

    @Test
    void printf_putsAValueInEveryHole() {
        assertEquals(List.of("silo has 3 of 2.5, a, 100%"), printed("int n = 3; double f = 2.5; char c = 'a'; "
                + "printf(\"%s has %d of %f, %c, 100%%\\n\", \"silo\", n, f, c);"));
    }

    @Test
    void printf_aBreakInTheMiddleIsTwoLines_andOneByItselfIsAnEmptyOne() {
        assertEquals(List.of("one", "two", ""), printed("printf(\"one\\ntwo\\n\"); printf(\"\\n\");"));
    }

    @Test
    void printf_aWholeNumberDoesForAFraction() {
        assertEquals(1, printed("int n = 4; printf(\"%f\\n\", n);").size());
    }

    /** Both languages have it, and it is one program whichever of them read it. */
    @Test
    void printf_isTheSameListingInBothLanguages() {
        final List<SourceFile> source = List.of(new SourceFile("Says.sg", PRELUDE
                + "class Says : Script { public override void OnTick() { int n = 3; printf(\"%d items\\n\", n); } }"));
        final SigmaCompiler.Result small = SigmaCompiler.compile(source, "jsc:x86_16", LanguageLevel.SIGMA);
        final SigmaCompiler.Result big = SigmaCompiler.compile(source, "jsc:x86_16", LanguageLevel.SIGMA_SHARP);
        assertTrue(small.ok(), () -> String.join("\n", small.lines()));
        assertTrue(big.ok(), () -> String.join("\n", big.lines()));
        assertEquals(small.assembly(), big.assembly());
    }

    /** Nothing of it is left to run: what is written down is the console's own call over the pieces joined. */
    @Test
    void printf_isWrittenDownAsTheConsolesOwnCall() {
        final String listing = built("int n = 3; printf(\"%d items\\n\", n);", LanguageLevel.SIGMA).assembly();
        assertTrue(listing.contains("Console") && listing.contains("Print"), listing);
        assertFalse(listing.contains("printf"), listing);
    }

    @Test
    void printf_aFormatThatIsNotWrittenOutIsRefused() {
        assertTrue(refusal("string f = \"%d\"; printf(f, 1);").contains("S3053"));
        assertTrue(refusal("printf();").contains("S3053"));
    }

    @Test
    void printf_aFormatItCannotReadIsRefusedWithWhy() {
        assertTrue(refusal("printf(\"%5d\\n\", 1);").contains("no widths or precisions"));
        assertTrue(refusal("printf(\"%q\\n\", 1);").contains("S3054"));
    }

    @Test
    void printf_aHoleWithNoValueOrAValueWithNoHoleIsRefused() {
        assertTrue(refusal("printf(\"%d and %d\\n\", 1);").contains("2 holes and the call gives 1 value"));
        assertTrue(refusal("printf(\"done\\n\", 1);").contains("S3055"));
    }

    @Test
    void printf_aValueOfTheWrongKindIsRefusedWhereItWasWritten() {
        final String said = refusal("printf(\"%d\\n\", \"three\");");
        assertTrue(said.contains("S3056") && said.contains("'%d' takes a whole number"), said);
        assertTrue(refusal("printf(\"%s\\n\", 3);").contains("'%s' takes text"));
        assertTrue(refusal("printf(\"%c\\n\", 3);").contains("S3056"));
    }

    /** A program with a printf of its own calls its own, as with any other name. */
    @Test
    void printf_ofTheProgramsOwnIsTheOneCalled() {
        final SigmaCompiler.Result own = SigmaCompiler.compile(List.of(new SourceFile("Says.sg", PRELUDE
                + "class Says : Script { void printf(int n) { } public override void OnTick() { printf(4); } }")),
                "jsc:x86_16", LanguageLevel.SIGMA);
        assertTrue(own.ok(), () -> String.join("\n", own.lines()));
    }
}
