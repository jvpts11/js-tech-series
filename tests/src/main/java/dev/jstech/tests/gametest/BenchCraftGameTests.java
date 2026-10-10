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
import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftingFixtures.Network;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.CraftingFixtures.buildCraftingNetwork;
import static dev.jstech.tests.testkit.CraftingFixtures.planksPattern;
import static dev.jstech.tests.testkit.CraftingFixtures.sticksPattern;
import static dev.jstech.tests.testkit.CraftingFixtures.storageKey;

/**
 * GameTests for bench crafts over a real network: a single pattern end to end, recursion with surplus,
 * partial scale-down, ingredients drawn across servers, and a request with no pattern.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BenchCraftGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private BenchCraftGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craft_executesSinglePatternEndToEnd(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 2);
                    net.cc().loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(net.mainframe().submitNetworkCraft(
                        storageKey(Items.OAK_PLANKS), 8, false, "test") != null, "a feasible CRAFT is accepted"))
                .thenExecuteAfter(40, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    helper.assertTrue(storage.count(Items.OAK_PLANKS) == 8,
                            "the network holds the 8 crafted planks; got " + storage.count(Items.OAK_PLANKS));
                    helper.assertTrue(storage.count(Items.OAK_LOG) == 0,
                            "both logs are consumed; got " + storage.count(Items.OAK_LOG));
                    helper.assertFalse(net.cc().craftBusy(), "the computer frees up after the craft");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craft_recursesAndReturnsSurplus(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 2);
                    net.cc().loadPattern(planksPattern(4));
                    net.cc().loadPattern(sticksPattern());
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(net.mainframe().submitNetworkCraft(
                        storageKey(Items.STICK), 4, false, "test") != null, "the recursive CRAFT is accepted"))
                .thenExecuteAfter(40, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    helper.assertTrue(storage.count(Items.STICK) == 4,
                            "the network holds the 4 crafted sticks; got " + storage.count(Items.STICK));
                    helper.assertTrue(storage.count(Items.OAK_LOG) == 1,
                            "only one log is needed; got " + storage.count(Items.OAK_LOG));
                    helper.assertTrue(storage.count(Items.OAK_PLANKS) == 2,
                            "the 2 surplus planks return to storage; got " + storage.count(Items.OAK_PLANKS));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craft_partialScalesDownAndReportsIt(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final AtomicReference<NetworkCraftOperation> opHolder = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 1);
                    net.cc().loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(net.mainframe().submitNetworkCraft(
                                    storageKey(Items.OAK_PLANKS), 16, false, "test") == null,
                            "an infeasible strict CRAFT is rejected");
                    opHolder.set(net.mainframe().submitNetworkCraft(storageKey(Items.OAK_PLANKS), 16, true, "test"));
                    helper.assertTrue(opHolder.get() != null, "the partial CRAFT is accepted");
                })
                .thenExecuteAfter(40, () -> {
                    final NetworkCraftOperation op = opHolder.get();
                    helper.assertTrue(op.isDone(), "the partial CRAFT settles");
                    helper.assertTrue(op.craftStatus() == OperationRecord.STATUS_PARTIAL,
                            "a scaled-down craft settles as COMPLETED_PARTIAL");
                    helper.assertTrue(op.delivered() == 4, "one log yields 4 planks; got " + op.delivered());
                    helper.assertTrue(net.storage(helper).count(Items.OAK_PLANKS) == 4, "the 4 planks are stored");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void craft_drawsIngredientsAcrossTwoServersAndReleasesLocks(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    TestWorldBuilder.mountDefaultServer(net.rack(), 1);
                    net.cc().loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    /*
                     * One log on each server, so an 8-plank craft (2 logs) must draw from both, the case
                     * where the lock-order server and the drained server once diverged and left reservations.
                     */
                    net.rack().getServerStorage(0).insert(Items.OAK_LOG, 1);
                    net.rack().getServerStorage(1).insert(Items.OAK_LOG, 1);
                })
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe().submitNetworkCraft(storageKey(Items.OAK_PLANKS), 8, false, "test") != null,
                        "the two-server craft is accepted"))
                .thenExecuteAfter(40, () -> {
                    final NetworkStorage storage = net.storage(helper);
                    helper.assertTrue(storage.count(Items.OAK_PLANKS) == 8,
                            "8 planks crafted from logs on two servers; got " + storage.count(Items.OAK_PLANKS));
                    helper.assertTrue(storage.count(Items.OAK_LOG) == 0,
                            "both logs are consumed; got " + storage.count(Items.OAK_LOG));
                    helper.assertTrue(net.mainframe().networkIndex().activeLockCount() == 0,
                            "the craft releases every ingredient reservation");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void craft_withoutPatternIsRejected(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> helper.assertTrue(
                        net.mainframe().submitNetworkCraft(storageKey(Items.PISTON), 1, true, "test") == null,
                        "no pattern on the network produces pistons"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void serverRack_faceReflectsInstalledServers(final GameTestHelper helper) {
        final BlockPos rack = new BlockPos(2, 2, 2);
        final Direction facing = Direction.NORTH;
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing));
        ((ServerRackBlock) ComputingModule.SERVER_RACK.get()).setPlacedBy(helper.getLevel(),
                helper.absolutePos(rack), helper.getBlockState(rack), null, ItemStack.EMPTY);
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            throw new IllegalStateException("no server rack at " + rack);
        }
        final BlockPos second = new BlockPos(ServerRackStructure.bayBlockPos(rack, facing, 1, 0));
        final BlockPos upper = new BlockPos(ServerRackStructure.bayBlockPos(rack, facing, 0, 1));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    /*
                     * Each visual bay block covers two rack-unit rows (bay b = rows 2b and 2b+1),
                     * so servers in U0, U2 and U4 light the controller, second column and upper bay.
                     */
                    rackBe.getServers().setStackInSlot(0, ServerStacks.defaultServer());
                    rackBe.getServers().setStackInSlot(2, ServerStacks.defaultServer());
                    rackBe.getServers().setStackInSlot(4, ServerStacks.defaultServer());
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(helper.getBlockState(rack).getValue(ServerRackBlock.BAYS) == 3,
                            "the controller bay lights up for its server");
                    helper.assertTrue(helper.getBlockState(second).getValue(ServerRackBlock.BAYS) == 3,
                            "the second column lights up for its server");
                    helper.assertTrue(helper.getBlockState(upper).getValue(ServerRackBlock.BAYS) == 3,
                            "the upper bay lights up for its server");
                    rackBe.getServers().setStackInSlot(2, ItemStack.EMPTY);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(helper.getBlockState(second).getValue(ServerRackBlock.BAYS) == 0,
                            "pulling a Server empties its bay on the face");
                    helper.assertTrue(helper.getBlockState(rack).getValue(ServerRackBlock.BAYS) == 3,
                            "the controller bay keeps its own server");
                })
                .thenSucceed();
    }
}
