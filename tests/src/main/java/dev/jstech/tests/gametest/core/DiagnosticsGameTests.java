/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.diagnostic.Bandwidth;
import dev.jstech.core.diagnostic.BandwidthLedger;
import dev.jstech.core.diagnostic.CostMap;
import dev.jstech.core.diagnostic.DiagnosticReports;
import dev.jstech.core.diagnostic.Diagnostics;
import dev.jstech.core.diagnostic.Health;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.diagnostic.Sections;
import dev.jstech.core.energy.LevelEnergy;
import dev.jstech.core.log.LogSystem;
import dev.jstech.core.log.LogSystems;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.ISystemReportExtender;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Diagnostics: a section runs in a frame named after it, which profilers show; the watchdog counts a run that goes on
 * too long; a section over its budget slows itself and speeds back up; what is sent to a player is counted by kind, by
 * mod and by the block whose updates took it; a measure of cost lists the worst places; the state of the systems is
 * read part by part, a failing part said; and the diagnostic file and the crash report's section hold what they say.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DiagnosticsGameTests {

    private static final String ARENA = "empty";
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private DiagnosticsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void section_runsInAFrameNamedAfterItAndIsTimed(final GameTestHelper helper) {
        final Section section = Probes.NAMED;
        final AtomicReference<StackTraceElement[]> stack = new AtomicReference<>();
        final AtomicReference<List<Sections.Now>> running = new AtomicReference<>();
        section.run(() -> {
            stack.set(Thread.currentThread().getStackTrace());
            running.set(Sections.runningNow());
        });
        helper.assertTrue(Arrays.stream(stack.get()).anyMatch(frame -> frame.getClassName()
                        .endsWith("Section$jstests$named_frame") && "jstests:named_frame".equals(frame.getFileName())),
                "the work runs in a frame named after the section, which a profiler's sample shows");
        helper.assertTrue(running.get().stream().anyMatch(now -> now.section() == section
                && now.thread().equals(Thread.currentThread().getName())), "and is seen running on its thread");
        helper.assertTrue(section.call(() -> "given").equals("given"), "a section hands back what its work gives");
        helper.assertTrue(Diagnostics.timings().summaries().stream()
                .anyMatch(summary -> summary.section().equals("jstests:named_frame")), "and is timed for F3");
        helper.assertTrue(Sections.byId(LevelEnergy.TIMING).isPresent(),
                "the energy grid's work is a section of the Core");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void watchdog_countsARunThatGoesOnTooLong(final GameTestHelper helper) {
        final Section section = Probes.STALLING;
        final int before = section.stalls();
        section.run(() -> sleep(500L));
        helper.assertTrue(section.stalls() == before + 1, "the watchdog counts a run past what it lets one take");
        helper.assertTrue(section.longestStallNanos() >= 400L * NANOS_PER_MILLI,
                "and keeps how long the run took once it ended");
        section.run(() -> { });
        helper.assertTrue(section.stalls() == before + 1, "a short run is no stall");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void budget_slowsASectionOverItAndSpeedsItBackUp(final GameTestHelper helper) {
        final Section section = Probes.BUDGETED;
        final boolean[] heavy = {true};
        helper.onEachTick(() -> {
            if (section.due(helper.getLevel().getGameTime())) {
                section.run(() -> spin(heavy[0] ? 3L * NANOS_PER_MILLI : 0L));
            }
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(section.slowed() && section.stride() >= 2,
                    "a section over its budget runs one tick in two, or fewer");
            helper.assertTrue(Sections.slowed().contains(section), "and is listed as slowed");
            heavy[0] = false;
        });
        helper.runAfterDelay(380, () -> {
            helper.assertFalse(section.slowed(), "once well within its budget, it runs every tick again");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void ledger_keepsAMinuteByKindModAndSender(final GameTestHelper helper) {
        final long[] now = {1_000L};
        final BandwidthLedger ledger = new BandwidthLedger(() -> now[0]);
        final BandwidthLedger.Sender chest = new BandwidthLedger.Sender(helper.getLevel().dimension(),
                new BlockPos(1, 2, 3), ResourceLocation.withDefaultNamespace("chest"));
        ledger.count("minecraft:block_entity_data", chest, 600L);
        now[0] += 30L;
        ledger.count("jstests:probe", null, 600L);
        helper.assertTrue(ledger.bytesPerSecond() == 20L, "a minute's bytes come to so much a second");
        final long[] minute = ledger.lastMinute();
        helper.assertTrue(minute[59] == 600L && minute[29] == 600L, "each second's bytes stand in their place");
        helper.assertTrue(ledger.mods().stream().map(BandwidthLedger.Rate::name).toList()
                .containsAll(List.of("minecraft", "jstests")), "the kinds add up by mod");
        helper.assertTrue(ledger.senders(5).get(0).sender().equals(chest), "a block's updates are its own");
        now[0] += 60L;
        helper.assertTrue(ledger.bytesPerSecond() == 0L, "nothing sent in the last minute is nothing a second");
        helper.assertTrue(ledger.kinds().stream().anyMatch(rate -> rate.bytesPerSecond() > 0L),
                "while the minute before still counts for its share");
        now[0] += 180L;
        helper.assertTrue(ledger.kinds().isEmpty(), "and is let go of once it is long past");
        helper.assertTrue(Bandwidth.graph(new long[60]).chars().allMatch(c -> c == Bandwidth.BARS.charAt(0)),
                "a minute of nothing is a line of the lowest bar");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bandwidth_countsWhatAConnectionSendsEitherSideOfItsEncoder(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.CHEST);
        final BlockEntity chest = helper.getLevel().getBlockEntity(pos);
        final UUID player = UUID.randomUUID();
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast("encoder", new FakeEncoder());
        try {
            helper.assertTrue(Bandwidth.watch(channel.pipeline(), player, () -> helper.getLevel().dimension()),
                    "a connection with an encoder is counted");
            helper.assertFalse(Bandwidth.watch(channel.pipeline(), player, () -> helper.getLevel().dimension()),
                    "once");
            channel.writeOutbound(ClientboundBlockEntityDataPacket.create(chest));
            channel.writeOutbound(new ClientboundCustomPayloadPacket(new ProbePayload()));
            final BandwidthLedger ledger = Bandwidth.of(player).orElseThrow();
            final List<String> kinds = ledger.kinds().stream().map(BandwidthLedger.Rate::name).toList();
            helper.assertTrue(kinds.contains("minecraft:block_entity_data") && kinds.contains("jstests:probe"),
                    "the game's packets by their kind and a mod's payloads by their id: " + kinds);
            helper.assertTrue(ledger.bytesPerSecond() == (FakeEncoder.UPDATE + FakeEncoder.OTHER) / 60L,
                    "each counted at the bytes the encoder wrote it as");
            final BandwidthLedger.Sender sender = ledger.senders(1).get(0).sender();
            helper.assertTrue(sender.pos().equals(pos) && sender.type().equals(ResourceLocation.withDefaultNamespace(
                    "chest")), "a block's update counts for the block");
        } finally {
            Bandwidth.forget(player);
            channel.finishAndReleaseAll();
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void cost_measuresThenListsTheWorstFirst(final GameTestHelper helper) {
        final BlockPos chestPos = new BlockPos(1, 1, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        final BlockEntity chest = helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
        final BlockEntityTicker<BlockEntity> measured = CostMap.measured((level, pos, state, entity) -> spin(
                NANOS_PER_MILLI));
        final AtomicReference<CostMap.Measure> result = new AtomicReference<>();
        CostMap.measure(helper.getLevel().getServer(), 1, result::set);
        helper.assertTrue(CostMap.measuring(), "a measure runs once asked for");
        helper.onEachTick(() -> {
            if (CostMap.measuring()) {
                measured.tick(helper.getLevel(), chest.getBlockPos(), chest.getBlockState(), chest);
                CostMap.add(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 1)), Probes.WORK,
                        5L * NANOS_PER_MILLI);
            }
        });
        helper.succeedWhen(() -> {
            final CostMap.Measure measure = result.get();
            helper.assertTrue(measure != null, "the measure ends by itself");
            final List<CostMap.Entry> places = measure.places();
            helper.assertTrue(places.stream().anyMatch(entry -> entry.kind().equals(Probes.WORK)
                    && entry.nanosPerTick() >= 4L * NANOS_PER_MILLI), "a kit's work is measured where it is done");
            helper.assertTrue(places.stream().anyMatch(entry -> entry.pos().equals(chest.getBlockPos())
                    && entry.kind().equals(ResourceLocation.withDefaultNamespace("chest"))),
                    "and a ticker, under its block entity's type");
            for (int i = 1; i < places.size(); i++) {
                helper.assertTrue(places.get(i - 1).nanosPerTick() >= places.get(i).nanosPerTick(),
                        "the worst come first");
            }
            helper.assertTrue(CostMap.last().orElseThrow() == measure, "and the last measure stays");
        });
    }

    @GameTest(template = ARENA)
    public static void health_readsEachPartAndSaysOneThatFails(final GameTestHelper helper)
            throws CommandSyntaxException {
        final MinecraftServer server = helper.getLevel().getServer();
        Probes.failing(true);
        try {
            final List<Health.Part> parts = Health.read(server);
            final List<String> systems = parts.stream().map(part -> part.system().id()).toList();
            helper.assertTrue(systems.containsAll(List.of("jscore:diagnostics", "jscore:tasks", "jscore:teams")),
                    "the Core's parts are read: " + systems);
            helper.assertTrue(parts.stream().filter(part -> part.system() == Probes.LOG)
                    .anyMatch(part -> part.lines().stream().anyMatch(Health.Line::warning)),
                    "a part that fails is said as a warning, and the rest are read");
            helper.assertTrue(server.getCommands().getDispatcher().execute("jstech health",
                    server.createCommandSourceStack().withSuppressedOutput()) > 0, "the command counts warnings");
        } finally {
            Probes.failing(false);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void diagnostics_writeTheFileAndTheCrashReportsSection(final GameTestHelper helper)
            throws CommandSyntaxException, IOException {
        final MinecraftServer server = helper.getLevel().getServer();
        final Path folder = FMLPaths.GAMEDIR.get().resolve(DiagnosticReports.FOLDER);
        server.getCommands().getDispatcher().execute("jstech diagnostics",
                server.createCommandSourceStack().withSuppressedOutput());
        final Path file;
        try (Stream<Path> files = Files.list(folder)) {
            file = files.max((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                    .orElseThrow();
        }
        try {
            final String text = Files.readString(file);
            helper.assertTrue(text.contains("== Mods ==") && text.contains("jscore "), "the file lists the mods");
            helper.assertTrue(text.contains("jstech-balance") && text.contains("stall_millis"),
                    "the settings, as their files hold them");
            helper.assertTrue(text.contains("== Health ==") && text.contains("[jscore:diagnostics]"),
                    "and the state of the systems");
        } finally {
            Files.deleteIfExists(file);
        }
        helper.assertTrue(CrashReportCallables.allCrashCallables().stream().map(ISystemReportExtender::getLabel)
                .anyMatch("J's Core"::equals), "a crash report has the Core's section");
        helper.assertTrue(DiagnosticReports.crashSection().contains("[jscore:diagnostics]"),
                "which, on the server's thread, holds the state of the systems");
        helper.succeed();
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void spin(final long nanos) {
        final long end = System.nanoTime() + nanos;
        while (System.nanoTime() < end) {
            Thread.onSpinWait();
        }
    }

    /* The sections and the part of the health these tests use, declared once for the whole run. */
    private static final class Probes {

        private static final LogSystem LOG = LogSystems.declare(JsTests.MODID, "diagnostics_probe");
        private static final Section NAMED = Section.builder(rl("named_frame"), LOG).declare();
        private static final Section STALLING = Section.builder(rl("stalling"), LOG)
                .stallAfter(Duration.ofMillis(50)).declare();
        private static final Section BUDGETED = Section.builder(rl("budgeted"), LOG)
                .budget(Duration.ofMillis(1)).declare();
        private static final ResourceLocation WORK = rl("measured_work");
        private static volatile boolean failing;

        static {
            Health.provide(LOG, (server, report) -> {
                if (failing) {
                    throw new IllegalStateException("the probe fails on purpose");
                }
            });
        }

        private Probes() {
        }

        private static void failing(final boolean value) {
            failing = value;
        }

        private static ResourceLocation rl(final String path) {
            return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
        }
    }

    /* Writes each packet as so many bytes, as the game's encoder would write it as its own. */
    private static final class FakeEncoder extends ChannelOutboundHandlerAdapter {

        private static final long UPDATE = 1200L;
        private static final long OTHER = 600L;

        @Override
        public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise promise) {
            if (message instanceof Packet<?> packet) {
                final long size = packet instanceof ClientboundBlockEntityDataPacket ? UPDATE : OTHER;
                context.write(Unpooled.buffer().writeZero((int) size), promise);
            } else {
                context.write(message, promise);
            }
        }
    }

    /* A mod's payload, as a packet carries it; only its id is read. */
    private record ProbePayload() implements CustomPacketPayload {

        private static final Type<ProbePayload> TYPE = new Type<>(Probes.rl("probe"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
