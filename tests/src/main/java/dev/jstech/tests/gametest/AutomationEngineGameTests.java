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
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The jobs are the Automation Engine's alone: a job is made only where it is installed, and fires only while it
 * runs, whichever Network Operations Engine plans the network's work.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class AutomationEngineGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 14;
    private static final int ROWS = 64;
    private static final long STOCK = 1000L;

    private AutomationEngineGameTests() {
    }

    @GameTest(template = ARENA)
    public static void createJob_needsTheAutomationEngine(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer machine = new ServerCliComputer(mainframe, helper.getLevel());
                    final IqlEngine.Outcome refused = query(mainframe, machine,
                            "CREATE JOB drainer AS DROP 10 cobblestone EVERY 5t");
                    helper.assertTrue(!refused.ok()
                                    && refused.message().equals(IqlEngine.JOBS_NEED_AUTOMATION.english()),
                            "the network's engine alone keeps no job; got " + refused.message());
                    mainframe.installAutomationEngine();
                    helper.assertTrue(query(mainframe, machine, "CREATE JOB drainer AS DROP 10 cobblestone EVERY 5t")
                            .ok(), "with the Automation Engine on the Mainframe the job is made");
                })
                .thenSucceed();
    }

    /** A job fires while the Automation Engine runs, and not on the network's engine alone. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void jobs_fireOnlyWhileTheAutomationEngineRuns(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rack(helper).getServerStorage(0).insert(Items.COBBLESTONE, STOCK);
                    mainframe.installAutomationEngine();
                    helper.assertTrue(query(mainframe, new ServerCliComputer(mainframe, helper.getLevel()),
                            "CREATE JOB drainer AS DROP 10 cobblestone EVERY 5t").ok(), "the job is made");
                    mainframe.uninstallAutomationEngine();
                })
                .thenExecuteAfter(40, () -> helper.assertTrue(stock(helper, mainframe) == STOCK,
                        "with only the Midsoft IQL Server running the job never fires; left "
                                + stock(helper, mainframe)))
                .thenExecuteAfter(0, mainframe::installAutomationEngine)
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe) < STOCK,
                        "back on the Automation Engine the job fires; left " + stock(helper, mainframe)))
                .thenSucceed();
    }

    private static IqlEngine.Outcome query(final MainframeBlockEntity mainframe, final ServerCliComputer machine,
                                           final String statement) {
        return mainframe.networkOperations().query(IqlEngine.viewOf(machine), statement, ROWS);
    }

    private static long stock(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        return NetworkStorage.of(helper.getLevel(), mainframe.networkUuid()).count(Items.COBBLESTONE);
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
}
