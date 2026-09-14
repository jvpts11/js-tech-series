/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * What one instruction costs while a program's other threads wait, stepped in quanta the way a machine runs its
 * programs.
 *
 * <p>A machine hands each program its tick in rounds of {@link #QUANTUM} instructions, so a program is stepped many
 * times a tick and whatever a step does before its first instruction is paid once a round. In every scenario the
 * main thread keeps adding numbers while {@link #WAITING} threads wait for something that never comes: the clock
 * here never moves, so a sleep never ends, a sleeping thread is never joined and a lock a sleeper holds is never
 * let go of. The score is the time of one of the main thread's instructions, with every round's overhead spread
 * over them.
 *
 * <p>Run on demand: {@code gradlew :computers:jmh}, with JMH options through {@code -PjmhArgs}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class SchedulerBenchmarks {

    /** The instructions a machine offers one program before the next program has its turn. */
    private static final int QUANTUM = 64;

    /** Rounds each invocation steps; enough that the per-invocation setup is not what is timed. */
    private static final int ROUNDS = 320;

    /** How many threads wait in every scenario. */
    private static final int WAITING = 16;

    /** Enough to start the waiting threads and let each reach its wait; starting a thread costs more than a call. */
    private static final int SET_UP = 8_000;

    /** The largest heap a program may be given. */
    private static final long ROOM = 64L * 1024 * 1024;

    /** What every script starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Machine.*; using System.Network.*; using System.Operations.*; "
            + "using System.Execution.*; using System.Threading.*; namespace Benchmarks; ";

    /** What the main thread does for ever once the waiting threads are started. */
    private static final String BUSY =
            "int a = 3; int b = 0; while (true) { b = b + a * 7 - b % 5; if (b > 100000) { b = 0; } }";

    @Param({"sleepers", "joiners", "lockers"})
    public String scenario;

    private ProgramImage program;
    private Process process;

    @Setup(Level.Trial)
    public void compile() {
        this.program = load(script(this.scenario));
    }

    /** A fresh process per invocation, its threads started and waiting before anything is timed. */
    @Setup(Level.Invocation)
    public void start() {
        this.process = new Process(this.program, ROOM, IHost.still());
        final Values.Obj self = this.process.create(this.program.entryPoint());
        if (self == null) {
            throw new IllegalStateException(this.scenario + " could not be started: " + this.process.message());
        }
        this.process.begin(self, "OnTick");
        this.process.step(SET_UP);
        int waiting = 0;
        for (final ProgramThread thread : this.process.threads0()) {
            if (thread.waiting()) {
                waiting++;
            }
        }
        if (waiting < WAITING) {
            throw new IllegalStateException(this.scenario + " has " + waiting + " threads waiting, not " + WAITING
                    + ": " + this.process.message());
        }
    }

    @Benchmark
    @OperationsPerInvocation(ROUNDS * QUANTUM)
    public int quanta() {
        int used = 0;
        for (int round = 0; round < ROUNDS; round++) {
            final int ran = this.process.step(QUANTUM);
            if (ran != QUANTUM) {
                throw new IllegalStateException(this.scenario + " stopped after " + (used + ran) + " of "
                        + ROUNDS * QUANTUM + " instructions: " + this.process.message());
            }
            used += ran;
        }
        return used;
    }

    /** The whole source of a scenario: start the waiting threads, then keep the main thread busy. */
    private static String script(final String scenario) {
        final String waits = switch (scenario) {
            case "sleepers" -> "for (int i = 0; i < " + WAITING + "; i++) { "
                    + "Thread.Start(() => { Thread.Sleep(1000000); }); }";
            case "joiners" -> "Thread held = Thread.Start(() => { Thread.Sleep(1000000); }); "
                    + "for (int i = 0; i < " + WAITING + "; i++) { Thread.Start(() => { held.Join(); }); }";
            /*
             * The holder is let run before any locker exists, so it holds the lock when they come; starting a thread
             * ends the main thread's turn soon after, and the yield ends it at once.
             */
            case "lockers" -> "object gate = new List<int>(); "
                    + "Thread.Start(() => { lock (gate) { Thread.Sleep(1000000); } }); Thread.Yield(); "
                    + "for (int i = 0; i < " + WAITING + "; i++) { Thread.Start(() => { lock (gate) { } }); }";
            default -> throw new IllegalArgumentException("no scheduler scenario " + scenario);
        };
        return "class Monitor : IScript {\n"
                + "    public void OnInit() { }\n"
                + "    public void OnTick() {\n" + waits + "\n" + BUSY + "\n    }\n"
                + "    public void OnDestroy() { }\n}\n";
    }

    private static ProgramImage load(final String source) {
        final SigmaCompiler.Result built =
                SigmaCompiler.compile(List.of(new SourceFile("Benchmark.sgs", PRELUDE + source)));
        if (!built.ok()) {
            throw new IllegalStateException(String.join("\n", built.lines()));
        }
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram listing = reader.read();
        if (reader.hasProblems()) {
            throw new IllegalStateException(String.join("\n",
                    reader.problems().stream().map(ListingProblem::format).toList()));
        }
        return ProgramImage.of(listing);
    }
}
