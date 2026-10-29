/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.wreck.RocketWreckage;
import dev.jstech.space.wreck.RocketWreckageBlockEntity;
import dev.jstech.space.wreck.StageImpacts;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A stage striking the ground: with the damage of falling stages on, its blast leaves a crater and its wreckage lies in
 * and round it once the blast has gone out; with it off, the ground stays whole and only the wreckage lies there. The
 * wreckage holds a third of the stage's blocks and gives them back as it is broken, and nothing else.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StageImpactGameTests {

    private static final String ARENA = "bench";
    private static final String BATCH = "jsspace_stage_impacts";
    private static final BlockPos MIDDLE = new BlockPos(24, 1, 24);
    /* The ground laid for the fall: a slab of dirt, this far round the middle and this deep. */
    private static final int GROUND_REACH = 6;
    private static final int GROUND_DEPTH = 3;
    /* The stage: nine blocks of iron and three of copper, two tonnes empty with a little propellant left. */
    private static final List<ItemStack> MADE = List.of(new ItemStack(Items.IRON_BLOCK, 9),
            new ItemStack(Items.COPPER_BLOCK, 3));
    private static final double DRY = 2_000;
    private static final double LEFTOVER = 10;
    private static final double SPEED = 60;
    /* How far round the middle wreckage is looked for. */
    private static final int LOOK = 16;

    private StageImpactGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void fall_leavesACraterAndItsWreckageOnceTheBlastHasGoneOut(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        ground(helper);
        final int before = dirt(helper);
        final StageImpacts.Impact impact = StageImpacts.strike(level, Vec3.atBottomCenterOf(helper.absolutePos(
                MIDDLE.above(GROUND_DEPTH))), DRY, LEFTOVER, SPEED, MADE, 7L);
        helper.assertTrue(impact.crater(), "with the damage on, it goes off as a blast");
        helper.assertTrue(impact.joules() > 0.5 * (DRY + LEFTOVER) * SPEED * SPEED, "its speed and its propellant");
        helper.succeedWhen(() -> {
            helper.assertTrue(!StageImpacts.pending(level), "the blast has gone out");
            helper.assertTrue(dirt(helper) < before, "it left a crater: " + (before - dirt(helper)) + " broken");
            assertSalvage(helper, salvage(helper));
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void fall_withTheDamageOffLeavesOnlyItsWreckage(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        ground(helper);
        final int before = dirt(helper);
        SpaceServerConfig.FILE.set(SpaceServerConfig.FALLING_STAGES_DO_DAMAGE, false);
        try {
            final StageImpacts.Impact impact = StageImpacts.strike(level, Vec3.atBottomCenterOf(helper.absolutePos(
                    MIDDLE.above(GROUND_DEPTH))), DRY, LEFTOVER, SPEED, MADE, 8L);
            helper.assertTrue(!impact.crater(), "no blast");
            helper.assertTrue(dirt(helper) == before, "the ground is whole");
            assertSalvage(helper, salvage(helper));
            helper.assertTrue(impact.pieces() == 2, "a piece for each tonne: " + impact.pieces());
        } finally {
            SpaceServerConfig.FILE.set(SpaceServerConfig.FALLING_STAGES_DO_DAMAGE, true);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void wreckage_brokenGivesBackWhatItHoldsAndNothingElse(final GameTestHelper helper) {
        helper.setBlock(MIDDLE, RocketWreckage.BLOCK.get());
        final RocketWreckageBlockEntity wreck = helper.getBlockEntity(MIDDLE);
        helper.assertTrue(wreck.hold(List.of(new ItemStack(Items.IRON_BLOCK, 2))).isEmpty(), "it takes the blocks");
        helper.getLevel().destroyBlock(helper.absolutePos(MIDDLE), true);
        helper.succeedWhen(() -> {
            final List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(MIDDLE)).inflate(2));
            helper.assertTrue(dropped.stream().mapToInt(item -> item.getItem().is(Items.IRON_BLOCK)
                    ? item.getItem().getCount() : 0).sum() == 2, "its two blocks of iron");
            helper.assertTrue(dropped.stream().noneMatch(item -> item.getItem().is(RocketWreckage.BLOCK.get()
                    .asItem())), "and not itself");
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 100)
    public static void blocksOf_countsAStructuresBlocks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(MIDDLE, Blocks.IRON_BLOCK);
        helper.setBlock(MIDDLE.above(), Blocks.IRON_BLOCK);
        helper.setBlock(MIDDLE.above(2), Blocks.COPPER_BLOCK);
        warm(level);
        final StructureResult made = MovingStructures.assembleFor(level, helper.absolutePos(MIDDLE),
                helper.absolutePos(MIDDLE.above(2)), UUID.randomUUID());
        try {
            helper.assertTrue(made.ok(), "a structure of three blocks");
            final List<ItemStack> counted = StageImpacts.blocksOf(made.structure());
            helper.assertTrue(count(counted, Items.IRON_BLOCK) == 2 && count(counted, Items.COPPER_BLOCK) == 1,
                    "two of iron and one of copper: " + counted);
        } finally {
            if (made.ok()) {
                MovingStructures.takeAway(made.structure());
            }
        }
        helper.succeed();
    }

    /* A third of the stage's blocks, laid in its wreckage: three of iron and one of copper. */
    private static void assertSalvage(final GameTestHelper helper, final List<ItemStack> held) {
        helper.assertTrue(count(held, Items.IRON_BLOCK) == 3 && count(held, Items.COPPER_BLOCK) == 1,
                "its wreckage holds a third of its blocks: " + held);
    }

    /* What every piece of wreckage round the middle holds. */
    private static List<ItemStack> salvage(final GameTestHelper helper) {
        final List<ItemStack> held = new ArrayList<>();
        final BlockPos middle = helper.absolutePos(MIDDLE);
        for (final BlockPos pos : BlockPos.betweenClosed(middle.offset(-LOOK, -1, -LOOK), middle.offset(LOOK, 6,
                LOOK))) {
            if (helper.getLevel().getBlockEntity(pos) instanceof RocketWreckageBlockEntity wreck) {
                held.addAll(wreck.salvage());
            }
        }
        return held;
    }

    private static int count(final List<ItemStack> stacks, final Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void ground(final GameTestHelper helper) {
        for (int dx = -GROUND_REACH; dx <= GROUND_REACH; dx++) {
            for (int dz = -GROUND_REACH; dz <= GROUND_REACH; dz++) {
                for (int dy = 0; dy < GROUND_DEPTH; dy++) {
                    helper.setBlock(MIDDLE.offset(dx, dy, dz), Blocks.DIRT);
                }
            }
        }
    }

    private static int dirt(final GameTestHelper helper) {
        int found = 0;
        for (final BlockPos pos : BlockPos.betweenClosed(MIDDLE.offset(-GROUND_REACH, 0, -GROUND_REACH),
                MIDDLE.offset(GROUND_REACH, GROUND_DEPTH - 1, GROUND_REACH))) {
            if (helper.getBlockState(pos).is(Blocks.DIRT)) {
                found++;
            }
        }
        return found;
    }

    /* Loads the chunks of the yard's next plot, so a structure made comes into it at once. */
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
}
