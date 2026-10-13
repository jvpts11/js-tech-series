/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Where each machine enters the network: the small computers by the access line to a router and never straight onto
 * the backbone, the Mainframe and the racks on the backbone, and the supercomputer's nodes on the high-compute fabric
 * of their era or an earlier one, up to the HBW Interface.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkEntryGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private NetworkEntryGameTests() {
    }

    /** A Server Rack takes the backbone on its back and no longer the access line. */
    @GameTest(template = ARENA)
    public static void rack_takesTheBackboneAndNotTheAccessLine(final GameTestHelper helper) {
        final BlockPos rack = new BlockPos(2, 2, 2);
        facingNorth(helper, rack, ComputingModule.SERVER_RACK.get());
        final BlockPos back = rack.south();
        TestCables.lay(helper, back, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, back, ComputingModule.HBW_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(TestCables.cable(helper, back).crosses(ComputingModule.ETHERNET_CABLE.get(),
                            Direction.NORTH), "the Ethernet stops at the rack's back");
                    helper.assertTrue(TestCables.cable(helper, back).crosses(ComputingModule.HBW_CABLE.get(),
                            Direction.NORTH), "while the HBW goes into it");
                })
                .thenSucceed();
    }

    /** A computer on the access line laid straight to the Mainframe stays off the network: it needs a router. */
    @GameTest(template = ARENA)
    public static void smallComputer_doesNotEnterTheMainframeDirectly(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        final BlockPos cable = new BlockPos(2, 2, 2);
        world.setBlock(cable, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(new BlockPos(3, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 2, () -> {
                    helper.assertFalse(TestCables.cable(helper, cable).crosses(ComputingModule.ETHERNET_CABLE.get(),
                            Direction.WEST), "the Mainframe takes no access cable");
                    helper.assertTrue(pc.networkUuid() == null, "and the computer is off the network");
                })
                .thenSucceed();
    }

    /** The Transition's InfiniBand reaches a node in the Standard Supercomputer Rack, an era that takes it. */
    @GameTest(template = ARENA)
    public static void infiniBand_reachesTheStandardSupercomputerRack(final GameTestHelper helper) {
        cluster(helper, ComputingModule.INFINIBAND_CABLE, ComputingModule.SUPERCOMPUTER_RACK.get(), 1,
                "the InfiniBand reaches the Standard rack's node");
    }

    /** The Advanced rack takes every earlier high-compute cable too. */
    @GameTest(template = ARENA)
    public static void hpc_reachesTheAdvancedSupercomputerRack(final GameTestHelper helper) {
        cluster(helper, ComputingModule.HPC_CABLE, ComputingModule.ADVANCED_SUPERCOMPUTER_RACK.get(), 1,
                "the Standard's cable reaches the Advanced rack's node");
    }

    /** The Standard HBW Interface takes no OSFP, the Advanced's cable, so the nodes along it are not its. */
    @GameTest(template = ARENA)
    public static void standardInterface_takesNoOsfp(final GameTestHelper helper) {
        cluster(helper, ComputingModule.OSFP_CABLE, ComputingModule.ADVANCED_SUPERCOMPUTER_RACK.get(), 0,
                "the OSFP does not reach the Standard interface");
    }

    /** The Advanced's OSFP goes into the Advanced Supercomputer Rack and stops at the Standard one. */
    @GameTest(template = ARENA)
    public static void osfp_entersTheAdvancedRackAndNotTheStandardOne(final GameTestHelper helper) {
        final BlockPos standard = new BlockPos(1, 2, 2);
        final BlockPos advanced = new BlockPos(3, 2, 2);
        facingNorth(helper, standard, ComputingModule.SUPERCOMPUTER_RACK.get());
        facingNorth(helper, advanced, ComputingModule.ADVANCED_SUPERCOMPUTER_RACK.get());
        TestCables.lay(helper, standard.south(), ComputingModule.OSFP_CABLE);
        TestCables.lay(helper, advanced.south(), ComputingModule.OSFP_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(TestCables.cable(helper, standard.south()).crosses(
                            ComputingModule.OSFP_CABLE.get(), Direction.NORTH), "the Standard rack takes no OSFP");
                    helper.assertTrue(TestCables.cable(helper, advanced.south()).crosses(
                            ComputingModule.OSFP_CABLE.get(), Direction.NORTH), "the Advanced rack takes it");
                })
                .thenSucceed();
    }

    /*
     * An HBW Interface with a run of {@code fabric} beside it and a cabinet of {@code rack} on the run holding one node;
     * passes when the interface finds {@code nodes} nodes.
     */
    private static void cluster(final GameTestHelper helper, final CableEntry fabric, final Block rack,
                                final int nodes, final String what) {
        final BlockPos hub = new BlockPos(2, 2, 2);
        final BlockPos cable = hub.east();
        final BlockPos cabinet = cable.above();
        helper.setBlock(hub, ComputingModule.HBW_INTERFACE.get());
        TestCables.lay(helper, cable, fabric);
        helper.setBlock(cabinet, rack);
        if (!(helper.getBlockEntity(cabinet) instanceof ServerRackBlockEntity seated)) {
            helper.fail("no Supercomputer Rack block entity placed");
            return;
        }
        seated.getServers().setStackInSlot(0, ServerStacks.defaultSupercomputerNode());
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    if (!(helper.getBlockEntity(hub) instanceof HbwInterfaceBlockEntity hbw)) {
                        throw new IllegalStateException("no HBW Interface block entity");
                    }
                    helper.assertTrue(hbw.clusterNodes().size() == nodes,
                            what + "; found " + hbw.clusterNodes().size());
                })
                .thenSucceed();
    }

    private static void facingNorth(final GameTestHelper helper, final BlockPos pos, final Block rack) {
        helper.setBlock(pos, rack.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }
}
