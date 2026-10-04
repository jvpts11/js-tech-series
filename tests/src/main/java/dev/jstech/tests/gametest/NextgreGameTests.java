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
import dev.jstech.computers.crafting.CraftRouting;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkCraftOperation;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.engine.nextgre.NextgreEngine;
import dev.jstech.computers.engine.nextgre.NextgrePlanView;
import dev.jstech.computers.engine.nextgre.NextgrePlanner;
import dev.jstech.computers.engine.nextgre.NextgreStatement;
import dev.jstech.computers.engine.nextgre.NextgreTexts;
import dev.jstech.computers.operation.INetworkOperation;
import dev.jstech.computers.operation.payload.NextgreActionPayload;
import dev.jstech.computers.operation.payload.NextgreStudioPayload;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.operation.payload.nextgre.NextgrePayloads;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.network.ServerNode;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftFiles;
import dev.jstech.tests.testkit.TestPlanner;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * NextgreIQL, the explicit engine: in the version of each age from the Legacy on; its EXPLAIN, which shows the plan
 * and runs nothing, and its EXPLAIN ANALYZE, which runs it and measures each step; its hints, which change where the
 * raw materials come from and how much runs at once; the choice between plans by what they cost; its rules, switched
 * off per Mainframe; what another mod adds to its planner; and the Planner Studio's answer.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NextgreGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 14;
    private static final int ROWS = 64;
    private static final ResourceLocation NEXTGRE = NetworkEngines.NEXTGRE_IQL.program();
    private static final String CRAFT_PLANKS = "CRAFT 8 oak_planks";

    private NextgreGameTests() {
    }

    @GameTest(template = ARENA)
    public static void nextgre_installsFromTheLegacyInTheVersionOfItsAge(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        final List<Age> ages = List.of(new Age(ComputingModule.VINTAGE_MAINFRAME.get(), null),
                new Age(ComputingModule.LEGACY_MAINFRAME.get(), "7.0"),
                new Age(ComputingModule.TRANSITION_MAINFRAME.get(), "8.3"),
                new Age(ComputingModule.MAINFRAME.get(), "9.0"),
                new Age(ComputingModule.ADVANCED_MAINFRAME.get(), "16"));
        for (final Age age : ages) {
            helper.setBlock(at, age.block());
            if (!(helper.getBlockEntity(at) instanceof MainframeBlockEntity mainframe)) {
                throw new IllegalStateException("no Mainframe at " + at);
            }
            final HardwareEra era = mainframe.mainframeEra();
            final boolean installed = mainframe.installEngine(NEXTGRE);
            helper.assertTrue(installed == (age.version() != null),
                    "NextgreIQL on a " + era + " Mainframe: " + installed);
            if (age.version() != null) {
                helper.assertValueEqual(mainframe.installedEngines().get(NEXTGRE), age.version(),
                        "the version of NextgreIQL a " + era + " Mainframe installs");
            }
            helper.setBlock(at, Blocks.AIR);
        }
        helper.succeed();
    }

    /** EXPLAIN shows the plan weighed and chosen, with another mod's rule and note in it, and runs nothing. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void explain_showsThePlanAndRunsNothing(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    final IqlEngine.Outcome outcome = query(helper, net, "EXPLAIN " + CRAFT_PLANKS);
                    helper.assertTrue(outcome.ok(), "EXPLAIN answers; said " + outcome.message());
                    final List<String> steps = column(outcome, 0);
                    helper.assertTrue(steps.get(0).startsWith("Craft Oak Planks x8"),
                            "the craft at the top; got " + steps);
                    helper.assertTrue(steps.stream().anyMatch(step -> step.contains("Pull Oak Log x2")),
                            "the two logs it draws; got " + steps);
                    helper.assertTrue(steps.stream().anyMatch(step -> step.contains(TestPlanner.NOTE + "oak_planks")),
                            "another mod's note under the step; got " + steps);
                    final NextgrePlanView plan = engine(net).history(net.mainframe()).get(0);
                    helper.assertTrue(plan.state() == NextgrePlanView.PLANNED && plan.chosen() != null,
                            "planned and not run");
                    helper.assertTrue(plan.chosen().notes().stream().map(GameText::resolve)
                                    .anyMatch(TestPlanner.RULE_NOTE::equals),
                            "another mod's rule weighed it; notes " + plan.chosen().notes());
                    helper.assertTrue(crafts(net.mainframe()).isEmpty() && indexed(net, Items.OAK_LOG) == 8L,
                            "and nothing runs or is taken");
                })
                .thenSucceed();
    }

    /** EXPLAIN ANALYZE runs the craft, and the plan fills in with what each step took, which the planner keeps. */
    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void explainAnalyze_runsTheCraftAndMeasuresIt(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    final IqlEngine.Outcome outcome = query(helper, net, "EXPLAIN ANALYZE " + CRAFT_PLANKS);
                    helper.assertTrue(outcome.ok(), "EXPLAIN ANALYZE answers; said " + outcome.message());
                    helper.assertTrue(engine(net).history(net.mainframe()).get(0).state() == NextgrePlanView.RUNNING,
                            "the plan runs");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        engine(net).history(net.mainframe()).get(0).state() == NextgrePlanView.DONE,
                        "the craft finishes"))
                .thenExecute(() -> {
                    final NextgrePlanView plan = engine(net).history(net.mainframe()).get(0);
                    final NextgrePlanView.Node root = plan.nodes().get(0);
                    helper.assertTrue(root.finished() && root.actual() >= 0 && plan.executionTicks() >= 0,
                            "the whole craft's time is known; root " + root);
                    helper.assertTrue(net.storage(helper.getLevel()).count(StorageKey.of(Items.OAK_PLANKS)) == 8L,
                            "the planks were made");
                    helper.assertTrue(statistics(net).stream().anyMatch(line -> line.contains("Oak Planks")
                                    && line.contains("a run")),
                            "the planks' time is kept for the next plan; " + statistics(net));
                })
                .thenSucceed();
    }

    /** PREFER SOURCE takes the raw materials from the named server, though another holds them too. */
    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void preferSource_takesFromTheNamedServer(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = twoServerNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    final String second = serverLabel(helper, net, 1);
                    final IqlEngine.Outcome outcome = query(helper, net,
                            CRAFT_PLANKS + " PREFER SOURCE '" + second + "'");
                    helper.assertTrue(outcome.ok(), "the craft starts; said " + outcome.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        net.storage(helper.getLevel()).count(StorageKey.of(Items.OAK_PLANKS)) == 8L, "planks made"))
                .thenExecute(() -> {
                    helper.assertTrue(held(net, 1, Items.OAK_LOG) == 2L && held(net, 0, Items.OAK_LOG) == 4L,
                            "the logs came from the preferred server; it holds " + held(net, 1, Items.OAK_LOG)
                                    + ", the other " + held(net, 0, Items.OAK_LOG));
                })
                .thenSucceed();
    }

    /** AVOID SOURCE takes nothing from the named server while another one has it. */
    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void avoidSource_takesFromAnother(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = twoServerNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> helper.assertTrue(query(helper, net,
                        CRAFT_PLANKS + " AVOID SOURCE '" + serverLabel(helper, net, 0) + "'").ok(), "the craft starts"))
                .thenWaitUntil(() -> helper.assertTrue(
                        net.storage(helper.getLevel()).count(StorageKey.of(Items.OAK_PLANKS)) == 8L, "planks made"))
                .thenExecute(() -> helper.assertTrue(held(net, 0, Items.OAK_LOG) == 4L
                                && held(net, 1, Items.OAK_LOG) == 2L,
                        "nothing came from the avoided server; it holds " + held(net, 0, Items.OAK_LOG)))
                .thenSucceed();
    }

    /** MAX PARALLEL goes with the craft, and the plan marks the hint where it changed it. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void maxParallel_goesWithTheCraftAndIsMarked(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    helper.assertTrue(query(helper, net, CRAFT_PLANKS + " MAX PARALLEL 1").ok(), "the craft starts");
                    final List<NetworkCraftOperation> running = crafts(net.mainframe());
                    helper.assertTrue(running.size() == 1 && running.get(0).routing().maxParallel() == 1,
                            "the craft runs one stage at a time");
                    final NextgrePlanView plan = engine(net).history(net.mainframe()).get(0);
                    helper.assertTrue(plan.nodes().get(0).hints().contains("MAX PARALLEL 1"),
                            "the hint is marked where it changed the plan; " + plan.nodes().get(0).hints());
                })
                .thenSucceed();
    }

    /** A hint that cannot be followed is refused with what was wrong, and so is an EXPLAIN of something else. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void hints_thatCannotBeFollowedAreRefused(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    expectRefused(helper, query(helper, net, CRAFT_PLANKS + " MAX PARALLEL 0"),
                            NextgreTexts.BAD_PARALLEL.text());
                    expectRefused(helper, query(helper, net, CRAFT_PLANKS + " PREFER SOURCE 'Nowhere'"),
                            NextgreTexts.NO_SERVER.with("Nowhere"));
                    expectRefused(helper, query(helper, net, "EXPLAIN QUERY items"),
                            NextgreTexts.EXPLAINS_ONLY.text());
                    expectRefused(helper, query(helper, net, "EXPLAIN"), NextgreTexts.EXPLAIN_NEEDS.text());
                    helper.assertTrue(crafts(net.mainframe()).isEmpty(), "and nothing runs");
                    helper.assertTrue(query(helper, net, "QUERY items").ok(), "the language's core still answers");
                })
                .thenSucceed();
    }

    /** The Midsoft IQL Server does not speak NextgreIQL's words: they belong to that vendor. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void midsoft_doesNotSpeakNextgresDialect(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(query(helper, net, "EXPLAIN " + CRAFT_PLANKS).ok(),
                            "the Midsoft IQL Server refuses EXPLAIN");
                    helper.assertFalse(query(helper, net, CRAFT_PLANKS + " MAX PARALLEL 2").ok(),
                            "and the hints");
                })
                .thenSucceed();
    }

    /** The planner keeps the cheaper of a bench plan and a machine plan, by the times it measured. */
    @GameTest(template = ARENA)
    public static void planner_keepsTheCheaperOfBenchAndMachine(final GameTestHelper helper) {
        final StorageKey ingot = StorageKey.of(Items.IRON_INGOT);
        final List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, new ItemStack(Items.IRON_NUGGET)));
        final CraftingPattern bench = new CraftingPattern(grid, new ItemStack(Items.IRON_INGOT));
        final ProcessingPattern furnace = CraftFiles.furnaceIron(200);
        final Map<StorageKey, Long> stock = Map.of(StorageKey.of(Items.IRON_NUGGET), 9L,
                StorageKey.of(Items.RAW_IRON), 1L);
        final String benchKey = "bench:" + ingot.id();
        final String machineKey = "machine:" + ingot.id();

        final NextgrePlanner.Weighed slowMachine = NextgrePlanner.weigh(inputs(ingot, bench, furnace, stock,
                Map.of(benchKey, 5L, machineKey, 5000L), Set.of()));
        helper.assertTrue(slowMachine.best() != null && !slowMachine.best().machineFirst(),
                "with a slow machine the bench plan wins; " + costs(slowMachine));

        final NextgrePlanner.Weighed slowBench = NextgrePlanner.weigh(inputs(ingot, bench, furnace, stock,
                Map.of(benchKey, 5000L, machineKey, 5L), Set.of()));
        helper.assertTrue(slowBench.best() != null && slowBench.best().machineFirst(),
                "with a slow bench the machine plan wins; " + costs(slowBench));

        final NextgrePlanner.Weighed noMachines = NextgrePlanner.weigh(inputs(ingot, bench, furnace, stock,
                Map.of(benchKey, 5000L, machineKey, 5L), Set.of(NextgrePlanner.WEIGH_MACHINES)));
        helper.assertTrue(noMachines.best() != null && !noMachines.best().machineFirst(),
                "with the rule off, the machine plan is never weighed; " + costs(noMachines));
        helper.succeed();
    }

    /** A rule switched off is kept off on the Mainframe, in its save, and its effect is gone. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void rules_switchedOffStayOffAndAreLeftOut(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    final NextgreEngine engine = engine(net);
                    helper.assertTrue(engine.toggleRule(net.mainframe(), TestPlanner.RULE.toString()),
                            "another mod's rule can be switched");
                    helper.assertFalse(engine.toggleRule(net.mainframe(), "nowhere:no_rule"),
                            "a rule nobody added cannot");
                    helper.assertTrue(engine.rules(net.mainframe()).stream()
                                    .anyMatch(rule -> rule.id().equals(TestPlanner.RULE.toString()) && !rule.on()),
                            "the rule is listed off");
                    query(helper, net, "EXPLAIN " + CRAFT_PLANKS);
                    final NextgrePlanView plan = engine.history(net.mainframe()).get(0);
                    helper.assertFalse(plan.chosen().notes().stream().map(GameText::resolve)
                            .anyMatch(TestPlanner.RULE_NOTE::equals), "a rule switched off weighs nothing");
                    final CompoundTag saved = net.mainframe().saveWithoutMetadata(helper.getLevel().registryAccess());
                    helper.assertTrue(saved.getCompound("EngineData").getCompound(NEXTGRE.toString())
                                    .getList("Off", Tag.TAG_STRING).toString()
                                    .contains(TestPlanner.RULE.toString()),
                            "the Mainframe's save keeps it off");
                })
                .thenSucceed();
    }

    /** Another mod's hint changes the plans of the statement that uses it, and is marked on the plan. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void addedHint_changesThePlansOfItsStatement(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    query(helper, net, "EXPLAIN " + CRAFT_PLANKS);
                    final long plain = engine(net).history(net.mainframe()).get(0).chosen().cost();
                    final IqlEngine.Outcome boosted = query(helper, net,
                            "EXPLAIN " + CRAFT_PLANKS + " " + TestPlanner.HINT + " 500");
                    helper.assertTrue(boosted.ok(), "the hint is read; said " + boosted.message());
                    final NextgrePlanView plan = engine(net).history(net.mainframe()).get(0);
                    helper.assertTrue(plan.chosen().cost() == plain + 500,
                            "it added what it was given; " + plain + " then " + plan.chosen().cost());
                    helper.assertTrue(plan.nodes().get(0).hints().contains(TestPlanner.HINT + " 500"),
                            "and is marked on the plan; " + plan.nodes().get(0).hints());
                })
                .thenSucceed();
    }

    /** ANALYZE gathers the statistics, which list what the planner reckons with, another mod's too. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void analyze_gathersWhatThePlannerReckonsWith(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    // The index is looked after from the Mainframe, as the network's own maintenance always is.
                    final IqlEngine.Outcome outcome = net.mainframe().networkOperations().query(IqlEngine.viewOf(
                            new ServerCliComputer(net.mainframe(), helper.getLevel())), "ANALYZE", ROWS);
                    helper.assertTrue(outcome.ok() && outcome.message().startsWith("statistics gathered"),
                            "ANALYZE says what it gathered; said " + outcome.message());
                    final List<String> lines = statistics(net);
                    helper.assertTrue(lines.stream().anyMatch(line -> line.startsWith("Item types indexed")
                                    && !line.endsWith(" 0")), "the item types are counted; " + lines);
                    helper.assertTrue(lines.stream().anyMatch(line -> line.contains(TestPlanner.STATISTIC_VALUE)),
                            "another mod's statistic is listed; " + lines);
                })
                .thenSucceed();
    }

    /** EXPLAIN SELECT shows which servers would hand the items over, and moves nothing. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void explainSelect_showsWhereTheItemsComeFrom(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    final IqlEngine.Outcome outcome = query(helper, net, "EXPLAIN SELECT 4 oak_log");
                    helper.assertTrue(outcome.ok(), "EXPLAIN SELECT answers; said " + outcome.message());
                    final List<String> steps = column(outcome, 0);
                    helper.assertTrue(steps.get(0).startsWith("Index Seek Oak Log x4")
                                    && steps.stream().anyMatch(step -> step.contains("Pull Oak Log x4")),
                            "the read and the server it reads from; " + steps);
                    helper.assertTrue(indexed(net, Items.OAK_LOG) == 8L, "nothing moved");
                })
                .thenSucceed();
    }

    /** The Planner Studio is answered with the plan it asked for, the planner's rules and its statistics. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void studio_isAnsweredWithThePlanRulesAndStatistics(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> {
                    final NextgreStudioPayload answer = NextgrePayloads.act(helper.getLevel(), net.cc(), null,
                            new NextgreActionPayload(net.cc().getBlockPos(), 5, NextgreActionPayload.EXPLAIN,
                                    "EXPLAIN " + CRAFT_PLANKS));
                    helper.assertTrue(answer.ok() && answer.window() == 5, "answered for the window that asked; "
                            + GameText.resolve(answer.message()));
                    helper.assertTrue(answer.plan().isPresent() && !answer.plan().get().nodes().isEmpty(),
                            "with the plan");
                    helper.assertTrue(GameText.resolve(answer.engine()).startsWith("NextgreIQL 9.0"),
                            "and the engine with its version; " + GameText.resolve(answer.engine()));
                    helper.assertTrue(answer.rules().stream().anyMatch(rule -> rule.id().equals(
                                    NextgrePlanner.WEIGH_MACHINES))
                                    && answer.rules().stream().anyMatch(rule -> rule.id().equals(
                                    NextgreStatement.PREFER_SOURCE))
                                    && answer.rules().stream().anyMatch(rule -> rule.id().equals(
                                    TestPlanner.RULE.toString())),
                            "its own rules, its hints and another mod's rule");
                    helper.assertTrue(answer.history().size() == 1, "and the plan in its history");
                    net.mainframe().activateEngine(NetworkEngines.MIDSOFT_IQL_SERVER.program());
                    final NextgreStudioPayload refused = NextgrePayloads.act(helper.getLevel(), net.cc(), null,
                            new NextgreActionPayload(net.cc().getBlockPos(), 5, NextgreActionPayload.REFRESH, ""));
                    helper.assertTrue(!refused.ok() && GameText.resolve(refused.message())
                                    .startsWith("No compatible NextgreIQL"),
                            "on another engine it finds no NextgreIQL; " + GameText.resolve(refused.message()));
                })
                .thenSucceed();
    }

    /** A craft any window asks for is planned by weighing, kept in the history and measured as it runs. */
    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void anyCraft_isWeighedKeptAndMeasured(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper, 8);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net, 8L))
                .thenExecute(() -> helper.assertTrue(query(helper, net, CRAFT_PLANKS).ok(),
                        "a plain CRAFT goes to the engine"))
                .thenWaitUntil(() -> helper.assertTrue(
                        net.storage(helper.getLevel()).count(StorageKey.of(Items.OAK_PLANKS)) == 8L, "planks made"))
                .thenWaitUntil(() -> {
                    final List<NextgrePlanView> history = engine(net).history(net.mainframe());
                    helper.assertTrue(!history.isEmpty() && history.get(0).state() == NextgrePlanView.DONE,
                            "the craft's plan is kept and settles done");
                })
                .thenSucceed();
    }

    /* A crafting network running NextgreIQL, with the planks recipe and {@code logs} logs in its server. */
    private static TestWorldBuilder.CraftingNetwork planksNetwork(final GameTestHelper helper, final int logs) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.runAfterDelay(SETTLE, () -> {
            net.mainframe().installEngine(NEXTGRE);
            net.mainframe().activateEngine(NEXTGRE);
            net.cc().loadPattern(CraftFiles.oakPlanks());
            net.seed(Items.OAK_LOG, logs);
        });
        return net;
    }

    /* The same with a second server in the rack, and four logs on each. */
    private static TestWorldBuilder.CraftingNetwork twoServerNetwork(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.runAfterDelay(SETTLE, () -> {
            TestWorldBuilder.mountDefaultServer(net.rack(), 1);
            net.mainframe().installEngine(NEXTGRE);
            net.mainframe().activateEngine(NEXTGRE);
            net.cc().loadPattern(CraftFiles.oakPlanks());
            net.rack().getServerStorage(0).insert(Items.OAK_LOG, 4);
            net.rack().getServerStorage(1).insert(Items.OAK_LOG, 4);
        });
        return net;
    }

    /* Whether the network runs NextgreIQL, knows the planks recipe and indexes {@code logs} logs. */
    private static void ready(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net,
                              final long logs) {
        helper.assertTrue(net.mainframe().runningEngine() instanceof NextgreEngine, "NextgreIQL runs");
        helper.assertFalse(net.mainframe().networkPatterns().isEmpty(), "the network knows the planks recipe");
        helper.assertTrue(indexed(net, Items.OAK_LOG) == logs, "the network indexes the logs");
    }

    private static IqlEngine.Outcome query(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net,
                                           final String statement) {
        return net.mainframe().networkOperations().query(
                IqlEngine.viewOf(new ServerCliComputer(net.cc(), helper.getLevel())), statement, ROWS);
    }

    private static NextgreEngine engine(final TestWorldBuilder.CraftingNetwork net) {
        final INetworkEngine engine = NetworkEngines.get(NEXTGRE);
        if (engine instanceof NextgreEngine nextgre) {
            return nextgre;
        }
        throw new IllegalStateException("NextgreIQL is not registered");
    }

    private static List<String> statistics(final TestWorldBuilder.CraftingNetwork net) {
        final List<String> out = new ArrayList<>();
        engine(net).statistics(net.mainframe()).forEach(row -> out.add(GameText.resolve(row.name()) + " "
                + GameText.resolve(row.value())));
        return out;
    }

    private static List<String> column(final IqlEngine.Outcome outcome, final int column) {
        final List<String> out = new ArrayList<>();
        for (final List<Text> row : outcome.table().rows()) {
            out.add(column < row.size() ? GameText.resolve(row.get(column)).strip() : "");
        }
        return out;
    }

    private static void expectRefused(final GameTestHelper helper, final IqlEngine.Outcome outcome,
                                      final Text expected) {
        helper.assertTrue(!outcome.ok() && outcome.message().equals(expected.english()),
                "refused with \"" + expected.english() + "\"; got " + outcome.ok() + " \"" + outcome.message() + "\"");
    }

    private static List<NetworkCraftOperation> crafts(final MainframeBlockEntity mainframe) {
        final List<NetworkCraftOperation> out = new ArrayList<>();
        for (final INetworkOperation operation : mainframe.liveOperations()) {
            if (operation instanceof NetworkCraftOperation craft) {
                out.add(craft);
            }
        }
        return out;
    }

    private static long indexed(final TestWorldBuilder.CraftingNetwork net, final Item item) {
        return net.mainframe().networkIndex().snapshot().getOrDefault(StorageKey.of(item), 0L);
    }

    private static long held(final TestWorldBuilder.CraftingNetwork net, final int slot, final Item item) {
        return net.rack().getServerStorage(slot).count(StorageKey.of(item));
    }

    private static String serverLabel(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net,
                                      final int slot) {
        final NodeUuid node = serverIn(helper, net, slot);
        if (node == null) {
            throw new IllegalStateException("no server in slot " + slot);
        }
        return NetworkLookup.serverLabel(helper.getLevel(), node);
    }

    @Nullable
    private static NodeUuid serverIn(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net,
                                     final int slot) {
        final NetworkSystem system = NetworkSystem.get(helper.getLevel());
        for (final ServerNode node : system.serversOf(net.mainframe().networkUuid())) {
            if (system.locationOf(node.nodeUuid()).map(at -> at.slot() == slot).orElse(false)) {
                return node.nodeUuid();
            }
        }
        return null;
    }

    private static NextgrePlanner.Inputs inputs(final StorageKey key, final CraftingPattern bench,
                                                final ProcessingPattern machine, final Map<StorageKey, Long> stock,
                                                final Map<String, Long> perRun, final Set<String> off) {
        return new NextgrePlanner.Inputs(key, 1, false, List.of(bench), List.of(machine), stock, Map.of(), 9L, 2,
                perRun, off, new NextgreStatement(false, false, "", List.of()), CraftRouting.NONE, List.of(),
                List.of(), List.of());
    }

    private static List<String> costs(final NextgrePlanner.Weighed weighed) {
        final List<String> out = new ArrayList<>();
        weighed.candidates().forEach(candidate -> out.add(GameText.resolve(candidate.description()) + "="
                + candidate.cost()));
        return out;
    }

    /** An age's Mainframe, and the version of NextgreIQL it installs, or null when it installs none. */
    private record Age(Block block, @Nullable String version) {
    }
}
