/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.serialization.JsonOps;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanReach;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModels;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Building by a plan: a part forms by itself once its last block is down, its blocks merging and never looking for an
 * owner, and comes apart, every block back, as soon as one goes; a wrong block does not count; each block put down
 * changes only its part's count; a merged block takes the shapes of the formed models reaching into it, only while
 * their parts are formed; a plan started again over what is already built finds it formed; and a plan handed on
 * points its blocks at the new owner and goes on counting. The plan is written down and read back the same.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BuildPlanGameTests {

    private static final String ARENA = "empty";
    /* The plan's origin and its owner, in the test's place. */
    private static final BlockPos ORIGIN = new BlockPos(1, 2, 1);
    private static final BlockPos OWNER = new BlockPos(0, 2, 0);
    private static final BlockPos OTHER_OWNER = new BlockPos(0, 3, 0);
    private static final ResourceLocation IRON = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_BLOCK);
    private static final ResourceLocation GOLD = BuiltInRegistries.BLOCK.getKey(Blocks.GOLD_BLOCK);

    private BuildPlanGameTests() {
    }

    @GameTest(template = ARENA)
    public static void plan_formsAPartOnItsLastBlockAndUndoesItWhenOneGoes(final GameTestHelper helper) {
        final PlannedBuild build = new PlannedBuild(twoParts(), helper.absolutePos(ORIGIN));
        final int[] told = new int[1];
        build.start(helper.getLevel(), helper.absolutePos(OWNER), () -> told[0]++);
        helper.assertTrue(build.missing(0) == 3 && build.missing(1) == 2 && !build.formed(0),
                "nothing is down yet: three of iron and two of gold missing");
        place(helper, 0, 0, Blocks.IRON_BLOCK);
        place(helper, 1, 0, Blocks.STONE);
        helper.assertTrue(build.missing(0) == 2, "iron counts, stone in iron's place does not: " + build.missing(0));
        place(helper, 1, 0, Blocks.IRON_BLOCK);
        helper.assertTrue(build.missing(0) == 1 && build.missing(1) == 2 && told[0] >= 2,
                "each block changes only its part's count, and the owner is told");
        place(helper, 2, 0, Blocks.IRON_BLOCK);
        helper.assertTrue(build.formed(0) && !build.formed(1), "its last block down, the iron part forms");
        for (int x = 0; x < 3; x++) {
            final BlockPos at = new BlockPos(ORIGIN.getX() + x, ORIGIN.getY(), ORIGIN.getZ());
            helper.assertTrue(helper.getBlockState(at).getBlock() instanceof FormedPartBlock
                    && helper.getBlockEntity(at) instanceof FormedPartBlockEntity part && part.planned()
                    && part.original().is(Blocks.IRON_BLOCK)
                    && helper.absolutePos(OWNER).equals(part.terminal()), "each iron block merged, laid by the plan,"
                    + " its owner told, remembering it was iron: " + at);
        }
        helper.setBlock(new BlockPos(ORIGIN.getX() + 1, ORIGIN.getY(), ORIGIN.getZ()), Blocks.AIR);
        helper.assertTrue(!build.formed(0) && build.missing(0) == 1
                && helper.getBlockState(ORIGIN).is(Blocks.IRON_BLOCK),
                "a block taken away, the part comes apart and its other blocks are iron again");
        build.stop(true);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void plan_shapesABlockWithTheModelsReachingIntoItWhileTheyAreFormed(final GameTestHelper helper) {
        final ResourceLocation model = TestModels.TURBINE.id();
        final int north = FormedShapes.ORIENTATIONS.indexOf(Orientation.NORTH);
        final FormedPlace own = new FormedPlace(model, north, 0, 0, -2);
        final FormedPlace reaching = new FormedPlace(model, north, 0, 1, -2);
        final BuildPlan plan = new BuildPlan(2, List.of(
                new PlanSpot(BlockPos.ZERO, IRON, 0, Optional.of(own), List.of(new PlanReach(1, reaching))),
                new PlanSpot(new BlockPos(0, 1, 0), GOLD, 1, Optional.of(new FormedPlace(model, north, 0, 1, 0)),
                        List.of())));
        final PlannedBuild build = new PlannedBuild(plan, helper.absolutePos(ORIGIN));
        build.start(helper.getLevel(), helper.absolutePos(OWNER), () -> { });
        place(helper, 0, 0, Blocks.IRON_BLOCK);
        final FormedPartBlockEntity iron = helper.getBlockEntity(ORIGIN);
        final FormedShape file = FormedShapes.get(helper.getLevel(), model).orElseThrow();
        helper.assertTrue(iron.also().isEmpty() && same(iron.shape(), file.shape(own)),
                "alone, it takes its own block's shape: " + iron.also());
        helper.setBlock(ORIGIN.above(), Blocks.GOLD_BLOCK);
        helper.assertTrue(build.formed(1) && iron.also().equals(List.of(reaching)) && same(iron.shape(),
                Shapes.joinUnoptimized(file.shape(own), file.shape(reaching), BooleanOp.OR).optimize()),
                "the gold part formed, the iron block takes the shape reaching into it too: " + iron.also());
        helper.setBlock(ORIGIN.above(), Blocks.AIR);
        helper.assertTrue(!build.formed(1) && iron.also().isEmpty(), "the gold part gone, the iron loses its shape");
        build.stop(true);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void plan_startedAgainFindsItBuiltAndHandedOnGoesOnCounting(final GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            place(helper, x, 0, Blocks.IRON_BLOCK);
        }
        final PlannedBuild first = new PlannedBuild(twoParts(), helper.absolutePos(ORIGIN));
        first.start(helper.getLevel(), helper.absolutePos(OWNER), () -> { });
        helper.assertTrue(first.formed(0), "what is already down when it starts forms at once");
        first.stop(false);
        final PlannedBuild again = new PlannedBuild(twoParts(), helper.absolutePos(ORIGIN));
        again.start(helper.getLevel(), helper.absolutePos(OWNER), () -> { });
        helper.assertTrue(again.formed(0) && again.missing(0) == 0 && helper.getBlockState(ORIGIN).getBlock()
                instanceof FormedPartBlock, "started again over its merged blocks, it finds the part formed");
        again.handTo(helper.absolutePos(OTHER_OWNER));
        final FormedPartBlockEntity merged = helper.getBlockEntity(ORIGIN);
        helper.assertTrue(helper.absolutePos(OTHER_OWNER).equals(merged.terminal())
                && again.owner().orElseThrow().equals(helper.absolutePos(OTHER_OWNER)),
                "handed on, its blocks point at the new owner");
        helper.setBlock(ORIGIN, Blocks.AIR);
        helper.assertTrue(!again.formed(0) && again.missing(0) == 1, "and it goes on counting for it");
        again.stop(true);
        final BuildPlan plan = twoParts();
        helper.assertTrue(BuildPlan.CODEC.parse(JsonOps.INSTANCE, BuildPlan.CODEC.encodeStart(JsonOps.INSTANCE, plan)
                .getOrThrow()).getOrThrow().equals(plan), "a plan is written down and read back the same");
        helper.succeed();
    }

    /* A plan of three iron blocks in a row, the first part, and two gold blocks over them, the second. */
    private static BuildPlan twoParts() {
        return new BuildPlan(2, List.of(spot(0, 0, IRON, 0), spot(1, 0, IRON, 0), spot(2, 0, IRON, 0),
                spot(0, 1, GOLD, 1), spot(1, 1, GOLD, 1)));
    }

    private static PlanSpot spot(final int x, final int y, final ResourceLocation block, final int part) {
        return new PlanSpot(new BlockPos(x, y, 0), block, part, Optional.empty(), List.of());
    }

    private static void place(final GameTestHelper helper, final int x, final int y, final Block block) {
        helper.setBlock(new BlockPos(ORIGIN.getX() + x, ORIGIN.getY() + y, ORIGIN.getZ()), block);
    }

    private static boolean same(final VoxelShape a, final VoxelShape b) {
        return !Shapes.joinIsNotEmpty(a, b, BooleanOp.NOT_SAME);
    }
}
