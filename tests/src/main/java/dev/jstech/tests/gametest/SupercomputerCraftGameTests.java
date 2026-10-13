/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftingFixtures.Network;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.CraftingFixtures.buildCraftingNetwork;
import static dev.jstech.tests.testkit.CraftingFixtures.placeCluster;
import static dev.jstech.tests.testkit.CraftingFixtures.placeSecondCraftingComputer;
import static dev.jstech.tests.testkit.CraftingFixtures.planksPattern;
import static dev.jstech.tests.testkit.CraftingFixtures.storageKey;

/**
 * GameTests for the Supercomputer's parallel crafting: the survey of Phi slots and budget, the unlock of
 * parallel crafts, and one craft fanned out across computers.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SupercomputerCraftGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private SupercomputerCraftGameTests() {
    }

    @GameTest(template = ARENA)
    public static void cluster_surveyAssignsSlotsAndBudget(final GameTestHelper helper) {
        final BlockPos hub = new BlockPos(2, 2, 2);
        // Three nodes in a row east of the interface: slots 1, 2, 3 by discovery order.
        placeCluster(helper, hub, 3);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    if (!(helper.getBlockEntity(hub) instanceof HbwInterfaceBlockEntity be)) {
                        throw new IllegalStateException("no hbw interface");
                    }
                    // 5100s fit slots 1-2 (8 + 16); the third 5100 is under-rated for slot 3.
                    helper.assertTrue(be.parallelCrafts() == 24, "two rated slots give 24; got "
                            + be.parallelCrafts());
                    final var slots = be.clusterSlots();
                    helper.assertTrue(slots.size() == 3, "three nodes surveyed");
                    helper.assertTrue(slots.get(2).code() == HbwInterfaceBlockEntity.SLOT_UNDER_RATED,
                            "a 5100 in slot 3 is flagged under-rated, never crashes");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void supercomputer_unlocksParallelCrafting(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final AtomicReference<NetworkCraftOperation> first = new AtomicReference<>();
        final AtomicReference<NetworkCraftOperation> second = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 8000);
                    net.cc().loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // No cluster yet: two long crafts, and the second must wait its turn.
                    first.set(net.mainframe().submitNetworkCraft(storageKey(Items.OAK_PLANKS), 12000, false, "test"));
                    second.set(net.mainframe().submitNetworkCraft(storageKey(Items.OAK_PLANKS), 12000, false, "test"));
                    helper.assertTrue(first.get() != null && second.get() != null, "both CRAFTs are accepted");
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertFalse(first.get().isWaiting(), "the first craft claims the computer");
                    helper.assertTrue(second.get().isWaiting(), "without a Supercomputer the second craft waits");
                    first.get().abandon();
                    second.get().abandon();
                })
                .thenExecuteAfter(SETTLE, () -> placeCluster(helper, new BlockPos(2, 2, 3), 1))
                .thenExecuteAfter(SETTLE + 2, () -> {
                    /*
                     * Smaller than phase one: the first run consumed some logs before being
                     * abandoned, and BOTH locks must still be fully coverable at once.
                     */
                    first.set(net.mainframe().submitNetworkCraft(storageKey(Items.OAK_PLANKS), 8000, false, "test"));
                    second.set(net.mainframe().submitNetworkCraft(storageKey(Items.OAK_PLANKS), 8000, false, "test"));
                    helper.assertTrue(first.get() != null && second.get() != null,
                            "both CRAFTs are accepted with the Supercomputer");
                })
                .thenExecuteAfter(6, () -> {
                    helper.assertFalse(first.get().isWaiting(), "the first craft runs under the cluster");
                    helper.assertFalse(second.get().isWaiting(), "the cluster runs both crafts in parallel");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void supercomputer_fansOneCraftAcrossComputers(final GameTestHelper helper) {
        final Network net = buildCraftingNetwork(helper);
        final BlockPos hub = new BlockPos(2, 2, 3);
        final BlockPos cc2Pos = new BlockPos(4, 2, 1);
        final AtomicReference<NetworkCraftOperation> opHolder = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    net.seed(Items.OAK_LOG, 8000);
                    net.cc().loadPattern(planksPattern(4));
                    placeSecondCraftingComputer(helper, cc2Pos).loadPattern(planksPattern(4));
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(net.mainframe().craftingComputerPositions().size() >= 2,
                            "both crafting computers join the network; got "
                                    + net.mainframe().craftingComputerPositions().size());
                    placeCluster(helper, hub, 1);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    if (!(helper.getBlockEntity(hub) instanceof HbwInterfaceBlockEntity sc) || !sc.clusterOnline()
                            || sc.parallelCrafts() < 2) {
                        helper.fail("the supercomputer cluster is not online with two slots or more");
                    }
                    helper.assertTrue(net.mainframe().supercomputerPositions().size() >= 1,
                            "the supercomputer is registered on the network");
                    opHolder.set(net.mainframe().submitNetworkCraft(storageKey(Items.OAK_PLANKS), 24000, false,
                            "test"));
                    helper.assertTrue(opHolder.get() != null, "the large craft is accepted");
                })
                .thenExecuteAfter(4, () -> {
                    /*
                     * The single request fanned out: it claimed one computer per supercomputer slot. The executor
                     * list persists after the craft settles, so this is robust to the craft finishing fast.
                     */
                    helper.assertTrue(opHolder.get().executorCount() >= 2,
                            "one large craft fans out across both crafting computers; executors="
                                    + opHolder.get().executorCount());
                })
                .thenSucceed();
    }
}
