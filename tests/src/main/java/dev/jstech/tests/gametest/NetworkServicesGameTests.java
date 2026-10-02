/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.EngineReplacement;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.engine.EngineSwap;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.operation.payload.NetworkServicesPayload;
import dev.jstech.computers.operation.payload.network.NetworkServicesPayloads;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.SubframeNode;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestEngines;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What the Network Manager's Services tab shows of a Mainframe's software, and replacing the network's engine: the
 * steps take time, the network has no engine while they run, and the new one plans the work once they are over.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkServicesGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 14;
    private static final int ROWS = 64;
    private static final ResourceLocation MIDSOFT = NetworkEngines.MIDSOFT_IQL_SERVER.program();

    private NetworkServicesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void collect_showsTheFactoryEngineAndTheOtherServices(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.installAutomationEngine();
                    final NetworkServicesPayload shown = NetworkServicesPayloads.collect(mainframe);
                    final NetworkServicesPayload.Engine engine = shown.engine();
                    helper.assertTrue(engine.state() == NetworkServicesPayload.ENGINE_RUNNING,
                            "the factory engine runs; state " + engine.state());
                    helper.assertValueEqual(engine.name(), "Midsoft IQL Server", "the engine's name");
                    helper.assertValueEqual(engine.version(), "2012", "a Standard Mainframe's version");
                    helper.assertValueEqual(engine.vendor(), "Midsoft", "who makes it");
                    helper.assertValueEqual(engine.dialect(), "IQL", "the language it reads");
                    helper.assertTrue(engine.memoryMb() > 0, "it takes memory while it runs");
                    helper.assertTrue(engine.capabilities().contains("procedures_and_views"),
                            "the Midsoft engine keeps procedures and views: " + engine.capabilities());
                    helper.assertTrue(shown.installed().size() == 1
                                    && shown.installed().getFirst().state() == NetworkServicesPayload.ROW_ACTIVE,
                            "the one engine installed is the active one");
                    helper.assertTrue(shown.services().size() == 2, "the Automation Engine and the Mirror");
                    helper.assertTrue(shown.services().get(0).state() == NetworkServicesPayload.SERVICE_RUNNING,
                            "the Automation Engine, installed, runs");
                    helper.assertTrue(shown.services().get(1).state() == NetworkServicesPayload.SERVICE_ABSENT,
                            "the Mirror is not installed");
                    helper.assertFalse(shown.replacement().underWay(), "nothing is being replaced");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void collect_marksTheSubframeOnAnotherEngine(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final NetworkSystem system = NetworkSystem.get(helper.getLevel());
                    final Optional<NodeUuid> orchestrator = Optional.of(mainframe.nodeUuid());
                    final NodeUuid same = NodeUuid.random();
                    final NodeUuid other = NodeUuid.random();
                    system.registerSubframe(new SubframeNode(same, mainframe.networkUuid(), 100L, orchestrator, 0,
                            MIDSOFT.toString()));
                    system.registerSubframe(new SubframeNode(other, mainframe.networkUuid(), 100L, orchestrator, 0,
                            TestEngines.PLAIN.toString()));
                    final NetworkServicesPayload shown = NetworkServicesPayloads.collect(mainframe);
                    helper.assertTrue(shown.subframes().size() == 2, "both Subframes are listed");
                    final long working = shown.subframes().stream()
                            .filter(NetworkServicesPayload.SubframeRow::takesWork).count();
                    helper.assertTrue(working == 1, "only the one on the Mainframe's engine takes work");
                    helper.assertTrue(shown.subframes().stream().anyMatch(row -> !row.takesWork()
                                    && row.engine().equals("Plain Engine")),
                            "the other is named with the engine it runs");
                    system.unregisterSubframe(mainframe.networkUuid(), same);
                    system.unregisterSubframe(mainframe.networkUuid(), other);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void replace_leavesTheNetworkWithoutAnEngineUntilTheStepsAreOver(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        final int[] ticks = {0};
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(mainframe.installEngine(TestEngines.PLAIN), "the plain engine installs");
                    helper.assertFalse(mainframe.replaceEngine(MIDSOFT),
                            "the engine running already is no replacement for itself");
                    helper.assertTrue(mainframe.replaceEngine(TestEngines.PLAIN), "the replacement begins");
                    helper.assertFalse(mainframe.replaceEngine(TestEngines.PLAIN), "one replacement at a time");
                    final EngineReplacement replacement = mainframe.engineReplacement();
                    helper.assertTrue(replacement != null && MIDSOFT.equals(replacement.from())
                            && TestEngines.PLAIN.equals(replacement.to()), "it replaces Midsoft with Plain");
                    ticks[0] = replacement.ticks();
                    helper.assertValueEqual(ticks[0], EngineSwap.ticksFor(mainframe.indexedTypes()),
                            "its length follows the item types to index");
                    helper.assertTrue(mainframe.runningEngine() == null, "the network has no engine meanwhile");
                    final IqlEngine.Outcome refused = query(helper, mainframe);
                    helper.assertFalse(refused.ok(), "a query is refused while the engine is replaced");
                    final NetworkServicesPayload shown = NetworkServicesPayloads.collect(mainframe);
                    helper.assertTrue(shown.engine().state() == NetworkServicesPayload.ENGINE_REPLACING,
                            "the card says it is being replaced");
                    helper.assertTrue(shown.replacement().underWay()
                            && shown.replacement().from().equals("Midsoft IQL Server"), "the steps are sent");
                    helper.assertFalse(mainframe.setEngineRunning(false), "nothing else changes meanwhile");
                })
                .thenWaitUntil(() -> helper.assertTrue(mainframe.engineReplacement() == null,
                        "the replacement ends after its " + ticks[0] + " ticks"))
                .thenExecute(() -> {
                    helper.assertTrue(TestEngines.PLAIN.equals(mainframe.activeEngine())
                            && mainframe.runningEngine() != null, "the new engine plans the network's work");
                    helper.assertTrue(query(helper, mainframe).ok(), "and answers the language");
                    final NetworkServicesPayload shown = NetworkServicesPayloads.collect(mainframe);
                    helper.assertValueEqual(shown.engine().name(), "Plain Engine", "the card shows the new engine");
                    helper.assertTrue(shown.installed().stream().anyMatch(row -> row.program()
                                    .equals(MIDSOFT.toString()) && row.state() == NetworkServicesPayload.ROW_INSTALLED),
                            "the old one stays installed");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void replace_takingTheNewEngineOffLeavesTheOldOneStopped(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.installEngine(TestEngines.PLAIN);
                    helper.assertTrue(mainframe.replaceEngine(TestEngines.PLAIN), "the replacement begins");
                    helper.assertTrue(mainframe.uninstallEngine(TestEngines.PLAIN), "the new engine comes off");
                    helper.assertTrue(mainframe.engineReplacement() == null, "which ends the replacement");
                    helper.assertTrue(MIDSOFT.equals(mainframe.activeEngine()) && !mainframe.engineRunning(),
                            "the old engine was stopped on the way, and stays chosen and stopped");
                    helper.assertTrue(NetworkServicesPayloads.collect(mainframe).engine().state()
                            == NetworkServicesPayload.ENGINE_STOPPED, "the card says so");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void replace_survivesSavingTheMainframe(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.installEngine(TestEngines.PLAIN);
                    mainframe.replaceEngine(TestEngines.PLAIN);
                    final EngineReplacement before = mainframe.engineReplacement();
                    final CompoundTag saved = mainframe.saveWithFullMetadata(helper.getLevel().registryAccess());
                    mainframe.loadWithComponents(saved, helper.getLevel().registryAccess());
                    helper.assertValueEqual(mainframe.engineReplacement(), before,
                            "the replacement under way is written down with the Mainframe");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void stop_leavesTheChosenEngineStopped(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(mainframe.setEngineRunning(false), "the engine stops");
                    final NetworkServicesPayload shown = NetworkServicesPayloads.collect(mainframe);
                    helper.assertTrue(shown.engine().state() == NetworkServicesPayload.ENGINE_STOPPED,
                            "the card shows it stopped");
                    helper.assertTrue(shown.installed().getFirst().state() == NetworkServicesPayload.ROW_STOPPED,
                            "and so does its row");
                    helper.assertTrue(shown.engine().upTicks() == 0L, "a stopped engine is not up");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void plansToday_countsWhatTheEnginePlanned(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        final int[] before = {0};
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    before[0] = mainframe.plansToday();
                    query(helper, mainframe);
                    query(helper, mainframe);
                    helper.assertValueEqual(mainframe.plansToday(), before[0] + 2, "each query is a plan");
                    mainframe.setEngineRunning(false);
                    query(helper, mainframe);
                    helper.assertValueEqual(mainframe.plansToday(), before[0] + 2,
                            "a refused query is planned by nobody");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void upTime_runsWhileTheEngineDoes(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(mainframe.engineUpTicks() > 0L,
                        "the engine has been up since the Mainframe started"))
                .thenSucceed();
    }

    private static IqlEngine.Outcome query(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        final ServerCliComputer machine = new ServerCliComputer(mainframe, helper.getLevel());
        return mainframe.networkOperations().query(IqlEngine.viewOf(machine), "QUERY items", ROWS);
    }

    /** A running Mainframe, a cable and a rack with one server, the rack behind the cable. */
    private static MainframeBlockEntity storageNetwork(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe =
                TestWorldBuilder.forGameTest(helper).placeRunningMainframe(new BlockPos(1, 2, 2));
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestWorldBuilder.forGameTest(helper).placeSeededRack(new BlockPos(3, 2, 2), Direction.EAST);
        return mainframe;
    }
}
