/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.RouterBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The network's routers, optical routers and repeaters: a router of each era joins its access line to its backbone and
 * takes no newer cable, the optical router turns the fibre and takes nothing but fibre, a repeater renews a run's range
 * and carries each line on its own, and none of them splits the network.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkDeviceGameTests {

    private static final String ARENA = "empty";
    private static final String BENCH = "bench";
    private static final int SETTLE = 4;

    private static final List<BlockEntry<RouterBlock>> ROUTERS = List.of(ComputingModule.VINTAGE_ROUTER,
            ComputingModule.PERSONAL_ROUTER, ComputingModule.TRANSITION_ROUTER, ComputingModule.STANDARD_ROUTER,
            ComputingModule.ADVANCED_ROUTER);
    private static final List<CableEntry> ACCESS = List.of(ComputingModule.THIN_COAX_CABLE,
            ComputingModule.ETHERNET_CABLE, ComputingModule.CAT5E_CABLE, ComputingModule.GIGABIT_CABLE,
            ComputingModule.CAT6A_CABLE);
    private static final List<CableEntry> BACKBONE = List.of(ComputingModule.THICK_COAX_CABLE,
            ComputingModule.HBW_CABLE, ComputingModule.CX4_CABLE, ComputingModule.FIBRE_CABLE,
            ComputingModule.OM5_CABLE);

    private NetworkDeviceGameTests() {
    }

    /** Each era's router joins its own access line to its own backbone. */
    @GameTest(template = ARENA)
    public static void router_joinsItsErasAccessToItsBackbone(final GameTestHelper helper) {
        for (int era = 0; era < ROUTERS.size(); era++) {
            final int z = 1 + era;
            TestCables.lay(helper, new BlockPos(1, 2, z), ACCESS.get(era));
            helper.setBlock(new BlockPos(2, 2, z), ROUTERS.get(era).get());
            TestCables.lay(helper, new BlockPos(3, 2, z), BACKBONE.get(era));
        }
        helper.runAfterDelay(SETTLE, () -> {
            for (int era = 0; era < ROUTERS.size(); era++) {
                final int z = 1 + era;
                helper.assertTrue(TestCables.joined(helper, new BlockPos(1, 2, z), new BlockPos(3, 2, z)),
                        ROUTERS.get(era).getId() + " joins its access line to its backbone");
            }
            helper.succeed();
        });
    }

    /** A router takes no newer era's cable: the Vintage router leaves the Ethernet out. */
    @GameTest(template = ARENA)
    public static void router_takesNoNewerErasCable(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.ETHERNET_CABLE);
        helper.setBlock(new BlockPos(2, 2, 2), ComputingModule.VINTAGE_ROUTER.get());
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.THICK_COAX_CABLE);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertFalse(TestCables.joined(helper, new BlockPos(1, 2, 2), new BlockPos(3, 2, 2)),
                    "the Vintage router takes no Ethernet");
            helper.succeed();
        });
    }

    /** Two eras of a line meet at the router of the newer one, which takes both. */
    @GameTest(template = ARENA)
    public static void router_joinsTwoErasOfALine(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.THIN_COAX_CABLE);
        helper.setBlock(new BlockPos(2, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.ETHERNET_CABLE);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(TestCables.joined(helper, new BlockPos(1, 2, 2), new BlockPos(3, 2, 2)),
                    "thin coax and Ethernet meet at the Legacy router");
            helper.succeed();
        });
    }

    /** The fibre does not turn on its own; an optical router at the corner turns it, and branches it. */
    @GameTest(template = ARENA)
    public static void opticalRouter_turnsAndBranchesTheFibre(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.FIBRE_CABLE);
        helper.setBlock(new BlockPos(2, 2, 2), ComputingModule.OPTICAL_ROUTER.get());
        TestCables.lay(helper, new BlockPos(2, 2, 3), ComputingModule.FIBRE_CABLE);
        TestCables.lay(helper, new BlockPos(2, 2, 1), ComputingModule.FIBRE_CABLE);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(TestCables.joined(helper, new BlockPos(1, 2, 2), new BlockPos(2, 2, 3)),
                    "the optical router turns the fibre");
            helper.assertTrue(TestCables.joined(helper, new BlockPos(2, 2, 1), new BlockPos(2, 2, 3)),
                    "and branches it");
            helper.succeed();
        });
    }

    /** The optical router is the fibre's: the copper backbone and the access line stay out of it. */
    @GameTest(template = ARENA)
    public static void opticalRouter_takesOnlyFibre(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(new BlockPos(2, 2, 2), ComputingModule.ADVANCED_OPTICAL_ROUTER.get());
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.OM5_CABLE);
        TestCables.lay(helper, new BlockPos(2, 2, 3), ComputingModule.FIBRE_CABLE);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertFalse(TestCables.joined(helper, new BlockPos(1, 2, 2), new BlockPos(3, 2, 2)),
                    "no HBW in an optical router");
            helper.assertTrue(TestCables.joined(helper, new BlockPos(3, 2, 2), new BlockPos(2, 2, 3)),
                    "the Advanced optical router takes the OM5 and the Standard's fibre");
            helper.succeed();
        });
    }

    /** A repeater carries each line on its own and never joins one line to another. */
    @GameTest(template = ARENA)
    public static void repeater_carriesEachLineOnItsOwn(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(new BlockPos(2, 2, 2), ComputingModule.LEGACY_REPEATER.get());
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.ETHERNET_CABLE);
        TestCables.lay(helper, new BlockPos(2, 2, 3), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(2, 2, 1), ComputingModule.THIN_COAX_CABLE);
        helper.runAfterDelay(SETTLE, () -> {
            final BlockPos west = new BlockPos(1, 2, 2);
            final BlockPos east = new BlockPos(3, 2, 2);
            helper.assertTrue(TestCables.wiresJoined(helper, west, ComputingModule.ETHERNET_CABLE, east,
                    ComputingModule.ETHERNET_CABLE), "the Ethernet runs on through the repeater");
            helper.assertTrue(TestCables.wiresJoined(helper, new BlockPos(2, 2, 1), ComputingModule.THIN_COAX_CABLE,
                    east, ComputingModule.ETHERNET_CABLE), "and meets the thin coax there, an earlier era of its line");
            helper.assertTrue(TestCables.wiresJoined(helper, west, ComputingModule.HBW_CABLE, new BlockPos(2, 2, 3),
                    ComputingModule.HBW_CABLE), "the HBW runs on through it too");
            helper.assertFalse(TestCables.wiresJoined(helper, east, ComputingModule.ETHERNET_CABLE,
                    new BlockPos(2, 2, 3), ComputingModule.HBW_CABLE), "and the two lines do not meet in it");
            helper.succeed();
        });
    }

    /**
     * A repeater renews the range: two runs of thin coax, each within its reach, carry the network across. The second
     * run turns at the repeater so the whole line stays inside the arena.
     */
    @GameTest(template = BENCH, timeoutTicks = 400)
    public static void repeater_renewsTheRange(final GameTestHelper helper) {
        final int run = new DataLink(DataLine.ACCESS, HardwareEra.VINTAGE).range() - 2;
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        for (int i = 0; i < run; i++) {
            world.setBlock(new BlockPos(4 + i, 2, 2), ComputingModule.THIN_COAX_CABLE);
        }
        final BlockPos repeater = new BlockPos(4 + run, 2, 2);
        world.setBlock(repeater, ComputingModule.VINTAGE_REPEATER.get());
        for (int i = 1; i <= run; i++) {
            world.setBlock(repeater.south(i), ComputingModule.THIN_COAX_CABLE);
        }
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(repeater.south(run + 1));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                                && mainframe.networkUuid().equals(pc.networkUuid()),
                        (run * 2) + " cables of thin coax with a repeater halfway reach; one network all along"))
                .thenExecute(() -> TestCables.lay(helper, repeater, ComputingModule.THIN_COAX_CABLE))
                .thenWaitUntil(() -> helper.assertTrue(pc.networkUuid() == null,
                        "with a cable in the repeater's place the run is too long, and the computer is off"))
                .thenSucceed();
    }

    /** A router taken away leaves the runs it joined apart. */
    @GameTest(template = ARENA)
    public static void router_takenAwaySeparatesTheRuns(final GameTestHelper helper) {
        TestCables.lay(helper, new BlockPos(1, 2, 2), ComputingModule.GIGABIT_CABLE);
        helper.setBlock(new BlockPos(2, 2, 2), ComputingModule.STANDARD_ROUTER.get());
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.FIBRE_CABLE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(TestCables.joined(helper, new BlockPos(1, 2, 2),
                        new BlockPos(3, 2, 2)), "joined through the Standard router"))
                .thenExecute(() -> helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR))
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(TestCables.joined(helper, new BlockPos(1, 2, 2),
                        new BlockPos(3, 2, 2)), "and apart without it"))
                .thenSucceed();
    }
}
