/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.block.part.ImportBusPart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.IsmsSchemaPayload;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.storage.ServerStore;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestCli;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningMainframe;
import static dev.jstech.tests.testkit.NetworkFixtures.placeRunningPC;
import static dev.jstech.tests.testkit.NetworkFixtures.runIql;

/**
 * GameTests for IQL against a data network: the Command Prompt, ISMS, saved objects, scheduled jobs, named bus
 * imports and exports, and the schema a view carries.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NetworkIqlGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private NetworkIqlGameTests() {
    }

    @GameTest(template = ARENA)
    public static void personalComputer_selectsItemsFromNetwork(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos pc = new BlockPos(5, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH)); // rear faces the cable to the north
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    // Seed the Server with 200 cobblestone.
                    final ServerStore store = rackBe.getServerStorage(0);
                    store.insert(Items.COBBLESTONE, 200);

                    helper.assertTrue(computer.networkUuid() != null, "PC must be on the network");
                    // The PC resolves the SAME network as the server's Rack across the Router.
                    final NetworkUuid net = computer.networkUuid();
                    final NetworkStorage ns = NetworkStorage.of(helper.getLevel(), net);
                    helper.assertTrue(ns.count(Items.COBBLESTONE) == 200,
                            "the PC's network should see the server's 200 cobblestone; got "
                                    + ns.count(Items.COBBLESTONE));
                    final ItemStackHandler dest = new ItemStackHandler(9);
                    final long moved = ns.select(Items.COBBLESTONE, 100, new dev.jstech.computers.storage.ExternalDataPort(dest, null));
                    helper.assertTrue(moved == 100, "SELECT should move 100 via the PC's network; got " + moved);
                    helper.assertTrue(ns.count(Items.COBBLESTONE) == 100,
                            "network should have 100 left after SELECT");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void commandPrompt_runsAgainstTheNetwork(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos pc = new BlockPos(5, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3); // behind the cable (rear-only connection)
        placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 200);
                    final var cli = new dev.jstech.computers.program.ServerCliComputer(
                            (dev.jstech.computers.terminal.IComputerTerminalHost) computer,
                            helper.getLevel());
                    final var shell = dev.jstech.computers.program.cli.CliCommands.newShell(50);

                    helper.assertTrue(TestCli.contains(shell.run("whoami", cli), "Personal Computer"),
                            "whoami should report the computer kind");
                    helper.assertTrue(TestCli.contains(shell.run("status", cli), "ONLINE"),
                            "status should report the running computer as online");
                    helper.assertTrue(TestCli.contains(shell.run("operation query items", cli), "cobblestone"),
                            "operation query items should list the network's cobblestone");
                    helper.assertTrue(
                            TestCli.contains(
                                    shell.run("operation query items WHERE name contains diamond", cli), "no rows"),
                            "operation query with a non-matching filter should say so");
                    helper.assertTrue(
                            TestCli.contains(shell.run("operation select 50 cobblestone", cli), "SELECT queued"),
                            "operation select should queue an operation through the network");
                    helper.assertTrue(
                            TestCli.contains(shell.run("operation select 50 not_a_real_item", cli), "unknown item"),
                            "operation select of an unknown item should be reported, not crash");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void isms_runsParsedIqlAgainstTheNetwork(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos pc = new BlockPos(5, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3);
        placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 200);
                    final var cli = new dev.jstech.computers.program.ServerCliComputer(
                            (dev.jstech.computers.terminal.IComputerTerminalHost) computer,
                            helper.getLevel());
                    final var read = dev.jstech.computers.program.iql.IqlParser.tryParse(
                            "QUERY items");
                    helper.assertTrue(read.ok(), "the read statement must parse");
                    helper.assertFalse(cli.queryObject("items", null, "", 64).isEmpty(),
                            "QUERY items must return the network's rows");
                    helper.assertFalse(cli.queryObject("servers", null, "", 64).isEmpty(),
                            "QUERY servers must list the rack's server");

                    final var selectAll = dev.jstech.computers.program.iql.IqlParser.tryParse(
                            "SELECT *");
                    helper.assertTrue(selectAll.ok(), "SELECT * must parse");
                    helper.assertTrue(cli.execute(selectAll.operation()).ok(),
                            "SELECT * must queue an extraction for every item type");

                    final var pull = dev.jstech.computers.program.iql.IqlParser.tryParse(
                            "SELECT 50 cobblestone");
                    helper.assertTrue(pull.ok(), "the pull statement must parse");
                    helper.assertTrue(cli.execute(pull.operation()).ok(),
                            "executing the pull must queue an operation");

                    // The Object Explorer snapshot must mirror the real network, not a static example tree.
                    final var schema = dev.jstech.computers.operation.payload.iql.IqlPayloads
                            .ismsSchema(helper.getLevel(),
                                    (dev.jstech.computers.terminal.IComputerTerminalHost) computer, 1);
                    helper.assertTrue(schema.network().english().startsWith("jsc-net-"),
                            "the Object Explorer must show the real network label");
                    helper.assertFalse(schema.servers().isEmpty(),
                            "the Object Explorer must list the rack's real server");
                    helper.assertTrue(schema.tableRows().get(0) >= 1,
                            "the Object Explorer must count the rows of the items table");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void iqlEngine_storesAndRunsSavedObjects(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos router = new BlockPos(3, 2, 2);
        final BlockPos eth = new BlockPos(4, 2, 2);
        final BlockPos pc = new BlockPos(5, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(router, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, eth, ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity computer = placeRunningPC(helper, pc);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        final var viewType = dev.jstech.computers.program.iql.IqlDefinition.ObjectType.VIEW;
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 200);
                    final var cli = new dev.jstech.computers.program.ServerCliComputer(
                            (dev.jstech.computers.terminal.IComputerTerminalHost) computer,
                            helper.getLevel());
                    /*
                     * The Mainframe ships with the engine. Taken off, it goes back on from its disc at the Mainframe,
                     * as the disc's setup does, and not from any computer with no disc at all.
                     */
                    mainframe.uninstallEngine(NetworkEngines.MIDSOFT_IQL_SERVER.program());
                    helper.assertFalse(cli.install("iqlengine").ok(),
                            "'install iqlengine' at a computer with no disc installs nothing");
                    helper.assertTrue(mainframe.installEngine(NetworkEngines.MIDSOFT_IQL_SERVER.program()),
                            "the Engine goes back on the Mainframe");
                    helper.assertTrue(cli.iqlEngineInstalled(),
                            "the computer must report the Engine installed");

                    helper.assertTrue(runIql(mainframe, cli, "CREATE VIEW stock AS QUERY items").ok(),
                            "CREATE VIEW must succeed");
                    helper.assertTrue(mainframe.iqlCatalog().contains(viewType, "stock"),
                            "the catalog must hold the created view");

                    // The studio's Object Explorer snapshot must reflect the real catalog, not mock examples.
                    final var schema = dev.jstech.computers.operation.payload.iql.IqlPayloads
                            .ismsSchema(helper.getLevel(),
                                    (dev.jstech.computers.terminal.IComputerTerminalHost) computer, 1);
                    helper.assertTrue(schema.views().stream().anyMatch(view -> view.name().equals("stock")),
                            "the studio's Object Explorer must list the created view");
                    helper.assertTrue(schema.engine().state() == IsmsSchemaPayload.EngineState.RUNNING,
                            "the studio must show the Engine as running");

                    final var query = runIql(mainframe, cli, "QUERY stock");
                    helper.assertTrue(query.ok(), "QUERY <view> must run the saved query: " + query.message());
                    helper.assertFalse(query.rows().isEmpty(), "QUERY <view> must return the network's rows");

                    // QUERY * returns every item; the full WHERE really filters by qty now (cobblestone = 200).
                    helper.assertFalse(runIql(mainframe, cli, "QUERY *").rows().isEmpty(),
                            "QUERY * must return all items");
                    helper.assertFalse(runIql(mainframe, cli, "QUERY items WHERE qty > 100").rows().isEmpty(),
                            "WHERE qty > 100 must keep the 200 cobblestone");
                    helper.assertTrue(runIql(mainframe, cli, "QUERY items WHERE qty > 1000").rows().isEmpty(),
                            "WHERE qty > 1000 must filter out the 200 cobblestone");

                    helper.assertTrue(runIql(mainframe, cli,
                                    "CREATE PROCEDURE refresh AS { QUERY items; QUERY servers }").ok(),
                            "CREATE PROCEDURE must succeed");
                    helper.assertTrue(runIql(mainframe, cli, "EXEC refresh").ok(),
                            "EXEC must run the procedure's statements in order");

                    // A stopped engine plans nothing, so the network answers no statement until it starts again.
                    mainframe.setEngineRunning(false);
                    helper.assertFalse(runIql(mainframe, cli, "CREATE VIEW v2 AS QUERY items").ok(),
                            "a CREATE must fail when the Engine is stopped");
                    mainframe.setEngineRunning(true);

                    helper.assertTrue(runIql(mainframe, cli, "DROP VIEW stock").ok(), "DROP VIEW must succeed");
                    helper.assertFalse(mainframe.iqlCatalog().contains(viewType, "stock"),
                            "the view must be gone after DROP");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void iqlJobAgent_firesScheduledJobs(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos hbw = new BlockPos(2, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, hbw, ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 8, () -> {
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 1_000_000L);
                    // Jobs are the Automation Engine's to keep and fire.
                    mainframe.installAutomationEngine();
                    final ServerCliComputer machine = new ServerCliComputer(mainframe, helper.getLevel());
                    // EVERY 1t: the agent (evaluating every 10 ticks) fires this within a couple of evaluations.
                    helper.assertTrue(runIql(mainframe, machine, "CREATE JOB drainer AS DROP 100 cobblestone EVERY 1t")
                                    .ok(), "CREATE JOB must succeed");
                })
                .thenExecuteAfter(60, () -> helper.assertTrue(
                        mainframe.completedOps() > 0 || !mainframe.recentOperations().isEmpty(),
                        "the EVERY job must have fired its DROP operation by now"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void iql_deleteToNamedBusExports(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        final BlockPos cableEnd = new BlockPos(4, 2, 2);
        if (helper.getBlockEntity(cableEnd) instanceof CableBlockEntity cable) {
            final ExportBusPart bus = new ExportBusPart(HardwareEra.STANDARD);
            cable.addPart(Direction.EAST, bus);
            bus.setName("out"); // no filter, so it never auto-exports; the query drives it by name
        }
        final BlockPos barrel = new BlockPos(5, 2, 2);
        helper.setBlock(barrel, net.minecraft.world.level.block.Blocks.BARREL);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () ->
                        rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 200))
                .thenExecuteAfter(10, () -> {
                    // Run the export only after the network has indexed the server's stock.
                    final var outcome = runIql(mainframe, new ServerCliComputer(mainframe, helper.getLevel()),
                            "DELETE cobblestone TO out");
                    helper.assertTrue(outcome.ok(), "DELETE TO a named bus should be accepted: " + outcome.message());
                })
                .thenExecuteAfter(40, () -> {
                    long inBarrel = 0L;
                    if (helper.getBlockEntity(barrel) instanceof net.minecraft.world.Container c) {
                        for (int i = 0; i < c.getContainerSize(); i++) {
                            if (c.getItem(i).is(Items.COBBLESTONE)) {
                                inBarrel += c.getItem(i).getCount();
                            }
                        }
                    }
                    helper.assertTrue(inBarrel > 0L, "DELETE TO a named bus must export into its inventory; got " + inBarrel);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void iql_insertFromNamedBusImports(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(3, 2, 2), ComputingModule.HBW_CABLE);
        TestCables.lay(helper, new BlockPos(4, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        final BlockPos cableEnd = new BlockPos(4, 2, 2);
        if (helper.getBlockEntity(cableEnd) instanceof CableBlockEntity cable) {
            final ImportBusPart bus = new ImportBusPart(HardwareEra.STANDARD);
            cable.addPart(Direction.EAST, bus);
            bus.setName("in");
            bus.toggleMode(); // redstone mode: with no signal it never auto-imports, so the query drives it
        }
        final BlockPos barrel = new BlockPos(5, 2, 2);
        helper.setBlock(barrel, net.minecraft.world.level.block.Blocks.BARREL);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    if (helper.getBlockEntity(barrel) instanceof net.minecraft.world.Container c) {
                        c.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
                    }
                    final var outcome = runIql(mainframe, new ServerCliComputer(mainframe, helper.getLevel()),
                            "INSERT cobblestone FROM in");
                    helper.assertTrue(outcome.ok(), "INSERT FROM a named bus should be accepted: " + outcome.message());
                })
                .thenExecuteAfter(80, () -> {
                    final long net = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.COBBLESTONE);
                    helper.assertTrue(net > 0L, "INSERT FROM a named bus must import into the network; got " + net);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void job_pausePreventsFiringUntilRestart(final GameTestHelper helper) {
        final BlockPos m = new BlockPos(1, 2, 2);
        final BlockPos rack = new BlockPos(2, 2, 3);
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, m);
        TestCables.lay(helper, new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        helper.setBlock(rack, ComputingModule.SERVER_RACK.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(rack) instanceof ServerRackBlockEntity rackBe)) {
            helper.fail("no server rack");
            return;
        }
        TestWorldBuilder.mountDefaultServer(rackBe, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    rackBe.getServerStorage(0).insert(Items.COBBLESTONE, 1000);
                    mainframe.installAutomationEngine();
                    runIql(mainframe, new ServerCliComputer(mainframe, helper.getLevel()),
                            "CREATE JOB killer AS DROP 64 cobblestone EVERY 5t");
                    mainframe.pauseJob("killer"); // paused from the start, so it must never fire
                })
                .thenExecuteAfter(40, () -> {
                    final long left = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.COBBLESTONE);
                    helper.assertTrue(left == 1000L, "a paused job must not fire; cobblestone left=" + left);
                    mainframe.restartJob("killer"); // resume + re-arm
                })
                .thenExecuteAfter(40, () -> {
                    final long left = NetworkStorage.of(helper.getLevel(), mainframe.networkUuid())
                            .count(Items.COBBLESTONE);
                    helper.assertTrue(left < 1000L, "a restarted job must fire again; cobblestone left=" + left);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void job_removeForgetsThePauseOfAJobCreatedAgainUnderTheSameName(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, new BlockPos(1, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.installAutomationEngine();
                    final var cli = new ServerCliComputer(mainframe, helper.getLevel());
                    runIql(mainframe, cli, "CREATE JOB tidy AS DROP 64 cobblestone EVERY 5t");
                    mainframe.pauseJob("tidy");
                    helper.assertTrue(mainframe.isJobPaused("tidy"), "the job should be paused before it is deleted");
                    helper.assertTrue(mainframe.deleteJob("tidy"), "removing a saved job should report it was there");
                    runIql(mainframe, cli, "CREATE JOB tidy AS DROP 64 cobblestone EVERY 5t");
                    helper.assertFalse(mainframe.isJobPaused("tidy"),
                            "a job created again under a deleted job's name must not start paused");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void ismsSchema_carriesWhatAViewRuns(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = placeRunningMainframe(helper, new BlockPos(1, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 6, () -> {
                    mainframe.iqlCatalog().put(new IqlSavedObject(IqlDefinition.ObjectType.VIEW, "low",
                            "QUERY items WHERE qty < 5", IqlDefinition.TriggerKind.NONE, ""));
                    final var schema = dev.jstech.computers.operation.payload.iql.IqlPayloads
                            .ismsSchema(helper.getLevel(), mainframe, 7);
                    helper.assertTrue(schema.window() == 7, "the schema answers the window that asked");
                    helper.assertTrue(schema.views().contains(new IsmsSchemaPayload.Saved("low",
                                    "QUERY items WHERE qty < 5")),
                            "the view travels with what it runs, so the studio can script it again; got: "
                                    + schema.views());
                })
                .thenSucceed();
    }
}
