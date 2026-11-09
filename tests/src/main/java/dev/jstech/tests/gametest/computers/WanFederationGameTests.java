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
import dev.jstech.computers.blockentity.WanGatewayBlockEntity;
import dev.jstech.computers.operation.payload.wan.BorderLinkStatePayload;
import dev.jstech.computers.operation.payload.wan.FederationPayloads;
import dev.jstech.computers.operation.payload.wan.FederationStatePayload;
import dev.jstech.computers.operation.payload.wan.WanPayloads;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.wan.FederatedOperation;
import dev.jstech.computers.wan.FederationPath;
import dev.jstech.computers.wan.WanFederation;
import dev.jstech.computers.wan.WanMode;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.computers.wan.WanTexts;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.computers.WanFixtures.FAR_MAINFRAME;
import static dev.jstech.tests.testkit.computers.WanFixtures.FAR_PORT;
import static dev.jstech.tests.testkit.computers.WanFixtures.NEAR_PORT;
import static dev.jstech.tests.testkit.computers.WanFixtures.farGateway;
import static dev.jstech.tests.testkit.computers.WanFixtures.farMainframe;
import static dev.jstech.tests.testkit.computers.WanFixtures.farRack;
import static dev.jstech.tests.testkit.computers.WanFixtures.line;
import static dev.jstech.tests.testkit.computers.WanFixtures.middleOfLine;
import static dev.jstech.tests.testkit.computers.WanFixtures.nearGateway;
import static dev.jstech.tests.testkit.computers.WanFixtures.nearMainframe;

