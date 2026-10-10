/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRackPartBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for the Server Rack: servers registering as nodes on the network, the cabinet forming, and housed
 * servers unregistering when their chunk unloads.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ServerRackGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private ServerRackGameTests() {
    }

    @GameTest(template = ARENA)
    public static void serverRack_registersAndUnregistersServer(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos rack = new BlockPos(3, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // cables attach through the rear (west side here)
        if (helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe) {
            TestWorldBuilder.mountDefaultServer(rackBe, 0);
        } else {
            helper.fail("no server rack block entity placed");
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(net != null, "mainframe owns a network");
                    final var servers = NetworkSystem.get(helper.getLevel()).serversOf(net);
                    helper.assertTrue(servers.size() == 1,
                            "the rack's Server should register on the mainframe network; got " + servers.size());
                    helper.assertTrue(servers.get(0).storageItems() > 0,
                            "registered Server should report its disk storage");
                })
                .thenExecute(() -> helper.setBlock(rack, Blocks.AIR)) // break the rack
                .thenExecuteAfter(SETTLE, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).serversOf(net).isEmpty(),
                            "breaking the rack must unregister its Server");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void serverRack_formsCabinetAndReadsCableOnPartFace(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(4, 2, 2);     // controller; footprint x[4,5] y[2,4] z[2,3]
        final BlockPos part = new BlockPos(4, 3, 2);     // a front part, one up from the controller
        // A cable run from the part's outward face to the mainframe, and it touches the
        final BlockPos[] cables = {
            new BlockPos(2, 3, 2), // against the REAR face of part (3,3,2), parts only
            new BlockPos(2, 2, 2), // claimed by the mainframe
        };
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        for (final BlockPos c : cables) {
            TestCables.lay(helper, c, ComputingModule.HBW_CABLE);
        }
        // Place the controller and drive its self-assembly so all 11 parts exist.
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // cables attach through the rear (west side here)
        ((ServerRackBlock) ComputingModule.SERVER_RACK.get()).setPlacedBy(
                helper.getLevel(), helper.absolutePos(rack), helper.getBlockState(rack), null, ItemStack.EMPTY);
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(helper.getBlockState(part).getBlock() instanceof ServerRackPartBlock,
                            "placing the rack must raise its structural parts");
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(net != null, "mainframe owns a network");
                    // The controller touches no cable; only a part does.
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).serversOf(net).size() == 1,
                            "a cable on a rack PART face must join the rack to the network");
                })
                .thenExecute(() -> helper.setBlock(part, Blocks.AIR)) // break one part
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(helper.getBlockState(rack).isAir(),
                            "breaking a part must take the controller down too");
                    helper.assertTrue(helper.getBlockState(part).isAir(),
                            "the broken part must be gone");
                    helper.assertTrue(helper.getBlockState(new BlockPos(5, 4, 3)).isAir(),
                            "the whole cabinet must dissolve, including the far-top-back corner");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void chunkUnload_unregistersHousedServers(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos rack = new BlockPos(3, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).serversOf(net).size() == 1,
                            "the rack's server is registered before the unload");
                    // A chunk unload removes the block entity without firing the block's onRemove.
                    rackBe.setRemoved();
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).serversOf(net).isEmpty(),
                            "setRemoved (the chunk-unload path) must unregister the housed servers");
                })
                .thenSucceed();
    }
}
