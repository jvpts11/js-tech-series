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
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.launch.RocketLaunch;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A rocket damaged where it stands, as in flight: a block of a part lost leaves the part formed and holed, and its
 * flight computer counts it, the part's function failing till the block is put back. A holed tank leaks what it
 * holds, and mended it leaks no more; a holed engine gives no thrust, so the delta-v it gave is gone; a holed capsule's
 * heat shield is breached.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketDamageGameTests {

    private static final String ARENA = "bench";
    private static final String DAMAGE = "jsspace_rocket_damage";
    /* Where the rocket's foot stands, and the block keeping its plan while it is built. */
    private static final BlockPos FOOT = new BlockPos(8, 1, 8);
    private static final BlockPos OWNER = new BlockPos(1, 1, 1);
    /* What its tank holds, in kilograms, and how long the holed tank is watched leaking, in ticks. */
    private static final double LOAD = 2_000;
    private static final int LEAKING = 40;
    /* Less than this share of its load is left after the watch: two seconds at two hundredths a second, and more. */
    private static final double LEFT_AFTER_LEAKING = 0.985;
    /* The places in its stack of its engine, its tank and its capsule. */
    private static final int ENGINE = 0;
    private static final int TANK = 1;
    private static final int CAPSULE = 2;

    private RocketDamageGameTests() {
    }

    @GameTest(template = ARENA, batch = DAMAGE, timeoutTicks = 600)
    public static void tank_holedLeaksTillItsBlockIsPutBack(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper);
        final MovingStructure body = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final FlightComputerBlockEntity[] aboard = new FlightComputerBlockEntity[1];
        final BlockPos[] holed = new BlockPos[1];
        final BlockState[] was = new BlockState[1];
        final double[] before = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    aboard[0] = aboard(level, body, computer);
                    holed[0] = blockOf(level, aboard[0], TANK);
                    was[0] = FormedPartBlock.originalAt(level, holed[0], level.getBlockState(holed[0]));
                    level.setBlockAndUpdate(holed[0], Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(aboard[0].lost(TANK) == 1 && Rockets.get(level.getServer(),
                        body.id()).map(rocket -> rocket.lost(TANK) == 1).orElse(false),
                        "its computer counts the block its tank lost, and the rocket's record holds it"))
                .thenExecute(() -> before[0] = held(level, body))
                .thenIdle(LEAKING)
                .thenExecute(() -> {
                    final double after = held(level, body);
                    helper.assertTrue(after < before[0] * LEFT_AFTER_LEAKING, "the holed tank leaks: " + before[0]
                            + " kg to " + after + " kg");
                    level.setBlockAndUpdate(holed[0], was[0]);
                    helper.assertTrue(FormedDamage.mend(level, holed[0]), "the block put back merges and mends it");
                })
                .thenWaitUntil(() -> helper.assertTrue(aboard[0].lost(TANK) == 0 && Rockets.get(level.getServer(),
                        body.id()).map(rocket -> rocket.lost().isEmpty()).orElse(false), "mended, it counts nothing"))
                .thenExecute(() -> MovingStructures.takeAway(body))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = DAMAGE, timeoutTicks = 600)
    public static void engineAndCapsule_holedGiveNoThrustAndNoShield(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final FlightComputerBlockEntity computer = build(helper);
        final MovingStructure body = RocketLaunch.release(level, computer.getBlockPos(), null).orElseThrow();
        final FlightComputerBlockEntity[] aboard = new FlightComputerBlockEntity[1];
        final double[] whole = new double[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "its body stands ready"))
                .thenExecute(() -> {
                    aboard[0] = aboard(level, body, computer);
                    final OptionalDouble dv = aboard[0].available();
                    helper.assertTrue(dv.isPresent() && dv.getAsDouble() > 0 && aboard[0].shield(),
                            "whole, its engine gives delta-v and its capsule shields it");
                    whole[0] = dv.getAsDouble();
                    level.setBlockAndUpdate(blockOf(level, aboard[0], ENGINE), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(blockOf(level, aboard[0], CAPSULE), Blocks.AIR.defaultBlockState());
                })
                .thenWaitUntil(() -> helper.assertTrue(aboard[0].lost(ENGINE) == 1 && aboard[0].lost(CAPSULE) == 1,
                        "its computer counts what its engine and its capsule lost"))
                .thenExecute(() -> {
                    helper.assertTrue(aboard[0].available().orElse(whole[0]) < whole[0] - 1, "the holed engine gives "
                            + "no thrust, its delta-v gone: " + aboard[0].available() + " of " + whole[0]);
                    helper.assertTrue(!aboard[0].shield(), "the holed capsule's heat shield is breached");
                    MovingStructures.takeAway(body);
                })
                .thenSucceed();
    }

    /* Its flight computer, carried aboard its body. */
    private static FlightComputerBlockEntity aboard(final ServerLevel level, final MovingStructure body,
                                                    final FlightComputerBlockEntity computer) {
        return (FlightComputerBlockEntity) level.getBlockEntity(BlockPos.containing(MovingStructures.toYard(body,
                Vec3.atCenterOf(computer.getBlockPos()))));
    }

    /* A merged block of the part at {@code section} aboard, on its side, away from the rocket's middle. */
    private static BlockPos blockOf(final ServerLevel level, final FlightComputerBlockEntity computer,
                                    final int section) {
        final BlockPos base = computer.getBlockPos().offset(computer.foot().orElseThrow());
        for (final PlanSpot spot : RocketPlans.of(level, computer.rocket().orElseThrow().stack()).spots()) {
            final BlockPos at = base.offset(spot.at());
            if (spot.part() == section && spot.place().isPresent() && spot.at().getX() != 0
                    && level.getBlockState(at).getBlock() instanceof FormedPartBlock) {
                return at;
            }
        }
        throw new IllegalStateException("no merged block on the side of the part at " + section);
    }

    /* What its tank holds now, its losses reckoned. */
    private static double held(final ServerLevel level, final MovingStructure body) {
        return Rockets.settle(level.getServer(), body.id()).map(rocket -> rocket.tanks().get(TANK))
                .map(TankLoad::total).orElse(0.0);
    }

    /*
     * Builds an engine, a tank and a capsule by their plan at the arena's foot, every part formed, lets its plan go
     * with its flight computer as its hangar does and fills its tank; gives its flight computer.
     */
    private static FlightComputerBlockEntity build(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3"), part("tank_3"), part("capsule_3"));
        final BlockPos foot = helper.absolutePos(FOOT);
        final PlannedBuild build = new PlannedBuild(RocketPlans.of(level, stack), foot);
        build.start(level, helper.absolutePos(OWNER), () -> { });
        BlockPos at = foot;
        for (final PlanSpot spot : build.plan().spots()) {
            final Block block = BuiltInRegistries.BLOCK.get(spot.block());
            level.setBlockAndUpdate(build.where(spot), block.defaultBlockState());
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
        computer.carry(new RocketDesign("HOLED", stack), foot.subtract(at), formed);
        Propellants.get(level, SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(TANK,
                TankLoad.filled(SkeletonPropellants.METHALOX, methalox, LOAD)));
        return computer;
    }

    private static RocketStack.Part part(final String path) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), 1);
    }
}
