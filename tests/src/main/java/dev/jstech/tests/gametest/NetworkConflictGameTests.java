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
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for conflict detection on a data network: two Mainframes on one segment both conflict, and the
 * conflict clears itself when one of them is destroyed or powered off.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkConflictGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private NetworkConflictGameTests() {
    }

    @GameTest(template = ARENA)
    public static void standaloneMainframe_ownsNetworkWithoutConflict(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final MainframeBlockEntity beA = placeRunningMainframe(helper, a);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(beA.isRunning(), "mainframe should be running");
                    helper.assertTrue(beA.networkUuid() != null, "standalone mainframe should own a network");
                    helper.assertFalse(beA.hasNetworkConflict(), "standalone mainframe has no conflict");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void twoMainframesOnOneSegment_bothConflict(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final BlockPos cable = new BlockPos(3, 2, 2);
        final BlockPos b = new BlockPos(4, 2, 2);
        final MainframeBlockEntity beA = placeRunningMainframe(helper, a);
        TestCables.lay(helper, cable, ComputingModule.HBW_CABLE);
        final MainframeBlockEntity beB = placeRunningMainframe(helper, b);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(beA.hasNetworkConflict(), "A should detect the conflict");
                    helper.assertTrue(beB.hasNetworkConflict(), "B should detect the conflict");
                    helper.assertTrue(beA.networkUuid() == null, "A in conflict owns no network");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void conflictClears_whenSecondMainframeDestroyed(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final BlockPos cable = new BlockPos(3, 2, 2);
        final BlockPos b = new BlockPos(4, 2, 2);
        final MainframeBlockEntity beA = placeRunningMainframe(helper, a);
        TestCables.lay(helper, cable, ComputingModule.HBW_CABLE);
        placeRunningMainframe(helper, b);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () ->
                        helper.assertTrue(beA.hasNetworkConflict(), "A should be in conflict first"))
                .thenExecute(() -> helper.setBlock(b, Blocks.AIR)) // destroy B
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(beA.hasNetworkConflict(), "A conflict must clear after B is destroyed");
                    helper.assertTrue(beA.networkUuid() != null, "A must reclaim its network");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void conflictClears_whenSecondMainframePoweredOff(final GameTestHelper helper) {
        final BlockPos a = new BlockPos(2, 2, 2);
        final BlockPos cable = new BlockPos(3, 2, 2);
        final BlockPos b = new BlockPos(4, 2, 2);
        final MainframeBlockEntity beA = placeRunningMainframe(helper, a);
        TestCables.lay(helper, cable, ComputingModule.HBW_CABLE);
        final MainframeBlockEntity beB = placeRunningMainframe(helper, b);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () ->
                        helper.assertTrue(beA.hasNetworkConflict(), "A should be in conflict first"))
                .thenExecute(beB::togglePower) // power off B
                .thenExecuteAfter(SETTLE, () ->
                        helper.assertFalse(beA.hasNetworkConflict(), "A conflict must clear after B powers off"))
                .thenSucceed();
    }
}
