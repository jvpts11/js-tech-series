/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.MainframePartBlock;
import dev.jstech.computers.block.MainframeStructure;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.droppedItems;
import static dev.jstech.tests.testkit.NetworkFixtures.formRunningMainframe;
import static dev.jstech.tests.testkit.NetworkFixtures.networkOf;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for the Mainframe multiblock: forming and dissolving, reading a cable on any part face, the core
 * face of the central column, teardown drops, and picking up an installed disk.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MainframeMultiblockGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private MainframeMultiblockGameTests() {
    }

    @GameTest(template = ARENA)
    public static void mainframe_ignoresEthernetCable(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(2, 2, 2);
        final BlockPos eth = new BlockPos(3, 2, 2); // Mainframe directly against an Ethernet cable
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(mainframe.networkUuid() != null, "mainframe owns its native network");
                    helper.assertTrue(networkOf(helper, eth).isEmpty(),
                            "a Mainframe must not assign its UUID to an Ethernet cable");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_formsAndDissolves(final GameTestHelper helper) {
        final BlockPos controller = new BlockPos(4, 2, 4);
        final Direction facing = Direction.NORTH;
        helper.setBlock(controller, ComputingModule.MAINFRAME.get().defaultBlockState()
                .setValue(MainframeBlock.FACING, facing));
        // Drive the self-assembly the way item placement would.
        ((MainframeBlock) ComputingModule.MAINFRAME.get()).setPlacedBy(
                helper.getLevel(), helper.absolutePos(controller),
                helper.getBlockState(controller), null, ItemStack.EMPTY);

        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    int count = 0;
                    for (final BlockPos p : MainframeStructure.allPositions(controller, facing)) {
                        final var block = helper.getBlockState(p).getBlock();
                        if (block instanceof MainframeBlock || block instanceof MainframePartBlock) {
                            count++;
                        }
                    }
                    helper.assertTrue(count == MainframeStructure.BLOCK_COUNT,
                            "the 3x2x2 footprint should hold " + MainframeStructure.BLOCK_COUNT
                                    + " blocks, found " + count);
                })
                .thenExecute(() -> helper.setBlock(
                        MainframeStructure.partPositions(controller, facing).get(0), Blocks.AIR))
                .thenExecuteAfter(2, () -> {
                    int remaining = 0;
                    for (final BlockPos p : MainframeStructure.allPositions(controller, facing)) {
                        final var block = helper.getBlockState(p).getBlock();
                        if (block instanceof MainframeBlock || block instanceof MainframePartBlock) {
                            remaining++;
                        }
                    }
                    helper.assertTrue(remaining == 0,
                            "breaking one part must dissolve the whole structure, " + remaining + " left");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_readsCableOnAnyPartFace(final GameTestHelper helper) {
        final BlockPos controller = new BlockPos(4, 2, 4);
        final Direction facing = Direction.NORTH;
        final MainframeBlockEntity be = formRunningMainframe(helper, controller, facing);
        /*
         * The far-right part sits two blocks from the controller; a cable on its
         * outward face is never adjacent to the controller itself.
         */
        final BlockPos farPart = controller.relative(facing.getClockWise());
        final BlockPos cable = farPart.relative(facing.getClockWise());
        TestCables.lay(helper, cable, ComputingModule.HBW_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(be.networkUuid() != null, "controller must own a network");
                    helper.assertTrue(networkOf(helper, cable).map(be.networkUuid()::equals).orElse(false),
                            "a cable on a part face must carry the controller's network UUID");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_centralColumnGetsCoreFace(final GameTestHelper helper) {
        final BlockPos controller = new BlockPos(4, 2, 4);
        final Direction facing = Direction.NORTH;
        formRunningMainframe(helper, controller, facing);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    for (final BlockPos p : MainframeStructure.partPositions(controller, facing)) {
                        final boolean expected = MainframeStructure.isCentralColumn(controller, facing, p);
                        final var st = helper.getBlockState(p);
                        helper.assertTrue(st.getBlock() instanceof MainframePartBlock, "part missing at " + p);
                        helper.assertTrue(st.getValue(MainframePartBlock.CORE) == expected,
                                "core flag at " + p + " should be " + expected);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_teardownDropsNothing(final GameTestHelper helper) {
        final BlockPos controller = new BlockPos(4, 2, 4);
        final Direction facing = Direction.NORTH;
        formRunningMainframe(helper, controller, facing); // installs a full hardware build
        helper.startSequence()
                .thenExecuteAfter(2, () -> helper.setBlock(
                        MainframeStructure.partPositions(controller, facing).get(0), Blocks.AIR))
                .thenExecuteAfter(2, () -> helper.assertTrue(droppedItems(helper, controller) == 0,
                        "dissolving the multiblock must not drop items (creative-safe teardown)"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void mainframe_buildPicksUpInstalledDisk(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final MainframeBlockEntity be = placeRunningMainframe(helper, a);
        // Replace the disk installed by the setup helper with a larger one.
        be.getInventory().setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    /*
                     * The installed OS (mc_net, 512-item footprint) reserves part of the disk;
                     * the usable storage is the raw disk capacity minus the OS reservation.
                     */
                    final long osFootprint = be.reservedByOs();
                    final long expected = DiskSize.TB_1.capacityItems() - osFootprint;
                    helper.assertTrue(
                            be.storageItems() == expected,
                            "build should report the installed disk's capacity net of the OS footprint"
                                    + " (raw=" + DiskSize.TB_1.capacityItems()
                                    + " footprint=" + osFootprint
                                    + " expected=" + expected
                                    + "); got " + be.storageItems());
                })
                .thenSucceed();
    }
}
