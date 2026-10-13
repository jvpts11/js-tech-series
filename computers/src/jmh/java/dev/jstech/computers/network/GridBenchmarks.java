/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.network;

import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridMember;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * What a grid costs as a player builds: putting a cable at the end of a long run and taking it out again, and asking
 * for the slowest cable on the way along the run, which the grid works out once a change and answers from then on.
 *
 * <p>Run on demand: {@code gradlew :computers:jmh -PjmhArgs="GridBenchmarks"}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class GridBenchmarks {

    private static final GridMember CABLE = GridMember.cable("bench:access", 0, 64, 0);

    @Param({"64", "1024", "8192"})
    public int cables;

    private Grid grid;
    private long end;

    @Setup(Level.Iteration)
    public void lay() {
        grid = new Grid();
        for (long pos = 0; pos < cables; pos++) {
            grid.place(pos, CABLE, pos == 0 ? List.of() : List.of(pos - 1));
        }
        end = cables - 1;
    }

    @Benchmark
    public int placeAndRemoveAtTheEnd() {
        grid.place(end + 1, CABLE, List.of(end));
        return grid.remove(end + 1).fragments();
    }

    @Benchmark
    public OptionalLong slowestAlongTheRun() {
        return grid.slowestBetween(List.of(0L), List.of(end));
    }
}
