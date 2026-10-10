/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.block.part.ImportBusPart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.machine.IqlTables;
import dev.jstech.computers.operation.index.IndexHealth;
import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.IsmsActionPayload;
import dev.jstech.computers.operation.payload.IsmsPlanPayload;
import dev.jstech.computers.operation.payload.IsmsSchemaPayload;
import dev.jstech.computers.operation.payload.RunIqlPayload;
import dev.jstech.computers.operation.payload.iql.IqlPayloads;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.trace.IsmsTraces;
import dev.jstech.computers.trace.TraceEvent;
import dev.jstech.computers.trace.TraceEventClass;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftFiles;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestEngines;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The IQL Server Management Studio on the server: a statement answers the window, the tab and the place in the script
 * that sent it, with the real columns of the table it read; the disks table, WHERE and ORDER BY on any column and the
 * IF guard; a statement longer than a packet cut, not crashing; the network as the Object Explorer shows it; what the
 * studio asks beside statements; a craft's estimated plan; and what the Profiler hears of the network's work.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IsmsGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final String WHO = "steve";
    private static final BlockPos BUS_CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos BUS_CHEST = new BlockPos(2, 2, 3);

    private IsmsGameTests() {
    }

    /** A statement's answer carries the window, the tab and the place in the script it answers, and its table. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_answersTheWindowAndTheTabThatAsked(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> net.seed(Items.COBBLESTONE, 200))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.COBBLESTONE)
                        == 200L, "the network holds the cobblestone"))
                .thenExecute(() -> {
                    final IqlResultPayload first = run(helper, net, 5, 3, 2, "QUERY items");
                    helper.assertTrue(first.window() == 5 && first.tab() == 3 && first.seq() == 2,
                            "the answer names the window, the tab and the statement that asked");
                    helper.assertTrue(first.ok(), "the read goes: " + GameText.resolve(first.message()));
                    helper.assertTrue(first.columns().equals(IqlTables.ITEMS),
                            "the items table comes back with its real columns; got " + first.columns());
                    helper.assertTrue(first.rows().stream().anyMatch(row -> cell(row, 0).equals("cobblestone")
                            && cell(row, 1).equals("200")), "with the cobblestone and how many");
                    final IqlResultPayload second = run(helper, net, 6, 1, 0, "QUERY items");
                    helper.assertTrue(second.window() == 6, "a second window gets its own answer");
                })
                .thenSucceed();
    }

    /** The disks table lists each server's disks, with its own columns. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_readsTheDisksTable(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final IqlResultPayload disks = run(helper, net, 1, 1, 0, "QUERY disks");
                    helper.assertTrue(disks.columns().equals(IqlTables.DISKS),
                            "the disks table has its columns; got " + disks.columns());
                    helper.assertFalse(disks.rows().isEmpty(), "and the rack's server's disk is in it");
                })
                .thenSucceed();
    }

    /** WHERE and ORDER BY work on the table's columns, not only on how many. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_filtersAndSortsOnTheColumns(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    net.seed(Items.COBBLESTONE, 200);
                    net.seed(Items.DIRT, 50);
                })
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.DIRT) == 50L,
                        "the network holds both"))
                .thenExecute(() -> {
                    final IqlResultPayload many = run(helper, net, 1, 1, 0, "QUERY items WHERE qty > 100");
                    helper.assertTrue(many.rows().size() == 1 && cell(many.rows().get(0), 0).equals("cobblestone"),
                            "WHERE qty keeps the cobblestone alone; got " + many.rows().size());
                    final IqlResultPayload named = run(helper, net, 1, 1, 0, "QUERY items WHERE item = \"dirt\"");
                    helper.assertTrue(named.rows().size() == 1, "WHERE on the item column keeps the dirt");
                    final IqlResultPayload fewFirst = run(helper, net, 1, 1, 0, "QUERY items ORDER BY qty");
                    helper.assertTrue(cell(fewFirst.rows().get(0), 0).equals("dirt"),
                            "ORDER BY qty puts the fewer first");
                    final IqlResultPayload byName = run(helper, net, 1, 1, 0, "QUERY items ORDER BY item DESC");
                    helper.assertTrue(cell(byName.rows().get(0), 0).equals("dirt"),
                            "ORDER BY item DESC sorts by the item's name, dirt before cobblestone");
                    final IqlResultPayload servers = run(helper, net, 1, 1, 0, "QUERY servers");
                    helper.assertTrue(servers.columns().equals(IqlTables.SERVERS) && !servers.rows().isEmpty(),
                            "the servers table has its own columns and rows");
                })
                .thenSucceed();
    }

    /** An IF that does not hold keeps the action from running at all. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_anIfThatDoesNotHoldRunsNothing(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> net.seed(Items.DIRT, 50))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.DIRT) == 50L,
                        "the network holds the dirt"))
                .thenExecute(() -> {
                    final IqlResultPayload held = run(helper, net, 1, 1, 0, "SELECT 5 dirt IF qty > 1000");
                    helper.assertTrue(held.ok() && GameText.resolve(held.message()).contains("IF did not hold"),
                            "the IF does not hold, so nothing runs; got " + GameText.resolve(held.message()));
                    helper.assertTrue(held.started().isEmpty(), "and no Operation is started");
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(indexed(net, Items.DIRT)
                        == 50L, "the dirt stays where it was"))
                .thenSucceed();
    }

    /** A statement that sets work going names the Operations it started, so the tab can stop them. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void run_namesTheOperationsItStarted(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> net.seed(Items.DIRT, 50))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.DIRT) == 50L,
                        "the network holds the dirt"))
                .thenExecute(() -> {
                    final IqlResultPayload sent = run(helper, net, 1, 1, 0, "SELECT 5 dirt");
                    helper.assertTrue(sent.ok(), "the SELECT is taken: " + GameText.resolve(sent.message()));
                    helper.assertTrue(sent.started().size() == 1, "it names the one Operation it started; got "
                            + sent.started());
                })
                .thenSucceed();
    }

    /** A statement longer than a packet carries is cut on the way, and the packet still goes. */
    @GameTest(template = ARENA)
    public static void statement_longerThanAPacketIsCutNotCrashing(final GameTestHelper helper) {
        final String longest = "QUERY items WHERE item = \"" + "a".repeat(600) + "\"";
        final RunIqlPayload payload = new RunIqlPayload(BlockPos.ZERO, BlockPos.ZERO, 1, 1, 0, longest);
        helper.assertTrue(payload.statement().length() == RunIqlPayload.MAX_LEN,
                "the statement is cut to what a packet carries");
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        RunIqlPayload.STREAM_CODEC.encode(buf, payload);
        helper.assertTrue(RunIqlPayload.STREAM_CODEC.decode(buf).statement().equals(payload.statement()),
                "and travels whole");
        helper.succeed();
    }

    /** The Object Explorer's network: every table's rows, the engine, the servers, the index and the locks. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void schema_countsEveryTableAndNamesTheEngine(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> net.seed(Items.COBBLESTONE, 64))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.COBBLESTONE)
                        == 64L, "the network holds the cobblestone"))
                .thenExecute(() -> {
                    run(helper, net, 1, 1, 0, "LOCK 10 cobblestone");
                    final IsmsSchemaPayload schema = IqlPayloads.ismsSchema(helper.getLevel(), net.cc(), 9);
                    helper.assertTrue(schema.window() == 9, "the schema answers the window that asked");
                    helper.assertTrue(schema.tableRows().size() == IqlTables.TABLES.size(),
                            "a count for every table");
                    helper.assertTrue(schema.tableRows().get(0) >= 1, "the items table counts the cobblestone");
                    helper.assertTrue(schema.engine().state() == IsmsSchemaPayload.EngineState.RUNNING
                            && schema.engine().compatible() && schema.engine().version().equals("2012"),
                            "the Standard Mainframe's Midsoft IQL Server 2012 runs; got " + schema.engine());
                    helper.assertFalse(schema.servers().isEmpty(), "the rack's server is listed");
                    helper.assertTrue(schema.index().state() == IndexHealth.State.OK, "the index is healthy");
                    helper.assertTrue(schema.locks().stream().anyMatch(lock -> lock.item().equals(
                            "minecraft:cobblestone") && lock.qty() == 10L), "the cobblestone held by hand is listed");
                    helper.assertTrue(schema.host().equals(net.cc().hostname()), "the computer is named");
                })
                .thenSucceed();
    }

    /** With another engine running the network, the studio is told it has no compatible server, and which runs. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void schema_saysWhenAnotherEngineRuns(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    net.mainframe().installEngine(TestEngines.PLAIN);
                    helper.assertTrue(net.mainframe().activateEngine(TestEngines.PLAIN), "the other engine runs");
                    final IsmsSchemaPayload.Engine engine = IqlPayloads.ismsSchema(helper.getLevel(), net.cc(), 1)
                            .engine();
                    helper.assertFalse(engine.compatible(), "the studio finds no compatible Midsoft IQL Server");
                    helper.assertFalse(engine.running().isEmpty(), "and is told which engine runs instead");
                })
                .thenSucceed();
    }

    /** The studio stops and starts the network's Midsoft IQL Server. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void action_stopsAndStartsTheEngine(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer computer = new ServerCliComputer(net.cc(), helper.getLevel());
                    helper.assertTrue(act(net, computer, IsmsActionPayload.ENGINE_STOP, "").ok(), "it stops");
                    helper.assertFalse(net.mainframe().engineRunning(), "the engine is stopped");
                    helper.assertTrue(IqlPayloads.ismsSchema(helper.getLevel(), net.cc(), 1).engine().state()
                            == IsmsSchemaPayload.EngineState.STOPPED, "and the explorer says so");
                    helper.assertTrue(act(net, computer, IsmsActionPayload.ENGINE_START, "").ok(), "it starts");
                    helper.assertTrue(net.mainframe().engineRunning(), "the engine runs again");
                })
                .thenSucceed();
    }

    /** The Automation Agent's jobs are paused, started and deleted from the studio. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void action_pausesStartsAndDeletesAJob(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MainframeBlockEntity mainframe = net.mainframe();
                    mainframe.iqlCatalog().put(new IqlSavedObject(IqlDefinition.ObjectType.JOB, "nightly",
                            "VACUUM", IqlDefinition.TriggerKind.EVERY, "20m"));
                    final ServerCliComputer computer = new ServerCliComputer(net.cc(), helper.getLevel());
                    helper.assertTrue(act(net, computer, IsmsActionPayload.JOB_PAUSE, "nightly").ok(), "paused");
                    helper.assertTrue(mainframe.isJobPaused("nightly"), "the job is paused");
                    final IsmsSchemaPayload.Job listed = IqlPayloads.ismsSchema(helper.getLevel(), net.cc(), 1).jobs()
                            .get(0);
                    helper.assertTrue(listed.paused() && listed.trigger().equals("EVERY 20m")
                            && listed.body().equals("VACUUM"), "the explorer lists it paused, with its trigger");
                    helper.assertTrue(act(net, computer, IsmsActionPayload.JOB_START, "nightly").ok(), "started");
                    helper.assertFalse(mainframe.isJobPaused("nightly"), "the job runs again");
                    helper.assertTrue(act(net, computer, IsmsActionPayload.JOB_DELETE, "nightly").ok(), "deleted");
                    helper.assertFalse(mainframe.iqlCatalog().contains(IqlDefinition.ObjectType.JOB, "nightly"),
                            "the job is gone");
                    helper.assertFalse(act(net, computer, IsmsActionPayload.JOB_PAUSE, "nightly").ok(),
                            "a job that is not there cannot be paused");
                })
                .thenSucceed();
    }

    /** An item held by hand is let go from the studio. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void action_unlocksAnItemHeldByHand(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> net.seed(Items.COBBLESTONE, 64))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.COBBLESTONE)
                        == 64L, "the network holds the cobblestone"))
                .thenExecute(() -> {
                    final IqlResultPayload locked = run(helper, net, 1, 1, 0, "LOCK 10 cobblestone");
                    helper.assertFalse(net.mainframe().lockedTypes().isEmpty(), "the cobblestone is held; got "
                            + GameText.resolve(locked.message()));
                    final ServerCliComputer computer = new ServerCliComputer(net.cc(), helper.getLevel());
                    helper.assertTrue(act(net, computer, IsmsActionPayload.UNLOCK, "cobblestone").ok(), "let go");
                    helper.assertTrue(net.mainframe().lockedTypes().isEmpty(), "nothing is held any more");
                })
                .thenSucceed();
    }

    /**
     * An Operation a statement started is stopped by its short id, as Cancel Executing Query stops it: here a SELECT
     * waiting for cobblestone held by hand, so it is still there to be stopped.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void action_cancelStopsAnOperation(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> net.seed(Items.COBBLESTONE, 64))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.COBBLESTONE)
                        == 64L, "the network holds the cobblestone"))
                .thenExecute(() -> {
                    helper.assertTrue(run(helper, net, 1, 1, 0, "LOCK 64 cobblestone").ok(), "it is held by hand");
                    final IqlResultPayload sent = run(helper, net, 1, 1, 0, "SELECT 64 cobblestone");
                    helper.assertTrue(sent.started().size() == 1, "one Operation is started, and waits");
                    final ServerCliComputer computer = new ServerCliComputer(net.cc(), helper.getLevel());
                    final ICliComputer.OpResult stopped = act(net, computer, IsmsActionPayload.CANCEL,
                            sent.started().get(0));
                    helper.assertTrue(stopped.ok(), "it is stopped by its short id: " + stopped.message());
                })
                .thenSucceed();
    }

    /** The estimated plan of a craft says how it would be made from now, and makes nothing. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void plan_estimatesACraftWithoutMakingIt(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    net.cc().loadPattern(CraftFiles.oakPlanks());
                    net.seed(Items.OAK_LOG, 4);
                })
                .thenWaitUntil(() -> helper.assertFalse(net.mainframe().networkPatterns().isEmpty(),
                        "the network knows the planks recipe"))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.OAK_LOG) == 4L,
                        "the network holds the logs"))
                .thenExecute(() -> {
                    final IsmsPlanPayload plan = IqlPayloads.plan(net.mainframe(), 3, "oak_planks", 8);
                    helper.assertTrue(plan.ok() && plan.window() == 3, "the plan is made for the window that asked; "
                            + plan.lines().stream().map(GameText::resolve).toList());
                    helper.assertTrue(plan.depth().get(0) == 0 && plan.depth().contains(1) && plan.depth().contains(2),
                            "root first, then the steps, then what it reads; got " + plan.depth());
                    helper.assertTrue(indexed(net, Items.OAK_LOG) == 4L
                            && indexed(net, Items.OAK_PLANKS) == 0L, "and nothing is made");
                    helper.assertFalse(IqlPayloads.plan(net.mainframe(), 3, "beacon", 1).ok(),
                            "an item no recipe makes has no plan");
                })
                .thenSucceed();
    }

    /** The Profiler hears a statement come and go, the Operation it starts, settle, and the lock it takes. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void trace_hearsStatementsOperationsAndLocks(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final List<TraceEvent> heard = new ArrayList<>();
        final Runnable[] stop = new Runnable[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> net.seed(Items.DIRT, 50))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.DIRT) == 50L,
                        "the network holds the dirt"))
                .thenExecute(() -> {
                    stop[0] = IsmsTraces.listen(net.mainframe().networkUuid(), TraceEventClass.Group.ALL, heard::add);
                    run(helper, net, 1, 1, 0, "SELECT 5 dirt");
                })
                .thenWaitUntil(() -> helper.assertTrue(kinds(heard).contains(TraceEventClass.OPERATION_SETTLED),
                        "the Operation settles; heard " + kinds(heard)))
                .thenExecute(() -> {
                    stop[0].run();
                    helper.assertTrue(kinds(heard).containsAll(List.of(TraceEventClass.STATEMENT_STARTING,
                            TraceEventClass.STATEMENT_COMPLETED, TraceEventClass.OPERATION_CREATED,
                            TraceEventClass.LOCK_ACQUIRED)), "and every step is heard; got " + kinds(heard));
                    final TraceEvent starting = heard.stream().filter(e -> e.kind()
                            == TraceEventClass.STATEMENT_STARTING).findFirst().orElseThrow();
                    helper.assertTrue(starting.requester().equals(WHO)
                            && starting.computer().equals(net.cc().hostname()), "said by who ran it, and where");
                    final TraceEvent settled = heard.stream().filter(e -> e.kind()
                            == TraceEventClass.OPERATION_SETTLED).findFirst().orElseThrow();
                    helper.assertTrue(settled.items() == 5L && !settled.detail().isEmpty(),
                            "the settled Operation says how many it moved and how; got " + settled);
                    final int before = heard.size();
                    run(helper, net, 1, 1, 0, "QUERY items");
                    helper.assertTrue(heard.size() == before, "a trace that ended hears nothing more");
                })
                .thenSucceed();
    }

    /** A trace that picks only the statements hears nothing else. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void trace_hearsOnlyTheGroupsItPicks(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final List<TraceEvent> heard = new ArrayList<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> net.seed(Items.DIRT, 50))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.DIRT) == 50L,
                        "the network holds the dirt"))
                .thenExecute(() -> {
                    final Runnable stop = IsmsTraces.listen(net.mainframe().networkUuid(),
                            TraceEventClass.Group.STATEMENTS.bit(), heard::add);
                    run(helper, net, 1, 1, 0, "SELECT 5 dirt");
                    stop.run();
                    helper.assertFalse(heard.isEmpty(), "the statements are heard");
                    helper.assertTrue(heard.stream().allMatch(e -> e.kind().group()
                            == TraceEventClass.Group.STATEMENTS), "and nothing but; got " + kinds(heard));
                })
                .thenSucceed();
    }

    /** The Profiler hears the plan the engine chose for a craft. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void trace_hearsThePlanChosenForACraft(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        final List<TraceEvent> heard = new ArrayList<>();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    net.cc().loadPattern(CraftFiles.oakPlanks());
                    net.seed(Items.OAK_LOG, 4);
                })
                .thenWaitUntil(() -> helper.assertFalse(net.mainframe().networkPatterns().isEmpty(),
                        "the network knows the planks recipe"))
                .thenWaitUntil(() -> helper.assertTrue(indexed(net, Items.OAK_LOG) == 4L,
                        "the network holds the logs"))
                .thenExecute(() -> {
                    final Runnable stop = IsmsTraces.listen(net.mainframe().networkUuid(),
                            TraceEventClass.Group.PLANS.bit(), heard::add);
                    run(helper, net, 1, 1, 0, "CRAFT 4 oak_planks");
                    stop.run();
                    helper.assertTrue(kinds(heard).contains(TraceEventClass.PLAN_CHOSEN),
                            "the chosen plan is heard; got " + kinds(heard));
                })
                .thenSucceed();
    }

    /** The Profiler hears what an Import Bus brings into the network. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void trace_hearsWhatTheBusesMoved(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(new BlockPos(1, 2, 2));
        TestCables.lay(helper, BUS_CABLE, ComputingModule.HBW_CABLE);
        world.placeSeededRack(new BlockPos(2, 2, 1));
        final List<TraceEvent> heard = new ArrayList<>();
        final Runnable[] stop = new Runnable[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    stop[0] = IsmsTraces.listen(mainframe.networkUuid(), TraceEventClass.Group.BUSES.bit(),
                            heard::add);
                    helper.setBlock(BUS_CHEST, Blocks.BARREL);
                    if (helper.getBlockEntity(BUS_CHEST) instanceof Container barrel) {
                        barrel.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
                    }
                    final ImportBusPart bus = ComputingParts.IMPORT.get().create();
                    TestCables.cable(helper, BUS_CABLE).addPart(Direction.SOUTH, bus);
                })
                .thenWaitUntil(() -> helper.assertFalse(heard.isEmpty(), "the bus's move is heard"))
                .thenExecute(() -> {
                    stop[0].run();
                    final String said = GameText.resolve(heard.get(0).text());
                    helper.assertTrue(heard.get(0).kind() == TraceEventClass.BUS_MOVED && said.contains("cobblestone"),
                            "as a bus moving the cobblestone; got " + said);
                })
                .thenSucceed();
    }

    private static IqlResultPayload run(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net,
                                        final int window, final int tab, final int seq, final String statement) {
        return IqlPayloads.runStatement(helper.getLevel(), net.cc(), WHO,
                new RunIqlPayload(net.cc().getBlockPos(), BlockPos.ZERO, window, tab, seq, statement));
    }

    private static ICliComputer.OpResult act(final TestWorldBuilder.CraftingNetwork net,
                                             final ServerCliComputer computer, final int action,
                                             final String target) {
        return IqlPayloads.act(new IsmsActionPayload(BlockPos.ZERO, net.cc().getBlockPos(), 1, action, target, 0),
                net.mainframe(), computer);
    }

    /* How many of {@code item} the network's index knows of, which is what a lock, a pull and a plan read. */
    private static long indexed(final TestWorldBuilder.CraftingNetwork net, final Item item) {
        return net.mainframe().networkIndex().snapshot().getOrDefault(StorageKey.of(item), 0L);
    }

    private static String cell(final List<Text> row, final int column) {
        return column < row.size() ? GameText.resolve(row.get(column)) : "";
    }

    private static List<TraceEventClass> kinds(final List<TraceEvent> events) {
        return events.stream().map(TraceEvent::kind).toList();
    }
}
