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
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.machine.IqlService;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The network's language as it is read now: a definition typed at the prompt or written in a file is the engine's
 * to keep, an IF decides whether an action runs, an ORDER BY sorts before the LIMIT takes, a WHERE picks the
 * variants an action takes, and the engine's package installs from its disc like every other.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IqlLanguageGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 14;
    private static final int ROWS = 64;

    private IqlLanguageGameTests() {
    }

    /** A LIMIT above the item type cap, cut at the cap, is reported; a LIMIT within it, or no cut, is not. */
    @GameTest(template = ARENA)
    public static void isLimitCapped_onlyWhenTheLimitExceedsTheCapAndTheCapCut(final GameTestHelper helper) {
        if (!IqlService.isLimitCapped(500, 256)) {
            helper.fail("a LIMIT of 500 cut at 256 types must be reported");
        }
        if (IqlService.isLimitCapped(500, 100)) {
            helper.fail("a LIMIT of 500 that found 100 types was not cut");
        }
        if (IqlService.isLimitCapped(256, 256) || IqlService.isLimitCapped(0, 256)) {
            helper.fail("a LIMIT within the cap, or none, is not a cut to report");
        }
        helper.succeed();
    }

    /** CREATE at the prompt answers as the studio does, where it used to say "iql failed: null". */
    @GameTest(template = ARENA)
    public static void promptIql_createsAView(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final CliShell shell = CliCommands.newShell(cli.shellFamily(), 52);
                    final String said = text(shell.run("iql CREATE VIEW stock AS QUERY items", cli));
                    helper.assertTrue(said.contains("view stock created") && !said.contains("null"),
                            "the prompt hands a definition to the engine; got " + said);
                    helper.assertTrue(mainframe.iqlCatalog().contains(IqlDefinition.ObjectType.VIEW, "stock"),
                            "and the engine keeps it");
                })
                .thenSucceed();
    }

    /** A file of statements with a definition in it runs the definition, where it used to break. */
    @GameTest(template = ARENA)
    public static void iqlFile_runsTheDefinitionsItHolds(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    helper.assertTrue(cli.writeFile("setup.iql", "CREATE VIEW stock AS QUERY items\n").ok(),
                            "the file is written");
                    final ICliComputer.FsResult ran = cli.runScript("setup.iql");
                    helper.assertTrue(ran.ok(), "the file runs; got " + ran.message().english());
                    helper.assertTrue(mainframe.iqlCatalog().contains(IqlDefinition.ObjectType.VIEW, "stock"),
                            "and its view is kept");
                })
                .thenSucceed();
    }

    /** An IF reads the network's holding of the statement's item and decides whether the action runs at all. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void ifGuard_decidesWhetherTheActionRuns(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> rack(helper).getServerStorage(0).insert(Items.COBBLESTONE, 200))
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final IqlEngine.Outcome held = query(mainframe, cli, "DROP 10 cobblestone IF qty > 1000");
                    helper.assertTrue(held.ok() && held.message().contains("IF did not hold"),
                            "the guard does not hold at 200; got " + held.message());
                    helper.assertTrue(query(mainframe, cli, "DROP 10 cobblestone IF qty > 100").ok(),
                            "the guard holds at 200");
                })
                .thenWaitUntil(() -> helper.assertTrue(stock(helper, mainframe, Items.COBBLESTONE) == 190,
                        "only the guarded DROP that held ran; left " + stock(helper, mainframe, Items.COBBLESTONE)))
                .thenSucceed();
    }

    /** An ORDER BY sorts every row before the LIMIT takes the first ones. */
    @GameTest(template = ARENA)
    public static void orderBy_sortsBeforeTheLimitTakes(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rack(helper).getServerStorage(0).insert(Items.COBBLESTONE, 200);
                    rack(helper).getServerStorage(0).insert(Items.DIRT, 50);
                    rack(helper).getServerStorage(0).insert(Items.SAND, 100);
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    final List<ICliComputer.StoredItem> byName =
                            query(mainframe, cli, "QUERY items ORDER BY name LIMIT 2").rows();
                    helper.assertTrue(byName.size() == 2 && byName.get(0).name().english().equals("Cobblestone")
                                    && byName.get(1).name().english().equals("Dirt"),
                            "by name, the first two; got " + names(byName));
                    final List<ICliComputer.StoredItem> fewest =
                            query(mainframe, cli, "QUERY items ORDER BY qty LIMIT 1").rows();
                    helper.assertTrue(fewest.size() == 1 && fewest.get(0).quantity() == 50,
                            "by quantity, the fewest; got " + names(fewest));
                })
                .thenSucceed();
    }

    /** A WHERE on a SELECT picks the variants it takes, where it used to be read and ignored. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void where_picksTheVariantsASelectTakes(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        final ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
        worn.setDamageValue(100);
        final StorageKey damaged = StorageKey.of(worn);
        final StorageKey fresh = StorageKey.of(new ItemStack(Items.IRON_PICKAXE));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    rack(helper).getServerStorage(0).insert(damaged, 1);
                    rack(helper).getServerStorage(0).insert(fresh, 1);
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(query(mainframe,
                        new ServerCliComputer(mainframe, helper.getLevel()),
                        "SELECT 1 iron_pickaxe WHERE damaged = true").ok(), "the SELECT is taken"))
                .thenWaitUntil(() -> helper.assertTrue(mainframe.localSnapshot().getOrDefault(damaged, 0L) == 1L
                                && network(helper, mainframe).count(fresh) == 1L,
                        "the damaged pickaxe came out and the new one stayed"))
                .thenSucceed();
    }

    /** The engine installs from its disc like every program, where it used to switch on with none. */
    @GameTest(template = ARENA)
    public static void installIqlEngine_needsItsDisc(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.uninstallEngine(NetworkEngines.MIDSOFT_IQL_SERVER.program());
                    final ICliComputer.OpResult refused =
                            new ServerCliComputer(mainframe, helper.getLevel()).install("iqlengine");
                    helper.assertTrue(!refused.ok() && refused.message().english().contains("install disc"),
                            "with no disc there is nothing to install from; got " + refused.message().english());
                    helper.assertTrue(mainframe.installedEngines().isEmpty(), "and nothing was installed");
                })
                .thenSucceed();
    }

    private static IqlEngine.Outcome query(final MainframeBlockEntity mainframe, final ServerCliComputer machine,
                                           final String statement) {
        return mainframe.networkOperations().query(IqlEngine.viewOf(machine), statement, ROWS);
    }

    private static NetworkStorage network(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        return NetworkStorage.of(helper.getLevel(), mainframe.networkUuid());
    }

    private static long stock(final GameTestHelper helper, final MainframeBlockEntity mainframe, final Item item) {
        return network(helper, mainframe).count(item);
    }

    private static String names(final List<ICliComputer.StoredItem> rows) {
        return rows.stream().map(row -> row.name().english() + " " + row.quantity()).toList().toString();
    }

    private static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
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
