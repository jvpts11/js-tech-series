/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for the Supercomputer Rack: it seats only nodes and drops its own item, and the HBW Interface
 * discovers the nodes seated in racks.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SupercomputerRackGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private SupercomputerRackGameTests() {
    }

    /**
     * A typed cabinet seats only its own kind: a node is refused by a Server Rack and a server by a
     * Supercomputer Rack, while rack equipment fits both. And a dismantled Supercomputer Rack hands back
     * its own item, never a Server Rack.
     */
    @GameTest(template = ARENA)
    public static void supercomputerRack_seatsOnlyNodesAndDropsItsOwnItem(final GameTestHelper helper) {
        final BlockPos serverRack = new BlockPos(2, 2, 2);
        final BlockPos scRack = new BlockPos(6, 2, 2);
        helper.setBlock(serverRack, ComputingModule.SERVER_RACK.get());
        helper.setBlock(scRack, ComputingModule.SUPERCOMPUTER_RACK.get());
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    if (!(helper.getBlockEntity(serverRack) instanceof ServerRackBlockEntity server)
                            || !(helper.getBlockEntity(scRack) instanceof ServerRackBlockEntity sc)) {
                        throw new IllegalStateException("a cabinet is missing its block entity");
                    }
                    final ItemStack node = ServerStacks.defaultSupercomputerNode();
                    final ItemStack plainServer = ServerStacks.defaultServer();
                    final ItemStack kvm = new ItemStack(ComputingModule.KVM_SWITCH.get());
                    helper.assertTrue(sc.rackType()
                                    == dev.jstech.computers.rack.RackChassis.RackType.SUPERCOMPUTER,
                            "the typed cabinet reports its own kind");
                    helper.assertTrue(sc.acceptsChassis(node), "a Supercomputer Rack seats a node");
                    helper.assertTrue(!sc.acceptsChassis(plainServer), "a Supercomputer Rack refuses a server");
                    helper.assertTrue(!server.acceptsChassis(node), "a Server Rack refuses a node");
                    helper.assertTrue(server.acceptsChassis(kvm) && sc.acceptsChassis(kvm),
                            "rack equipment fits every cabinet");
                    helper.assertTrue(!sc.getServers().insertItem(0, plainServer, false).isEmpty(),
                            "the slot itself must refuse the wrong chassis, not only the check");
                    helper.assertTrue(sc.getServers().insertItem(0, node, false).isEmpty(),
                            "the slot takes a node");
                })
                .thenSucceed();
    }

    /**
     * The HBW Interface finds its nodes inside Supercomputer Racks on the high-compute fabric: each
     * seated node is a cluster slot, rated by the co-processor it carries, and the interface's parallel
     * craft budget is the sum of the slots it can actually use.
     */
    @GameTest(template = ARENA)
    public static void hbwInterface_discoversNodesSeatedInRacks(final GameTestHelper helper) {
        final BlockPos hub = new BlockPos(2, 2, 2);
        final BlockPos cable = hub.east();
        final BlockPos rackPos = cable.above(); // a leaf on the fabric, kept inside the arena
        helper.setBlock(hub, ComputingModule.HBW_INTERFACE.get());
        TestCables.lay(helper, cable, ComputingModule.HPC_CABLE);
        helper.setBlock(rackPos, ComputingModule.SUPERCOMPUTER_RACK.get());
        if (helper.getBlockEntity(rackPos) instanceof ServerRackBlockEntity rack) {
            rack.getServers().setStackInSlot(0, ServerStacks.defaultSupercomputerNode());
            rack.getServers().setStackInSlot(2, ServerStacks.defaultSupercomputerNode());
        } else {
            helper.fail("no Supercomputer Rack block entity placed");
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    if (!(helper.getBlockEntity(hub)
                            instanceof dev.jstech.computers.blockentity.HbwInterfaceBlockEntity hbw)) {
                        throw new IllegalStateException("no HBW Interface block entity");
                    }
                    final var slots = hbw.clusterSlots();
                    helper.assertTrue(slots.size() == 2,
                            "two seated nodes are two cluster slots; got " + slots.size());
                    helper.assertTrue(slots.get(0).node().equals(helper.absolutePos(rackPos))
                                    && slots.get(0).row() == 0 && slots.get(1).row() == 2,
                            "slots point at the rack and the rows the nodes occupy");
                    // Two Phi 5100 fill slots 1 and 2: 8 + 16 parallel crafts.
                    helper.assertTrue(hbw.parallelCrafts() == 24,
                            "the budget sums the rated slots; got " + hbw.parallelCrafts());
                    // Switch the second node's bay off: its slot goes dark and the budget drops.
                    ((ServerRackBlockEntity) helper.getBlockEntity(rackPos)).toggleBayPower(2);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    if (helper.getBlockEntity(hub)
                            instanceof dev.jstech.computers.blockentity.HbwInterfaceBlockEntity hbw) {
                        helper.assertTrue(hbw.parallelCrafts() == 8,
                                "a node whose bay is off contributes nothing; got " + hbw.parallelCrafts());
                    }
                })
                .thenSucceed();
    }
}
