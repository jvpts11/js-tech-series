/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine.prophet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * What Prophet YourIQL's evaluation costs on a large network: one with 30,000 kinds of item and many states declared,
 * standing still (nothing changed since the last look) and moving (some counts changed every look), against reading
 * every state every time, which is what an engine that did not track what changed would do.
 *
 * <p>Run on demand: {@code gradlew :computers:jmh -PjmhArgs="ProphetBenchmarks"}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class ProphetBenchmarks {

    @Param({"100", "1000"})
    public int states;

    private final Map<String, Long> network = new HashMap<>();
    private ProphetStates prophet;
    private String[] watched;
    private long version;
    private long now;
    private int turn;

    private static final int ITEM_TYPES = 30_000;
    /** How many counts change between two looks on the moving network. */
    private static final int CHANGES = 50;

    @Setup(Level.Iteration)
    public void build() {
        network.clear();
        for (int i = 0; i < ITEM_TYPES; i++) {
            network.put("item_" + i, 1_000L + i % 977);
        }
        prophet = new ProphetStates();
        watched = new String[states];
        for (int i = 0; i < states; i++) {
            watched[i] = "item_" + i * (ITEM_TYPES / states);
            prophet.keep(watched[i], 500, 2_000);
        }
        version = 1;
        prophet.evaluate(version, now, network::get);
    }

    /** Nothing changed: one comparison. */
    @Benchmark
    public List<ProphetStates.Reaction> still() {
        return prophet.evaluate(version, ++now, network::get);
    }

    /** {@link #CHANGES} counts changed, some of them ones a state is about. */
    @Benchmark
    public List<ProphetStates.Reaction> moving() {
        for (int c = 0; c < CHANGES; c++) {
            final String item = watched[(turn + c) % watched.length];
            network.put(item, network.get(item) + 1);
        }
        turn++;
        return prophet.evaluate(++version, ++now, network::get);
    }

    /** Every state read again every time, whatever changed: the cost of not tracking what changed. */
    @Benchmark
    public List<ProphetStates.Reaction> everyStateEveryTime() {
        prophet.touch();
        return prophet.evaluate(++version, ++now, network::get);
    }
}
