/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Loaded;
import dev.jstech.computers.cannon.run.Process;
import dev.jstech.computers.cannon.run.Snapshot;
import dev.jstech.computers.cannon.run.Values;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * More than one thing at once inside one program: threads take turns a few instructions at a time over
 * the same memory, wait on each other, on the clock and on locks, and come back from a save mid-wait.
 */
class ThreadsTest {

    private static final long ROOM = 64L * 1024;
    private static final int PLENTY = 1_000_000;

    /** What every file starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Machine.*; using System.Network.*; using System.Operations.*; "
            + "using System.Execution.*; using System.Threading.*; namespace Tests; ";

    /** A world whose clock the test moves by hand. */
    private static final class Clock implements IHost {
        long now;

        @Override
        public long tick() {
            return this.now;
        }

        @Override
        public long dayTime() {
            return this.now % 24_000L;
        }

        @Override
        public long day() {
            return this.now / 24_000L;
        }
    }

    private static Loaded load(final String members, final String tick) {
        final String source = "class Monitor : IScript {\n" + members
                + "    public void OnInit() { }\n"
                + "    public void OnTick() {\n" + tick + "\n    }\n"
                + "    public void OnDestroy() { }\n}\n";
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("Monitor.can", PRELUDE + source)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final DiagnosticBag bag = new DiagnosticBag("Monitor.asm");
        final AsmProgram program = new AsmReader(built.assembly(), bag).read();
        assertFalse(bag.hasErrors(), () -> String.join("\n",
                bag.sorted().stream().map(Diagnostic::format).toList()));
        return Loaded.of(program);
    }

    /** A script ready for its first tick, with the tick queued and nothing run yet. */
    private static Process start(final Loaded program, final IHost host) {
        final Process process = new Process(program, ROOM, host);
        final Values.Obj self = process.create(program.entryPoint());
        assertNotNull(self);
        process.begin(self, "OnTick");
        return process;
    }

    private static Process run(final String members, final String tick) {
        final Process process = start(load(members, tick), IHost.still());
        process.step(PLENTY);
        return process;
    }

    @Test
    void start_runsABodyBesideTheMainThread() {
        final Process process = run("", """
                Console.PrintLine("a");
                Thread.Start(() => { Console.PrintLine("t"); });
                Console.PrintLine("b");
                """);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertTrue(process.console().contains("t"), () -> String.valueOf(process.console()));
        assertTrue(process.console().indexOf("a") < process.console().indexOf("b"));
    }

    @Test
    void start_costsMoreThanACall() {
        final Process bare = run("", "        int n = 1;");
        final Process threaded = run("", "        Thread.Start(() => { });");
        assertTrue(threaded.spent() >= bare.spent() + 49, threaded.spent() + " against " + bare.spent());
    }

    @Test
    void threads_takeTurnsAFewInstructionsAtATime() {
        final Process process = run("", """
                Thread.Start(() => { for (int i = 0; i < 400; i++) { Console.PrintLine("one"); } });
                Thread.Start(() => { for (int i = 0; i < 400; i++) { Console.PrintLine("two"); } });
                """);
        final List<String> said = process.console();
        assertTrue(said.contains("one") && said.contains("two"));
        /*
         * Neither thread ran to the end before the other began: somewhere the lines change hands and
         * change back, which a program running one thread to the end would never show.
         */
        int changes = 0;
        for (int i = 1; i < said.size(); i++) {
            if (!said.get(i).equals(said.get(i - 1))) {
                changes++;
            }
        }
        assertTrue(changes >= 2, "the two took turns; changes of hand: " + changes);
    }

    @Test
    void current_namesTheThreadItIsReadOn() {
        final Process process = run("", """
                Console.PrintLine("main " + Thread.Current.Id);
                Thread.Start(() => { Console.PrintLine("child " + Thread.Current.Id); });
                """);
        assertTrue(process.console().contains("main 1"), () -> String.valueOf(process.console()));
        assertTrue(process.console().contains("child 2"), () -> String.valueOf(process.console()));
    }

    @Test
    void join_waitsForTheThreadToEnd() {
        final Process process = run("", """
                Thread t = Thread.Start(() => { for (int i = 0; i < 200; i++) { } Console.PrintLine("work"); });
                t.Join();
                Console.PrintLine("after " + t.Running);
                """);
        assertEquals(List.of("work", "after false"), process.console(), () -> String.valueOf(process.message()));
    }

    @Test
    void sleep_parksTheThreadUntilTheTickComes() {
        final Clock clock = new Clock();
        final Process process = start(load("", """
                Thread.Start(() => { Console.PrintLine("down"); Thread.Sleep(5); Console.PrintLine("up"); });
                """), clock);
        process.step(PLENTY);
        assertEquals(List.of("down"), process.console());
        clock.now = 4;
        process.step(PLENTY);
        assertEquals(List.of("down"), process.console(), "not yet");
        clock.now = 5;
        process.step(PLENTY);
        assertEquals(List.of("down", "up"), process.console());
    }

    @Test
    void join_withATimeGivesUpWhenItRunsOut() {
        final Clock clock = new Clock();
        final Process process = start(load("", """
                Thread t = Thread.Start(() => { Thread.Sleep(100); });
                bool done = t.Join(3);
                Console.PrintLine("joined " + done);
                """), clock);
        process.step(PLENTY);
        assertTrue(process.console().isEmpty(), "still waiting");
        clock.now = 3;
        process.step(PLENTY);
        assertEquals(List.of("joined false"), process.console());
    }

    @Test
    void stop_endsAThreadWhereItStands() {
        final Process process = run("", """
                Thread t = Thread.Start(() => { while (true) { Thread.Yield(); } });
                Thread.Yield();
                t.Stop();
                Console.PrintLine("running " + t.Running);
                """);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals(List.of("running false"), process.console());
    }

