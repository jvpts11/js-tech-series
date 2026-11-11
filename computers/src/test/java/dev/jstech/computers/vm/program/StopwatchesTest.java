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

class StopwatchesTest {

    @Test
    void stopwatch_readsTheCreditsSpentSinceItStartedAndStartsAgain() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Says.sgs",
                "using System.*; using System.IO.*; using System.Utils.*; using Standard.Script; namespace Tests; "
                        + "class Says : Script { public override void OnTick() { "
                        + "Stopwatch watch = Stopwatch.Start(); int sum = 0; "
                        + "for (int i = 0; i < 100; i++) { sum = sum + i; } "
                        + "long spent = watch.Credits; watch.Restart(); "
                        + "Console.PrintLine((spent > 100 ? \"spent\" : \"idle\") + \" \" + watch.Ticks + \" \" "
                        + "+ (watch.Credits < spent ? \"again\" : \"not again\")); } }")));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final ProgramImage program = ProgramImage.of(reader.read());
        assertFalse(reader.hasProblems());
        final Process process = new Process(program, 64L * 1024, IHost.still());
        process.begin(process.create(program.entryPoint()), "OnTick");
        process.step(1_000_000);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals(List.of("spent 0 again"), process.console());
    }
}
