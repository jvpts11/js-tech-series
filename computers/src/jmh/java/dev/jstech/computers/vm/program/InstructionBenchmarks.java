/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
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
 * What one instruction of each family costs the runtime, measured on the real {@link Process} with no world around it.
 *
 * <p>Every family is a script whose tick never ends, so each invocation spends exactly {@link #BUDGET} instructions
 * and the score is the time of one instruction. The default report is nanoseconds per instruction; JMH's
 * {@code -bm thrpt -tu ms} turns the same run into instructions per millisecond. A script that stops early (a halt,
 * a full heap, a wait) fails the run instead of reporting a number that no longer means anything.
 *
 * <p>Run on demand: {@code gradlew :computers:jmh}, with JMH options through {@code -PjmhArgs}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class InstructionBenchmarks {

    /** Instructions each invocation spends; large enough that the per-invocation setup is not what is timed. */
    private static final int BUDGET = 20_000;

    /** The largest heap a program may be given, so no family runs out of room inside one invocation. */
    private static final long ROOM = 64L * 1024 * 1024;

    /** What every script starts with, on one line so the sources keep their line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Collections.*; "
            + "using System.Utils.*; using System.Machine.*; using System.Network.*; using System.Operations.*; "
            + "using System.Execution.*; using System.Threading.*; namespace Benchmarks; ";

    /** A class behind an interface, for the families that reach an object through what it implements. */
    private static final String SHAPES = "interface IShape { int Area(); }\n"
            + "class Square : IShape { public int Side; public int Area() { return Side * Side; } }\n";

    @Param({"arithmetic", "fields", "calls", "objects", "arrays", "strings", "library", "locks", "virtual",
            "constructors", "casts", "text", "collections"})
    public String family;

    private ProgramImage program;
    private Process process;

    @Setup(Level.Trial)
    public void compile() {
        this.program = load(script(this.family));
    }

    /** A fresh process per invocation, so heap and console never carry from one measurement to the next. */
    @Setup(Level.Invocation)
    public void start() {
        this.process = new Process(this.program, ROOM, IHost.still());
        final Values.Obj self = this.process.create(this.program.entryPoint());
        if (self == null) {
            throw new IllegalStateException(this.family + " could not be started: " + this.process.message());
        }
        this.process.begin(self, "OnTick");
    }

    @Benchmark
    @OperationsPerInvocation(BUDGET)
    public int instructions() {
        final int used = this.process.step(BUDGET);
        if (used != BUDGET) {
            throw new IllegalStateException(this.family + " stopped after " + used + " of " + BUDGET
                    + " instructions: " + this.process.message());
        }
        return used;
    }

    /** The whole source of a family's script: classes before the script, members of the script, and its tick. */
    private static String script(final String family) {
        final String[] parts = switch (family) {
            case "arithmetic" -> new String[]{"", "",
                    "int a = 3; int b = 0; while (true) { b = b + a * 7 - b % 5; if (b > 100000) { b = 0; } }"};
            case "fields" -> new String[]{"", "int count; int total;",
                    "while (true) { count = count + 1; total = total + count; if (total > 100000) { total = 0; } }"};
            case "calls" -> new String[]{"", "int Twice(int v) { return v + v; }",
                    "int x = 1; while (true) { x = Twice(x) % 1000 + 1; }"};
            case "objects" -> new String[]{"class Point { public int X; public int Y; }\n", "",
                    "int i = 0; while (true) { Point p = new Point(); p.X = i; p.Y = p.X + 1; i = i + 1; }"};
            case "arrays" -> new String[]{"", "",
                    "int[] data = new int[64]; int i = 0; while (true) { data[i % 64] = data[(i + 1) % 64] + i; i = i + 1; }"};
            case "strings" -> new String[]{"", "",
                    "int i = 0; string s = \"\"; while (true) { s = \"n\" + i; i = i + 1; }"};
            case "library" -> new String[]{"", "",
                    "double d = 7.0; while (true) { d = Math.Abs(d - 5.0); }"};
            case "locks" -> new String[]{"", "int count;",
                    "while (true) { lock (this) { count = count + 1; } }"};
            case "virtual" -> new String[]{SHAPES, "",
                    "Square square = new Square(); square.Side = 3; IShape shape = square; int total = 0; "
                            + "while (true) { total = shape.Area() + total % 1000; }"};
            case "constructors" -> new String[]{"class Base { public int A; public Base(int a) { A = a; } }\n"
                    + "class Point : Base { public int B; public Point(int a, int b) : base(a) { B = b; } }\n", "",
                    "int i = 0; while (true) { Point p = new Point(i, i + 1); i = i + 1; }"};
            case "casts" -> new String[]{SHAPES, "",
                    "object thing = new Square(); int hits = 0; while (true) { if (thing is IShape) { hits = hits + 1; } "
                            + "IShape shape = thing as IShape; if (hits > 100000) { hits = 0; } }"};
            case "text" -> new String[]{"", "",
                    "string line = \"iron ore, 64\"; int found = 0; while (true) { string head = line.Substring(0, 4); "
                            + "found = found + line.IndexOf(\",\"); if (found > 100000) { found = 0; } }"};
            case "collections" -> new String[]{"", "",
                    "List<int> items = new List<int>(); Map<string, int> counts = new Map<string, int>(); int i = 0; "
                            + "while (true) { items.Add(i); int last = items.Get(i); counts.Put(\"iron\", i); "
                            + "int held = counts.Get(\"iron\"); i = i + 1; }"};
            default -> throw new IllegalArgumentException("no benchmark family " + family);
        };
        return parts[0] + "class Monitor : IScript {\n" + parts[1] + "\n"
                + "    public void OnInit() { }\n"
                + "    public void OnTick() {\n" + parts[2] + "\n    }\n"
                + "    public void OnDestroy() { }\n}\n";
    }

    private static ProgramImage load(final String source) {
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("Benchmark.can", PRELUDE + source)));
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
