/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.engine.CraftRequest;
import dev.jstech.computers.engine.EngineVerb;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.engine.NetworkOperationsService;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestEngines;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The network's Network Operations Engines: the Midsoft IQL Server every Mainframe ships with, in the version of its
 * age; the door every request comes in by; and the three levels of the contract, the network's (always there),
 * the engine's (every engine) and the extras (only the engine that offers them).
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkEngineGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 14;
    private static final int ROWS = 64;
    private static final ResourceLocation MIDSOFT = NetworkEngines.MIDSOFT_IQL_SERVER.program();

    /** Each age's Mainframe and the version of the Midsoft IQL Server it ships with. */
    private static final List<Shipped> SHIPPED = List.of(
            new Shipped(ComputingModule.VINTAGE_MAINFRAME, "4.2"),
            new Shipped(ComputingModule.LEGACY_MAINFRAME, "2000"),
            new Shipped(ComputingModule.TRANSITION_MAINFRAME, "2008"),
            new Shipped(ComputingModule.MAINFRAME, "2012"),
            new Shipped(ComputingModule.ADVANCED_MAINFRAME, "2022"));

    private NetworkEngineGameTests() {
    }

    @GameTest(template = ARENA)
    public static void midsoft_shipsOnEveryNewMainframeInTheVersionOfItsAge(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        for (final Shipped shipped : SHIPPED) {
            helper.setBlock(at, shipped.block().get());
            if (!(helper.getBlockEntity(at) instanceof MainframeBlockEntity mainframe)) {
                throw new IllegalStateException("no Mainframe at " + at);
            }
            final HardwareEra era = mainframe.mainframeEra();
            helper.assertValueEqual(mainframe.installedEngines(), Map.of(MIDSOFT, shipped.version()),
                    "a new " + era + " Mainframe ships with the Midsoft IQL Server of its age");
            helper.assertTrue(MIDSOFT.equals(mainframe.activeEngine()) && mainframe.engineRunning(),
                    "chosen and started, so a new " + era + " network works the moment it is built");
            helper.setBlock(at, Blocks.AIR);
        }
        helper.succeed();
    }

    /** The network's level is there with or without an engine, and so are the verbs that only move things. */
    @GameTest(template = ARENA)
    public static void networkLevel_movesWhatIsThereWithNoEngine(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        final long[] taken = {0L};
        final IDataSink hand = (key, amount, simulate) -> {
            if (!simulate) {
                taken[0] += amount;
            }
            return amount;
        };
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> rack(helper).getServerStorage(0).insert(Items.COBBLESTONE, 200))
                // The network's index sees the stock before anything is taken from it.
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.uninstallEngine(MIDSOFT);
                    final NetworkOperationsService network = mainframe.networkOperations();
                    helper.assertTrue(network.engine() == null,
                            "taking the only engine off leaves the network without");
                    for (final EngineVerb verb : EngineVerb.values()) {
                        helper.assertTrue(network.serves(verb) == verb.transfer(),
                                verb + (verb.transfer() ? " still works with no engine" : " needs an engine"));
                    }
                    helper.assertTrue(network.pull(StorageKey.of(Items.COBBLESTONE), 50, hand, "test") != null,
                            "a pull goes straight through the Operations core");
                })
                .thenWaitUntil(() -> helper.assertTrue(taken[0] == 50L, "the 50 arrived; got " + taken[0]))
                .thenSucceed();
    }

    /** What only an engine plans is refused with no engine running, and answered by any engine that runs. */
    @GameTest(template = ARENA)
    public static void engineLevel_refusedWithNoEngineAndAnsweredByEvery(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final NetworkOperationsService network = mainframe.networkOperations();
                    final ServerCliComputer machine = new ServerCliComputer(mainframe, helper.getLevel());
                    helper.assertTrue(query(mainframe, machine, "QUERY items").ok(),
                            "the Midsoft IQL Server answers the language");
                    helper.assertTrue(network.planner() != null && network.refusal(EngineVerb.CRAFT) == null,
                            "and plans crafts");

                    mainframe.setEngineRunning(false);
                    final IqlEngine.Outcome refused = query(mainframe, machine, "QUERY items");
                    helper.assertTrue(!refused.ok() && refused.message().equals(
                                    NetworkOperationsService.UNAVAILABLE.english()),
                            "a stopped engine leaves the language refused as unavailable; got " + refused.message());
                    helper.assertTrue(network.planner() == null
                                    && network.craft(CraftRequest.of(StorageKey.of(Items.STICK), 1, true, "test",
                                            null)) == null,
                            "and nothing planned or crafted");

                    mainframe.installEngine(TestEngines.PLAIN);
                    mainframe.activateEngine(TestEngines.PLAIN);
                    helper.assertTrue(query(mainframe, machine, "QUERY items").ok() && network.planner() != null,
                            "another mod's engine answers the same level");
                })
                .thenSucceed();
    }

    /** Views and procedures are an extra: only the engine that offers them keeps them. */
    @GameTest(template = ARENA)
    public static void extras_onlyWhereTheEngineOffersThem(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer machine = new ServerCliComputer(mainframe, helper.getLevel());
                    mainframe.installEngine(TestEngines.PLAIN);
                    mainframe.activateEngine(TestEngines.PLAIN);
                    helper.assertFalse(query(mainframe, machine, "CREATE VIEW stock AS QUERY items").ok(),
                            "an engine without views keeps none");
                    mainframe.activateEngine(MIDSOFT);
                    helper.assertTrue(query(mainframe, machine, "CREATE VIEW stock AS QUERY items").ok(),
                            "the Midsoft IQL Server keeps them");
                })
                .thenSucceed();
    }

    private static IqlEngine.Outcome query(final MainframeBlockEntity mainframe, final ServerCliComputer machine,
                                           final String statement) {
        return mainframe.networkOperations().query(IqlEngine.viewOf(machine), statement, ROWS);
    }

    /** A running Mainframe, a cable and a rack with one server, the rack behind the cable. */
    private static MainframeBlockEntity storageNetwork(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe =
                TestWorldBuilder.forGameTest(helper).placeRunningMainframe(new BlockPos(1, 2, 2));
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestWorldBuilder.forGameTest(helper).placeSeededRack(new BlockPos(3, 2, 2), Direction.EAST);
        return mainframe;
    }

    private static ServerRackBlockEntity rack(final GameTestHelper helper) {
        if (helper.getBlockEntity(new BlockPos(3, 2, 2)) instanceof ServerRackBlockEntity rack) {
            return rack;
        }
        throw new IllegalStateException("no rack at (3,2,2)");
    }

    /** An age's Mainframe and the version it ships with. */
    private record Shipped(BlockEntry<MainframeBlock> block, String version) {
    }
}
