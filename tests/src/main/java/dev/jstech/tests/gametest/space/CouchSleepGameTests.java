/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.cruise.CouchSleep;
import dev.jstech.space.cruise.RocketCruise;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.PlannedTransfer;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import dev.jstech.testkit.gametest.GameTestPlayers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Sleeping in a crew seat: its key does nothing on the pad; out in space it puts the player sitting in the capsule's
 * seat to sleep and wakes them again, and leaving the seat wakes them. On the cruise, the only one aboard asleep in
 * the seat as long as one in a bed must be, the cruise passes, and they wake at its end.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CouchSleepGameTests {

    private static final String ARENA = "bench";
    private static final String BATCH = "jsspace_couch_sleep";
    private static final String CRUISE_BATCH = "jsspace_couch_sleep_cruise";
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    /* The tank filled for the transfer burn; that burn's and the capture's delta-v, in metres a second. */
    private static final int TANK = 1;
    private static final double LOAD = 2_000;
    private static final double BURN = 100;
    private static final double CAPTURE = 30;
    /* A cruise far longer than the test, which only sleep can end. */
    private static final long LONG_WAY = 1_000_000;

    private CouchSleepGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void couchSleep_inSpaceSleepsTillTheKeyOrLeavingTheSeat(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos[] places = build(helper);
        final ServerPlayer crew = GameTestPlayers.connected(helper, "couch_sleeper");
        crew.moveTo(places[1].getX() + 0.5, places[1].getY() + 1, places[1].getZ() + 0.5);
        helper.assertTrue(MovingStructures.sit(crew, places[1]), "the player sits in the capsule's seat on the pad");
        CouchSleep.pressed(crew);
        helper.assertFalse(CouchSleep.asleep(crew), "on the pad the key puts no one to sleep");
        final MovingStructure released = RocketLaunch.release(level, places[0], crew.getUUID()).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    forced(space, spot, true);
                    MovingStructures.travel(released, space, new Vec3(spot[0], SystemLayout.ORBIT_Y, spot[1]));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(space, id) != null && body(space, id).ready()
                        && crew.getVehicle() instanceof StructureSeatEntity, "it rests in the Overworld's orbit, the"
                        + " player in its seat: riding " + crew.getVehicle()))
                .thenExecute(() -> {
                    CouchSleep.pressed(crew);
                    helper.assertTrue(CouchSleep.asleep(crew), "out in space the key puts them to sleep in the seat: "
                            + seated(crew));
                    CouchSleep.pressed(crew);
                    helper.assertFalse(CouchSleep.asleep(crew), "and wakes them");
                    CouchSleep.pressed(crew);
                    crew.stopRiding();
                    crew.doTick();
                    helper.assertFalse(CouchSleep.asleep(crew), "leaving the seat wakes them");
                    GameTestPlayers.leave(crew);
                    forced(space, spot, false);
                    MovingStructures.takeAway(body(space, id));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = CRUISE_BATCH, timeoutTicks = 1200)
    public static void couchSleep_theCrewAsleepTheCruisePasses(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos[] places = build(helper);
        final ServerPlayer crew = GameTestPlayers.connected(helper, "cruise_sleeper");
        crew.moveTo(places[1].getX() + 0.5, places[1].getY() + 1, places[1].getZ() + 0.5);
        helper.assertTrue(MovingStructures.sit(crew, places[1]), "the player sits in the capsule's seat on the pad");
        final MovingStructure released = RocketLaunch.release(level, places[0], crew.getUUID()).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, 0);
        final FlightComputerBlockEntity[] aboard = new FlightComputerBlockEntity[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    forced(space, spot, true);
                    MovingStructures.travel(released, space, new Vec3(spot[0], SystemLayout.ORBIT_Y, spot[1]));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(space, id) != null && body(space, id).ready()
                        && crew.getVehicle() instanceof StructureSeatEntity, "it rests in the Overworld's orbit, the"
                        + " player in its seat: riding " + crew.getVehicle()))
                .thenExecute(() -> aboard[0] = computerOf(space, body(space, id)))
                .thenWaitUntil(() -> helper.assertTrue(ticking(space, aboard[0].getBlockPos()),
                        "its flight computer's place in the yard ticks"))
                .thenExecute(() -> aboard[0].execute(new PlannedTransfer(SolarSystemWorlds.MOON,
                        space.getGameTime(), BURN, CAPTURE, LONG_WAY, false, false)))
                .thenWaitUntil(() -> helper.assertTrue(body(space, id).motion() instanceof RocketCruise
                        && aboard[0].cruising(), "its window come, it sets out on a cruise far longer than the test"))
                .thenExecute(() -> {
                    CouchSleep.pressed(crew);
                    helper.assertTrue(CouchSleep.asleep(crew), "on the cruise the player falls asleep in the seat");
                })
                .thenWaitUntil(() -> helper.assertTrue(!aboard[0].cruising() && body(space, id).motion() == null,
                        "the only one aboard asleep, the cruise passes"))
                .thenExecute(() -> {
                    helper.assertFalse(CouchSleep.asleep(crew), "and they wake at its end");
                    crew.stopRiding();
                    GameTestPlayers.leave(crew);
                    forced(space, spot, false);
                    MovingStructures.takeAway(body(space, id));
                })
                .thenSucceed();
    }

    /* Where {@code crew} sits, for a test's failure to tell. */
    private static String seated(final ServerPlayer crew) {
        if (!(crew.getVehicle() instanceof StructureSeatEntity seat)) {
            return "riding " + crew.getVehicle();
        }
        return "the player in " + crew.level().dimension().location() + ", the seat in "
                + seat.level().dimension().location() + " at " + seat.seat().toShortString() + ", "
                + seat.level().getBlockState(seat.seat()) + ", of " + seat.structure();
    }

    /*
     * Whether the block entity at {@code pos} ticks: the game ticks one only where its blocks tick and its chunk's
     * entities have loaded, which may come later.
     */
    private static boolean ticking(final ServerLevel level, final BlockPos pos) {
        return level.shouldTickBlocksAt(pos) && level.areEntitiesLoaded(ChunkPos.asLong(pos));
    }

    private static void forced(final ServerLevel space, final double[] spot, final boolean kept) {
        space.setChunkForced(SectionPos.blockToSectionCoord(spot[0]), SectionPos.blockToSectionCoord(spot[1]), kept);
    }

    private static @Nullable MovingStructure body(final ServerLevel space, final StructureUuid id) {
        return MovingStructures.byId(space, id);
    }

    /* The flight computer aboard {@code body}, in its yard. */
    private static FlightComputerBlockEntity computerOf(final ServerLevel level, final MovingStructure body) {
        final Box box = body.bounds();
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (level.getBlockEntity(pos) instanceof FlightComputerBlockEntity computer) {
                return computer;
            }
        }
        throw new GameTestAssertException("no flight computer aboard");
    }

    /*
     * A rocket built whole on the arena's floor, its capsule formed and its tank filled: its flight computer's place
     * and its seat's.
     */
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
        rocket.carry(new RocketDesign("SLEEPER", stack), foot.subtract(computer), formed);
        Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(TANK,
                TankLoad.filled(SkeletonPropellants.METHALOX, methalox, LOAD)));
        return new BlockPos[] {computer, seat};
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
