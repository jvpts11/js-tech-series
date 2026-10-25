/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.IsmsSchemaPayload;
import dev.jstech.computers.operation.payload.iql.IqlPayloads;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.core.jstechseries.network.NetworkCategory;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.network.NetworkTables;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestNetworkMembers;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for the network reading other mods and the world: a QUERY of another mod's state table by its id or its
 * path, the error of a name no table has or two tables share, a machine whose chunk is not loaded read without loading
 * it, and the world events the network sees, in its events table, in its event log and in its jobs' triggers.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkWorldReadGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 14;
    private static final int ROWS = 64;
    private static final long STOCK = 10L;
    /** How far ahead the watched storm is planned: it is warned of 40 ticks before, and lasts 40. */
    private static final int STORM_IN = 100;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos RACK = new BlockPos(3, 2, 2);
    private static final BlockPos HOT = new BlockPos(2, 2, 1);
    private static final BlockPos COLD = new BlockPos(2, 2, 3);

    private NetworkWorldReadGameTests() {
    }

    /** Another mod's table is read by its id or by its path, a row for each machine with where it stands. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void query_readsAnotherModsTableByItsIdOrItsPath(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.setBlock(HOT, TestNetworkMembers.REACTOR.get());
        helper.setBlock(COLD, TestNetworkMembers.REACTOR.get());
        reactor(helper, HOT).reads(350.0, 1200.0, true, "Alpha");
        reactor(helper, COLD).reads(20.0, 0.0, false, "Beta");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && NetworkTables.rows(helper.getLevel(), mainframe.networkUuid(),
                        TestNetworkMembers.REACTORS).size() == 2, "both reactors join the network: " + joined(helper,
                        mainframe)))
                .thenExecute(() -> {
                    final IqlEngine.Outcome hot = run(helper, mainframe,
                            "QUERY jstests:test_reactors WHERE temperature > 90");
                    helper.assertTrue(hot.ok(), "the query runs: " + hot.message());
                    final IqlTable table = hot.table();
                    helper.assertTrue(table.rows().size() == 1 && table.cell(0, "name").english().equals("Alpha"),
                            "only the hot reactor: " + table.rows());
                    helper.assertTrue(table.columns().contains("temperature (°C)") && table.columns().contains("at"),
                            "its columns, each with its unit, and where it stands: " + table.columns());
                    helper.assertTrue(table.cell(0, "at").english().equals(helper.absolutePos(HOT).toShortString()),
                            "where the reactor stands: " + table.cell(0, "at").english());
                    helper.assertTrue(table.cell(0, "running").english().equals("yes"), "a flag reads yes or no");
                    final IqlEngine.Outcome gauges = run(helper, mainframe, "QUERY test_gauges");
                    helper.assertTrue(gauges.ok() && gauges.table().columns().contains("reading"),
                            "a path one table alone has names it: " + gauges.message());
                    // The studio's Object Explorer lists the table with its columns, their units and its rows.
                    final List<IsmsSchemaPayload.OtherTable> others = IqlPayloads.ismsSchema(helper.getLevel(),
                            mainframe, 1).otherTables();
                    helper.assertTrue(others.size() == 1 && others.get(0).id().equals("jstests:test_reactors")
                                    && others.get(0).rows() == 2 && others.get(0).units().get(0).equals("°C"),
                            "the studio is told of the table: " + others);
                })
                .thenSucceed();
    }

    /** A name no table has, and a path two tables share, are said rather than read as nothing. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void query_saysWhenNoTableOrTwoTablesHaveTheName(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null, "the network forms"))
                .thenExecute(() -> {
                    final IqlEngine.Outcome none = run(helper, mainframe, "QUERY no_such_table");
                    helper.assertTrue(!none.ok() && none.message().equals("No table is named no_such_table"),
                            "no table by that name: " + none.message());
                    final IqlEngine.Outcome shared = run(helper, mainframe, "QUERY test_reactors");
                    helper.assertTrue(!shared.ok() && shared.message().contains("jstests:test_reactors")
                                    && shared.message().contains("jstests_twin:test_reactors"),
                            "the ids of both tables: " + shared.message());
                    helper.assertTrue(run(helper, mainframe, "QUERY items").ok(), "the network's own tables still");
                })
                .thenSucceed();
    }

    /** A member whose chunk is not in memory has no row, and reading the table does not load its chunk. */
    @GameTest(template = ARENA)
    public static void rows_leaveOutAMemberWhoseChunkIsNotLoaded(final GameTestHelper helper) {
        final NetworkUuid network = new NetworkUuid(UUID.randomUUID());
        final BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(1_000_000, 0, 1_000_000);
        final NetworkSystem system = NetworkSystem.get(helper.getLevel());
        system.registerMember(new NetworkSystem.MemberNode(new NodeUuid(UUID.randomUUID()), network, far.asLong(),
                NetworkCategory.B, List.of(), List.of(TestNetworkMembers.REACTORS.id())));
        try {
            helper.assertTrue(NetworkTables.rows(helper.getLevel(), network, TestNetworkMembers.REACTORS).isEmpty(),
                    "no row for a member far away");
            helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(far.getX() >> 4, far.getZ() >> 4)
                    == null, "and its chunk stays unloaded");
        } finally {
            system.unregisterMemberAt(far.asLong());
        }
        helper.succeed();
    }

    /**
     * A storm the network has the instrument of: it shows as coming once warned, under way once begun, and gone once
     * over, in the events table; each step is written into the event log; and a job fires at each step it waits for.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void aWorldEvent_isSeenWrittenDownAndFiresTheJobsWaitingForIt(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.setBlock(HOT, TestNetworkMembers.REACTOR.get());
        final TestNetworkMembers.ReactorBlockEntity instrument = reactor(helper, HOT);
        instrument.watches(TestNetworkMembers.WATCHED_STORM);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rack(helper).getServerStorage(0).insert(Items.COBBLESTONE, STOCK);
                    mainframe.installAutomationEngine();
                    job(helper, mainframe, "CREATE JOB on_coming AS DROP 4 cobblestone"
                            + " WHEN EVENT jstests:test_watched_storm COMING");
                    job(helper, mainframe, "CREATE JOB on_storm AS DROP 1 cobblestone"
                            + " WHEN EVENT test_watched_storm UNDER WAY");
                    job(helper, mainframe, "CREATE JOB on_calm AS DROP 2 cobblestone"
                            + " WHEN EVENT jstests:test_watched_storm ENDED");
                    helper.assertTrue(WorldEvents.plan(helper.getLevel(), TestNetworkMembers.WATCHED_STORM, STORM_IN,
                            Optional.empty()), "the storm is planned");
                    helper.assertTrue(stateOf(helper, mainframe).isEmpty(), "not seen before its warning");
                })
                .thenWaitUntil(() -> helper.assertTrue(stateOf(helper, mainframe).equals("coming")
                        && stock(helper, mainframe) == STOCK - 4, "seen coming, and its job fired: "
                        + stateOf(helper, mainframe) + ", " + stock(helper, mainframe)))
                .thenExecute(() -> {
                    instrument.watches();
                    helper.assertTrue(stateOf(helper, mainframe).isEmpty(), "coming only with its instrument on");
                    instrument.watches(TestNetworkMembers.WATCHED_STORM);
                })
                .thenWaitUntil(() -> helper.assertTrue(stateOf(helper, mainframe).equals("under way")
                        && stock(helper, mainframe) == STOCK - 5, "seen under way, and its job fired: "
                        + stateOf(helper, mainframe) + ", " + stock(helper, mainframe)))
                .thenWaitUntil(() -> helper.assertTrue(stateOf(helper, mainframe).isEmpty()
                        && stock(helper, mainframe) == STOCK - 7, "over, and its job fired: "
                        + stateOf(helper, mainframe) + ", " + stock(helper, mainframe)))
                // The end is told as the world's events finish their tick, after the jobs' last look.
                .thenWaitUntil(() -> {
                    final List<NetworkEvent> said = NetworkEventLog.of(helper.getLevel())
                            .newest(mainframe.networkUuid(), helper.getLevel().getGameTime(),
                                    event -> event.machine() == null && event.message().english()
                                            .startsWith("Watched storm"), NetworkEventLog.MOST);
                    final List<String> words = said.stream().map(event -> event.message().english()).toList();
                    helper.assertTrue(said.size() == 3 && words.get(0).equals("Watched storm is over")
                                    && words.get(1).equals("Watched storm under way")
                                    && words.get(2).startsWith("Watched storm expected in"),
                            "the log heard each step: " + words);
                    helper.assertTrue(said.get(0).level() == EventLevel.INFO && said.get(1).level() == EventLevel.WARN
                            && said.get(2).level() == EventLevel.WARN, "the end an INFO, the rest WARN");
                })
                .thenSucceed();
    }

    /** A running Mainframe, a cable and a rack with one server behind it. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        TestCables.lay(helper, CABLE, ComputingModule.HBW_CABLE);
        TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK, Direction.EAST);
        return mainframe;
    }

    /* What a test that waits for members to join says while it waits: the network, the cable's and the members. */
    private static String joined(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        final NetworkSystem system = NetworkSystem.get(helper.getLevel());
        return "mainframe " + mainframe.networkUuid() + ", cable "
                + system.connectivity().networkOf(helper.absolutePos(CABLE).asLong()) + ", members "
                + (mainframe.networkUuid() == null ? 0 : system.membersOf(mainframe.networkUuid()).size())
                + ", hot " + system.memberAt(helper.absolutePos(HOT).asLong());
    }

    private static IqlEngine.Outcome run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String statement) {
        return mainframe.networkOperations().query(
                IqlEngine.viewOf(new ServerCliComputer(mainframe, helper.getLevel())), statement, ROWS);
    }

    private static void job(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                            final String statement) {
        final IqlEngine.Outcome made = run(helper, mainframe, statement);
        helper.assertTrue(made.ok(), "the job is made: " + made.message());
    }

    /* The state the events table gives the watched storm, or empty when the network does not see it. */
    private static String stateOf(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        final IqlTable table = run(helper, mainframe, "QUERY events WHERE event = 'jstests:test_watched_storm'")
                .table();
        return table.rows().isEmpty() ? "" : table.cell(0, "state").english();
    }

    private static long stock(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        return NetworkStorage.of(helper.getLevel(), mainframe.networkUuid()).count(Items.COBBLESTONE);
    }

    private static ServerRackBlockEntity rack(final GameTestHelper helper) {
        if (helper.getBlockEntity(RACK) instanceof ServerRackBlockEntity rack) {
            return rack;
        }
        throw new GameTestAssertException("no rack at " + RACK.toShortString());
    }

    private static TestNetworkMembers.ReactorBlockEntity reactor(final GameTestHelper helper, final BlockPos at) {
        if (helper.getBlockEntity(at) instanceof TestNetworkMembers.ReactorBlockEntity reactor) {
            return reactor;
        }
        throw new GameTestAssertException("no reactor at " + at.toShortString());
    }
}
