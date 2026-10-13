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
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.datacenter.DatacenterSection;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.networkOf;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;
import static dev.jstech.tests.testkit.NetworkFixtures.sameNetwork;
import static dev.jstech.tests.testkit.NetworkFixtures.seedServer;

/**
 * GameTests for the Server Router: bridging a network across faces, sectioning racks per face, ignoring
 * Supercomputer Racks, and splitting the network when it is removed.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ServerRouterGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private ServerRouterGameTests() {
    }

    @GameTest(template = ARENA)
    public static void serverRouter_bridgesNetworkAcrossFaces(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbwA = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos hbwB = new BlockPos(4, 2, 2);
        final BlockPos rack = new BlockPos(5, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbwA, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.SERVER_ROUTER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // back (the uplink) faces the Mainframe cable to the west
        TestCables.lay(helper, hbwB, ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // cables attach through the rear (west side here)
        seedServer(helper, rack);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final NetworkUuid net = mainframe.networkUuid();
                    helper.assertTrue(net != null, "mainframe owns a network");
                    helper.assertTrue(sameNetwork(helper, hbwA, hbwB),
                            "the Server Router bridges the cables on its two faces into one network");
                    helper.assertTrue(networkOf(helper, router).map(net::equals).orElse(false),
                            "the router sits on the mainframe's network");
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).serversOf(net).size() == 1,
                            "the rack behind the router registers its Server on the mainframe network");
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).routersOf(net).size() == 1,
                            "the router registers itself as a topology element on the network");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void serverRouter_groupsRacksIntoSections(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbwIn = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos hbwEast = new BlockPos(4, 2, 2);
        final BlockPos rackEast = new BlockPos(5, 2, 2);
        final BlockPos hbwSouth = new BlockPos(3, 2, 3);
        final BlockPos rackSouth = new BlockPos(3, 2, 4);
        placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbwIn, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.SERVER_ROUTER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // back (the uplink) faces the Mainframe cable to the west
        TestCables.lay(helper, hbwEast, ComputingModule.HBW_CABLE);
        helper.setBlock(rackEast, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // rear faces the section cable to the west
        TestCables.lay(helper, hbwSouth, ComputingModule.HBW_CABLE);
        helper.setBlock(rackSouth, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the section cable to the north
        seedServer(helper, rackEast);
        seedServer(helper, rackSouth);
        if (!(helper.getBlockEntity(router) instanceof ServerRouterBlockEntity routerBe)) {
            helper.fail("no server router");
            return;
        }
        helper.startSequence()
                // Let the racks register their Servers, then force a fresh topology compute.
                .thenExecuteAfter(SETTLE + 4, routerBe::recomputeNow)
                .thenExecute(() -> {
                    helper.assertTrue(routerBe.inputFace() == Direction.WEST,
                            "the back face is the dedicated uplink; got " + routerBe.inputFace());
                    final java.util.List<DatacenterSection> sections = routerBe.sections();
                    helper.assertTrue(sections.size() == 2,
                            "two output faces with racks form two sections; got " + sections.size());
                    for (final DatacenterSection section : sections) {
                        helper.assertTrue(section.rackCount() == 1,
                                "each section has one rack; got " + section.rackCount());
                        helper.assertTrue(section.serverCount() == 1,
                                "each section has one Server; got " + section.serverCount());
                    }
                    final java.util.Set<Direction> faces = new java.util.HashSet<>();
                    for (final DatacenterSection section : sections) {
                        faces.add(section.face());
                    }
                    helper.assertTrue(faces.contains(Direction.EAST) && faces.contains(Direction.SOUTH),
                            "sections hang off the EAST and SOUTH output faces; got " + faces);
                })
                .thenSucceed();
    }

    /**
     * Every supercomputer runs its own queue: a craft asks the online supercomputer with the most free
     * slots, so a second supercomputer takes work while the first is full instead of idling behind it.
     */
    @GameTest(template = ARENA)
    public static void supercomputers_craftsGoToTheOneWithRoom(final GameTestHelper helper) {
        /*
         * A cluster is online only on a network: one Mainframe feeds a bandwidth cable along z=2, and each
         * interface hangs off it to the south with its own fabric (HPC cable + rack) further south, far
         * enough apart that the two fabrics never touch.
         */
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hubA = new BlockPos(3, 2, 3);
        final BlockPos hubB = new BlockPos(6, 2, 3);
        placeRunningMainframe(helper, m);
        for (int x = 2; x <= 6; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 2), ComputingModule.HBW_CABLE);
        }
        for (final BlockPos hub : new BlockPos[]{hubA, hubB}) {
            final BlockPos cable = hub.south();
            final BlockPos rackPos = cable.above();
            helper.setBlock(hub, ComputingModule.HBW_INTERFACE.get());
            TestCables.lay(helper, cable, ComputingModule.HPC_CABLE);
            helper.setBlock(rackPos, ComputingModule.SUPERCOMPUTER_RACK.get());
            if (helper.getBlockEntity(rackPos) instanceof ServerRackBlockEntity rack) {
                rack.getServers().setStackInSlot(0, ServerStacks.defaultSupercomputerNode());
            }
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    if (!(helper.getBlockEntity(hubA)
                            instanceof dev.jstech.computers.blockentity.HbwInterfaceBlockEntity a)
                            || !(helper.getBlockEntity(hubB)
                            instanceof dev.jstech.computers.blockentity.HbwInterfaceBlockEntity b)) {
                        throw new IllegalStateException("an HBW Interface is missing its block entity");
                    }
                    helper.assertTrue(a.parallelCrafts() == 8 && b.parallelCrafts() == 8,
                            "each cluster rates one slot of 8 crafts; got " + a.parallelCrafts() + "/" + b.parallelCrafts());
                    helper.assertTrue(a.clusterOnline() && b.clusterOnline(),
                            "both clusters are online on the Mainframe's network");
                    final var both = java.util.List.of(a, b);
                    // Fill A completely: the next craft must be sent to B, not left waiting on A.
                    final java.util.UUID holdingA = java.util.UUID.randomUUID();
                    a.acquireCraftSlots(holdingA, 8);
                    helper.assertTrue(dev.jstech.computers.crafting.NetworkCraftOperation
                                    .chooseLeastLoaded(both) == b,
                            "with A full, the craft goes to B");
                    // Free A while B holds three: A has 8 free against B's 5, so A is chosen again.
                    a.releaseCraftSlot(holdingA);
                    b.acquireCraftSlots(java.util.UUID.randomUUID(), 3);
                    helper.assertTrue(dev.jstech.computers.crafting.NetworkCraftOperation
                                    .chooseLeastLoaded(both) == a,
                            "the emptier supercomputer wins");
                })
                .thenSucceed();
    }

    /**
     * A datacenter is made of Server Racks. The router's branch walk follows any data cable, the
     * high-compute fabric included, so a Supercomputer Rack it reaches that way must still not become a
     * section member, while a Server Rack on another face forms its section as usual.
     */
    @GameTest(template = ARENA)
    public static void serverRouter_ignoresSupercomputerRacks(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbwIn = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos hpcEast = new BlockPos(4, 2, 2);
        final BlockPos scRack = new BlockPos(5, 2, 2);
        final BlockPos hbwSouth = new BlockPos(3, 2, 3);
        final BlockPos rackSouth = new BlockPos(3, 2, 4);
        placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbwIn, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.SERVER_ROUTER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // back (the uplink) faces the Mainframe cable to the west
        TestCables.lay(helper, hpcEast, ComputingModule.HPC_CABLE);
        helper.setBlock(scRack, ComputingModule.SUPERCOMPUTER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // rear faces the fabric cable to the west
        if (helper.getBlockEntity(scRack) instanceof ServerRackBlockEntity sc) {
            sc.getServers().setStackInSlot(0, ServerStacks.defaultSupercomputerNode());
        }
        TestCables.lay(helper, hbwSouth, ComputingModule.HBW_CABLE);
        helper.setBlock(rackSouth, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the section cable to the north
        seedServer(helper, rackSouth);
        if (!(helper.getBlockEntity(router) instanceof ServerRouterBlockEntity routerBe)) {
            helper.fail("no server router");
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, routerBe::recomputeNow)
                .thenExecute(() -> {
                    final java.util.List<DatacenterSection> sections = routerBe.sections();
                    helper.assertTrue(sections.size() == 1,
                            "only the Server Rack forms a section; got " + sections.size());
                    helper.assertTrue(sections.get(0).face() == Direction.SOUTH,
                            "the section is the Server Rack's, not the supercomputer's; got "
                                    + sections.get(0).face());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void serverRouter_removalSplitsNetworkAndUnregisters(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbwA = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos hbwB = new BlockPos(4, 2, 2);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbwA, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.SERVER_ROUTER.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.EAST)); // back (the uplink) faces the Mainframe cable to the west
        TestCables.lay(helper, hbwB, ComputingModule.HBW_CABLE);
        final NetworkUuid[] net = new NetworkUuid[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    net[0] = mainframe.networkUuid();
                    helper.assertTrue(net[0] != null, "mainframe owns a network");
                    helper.assertTrue(sameNetwork(helper, hbwA, hbwB),
                            "the router bridges the two cable runs while present");
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).routersOf(net[0]).size() == 1,
                            "the router is registered while present");
                })
                .thenExecute(() -> helper.setBlock(router, Blocks.AIR)) // remove the router
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertFalse(sameNetwork(helper, hbwA, hbwB),
                            "removing the router splits its two cable runs apart");
                    helper.assertTrue(networkOf(helper, hbwB).isEmpty(),
                            "the far run, cut off from the Mainframe, becomes network-less");
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).routersOf(net[0]).isEmpty(),
                            "the removed router unregisters itself from the network");
                })
                .thenSucceed();
    }
}
