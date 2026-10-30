/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanAssembler;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A machine laying a planned build by itself: its inbox takes only the blocks the plan still wants and no more of
 * each, it waits with nothing to lay and lays the lowest missing block first, layer by layer, at a builder's pace, so
 * the plan's parts form as their last blocks go down; what the plan no longer wants comes back out of the inbox, and
 * what it holds drops when its machine is broken.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PlanAssemblerGameTests {

    private static final String ARENA = "empty";
    /* The plan's origin and the machine laying it, in the test's place. */
    private static final BlockPos ORIGIN = new BlockPos(2, 2, 2);
    private static final BlockPos MACHINE = new BlockPos(0, 2, 0);
    private static final ResourceLocation IRON = BuiltInRegistries.BLOCK.getKey(Blocks.IRON_BLOCK);
    private static final ResourceLocation GOLD = BuiltInRegistries.BLOCK.getKey(Blocks.GOLD_BLOCK);
    private static final int ALL = 64;
    /* Long enough for the machine to look and find nothing to lay. */
    private static final int LOOK = 10;

    private PlanAssemblerGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void assembler_laysTheLowestMissingBlockItHoldsLayerByLayer(final GameTestHelper helper) {
        final PlannedBuild build = new PlannedBuild(twoLayers(), helper.absolutePos(ORIGIN));
        build.start(helper.getLevel(), helper.absolutePos(MACHINE), () -> { });
        final PlanAssembler assembler = new PlanAssembler(helper.absolutePos(MACHINE), () -> { });
        assembler.build(build);
        helper.onEachTick(() -> assembler.tick(helper.getLevel()));
        final IItemHandler inbox = assembler.inbox();
        helper.assertTrue(assembler.needs().equals(Map.of(Items.IRON_BLOCK, 3, Items.GOLD_BLOCK, 2))
                        && assembler.needs().keySet().iterator().next() == Items.IRON_BLOCK,
                "it waits for the plan's blocks, the iron of the lower layer first: " + assembler.needs());
        helper.startSequence()
                .thenExecuteAfter(LOOK, () -> {
                    helper.assertTrue(assembler.waiting(), "holding nothing, it waits");
                    helper.assertTrue(insert(inbox, Items.STONE, ALL) == 0, "stone, which the plan does not want,"
                            + " does not go in");
                    // Gold first: the gold is held before the iron comes, and still the iron is laid first.
                    helper.assertTrue(insert(inbox, Items.GOLD_BLOCK, ALL) == 2, "two of gold, as many as it wants");
                    helper.assertTrue(insert(inbox, Items.GOLD_BLOCK, 1) == 0, "and no more of it");
                })
                .thenWaitUntil(() -> helper.assertTrue(at(helper, 0, 1).is(Blocks.GOLD_BLOCK),
                        "with no iron, it lays what it holds: the gold"))
                .thenExecute(() -> {
                    helper.assertTrue(insert(inbox, Items.IRON_BLOCK, ALL) == 3, "three of iron go in");
                    helper.assertTrue(assembler.needs().isEmpty(), "it waits for nothing more");
                })
                .thenWaitUntil(() -> helper.assertTrue(build.complete(), "it lays the rest of the plan"))
                .thenExecute(() -> {
                    helper.assertTrue(build.formed(0) && build.formed(1), "each part formed as its last block went"
                            + " down");
                    helper.assertTrue(assembler.held().isEmpty() && !assembler.waiting(),
                            "it holds nothing, and waits for nothing: " + assembler.held());
                    build.stop(true);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void assembler_givesBackWhatThePlanNoLongerWantsAndDropsWhatItHolds(final GameTestHelper helper) {
        final PlannedBuild build = new PlannedBuild(twoLayers(), helper.absolutePos(ORIGIN));
        build.start(helper.getLevel(), helper.absolutePos(MACHINE), () -> { });
        final PlanAssembler assembler = new PlanAssembler(helper.absolutePos(MACHINE), () -> { });
        assembler.build(build);
        final IItemHandler inbox = assembler.inbox();
        helper.assertTrue(insert(inbox, Items.IRON_BLOCK, ALL) == 3, "three of iron go in");
        helper.assertTrue(inbox.extractItem(0, ALL, false).isEmpty(), "none comes back out while the plan wants it");
        // A player lays one of the iron blocks by hand: the plan wants one fewer.
        helper.setBlock(ORIGIN, Blocks.IRON_BLOCK);
        final ItemStack back = inbox.extractItem(0, ALL, false);
        helper.assertTrue(back.is(Items.IRON_BLOCK) && back.getCount() == 1 && assembler.held(Items.IRON_BLOCK) == 2,
                "the one the plan no longer wants comes back out: " + back);
        assembler.build(null);
        helper.assertTrue(inbox.extractItem(0, 1, true).getCount() == 1, "without a plan, all it holds may come out");
        assembler.spill(helper.getLevel(), helper.absolutePos(MACHINE));
        final List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(MACHINE)).inflate(2.0));
        helper.assertTrue(dropped.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 2
                        && assembler.held().isEmpty(), "broken, it drops what it held: " + dropped);
        dropped.forEach(ItemEntity::discard);
        build.stop(true);
        helper.succeed();
    }

    /* Three of iron on the ground, part 0, and two of gold on them, part 1. */
    private static BuildPlan twoLayers() {
        return new BuildPlan(2, List.of(spot(0, 0, IRON, 0), spot(1, 0, IRON, 0), spot(2, 0, IRON, 0),
                spot(0, 1, GOLD, 1), spot(1, 1, GOLD, 1)));
    }

    private static PlanSpot spot(final int x, final int y, final ResourceLocation block, final int part) {
        return new PlanSpot(new BlockPos(x, y, 0), block, part, Optional.empty(), List.of());
    }

    /* How many of {@code count} of {@code item} the inbox takes. */
    private static int insert(final IItemHandler inbox, final Item item, final int count) {
        return count - inbox.insertItem(inbox.getSlots() - 1, new ItemStack(item, count), false).getCount();
    }

    private static BlockState at(final GameTestHelper helper, final int x, final int y) {
        return helper.getBlockState(ORIGIN.offset(x, y, 0));
    }
}
