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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The network knows the slowest cable between two points of it, across the cables, the routers and the
 * Mainframe that joins its runs, which is how fast data between them can go.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkBottleneckGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private static final BlockPos MAINFRAME = new BlockPos(3, 2, 3);
    private static final BlockPos EAST_HBW = new BlockPos(4, 2, 3);
    private static final BlockPos EAST_ROUTER = new BlockPos(5, 2, 3);
    private static final BlockPos WEST_HBW = new BlockPos(2, 2, 3);

    private NetworkBottleneckGameTests() {
    }

    @GameTest(template = ARENA)
    public static void slowestCableTo_isTheEthernetBetweenTwoComputersAcrossAMainframe(final GameTestHelper helper) {
        final Base base = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final ConnectivityIndex index = NetworkSystem.get(helper.getLevel()).connectivity();
                    final Optional<DataTier> between = base.east().slowestCableTo(helper.getLevel(), base.west());
                    helper.assertTrue(between.equals(Optional.of(DataTier.T1_ETHERNET)),
                            "two computers across the Mainframe are as fast as their Ethernet; got " + between);
                    final Optional<DataTier> fromMainframe =
                            base.mainframe().slowestCableTo(helper.getLevel(), base.east());
                    helper.assertTrue(fromMainframe.equals(Optional.of(DataTier.T1_ETHERNET)),
                            "and so is the Mainframe to a computer; got " + fromMainframe);
                    final Optional<DataTier> acrossTheCabinet =
                            index.slowestBetween(Set.of(encoded(helper, EAST_HBW)), Set.of(encoded(helper, WEST_HBW)));
                    helper.assertTrue(acrossTheCabinet.equals(Optional.of(DataTier.T2_HBW)),
                            "the Mainframe passes data between its HBW runs at HBW speed; got " + acrossTheCabinet);
                    helper.assertTrue(index.tierOf(encoded(helper, EAST_HBW)).equals(Optional.of(DataTier.T2_HBW)),
                            "a cable is known by its tier");
                    helper.assertTrue(index.tierOf(encoded(helper, EAST_ROUTER)).isEmpty(),
                            "a router is no cable, so it slows nothing down");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void slowestCableTo_isNothingOnceTheMainframeBetweenIsSwitchedOff(final GameTestHelper helper) {
        final Base base = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> helper.assertTrue(
                        base.east().slowestCableTo(helper.getLevel(), base.west()).isPresent(),
                        "the two computers are joined while the Mainframe runs"))
                .thenExecute(() -> base.mainframe().togglePower())
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(!base.mainframe().isRunning(), "the Mainframe is off");
                    final Optional<DataTier> between = base.east().slowestCableTo(helper.getLevel(), base.west());
                    helper.assertTrue(between.isEmpty(),
                            "no data passes through a Mainframe that is off; got " + between);
                })
                .thenSucceed();
    }

    /* A Mainframe with an HBW run, a router and an Ethernet run to a personal computer on either side. */
    private static Base wire(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        world.setBlock(EAST_HBW, ComputingModule.HBW_CABLE);
        world.setBlock(EAST_ROUTER, ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(5, 2, 4), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity east = world.placeRunningPersonalComputer(new BlockPos(5, 2, 5));
        world.setBlock(WEST_HBW, ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(1, 2, 3), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(1, 2, 4), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity west = world.placeRunningPersonalComputer(new BlockPos(1, 2, 5));
        return new Base(mainframe, east, west);
    }

    /* The number the data grid knows the block at {@code relative} by: its wire, or the whole block of a router. */
    private static long encoded(final GameTestHelper helper, final BlockPos relative) {
        return TestCables.dataNumber(helper, relative).orElse(Long.MIN_VALUE);
    }

    /** The Mainframe in the middle and the computer on each side of it. */
    private record Base(MainframeBlockEntity mainframe, PersonalComputerBlockEntity east,
                        PersonalComputerBlockEntity west) {
    }
}
