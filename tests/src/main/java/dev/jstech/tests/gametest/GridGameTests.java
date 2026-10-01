/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A dimension's grids: one of each kind, the data grid being the data network's own, and the data cables laid in the
 * world standing in it as the cables of their tier.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class GridGameTests {

    private static final String ARENA = "empty";

    private GridGameTests() {
    }

    @GameTest(template = ARENA)
    public static void levelGrids_holdOneGridOfEachKind(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Grid power = CoreGrids.of(level, GridKind.POWER);

        helper.assertTrue(power == CoreGrids.of(level, GridKind.POWER), "the same power grid each time");
        helper.assertTrue(power != CoreGrids.of(level, GridKind.FLUID), "a grid of each kind");
        helper.assertTrue(CoreGrids.of(level, GridKind.DATA) == NetworkSystem.get(level).connectivity().grid(),
                "the data grid is the network's own");
        helper.assertTrue(GridKind.DATA.carriesNetwork() && !GridKind.POWER.carriesNetwork(),
                "only data carries a network's identity");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dataCables_standInTheDataGridAsTheirTier(final GameTestHelper helper) {
        final BlockPos first = new BlockPos(1, 2, 1);
        TestCables.lay(helper, first, ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, first.east(), ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, first.east(2), ComputingModule.HBW_CABLE);

        helper.succeedWhen(() -> {
            final Grid data = CoreGrids.of(helper.getLevel(), GridKind.DATA);
            final long a = TestCables.dataNumber(helper, first).orElse(-1L);
            final long b = TestCables.dataNumber(helper, first.east()).orElse(-1L);
            final long c = TestCables.dataNumber(helper, first.east(2)).orElse(-1L);
            helper.assertTrue(data.contains(a) && data.contains(b) && data.contains(c), "every cable in the grid");
            helper.assertTrue(data.connected(a, b), "two Ethernet cables join");
            helper.assertTrue(!data.connected(b, c), "Ethernet and HBW stay apart");
            helper.assertTrue(Objects.equals(DataTier.T1_ETHERNET.line(), data.memberOf(a).line()),
                    "a cable stands as its tier's line");
            helper.assertTrue(data.runLength(a) == 2, "the two Ethernet cables are one run");
        });
    }
}
