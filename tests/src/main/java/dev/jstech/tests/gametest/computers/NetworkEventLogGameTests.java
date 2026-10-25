/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.eventlog.NetworkEvents;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.operation.payload.NetworkEventsPayload;
import dev.jstech.computers.operation.payload.RequestNetworkEventsPayload;
import dev.jstech.computers.operation.payload.network.NetworkEventPayloads;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.computers.NetworkFixtures.placeRunningMainframe;

/**
 * GameTests for a network's event log: what writes into it (a machine switched on and off, a server's bay switched, a
 * conflict over the network starting and ending), how much it keeps, that it is saved, and what the Network Manager is
 * sent through its filters.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkEventLogGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final int PATIENT = 200;
    private static final BlockPos HOST = new BlockPos(1, 2, 2);

    private NetworkEventLogGameTests() {
    }

    /** A computer switched off and on again: its network writes both down, newest first, under its name. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void aComputerSwitchedOffAndOn_isWrittenIntoItsNetworksLog(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork().cc();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.networkUuid() != null,
                        "the Crafting Computer joins the network"))
                .thenExecute(computer::togglePower)
                .thenExecute(computer::togglePower)
                .thenExecute(() -> {
                    // Its own lines: a world event another test plans is written into every network's log too.
                    final List<NetworkEvent> log = newest(helper, computer.networkUuid()).stream()
                            .filter(event -> computer.nodeUuid().equals(event.machine())).toList();
                    helper.assertTrue(log.size() >= 2, "both switches are written down: " + log);
                    final NetworkEvent on = log.get(0);
                    final NetworkEvent off = log.get(1);
                    helper.assertTrue(on.message().english().equals("Turned on")
                            && off.message().english().equals("Turned off"), "on after off: " + log);
                    helper.assertTrue(on.level() == EventLevel.INFO && off.level() == EventLevel.INFO,
                            "a switch is INFO: " + log);
                    helper.assertTrue(computer.nodeUuid().equals(on.machine()), "it is the computer's: " + on);
                    helper.assertTrue(on.machineName().startsWith("CC-"), "named by its kind and id: " + on);
                })
                .thenSucceed();
    }

    /** A server's bay switched off: the server is a machine of the network, and the network writes it down. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void aServersBaySwitchedOff_isWrittenUnderTheServersName(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork().rack();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(rack.networkUuid() != null
                        && ServerItem.nodeUuid(rack.getServers().getStackInSlot(0)) != null,
                        "the rack's server joins the network"))
                .thenExecute(() -> rack.toggleBayPower(0))
                .thenExecute(() -> {
                    final NodeUuid server = new NodeUuid(ServerItem.nodeUuid(rack.getServers().getStackInSlot(0)));
                    final List<NetworkEvent> log = newest(helper, rack.networkUuid()).stream()
                            .filter(event -> server.equals(event.machine())).toList();
                    helper.assertTrue(!log.isEmpty() && server.equals(log.get(0).machine())
                            && log.get(0).message().english().equals("Turned off"),
                            "the server's switch is written down: " + log);
                    helper.assertTrue(log.get(0).machineName().startsWith("SRV-"),
                            "named as the network names its servers: " + log.get(0));
                })
                .thenSucceed();
    }

    /** A second Mainframe on the cable is an ERROR in the network's log, and taking it away an INFO after it. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void aConflict_isAnErrorAndItsEndAnInfo(final GameTestHelper helper) {
        final BlockPos second = new BlockPos(4, 2, 2);
        final MainframeBlockEntity first = placeRunningMainframe(helper, new BlockPos(2, 2, 2));
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(first.networkUuid() != null, "the first owns its network"))
                .thenExecute(() -> {
                    network[0] = first.networkUuid();
                    TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
                    placeRunningMainframe(helper, second);
                })
                .thenWaitUntil(() -> helper.assertTrue(first.hasNetworkConflict(), "the two conflict"))
                .thenExecute(() -> helper.setBlock(second, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertFalse(first.hasNetworkConflict(), "the conflict ends"))
                .thenExecute(() -> {
                    final List<NetworkEvent> log = newest(helper, network[0]).stream()
                            .filter(event -> event.machine() != null).toList();
                    helper.assertTrue(!log.isEmpty() && log.get(0).level() == EventLevel.INFO
                            && log.get(0).message().english().equals("Network conflict resolved"),
                            "its end is the newest: " + log);
                    helper.assertTrue(log.stream().anyMatch(event -> event.level() == EventLevel.ERROR
                            && event.message().english().startsWith("Network conflict: a second Mainframe")),
                            "it began as an ERROR: " + log);
                })
                .thenSucceed();
    }

    /** A log keeps at most two thousand events, and none older than seven days of the world's clock. */
    @GameTest(template = ARENA)
    public static void aLog_keepsItsLastSevenDaysAndAtMostTwoThousand(final GameTestHelper helper) {
        final NetworkEventLog log = new NetworkEventLog();
        final NetworkUuid network = NetworkUuid.random();
        for (int i = 0; i < NetworkEventLog.MOST + 5; i++) {
            log.add(network, event(100L, EventLevel.INFO, null, "Turned on"));
        }
        helper.assertTrue(log.size(network) == NetworkEventLog.MOST, "it keeps two thousand: " + log.size(network));
        helper.assertTrue(log.newest(network, 100L + NetworkEventLog.KEPT_TICKS + 1, any -> true, 10).isEmpty(),
                "what is past seven days is let go when it is read");
        log.add(network, event(100L + NetworkEventLog.KEPT_TICKS + 2, EventLevel.WARN, null, "Late"));
        helper.assertTrue(log.size(network) == 1, "and when a new one comes: " + log.size(network));
        helper.succeed();
    }

    /** A log is saved with the world and read back as it was, the machine, its name and the words included. */
    @GameTest(template = ARENA)
    public static void aLog_isSavedAndReadBack(final GameTestHelper helper) {
        final NetworkEventLog log = new NetworkEventLog();
        final NetworkUuid network = NetworkUuid.random();
        final NodeUuid machine = new NodeUuid(UUID.randomUUID());
        log.add(network, event(10L, EventLevel.INFO, machine, "Turned on"));
        log.add(network, new NetworkEvent(20L, 6020L, EventLevel.ERROR, null, "",
                NetworkEvents.CONFLICT.with("1, 2, 3")));
        final CompoundTag saved = log.save(new CompoundTag(), helper.getLevel().registryAccess());
        final NetworkEventLog read = NetworkEventLog.factory().deserializer().apply(saved,
                helper.getLevel().registryAccess());
        helper.assertTrue(read.newest(network, 30L, any -> true, 10).equals(log.newest(network, 30L, any -> true, 10)),
                "read back as written: " + read.newest(network, 30L, any -> true, 10));
        helper.succeed();
    }

    /** The Network Manager is sent what its filters keep, and only a short word when the log has not changed. */
    @GameTest(template = ARENA)
    public static void theNetworkManager_isSentWhatItsFiltersKeep(final GameTestHelper helper) {
        final NetworkEventLog log = new NetworkEventLog();
        final NetworkUuid network = NetworkUuid.random();
        final NodeUuid server = new NodeUuid(UUID.randomUUID());
        final NodeUuid computer = new NodeUuid(UUID.randomUUID());
        final long now = 50_000L;
        log.add(network, event(now - 30_000L, EventLevel.ERROR, server, "Old"));
        log.add(network, event(now - 500L, EventLevel.WARN, null, "Storm"));
        log.add(network, event(now - 100L, EventLevel.INFO, computer, "Turned on"));

        final NetworkEventsPayload all = ask(log, network, RequestNetworkEventsPayload.ANY_LEVEL,
                RequestNetworkEventsPayload.ANY_MACHINE, RequestNetworkEventsPayload.PERIOD_ALL,
                RequestNetworkEventsPayload.NOTHING_KNOWN, now);
        helper.assertTrue(words(all).equals(List.of("Turned on", "Storm", "Old")), "newest first: " + words(all));
        helper.assertTrue(all.machines().size() == 2, "the two machines named: " + all.machines());
        helper.assertTrue(words(ask(log, network, (byte) EventLevel.ERROR.id(),
                        RequestNetworkEventsPayload.ANY_MACHINE, RequestNetworkEventsPayload.PERIOD_ALL,
                        RequestNetworkEventsPayload.NOTHING_KNOWN, now)).equals(List.of("Old")), "by level");
        helper.assertTrue(words(ask(log, network, RequestNetworkEventsPayload.ANY_LEVEL, computer.asString(),
                        RequestNetworkEventsPayload.PERIOD_ALL, RequestNetworkEventsPayload.NOTHING_KNOWN, now))
                .equals(List.of("Turned on")), "by machine");
        helper.assertTrue(words(ask(log, network, RequestNetworkEventsPayload.ANY_LEVEL,
                        RequestNetworkEventsPayload.WHOLE_NETWORK, RequestNetworkEventsPayload.PERIOD_ALL,
                        RequestNetworkEventsPayload.NOTHING_KNOWN, now)).equals(List.of("Storm")),
                "the whole network's own");
        helper.assertTrue(words(ask(log, network, RequestNetworkEventsPayload.ANY_LEVEL,
                        RequestNetworkEventsPayload.ANY_MACHINE, RequestNetworkEventsPayload.PERIOD_HOUR,
                        RequestNetworkEventsPayload.NOTHING_KNOWN, now)).equals(List.of("Turned on", "Storm")),
                "the last hour");
        final NetworkEventsPayload again = ask(log, network, RequestNetworkEventsPayload.ANY_LEVEL,
                RequestNetworkEventsPayload.ANY_MACHINE, RequestNetworkEventsPayload.PERIOD_ALL, all.revision(), now);
        helper.assertTrue(again.unchanged() && again.rows().isEmpty(), "nothing new to send: " + again);
        helper.succeed();
    }

    private static List<NetworkEvent> newest(final GameTestHelper helper, final NetworkUuid network) {
        return NetworkEventLog.of(helper.getLevel()).newest(network, helper.getLevel().getGameTime(), any -> true,
                NetworkEventLog.MOST);
    }

    private static NetworkEvent event(final long time, final EventLevel level, final NodeUuid machine,
                                      final String what) {
        return new NetworkEvent(time, time, level, machine, machine == null ? "" : "SRV-000000", Text.literal(what));
    }

    private static NetworkEventsPayload ask(final NetworkEventLog log, final NetworkUuid network, final byte level,
                                            final String machine, final byte period, final long known,
                                            final long now) {
        return NetworkEventPayloads.collect(log, HOST, network,
                new RequestNetworkEventsPayload(HOST, level, machine, period, known), now);
    }

    private static List<String> words(final NetworkEventsPayload payload) {
        return payload.rows().stream().map(row -> row.message().english()).toList();
    }
}
