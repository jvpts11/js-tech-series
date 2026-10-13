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
import dev.jstech.computers.datacenter.LoadBalanceMode;
import dev.jstech.computers.datacenter.LoadBalancer;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.seedServer;

/**
 * GameTests for the round-robin load balancer spreading stores and stacks across servers.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LoadBalancerGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private LoadBalancerGameTests() {
    }

    @GameTest(template = ARENA)
    public static void loadBalancer_roundRobinSpreadsAcrossServers(final GameTestHelper helper) {
        final BlockPos rackA = new BlockPos(2, 2, 2);
        final BlockPos rackB = new BlockPos(4, 2, 2);
        helper.setBlock(rackA, ComputingModule.SERVER_RACK.get());
        helper.setBlock(rackB, ComputingModule.SERVER_RACK.get());
        seedServer(helper, rackA);
        seedServer(helper, rackB);
        if (!(helper.getBlockEntity(rackA) instanceof ServerRackBlockEntity a)
                || !(helper.getBlockEntity(rackB) instanceof ServerRackBlockEntity b)) {
            helper.fail("no server racks");
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerStore sa = a.getServerStorage(0);
                    final ServerStore sb = b.getServerStorage(0);
                    final long stored = LoadBalancer.insert(java.util.List.of(sa, sb),
                            StorageKey.of(Items.COBBLESTONE), 128L, LoadBalanceMode.ROUND_ROBIN);
                    helper.assertTrue(stored == 128L, "round-robin should store all 128; got " + stored);
                    helper.assertTrue(sa.count(Items.COBBLESTONE) == 64L && sb.count(Items.COBBLESTONE) == 64L,
                            "round-robin should spread evenly (64/64); got "
                                    + sa.count(Items.COBBLESTONE) + "/" + sb.count(Items.COBBLESTONE));
                })
                .thenSucceed();
    }

    /**
     * The write a player actually makes is one stack, and it must be shared out, since a batch of a whole stack
     * put all 64 on the first server, so the setting looked dead however it was set. A run of single items
     * has to move down the row too, which is what the rotation is for.
     */
    @GameTest(template = ARENA)
    public static void loadBalancer_roundRobinSplitsOneStackAndTakesTurns(final GameTestHelper helper) {
        final BlockPos rackA = new BlockPos(2, 2, 2);
        final BlockPos rackB = new BlockPos(4, 2, 2);
        helper.setBlock(rackA, ComputingModule.SERVER_RACK.get());
        helper.setBlock(rackB, ComputingModule.SERVER_RACK.get());
        seedServer(helper, rackA);
        seedServer(helper, rackB);
        if (!(helper.getBlockEntity(rackA) instanceof ServerRackBlockEntity a)
                || !(helper.getBlockEntity(rackB) instanceof ServerRackBlockEntity b)) {
            helper.fail("no server racks");
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerStore sa = a.getServerStorage(0);
                    final ServerStore sb = b.getServerStorage(0);
                    final StorageKey key = StorageKey.of(Items.COBBLESTONE);
                    helper.assertTrue(LoadBalancer.insert(java.util.List.of(sa, sb), key, 64L,
                            LoadBalanceMode.ROUND_ROBIN) == 64L, "one stack is stored whole");
                    helper.assertTrue(sa.count(Items.COBBLESTONE) == 32L && sb.count(Items.COBBLESTONE) == 32L,
                            "one stack splits across the servers (32/32); got "
                                    + sa.count(Items.COBBLESTONE) + "/" + sb.count(Items.COBBLESTONE));
                    // Single items, one write each: the rotation must land them on alternate servers.
                    final StorageKey dirt = StorageKey.of(Items.DIRT);
                    for (int i = 0; i < 4; i++) {
                        LoadBalancer.insert(java.util.List.of(sa, sb), dirt, 1L, LoadBalanceMode.ROUND_ROBIN, i);
                    }
                    helper.assertTrue(sa.count(Items.DIRT) == 2L && sb.count(Items.DIRT) == 2L,
                            "four single deposits alternate (2/2); got "
                                    + sa.count(Items.DIRT) + "/" + sb.count(Items.DIRT));
                    // Manual is the one mode that deliberately fills in order, top server first.
                    final StorageKey sand = StorageKey.of(Items.SAND);
                    LoadBalancer.insert(java.util.List.of(sa, sb), sand, 64L, LoadBalanceMode.MANUAL);
                    helper.assertTrue(sa.count(Items.SAND) == 64L && sb.count(Items.SAND) == 0L,
                            "manual fills the first server; got " + sa.count(Items.SAND) + "/" + sb.count(Items.SAND));
                })
                .thenSucceed();
    }
}
