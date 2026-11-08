/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.boot.DiskCheck;
import dev.jstech.computers.os.boot.DiskChecks;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.NetworkFixtures;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A computer that goes dark suddenly, by a power loss or a failing supply: what its Operations were carrying is lost,
 * and its network's log says how much and of what, unless the server's settings give it back; the disks its system had
 * in use are checked at its next start, each system as its file system needs, and a clean shutdown leaves them clean.
 * Its Local header shows what it draws and its id.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SuddenShutdownGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    /* Long enough for a machine to test itself, come up, go dark and come up again. */
    private static final int PATIENT = 1_200;
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    /** An INSERT far beyond a tick of the server's writing, so it is in flight when the power goes. */
    private static final long DEMAND = 30_000L;
    private static final String LOST = "Lost in transit when it went dark: ";

    private SuddenShutdownGameTests() {
    }

    @BeforeBatch(batch = TestComputerEnergy.NO_LOSS_BATCH)
    public static void nothingIsLost(final ServerLevel level) {
        TestComputerEnergy.losing(false);
    }

    @AfterBatch(batch = TestComputerEnergy.NO_LOSS_BATCH)
    public static void lossesAgain(final ServerLevel level) {
        TestComputerEnergy.losing(true);
    }

    /** The Mainframe goes dark with an INSERT in flight: what it had not written is lost, and the log says what. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void powerLoss_losesWhatTheOperationsCarryAndTheLogSaysWhat(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final NetworkInsertOperation[] op = new NetworkInsertOperation[1];
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.isRunning() && mainframe.networkUuid() != null,
                        "the Mainframe runs its network"))
                .thenExecuteAfter(SETTLE, () -> {
                    network[0] = mainframe.networkUuid();
                    op[0] = mainframe.submitNetworkInsert(Items.COBBLESTONE, DEMAND, "test");
                    helper.assertTrue(op[0] != null, "the INSERT dispatches");
                })
                .thenExecuteAfter(1, () -> mainframe.stopSuddenly(null, CrashPages.Moment.none()))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(op[0].isDone(), "the INSERT settles with the Mainframe");
                    helper.assertTrue(op[0].leftover() == 0L, "and nothing is left over to give back; got "
                            + op[0].leftover());
                    final List<NetworkEvent> lost = events(helper, network[0], EventLevel.ERROR, LOST);
                    helper.assertTrue(lost.size() == 1 && lost.getFirst().message().english().contains("Cobblestone"),
                            "the network's log says what was lost; got " + lost);
                })
                .thenSucceed();
    }

    /** With the server's key off, a sudden shutdown gives everything back, as switching off does. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = TestComputerEnergy.NO_LOSS_BATCH)
    public static void powerLoss_withTheKeyOffGivesEverythingBack(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final NetworkInsertOperation[] op = new NetworkInsertOperation[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.isRunning() && mainframe.networkUuid() != null,
                        "the Mainframe runs its network"))
                .thenExecuteAfter(SETTLE, () -> op[0] = mainframe.submitNetworkInsert(Items.COBBLESTONE, DEMAND,
                        "test"))
                .thenExecuteAfter(1, () -> mainframe.stopSuddenly(null, CrashPages.Moment.none()))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(op[0].isDone(), "the INSERT settles with the Mainframe");
                    helper.assertTrue(op[0].leftover() == DEMAND - op[0].writtenTotal(),
                            "what it did not write goes back; leftover " + op[0].leftover());
                })
                .thenSucceed();
    }

    /**
     * Frames 11 gone dark with its disk in use checks it as it next comes up, under its mark, for the two seconds
     * NTFS's journal takes, and the disk is clean again once it is up.
     */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void powerLoss_theNextStartChecksTheDiskByItsJournal(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        checkedAtTheNextStart(helper, pc, DiskCheck.Look.FRAMES_11, FileSystemType.NTFS, 40);
    }

    /** UNIX reads its UFS from end to end, no less than five seconds, with fsck's five passes on its console. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void powerLoss_unixReadsItsDiskFromEndToEnd(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC,
                ResourceLocation.fromNamespaceAndPath("jsc", "unix"));
        checkedAtTheNextStart(helper, pc, DiskCheck.Look.UNIX, FileSystemType.UFS, 100);
    }

    /** MC-DOS checks nothing, as the real one did not: it comes up and leaves the mark for a system that does. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void powerLoss_mcDosChecksNothingAndLeavesTheMark(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC,
                ResourceLocation.fromNamespaceAndPath("jsc", "mc_dos"));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "MC-DOS comes up"))
                .thenExecute(() -> {
                    pc.stopSuddenly(null, CrashPages.Moment.none());
                    helper.assertTrue(DiskChecks.unclean(pc.diskInSlot(0)), "its disk is left unclean");
                    pc.togglePower();
                })
                .thenWaitUntil(() -> helper.assertTrue(pc.booting() || up(pc), "it starts again"))
                .thenExecute(() -> helper.assertTrue(pc.diskCheck().isEmpty(), "with no check of its disk"))
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "and comes up"))
                .thenExecute(() -> helper.assertTrue(DiskChecks.unclean(pc.diskInSlot(0)),
                        "leaving the mark where it was"))
                .thenSucceed();
    }

    /** A clean shutdown leaves the disks clean, and the next start checks nothing. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void cleanShutdown_leavesTheDisksClean(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the computer comes up"))
                .thenExecute(() -> {
                    pc.togglePower();
                    helper.assertFalse(pc.isRunning() || pc.stoppedSuddenly(), "switched off, cleanly");
                    helper.assertFalse(DiskChecks.unclean(pc.diskInSlot(0)), "its disk is clean");
                })
                .thenSucceed();
    }

    /** The Local header's readings: the parts' watts, what the line gives them, and the machine's id by its kind. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void localHeader_showsTheDrawAndTheId(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.partsWatts() > 0 && pc.lineWatts() > pc.partsWatts(),
                            "the line gives more than the parts draw: " + pc.lineWatts() + " W for "
                                    + pc.partsWatts() + " W");
                    final String id = pc.machineId();
                    helper.assertTrue(id.matches("PC-[0-9a-f]{6}") && id.endsWith(pc.nodeUuid().asString()
                            .substring(0, 6)), "its id is its kind's mark and its short id; got " + id);
                })
                .thenSucceed();
    }

    /*
     * Brings {@code pc} up, takes its power suddenly, and starts it again: its disk is unclean in between, the start
     * checks it as {@code look} shows a check of {@code type} for {@code ticks}, and it is clean once the system is up.
     */
    private static void checkedAtTheNextStart(final GameTestHelper helper, final PersonalComputerBlockEntity pc,
                                              final DiskCheck.Look look, final FileSystemType type,
                                              final int ticks) {
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the computer comes up"))
                .thenExecute(() -> {
                    pc.stopSuddenly(null, CrashPages.Moment.none());
                    helper.assertFalse(pc.isRunning(), "it goes dark");
                    helper.assertTrue(pc.stoppedSuddenly(), "suddenly");
                    helper.assertTrue(DiskChecks.unclean(pc.diskInSlot(0)), "leaving its disk unclean");
                    pc.togglePower();
                })
                .thenWaitUntil(() -> helper.assertTrue(pc.booting() && !pc.diskCheck().isEmpty(),
                        "its next start begins with a check of the disk"))
                .thenExecute(() -> {
                    final DiskCheck check = pc.diskCheck();
                    helper.assertTrue(check.look() == look && check.type() == type, "as its system checks "
                            + type.displayName() + "; got " + check);
                    helper.assertTrue(ticks >= 100 ? check.ticks() >= ticks : check.ticks() == ticks,
                            "for as long as its file system takes; got " + check.ticks());
                    helper.assertTrue(pc.bootTotal() > check.ticks(), "at the head of the coming-up");
                })
                .thenWaitUntil(() -> helper.assertTrue(up(pc), "the system comes up after it"))
                .thenExecute(() -> helper.assertFalse(DiskChecks.unclean(pc.diskInSlot(0)), "and the disk is clean"))
                .thenSucceed();
    }

    private static boolean up(final PersonalComputerBlockEntity pc) {
        return pc.isRunning() && !pc.needsPost() && !pc.booting() && !pc.atBootMenu();
    }

    private static List<NetworkEvent> events(final GameTestHelper helper, final NetworkUuid network,
                                             final EventLevel level, final String startsWith) {
        return NetworkEventLog.of(helper.getLevel()).newest(network, helper.getLevel().getGameTime(), any -> true,
                        NetworkEventLog.MOST).stream()
                .filter(event -> event.level() == level && event.message().english().startsWith(startsWith))
                .toList();
    }
}
