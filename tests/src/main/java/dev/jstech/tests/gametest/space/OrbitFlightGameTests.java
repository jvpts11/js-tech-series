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
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.cruise.RocketCruise;
import dev.jstech.space.descent.DescentFlight;
import dev.jstech.space.descent.DescentPhase;
import dev.jstech.space.descent.SurfaceLaunch;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.PlannedLanding;
import dev.jstech.space.flight.PlannedTransfer;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.orbit.OrbitObjects;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * A rocket in a world's orbit and on a world's ground, as its flight computer flies it: in an orbit it plans its
 * transfer from that world and maps that world to land on; a transfer it has not the delta-v for, its window come, is
 * let go rather than waited on for ever; a landing set on another world than the one it orbits is held and let go;
 * landed, the climb to the orbit comes out of what it has; a lander with nothing over its descent stage to climb on
 * stays, its transfer let go; and a rocket bound for an orbit where another rests comes to rest beside it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OrbitFlightGameTests {

    private static final String ARENA = "bench";
    /* Each flight to an orbit a batch of its own, so no two rockets rest at one world's spot at once. */
    private static final String PLANS = "jsspace_orbit_plans";
    private static final String ARRIVALS = "jsspace_orbit_arrivals";
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OTHER_FOOT = new BlockPos(8, 1, 2);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    private static final double LOAD = 2_000;
    private static final int TANK = 1;
    /* A transfer burn far past what the test's rocket has, and a small one; a cruise long enough to look at. */
    private static final double TOO_MUCH = 1.0e6;
    private static final double SMALL = 50;
    private static final long LONG_WAY = 1_000_000;
    private static final int ORBIT_TIME = 6_000;

    private OrbitFlightGameTests() {
    }

    @GameTest(template = ARENA, batch = PLANS, timeoutTicks = ORBIT_TIME)
    public static void orbit_plansFromTheWorldItOrbitsAndLetsGoWhatItCannotFly(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity built = build(helper, FOOT, crewed(), TANK);
        final MovingStructure released = RocketLaunch.release(level, built.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    forced(space, spot, true);
                    MovingStructures.travel(released, space, new Vec3(spot[0], SystemLayout.ORBIT_Y, spot[1]));
                })
                .thenWaitUntil(() -> helper.assertTrue(body(space, id) != null && body(space, id).ready(),
                        "it rests in the Overworld's orbit"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity computer = computerOf(space, body(space, id));
                    helper.assertTrue(computer.orbiting().equals(Optional.of(SolarBody.OVERWORLD)),
                            "it knows the orbit it is in: " + computer.orbiting());
                    helper.assertTrue(computer.plan().isPresent() && computer.plan().get().from()
                            == SolarBody.OVERWORLD, "it plans its transfer from there: " + computer.plan());
                    helper.assertTrue(!computer.destinations().contains(SolarSystemWorlds.OVERWORLD)
                            && computer.destinations().size() == 2, "to the other worlds: " + computer.destinations());
                    helper.assertTrue(computer.landingWorld().equals(SolarSystemWorlds.OVERWORLD),
                            "and maps the world it orbits to land on");
                    computer.execute(new PlannedTransfer(SolarSystemWorlds.MOON, space.getGameTime(), TOO_MUCH, 0,
                            LONG_WAY, false, false));
                })
                .thenWaitUntil(() -> helper.assertTrue(computerOf(space, body(space, id)).executed().isEmpty(),
                        "its window come and its delta-v short, the transfer is let go"))
                .thenExecute(() -> {
                    helper.assertTrue(body(space, id).motion() == null, "and it stays in its orbit");
                    final FlightComputerBlockEntity computer = computerOf(space, body(space, id));
                    OrbitObjects.arrived(space, body(space, id), SolarBody.OVERWORLD);
                    final boolean began = DescentFlight.begin(space, body(space, id), computer, new PlannedLanding(
                            SolarSystemWorlds.MOON, 0, 0));
                    helper.assertTrue(!began && computer.landing().isEmpty() && computer.descent()
                            == DescentPhase.NONE && body(space, id).motion() == null, "a landing set on the Moon,"
                            + " from the Overworld's orbit, is held and let go");
                    forced(space, spot, false);
                    MovingStructures.takeAway(body(space, id));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = ORBIT_TIME)
    public static void landed_reckonsTheClimbToItsOrbitOutOfWhatItHas(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity built = build(helper, FOOT, crewed(), TANK);
        final MovingStructure released = RocketLaunch.release(level, built.getBlockPos(), null).orElseThrow();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity computer = computerOf(level, released);
                    final double flying = computer.available().orElse(0);
                    computer.landed();
                    final double landed = computer.available().orElse(0);
                    helper.assertTrue(flying > 0 && landed < flying, "landed on a world, the climb to its orbit"
                            + " comes out of its delta-v: " + flying + " flying, " + landed + " landed");
                    MovingStructures.takeAway(released);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = ORBIT_TIME)
    public static void liftOff_withNothingThatClimbsLetsTheTransferGo(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // A lander whose ascent stage holds nothing: its descent stage would stay, and nothing over it can climb.
        final RocketStack lander = RocketStack.of(part("lunar_lander_descent"), part("lunar_lander_ascent"));
        final FlightComputerBlockEntity built = build(helper, FOOT, lander, -1);
        final MovingStructure released = RocketLaunch.release(level, built.getBlockPos(), null).orElseThrow();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    final FlightComputerBlockEntity computer = computerOf(level, released);
                    computer.landed();
                    computer.execute(new PlannedTransfer(SolarSystemWorlds.MOON, level.getGameTime(), SMALL, 0,
                            LONG_WAY, false, false));
                    helper.assertTrue(!SurfaceLaunch.liftOff(level, released, computer)
                            && computer.executed().isEmpty() && released.motion() == null
                            && computer.separated() == 0, "it stays whole on the ground, its transfer let go");
                    MovingStructures.takeAway(released);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = ARRIVALS, timeoutTicks = ORBIT_TIME)
    public static void arrival_restsBesideARocketAlreadyInTheOrbit(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity resting = build(helper, FOOT, crewed(), TANK);
        final FlightComputerBlockEntity coming = build(helper, OTHER_FOOT, crewed(), TANK);
        final MovingStructure first = RocketLaunch.release(level, resting.getBlockPos(), null).orElseThrow();
        final MovingStructure second = RocketLaunch.release(level, coming.getBlockPos(), null).orElseThrow();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double[] moon = SystemLayout.orbitSpot(SolarBody.MOON, 0, 0);
        final double[] home = SystemLayout.orbitSpot(SolarBody.OVERWORLD, 0, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(first.ready() && second.ready(), "their bodies stand ready"))
                .thenExecute(() -> {
                    forced(space, moon, true);
                    forced(space, home, true);
                    MovingStructures.travel(first, space, new Vec3(moon[0], SystemLayout.ORBIT_Y, moon[1]));
                    MovingStructures.travel(second, space, new Vec3(home[0], SystemLayout.ORBIT_Y, home[1]));
                })
                .thenWaitUntil(() -> helper.assertTrue(ready(space, first.id()) && ready(space, second.id()),
                        "one rests in the Moon's orbit, the other in the Overworld's"))
                .thenExecute(() -> computerOf(space, body(space, second.id())).execute(new PlannedTransfer(
                        SolarSystemWorlds.MOON, space.getGameTime(), SMALL, 0, LONG_WAY, false, false)))
                .thenWaitUntil(() -> helper.assertTrue(body(space, second.id()).motion() instanceof RocketCruise,
                        "the other sets out for the Moon"))
                .thenExecute(() -> {
                    final RocketCruise cruise = (RocketCruise) body(space, second.id()).motion();
                    final MovingStructure there = body(space, first.id());
                    final double apart = Math.hypot(cruise.toX() - there.pose().x(), cruise.toZ()
                            - there.pose().z());
                    final double room = (there.worldBox().getXsize() + body(space, second.id()).worldBox()
                            .getXsize()) / 2;
                    helper.assertTrue(apart > room, "it is bound for a spot beside the rocket resting there, not"
                            + " inside it: " + apart + " blocks apart, " + room + " needed");
                    forced(space, moon, false);
                    forced(space, home, false);
                    MovingStructures.takeAway(there);
                    MovingStructures.takeAway(body(space, second.id()));
                })
                .thenSucceed();
    }

    /* Keeps the chunk of the orbit spot {@code spot} of {@code space} going, as a crew aboard would, or lets it go. */
    private static void forced(final ServerLevel space, final double[] spot, final boolean kept) {
        space.setChunkForced(SectionPos.blockToSectionCoord(spot[0]), SectionPos.blockToSectionCoord(spot[1]), kept);
    }

    private static boolean ready(final ServerLevel space, final StructureUuid id) {
        return body(space, id) != null && body(space, id).ready();
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

    /* An engine, a tank and a capsule. */
    private static RocketStack crewed() {
        return RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
    }

    /*
     * Builds {@code stack} at {@code foot} of the arena, every part formed, and fills the tank at {@code tank}, or none
     * where it is below nought.
     */
    private static FlightComputerBlockEntity build(final GameTestHelper helper, final BlockPos foot,
                                                   final RocketStack stack, final int tank) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(foot);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), at);
        build.start(level, helper.absolutePos(OWNER), () -> { });
        BlockPos computerAt = at;
        for (final PlanSpot spot : build.plan().spots()) {
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
            if (spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                computerAt = build.where(spot);
            }
        }
        helper.assertTrue(build.complete(), "the rocket is built whole");
        build.stop(false);
        final FlightComputerBlockEntity computer = (FlightComputerBlockEntity) level.getBlockEntity(computerAt);
        helper.assertTrue(computer != null, "it holds its flight computer");
        final List<Integer> formed = new ArrayList<>();
        for (int section = 0; section < stack.sections().size(); section++) {
            formed.add(section);
        }
        computer.carry(new RocketDesign("ORBITER", stack), at.subtract(computerAt), formed);
        if (tank >= 0) {
            Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(tank,
                    TankLoad.filled(SkeletonPropellants.METHALOX, methalox, LOAD)));
        }
        return computer;
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
