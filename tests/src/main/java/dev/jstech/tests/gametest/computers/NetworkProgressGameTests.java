/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a team's progress sets off on the networks it owns: an era or a tier reached is written into the log of the
 * team's network, as an INFO line, and fires the jobs waiting on it, once; another team's progress touches neither.
 */
@PrefixGameTestTemplate(false)
@GameTestHolder(JsTests.MODID)
public final class NetworkProgressGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 14;
    private static final int QUIET = 30;
    private static final int ROWS = 64;
    private static final long STOCK = 10L;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos RACK = new BlockPos(3, 2, 2);

    private NetworkProgressGameTests() {
    }

    /**
     * The team that owns the network reaches the Standard era, then the third tier: each is written into the network's
     * log and fires its job once; another team's era, and an era already reached, do nothing.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void aStepTheTeamReaches_isWrittenDownAndFiresTheJobsWaitingForIt(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID operator = UUID.randomUUID();
        final String team = CoreTeams.teamOf(server, operator);
        MachineOperators.note(mainframe, operator);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rack(helper).getServerStorage(0).insert(Items.COBBLESTONE, STOCK);
                    mainframe.installAutomationEngine();
                    job(helper, mainframe, "CREATE JOB on_era AS DROP 4 cobblestone WHEN ERA standard REACHED");
                    job(helper, mainframe, "CREATE JOB on_tier AS DROP 1 cobblestone WHEN TIER t3 REACHED");
                    TeamProgress.reach(server, CoreTeams.solo(UUID.randomUUID()), SeriesAxes.HARDWARE_ERA,
                            HardwareEra.STANDARD);
                })
                .thenExecuteAfter(QUIET, () -> {
                    helper.assertTrue(stock(helper, mainframe) == STOCK, "another team's era fires nothing: "
                            + stock(helper, mainframe));
                    helper.assertTrue(reached(helper, mainframe).isEmpty(), "and writes nothing");
                    TeamProgress.reach(server, team, SeriesAxes.HARDWARE_ERA, HardwareEra.STANDARD);
                })
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe) == STOCK - 4,
                        "the era fires its job: " + stock(helper, mainframe)))
                .thenExecute(() -> TeamProgress.reach(server, team, SeriesAxes.INDUSTRIAL_TIER, IndustrialTier.T3))
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe) == STOCK - 5,
                        "the tier fires its job: " + stock(helper, mainframe)))
                .thenExecute(() -> TeamProgress.reach(server, team, SeriesAxes.HARDWARE_ERA, HardwareEra.STANDARD))
                .thenExecuteAfter(QUIET, () -> {
                    helper.assertTrue(stock(helper, mainframe) == STOCK - 5, "an era already reached fires nothing: "
                            + stock(helper, mainframe));
                    final List<NetworkEvent> said = reached(helper, mainframe);
                    final List<String> words = said.stream().map(event -> event.message().english()).toList();
                    helper.assertTrue(words.equals(List.of("Tier T3 reached by someone",
                            "Era Standard reached by someone")), "the log heard the era and the tier: " + words);
                    helper.assertTrue(said.stream().allMatch(event -> event.level() == EventLevel.INFO),
                            "each an INFO line");
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

    /* The lines of the network's log that tell an era or a tier reached, newest first. */
    private static List<NetworkEvent> reached(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        return NetworkEventLog.of(helper.getLevel()).newest(mainframe.networkUuid(), helper.getLevel().getGameTime(),
                event -> event.machine() == null && event.message().english().contains(" reached by "),
                NetworkEventLog.MOST);
    }

    private static void job(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                            final String statement) {
        final IqlEngine.Outcome made = mainframe.networkOperations().query(
                IqlEngine.viewOf(new ServerCliComputer(mainframe, helper.getLevel())), statement, ROWS);
        helper.assertTrue(made.ok(), "the job is made: " + made.message());
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
}
