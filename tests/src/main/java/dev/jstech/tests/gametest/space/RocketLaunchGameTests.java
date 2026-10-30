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
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.flight.FlightComputerBlock;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.rocket.CabinLamp;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A rocket's launch: released, its blocks become one moving structure by the places of its plan, its parts still
 * formed and its flight computer keeping the plan, told as a rocket with what that computer kept in its tanks; once
 * ready it climbs on its engines' thrust, faster as it burns lighter, and its cabins read the flight.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketLaunchGameTests {

    private static final String TALL = "tall";
    private static final String BATCH = "jsspace_launch";
    /* Where the rocket's foot stands: the middle of the arena's floor; where the block keeping its plan stands. */
    private static final BlockPos FOOT = new BlockPos(6, 2, 6);
    private static final BlockPos OWNER = new BlockPos(0, 2, 0);
    private static final int CLIMBED = 3;

    private RocketLaunchGameTests() {
    }

    @GameTest(template = TALL, batch = BATCH, timeoutTicks = 400)
    public static void rocket_isReleasedAsOneBodyAndClimbs(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketStack stack = RocketStack.of(new RocketStack.Part(SkeletonRocketParts.id("sea_level_engine_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("tank_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("capsule_3"), 1));
        final BlockPos foot = helper.absolutePos(FOOT);
        // Built by its plan as its hangar builds it, every part formed, then let go with its plan as the hangar does.
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), foot);
        build.start(level, helper.absolutePos(OWNER), () -> { });
        for (final PlanSpot spot : build.plan().spots()) {
            level.setBlockAndUpdate(build.where(spot), BuiltInRegistries.BLOCK.get(spot.block()).defaultBlockState());
        }
        helper.assertTrue(build.complete(), "the rocket is built whole, every part formed");
        build.stop(false);
        final BlockPos computer = where(build, RocketKitBlocks.FLIGHT_COMPUTER.getId());
        final BlockPos nozzle = build.where(build.plan().spots().getFirst());
        helper.assertTrue(level.getBlockEntity(computer) instanceof FlightComputerBlockEntity,
                "the rocket's capsule holds its flight computer");
        final FlightComputerBlockEntity rocket = (FlightComputerBlockEntity) level.getBlockEntity(computer);
        rocket.carry(new RocketDesign("ASCENT", stack), foot.subtract(computer), List.of(0, 1, 2));
        Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> rocket.load(1,
                TankLoad.filled(SkeletonPropellants.METHALOX, methalox, 5_800.0)));
        final Optional<MovingStructure> released = RocketLaunch.release(level, computer, null);
        helper.assertTrue(released.isPresent(), "released, the rocket becomes one moving structure");
        final MovingStructure body = released.get();
        final double start = body.pose().y();
        final BlockPos[] flying = new BlockPos[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    // Where its blocks are aboard, read before it moves.
                    flying[0] = aboard(body, computer);
                    helper.assertTrue(level.getBlockState(aboard(body, nozzle)).getBlock() instanceof FormedPartBlock,
                            "its parts go as they were, formed, nothing checked again");
                    helper.assertTrue(level.getBlockEntity(flying[0]) instanceof FlightComputerBlockEntity kept
                            && kept.rocket().isPresent() && kept.formed().equals(List.of(0, 1, 2))
                            && level.getBlockState(flying[0]).getValue(FlightComputerBlock.FORMED),
                            "its flight computer goes with it, keeping its plan to draw its parts");
                    RocketLaunch.lift(level, body);
                    helper.assertTrue(Rockets.get(level.getServer(), body.id()).map(found -> found.tanks()
                            .containsKey(1)).orElse(false), "it is told as a rocket, its tank loaded");
                    helper.assertTrue(body.motion() instanceof RocketAscent, "and climbs by its engines");
                })
                .thenWaitUntil(() -> helper.assertTrue(body.pose().y() > start + CLIMBED,
                        "it climbs: " + body.pose().y() + " from " + start))
                .thenExecute(() -> {
                    final double speed = ((RocketAscent) body.motion()).climb();
                    helper.assertTrue(speed > 0, "faster and faster: " + speed);
                })
                .thenWaitUntil(() -> {
                    final CabinReadings read = level.getBlockEntity(flying[0])
                            instanceof FlightComputerBlockEntity kept ? kept.readings().orElse(null) : null;
                    helper.assertTrue(read != null && read.lit(CabinLamp.ENGINE) && read.lit(CabinLamp.GUIDANCE)
                            && read.climb() > 0 && read.throttle() == 1 && read.fuel() < 1 && read.clock() >= 0,
                            "its cabins read the flight: " + read);
                })
                .thenExecute(() -> MovingStructures.takeAway(body))
                .thenSucceed();
    }

    /* Where the block that stood at the world's {@code pos} is aboard {@code body} now. */
    private static BlockPos aboard(final MovingStructure body, final BlockPos pos) {
        return BlockPos.containing(MovingStructures.toYard(body, Vec3.atCenterOf(pos)));
    }

    /* Where the plan wants the block {@code block}, the first place of it. */
    private static BlockPos where(final PlannedBuild build, final ResourceLocation block) {
        return build.where(build.plan().spots().stream().filter(spot -> spot.block().equals(block)).findFirst()
                .orElseThrow());
    }
}
