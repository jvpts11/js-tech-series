/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class PatternCallsTest {

    private static final long ROOM = 256L * 1024;
    private static final int PLENTY = 2_000_000;
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Text.*; using Standard.Script; namespace Tests; ";

    @Test
    void regex_matchesFindsReplacesAndSplits() {
        final Process process = ran("""
                Console.PrintLine(Regex.IsMatch("ore x64", "x(\\\\d+)") ? "yes" : "no");
                List<string> groups = Regex.Match("ore x64", "x(\\\\d+)");
                Console.PrintLine(groups[0] + " " + groups[1]);
                List<string> all = Regex.Matches("a1 b22 c333", "\\\\d+");
                Console.PrintLine(all[0] + all[1] + all[2]);
                Console.PrintLine(Regex.Replace("ore x64", "x(\\\\d+)", "$1 items"));
                List<string> parts = Regex.Split("a, b,c", ",\\\\s*");
                Console.PrintLine(parts[0] + parts[1] + parts[2] + " " + parts.Count);
                Console.PrintLine("" + Regex.Match("none", "\\\\d").Count);
                """);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals(List.of("yes", "x64 64", "122333", "ore 64 items", "abc 3", "0"), process.console());
    }

    @Test
    void regex_stopsAPatternThatReadsTheTextWithoutEnd() {
        // Sixty letters read by this pattern some two thousand times over; ten more pushed it to millions of times.
        final Process process = ran("""
                string text = "";
                for (int i = 0; i < 60; i++) { text = text + "x"; }
                Console.PrintLine(Regex.IsMatch(text, "(x+x+)+y") ? "y" : "n");
                Console.PrintLine("never");
                """);
        assertEquals(Process.State.HALTED, process.state());
        assertTrue(String.valueOf(process.message()).contains("(x+x+)+y"), () -> String.valueOf(process.message()));
        assertFalse(process.console().contains("never"));
    }

    @Test
    void regex_haltsOnWhatIsNoPattern() {
        final Process process = ran("Console.PrintLine(Regex.IsMatch(\"x\", \"(\") ? \"y\" : \"n\");");
        assertEquals(Process.State.HALTED, process.state());
    }

    @Test
    void regex_haltsOnANullPatternRatherThanMatchingTheWordNull() {
        final Process process = ran("""
                string pattern = null;
                Console.PrintLine(Regex.IsMatch("null and void", pattern) ? "y" : "n");
                """);
        assertEquals(Process.State.HALTED, process.state());
        assertFalse(process.console().contains("y"), "nothing was matched against the word null");
    }

    /** A program run to its end or its halt. */
    private static Process ran(final String body) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Says.sgs", PRELUDE
                + "class Says : Script { public override void OnTick() { " + body + " } }")));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems(), () -> String.valueOf(reader.problems()));
        final Process process = new Process(program, ROOM, IHost.still());
        process.begin(process.create(program.entryPoint()), "OnTick");
        process.step(PLENTY);
        return process;
    }
}
