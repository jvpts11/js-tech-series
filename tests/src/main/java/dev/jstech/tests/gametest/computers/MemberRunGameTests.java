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
import dev.jstech.computers.operation.MemberRuns;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.iql.IqlTable;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.core.world.event.WorldEvents;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestNetworkMembers;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.StorageNetworkFixture;
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
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GameTests for the Mainframe giving another mod's kinds of Operation to the machines of its network: a {@code RUN}
 * from the network's language to the machine and through to the log, the machine named by its name or its place, the
 * refusals of a kind, a machine or a field that is not there, a request called off, the queue let go once the machine
 * has the work, and a job that runs a machine when a world event comes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MemberRunGameTests {

    private static final String ARENA = "empty";
    private static final int ROWS = 64;
    private static final long STOCK = 10L;
    /** How far ahead the beam storm is planned: it is warned of 20 ticks before, and lasts 40. */
    private static final int STORM_IN = 40;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos RACK = new BlockPos(3, 2, 2);
    private static final BlockPos FIRST = new BlockPos(2, 2, 1);
    private static final BlockPos SECOND = new BlockPos(2, 2, 3);
    private static final String KIND = "jstests:accelerate";

    private MemberRunGameTests() {
    }

    /**
     * A RUN goes to the machine, which works it over the ticks after with no queue of the Mainframe's held, and the
     * log keeps the kind, the machine and what was asked once the machine says it is done.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_givesTheRequestToTheMachineAndFollowsItToTheEnd(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.setBlock(FIRST, TestNetworkMembers.ACCELERATOR.get());
        final TestNetworkMembers.AcceleratorBlockEntity accelerator = accelerator(helper, FIRST);
        helper.startSequence()
                .thenWaitUntil(() -> able(helper, mainframe, 1))
                .thenExecute(() -> {
                    final IqlEngine.Outcome given = run(helper, mainframe, "RUN " + KIND + " WITH seconds = 1");
                    final String name = nameOf(helper, FIRST);
                    helper.assertTrue(given.ok() && given.message().equals("Accelerate queued for " + name),
                            "the request is queued for the accelerator: " + given.message());
                    helper.assertTrue(name.startsWith("Test accelerator-") && name.length() == 23,
                            "a machine with no name of its own goes by its block and its short id: " + name);
                })
                .thenWaitUntil(() -> helper.assertTrue(accelerator.running(), "the accelerator was given the beam"))
                .thenExecute(() -> {
                    final OperationRecord live = only(helper, mainframe.activeOperationRecords());
                    helper.assertTrue(live.type() == OperationRecord.TYPE_RUN && live.kind().equals(KIND)
                                    && live.status() == OperationRecord.STATUS_PROCESSING
                                    && live.asked().equals("seconds 1"),
                            "in flight as the accelerator's, running: " + live);
                    helper.assertTrue(mainframe.runningOps() == 0,
                            "and holding none of the Mainframe's queues: " + mainframe.runningOps());
                })
                .thenWaitUntil(() -> helper.assertTrue(accelerator.finished() == 1 && !logged(mainframe).isEmpty(),
                        "the beam ran its second and the log has it"))
                .thenExecute(() -> {
                    final OperationRecord done = logged(mainframe).getFirst();
                    helper.assertTrue(done.status() == OperationRecord.STATUS_COMPLETED
                                    && done.machine().equals(nameOf(helper, FIRST)) && done.ranTicks() >= 18,
                            "done, by the accelerator, after its second: " + done);
                    helper.assertTrue(done.name().getString().equals("Accelerate by " + done.machine()
                            + ": seconds 1"), "the row reads its kind, its machine and what was asked: "
                            + done.name().getString());
                    final IqlTable rows = run(helper, mainframe, "QUERY operations WHERE verb = 'RUN'").table();
                    helper.assertTrue(rows.rows().size() == 1, "a QUERY of the operations finds it: " + rows.rows());
                })
                .thenSucceed();
    }

    /** ON names the machine by the name a player gave it, by its block and short id, or by where it stands. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_onNamesTheMachineByItsNameOrItsPlace(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.setBlock(FIRST, TestNetworkMembers.ACCELERATOR.get());
        helper.setBlock(SECOND, TestNetworkMembers.ACCELERATOR.get());
        final TestNetworkMembers.AcceleratorBlockEntity first = accelerator(helper, FIRST);
        final TestNetworkMembers.AcceleratorBlockEntity second = accelerator(helper, SECOND);
        first.name("Beamline");
        helper.startSequence()
                .thenWaitUntil(() -> able(helper, mainframe, 2))
                .thenExecute(() -> {
                    final IqlEngine.Outcome byId = run(helper, mainframe, "RUN " + KIND + " ON '"
                            + nameOf(helper, SECOND) + "' WITH seconds = 0");
                    helper.assertTrue(byId.ok(), "by its block and short id: " + byId.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(second.finished() == 1 && first.finished() == 0,
                        "the run of no seconds went to the one it named"))
                .thenExecute(() -> {
                    helper.assertTrue(run(helper, mainframe, "RUN " + KIND + " ON 'beamline' WITH seconds = 5").ok(),
                            "the name a player gave it, in any case");
                    final String place = helper.absolutePos(SECOND).toShortString();
                    helper.assertTrue(run(helper, mainframe, "RUN " + KIND + " ON '" + place + "' WITH seconds = 5")
                            .ok(), "where it stands, as the at column writes it");
                    final IqlEngine.Outcome nowhere = run(helper, mainframe, "RUN " + KIND
                            + " ON 'Nowhere' WITH seconds = 1");
                    helper.assertTrue(!nowhere.ok()
                                    && nowhere.message().equals("no machine named Nowhere is on the network"),
                            "a name no machine has: " + nowhere.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(first.running() && second.running(),
                        "each went to the machine it named: " + first.running() + ", " + second.running()))
                .thenSucceed();
    }

    /** A kind nobody registered, and a network with no machine that carries it out, are refused with the reason. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void run_refusesAKindOrAMachineThatIsNotThere(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null, "the network forms"))
                .thenExecute(() -> {
                    final IqlEngine.Outcome none = run(helper, mainframe, "RUN " + KIND + " WITH seconds = 1");
                    helper.assertTrue(!none.ok()
                                    && none.message().equals("no machine on the network carries out Accelerate"),
                            "no accelerator on the network: " + none.message());
                    final IqlEngine.Outcome unknown = run(helper, mainframe, "RUN jstests:no_such_kind");
                    helper.assertTrue(!unknown.ok() && unknown.message()
                                    .equals("no mod registered a kind of Operation jstests:no_such_kind"),
                            "a kind nobody registered: " + unknown.message());
                    helper.assertTrue(mainframe.activeOperationRecords().isEmpty(), "and nothing is queued");
                })
                .thenSucceed();
    }

    /** A field the kind has not, a value it cannot read, and one past what it takes are refused, naming the field. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void run_refusesFieldsTheKindCannotRead(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.setBlock(FIRST, TestNetworkMembers.ACCELERATOR.get());
        helper.startSequence()
                .thenWaitUntil(() -> able(helper, mainframe, 1))
                .thenExecute(() -> {
                    final IqlEngine.Outcome speed = run(helper, mainframe, "RUN " + KIND + " WITH speed = 3");
                    helper.assertTrue(!speed.ok() && speed.message()
                                    .equals("Accelerate has no field speed; its fields are seconds"),
                            "a field it has not: " + speed.message());
                    final IqlEngine.Outcome words = run(helper, mainframe, "RUN " + KIND + " WITH seconds = 'fast'");
                    helper.assertTrue(!words.ok() && words.message().startsWith("the fields do not serve Accelerate"),
                            "words where it reads a number: " + words.message());
                    final IqlEngine.Outcome tooLong = run(helper, mainframe, "RUN " + KIND + " WITH seconds = 9000");
                    helper.assertTrue(!tooLong.ok()
                                    && tooLong.message().startsWith("the fields do not serve Accelerate"),
                            "more seconds than it runs: " + tooLong.message());
                    final IqlEngine.Outcome bare = run(helper, mainframe, "RUN " + KIND + " WITH seconds");
                    helper.assertTrue(!bare.ok() && bare.message().contains("name = value"),
                            "a field with no value does not read: " + bare.message());
                    helper.assertTrue(mainframe.activeOperationRecords().isEmpty(), "and nothing is queued");
                })
                .thenSucceed();
    }

    /** Calling a request off stops the machine and leaves it DISCARDED in the log. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_calledOffStopsTheMachine(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.setBlock(FIRST, TestNetworkMembers.ACCELERATOR.get());
        final TestNetworkMembers.AcceleratorBlockEntity accelerator = accelerator(helper, FIRST);
        final UUID[] given = new UUID[1];
        helper.startSequence()
                .thenWaitUntil(() -> able(helper, mainframe, 1))
                .thenExecute(() -> helper.assertTrue(run(helper, mainframe, "RUN " + KIND + " WITH seconds = 30")
                        .ok(), "a long beam is asked for"))
                .thenWaitUntil(() -> helper.assertTrue(accelerator.running(), "the accelerator runs it"))
                .thenExecute(() -> {
                    given[0] = only(helper, mainframe.activeOperationRecords()).id();
                    helper.assertTrue(mainframe.cancelOperation(given[0]), "it is called off");
                })
                .thenWaitUntil(() -> helper.assertTrue(!logged(mainframe).isEmpty(), "the log has it"))
                .thenExecute(() -> {
                    final OperationRecord off = logged(mainframe).getFirst();
                    helper.assertTrue(off.id().equals(given[0]) && off.status() == OperationRecord.STATUS_DISCARDED,
                            "discarded: " + off);
                    helper.assertTrue(!accelerator.running() && accelerator.stopped() == 1
                            && accelerator.finished() == 0, "and the accelerator stopped its beam");
                })
                .thenSucceed();
    }

    /**
     * A request waits at its priority for a queue like any other, and lets the queue go once the machine has it: a
     * single-queue Mainframe moves items while the accelerator still runs.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_letsItsQueueGoOnceTheMachineHasIt(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.setBlock(FIRST, TestNetworkMembers.ACCELERATOR.get());
        final TestNetworkMembers.AcceleratorBlockEntity accelerator = accelerator(helper, FIRST);
        final ItemStackHandler dest = new ItemStackHandler(9);
        helper.startSequence()
                .thenWaitUntil(() -> able(helper, mainframe, 1))
                .thenExecute(() -> {
                    rack(helper).getServerStorage(0).insert(Items.COBBLESTONE, STOCK);
                    helper.assertTrue(run(helper, mainframe, "RUN " + KIND + " WITH seconds = 30 PRIORITY HIGH").ok(),
                            "a long beam is asked for, high");
                    final OperationRecord queued = only(helper, mainframe.activeOperationRecords());
                    helper.assertTrue(queued.priority() == OperationPriority.HIGH
                            && queued.status() == OperationRecord.STATUS_PENDING, "queued, high: " + queued);
                })
                .thenWaitUntil(() -> helper.assertTrue(accelerator.running(), "the accelerator runs it"))
                .thenExecute(() -> helper.assertTrue(mainframe.networkOperations().pull(
                        StorageKey.of(Items.COBBLESTONE), 4L, StorageNetworkFixture.port(dest), "test") != null,
                        "items are asked for"))
                .thenWaitUntil(() -> helper.assertTrue(StorageNetworkFixture.count(dest) == 4,
                        "the items came while the beam runs: " + StorageNetworkFixture.count(dest)))
                .thenExecute(() -> helper.assertTrue(accelerator.running(), "and the beam is still running"))
                .thenSucceed();
    }

    /** A job waiting on a world event runs another mod's Operation on a machine of the network when it comes. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void aJob_runsAMachineWhenAWorldEventComes(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = network(helper);
        helper.setBlock(FIRST, TestNetworkMembers.ACCELERATOR.get());
        final TestNetworkMembers.AcceleratorBlockEntity accelerator = accelerator(helper, FIRST);
        helper.startSequence()
                .thenWaitUntil(() -> able(helper, mainframe, 1))
                .thenExecute(() -> {
                    // A storm a run before this one left under way, as the world was saved, is no storm of this test.
                    WorldEvents.stop(helper.getLevel(), TestNetworkMembers.BEAM_STORM);
                    mainframe.installAutomationEngine();
                    final IqlEngine.Outcome job = run(helper, mainframe, "CREATE JOB beam AS RUN " + KIND
                            + " WITH seconds = 1 WHEN EVENT jstests:test_beam_storm UNDER WAY");
                    helper.assertTrue(job.ok(), "the job is made: " + job.message());
                    helper.assertTrue(WorldEvents.plan(helper.getLevel(), TestNetworkMembers.BEAM_STORM, STORM_IN,
                            Optional.empty()), "the storm is planned");
                    helper.assertTrue(accelerator.finished() == 0 && !accelerator.running(), "nothing runs before");
                })
                .thenWaitUntil(() -> helper.assertTrue(accelerator.finished() == 1,
                        "the storm came and the job ran the beam: " + accelerator.finished()))
                // The Mainframe writes it down on its own tick, after the machine has said it is done.
                .thenWaitUntil(() -> helper.assertTrue(!logged(mainframe).isEmpty()
                        && logged(mainframe).getFirst().status() == OperationRecord.STATUS_COMPLETED,
                        "and the log has it done: " + logged(mainframe)))
                // The storm is let go of, so a world saved after this test holds none still under way.
                .thenExecute(() -> WorldEvents.stop(helper.getLevel(), TestNetworkMembers.BEAM_STORM))
                .thenSucceed();
    }

    /** A running Mainframe, a cable and a rack with one server behind it. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        TestCables.lay(helper, CABLE, ComputingModule.HBW_CABLE);
        TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK, Direction.EAST);
        return mainframe;
    }

    /* That {@code count} accelerators are on the Mainframe's network, able to take the kind. */
    private static void able(final GameTestHelper helper, final MainframeBlockEntity mainframe, final int count) {
        final int on = mainframe.networkUuid() == null ? 0 : NetworkSystem.get(helper.getLevel())
                .membersCarryingOut(mainframe.networkUuid(), KIND).size();
        helper.assertTrue(on == count, "accelerators on the network: " + on);
    }

    private static IqlEngine.Outcome run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String statement) {
        return mainframe.networkOperations().query(
                IqlEngine.viewOf(new ServerCliComputer(mainframe, helper.getLevel())), statement, ROWS);
    }

    /* The name the network calls the accelerator at {@code at} by. */
    private static String nameOf(final GameTestHelper helper, final BlockPos at) {
        return NetworkSystem.get(helper.getLevel()).memberAt(helper.absolutePos(at).asLong())
                .map(member -> MemberRuns.nameOf(helper.getLevel(), member)).orElse("");
    }

    /* The other mods' Operations the log has, newest first. */
    private static List<OperationRecord> logged(final MainframeBlockEntity mainframe) {
        return mainframe.recentOperations().stream().filter(OperationRecord::ofAnotherMod).toList();
    }

    private static OperationRecord only(final GameTestHelper helper, final List<OperationRecord> records) {
        helper.assertTrue(records.size() == 1, "one Operation in flight: " + records);
        return records.getFirst();
    }

    private static TestNetworkMembers.AcceleratorBlockEntity accelerator(final GameTestHelper helper,
                                                                         final BlockPos at) {
        if (helper.getBlockEntity(at) instanceof TestNetworkMembers.AcceleratorBlockEntity accelerator) {
            return accelerator;
        }
        throw new GameTestAssertException("no accelerator at " + at.toShortString());
    }

    private static ServerRackBlockEntity rack(final GameTestHelper helper) {
        if (helper.getBlockEntity(RACK) instanceof ServerRackBlockEntity rack) {
            return rack;
        }
        throw new GameTestAssertException("no rack at " + RACK.toShortString());
    }
}
