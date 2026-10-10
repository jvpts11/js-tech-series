/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.sameNetwork;

/**
 * GameTests for the Personal Router bridging Ethernet and HBW cable.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PersonalRouterGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private PersonalRouterGameTests() {
    }

    @GameTest(template = ARENA)
    public static void personalRouter_bridgesEthernetAndHbw(final GameTestHelper helper) {
        final BlockPos eth = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos hbw = new BlockPos(4, 2, 2);
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(sameNetwork(helper, eth, hbw),
                        "Ethernet and HBW should share one network through the router"))
                .thenExecute(() -> helper.setBlock(router, Blocks.AIR)) // remove the bridge
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(sameNetwork(helper, eth, hbw),
                        "removing the router must split Ethernet from HBW"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void ethernetAndHbw_doNotJoinDirectly(final GameTestHelper helper) {
        final BlockPos eth = new BlockPos(2, 2, 2);
        final BlockPos hbw = new BlockPos(3, 2, 2);
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(sameNetwork(helper, eth, hbw),
                        "different cable tiers must not join without a router"))
                .thenSucceed();
    }
}
