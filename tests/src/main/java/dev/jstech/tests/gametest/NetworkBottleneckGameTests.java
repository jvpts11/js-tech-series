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
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.network.ConnectivityIndex;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The network knows the slowest cable between two points of it, across the cables, the routers and the
 * Mainframe that joins its runs, which is how fast data between them can go, and how fast an Operation moves. A run
 * longer than its cable reaches carries nothing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkBottleneckGameTests {

    private static final String ARENA = "empty";
    /** The large arena, room for a run longer than a cable reaches. */
    private static final String BENCH = "bench";
    private static final int SETTLE = 4;
    private static final DataLink THIN_COAX = new DataLink(DataLine.ACCESS, HardwareEra.VINTAGE);

    private static final BlockPos MAINFRAME = new BlockPos(3, 2, 3);
    private static final BlockPos EAST_HBW = new BlockPos(4, 2, 3);
    private static final BlockPos EAST_ROUTER = new BlockPos(5, 2, 3);
    private static final BlockPos WEST_HBW = new BlockPos(2, 2, 3);
    private static final DataLink ETHERNET = new DataLink(DataLine.ACCESS, HardwareEra.LEGACY);
    private static final DataLink HBW = new DataLink(DataLine.BACKBONE, HardwareEra.LEGACY);

    private NetworkBottleneckGameTests() {
    }

    @GameTest(template = ARENA)
    public static void slowestCableTo_isTheEthernetBetweenTwoComputersAcrossAMainframe(final GameTestHelper helper) {
        final Base base = wire(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    final ConnectivityIndex index = NetworkSystem.get(helper.getLevel()).connectivity();
                    final Optional<DataLink> between = base.east().slowestCableTo(helper.getLevel(), base.west());
                    helper.assertTrue(between.equals(Optional.of(ETHERNET)),
                            "two computers across the Mainframe are as fast as their Ethernet; got " + between);
                    final Optional<DataLink> fromMainframe =
                            base.mainframe().slowestCableTo(helper.getLevel(), base.east());
                    helper.assertTrue(fromMainframe.equals(Optional.of(ETHERNET)),
                            "and so is the Mainframe to a computer; got " + fromMainframe);
                    final Optional<DataLink> acrossTheCabinet =
                            index.slowestBetween(Set.of(encoded(helper, EAST_HBW)), Set.of(encoded(helper, WEST_HBW)));
                    helper.assertTrue(acrossTheCabinet.equals(Optional.of(HBW)),
                            "the Mainframe passes data between its HBW runs at HBW speed; got " + acrossTheCabinet);
                    helper.assertTrue(index.linkOf(encoded(helper, EAST_HBW)).equals(Optional.of(HBW)),
                            "a cable is known by its tier");
                    helper.assertTrue(index.linkOf(encoded(helper, EAST_ROUTER)).isEmpty(),
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
                    final Optional<DataLink> between = base.east().slowestCableTo(helper.getLevel(), base.west());
                    helper.assertTrue(between.isEmpty(),
                            "no data passes through a Mainframe that is off; got " + between);
                })
                .thenSucceed();
    }

    /** A run of thin coax as long as its cable reaches carries the network to the computer at its end. */
    @GameTest(template = BENCH)
    public static void range_aRunAsLongAsItsCableReachesCarriesTheNetwork(final GameTestHelper helper) {
        thinCoaxRun(helper, THIN_COAX.range(), true);
    }

    /** One cable longer, and the run carries nothing: the computer at its end is off the network. */
    @GameTest(template = BENCH)
    public static void range_aRunLongerThanItsCableReachesCarriesNothing(final GameTestHelper helper) {
        thinCoaxRun(helper, THIN_COAX.range() + 1, false);
    }

    /** An Operation moves no faster than the slowest cable between the Mainframe and the server it reads. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void pull_movesNoFasterThanTheSlowestCable(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.THICK_COAX_CABLE);
        final ServerRackBlockEntity rack = world.placeSeededRack(new BlockPos(3, 2, 2), Direction.EAST);
        final Map<Long, Long> perTick = new HashMap<>();
        final long[] total = {0L};
        final IDataSink hand = (key, amount, simulate) -> {
            if (!simulate) {
                perTick.merge(helper.getLevel().getGameTime(), amount, Long::sum);
                total[0] += amount;
            }
            return amount;
        };
        final long thickCoax = new DataLink(DataLine.BACKBONE, HardwareEra.VINTAGE).throughput();
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 3, () -> rack.getServerStorage(0).insert(Items.COBBLESTONE, 200))
                .thenExecuteAfter(SETTLE * 3, () -> helper.assertTrue(mainframe.networkOperations()
                        .pull(StorageKey.of(Items.COBBLESTONE), 160, hand, "test") != null, "the pull starts"))
                .thenWaitUntil(() -> helper.assertTrue(total[0] == 160L, "all 160 came; got " + total[0]))
                .thenExecute(() -> {
                    final long most = perTick.values().stream().mapToLong(Long::longValue).max().orElse(0L);
                    helper.assertTrue(most <= thickCoax, "no tick moved more than the thick coax carries ("
                            + thickCoax + "); the most was " + most);
                    helper.assertTrue(perTick.size() >= 160 / thickCoax, "so the 160 took "
                            + perTick.size() + " ticks");
                })
                .thenSucceed();
    }

    /*
     * A running Mainframe, an HBW cable to a Personal Router, {@code length} thin coax cables out of the router and a
     * running computer at their end; whether it is on the Mainframe's network is {@code reaches}.
     */
    private static void thinCoaxRun(final GameTestHelper helper, final int length, final boolean reaches) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        for (int i = 0; i < length; i++) {
            world.setBlock(new BlockPos(4 + i, 2, 2), ComputingModule.THIN_COAX_CABLE);
        }
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(new BlockPos(4 + length, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE * 4, () -> {
                    final boolean on = mainframe.networkUuid() != null
                            && mainframe.networkUuid().equals(pc.networkUuid());
                    helper.assertTrue(on == reaches, length + " cables of thin coax "
                            + (reaches ? "reach" : "do not reach") + "; the computer is "
                            + (on ? "on" : "off") + " the network");
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
