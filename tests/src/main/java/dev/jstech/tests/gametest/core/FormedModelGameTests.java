/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModels;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A multiblock whose blocks merge into its controller's model once it forms: its iron turns into merged blocks that
 * remember being iron, built facing any way; breaking one drops its iron and gives every other back as it was; and a
 * merged block whose controller has gone turns back by itself.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FormedModelGameTests {

    private static final String ARENA = "empty";
    /* The controller, in the middle of the south side of a turbine built facing north. */
    private static final BlockPos CONTROLLER = new BlockPos(4, 1, 5);
    private static final int WAIT = 200;

    private FormedModelGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT)
    public static void turbine_mergesItsIronIntoItsModelOnceFormed(final GameTestHelper helper) {
        final List<BlockPos> iron = build(helper, false);
        helper.succeedWhen(() -> {
            final MachineStructure structure = structure(helper);
            helper.assertTrue(structure.formed() && structure.merged(), "the turbine forms and merges");
            helper.assertTrue(structure.orientation().orElseThrow() == Orientation.NORTH,
                    "standing as its pattern is drawn");
            for (final BlockPos at : iron) {
                helper.assertTrue(helper.getBlockState(at).getBlock() instanceof FormedPartBlock,
                        "every iron block merges; not " + at);
                helper.assertTrue(helper.getBlockEntity(at) instanceof FormedPartBlockEntity part
                        && part.original().is(Blocks.IRON_BLOCK), "and remembers being iron");
            }
            helper.assertBlockPresent(TestModels.TURBINE_BLOCK.get(), CONTROLLER);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT)
    public static void turbine_builtTurnedFormsTurned(final GameTestHelper helper) {
        build(helper, true);
        helper.succeedWhen(() -> {
            final MachineStructure structure = structure(helper);
            helper.assertTrue(structure.formed() && structure.merged(), "the turbine built facing east forms");
            helper.assertTrue(structure.orientation().orElseThrow() != Orientation.NORTH,
                    "and stands turned; it stands " + structure.orientation());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT * 2)
    public static void turbine_brokenGivesEveryBlockBack(final GameTestHelper helper) {
        final List<BlockPos> iron = build(helper, false);
        final BlockPos broken = iron.getFirst();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(structure(helper).merged(), "the turbine merges first"))
                .thenExecute(() -> helper.getLevel().destroyBlock(helper.absolutePos(broken), true))
                .thenWaitUntil(() -> {
                    helper.assertTrue(!structure(helper).formed(), "a block of it broken undoes it");
                    for (final BlockPos at : iron) {
                        if (!at.equals(broken)) {
                            helper.assertBlockPresent(Blocks.IRON_BLOCK, at);
                        }
                    }
                    helper.assertBlockPresent(Blocks.AIR, broken);
                    final List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            new AABB(helper.absolutePos(broken)).inflate(2));
                    helper.assertTrue(dropped.stream().anyMatch(item -> item.getItem().is(Items.IRON_BLOCK)),
                            "the broken one drops its iron");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = WAIT * 2)
    public static void mergedBlock_whoseControllerIsGoneTurnsBack(final GameTestHelper helper) {
        final List<BlockPos> iron = build(helper, false);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(structure(helper).merged(), "the turbine merges first"))
                // The controller replaced at once, without its block's breaking: its structure never comes apart.
                .thenExecute(() -> helper.getLevel().removeBlock(helper.absolutePos(CONTROLLER), false))
                .thenWaitUntil(() -> {
                    for (final BlockPos at : iron) {
                        helper.assertBlockPresent(Blocks.IRON_BLOCK, at);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void merges_takesOnlyPlainBlocks(final GameTestHelper helper) {
        helper.assertTrue(FormedParts.merges(Blocks.IRON_BLOCK.defaultBlockState()), "iron merges");
        helper.assertTrue(!FormedParts.merges(Blocks.CHEST.defaultBlockState()),
                "a block that keeps something does not");
        helper.assertTrue(!FormedParts.merges(Blocks.AIR.defaultBlockState()) && !FormedParts.merges(
                Blocks.WATER.defaultBlockState()), "nor air, nor a fluid");
        helper.succeed();
    }

    /*
     * The turbine: two layers of iron three by three, its controller in the middle of the south side, or of the west
     * side for one turned to face east; the places of its iron.
     */
    private static List<BlockPos> build(final GameTestHelper helper, final boolean turned) {
        final List<BlockPos> iron = new ArrayList<>();
        for (int y = 0; y < 2; y++) {
            for (int a = -1; a <= 1; a++) {
                for (int b = 0; b <= 2; b++) {
                    // Facing north the turbine runs north of its controller; turned, it runs east of it.
                    final BlockPos at = turned ? CONTROLLER.offset(b, y, a) : CONTROLLER.offset(a, y, -b);
                    if (!at.equals(CONTROLLER)) {
                        helper.setBlock(at, Blocks.IRON_BLOCK);
                        iron.add(at);
                    }
                }
            }
        }
        helper.setBlock(CONTROLLER, TestModels.TURBINE_BLOCK.get());
        return iron;
    }

    private static MachineStructure structure(final GameTestHelper helper) {
        final TestModels.TurbineEntity turbine = helper.getBlockEntity(CONTROLLER);
        return turbine.structure();
    }
}
