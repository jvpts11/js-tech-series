/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.NetworkProcessingOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachineBlockEntity;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.CraftingRig;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Operations in flight must survive the world being saved and reopened: the Mainframe writes them into its
 * NBT and resumes them after the boot. These tests replay that round trip server-side: snapshot the running
 * Mainframe's NBT, replace the block, load the snapshot into the fresh block entity, and check the craft
 * carries on where it stopped instead of vanishing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OperationResumeGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);

    private OperationResumeGameTests() {
    }

    /**
     * A job saved mid-run comes back on the interface it had: what it fed before the save is credited to it after,
     * nothing twice, and it settles with the whole yield and the level it was given.
     */
    @GameTest(template = ARENA, timeoutTicks = 500)
    public static void processing_resumesAfterTheMainframeIsSavedAndReloaded(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = CraftingRig.direct(world, net.cc(), TestMachines.KILN.get());
        final ProcessingPattern pattern = CraftingRig.pattern(Items.COBBLESTONE, Items.STONE, 600);
        final StorageKey stone = StorageKey.of(Items.STONE);
        final CompoundTag[] snapshot = new CompoundTag[1];

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.COBBLESTONE, 32);
                    rig.hold(pattern);
                    kiln(rig).setTicksPerItem(6);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkProcessingOperation op = net.mainframe().submitNetworkProcessing(pattern, 16,
                            "resume");
                    helper.assertTrue(op != null, "the processing operation is accepted");
                    // A level other than the default must come back with the operation after the reload.
                    op.setPriority(OperationPriority.HIGH);
                })
                // Let it feed the kiln and collect part of the stone, then take the world's snapshot.
                .thenExecuteAfter(40, () -> {
                    final long stored = net.storage(helper.getLevel()).count(stone);
                    helper.assertTrue(stored > 0 && stored < 16,
                            "part of the stone is collected before the reload; got " + stored);
                    helper.assertTrue(!net.mainframe().activeOperationRecords().isEmpty(),
                            "the operation is still running before the reload");
                    snapshot[0] = net.mainframe().saveWithoutMetadata(helper.getLevel().registryAccess());
                    helper.assertTrue(snapshot[0].contains("ActiveOperations"),
                            "the Mainframe's NBT carries the in-flight operation");
                    /*
                     * Replace the block: the old block entity is torn down as a chunk unload tears it down (its
                     * resumable Operations dropped, never abandoned: breaking it would discard them, and what they
                     * fed would be owed to the discarded job), and the fresh one gets the saved NBT, exactly as
                     * loading the chunk would give it.
                     */
                    world.blockEntity(MAINFRAME, MainframeBlockEntity.class).setRemoved();
                    world.setBlock(MAINFRAME, Blocks.AIR);
                })
                // The kiln goes on working what it was fed while the Mainframe is gone.
                .thenExecuteAfter(SETTLE, () -> {
                    world.setBlock(MAINFRAME, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity fresh = world.blockEntity(MAINFRAME, MainframeBlockEntity.class);
                    fresh.loadWithComponents(snapshot[0], helper.getLevel().registryAccess());
                })
                // Boot, the resume delay, and the rest of the stone.
                .thenExecuteAfter(200, () -> {
                    final MainframeBlockEntity fresh = world.blockEntity(MAINFRAME, MainframeBlockEntity.class);
                    final long stored = net.storage(helper.getLevel()).count(stone);
                    helper.assertTrue(stored == 16, "the resumed job collects the rest of the stone, nothing twice;"
                            + " got " + stored + " active=" + fresh.activeOperationRecords());
                    helper.assertTrue(net.storage(helper.getLevel()).count(StorageKey.of(Items.COBBLESTONE)) == 16,
                            "exactly sixteen cobblestone left the network");
                    final boolean completed = fresh.recentOperations().stream()
                            .anyMatch(r -> r.status() == OperationRecord.STATUS_COMPLETED && r.moved() == 16);
                    helper.assertTrue(completed, "the resumed job settles COMPLETED with the full yield; recent="
                            + fresh.recentOperations() + " active=" + fresh.activeOperationRecords() + " credits="
                            + rig.bus().log().entries() + " owed=" + rig.part().owed().size() + " interface="
                            + rig.part().log().entries());
                    final boolean keptLevel = fresh.recentOperations().stream()
                            .anyMatch(r -> r.status() == OperationRecord.STATUS_COMPLETED
                                    && r.priority() == OperationPriority.HIGH);
                    helper.assertTrue(keptLevel, "the resumed job keeps the HIGH level it was given; recent="
                            + fresh.recentOperations());
                })
                .thenSucceed();
    }

    private static TestMachineBlockEntity kiln(final CraftingRig rig) {
        final TestMachineBlockEntity kiln = rig.machine();
        if (kiln == null) {
            throw new IllegalStateException("no test kiln at " + rig.machinePos());
        }
        return kiln;
    }
}
