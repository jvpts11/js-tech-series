/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * A capsule's Crew Seat is sat on once its capsule is formed, whether the rocket stands on its pad or flies: a player
 * sits on it on the pad, stays sat on it as the rocket is released and its blocks become one body, and stays sat on
 * it as that body is set down again; a Crew Seat standing alone, of no capsule formed, is not sat on.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CrewSeatGameTests {

    private static final String ARENA = "bench";
    private static final String BATCH = "jsspace_crew_seat";
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    private static final BlockPos ALONE = new BlockPos(20, 2, 20);

    private CrewSeatGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void crewSeat_isSatOnOnThePadAndStaysSatThroughReleaseAndSettingDown(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos[] places = build(helper);
        final BlockPos computer = places[0];
        final BlockPos seat = places[1];
        final ServerPlayer crew = player(helper, "crew-seat");
        crew.moveTo(seat.getX() + 0.5, seat.getY() + 1, seat.getZ() + 0.5);
        helper.assertTrue(MovingStructures.sit(crew, seat), "the formed capsule's seat is sat on, on its pad: "
                + level.getBlockState(seat) + ", a seat " + BlockAboard.at(level, seat).seat().isPresent()
                + ", riding " + crew.getVehicle());
        helper.assertTrue(crew.getVehicle() instanceof StructureSeatEntity sitting && sitting.seat().equals(seat),
                "the player sits on it there");
        final MovingStructure body = RocketLaunch.release(level, computer, crew.getUUID()).orElse(null);
        helper.assertTrue(body != null, "the rocket is released");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    helper.assertTrue(crew.getVehicle() instanceof StructureSeatEntity sitting
                            && sitting.structure() == body, "the player sits on the same seat aboard, released");
                    helper.assertTrue(MovingStructures.setDown(body).ok(), "the rocket is set down");
                })
                .thenExecute(() -> helper.assertTrue(crew.getVehicle() instanceof StructureSeatEntity sitting
                        && !YardLayout.isYardBlock(sitting.seat().getX()) && sitting.structure() == null,
                        "and still sits on it, set down in the world"))
                .thenExecute(() -> {
                    crew.stopRiding();
                    level.getServer().getPlayerList().remove(crew);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void crewSeat_ofNoCapsuleFormedIsNotSatOn(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(ALONE, RocketKitBlocks.CREW_SEAT.get());
        final ServerPlayer crew = player(helper, "seat-alone");
        final boolean sat = MovingStructures.sit(crew, helper.absolutePos(ALONE));
        level.getServer().getPlayerList().remove(crew);
        helper.assertFalse(sat, "a Crew Seat of no capsule formed is no seat");
        helper.succeed();
    }

    /* A player of a game connected to the server, with every mod's channels, in the test's arena. */
    private static ServerPlayer player(final GameTestHelper helper, final String name) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                name), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation());
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /* A rocket built whole on the arena's floor, its capsule formed: its flight computer's place and a seat's. */
    private static BlockPos[] build(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
        final BlockPos foot = helper.absolutePos(FOOT);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), foot);
        build.start(level, helper.absolutePos(OWNER), () -> { });
        BlockPos computer = null;
        BlockPos seat = null;
        for (final PlanSpot spot : build.plan().spots()) {
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
            if (spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                computer = build.where(spot);
            } else if (spot.block().equals(RocketKitBlocks.CREW_SEAT.getId())) {
                seat = build.where(spot);
            }
        }
        helper.assertTrue(build.complete(), "the rocket is built whole");
        build.stop(false);
        if (computer == null || seat == null
                || !(level.getBlockEntity(computer) instanceof FlightComputerBlockEntity rocket)) {
            throw new GameTestAssertException("its capsule holds a flight computer and a seat");
        }
        final List<Integer> formed = new ArrayList<>();
        for (int section = 0; section < stack.sections().size(); section++) {
            formed.add(section);
        }
        rocket.carry(new RocketDesign("SEATED", stack), foot.subtract(computer), formed);
        return new BlockPos[] {computer, seat};
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
