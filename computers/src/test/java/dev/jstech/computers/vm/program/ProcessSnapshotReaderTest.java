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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    /** The snapshot of a process stopped inside {@code Inner}, called from {@code OnTick}. */
    private static Snapshot written() {
        final Process process = new Process(PROGRAM, 64L * 1024, IHost.still());
        process.begin(process.create(PROGRAM.entryPoint()), "OnTick");
        process.step(200);
        return ProcessSnapshotWriter.write(process);
    }

    private static Process readBack() {
        return ProcessSnapshotReader.read(PROGRAM, written(), IHost.still());
    }

    /** The same snapshot with its format, listing, heap, threads and script replaced. */
    private static Snapshot changed(final Snapshot shot, final int format, final String listing,
                                    final Snapshot.HeapShot heap, final Snapshot.ThreadsShot threads,
                                    final Snapshot.IValue script) {
        return new Snapshot(format, listing, heap, shot.identity(), shot.console(), shot.input(), shot.callbacks(),
                shot.windows(), shot.watches(), shot.listeners(), threads, shot.monitors(), shot.statics(), script);
    }

    /** The same snapshot with only its main thread, written as {@code main}. */
    private static Snapshot withMainThread(final Snapshot shot, final Snapshot.ThreadShot main) {
        return changed(shot, shot.format(), shot.listing(), shot.heap(),
                new Snapshot.ThreadsShot(List.of(main), shot.threads().nextThread()), shot.script());
    }

    private static void assertRefused(final Snapshot damaged) {
        assertThrows(SnapshotException.class, () -> ProcessSnapshotReader.read(PROGRAM, damaged, IHost.still()));
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

    @Test
    void read_refusesASnapshotOfAnotherFormat() {
        final Snapshot shot = written();

        assertRefused(changed(shot, 0, shot.listing(), shot.heap(), shot.threads(), shot.script()));
    }

    @Test
    void read_refusesASnapshotTakenFromAnotherListing() {
        final Snapshot shot = written();

        assertRefused(changed(shot, shot.format(), "another listing", shot.heap(), shot.threads(), shot.script()));
    }

    @Test
    void read_refusesANumberWrittenDownTwice() {
        final Snapshot shot = written();
        final List<Snapshot.IHeld> held = new ArrayList<>(shot.heap().held());
        held.add(held.getFirst());

        assertRefused(changed(shot, shot.format(), shot.listing(),
                new Snapshot.HeapShot(shot.heap().budget(), held), shot.threads(), shot.script()));
    }

    @Test
    void read_refusesAReferenceToNothingWrittenDown() {
        final Snapshot shot = written();

        assertRefused(changed(shot, shot.format(), shot.listing(), shot.heap(), shot.threads(),
                new Snapshot.IValue.Ref(100_000)));
    }

    @Test
    void read_refusesACallInAMethodTheListingDoesNotHave() {
        final Snapshot shot = written();
        final Snapshot.ThreadShot main = shot.threads().running().getFirst();
        final List<Snapshot.FrameShot> frames = new ArrayList<>(main.frames());
        final Snapshot.FrameShot inner = frames.getLast();
        frames.set(frames.size() - 1, new Snapshot.FrameShot(inner.owner(), "Gone", inner.parameters(), inner.at(),
                inner.self(), inner.slots(), inner.stack(), inner.discard()));

        assertRefused(withMainThread(shot, new Snapshot.ThreadShot(main.id(), frames, main.parked(), main.until(),
                main.on(), main.token(), main.timedOut(), main.onHost())));
    }

    @Test
    void read_refusesAWaitNoSnapshotWrites() {
        final Snapshot shot = written();
        final Snapshot.ThreadShot main = shot.threads().running().getFirst();

        assertRefused(withMainThread(shot, new Snapshot.ThreadShot(main.id(), main.frames(), "napping", main.until(),
                main.on(), main.token(), main.timedOut(), main.onHost())));
    }
}
