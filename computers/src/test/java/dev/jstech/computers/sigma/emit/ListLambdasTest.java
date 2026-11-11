/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

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
import dev.jstech.computers.vm.program.Values;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListLambdasTest {

    private static final long ROOM = 256L * 1024;
    private static final int PLENTY = 1_000_000;
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using Standard.Script; namespace Tests; ";

    @Test
    void find_givesTheFirstElementTheTestSaysYesTo() {
        assertEquals(List.of("12", "0", "bob", "null"), printed("""
                List<int> numbers = new List<int>(); numbers.Add(3); numbers.Add(12); numbers.Add(15);
                Console.PrintLine("" + numbers.Find(n => n > 10));
                Console.PrintLine("" + numbers.Find(n => n > 100));
                List<string> names = new List<string>(); names.Add("ann"); names.Add("bob");
                Console.PrintLine(names.Find(n => n.StartsWith("b")));
                Console.PrintLine(names.Find(n => n == "zed") == null ? "null" : "found");
                """));
    }

    @Test
    void find_seesWhatTheLambdaCaught() {
        assertEquals(List.of("7"), printed("""
                List<int> numbers = new List<int>(); numbers.Add(5); numbers.Add(7); int least = 6;
                Console.PrintLine("" + numbers.Find(n => n >= least));
                """));
    }

    @Test
    void sort_ordersByTheComparisonAndKeepsEqualOnesInOrder() {
        assertEquals(List.of("1 2 3 5 8", "8 5 3 2 1", "bb:1 aa:2 cc:2"), printed("""
                List<int> numbers = new List<int>();
                numbers.Add(5); numbers.Add(2); numbers.Add(8); numbers.Add(1); numbers.Add(3);
                numbers.Sort((a, b) => a - b);
                Console.PrintLine(numbers[0] + " " + numbers[1] + " " + numbers[2] + " " + numbers[3] + " "
                        + numbers[4]);
                numbers.Sort((a, b) => b - a);
                Console.PrintLine(numbers[0] + " " + numbers[1] + " " + numbers[2] + " " + numbers[3] + " "
                        + numbers[4]);
                List<string> pairs = new List<string>(); pairs.Add("aa:2"); pairs.Add("bb:1"); pairs.Add("cc:2");
                pairs.Sort((a, b) => Convert.ToInt(a.Substring(3)) - Convert.ToInt(b.Substring(3)));
                Console.PrintLine(pairs[0] + " " + pairs[1] + " " + pairs[2]);
                """));
    }

    @Test
    void sort_leavesAnEmptyListAndOneOfOneAsTheyWere() {
        assertEquals(List.of("0", "9"), printed("""
                List<int> none = new List<int>(); none.Sort((a, b) => a - b);
                Console.PrintLine("" + none.Count);
                List<int> one = new List<int>(); one.Add(9); one.Sort((a, b) => a - b);
                Console.PrintLine("" + one[0]);
                """));
    }

    @Test
    void sort_withNoComparisonStillSortsInJava() {
        assertEquals(List.of("1 2 3"), printed("""
                List<int> numbers = new List<int>(); numbers.Add(3); numbers.Add(1); numbers.Add(2);
                numbers.Sort();
                Console.PrintLine(numbers[0] + " " + numbers[1] + " " + numbers[2]);
                """));
    }

    @Test
    void find_isRefusedBeforeTheThirdVersion() {
        final SigmaCompiler.Result built = compiled("List<int> n = new List<int>(); int f = n.Find(x => x > 1);",
                2);
        assertFalse(built.ok(), "the second version has no Find with a function");
        assertTrue(String.join("\n", built.lines()).contains("S3057"), () -> String.join("\n", built.lines()));
    }

    private static SigmaCompiler.Result compiled(final String body, final int version) {
        final String source = PRELUDE + "class Says : Script { public override void OnTick() { " + body + " } }";
        return SigmaCompiler.compile(List.of(new SourceFile("Says.sgs", source)), AsmProgram.DEFAULT_ISA,
                LanguageLevel.SIGMA_SHARP, version);
    }

    /** What the program printed, a line to an entry. */
    private static List<String> printed(final String body) {
        final SigmaCompiler.Result built = compiled(body, 3);
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems(), () -> String.valueOf(reader.problems()));
        final Process process = new Process(program, ROOM, IHost.still());
        final Values.Obj self = process.create(program.entryPoint());
        process.begin(self, "OnTick");
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        return process.console();
    }
}
