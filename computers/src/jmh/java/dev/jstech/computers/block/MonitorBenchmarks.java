/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * What the server pays for the screens in the world: working out the shape of the biggest screen there is, which a
 * monitor does again only when a monitor is placed or taken away, and telling a description of a face from the one a
 * player already holds, which is all a face standing still costs each time it is described.
 *
 * <p>Run on demand: {@code gradlew :computers:jmh}, with JMH options through {@code -PjmhArgs}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class MonitorBenchmarks {

    private Set<Long> biggestScreen;
    private IMonitorPicture console;
    private IMonitorPicture sameConsole;

    @Setup
    public void setUp() {
        biggestScreen = new HashSet<>();
        for (int u = 0; u < PanelShape.MAX_WIDTH; u++) {
            for (int v = 0; v < PanelShape.MAX_HEIGHT; v++) {
                biggestScreen.add(PanelShape.cell(u, v));
            }
        }
        console = consoleOfFullGlass();
        sameConsole = consoleOfFullGlass();
    }

    @Benchmark
    public PanelShape panelShapeOfTheBiggestScreen() {
        return PanelShape.of(biggestScreen);
    }

    @Benchmark
    public boolean fullConsoleComparedWithTheOneHeld() {
        return console.equals(sameConsole);
    }

    /* A console with every line of its glass written, the most a description of one carries. */
    private static IMonitorPicture consoleOfFullGlass() {
        final List<WireLine> lines = new ArrayList<>();
        for (int i = 0; i < IMonitorPicture.MAX_LINES; i++) {
            lines.add(new WireLine("C:\\> dir /w   VOLUME IN DRIVE C IS SYSTEM   " + i, 0));
        }
        return new IMonitorPicture.Console(HardwareEra.VINTAGE, lines, "C:\\>", null, 0);
    }
}
