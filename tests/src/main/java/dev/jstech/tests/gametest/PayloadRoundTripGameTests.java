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
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload;
import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.NetworkServersPayload;
import dev.jstech.computers.operation.payload.NiGridClickPayload;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.OperationsLogPayload;
import dev.jstech.computers.operation.payload.TerminalSelectPayload;
import dev.jstech.computers.operation.payload.ThisPcPayload;
import dev.jstech.computers.operation.payload.UiEventPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.operation.OperationFailure;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.tests.JsTests;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        final ThisPcPayload.WireMachine machine = new ThisPcPayload.WireMachine("desk", "Personal Computer", "Standard",
                "Frames 11", 2021, "on CORE", "MF ATX Standard", "Integra Apex 7 4790K", 1, "x86-64, 64-bit",
                16384, 3072, 1, "PSU 650G", true, "Monitor");
        final ThisPcPayload.WireDisk disk = new ThisPcPayload.WireDisk(0, "Vaultis Swift SSD 500 GB", 2000L, 120L, true,
                "C:\\", 80L, 20L, 20L);
        final ThisPcPayload.WireMedia media = new ThisPcPayload.WireMedia(123L, "D:", "Frames 11 USB", "OS", "FRAMES",
                true, "Frames 11", 2021, "jsc:frames_11", "", 0L, 2);
        roundTrip(helper, ThisPcPayload.STREAM_CODEC,
                new ThisPcPayload(machine, List.of(disk), List.of(media), List.of("jsc:nms")));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void files_diskFilesRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, DiskFilesPayload.STREAM_CODEC, new DiskFilesPayload("C:\\Users",
                List.of(new DiskFilesPayload.WireFile("C:\\notes.txt", "txt", 4L, false, false),
                        new DiskFilesPayload.WireFile("C:\\Data", "", 0L, false, true, "minecraft:oak_log", 640L)),
                List.of(new DiskFilesPayload.WireVolume("c", "System"))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void iql_resultRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, IqlResultPayload.STREAM_CODEC, new IqlResultPayload(true, "2 rows",
                List.of(new IqlResultPayload.Row("Oak Log", 640L), new IqlResultPayload.Row("Iron Ingot", 12L))));
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
                new FirmwareStatePayload.Machine("RENDER-01", "Integra Apex 7 4790K", 4, 4000, "x86-64", 64,
                        "MF ATX Standard Motherboard", 16384, 2, 4, "Stratix DDR3-8192",
                        "Visara Vertex GTX 780 Ti", 1, 2, "Standard"),
                0, -1,
                List.of(new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_DISK, 0L, "jsc:frames_11",
                                "Frames 11", "Vaultis Swift SSD 500 GB", "500 GB", "", true, 0),
                        new FirmwareStatePayload.Entry(FirmwareStatePayload.KIND_MEDIA, 123L, "jsc:ubuntu",
                                "Ubuntu installer", "CD drive", "", "", true, 1)),
                new FirmwareStatePayload.RaidInfo(true, 1, 2, 2, List.of(512L, 512L))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void automation_createJobRoundTrips(final GameTestHelper helper) {
        roundTrip(helper, CreateAutomationJobPayload.STREAM_CODEC, new CreateAutomationJobPayload(HOST, MONITOR,
                CreateAutomationJobPayload.TYPE_KEEP_STOCK, "Logs", "minecraft:oak_log", 640L, "", "", "20s"));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cluster_stateRoundTrips(final GameTestHelper helper) {
        final ClusterManagerStatePayload.Detail detail = new ClusterManagerStatePayload.Detail(0, 0, "Kraken",
                "8 nodes", true, 50,
                List.of(new ClusterManagerStatePayload.WireNode(123L, 0, 1, "node-1", "Ubuntu", "sigma", 1, true, 0,
                        0, 1024L, 2048L, 1)),
                List.of(new ClusterManagerStatePayload.WireCraft("Iron Block x64", "desk", 2, false)));
        final ClusterManagerStatePayload.WireJob job = new ClusterManagerStatePayload.WireJob(true, 0, "Install Ubuntu",
                0, 0, 3, 0, 5, 8, 120, false, List.of(new ClusterManagerStatePayload.WireLane("lane 1", 500)), "3 of 8");
        roundTrip(helper, ClusterManagerStatePayload.STREAM_CODEC, new ClusterManagerStatePayload(
                new ClusterManagerStatePayload.Head(true, 32, 4, "Frames 11", "Cluster Manager", "ready"),
                List.of(new ClusterManagerStatePayload.WireCluster(0, 0, "Kraken", true, 8, 3L, 12L, 50, "8 nodes", true)),
                detail, job,
                List.of(new NetworkItemEntry(logs(), 640L, List.of(new NetworkItemEntry.StorageShare("rack-1", 640L)))),
                List.of(new ClusterManagerStatePayload.WireDest(456L, "Vault A"))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gateway_stateRoundTrips(final GameTestHelper helper) {
        final List<ItemStack> buffer = new ArrayList<>();
        for (int i = 0; i < NetworkGatewayBlockEntity.BUFFER_SLOTS; i++) {
            buffer.add(i == 0 ? new ItemStack(Items.OAK_LOG, 32) : ItemStack.EMPTY);
        }
        final GatewayManagerStatePayload.WireLog entry =
                new GatewayManagerStatePayload.WireLog("12:00", "desk", "link", "ok", 0);
        final GatewayManagerStatePayload.Detail detail = new GatewayManagerStatePayload.Detail(123L, "gateway-1",
                "adjacent", 4000, 1, true, 750, false, 0, 0, 12, 3, "jsc_gateway_gateway_1", 5, buffer, List.of(entry),
                true, true, 2, 2, List.of(new GatewayManagerStatePayload.WireComputer(5, "turtle", true, false, "now")),
                List.of(entry));
        roundTrip(helper, GatewayManagerStatePayload.STREAM_CODEC, new GatewayManagerStatePayload(
                new GatewayManagerStatePayload.Head("desk", true, "1.120.2", 12, 2),
                List.of(new GatewayManagerStatePayload.WireGateway(123L, "gateway-1", "adjacent", true, false)),
                detail, ""));
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
