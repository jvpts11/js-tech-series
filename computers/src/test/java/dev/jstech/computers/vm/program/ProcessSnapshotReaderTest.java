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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessSnapshotReaderTest {

    private static final String SOURCE = "using System.*; using System.Collections.*; namespace Tests; "
            + "class Monitor : IScript {\n"
            + "    public void OnInit() { }\n"
            + "    public void OnTick() {\n"
            + "        List<string> names = new List<string>(); List<string> also = names; Inner();\n"
            + "    }\n"
            + "    void Inner() { while (true) { } }\n"
            + "    public void OnDestroy() { }\n}\n";

    private static final ProgramImage PROGRAM = load();

    private static ProgramImage load() {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Monitor.sgs", SOURCE)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram listing = reader.read();
        assertFalse(reader.hasProblems(), () -> String.join("\n",
                reader.problems().stream().map(ListingProblem::format).toList()));
        return ProgramImage.of(listing);
    }

    /** A process stopped inside {@code Inner}, called from {@code OnTick}, written down and read back. */
    private static Process readBack() {
        final Process process = new Process(PROGRAM, 64L * 1024, IHost.still());
        process.begin(process.create(PROGRAM.entryPoint()), "OnTick");
        process.step(200);
        return ProcessSnapshotReader.read(PROGRAM, ProcessSnapshotWriter.write(process), IHost.still());
    }

    @Test
    void read_bringsBackAThreadsCallsWithTheInnermostOnTop() {
        final ProgramThread main = readBack().mainThread();

        assertEquals(2, main.frames.size());
        assertEquals("Inner", main.frames.peek().method.name());
    }

    @Test
    void read_bringsBackTwoNamesForOneObjectAsOneObject() {
        final List<Frame> stack = new ArrayList<>(readBack().mainThread().frames);
        final Frame tick = stack.getLast();

        final List<Object> lists = Arrays.stream(tick.slots).filter(Values.ListValue.class::isInstance).toList();

        assertEquals(2, lists.size());
        assertSame(lists.get(0), lists.get(1));
    }
}
