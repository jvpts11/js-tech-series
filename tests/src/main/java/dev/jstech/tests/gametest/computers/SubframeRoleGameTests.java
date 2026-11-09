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
import dev.jstech.computers.engine.EngineRole;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.operation.payload.NetworkServicesPayload;
import dev.jstech.computers.operation.payload.network.NetworkServicesPayloads;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.core.jstechseries.network.FailoverRole;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.network.SubframeNode;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.computers.NetworkFixtures.placeRunningMainframe;

/**
 * A Mainframe's network engine plays orchestrator or Subframe (computers/docs/design/computers.md): a Subframe counts
 * for no conflict, keeps no index and lends the orchestrator its share when both run the same engine; when the
 * orchestrator falls, a Mainframe in failover takes the network over first, else the first Subframe in line, and the
 * network's log says who stopped, who took over, what engine runs and how many Operations were lost.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SubframeRoleGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final BlockPos FIRST = new BlockPos(1, 2, 2);
    private static final BlockPos SECOND = new BlockPos(5, 2, 2);
    private static final BlockPos THIRD = new BlockPos(9, 2, 2);
    /* Past the promotion delay of 60 ticks, with room for the takeover to settle. */
    private static final int PAST_THE_DELAY = 75;

    private SubframeRoleGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void subframe_lendsTheOrchestratorItsShareAndCountsForNoConflict(final GameTestHelper helper) {
        final MainframeBlockEntity orchestrator = placeRunningMainframe(helper, FIRST);
        final MainframeBlockEntity subframe = placeRunningMainframe(helper, SECOND);
        subframe.setEngineRole(EngineRole.SUBFRAME);
        cables(helper, 2, 4);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    helper.assertFalse(orchestrator.hasNetworkConflict() || subframe.hasNetworkConflict(),
                            "a Subframe and its orchestrator do not conflict");
                    final NetworkUuid network = orchestrator.networkUuid();
                    helper.assertTrue(network != null && network.equals(subframe.networkUuid()),
                            "the Subframe is on its orchestrator's network");
                    final NetworkSystem system = NetworkSystem.get(helper.getLevel());
                    helper.assertTrue(system.mainframeOf(network).map(node -> node.nodeUuid()
                            .equals(orchestrator.nodeUuid())).orElse(false), "only the orchestrator runs it");
                    final List<SubframeNode> subframes = system.subframesOf(network);
                    helper.assertTrue(subframes.size() == 1
                                    && subframes.get(0).orchestratingMainframeUuid().isPresent(),
                            "the Subframe registers as one, lending to the orchestrator");
                    helper.assertTrue(orchestrator.pooledCapacity() > orchestrator.capacity(),
                            "the orchestrator runs on its own capacity and the Subframe's share");
                    helper.assertTrue(subframe.liveOperations().isEmpty(), "the Subframe runs no Operations");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 260)
    public static void subframe_takesTheNetworkOverWhenTheOrchestratorFalls(final GameTestHelper helper) {
        final MainframeBlockEntity orchestrator = placeRunningMainframe(helper, FIRST);
        final MainframeBlockEntity subframe = placeRunningMainframe(helper, SECOND);
        subframe.setEngineRole(EngineRole.SUBFRAME);
        cables(helper, 2, 4);
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    network[0] = orchestrator.networkUuid();
                    helper.assertTrue(network[0] != null, "the orchestrator runs a network");
                    helper.assertTrue(orchestrator.successor() == subframe, "the Subframe is next in line");
                })
                .thenExecute(() -> helper.setBlock(FIRST, Blocks.AIR))
                .thenExecuteAfter(PAST_THE_DELAY, () -> {
                    helper.assertTrue(subframe.engineRole() == EngineRole.ORCHESTRATOR,
                            "the Subframe orchestrates now");
                    helper.assertTrue(network[0].equals(subframe.networkUuid()), "the same network, not a new one");
                    helper.assertTrue(NetworkSystem.get(helper.getLevel()).mainframeOf(network[0])
                            .map(node -> node.nodeUuid().equals(subframe.nodeUuid())).orElse(false),
                            "and is registered as its Mainframe");
                    helper.assertTrue(logged(helper, network[0], EventLevel.ERROR, "Stopped answering (60 ticks)"),
                            "the log says the orchestrator stopped answering");
                    helper.assertTrue(logged(helper, network[0], EventLevel.WARN, "Took the network over"),
                            "who took it over");
                    helper.assertTrue(logged(helper, network[0], EventLevel.INFO, "Engine stays Midsoft IQL Server"),
                            "that the engine stays");
                    helper.assertTrue(logged(helper, network[0], EventLevel.INFO, "of its Operations DISCARDED"),
                            "and what was lost");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 260)
    public static void failover_takesTheNetworkOverBeforeASubframe(final GameTestHelper helper) {
        final MainframeBlockEntity orchestrator = placeRunningMainframe(helper, FIRST);
        final MainframeBlockEntity standby = placeRunningMainframe(helper, SECOND);
        standby.toggleFailover();
        final MainframeBlockEntity subframe = placeRunningMainframe(helper, THIRD);
        subframe.setEngineRole(EngineRole.SUBFRAME);
        // Two runs, the standby between them: it bridges every cable it touches into its one network.
        cables(helper, 2, 4);
        cables(helper, 6, 8);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> helper.assertTrue(orchestrator.successor() == standby,
                        "the Mainframe in failover is first in line"))
                .thenExecute(() -> helper.setBlock(FIRST, Blocks.AIR))
                .thenExecuteAfter(PAST_THE_DELAY, () -> {
                    helper.assertTrue(standby.failoverRole() == FailoverRole.ACTIVE, "the standby took over");
                    helper.assertTrue(subframe.engineRole() == EngineRole.SUBFRAME, "the Subframe stayed one");
                    helper.assertFalse(standby.hasNetworkConflict() || subframe.hasNetworkConflict(),
                            "and nothing conflicts");
                    helper.assertTrue(standby.networkUuid() != null
                            && standby.networkUuid().equals(subframe.networkUuid()), "on the same network");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void subframe_withAnotherEngineIsAReserve(final GameTestHelper helper) {
        final MainframeBlockEntity orchestrator = placeRunningMainframe(helper, FIRST);
        final MainframeBlockEntity subframe = placeRunningMainframe(helper, SECOND);
        subframe.installEngine(NetworkEngines.NEXTGRE_IQL.program());
        subframe.activateEngine(NetworkEngines.NEXTGRE_IQL.program());
        subframe.setEngineRole(EngineRole.SUBFRAME);
        cables(helper, 2, 4);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    helper.assertTrue(orchestrator.pooledCapacity() == orchestrator.capacity(),
                            "a Subframe running another engine lends nothing");
                    final NetworkServicesPayload services = NetworkServicesPayloads.collect(orchestrator);
                    helper.assertTrue(services.subframes().size() == 1 && services.subframes().get(0).work()
                            == NetworkServicesPayload.SUBFRAME_RESERVE, "the Services tab shows it as a reserve");
                    helper.assertTrue(orchestrator.successor() == subframe, "it still takes over if nobody else");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void subframe_switchedBackToOrchestratorConflicts(final GameTestHelper helper) {
        final MainframeBlockEntity orchestrator = placeRunningMainframe(helper, FIRST);
        final MainframeBlockEntity subframe = placeRunningMainframe(helper, SECOND);
        subframe.setEngineRole(EngineRole.SUBFRAME);
        cables(helper, 2, 4);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> helper.assertFalse(orchestrator.hasNetworkConflict(),
                        "no conflict while it is a Subframe"))
                .thenExecute(() -> helper.assertTrue(new ServerCliComputer(subframe, helper.getLevel())
                        .engineRole("orchestrator").ok(), "the prompt gives its engine the orchestrator's role"))
                .thenWaitUntil(() -> helper.assertTrue(orchestrator.hasNetworkConflict()
                        || subframe.hasNetworkConflict(), "two orchestrators on one network conflict"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void prompt_saysAndSetsTheRole(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, FIRST);
        final ServerCliComputer prompt = new ServerCliComputer(mainframe, helper.getLevel());
        helper.assertTrue(prompt.engineRole("").message().english().contains("plays orchestrator"),
                "with no word it says the role");
        helper.assertTrue(prompt.engineRole("SUBFRAME").ok() && mainframe.engineRole() == EngineRole.SUBFRAME,
                "a word in any case sets it");
        helper.assertFalse(prompt.engineRole("subframe").ok(), "setting it again is refused");
        helper.assertFalse(prompt.engineRole("boss").ok(), "a word that names no role is refused");
        helper.succeed();
    }

    /* Lays a backbone cable at x {@code from} to {@code to}, between the Mainframes. */
    private static void cables(final GameTestHelper helper, final int from, final int to) {
        for (int x = from; x <= to; x++) {
            TestCables.lay(helper, new BlockPos(x, 2, 2), ComputingModule.HBW_CABLE);
        }
    }

    /* Whether the network's log has a line of {@code level} that says {@code words}. */
    private static boolean logged(final GameTestHelper helper, final NetworkUuid network, final EventLevel level,
                                  final String words) {
        final List<NetworkEvent> events = NetworkEventLog.of(helper.getLevel()).newest(network,
                helper.getLevel().getGameTime(), any -> true, NetworkEventLog.MOST);
        return events.stream().anyMatch(event -> event.level() == level && event.message().english().contains(words));
    }
}
