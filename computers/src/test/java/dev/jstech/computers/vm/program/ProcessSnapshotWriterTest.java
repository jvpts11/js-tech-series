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
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessSnapshotWriterTest {

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

    /** A process stopped inside {@code Inner}, called from {@code OnTick}, which holds one list under two names. */
    private static Process running() {
        final Process process = new Process(PROGRAM, 64L * 1024, IHost.still());
        process.begin(process.create(PROGRAM.entryPoint()), "OnTick");
        process.step(200);
        return process;
    }

    @Test
    void write_writesAThreadsCallsBottomFirst() {
        final Snapshot shot = ProcessSnapshotWriter.write(running());

        final List<String> called = shot.threads().running().getFirst().frames().stream()
                .map(Snapshot.FrameShot::name).toList();

        assertEquals(List.of("OnTick", "Inner"), called);
    }

    @Test
    void write_writesTwoNamesForOneObjectUnderOneNumber() {
        final Snapshot shot = ProcessSnapshotWriter.write(running());

        final Snapshot.FrameShot tick = shot.threads().running().getFirst().frames().getFirst();
        final List<Snapshot.IValue> references = tick.slots().stream()
                .filter(Snapshot.IValue.Ref.class::isInstance).toList();

        assertEquals(2, references.size(), () -> String.valueOf(shot.threads()));
        assertEquals(references.get(0), references.get(1));
    }
}