/**
 * Two places joined by a long-distance line between their WAN Gateway Computers (computers/docs/design/network.md):
 * a federation when each has a Mainframe, an extension when only one does, a Mainframe coming up over an extension
 * turning it into a federation rather than a conflict, and Operations crossing a federation as the far network allows.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WanFederationGameTests {

    private static final String ARENA = "wan";
    private static final StorageKey COBBLE = StorageKey.of(Items.COBBLESTONE);

    private WanFederationGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void federation_twoMainframesLinkAndStayTwoNetworks(final GameTestHelper helper) {
        final MainframeBlockEntity near = nearMainframe(helper);
        final WanGatewayBlockEntity nearGateway = nearGateway(helper);
        final MainframeBlockEntity far = farMainframe(helper);
        final WanGatewayBlockEntity farGateway = farGateway(helper);
        line(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(nearGateway.links().mode(NEAR_PORT) == WanMode.FEDERATION
                        && farGateway.links().mode(FAR_PORT) == WanMode.FEDERATION, "both ports federate"))
                .thenExecute(() -> {
                    helper.assertTrue(near.networkUuid() != null && far.networkUuid() != null
                            && !near.networkUuid().equals(far.networkUuid()), "two networks, never merged");
                    helper.assertFalse(near.hasNetworkConflict() || far.hasNetworkConflict(), "and no conflict");
                    helper.assertTrue(path(helper, near, far) != null && path(helper, far, near) != null,
                            "each reaches the other");
                    helper.assertTrue(nearGateway.links().farName(NEAR_PORT).equals(farGateway.displayName()),
                            "the near port names the far gateway");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void extension_farPlaceJoinsTheNetwork(final GameTestHelper helper) {
        final MainframeBlockEntity near = nearMainframe(helper);
        final WanGatewayBlockEntity nearGateway = nearGateway(helper);
        final WanGatewayBlockEntity farGateway = farGateway(helper);
        TestCables.lay(helper, FAR_MAINFRAME, ComputingModule.HBW_CABLE);
        final ServerRackBlockEntity rack = farRack(helper);
        line(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(nearGateway.links().mode(NEAR_PORT) == WanMode.EXTENSION
                        && near.networkUuid() != null && near.networkUuid().equals(farGateway.networkUuid()),
                        "the far gateway is on the near network"))
                .thenWaitUntil(() -> helper.assertTrue(near.networkUuid().equals(rack.networkUuid()),
                        "and so is the far place's rack"))
                .thenExecute(() -> helper.assertTrue(!NetworkSystem.get(helper.getLevel())
                        .serversOf(near.networkUuid()).isEmpty(), "whose servers serve the near network"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void mainframeOverAnExtension_makesAFederationNotAConflict(final GameTestHelper helper) {
        final MainframeBlockEntity near = nearMainframe(helper);
        final WanGatewayBlockEntity nearGateway = nearGateway(helper);
        farGateway(helper);
        TestCables.lay(helper, FAR_MAINFRAME, ComputingModule.HBW_CABLE);
        line(helper);
        final MainframeBlockEntity[] far = new MainframeBlockEntity[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(nearGateway.links().mode(NEAR_PORT) == WanMode.EXTENSION,
                        "first an extension"))
                .thenExecute(() -> far[0] = farMainframe(helper))
                .thenWaitUntil(() -> helper.assertTrue(nearGateway.links().mode(NEAR_PORT) == WanMode.FEDERATION
                        && far[0].networkUuid() != null && !far[0].networkUuid().equals(near.networkUuid()),
                        "then a federation, the far Mainframe running a network of its own"))
                .thenExecute(() -> helper.assertFalse(near.hasNetworkConflict() || far[0].hasNetworkConflict(),
                        "with no conflict on either side"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void take_bringsTheFarNetworksItemsAcrossTheLine(final GameTestHelper helper) {
        final MainframeBlockEntity near = nearMainframe(helper);
        nearGateway(helper);
        final MainframeBlockEntity far = farMainframe(helper);
        final WanGatewayBlockEntity farGateway = farGateway(helper);
        farGateway.setPortSettings(FAR_PORT, farGateway.portSettings(FAR_PORT).with(WanRight.TAKE, true));
        final ServerRackBlockEntity rack = farRack(helper);
        line(helper);
        final Map<StorageKey, Long> got = new HashMap<>();
        final FederatedOperation[] op = new FederatedOperation[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(path(helper, near, far) != null, "federated"))
                .thenExecute(() -> rack.getServerStorage(0).insert(Items.COBBLESTONE, 64))
                .thenWaitUntil(() -> helper.assertTrue(far.networkIndex().available(COBBLE) == 64, "stocked"))
                .thenExecute(() -> op[0] = near.submitFederated(FederatedOperation.take(helper.getLevel(),
                        path(helper, near, far), COBBLE, 32, collecting(got)), OperationPriority.DEFAULT))
                .thenWaitUntil(() -> helper.assertTrue(op[0] != null && op[0].isDone(), "the take settles"))
                .thenExecute(() -> {
                    helper.assertTrue(op[0].toRecord().status() == OperationStatus.COMPLETED.id(), "completed");
                    helper.assertTrue(got.getOrDefault(COBBLE, 0L) == 32L, "32 came across; " + got);
                })
                .thenWaitUntil(() -> helper.assertTrue(far.networkIndex().available(COBBLE) == 32,
                        "and 32 are left on the far network"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void take_refusedWhereTheFarPortDoesNotAllowIt(final GameTestHelper helper) {
        final MainframeBlockEntity near = nearMainframe(helper);
        nearGateway(helper);
        final MainframeBlockEntity far = farMainframe(helper);
        farGateway(helper);
        line(helper);
        final FederatedOperation[] op = new FederatedOperation[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(path(helper, near, far) != null, "federated"))
                .thenExecute(() -> {
                    helper.assertFalse(path(helper, near, far).allows(WanRight.TAKE),
                            "a new port lets nothing be taken");
                    op[0] = near.submitFederated(FederatedOperation.take(helper.getLevel(), path(helper, near, far),
                            COBBLE, 8, collecting(new HashMap<>())), OperationPriority.DEFAULT);
                })
                .thenWaitUntil(() -> helper.assertTrue(op[0].isDone(), "settles at once"))
                .thenExecute(() -> {
                    helper.assertTrue(op[0].toRecord().status() == OperationStatus.FAILED.id(), "failed");
                    helper.assertTrue(op[0].note().key().equals(WanTexts.NOT_ALLOWED.key()), "and says why");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void lineCut_losesWhatWasOnIt(final GameTestHelper helper) {
        final MainframeBlockEntity near = nearMainframe(helper);
        nearGateway(helper);
        final MainframeBlockEntity far = farMainframe(helper);
        final WanGatewayBlockEntity farGateway = farGateway(helper);
        farGateway.setPortSettings(FAR_PORT, farGateway.portSettings(FAR_PORT).with(WanRight.TAKE, true));
        final ServerRackBlockEntity rack = farRack(helper);
        line(helper);
        final FederatedOperation[] op = new FederatedOperation[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(path(helper, near, far) != null, "federated"))
                .thenExecute(() -> rack.getServerStorage(0).insert(Items.COBBLESTONE, 4000))
                .thenWaitUntil(() -> helper.assertTrue(far.networkIndex().available(COBBLE) == 4000, "stocked"))
                .thenExecute(() -> op[0] = near.submitFederated(FederatedOperation.take(helper.getLevel(),
                        path(helper, near, far), COBBLE, 4000, collecting(new HashMap<>())),
                        OperationPriority.DEFAULT))
                .thenExecuteAfter(10, () -> helper.setBlock(middleOfLine(), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(op[0].isDone(), "the take ends with the line"))
                .thenExecute(() -> {
                    helper.assertTrue(op[0].toRecord().status() == OperationStatus.FAILED.id(), "failed");
                    helper.assertTrue(op[0].note().key().equals(WanTexts.LINE_FELL.key()), "the line fell");
                    helper.assertTrue(op[0].toRecord().moved() < 4000L, "short of what was asked");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void send_putsItemsIntoTheFarNetwork(final GameTestHelper helper) {
        final MainframeBlockEntity near = nearMainframe(helper);
        nearGateway(helper);
        final MainframeBlockEntity far = farMainframe(helper);
        final WanGatewayBlockEntity farGateway = farGateway(helper);
        farGateway.setPortSettings(FAR_PORT, farGateway.portSettings(FAR_PORT).with(WanRight.SEND, true));
        farRack(helper);
        line(helper);
        final Player sender = helper.makeMockPlayer(GameType.SURVIVAL);
        final FederatedOperation[] op = new FederatedOperation[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(path(helper, near, far) != null, "federated"))
                .thenExecute(() -> op[0] = near.submitFederated(FederatedOperation.send(helper.getLevel(),
                        path(helper, near, far), COBBLE, 16, sender), OperationPriority.DEFAULT))
                .thenWaitUntil(() -> helper.assertTrue(op[0].isDone(), "the send settles"))
                .thenExecute(() -> helper.assertTrue(op[0].toRecord().status() == OperationStatus.COMPLETED.id(),
                        "completed; " + op[0].note()))
                .thenWaitUntil(() -> helper.assertTrue(far.networkIndex().available(COBBLE) == 16,
                        "the far network holds the 16"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void portOff_borderLinkStillShowsTheFarEndAndWhatTheLineDid(final GameTestHelper helper) {
        nearMainframe(helper);
        final WanGatewayBlockEntity nearGateway = nearGateway(helper);
        farMainframe(helper);
        final WanGatewayBlockEntity farGateway = farGateway(helper);
        line(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(nearGateway.links().mode(NEAR_PORT) == WanMode.FEDERATION,
                        "federated"))
                .thenExecute(() -> farGateway.setPortSettings(FAR_PORT,
                        farGateway.portSettings(FAR_PORT).withOn(false)))
                .thenWaitUntil(() -> helper.assertTrue(
                        nearGateway.links().state(NEAR_PORT) == WanGatewayBlockEntity.LINE_DOWN, "the line is down"))
                .thenExecute(() -> {
                    final BorderLinkStatePayload.Port port = WanPayloads.state(nearGateway, helper.getLevel())
                            .ports().get(NEAR_PORT - 1);
                    helper.assertFalse(port.up(), "BorderLink shows it down");
                    helper.assertTrue(port.far().equals(farGateway.displayName()), "with the gateway still there");
                    helper.assertTrue(port.wanMode() == WanMode.FEDERATION, "and what the line did while up");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void federatedTab_listsTheFarNetworkAndOnlyWhatItLetsBeSeen(final GameTestHelper helper) {
        final MainframeBlockEntity near = nearMainframe(helper);
        nearGateway(helper);
        final MainframeBlockEntity far = farMainframe(helper);
        final WanGatewayBlockEntity farGateway = farGateway(helper);
        final ServerRackBlockEntity rack = farRack(helper);
        line(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(path(helper, near, far) != null, "federated"))
                .thenExecute(() -> rack.getServerStorage(0).insert(Items.COBBLESTONE, 64))
                .thenWaitUntil(() -> helper.assertTrue(far.networkIndex().available(COBBLE) == 64, "stocked"))
                .thenExecute(() -> {
                    final FederationStatePayload state = FederationPayloads.state(helper.getLevel(), near,
                            near.getBlockPos(), "");
                    helper.assertTrue(state.peers().size() == 1, "one network federated with this one");
                    final FederationStatePayload.Peer peer = state.shownPeer();
                    helper.assertTrue(peer != null && peer.name().equals(farGateway.displayName()),
                            "shown by the gateway its line ends in");
                    helper.assertTrue(peer.allows(WanRight.SEE) && !peer.allows(WanRight.TAKE),
                            "a new port lets it see and nothing more");
                    helper.assertTrue(state.items().stream().anyMatch(e -> e.key().equals(COBBLE)),
                            "what it holds is listed");
                    farGateway.setPortSettings(FAR_PORT, farGateway.portSettings(FAR_PORT).with(WanRight.SEE,
                            false));
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(FederationPayloads.state(helper.getLevel(), near,
                        near.getBlockPos(), "").items().isEmpty(), "and nothing once it does not let it see"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void borderlinkCommand_setsAPortsSwitchAndListsThePorts(final GameTestHelper helper) {
        nearMainframe(helper);
        final WanGatewayBlockEntity nearGateway = nearGateway(helper);
        farMainframe(helper);
        farGateway(helper);
        line(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(nearGateway.links().mode(NEAR_PORT) == WanMode.FEDERATION,
                        "federated"))
                .thenExecute(() -> {
                    final CliShell shell = CliCommands.newShell(60);
                    final ServerCliComputer cli = new ServerCliComputer(nearGateway, helper.getLevel());
                    shell.run("borderlink port " + NEAR_PORT + " relay on", cli);
                    helper.assertTrue(nearGateway.portSettings(NEAR_PORT).relay(), "the prompt set the switch");
                    final String listed = text(shell.run("borderlink", cli));
                    helper.assertTrue(listed.contains("UP") && listed.contains("Federation"),
                            "and lists the port up, federating; got " + listed);
                })
                .thenSucceed();
    }

    private static String text(final CliShell.Response response) {
        return response.lines().stream().map(CliLine::text).collect(Collectors.joining("\n"));
    }

    /* The way from {@code from}'s network to {@code to}'s, as the world's federations stand now, or null. */
    private static FederationPath path(final GameTestHelper helper, final MainframeBlockEntity from,
                                       final MainframeBlockEntity to) {
        final NetworkUuid a = from.networkUuid();
        final NetworkUuid b = to.networkUuid();
        return a == null || b == null ? null : WanFederation.of(helper.getLevel())
                .path(a, b, helper.getLevel().getGameTime()).orElse(null);
    }

    /* Where a take lands in a test: it keeps all it is given. */
    private static IDataSink collecting(final Map<StorageKey, Long> got) {
        return (key, amount, simulate) -> {
            if (!simulate) {
                got.merge(key, amount, Long::sum);
            }
            return amount;
        };
    }
}
