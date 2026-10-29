/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.space.geology.LooseStones;
import dev.jstech.space.geology.LooseStonesBlock;
import dev.jstech.space.geology.StoneArrangements;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Loose stones: small stones lying on the ground, never a whole block, walked through, broken to be picked up whole,
 * lying only on ground that holds them and going with it when it goes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LooseStonesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos STONES = new BlockPos(1, 2, 1);
    private static final List<BlockEntry<LooseStonesBlock>> KINDS = List.of(LooseStones.MARE_BASALT_STONES,
            LooseStones.ANORTHOSITE_STONES, LooseStones.MARTIAN_BASALT_STONES);

    private LooseStonesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void stones_areSmallAndWalkedThroughInEveryArrangementAndFacing(final GameTestHelper helper) {
        for (final BlockEntry<LooseStonesBlock> kind : KINDS) {
            for (int arrangement = 0; arrangement < StoneArrangements.ALL.size(); arrangement++) {
                for (final Direction facing : Direction.Plane.HORIZONTAL) {
                    final BlockState state = kind.get().defaultBlockState()
                            .setValue(LooseStonesBlock.ARRANGEMENT, arrangement)
                            .setValue(LooseStonesBlock.FACING, facing);
                    final String id = kind.getId() + " " + arrangement + " " + facing;
                    final AABB outline = state.getShape(helper.getLevel(), BlockPos.ZERO).bounds();
                    helper.assertTrue(outline.maxY < 1 && outline.getXsize() < 1 && outline.getZsize() < 1,
                            id + " is smaller than a block: " + outline);
                    helper.assertTrue(state.getCollisionShape(helper.getLevel(), BlockPos.ZERO,
                            CollisionContext.empty()).isEmpty(), id + " is walked through");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void stones_areBrokenAtOnceAndPickedUpWhole(final GameTestHelper helper) {
        for (final BlockEntry<LooseStonesBlock> kind : KINDS) {
            final BlockState state = kind.get().defaultBlockState();
            helper.assertTrue(state.getDestroySpeed(helper.getLevel(), BlockPos.ZERO) == 0,
                    kind.getId() + " breaks at once");
            final List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(STONES), null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(kind.get().asItem()),
                    kind.getId() + " gives itself back; got " + drops);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void stones_goWithTheGroundUnderThem(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        final BlockState stones = LooseStones.MARE_BASALT_STONES.get().defaultBlockState();
        helper.assertTrue(stones.canSurvive(helper.getLevel(), helper.absolutePos(STONES)),
                "stones lie on stone");
        helper.setBlock(STONES, stones);
        helper.assertFalse(stones.canSurvive(helper.getLevel(), helper.absolutePos(STONES.above())),
                "and not on air");
        helper.setBlock(GROUND, Blocks.AIR);
        helper.succeedWhen(() -> helper.assertBlockNotPresent(LooseStones.MARE_BASALT_STONES.get(), STONES));
    }

    @GameTest(template = ARENA)
    public static void stones_lieAsChanceHasItTheSameAtTheSamePlace(final GameTestHelper helper) {
        final LooseStonesBlock block = LooseStones.MARE_BASALT_STONES.get();
        final BlockPos here = helper.absolutePos(STONES);
        int differ = 0;
        for (int x = 0; x < 16; x++) {
            final BlockState first = placed(helper, block, here.east(x));
            helper.assertTrue(first.equals(placed(helper, block, here.east(x))),
                    "stones placed again at the same place lie the same way");
            differ += first.equals(placed(helper, block, here)) ? 0 : 1;
        }
        helper.assertTrue(differ > 0, "and other places lie other ways");
        helper.succeed();
    }

    /* The way {@code block} lies when placed at {@code at}, as a player placing it from above would. */
    private static BlockState placed(final GameTestHelper helper, final LooseStonesBlock block, final BlockPos at) {
        return block.getStateForPlacement(new BlockPlaceContext(helper.getLevel(), null, InteractionHand.MAIN_HAND,
                new ItemStack(block), new BlockHitResult(at.getCenter(), Direction.UP, at, false)));
    }
}
