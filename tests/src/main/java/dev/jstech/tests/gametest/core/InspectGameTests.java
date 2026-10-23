/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.diagnostic.Health;
import dev.jstech.core.diagnostic.TickLedger;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.inspect.InspectTexts;
import dev.jstech.core.inspect.InspectWatch;
import dev.jstech.core.inspect.InspectionPayload;
import dev.jstech.core.inspect.Inspectors;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.log.LogSystem;
import dev.jstech.core.log.LogSystems;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.jstechseries.network.ConnectivityIndex;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.text.Text;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestHeat;
import dev.jstech.tests.TestNetworkMembers;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The inspection of a place: a chest's block, state and fields and what it offers, every face alike said once; a
 * block of heat's temperature; a member of the data network, on its network; a part that fails said and the rest
 * read; the command, and the live view a player watches, as it travels; and the state of the systems counting the
 * bodies of heat awake and the moving structures.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class InspectGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final double HOT = 600.0;

    private InspectGameTests() {
    }

    @GameTest(template = ARENA)
    public static void inspect_readsTheBlockItsFieldsAndWhatItOffersOnEveryFaceOnce(final GameTestHelper helper) {
        helper.setBlock(AT, Blocks.CHEST);
        final ChestBlockEntity chest = helper.getBlockEntity(AT);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        final List<Inspectors.Line> lines = linesOf(Inspectors.read(helper.getLevel(), helper.absolutePos(AT)),
                CoreLogs.CONTENT);
        helper.assertTrue(has(lines, InspectTexts.BLOCK.text(), "minecraft:chest"), "the block: " + lines);
        helper.assertTrue(has(lines, Text.literal("waterlogged"), "false"), "its state");
        helper.assertTrue(has(lines, InspectTexts.BLOCK_ENTITY.text(), "minecraft:chest"), "its block entity");
        helper.assertTrue(has(lines, Text.literal("Items"), "1 entries"), "its fields, as they are saved");
        helper.assertTrue(has(lines, Text.literal("0"), "minecraft:diamond x3")
                && has(lines, InspectTexts.ITEMS.text(), "1 / 27"), "the items it offers");
        final long offers = lines.stream().filter(line -> line.name() instanceof Text.Translated translated
                && translated.key().equals(InspectTexts.OFFERS)).count();
        helper.assertTrue(offers == 1L && has(lines, InspectTexts.OFFERS.with(InspectTexts.ALL_FACES.text()), ""),
                "offered alike on every face, so said once: " + offers);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void inspect_readsAHeatBodyAndHealthCountsTheBodiesAwake(final GameTestHelper helper) {
        helper.setBlock(AT, TestHeat.COPPER.get());
        final TestHeat.HeatBlockEntity entity = helper.getBlockEntity(AT);
        final HeatBody body = entity.heatBody();
        helper.runAfterDelay(2, () -> {
            body.set(HOT);
            final List<Inspectors.Line> lines = linesOf(Inspectors.read(helper.getLevel(),
                    helper.absolutePos(AT)), CoreLogs.CONTENT);
            helper.assertTrue(lines.stream().anyMatch(line -> line.name().equals(InspectTexts.HEAT.text())
                    && line.value().startsWith("600.") && line.value().endsWith(" K")), "its heat: " + lines);
            final String dimension = helper.getLevel().dimension().location().toString();
            helper.assertTrue(healthOf(helper, CoreLogs.HEAT).stream()
                    .anyMatch(line -> line.text().english().startsWith(dimension)),
                    "and the state of the systems counts it among the bodies of heat awake");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void inspect_readsAMemberOfTheDataNetworkOnItsNetwork(final GameTestHelper helper) {
        final ConnectivityIndex connectivity = NetworkSystem.get(helper.getLevel()).connectivity();
        final long cable = helper.absolutePos(AT).asLong();
        final NetworkUuid network = new NetworkUuid(UUID.randomUUID());
        connectivity.onCablePlaced(cable, Set.of(), new DataLink(DataLine.ACCESS, HardwareEra.STANDARD));
        connectivity.assignUuid(cable, network);
        helper.setBlock(AT.west(), TestNetworkMembers.REACTOR.get());
        helper.startSequence().thenWaitUntil(() -> {
            final List<Inspectors.Line> lines = linesOf(Inspectors.read(helper.getLevel(),
                    helper.absolutePos(AT.west())), CoreLogs.NETWORK);
            helper.assertTrue(has(lines, InspectTexts.NETWORK.text(), network.value().toString()),
                    "the network it is on: " + lines);
            helper.assertTrue(has(lines, InspectTexts.MEMBERS.text(), "1"), "with its members");
        }).thenExecute(() -> {
            connectivity.onCableRemoved(cable);
            connectivity.clearNetwork(network);
        }).thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void inspect_saysAPartThatFailsAndReadsTheRest(final GameTestHelper helper) {
        Probes.failing(true);
        try {
            final List<Inspectors.Part> parts = Inspectors.read(helper.getLevel(), helper.absolutePos(AT));
            helper.assertTrue(has(linesOf(parts, Probes.LOG), InspectTexts.FAILED.text(),
                    "java.lang.IllegalStateException: the probe fails on purpose"), "the part that fails says so");
            helper.assertTrue(has(linesOf(parts, CoreLogs.CONTENT), InspectTexts.BLOCK.text(), "minecraft:air"),
                    "and the rest are read");
        } finally {
            Probes.failing(false);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void commands_inspectAPlaceAndAPlayerWatchesIt(final GameTestHelper helper)
            throws CommandSyntaxException {
        helper.setBlock(AT, Blocks.CHEST);
        final BlockPos pos = helper.absolutePos(AT);
        final MinecraftServer server = helper.getLevel().getServer();
        final CommandSourceStack source = server.createCommandSourceStack().withLevel(helper.getLevel())
                .withSuppressedOutput();
        helper.assertTrue(server.getCommands().getDispatcher().execute("jstech inspect " + pos.getX() + " "
                + pos.getY() + " " + pos.getZ(), source) > 0, "the command reads the place's parts");
        final ServerPlayer player = player(helper);
        try {
            InspectWatch.watch(player, pos);
            helper.assertTrue(InspectWatch.watched(player).equals(Optional.of(pos)), "the player watches it");
            final InspectionPayload payload = InspectWatch.payloadOf(player, pos);
            helper.assertTrue(payload.lines().stream().anyMatch(line -> line.depth() == 0
                    && line.name().getString().equals(CoreLogs.CONTENT.id())), "under each system's name");
            helper.assertTrue(payload.lines().stream().anyMatch(line -> line.depth() == 1
                    && line.value().equals("minecraft:chest")), "what it says, a line under it");
            final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                    helper.getLevel().registryAccess());
            InspectionPayload.STREAM_CODEC.encode(buffer, payload);
            final InspectionPayload read = InspectionPayload.STREAM_CODEC.decode(buffer);
            helper.assertTrue(read.pos().equals(pos) && read.lines().size() == payload.lines().size()
                    && read.lines().getLast().name().getString().equals(payload.lines().getLast().name()
                    .getString()), "and it travels whole");
            InspectWatch.stop(player);
            helper.assertTrue(InspectWatch.watched(player).isEmpty(), "until they stop");
        } finally {
            server.getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void payload_carriesSoManyLinesAndATicksPacketsByKind(final GameTestHelper helper) {
        final List<InspectionPayload.Line> many = new ArrayList<>();
        for (int i = 0; i < InspectionPayload.MOST_LINES + 50; i++) {
            many.add(new InspectionPayload.Line(0, Component.literal(String.valueOf(i)), "x".repeat(300)));
        }
        final InspectionPayload payload = new InspectionPayload(BlockPos.ZERO, many, List.of());
        helper.assertTrue(payload.lines().size() == InspectionPayload.MOST_LINES, "at most so many lines");
        helper.assertTrue(payload.lines().getFirst().value().length() == InspectionPayload.LONGEST_VALUE,
                "each value cut to its longest");
        final List<InspectionPayload.Line> sent = InspectWatch.linesOf(new TickLedger.Tick(42L, List.of(
                new TickLedger.Sent("jscore:hazards", 2L, 300L, 200L), new TickLedger.Sent("minecraft:move_entity",
                        5L, 100L, 20L))));
        helper.assertTrue(sent.size() == 3 && sent.getFirst().depth() == 0, "the tick, then each kind under it");
        helper.assertTrue(sent.get(1).name().getString().equals("jscore:hazards")
                && sent.get(1).value().equals("2, 300 bytes"), "with how many and how many bytes");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_assembly")
    public static void health_countsTheMovingStructures(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(AT, Blocks.STONE);
        helper.setBlock(AT.east(), Blocks.STONE);
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(AT),
                helper.absolutePos(AT.east()));
        helper.assertTrue(made.ok() && made.structure() != null, "a structure is made");
        try {
            final String dimension = level.dimension().location().toString();
            helper.assertTrue(healthOf(helper, CoreLogs.MOVING).stream()
                    .anyMatch(line -> line.text().english().startsWith(dimension)),
                    "the state of the systems counts the moving structures");
        } finally {
            MovingStructures.takeAway(made.structure());
        }
        helper.succeed();
    }

    /* Every line {@code system} said, in all its parts. */
    private static List<Inspectors.Line> linesOf(final List<Inspectors.Part> parts, final LogSystem system) {
        return parts.stream().filter(part -> part.system() == system).flatMap(part -> part.lines().stream())
                .toList();
    }

    private static boolean has(final List<Inspectors.Line> lines, final Text name, final String value) {
        return lines.stream().anyMatch(line -> line.name().equals(name) && line.value().equals(value));
    }

    private static List<Health.Line> healthOf(final GameTestHelper helper, final LogSystem system) {
        return Health.read(helper.getLevel().getServer()).stream().filter(part -> part.system() == system)
                .flatMap(part -> part.lines().stream()).toList();
    }

    /* A player of a game connected to the server, with every mod's channels, as the Core's payloads go to every one. */
    private static ServerPlayer player(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                "inspector"), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation());
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    /* Loads the chunks round the yard's next plot, so a structure is made at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }

    /* A part of the inspection that fails when asked to, declared once for the whole run. */
    private static final class Probes {

        private static final LogSystem LOG = LogSystems.declare(JsTests.MODID, "inspect_probe");
        private static volatile boolean failing;

        static {
            Inspectors.provide(LOG, (level, pos, report) -> {
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
    }
}
