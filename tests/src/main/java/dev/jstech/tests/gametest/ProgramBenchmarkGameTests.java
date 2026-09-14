/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.BenchReport;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the programs players write cost the server: busy programs sharing one machine, windows and canvases
 * redrawn every tick, a machine's programs saved and loaded, and dozens of machines each running one.
 *
 * <p>Each benchmark runs alone in its own batch so the clock is not shared. The numbers go to {@code local/bench/}
 * through {@link BenchReport}: the first run is the baseline, which records only what the code did when it was
 * taken, not that it was efficient. A later run fails when a timed metric is slower than the baseline by more than
 * {@link #NOISE_TOLERANCE}, and any tick past {@link #CATASTROPHE_MS} fails outright.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProgramBenchmarkGameTests {

    private ProgramBenchmarkGameTests() {
    }

    private static final String ARENA = "empty";
    private static final String BENCH_ARENA = "bench";
    private static final int SETTLE = 4;
    /** Calls before a direct timing starts, for the JIT to settle on the path being measured. */
    private static final int WARMUP_CALLS = 50;
    private static final int MEASURED_CALLS = 200;
    /** How many ticks {@link MinecraftServer#getAverageTickTimeNanos()} averages over. */
    private static final int TICK_AVERAGE_WINDOW = 100;

    /** A tick this long drops the server below 20 TPS. */
    private static final double CATASTROPHE_MS = 50.0;
    /** How much slower than its baseline a timed metric may run before the benchmark fails: run-to-run noise. */
    private static final double NOISE_TOLERANCE = 2.5;
    /** Timed metrics under this are left out of the comparison, where the timer's own noise dominates. */
    private static final double NOISE_FLOOR_MS = 0.25;

    private static final int[] PROGRAM_COUNTS = {1, 8, 32};
    private static final int MACHINES = 24;
    private static final int SAVED_PROGRAMS = 32;

    /** A program that never finishes its tick: every instruction the machine gives it is spent. */
    private static final String BUSY = """
            using System.*;
            namespace Bench;
            class Busy : IScript {
                int total;
                public void OnInit() { total = 0; }
                public void OnTick() { while (true) { total = total + 1; if (total > 1000000) { total = 0; } } }
                public void OnDestroy() { }
            }
            """;

    /** A window whose label and canvas change every tick, the way a live dashboard does. */
    private static final String LIVE = """
            using System.*;
            using System.UI.*;
            namespace Bench;
            class Live : IScript {
                Window window;
                Label clock;
                Canvas canvas;
                int frame;
                public void OnInit() {
                    clock = new Label("frame 0");
                    canvas = new Canvas(200, 100);
                    Column page = new Column();
                    page.Add(clock);
                    page.Add(canvas, 1);
                    window = new Window("Live", 240, 150);
                    window.Content = page;
                    window.Show();
                }
                public void OnTick() {
                    frame = frame + 1;
                    clock.Text = "frame " + frame;
                    canvas.Clear(0);
                    for (int i = 0; i < 24; i++) {
                        int x = (frame + i * 8) % 200;
                        canvas.FillRect(x, i * 4, x + 6, i * 4 + 3, 65280 + i);
                    }
                }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** A window that never changes after it opens. */
    private static final String STILL = """
            using System.*;
            using System.UI.*;
            namespace Bench;
            class Still : IScript {
                Window window;
                public void OnInit() {
                    Column page = new Column();
                    page.Add(new Label("nothing changes here"));
                    window = new Window("Still", 240, 150);
                    window.Content = page;
                    window.Show();
                }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** A program holding a list of text, so a save has something of its own to write. */
    private static final String HOLDER = """
            using System.*;
            using System.Collections.*;
            namespace Bench;
            class Holder : IScript {
                List<string> notes;
                public void OnInit() {
                    notes = new List<string>();
                    for (int i = 0; i < 500; i++) { notes.Add("note number " + i); }
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    /** One machine for each program count, all busy: what a tick of that many programs costs the machine. */
    @GameTest(template = ARENA, batch = "jsc_bench_programs", timeoutTicks = 400)
    public static void bench_busyProgramsOnOneMachine(final GameTestHelper helper) {
        final BenchReport report = new BenchReport("programs");
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final long buildStart = System.nanoTime();
        final List<PersonalComputerBlockEntity> machines = new ArrayList<>();
        for (int i = 0; i < PROGRAM_COUNTS.length; i++) {
            machines.add(world.placeRunningPersonalComputer(new BlockPos(1 + i * 2, 2, 2)));
        }
        report.put("build_ms", (System.nanoTime() - buildStart) / 1_000_000.0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> guarded(() -> {
                    for (int i = 0; i < PROGRAM_COUNTS.length; i++) {
                        final PersonalComputerBlockEntity pc = machines.get(i);
                        final int count = PROGRAM_COUNTS[i];
                        for (int p = 0; p < count; p++) {
                            final MachinePrograms.Started started = pc.sigma().start("busy.sgs", BUSY, 1, pc);
                            helper.assertTrue(started.ok(), "busy program " + p + " starts: " + started.message());
                        }
                        final int credits = pc.sigmaCredits();
                        final double ms = timeTicks(pc.sigma(), credits);
                        assertAllAlive(helper, pc.sigma(), count);
                        report.put("credits_per_tick", credits);
                        report.put("busy_" + count + "_tick_ms", ms);
                        report.put("busy_" + count + "_per_program_us", ms * 1000.0 / count);
                        helper.assertTrue(ms < CATASTROPHE_MS, count + " busy programs took " + ms + " ms a tick");
                    }
                    finish(helper, report);
                }))
                .thenSucceed();
    }

    /**
     * A live window and a still one on one machine, as a player at its desktop receives them: every tick each window
     * is built, compared with what was sent last, and sent whole when anything in it changed.
     */
    @GameTest(template = ARENA, batch = "jsc_bench_windows", timeoutTicks = 400)
    public static void bench_windowsAndCanvasesPerTick(final GameTestHelper helper) {
        final BenchReport report = new BenchReport("windows");
        final long buildStart = System.nanoTime();
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        report.put("build_ms", (System.nanoTime() - buildStart) / 1_000_000.0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> guarded(() -> {
                    final MachinePrograms sigma = pc.sigma();
                    final MachinePrograms.Started live = sigma.start("live.sgs", LIVE, 1, pc);
                    final MachinePrograms.Started still = sigma.start("still.sgs", STILL, 1, pc);
                    helper.assertTrue(live.ok() && still.ok(), "both start: " + live.message() + " / " + still.message());
                    final int credits = pc.sigmaCredits();
                    final ServerLevel level = helper.getLevel();
                    Map<Long, UiWindowPayload> sent = new HashMap<>();
                    long bytes = 0;
                    long liveNanos = 0;
                    long stillNanos = 0;
                    int widgets = 0;
                    for (int tick = 0; tick < WARMUP_CALLS + MEASURED_CALLS; tick++) {
                        sigma.tick(credits);
                        final Map<Long, UiWindowPayload> open = new HashMap<>();
                        final long liveStart = System.nanoTime();
                        collectWindows(sigma, live.id(), pc.getBlockPos(), open);
                        final long liveEnd = System.nanoTime();
                        collectWindows(sigma, still.id(), pc.getBlockPos(), open);
                        final long stillEnd = System.nanoTime();
                        if (tick < WARMUP_CALLS) {
                            sent = open;
                            continue;
                        }
                        liveNanos += liveEnd - liveStart;
                        stillNanos += stillEnd - liveEnd;
                        for (final Map.Entry<Long, UiWindowPayload> entry : open.entrySet()) {
                            if (!entry.getValue().equals(sent.get(entry.getKey()))) {
                                bytes += encodedBytes(level, entry.getValue());
                            }
                            widgets = Math.max(widgets, entry.getValue().widgets().size());
                        }
                        sent = open;
                    }
                    helper.assertTrue(sent.size() == 2, "both windows are open to send; got " + sent.size());
                    report.put("window_bytes_per_tick", bytes / MEASURED_CALLS);
                    report.put("live_window_build_us", liveNanos / 1000.0 / MEASURED_CALLS);
                    report.put("still_window_build_us", stillNanos / 1000.0 / MEASURED_CALLS);
                    report.put("window_build_ms", (liveNanos + stillNanos) / 1_000_000.0 / MEASURED_CALLS);
                    report.put("most_widgets", widgets);
                    finish(helper, report);
                }))
                .thenSucceed();
    }

    /** A machine with many programs, each holding a list: what saving and loading them costs. */
    @GameTest(template = ARENA, batch = "jsc_bench_program_save", timeoutTicks = 400)
    public static void bench_programSaveAndLoad(final GameTestHelper helper) {
        final BenchReport report = new BenchReport("program_save");
        final long buildStart = System.nanoTime();
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        report.put("build_ms", (System.nanoTime() - buildStart) / 1_000_000.0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> guarded(() -> {
                    final MachinePrograms sigma = pc.sigma();
                    for (int p = 0; p < SAVED_PROGRAMS; p++) {
                        final MachinePrograms.Started started = sigma.start("holder.sgs", HOLDER, 1, pc);
                        helper.assertTrue(started.ok(), "holder " + p + " starts: " + started.message());
                    }
                    for (int tick = 0; tick < 20; tick++) {
                        sigma.tick(pc.sigmaCredits());
                    }
                    assertAllAlive(helper, sigma, SAVED_PROGRAMS);
                    CompoundTag saved = new CompoundTag();
                    for (int i = 0; i < WARMUP_CALLS; i++) {
                        saved = new CompoundTag();
                        sigma.save(saved);
                    }
                    final long saveStart = System.nanoTime();
                    for (int i = 0; i < MEASURED_CALLS; i++) {
                        saved = new CompoundTag();
                        sigma.save(saved);
                    }
                    final double saveMs = (System.nanoTime() - saveStart) / 1_000_000.0 / MEASURED_CALLS;
                    MachinePrograms loaded = new MachinePrograms();
                    for (int i = 0; i < WARMUP_CALLS; i++) {
                        loaded = new MachinePrograms();
                        loaded.load(saved, pc);
                    }
                    final long loadStart = System.nanoTime();
                    for (int i = 0; i < MEASURED_CALLS; i++) {
                        loaded = new MachinePrograms();
                        loaded.load(saved, pc);
                    }
                    final double loadMs = (System.nanoTime() - loadStart) / 1_000_000.0 / MEASURED_CALLS;
                    helper.assertTrue(loaded.all().size() == SAVED_PROGRAMS,
                            "every program comes back; got " + loaded.all().size());
                    report.put("programs", SAVED_PROGRAMS);
                    report.put("save_ms", saveMs);
                    report.put("save_bytes", nbtBytes(saved));
                    report.put("load_ms", loadMs);
                    finish(helper, report);
                }))
                .thenSucceed();
    }

    /** Two dozen machines, idle and then each running one busy program: what the whole server tick pays. */
    @GameTest(template = BENCH_ARENA, batch = "jsc_bench_machines", timeoutTicks = 1200)
    public static void bench_dozensOfBusyMachines(final GameTestHelper helper) {
        final BenchReport report = new BenchReport("machines");
        final MinecraftServer server = helper.getLevel().getServer();
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final long buildStart = System.nanoTime();
        final List<PersonalComputerBlockEntity> machines = new ArrayList<>();
        for (int i = 0; i < MACHINES; i++) {
            machines.add(world.placeRunningPersonalComputer(new BlockPos(2 + (i % 6) * 3, 2, 2 + (i / 6) * 3)));
        }
        report.put("build_ms", (System.nanoTime() - buildStart) / 1_000_000.0);
        report.put("machines", MACHINES);
        final double[] first = {Double.NaN};
        helper.startSequence()
                .thenExecuteAfter(SETTLE + TICK_AVERAGE_WINDOW + 20, () -> first[0] = averageTickMs(server))
                // The better of two consecutive windows, so an autosave inside one of them is not read as the cost.
                .thenExecuteAfter(TICK_AVERAGE_WINDOW, () -> guarded(() -> {
                    report.put("idle_ms", Math.min(first[0], averageTickMs(server)));
                    for (final PersonalComputerBlockEntity pc : machines) {
                        final MachinePrograms.Started started = pc.sigma().start("busy.sgs", BUSY, 1, pc);
                        helper.assertTrue(started.ok(), "a busy program starts on every machine: " + started.message());
                    }
                }))
                .thenExecuteAfter(TICK_AVERAGE_WINDOW + 20, () -> first[0] = averageTickMs(server))
                .thenExecuteAfter(TICK_AVERAGE_WINDOW, () -> guarded(() -> {
                    final double busy = Math.min(first[0], averageTickMs(server));
                    for (final PersonalComputerBlockEntity pc : machines) {
                        assertAllAlive(helper, pc.sigma(), 1);
                    }
                    report.put("busy_ms", busy);
                    report.put("busy_per_machine_us", busy * 1000.0 / MACHINES);
                    helper.assertTrue(busy < CATASTROPHE_MS, MACHINES + " busy machines took " + busy + " ms a tick");
                    finish(helper, report);
                }))
                .thenSucceed();
    }

    /** Milliseconds a tick of these programs takes, timed straight on the machine's program table. */
    private static double timeTicks(final MachinePrograms sigma, final int credits) {
        for (int i = 0; i < WARMUP_CALLS; i++) {
            sigma.tick(credits);
        }
        final long start = System.nanoTime();
        for (int i = 0; i < MEASURED_CALLS; i++) {
            sigma.tick(credits);
        }
        return (System.nanoTime() - start) / 1_000_000.0 / MEASURED_CALLS;
    }

    private static void assertAllAlive(final GameTestHelper helper, final MachinePrograms sigma, final int expected) {
        helper.assertTrue(sigma.all().size() == expected, "expected " + expected + " programs; got " + sigma.all().size());
        for (final MachinePrograms.Live one : sigma.all()) {
            final ILanguageProcess.State state = one.process().state();
            helper.assertTrue(state != ILanguageProcess.State.HALTED,
                    "program " + one.id() + " halted: " + one.process().message());
        }
    }

    /** The windows one program has open, as the machine builds them for whoever is at its desktop. */
    private static void collectWindows(final MachinePrograms sigma, final int program, final BlockPos host,
                                       final Map<Long, UiWindowPayload> into) {
        for (final Values.Obj window : sigma.windowsOf(program)) {
            final UiWindowPayload payload = UiWindowPayload.of(host, program, window);
            if (payload != null) {
                into.put(((long) payload.program() << 32) | (payload.window() & 0xFFFFFFFFL), payload);
            }
        }
    }

    private static int encodedBytes(final ServerLevel level, final UiWindowPayload payload) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            UiWindowPayload.STREAM_CODEC.encode(buf, payload);
            return buf.writerIndex();
        } finally {
            buf.release();
        }
    }

    private static long nbtBytes(final CompoundTag tag) {
        final long[] count = new long[1];
        final OutputStream counter = new OutputStream() {
            @Override
            public void write(final int b) {
                count[0]++;
            }

            @Override
            public void write(final byte[] b, final int off, final int len) {
                count[0] += len;
            }
        };
        try {
            NbtIo.write(tag, new DataOutputStream(counter));
        } catch (final IOException e) {
            throw new GameTestAssertException("could not measure the save: " + e);
        }
        return count[0];
    }

    private static double averageTickMs(final MinecraftServer server) {
        return server.getAverageTickTimeNanos() / 1_000_000.0;
    }

    /** Writes the run and fails when a timed metric is slower than its baseline beyond the noise tolerance. */
    private static void finish(final GameTestHelper helper, final BenchReport report) {
        try {
            report.write();
            final List<String> slower = report.regressions(NOISE_TOLERANCE, NOISE_FLOOR_MS);
            helper.assertTrue(slower.isEmpty(), "slower than the baseline beyond the noise tolerance: " + slower);
        } catch (final IOException e) {
            throw new GameTestAssertException("could not write the benchmark report: " + e);
        }
    }

    /** A step's crash becomes this test's failure instead of bringing the whole test server down. */
    private static void guarded(final Runnable step) {
        try {
            step.run();
        } catch (final GameTestAssertException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new GameTestAssertException("benchmark step crashed: " + e);
        }
    }
}
