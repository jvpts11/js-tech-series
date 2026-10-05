/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload;
import dev.jstech.computers.operation.payload.CreateAutomationJobPayload;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.EngineActionPayload;
import dev.jstech.computers.operation.payload.FileSavedPayload;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload;
import dev.jstech.computers.operation.index.IndexHealth;
import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.IsmsActionPayload;
import dev.jstech.computers.operation.payload.IsmsPlanPayload;
import dev.jstech.computers.operation.payload.IsmsSchemaPayload;
import dev.jstech.computers.operation.payload.IsmsTracePayload;
import dev.jstech.computers.operation.payload.RequestIsmsSchemaPayload;
import dev.jstech.computers.operation.payload.RunIqlPayload;
import dev.jstech.computers.trace.TraceEvent;
import dev.jstech.computers.trace.TraceEventClass;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.NetworkManagerPayload;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.computers.operation.payload.NetworkServersPayload;
import dev.jstech.computers.operation.payload.NetworkServicesPayload;
import dev.jstech.computers.operation.payload.NextgreActionPayload;
import dev.jstech.computers.operation.payload.NextgreStudioPayload;
import dev.jstech.computers.operation.payload.NiGridClickPayload;
import dev.jstech.computers.engine.nextgre.NextgreEngine;
import dev.jstech.computers.engine.nextgre.NextgrePlanView;
import dev.jstech.computers.engine.prophet.ProphetEngine;
import dev.jstech.computers.engine.prophet.ProphetStates;
import dev.jstech.computers.operation.payload.ProphetActionPayload;
import dev.jstech.computers.operation.payload.ProphetConsolePayload;
import dev.jstech.computers.operation.payload.NodeLink;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.OperationsLogPayload;
import dev.jstech.computers.operation.payload.TerminalSelectPayload;
import dev.jstech.computers.operation.payload.ThisPcPayload;
import dev.jstech.computers.operation.payload.UiEventPayload;
import dev.jstech.computers.os.DesktopEffects;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.operation.OperationFailure;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * One payload of every family goes over the wire and back: written, read, and written again to the same bytes, with
 * nothing left unread. The crafting and machine families are covered by {@code MachinePayloadGameTests}; the two
 * payloads that carry a priority by its id go through every priority there is.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PayloadRoundTripGameTests {

    private PayloadRoundTripGameTests() {
    }

    private static final String ARENA = "empty";
    private static final BlockPos HOST = new BlockPos(12, -40, 900);
    private static final BlockPos MONITOR = new BlockPos(13, -40, 900);

    @GameTest(template = ARENA)
    public static void program_uiEventRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, UiEventPayload.STREAM_CODEC, new UiEventPayload(HOST, 7, 3L, 42L, "text", "hello", 5, 9));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void desktop_thisPcRoundTrips(final GameTestHelper helper) {
        final ThisPcPayload.AboutFacts about = new ThisPcPayload.AboutFacts(Text.literal("FreeBSD 14.1-RELEASE"),
                Text.literal("vel64"), Text.literal("14.1-RELEASE GENERIC"), Text.literal("KDE Plasma"),
                Text.literal("desk"), ThisPcPayload.WITH_CLOCK.with("Integra Centro c7 4790K", "4.0 GHz"), 8192L,
                2000L, 120L, true, true);
        final ThisPcPayload.WireMachine machine = new ThisPcPayload.WireMachine("desk",
                ThisPcPayload.PERSONAL_COMPUTER.text(), Text.literal("Standard"), "Frames 11", 2021, "on CORE",
                Text.literal("MF ATX Standard"), ThisPcPayload.WITH_CLOCK.with("Integra Centro c7 4790K", "4.0 GHz"),
                1, Text.literal("x86-64, 64-bit"), 16384, 3072, 1, Text.literal("PSU 650G"), true,
                ThisPcPayload.COUNTED.with(2, ThisPcPayload.MONITOR), about);
        final ThisPcPayload.WireDisk disk = new ThisPcPayload.WireDisk(0, Text.literal("Vaultis Swift SSD 500 GB"),
                2000L, 120L, true, "C:\\", 80L, 20L, 20L);
        final ThisPcPayload.WireMedia media = new ThisPcPayload.WireMedia(123L, "D:", Text.literal("Frames 11 USB"),
                "OS", "FRAMES", true, "Frames 11", 2021, "jsc:frames_11",
                List.of(Text.literal("Standard era or later"), Text.literal("64 it of memory")), 0L, 2);
        roundTrip(helper, ThisPcPayload.STREAM_CODEC,
                new ThisPcPayload(HOST, machine, List.of(disk), List.of(media), List.of("jsc:isms")));
        helper.succeed();
    }

    /**
     * A desktop with every program there is installed on it still gets its desktop. The list of them used to be
     * refused whole past sixteen, which took the desktop away from a machine with a seventeenth program on it.
     */
    @GameTest(template = ARENA)
    public static void desktop_filesRoundTripsWithEveryProgramInstalled(final GameTestHelper helper) {
        final List<String> programs = new ArrayList<>();
        for (final ProgramSpec spec : OsRegistry.programs()) {
            programs.add(spec.id().getPath());
        }
        helper.assertTrue(programs.size() > 16, "there are more programs than the old cap; got " + programs.size());
        roundTrip(helper, DesktopFilesPayload.STREAM_CODEC, new DesktopFilesPayload(List.of(), "", "", "Desk",
                programs, List.of("screenfetch", "vim"), List.of(),
                new DesktopFilesPayload.Prefs(0, 100, false, true, false, 100,
                        new DesktopEffects(List.of("animations"), 60, true)), List.of(), List.of(),
                List.of("files:m", "system_monitor:w", "network_manager:s"), Map.of(),
                false, Map.of("sgsc", "2.0", "scc", "1.0"), 160.0F));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void files_diskFilesRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, DiskFilesPayload.STREAM_CODEC, new DiskFilesPayload("C:\\Users",
                List.of(new DiskFilesPayload.WireFile("C:\\notes.txt", "txt", 4L, false, false),
                        new DiskFilesPayload.WireFile("C:\\Data", "", 0L, false, true, "minecraft:oak_log", 640L)),
                List.of(new DiskFilesPayload.WireVolume("c", Text.literal("System")),
                        new DiskFilesPayload.WireVolume("media:1", DiskFilesPayload.SETUP.with("Frames 11")))));
        helper.succeed();
    }

    /* A save names its path, and a path near the longest allowed once made an answer past the old cap. */
    @GameTest(template = ARENA)
    public static void files_savedAnswerCarriesALongPath(final GameTestHelper helper) {
        roundTrip(helper, FileSavedPayload.STREAM_CODEC,
                new FileSavedPayload(true, FileSavedPayload.SAVED.with("progs/" + "a".repeat(154))));
        helper.succeed();
    }

    /* Every part of the Services tab filled, a replacement under way, and the names at the longest it is sent. */
    @GameTest(template = ARENA)
    public static void network_servicesRoundTrip(final GameTestHelper helper) {
        final String longest = "M".repeat(NetworkServicesPayload.MAX_NAME);
        final NetworkServicesPayload.Engine engine = new NetworkServicesPayload.Engine("jstests:plain_engine",
                longest, "16", "Nextgre", "MF-1a2b3c", NetworkServicesPayload.ENGINE_REPLACING, "IQL", 96,
                List.of("procedures_and_views", "subscriptions"), 1_204, 3, 3_744_000L, 318, 3);
        roundTrip(helper, NetworkServicesPayload.STREAM_CODEC, new NetworkServicesPayload(HOST, engine,
                List.of(new NetworkServicesPayload.EngineRow("jsc:iqlengine", "Midsoft IQL Server", "Midsoft", "2022",
                        NetworkServicesPayload.ROW_INSTALLED)),
                List.of(new NetworkServicesPayload.SubframeRow("SUB-52ddaa", "", "", true),
                        new NetworkServicesPayload.SubframeRow("SUB-8f10bb", "jsc:iqlengine", longest, false)),
                List.of(new NetworkServicesPayload.ServiceRow("Automation Engine", "Red Cap", "4.0",
                        NetworkServicesPayload.SERVICE_RUNNING)),
                new NetworkServicesPayload.Replacement("Midsoft IQL Server", "NextgreIQL 16", 120, 341, 3, 1_204)));
        roundTrip(helper, EngineActionPayload.STREAM_CODEC,
                new EngineActionPayload(HOST, EngineActionPayload.REPLACE, "jstests:plain_engine"));
        helper.succeed();
    }

    /* A node on the fibre through an optical router, one whose fibre bends, one cut off by a run too long. */
    @GameTest(template = ARENA)
    public static void network_managerRoundTripsWithItsLinks(final GameTestHelper helper) {
        final DataLink fibre = new DataLink(DataLine.BACKBONE, HardwareEra.STANDARD);
        final DataLink thinCoax = new DataLink(DataLine.ACCESS, HardwareEra.VINTAGE);
        final List<NetworkNodeInfo> nodes = List.of(
                new NetworkNodeInfo(NetworkNodeInfo.KIND_MAINFRAME, "1a2b", "Core", "96 it/t", true, 2000, 0, 0L, 0L,
                        NetworkNodeInfo.SHARE_UNKNOWN, "Frames 11", NodeLink.up(fibre, true, 140, HOST.asLong())),
                new NetworkNodeInfo(NetworkNodeInfo.KIND_SERVER, "9e03", "Storage B", "", false, 0, 0, 0L, 0L,
                        NetworkNodeInfo.SHARE_UNKNOWN, "", NodeLink.bends(fibre, true, MONITOR.asLong())),
                new NetworkNodeInfo(NetworkNodeInfo.KIND_PC, "3f9a", "Old Lab", "", true, 0, 0, 0L, 0L, 200,
                        "Frames XP", NodeLink.tooLong(thinCoax, false, 33)));
        roundTrip(helper, NetworkManagerPayload.STREAM_CODEC, new NetworkManagerPayload(HOST, "1a2b-77e0", nodes,
                new NetworkManagerPayload.Hardware(96L, 3, 4_096L, 1_284_330L, 1, 1, fibre.serializedName(),
                        thinCoax.serializedName(), "Old Lab"), NetworkManagerPayload.Statistics.EMPTY));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void iql_resultRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, IqlResultPayload.STREAM_CODEC, new IqlResultPayload(3, 2, 1, true, Text.literal("2 rows"),
                List.of("item", "qty", "server"),
                List.of(List.of(Text.literal("oak_log"), Text.literal("640"), Text.literal("Server A")),
                        List.of(Text.literal("iron_ingot"), Text.literal("12"), Text.literal("2 servers"))),
                List.of("1a2b3c4d")));
        roundTrip(helper, IqlResultPayload.STREAM_CODEC, IqlResultPayload.said(3, -1, 0, false,
                Text.literal("no such job")));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void isms_requestsRoundTrip(final GameTestHelper helper) {
        roundTrip(helper, RunIqlPayload.STREAM_CODEC, new RunIqlPayload(HOST, HOST.above(), 4, 7, 2,
                "QUERY items WHERE qty < 64"));
        roundTrip(helper, RequestIsmsSchemaPayload.STREAM_CODEC, new RequestIsmsSchemaPayload(HOST, 4));
        roundTrip(helper, IsmsActionPayload.STREAM_CODEC, new IsmsActionPayload(HOST, HOST.above(), 4,
                IsmsActionPayload.PLAN, "iron_ingot", 64));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void isms_schemaRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, IsmsSchemaPayload.STREAM_CODEC, new IsmsSchemaPayload(4, Text.literal("jsc-net-1a2b"),
                "desk", new IsmsSchemaPayload.Engine(IsmsSchemaPayload.EngineState.RUNNING, "Midsoft IQL Server",
                        "2012", true, ""), List.of(1284, 4, 11, 418, 37, 9),
                List.of(new IsmsSchemaPayload.Saved("low_stock", "QUERY items WHERE qty < 64")),
                List.of(new IsmsSchemaPayload.Saved("restock", "{ CRAFT 64 torch; CRAFT 8 chest }")),
                List.of(new IsmsSchemaPayload.Job("nightly_vacuum", true, "EVERY 20m", "VACUUM")),
                List.of("Server A", "Server B"),
                new IsmsSchemaPayload.Index(IndexHealth.State.STALE, 120, 2),
                List.of(new IsmsSchemaPayload.Lock("minecraft:diamond", Text.literal("Diamond"), 64L))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void isms_planAndTraceRoundTrip(final GameTestHelper helper) {
        roundTrip(helper, IsmsPlanPayload.STREAM_CODEC, new IsmsPlanPayload(4, true,
                List.of(Text.literal("CRAFT 64 piston"), Text.literal("Bench Craft: piston x64, 64 runs")),
                List.of(0, 1)));
        roundTrip(helper, IsmsTracePayload.STREAM_CODEC, new IsmsTracePayload(5, List.of(
                new TraceEvent(TraceEventClass.OPERATION_SETTLED, Text.literal("#418 CRAFT 64 iron_ingot"),
                        "steve", "desk", 64L, 312L, 9000L, List.of(Text.literal("took 312 ticks"))),
                new TraceEvent(TraceEventClass.STATEMENT_STARTING, Text.literal("QUERY items"), "steve", "desk",
                        TraceEvent.NONE, TraceEvent.NONE, 9001L, List.of()))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void nextgre_studioRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, NextgreActionPayload.STREAM_CODEC, new NextgreActionPayload(HOST, 6,
                NextgreActionPayload.EXPLAIN_ANALYZE, "CRAFT 64 piston PREFER SOURCE 'Vault B' MAX PARALLEL 2"));
        final NextgrePlanView plan = new NextgrePlanView(3, "EXPLAIN ANALYZE CRAFT 64 piston", true, 9000L,
                List.of(new NextgrePlanView.Alternative(1, Text.literal("bench recipes first"), 812L, Text.EMPTY,
                                true, List.of(Text.literal("the test rule added 7 ticks"))),
                        new NextgrePlanView.Alternative(2, Text.literal("raw materials from the fastest servers"),
                                930L, Text.literal("set aside by PREFER SOURCE"), false, List.of())),
                List.of(new NextgrePlanView.Node(-1, 1, NextgrePlanView.ROOT, Text.literal("Craft Piston x64"),
                                Text.literal("2 steps, 2 at once"), 812L, 860L, true, List.of("MAX PARALLEL 2")),
                        new NextgrePlanView.Node(0, -1, NextgrePlanView.PULL, Text.literal("Pull Oak Planks x192"),
                                Text.literal("from Vault B"), 4L, -1L, false, List.of("PREFER SOURCE Vault B"))),
                3200L, 860L, NextgrePlanView.DONE, Text.literal("plan 1 of 2 started"));
        roundTrip(helper, NextgreStudioPayload.STREAM_CODEC, new NextgreStudioPayload(6, true,
                Text.literal("NextgreIQL 16"), Text.literal("jsc-net-1a2b"), Text.EMPTY, Optional.of(plan),
                List.of(new NextgreStudioPayload.HistoryRow(3, "EXPLAIN ANALYZE CRAFT 64 piston", 812L, 860L,
                        NextgrePlanView.DONE, 9000L, true)),
                List.of(new NextgreEngine.RuleRow("nextgre:weigh_machines", Text.literal("Weigh machines"),
                        Text.literal("Also plans with machines first."), false, NextgreEngine.RuleRow.OWN_RULE)),
                List.of(new NextgreEngine.StatRow(Text.literal("Servers"), Text.literal("2")))));
        roundTrip(helper, NextgreStudioPayload.STREAM_CODEC, new NextgreStudioPayload(7, false, Text.EMPTY,
                Text.EMPTY, Text.literal("No compatible NextgreIQL was found on this network."), Optional.empty(),
                List.of(), List.of(), List.of()));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void prophet_consoleRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, ProphetActionPayload.STREAM_CODEC, new ProphetActionPayload(HOST, 4,
                ProphetActionPayload.APPLY, "KEEP uranium_fuel BETWEEN 500 AND 1000"));
        final ProphetEngine.StateRow state = new ProphetEngine.StateRow("minecraft:iron_ingot",
                Text.literal("Iron Ingot"), "KEEP iron_ingot >= 256", Text.literal("Working"),
                ProphetEngine.StateRow.TONE_WORKING, 200L, 56L, 256L, Long.MAX_VALUE,
                Text.literal("asked for 56 Iron Ingot, Operation 1a2b3c"),
                List.of(new ProphetStates.Sample(9000L, 180L, 0L), new ProphetStates.Sample(9020L, 200L, 56L)),
                List.of(Text.literal("#1a2b3c running")));
        roundTrip(helper, ProphetConsolePayload.STREAM_CODEC, new ProphetConsolePayload(4, true,
                Text.literal("Prophet YourIQL 8.0"), Text.literal("jsc-net-1a2b"), Text.EMPTY, List.of(state),
                List.of(new ProphetEngine.WatchRow(1, "WATCH redstone < 500 DO CRAFT redstone TO 1000", false, true,
                        2)),
                List.of(new ProphetEngine.ReactionRow(9020L, Text.literal("asked for 56 Iron Ingot"))),
                new ProphetEngine.Settings(20, 1024L, true), 9040L));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void terminal_selectRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, TerminalSelectPayload.STREAM_CODEC, new TerminalSelectPayload(MONITOR, HOST, logs(), 64L,
                List.of("server-a", "server-b"), TerminalSelectPayload.DEST_AUTO, ""));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void interactor_gridClickRoundTripsAtEveryPriority(final GameTestHelper helper) {
        for (final OperationPriority priority : OperationPriority.values()) {
            roundTrip(helper, NiGridClickPayload.STREAM_CODEC, new NiGridClickPayload(HOST, MONITOR, logs(), 64L,
                    NiGridClickPayload.MODE_NET_TO_LOCAL, priority));
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void operations_logRoundTripsAtEveryPriority(final GameTestHelper helper) {
        final List<OperationRecord> records = new ArrayList<>();
        for (final OperationPriority priority : OperationPriority.values()) {
            /*
             * The reason carries characters that are not one byte each on purpose: it is measured in bytes on
             * the wire and in characters where it is built, and only a name that is longer one way than the
             * other tells the two apart. The last of them is a pair of surrogates, four bytes and two chars,
             * which is what a cut lands in the middle of when nobody is careful.
             */
            records.add(new OperationRecord(new UUID(7L, priority.id()), (byte) 0, logs(), 64L, 32L, (byte) 1,
                    priority, List.of(new OperationRecord.MoveRow("rack-1", 32L, "Σ#: Programs.Restock")),
                    List.of(new OperationRecord.SubRow("rack-1", 64L, 32L, OperationRecord.SubRow.SUB_STREAMING)),
                    4, 12, OperationFailure.of("jsc.operation.failure.no_room", "Smelter Ω 𝚫")));
        }
        roundTrip(helper, OperationsLogPayload.STREAM_CODEC, new OperationsLogPayload(records));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void network_serversRoundTrip(final GameTestHelper helper) {
        roundTrip(helper, NetworkServersPayload.STREAM_CODEC, new NetworkServersPayload(List.of(
                new NetworkServersPayload.ServerEntry("n1", "Vault A", 8192L),
                new NetworkServersPayload.ServerEntry("n2", "Vault B", 0L))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void firmware_stateRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, FirmwareStatePayload.STREAM_CODEC, new FirmwareStatePayload(HOST, 2,
                new FirmwareStatePayload.Machine(Text.literal("RENDER-01"), Text.literal("Integra Centro c7 4790K"),
                        4, 4000, "x86-64", 64, Text.literal("MF ATX Standard Motherboard"), 16384, 2, 4,
                        Text.literal("Stratix Layer DDR3-8192"), Text.literal("Envya Vertex GTX 780 Ti"), 1, 2,
                        Text.literal("Standard")),
                0, -1,
                List.of(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_DISK, 0L, "jsc:frames_11",
                                Text.literal("Frames 11"), Text.literal("Vaultis Swift SSD 500 GB"), "500 GB",
                                Text.EMPTY, true, 0),
                        new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, 123L, "jsc:ubuntu",
                                Text.literal("Ubuntu installer"), Text.literal("CD drive"), "", Text.EMPTY, true, 1)),
                new FirmwareStatePayload.RaidInfo(true, 1, 2, 2, List.of(512L, 512L))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void automation_createJobRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, CreateAutomationJobPayload.STREAM_CODEC, new CreateAutomationJobPayload(HOST, MONITOR,
                CreateAutomationJobPayload.TYPE_RESTOCK_BELOW, "Logs", "minecraft:oak_log", 640L, "", "", "20s"));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cluster_stateRoundTrips(final GameTestHelper helper) {
        final ClusterManagerStatePayload.Detail detail = new ClusterManagerStatePayload.Detail(0, 0, "Kraken",
                Text.literal("8 nodes"), true, 50,
                List.of(new ClusterManagerStatePayload.WireNode(123L, 0, 1, "node-1", "Ubuntu", "sigma", 1, true, 0,
                        0, 1024L, 2048L, 1)),
                List.of(new ClusterManagerStatePayload.WireCraft(Text.literal("Iron Block x64"), "desk", 2, false)));
        final ClusterManagerStatePayload.WireJob job = new ClusterManagerStatePayload.WireJob(true, 0, "Install Ubuntu",
                0, 0, 3, 0, 5, 8, 120, false, List.of(new ClusterManagerStatePayload.WireLane("lane 1", 500)),
                Text.literal("3 of 8"));
        roundTrip(helper, ClusterManagerStatePayload.STREAM_CODEC, new ClusterManagerStatePayload(
                new ClusterManagerStatePayload.Head(true, 32, 4, "Frames 11", "Cluster Manager", Text.literal("ready")),
                List.of(new ClusterManagerStatePayload.WireCluster(0, 0, "Kraken", true, 8, 3L, 12L, 50,
                        Text.literal("8 nodes"), true)),
                detail, job,
                List.of(new NetworkItemEntry(logs(), 640L,
                        List.of(new NetworkItemEntry.StorageShare(Text.literal("rack-1"), 640L)))),
                List.of(new ClusterManagerStatePayload.WireDest(456L, Text.literal("Vault A")))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gateway_stateRoundTrips(final GameTestHelper helper) {
        final List<ItemStack> buffer = new ArrayList<>();
        for (int i = 0; i < NetworkGatewayBlockEntity.BUFFER_SLOTS; i++) {
            buffer.add(i == 0 ? new ItemStack(Items.OAK_LOG, 32) : ItemStack.EMPTY);
        }
        final GatewayManagerStatePayload.WireLog entry =
                new GatewayManagerStatePayload.WireLog("12:00", Text.literal("desk"), Text.literal("link"),
                        Text.literal("ok"), 0);
        final GatewayManagerStatePayload.Detail detail = new GatewayManagerStatePayload.Detail(123L, "gateway-1",
                Text.literal("adjacent"), true, 4000, 1, true, 750, false, 0, 0, 12, 3, "jsc_gateway_gateway_1", 5,
                buffer, List.of(entry),
                true, true, 2, 2, List.of(new GatewayManagerStatePayload.WireComputer(5, "turtle", true, false,
                        Text.literal("now"))),
                List.of(entry));
        roundTrip(helper, GatewayManagerStatePayload.STREAM_CODEC, new GatewayManagerStatePayload(
                new GatewayManagerStatePayload.Head("desk", true, "1.120.2", 12, 2),
                List.of(new GatewayManagerStatePayload.WireGateway(123L, "gateway-1", Text.literal("at 1, 2, 3"),
                        Text.literal("adjacent"), true, false)),
                detail, Text.EMPTY));
        helper.succeed();
    }

    private static StorageKey logs() {
        return StorageKey.of(new ItemStack(Items.OAK_LOG));
    }

    /** Writes the payload, reads it back, and writes what was read: the same bytes, and nothing left unread. */
    private static <T> void roundTrip(final GameTestHelper helper,
                                      final StreamCodec<? super RegistryFriendlyByteBuf, T> codec, final T payload) {
        final RegistryFriendlyByteBuf first = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        final RegistryFriendlyByteBuf second = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        try {
            codec.encode(first, payload);
            final byte[] written = ByteBufUtil.getBytes(first);
            final T read = codec.decode(first);
            helper.assertTrue(first.readableBytes() == 0, payload.getClass().getSimpleName()
                    + " left " + first.readableBytes() + " bytes unread");
            codec.encode(second, read);
            helper.assertTrue(Arrays.equals(written, ByteBufUtil.getBytes(second)),
                    payload.getClass().getSimpleName() + " does not write back to the same bytes");
        } finally {
            first.release();
            second.release();
        }
    }
}
