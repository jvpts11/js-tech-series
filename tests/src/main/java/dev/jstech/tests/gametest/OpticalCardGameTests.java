/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.OpticalPort;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Optical Network Card: the fibre of the backbone enters a Mainframe, a rack or a Cluster Management Computer only
 * while the machine holds one, every block of a cabinet with it, and leaves again when it is taken out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OpticalCardGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private OpticalCardGameTests() {
    }

    /** A Mainframe takes the fibre only while it holds the card, and the card is told on every block of its cabinet. */
    @GameTest(template = ARENA)
    public static void mainframe_takesTheFibreOnlyWithTheCard(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(4, 2, 4);
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(at);
        final BlockState state = helper.getBlockState(at);
        state.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(at), state, null, ItemStack.EMPTY);
        final List<BlockPos> cabinet = ((MainframeBlock) state.getBlock())
                .footprint(helper.absolutePos(at), state.getValue(HorizontalDirectionalBlock.FACING));
        final Direction side = freeSide(helper, at, cabinet);
        final BlockPos fibre = at.relative(side);
        TestCables.lay(helper, fibre, ComputingModule.FIBRE_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(crosses(helper, fibre, side.getOpposite()), "no card, and the fibre stays out");
                    mainframe.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get()));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    for (final BlockPos block : cabinet) {
                        helper.assertTrue(helper.getLevel().getBlockState(block).getValue(OpticalPort.OPTICAL),
                                "every block of the cabinet knows of the card, " + block.toShortString() + " too");
                    }
                    helper.assertTrue(crosses(helper, fibre, side.getOpposite()), "with the card the fibre goes in");
                    mainframe.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, ItemStack.EMPTY);
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(crosses(helper, fibre, side.getOpposite()),
                        "the card taken out, the fibre is left out again"))
                .thenSucceed();
    }

    /** A Server Rack takes the fibre while a server seated in it holds the card. */
    @GameTest(template = ARENA)
    public static void rack_takesTheFibreWithACardInAServer(final GameTestHelper helper) {
        final BlockPos rack = new BlockPos(2, 2, 2);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockPos back = rack.south();
        TestCables.lay(helper, back, ComputingModule.FIBRE_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(crosses(helper, back, Direction.NORTH), "no card in any server, no fibre");
                    cabinet(helper, rack).getServers().setStackInSlot(0, ServerStacks.opticalServer());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(helper.getBlockState(rack).getValue(OpticalPort.OPTICAL),
                            "the cabinet knows a server holds the card");
                    helper.assertTrue(crosses(helper, back, Direction.NORTH), "and takes the fibre");
                    cabinet(helper, rack).getServers().setStackInSlot(0, ServerStacks.defaultServer());
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(crosses(helper, back, Direction.NORTH),
                        "a server without one in its place, the fibre is left out"))
                .thenSucceed();
    }

    /** A Cluster Management Computer takes the fibre on its back only with the card. */
    @GameTest(template = ARENA)
    public static void clusterManager_takesTheFibreOnlyWithTheCard(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ComputingModule.CLUSTER_MANAGEMENT_COMPUTER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        final BlockPos back = at.south();
        TestCables.lay(helper, back, ComputingModule.FIBRE_CABLE);
        if (!(helper.getBlockEntity(at) instanceof ClusterManagementComputerBlockEntity manager)) {
            helper.fail("no Cluster Management Computer placed");
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(crosses(helper, back, Direction.NORTH), "no card, no fibre");
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.CPU_SLOT,
                            new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.PSU_SLOT,
                            new ItemStack(ComputingModule.PSU_650G.get()));
                    manager.getHardware().setStackInSlot(ClusterManagementComputerBlockEntity.PCIE_SLOTS_START,
                            new ItemStack(ComputingModule.OPTICAL_NETWORK_CARD.get()));
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(crosses(helper, back, Direction.NORTH),
                        "assembled with the card, it takes the fibre"))
                .thenSucceed();
    }

    private static boolean crosses(final GameTestHelper helper, final BlockPos cable, final Direction face) {
        return TestCables.cable(helper, cable).crosses(ComputingModule.FIBRE_CABLE.get(), face);
    }

    private static ServerRackBlockEntity cabinet(final GameTestHelper helper, final BlockPos at) {
        if (!(helper.getBlockEntity(at) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no Server Rack at " + at.toShortString());
        }
        return rack;
    }

    /* A side of the block at {@code at} that the cabinet does not stand on, for a cable to come in by. */
    private static Direction freeSide(final GameTestHelper helper, final BlockPos at, final List<BlockPos> cabinet) {
        for (final Direction side : Direction.Plane.HORIZONTAL) {
            if (!cabinet.contains(helper.absolutePos(at.relative(side)))) {
                return side;
            }
        }
        throw new IllegalStateException("the cabinet stands on every side of " + at.toShortString());
    }
}
