/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRackPartBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.computers.NetworkFixtures.placeRunningMainframe;

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
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)); // cables attach at the rear, west here
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
        final BlockPos rack = new BlockPos(4, 2, 2);     // origin, facing east: the cabinet reaches back west to x 3
        final BlockPos part = new BlockPos(4, 3, 2);     // a front part, one up from the origin
        // A cable run from the rear face of the cabinet's back parts to the mainframe; it touches no origin face.
        final List<BlockPos> parts = new ArrayList<>();
        final BlockPos[] cables = {
            new BlockPos(2, 3, 2), // against the REAR face of part (3,3,2), parts only
            new BlockPos(2, 2, 2), // claimed by the mainframe
        };
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        for (final BlockPos c : cables) {
            TestCables.lay(helper, c, ComputingModule.HBW_CABLE);
        }
        // Place the origin and drive its self-assembly so all 11 parts exist.
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)); // cables attach at the rear, west here
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
                    // Every part the cabinet raised, wherever its rotation put it, so its whole going can be checked.
                    for (final BlockPos at : BlockPos.betweenClosed(2, 2, 0, 6, 4, 4)) {
                        if (helper.getBlockState(at).getBlock() instanceof ServerRackPartBlock) {
                            parts.add(at.immutable());
                        }
                    }
                    helper.assertTrue(parts.size() == 11, "the cabinet raises its eleven parts; found " + parts);
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(net != null, "mainframe owns a network");
                    // The origin touches no cable; only a part does.
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).serversOf(net).size() == 1,
                            "a cable on a rack PART face must join the rack to the network");
                })
                .thenExecute(() -> helper.setBlock(part, Blocks.AIR)) // break one part
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(helper.getBlockState(rack).isAir(),
                            "breaking a part must take the origin down too");
                    helper.assertTrue(helper.getBlockState(part).isAir(),
                            "the broken part must be gone");
                    for (final BlockPos at : parts) {
                        helper.assertTrue(helper.getBlockState(at).isAir(),
                                "the whole cabinet must dissolve; still standing at " + at);
                    }
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
