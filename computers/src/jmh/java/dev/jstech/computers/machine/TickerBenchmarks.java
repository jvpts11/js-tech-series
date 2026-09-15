/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.core.JsCore;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IMachineView;
import dev.jstech.core.language.IProgrammingLanguage;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * What a machine's tick costs before any of its programs runs an instruction: sorting the programs into those that
 * run and those that leave, dealing the credits out in rounds and stepping each program in turn.
 *
 * <p>The programs here are busy and free: each uses every instruction it is offered and costs nothing to step. A real
 * program's instructions would bury the machine's own bookkeeping, so the score is that bookkeeping alone, in
 * nanoseconds per tick.
 *
 * <p>Run on demand: {@code gradlew :computers:jmh}, with JMH options through {@code -PjmhArgs}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class TickerBenchmarks {

    /** What a machine in the program benchmarks is worth in a tick. */
    private static final int CREDITS = 1700;

    private static final ResourceLocation LANGUAGE = ResourceLocation.fromNamespaceAndPath("jsc", "bench_busy");

    @Param({"1", "8", "32"})
    public String programs;

    private MachinePrograms machine;

    @Setup(Level.Trial)
    public void start() {
        JsCore.languages().register(new BusyLanguage());
        this.machine = new MachinePrograms();
        final int count = Integer.parseInt(this.programs);
        for (int i = 0; i < count; i++) {
            final MachinePrograms.Started started = this.machine.start("busy.bench", "busy", 1, null);
            if (!started.ok()) {
                throw new IllegalStateException("a busy program did not start: " + started.message());
            }
        }
    }

    @TearDown(Level.Trial)
    public void stop() {
        JsCore.languages().unregister(LANGUAGE);
    }

    @Benchmark
    public void tick() {
        this.machine.tick(CREDITS);
    }

    /** A language whose every program is busy and free to step. */
    private static final class BusyLanguage implements IProgrammingLanguage {

        @Override
        public ResourceLocation id() {
            return LANGUAGE;
        }

        @Override
        public String displayName() {
            return "Busy";
        }

        @Override
        public Set<String> sourceExtensions() {
            return Set.of();
        }

        @Override
        public Set<String> binaryExtensions() {
            return Set.of("bench");
        }

        @Override
        public CompileResult compile(final List<SourceText> sources) {
            return CompileResult.of(sources.isEmpty() ? "" : sources.getFirst().text());
        }

        @Override
        public List<Token> tokenize(final String text) {
            return List.of();
        }

        @Override
        public ILanguageProcess start(final String binary, final IMachineView machine, final List<String> arguments) {
            return new BusyProcess();
        }

        @Override
        public ILanguageProcess restore(final String binary, final CompoundTag saved, final int version,
                                        final IMachineView machine) {
            return new BusyProcess();
        }
    }

    /** A program that spends whatever it is offered and never finishes. */
    private static final class BusyProcess implements ILanguageProcess {

        private long spent;

        @Override
        public int step(final int budget) {
            this.spent += budget;
            return budget;
        }

        @Override
        public State state() {
            return State.RUNNING;
        }

        @Override
        public String message() {
            return "";
        }

        @Override
        public long spent() {
            return this.spent;
        }

        @Override
        public long heldBytes() {
            return 0L;
        }

        @Override
        public boolean isService() {
            return false;
        }

        @Override
        public void onTick() {
        }

        @Override
        public void onStop(final int budget) {
        }

        @Override
        public String name() {
            return "busy";
        }

        @Override
        public void save(final CompoundTag tag) {
        }
    }
}
