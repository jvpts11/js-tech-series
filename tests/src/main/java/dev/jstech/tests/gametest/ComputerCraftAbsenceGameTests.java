/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * CC: Tweaked is a soft dependency: the mods run unchanged without it. The build runs the GameTests with the mod and
 * without it, telling them which through a system property, so a run that silently lost or gained the mod fails here,
 * and the Network Gateway, the one block built for ComputerCraft, is shown to link and keep its log either way.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerCraftAbsenceGameTests {

    private ComputerCraftAbsenceGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 8;
    private static final BlockPos COMPUTER = new BlockPos(1, 2, 2);
    private static final BlockPos GATEWAY = new BlockPos(2, 2, 2);

    /** What the build says about this run: {@code absent} without CC: Tweaked, {@code present} otherwise. */
    private static boolean expectedLoaded() {
        return !"absent".equals(System.getProperty("jsc.gametests.computercraft", "present"));
    }

    @GameTest(template = ARENA)
    public static void integration_isLoadedExactlyWhenTheBuildSaysSo(final GameTestHelper helper) {
        final boolean loaded = ComputerCraftIntegration.isLoaded();
        helper.assertTrue(loaded == expectedLoaded(),
                "CC: Tweaked loaded=" + loaded + " but the build expected loaded=" + expectedLoaded());
        helper.assertTrue(loaded != ComputerCraftIntegration.installedVersion().isEmpty(),
                "a version is reported exactly when the mod is there; got '"
                        + ComputerCraftIntegration.installedVersion() + "'");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gateway_linksAndLogsWhetherOrNotComputerCraftIsThere(final GameTestHelper helper) {
        TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
        // Facing east, the back socket touches the computer to the west.
        helper.setBlock(GATEWAY, ComputingModule.NETWORK_GATEWAY.get().defaultBlockState()
                .setValue(NetworkGatewayBlock.FACING, Direction.EAST));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    if (!(helper.getBlockEntity(GATEWAY) instanceof NetworkGatewayBlockEntity gateway)) {
                        helper.fail("no Network Gateway at " + GATEWAY);
                        return;
                    }
                    final boolean loaded = ComputerCraftIntegration.isLoaded();
                    helper.assertTrue(gateway.online(), "the Gateway links to its computer; loaded=" + loaded);
                    helper.assertTrue(gateway.log().entries().stream().anyMatch(e -> e.what().english().equals("link")),
                            "and logs the link; loaded=" + loaded);
                    helper.assertTrue((gateway.bridge() != null) == loaded,
                            "it has a bridge to ComputerCraft exactly when the mod is there; loaded=" + loaded);
                    helper.assertTrue(loaded || !gateway.ccOnline(),
                            "without the mod no ComputerCraft computer is ever online");
                })
                .thenSucceed();
    }
}
