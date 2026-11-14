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
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A rocket's trips between the worlds, flown as a player flies them: the climb off a world into its low orbit by the
 * rocket's own engines, which spends what the climb asks, and short of it falls back.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketRoutesGameTests {

    private static final String TALL = "tall";
    private static final String BATCH = "jsspace_routes_climb";
    private static final String SHORT_BATCH = "jsspace_routes_climb_short";
    private static final BlockPos FOOT = new BlockPos(6, 2, 6);
    private static final BlockPos OWNER = new BlockPos(0, 2, 0);
    private static final String TANK = "tank_3";
    /* What a layer of the small tank holds, in kilograms. */
    private static final double LAYER = 5_800;
    /* How near the climb's cost and the page's count the flight comes, in metres a second. */
    private static final double ABOUT = 300;
    /* A rocket of one stage, about 2,500 m/s, and one of two, about 10,000. */
    private static final RocketStack ONE_STAGE = RocketStack.of(part("sea_level_engine_3"), part(TANK),
            part("capsule_3"));
    private static final RocketStack TWO_STAGES = RocketStack.of(part("sea_level_engine_3"), part(TANK, 14),
            part("separation_ring_3"), part("vacuum_engine_3"), part(TANK, 6), part("capsule_3"));

    private RocketRoutesGameTests() {
    }

    /*
     * A rocket of two stages with more delta-v than the climb to the Overworld's low orbit asks, released and lifted
     * as its count releases it, climbs by itself into space, its first stage let go on the way, and comes to its orbit
     * having spent what the climb asks.
     */
    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 6_000)
    public static void climb_spendsWhatTheClimbToTheLowOrbitAsks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, TWO_STAGES);
        final double onThePad = computer.availableToLand().orElse(-1);
        final double[] whole = new double[1];
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double width = Math.max(1, level.getWorldBorder().getSize());
        final double[] spot = SystemLayout.orbitSpot(SolarBody.OVERWORLD, SphereMap.longitude(released.pose().x(),
                width), SphereMap.latitude(released.pose().z(), width));
        final int chunkX = SectionPos.blockToSectionCoord(spot[0]);
        final int chunkZ = SectionPos.blockToSectionCoord(spot[1]);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    // Off the ground, its blocks in the yard, the computer counts all its stages give.
                    whole[0] = computerOf(level, released).availableToLand().orElse(-1);
                    space.setChunkForced(chunkX, chunkZ, true);
                    RocketLaunch.lift(level, released);
                })
                .thenWaitUntil(() -> helper.assertTrue(MovingStructures.byId(space, id) != null
                        && MovingStructures.byId(space, id).ready(), "it climbs into space"))
                // Its computer reads its tanks once a second.
                .thenWaitUntil(() -> {
                    final MovingStructure there = MovingStructures.byId(space, id);
                    final double left = computerOf(space, there).availableToLand().orElse(-1);
                    final double spent = whole[0] - left;
                    helper.assertTrue(spent >= Rocketry.SURFACE_TO_LOW_ORBIT && spent < Rocketry.SURFACE_TO_LOW_ORBIT
                                    + ABOUT && Math.abs(left - onThePad) < ABOUT,
                            "the climb to the low orbit spends what it asks, " + Rocketry.SURFACE_TO_LOW_ORBIT
                                    + " m/s, and what the pad's page said is left: the rocket had " + whole[0]
                                    + " m/s, the page said " + onThePad + " m/s for a transfer, and it has " + left
                                    + " m/s left in orbit");
                })
                .thenExecute(() -> {
                    space.setChunkForced(chunkX, chunkZ, false);
                    MovingStructures.takeAway(MovingStructures.byId(space, id));
                })
                .thenSucceed();
    }

    /*
     * A rocket of one stage with less delta-v than the climb to the Overworld's low orbit asks burns all it has at the
     * edge of space and falls back to its pad, never reaching its orbit.
     */
    @GameTest(template = TALL, batch = SHORT_BATCH, timeoutTicks = 4_000)
    public static void climb_shortOfTheLowOrbitFallsBack(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper, ONE_STAGE);
        final MovingStructure released = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final StructureUuid id = released.id();
        final ServerLevel space = SystemSpaces.spaceOf(level.getServer(), StarSystems.SOLAR_SYSTEM);
        final double start = released.pose().y();
        final double[] highest = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(released.ready(), "its body stands ready"))
                .thenExecute(() -> RocketLaunch.lift(level, released))
                .thenWaitUntil(() -> {
                    highest[0] = Math.max(highest[0], released.pose().y());
                    helper.assertTrue(MovingStructures.byId(space, id) == null, "it never reaches space");
                    helper.assertTrue(released.motion() == null && released.pose().y() < start + 1,
                            "it falls back to its pad: at " + released.pose().y() + ", highest " + highest[0]);
                })
                .thenExecute(() -> helper.assertTrue(highest[0] >= SpaceClimbs.ceiling(level) - 1,
                        "it climbed to the edge of space: " + highest[0]))
                // Its computer reads its tanks once a second.
                .thenWaitUntil(() -> helper.assertTrue(computerOf(level, released).availableToLand().orElse(-1) < 1,
                        "having burnt all it had: " + computerOf(level, released).availableToLand().orElse(-1)
                                + " m/s left"))
                .thenExecute(() -> MovingStructures.takeAway(released))
                .thenSucceed();
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
        throw new IllegalStateException("no flight computer aboard");
    }

    /* The rocket {@code stack}, built by its plan as its hangar builds it, kept by its computer, its tanks full. */
    private static FlightComputerBlockEntity build(final GameTestHelper helper, final RocketStack stack) {
        final ServerLevel level = helper.getLevel();
        final BlockPos foot = helper.absolutePos(FOOT);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), foot);
        build.start(level, helper.absolutePos(OWNER), () -> { });
        BlockPos at = foot;
        for (final PlanSpot spot : build.plan().spots()) {
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
            if (spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                at = build.where(spot);
            }
        }
        helper.assertTrue(build.complete(), "the rocket is built whole");
        build.stop(false);
        final FlightComputerBlockEntity computer = (FlightComputerBlockEntity) level.getBlockEntity(at);
        helper.assertTrue(computer != null, "its capsule holds its flight computer");
        final List<Integer> formed = new ArrayList<>();
        for (int section = 0; section < stack.sections().size(); section++) {
            formed.add(section);
        }
        computer.carry(new RocketDesign("CLIMBER", stack), foot.subtract(at), formed);
        for (int section = 0; section < stack.sections().size(); section++) {
            final RocketStack.Part part = stack.sections().get(section).part();
            if (part.id().equals(SkeletonRocketParts.id(TANK))) {
                final int tank = section;
                Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(tank,
                        TankLoad.filled(SkeletonPropellants.METHALOX, methalox, LAYER * part.layers())));
            }
        }
        return computer;
    }

    private static RocketStack.Part part(final String path) {
        return part(path, 1);
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }
}
