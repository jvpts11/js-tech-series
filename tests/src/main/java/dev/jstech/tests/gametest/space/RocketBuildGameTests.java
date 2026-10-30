/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.space.JsSpace;
import dev.jstech.space.flight.FlightComputerBlock;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketPartModels;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A rocket built by its hangar's plan: every built model of a part has its shape, the capsule's laying its crew on
 * couches; the plan sent to the bay counts each part's blocks, a part forming as its last block goes down and holding,
 * holed, as one goes, mended by the block put back, its merged blocks taking their model's shape; the Flight Computer,
 * which keeps its state, stays a block of its own and stops drawing itself once its capsule forms; and a separation
 * ring's skirt shapes the blocks of the engine standing on it while the ring is formed.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RocketBuildGameTests {

    private static final String ARENA = "empty";
    private static final String HANGAR = "tall";
    private static final String BATCH = "jsspace_rocket_build";
    /* The hangar's bay, from (2, 2, 2) to (8, 13, 8), its hoist at 12; the rocket stands on (5, 2, 5). */
    private static final int LOW = 2;
    private static final int HIGH_XZ = 8;
    private static final int TOP = 13;
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 9);
    private static final BlockPos DOOR = new BlockPos(1, 2, 5);
    private static final BlockPos HOIST = new BlockPos(5, 12, 5);
    /* The models of legs spread to land, which are not built. */
    private static final Set<String> SPREAD = Set.of("landing_legs_3", "landing_legs_5", "lunar_lander_descent");

    private RocketBuildGameTests() {
    }

    @GameTest(template = ARENA)
    public static void shapes_everyBuiltModelHasOneAndTheCapsuleLaysItsCrew(final GameTestHelper helper) {
        for (final String name : RocketPartModels.names()) {
            if (SPREAD.contains(name)) {
                continue;
            }
            helper.assertTrue(FormedShapes.get(helper.getLevel(), model(name)).isPresent(),
                    "the built model " + name + " has its shape file");
        }
        final FormedShape capsule = FormedShapes.get(helper.getLevel(), model("capsule_3")).orElseThrow();
        helper.assertTrue(capsule.aboard().get(RocketKitBlocks.CREW_SEAT.getId()).seat().orElseThrow().lying(),
                "in a capsule the crew seat is a couch");
        helper.succeed();
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 200)
    public static void hangar_buildsItsRocketPartByPart(final GameTestHelper helper) {
        build(helper);
        final HangarTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        terminal.look();
        terminal.design(new RocketDesign("BUILT", RocketStack.of(part("sea_level_engine_3", 1),
                part("tank_3", 2), part("capsule_3", 1))));
        helper.assertTrue(terminal.confirm(), "a ready design is sent to the bay");
        final PlannedBuild plan = terminal.build().orElseThrow();
        helper.assertTrue(plan.plan().parts() == 3 && plan.missing(0) > 0 && terminal.formedParts().isEmpty(),
                "its plan counts its three parts, none formed");
        lay(helper.getLevel(), plan, 0);
        helper.assertTrue(plan.formed(0) && terminal.formedParts().equals(List.of(0))
                && terminal.missingParts().get(0) == 0, "the engine's last block down, it forms, and its players"
                + " are told: " + terminal.formedParts());
        final BlockPos nozzle = plan.where(plan.plan().spots().getFirst());
        helper.assertTrue(helper.getLevel().getBlockState(nozzle).getBlock() instanceof FormedPartBlock
                && helper.getLevel().getBlockEntity(nozzle) instanceof FormedPartBlockEntity merged
                && merged.place() != null && !merged.shape().equals(Shapes.block()),
                "its blocks merged into its model, each shaped as its part of it");
        lay(helper.getLevel(), plan, 1);
        lay(helper.getLevel(), plan, 2);
        helper.assertTrue(plan.complete() && terminal.formedParts().equals(List.of(0, 1, 2)),
                "the whole rocket formed");
        final BlockPos computer = where(plan, RocketKitBlocks.FLIGHT_COMPUTER.getId());
        final BlockState screen = helper.getLevel().getBlockState(computer);
        helper.assertTrue(screen.getBlock() instanceof FlightComputerBlock
                && screen.getValue(FlightComputerBlock.FORMED), "the Flight Computer stays a block of its own, drawn"
                + " by its capsule now");
        final BlockPos seat = where(plan, RocketKitBlocks.CREW_SEAT.getId());
        helper.assertTrue(BlockAboard.at(helper.getLevel(), seat).seat().orElseThrow().lying(),
                "its crew seat is a couch in the formed capsule");
        final BlockPos tank = plan.where(plan.plan().spots().stream().filter(spot -> spot.part() == 1
                && spot.place().isPresent()).findFirst().orElseThrow());
        final BlockState tankBlock = FormedPartBlock.originalAt(helper.getLevel(), tank,
                helper.getLevel().getBlockState(tank));
        helper.getLevel().setBlockAndUpdate(tank, Blocks.AIR.defaultBlockState());
        helper.assertTrue(plan.formed(1) && plan.missing(1) == 1 && terminal.formedParts().equals(List.of(0, 1, 2)),
                "a tank block taken, the tank holds, formed and holed there, the block missing");
        helper.getLevel().setBlockAndUpdate(tank, tankBlock);
        helper.assertTrue(plan.missing(1) == 0 && helper.getLevel().getBlockState(tank).getBlock()
                instanceof FormedPartBlock, "the tank block put back merges again and mends it");
        terminal.undo();
        helper.assertTrue(!(helper.getLevel().getBlockState(nozzle).getBlock() instanceof FormedPartBlock)
                && !helper.getLevel().getBlockState(computer).getValue(FlightComputerBlock.FORMED),
                "the hangar undone, its rocket's parts come apart");
        helper.succeed();
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 200)
    public static void plan_shapesTheEngineWithTheSkirtOfTheRingUnderIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PlannedBuild plan = new PlannedBuild(RocketPlans.of(level, RocketStack.of(part("separation_ring_3", 1),
                part("vacuum_engine_3", 1))), helper.absolutePos(new BlockPos(5, 2, 5)));
        plan.start(level, helper.absolutePos(new BlockPos(0, 2, 0)), () -> { });
        lay(level, plan, 0);
        lay(level, plan, 1);
        final PlanSpot reached = plan.plan().spots().stream().filter(spot -> spot.part() == 1
                && spot.reaches().stream().anyMatch(reach -> reach.part() == 0)).findFirst().orElseThrow();
        final FormedPartBlockEntity engine = (FormedPartBlockEntity) level.getBlockEntity(plan.where(reached));
        helper.assertTrue(engine.also().size() >= 1 && engine.places().size() == 1 + engine.also().size(),
                "an engine block takes the shape of the skirt rising round it: " + engine.also());
        level.setBlockAndUpdate(plan.where(plan.plan().spots().getFirst()), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!plan.formed(0) && engine.also().isEmpty(), "the ring come apart, the skirt is gone from it");
        plan.stop(true);
        helper.succeed();
    }

    /* Lays every block of the part {@code part} of {@code plan}, each the block wanted there. */
    private static void lay(final ServerLevel level, final PlannedBuild plan, final int part) {
        for (final PlanSpot spot : plan.plan().spots()) {
            if (spot.part() == part) {
                level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                        .defaultBlockState());
            }
        }
    }

    /* Where the plan wants the block {@code block}, the first place of it. */
    private static BlockPos where(final PlannedBuild plan, final ResourceLocation block) {
        return plan.where(plan.plan().spots().stream().filter(spot -> spot.block().equals(block)).findFirst()
                .orElseThrow());
    }

    private static ResourceLocation model(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "rocket/" + name);
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /* The hangar of stone, its terminal in the south wall looking north, its crane, its door. */
    private static void build(final GameTestHelper helper) {
        for (int x = LOW - 1; x <= HIGH_XZ + 1; x++) {
            for (int z = LOW - 1; z <= HIGH_XZ + 1; z++) {
                for (int y = LOW - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < LOW || x > HIGH_XZ || z < LOW || z > HIGH_XZ || y < LOW || y > TOP;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        helper.setBlock(TERMINAL, HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        for (int x = LOW; x <= HIGH_XZ; x++) {
            helper.setBlock(new BlockPos(x, TOP - 1, LOW), HangarBlocks.CRANE_RAIL.get());
            helper.setBlock(new BlockPos(x, TOP - 1, HIGH_XZ), HangarBlocks.CRANE_RAIL.get());
        }
        for (int z = LOW; z <= HIGH_XZ; z++) {
            helper.setBlock(new BlockPos(5, TOP, z), HangarBlocks.CRANE_BRIDGE.get());
        }
        helper.setBlock(HOIST, HangarBlocks.CRANE_HOIST.get());
        helper.setBlock(DOOR, HangarBlocks.BAY_DOOR.get());
    }
}