    private static final String COUNTER = """
                object gate = new List<int>();
                int count = 0;

                void Bump(int times) {
                    for (int i = 0; i < times; i++) {
                        lock (gate) {
                            int seen = count;
                            Thread.Yield();
                            count = seen + 1;
                        }
                    }
                }

                void BumpUnlocked(int times) {
                    for (int i = 0; i < times; i++) {
                        int seen = count;
                        Thread.Yield();
                        count = seen + 1;
                    }
                }
            """;

    @Test
    void lock_keepsAReadAndAWriteTogether() {
        final Process locked = run(COUNTER, """
                Thread a = Thread.Start(() => { Bump(50); });
                Thread b = Thread.Start(() => { Bump(50); });
                a.Join();
                b.Join();
                Console.PrintLine("count " + count);
                """);
        assertEquals(List.of("count 100"), locked.console(), () -> String.valueOf(locked.message()));

        final Process torn = run(COUNTER, """
                Thread a = Thread.Start(() => { BumpUnlocked(50); });
                Thread b = Thread.Start(() => { BumpUnlocked(50); });
                a.Join();
                b.Join();
                Console.PrintLine("count " + count);
                """);
        assertFalse(torn.console().contains("count 100"),
                () -> "without the lock a yield in the middle loses updates; got " + torn.console());
    }

    @Test
    void lock_isLetGoOfOnEveryWayOut() {
        final Process process = run("""
                    object gate = new List<int>();

                    int Early() {
                        lock (gate) { return 7; }
                    }

                    void Breaking() {
                        for (int i = 0; i < 3; i++) {
                            lock (gate) { if (i == 1) { break; } }
                        }
                    }
                """, """
                int got = Early();
                Breaking();
                Thread t = Thread.Start(() => { lock (gate) { Console.PrintLine("in " + got); } });
                t.Join();
                """);
        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals(List.of("in 7"), process.console());
    }

    @Test
    void lock_haltsAThreadThatLetsGoOfWhatItDoesNotHold() {
        final Process process = run("", """
                object gate = new List<int>();
                Thread t = Thread.Start(() => { lock (gate) { Thread.Sleep(1000); } });
                Thread.Yield();
                lock (gate) { Console.PrintLine("never"); }
                """);
        assertEquals(Process.State.PARKED, process.state(), "the main thread waits on a lock a sleeper holds");
        assertTrue(process.console().isEmpty());
    }

    @Test
    void save_bringsBackThreadsLocksAndWaits() {
        final Loaded program = load(COUNTER, """
                Thread a = Thread.Start(() => { Bump(30); });
                Thread b = Thread.Start(() => { Bump(30); });
                a.Join();
                b.Join();
                Console.PrintLine("count " + count);
                """);
        final Process straight = start(program, IHost.still());
        straight.step(PLENTY);

        Process saved = start(program, IHost.still());
        for (int i = 0; i < 10_000 && saved.state() == Process.State.RUNNING; i++) {
            saved.step(7);
            final Snapshot shot = saved.save();
            assertTrue(shot.threads().size() >= 1 && shot.threads().size() <= 3, "the threads are written down");
            saved = Process.restore(program, shot, IHost.still());
        }
        final Process back = saved;
        assertEquals(straight.state(), back.state(), () -> String.valueOf(back.message()));
        assertEquals(straight.console(), back.console());
        assertEquals(List.of("count 60"), back.console());
    }

    @Test
    void sleep_inALoopUnderALockCountsTheTicksAScriptIsGiven() {
        final Clock clock = new Clock();
        final Loaded program = load("""
                    object gate = new List<int>();
                    int seen;
                """, "");
        final Process process = new Process(program, ROOM, clock);
        final Values.Obj self = process.create(program.entryPoint());
        assertNotNull(self);
        process.begin(self, "OnInit");
        process.step(PLENTY);
        // OnInit is empty here; the thread is started by hand the way the script would.
        assertEquals(Process.State.FINISHED, process.state());
        final Loaded counting = load("""
                    object gate = new List<int>();
                    int seen;

                    public void Begin() {
                        Thread.Start(() => {
                            while (true) {
                                lock (gate) { seen = seen + 1; Console.PrintLine("t" + seen); }
                                Thread.Sleep(1);
                            }
                        });
                    }
                """, "");
        final Process script = new Process(counting, ROOM, clock);
        final Values.Obj monitor = script.create(counting.entryPoint());
        assertNotNull(monitor);
        script.begin(monitor, "Begin");
        script.step(PLENTY);
        assertEquals(List.of("t1"), script.console(), () -> String.valueOf(script.message()));
        for (int tick = 1; tick <= 3; tick++) {
            clock.now = tick;
            script.begin(monitor, "OnTick");
            script.step(PLENTY);
            assertEquals(Process.State.FINISHED, script.state(), () -> String.valueOf(script.message()));
        }
        assertEquals(List.of("t1", "t2", "t3", "t4"), script.console());
    }

    @Test
    void save_keepsASleeperAsleepAcrossTheSave() {
        final Clock clock = new Clock();
        final Loaded program = load("", """
                Thread.Start(() => { Thread.Sleep(10); Console.PrintLine("up"); });
                """);
        Process process = start(program, clock);
        process.step(PLENTY);
        process = Process.restore(program, process.save(), clock);
        clock.now = 9;
        process.step(PLENTY);
        assertTrue(process.console().isEmpty(), "still asleep after the save");
        clock.now = 10;
        process.step(PLENTY);
        assertEquals(List.of("up"), process.console());
    }
}
