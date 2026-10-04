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
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.engine.prophet.ProphetEngine;
import dev.jstech.computers.engine.prophet.ProphetTexts;
import dev.jstech.computers.operation.payload.ProphetActionPayload;
import dev.jstech.computers.operation.payload.ProphetConsolePayload;
import dev.jstech.computers.operation.payload.prophet.ProphetPayloads;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftFiles;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Prophet YourIQL, the state-oriented engine: in the version of each age from the Legacy on; a state kept, made up
 * counting what is on its way and held; a state nothing can make, which says so; a watch, which fires once and makes
 * up to a level; states and watches let go and listed; a statement written wrong; reactions paused; what the
 * Mainframe keeps of it in its save; and the Reactive Console's answer.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProphetGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 14;
    private static final int ROWS = 64;
    private static final ResourceLocation PROPHET = NetworkEngines.PROPHET_YOURIQL.program();

    private ProphetGameTests() {
    }

    @GameTest(template = ARENA)
    public static void prophet_installsFromTheLegacyInTheVersionOfItsAge(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        for (final Age age : List.of(new Age(ComputingModule.VINTAGE_MAINFRAME.get(), null),
                new Age(ComputingModule.LEGACY_MAINFRAME.get(), "3.23"),
                new Age(ComputingModule.TRANSITION_MAINFRAME.get(), "5.0"),
                new Age(ComputingModule.MAINFRAME.get(), "5.6"),
                new Age(ComputingModule.ADVANCED_MAINFRAME.get(), "8.0"))) {
            helper.setBlock(at, age.block());
            if (!(helper.getBlockEntity(at) instanceof MainframeBlockEntity mainframe)) {
                throw new IllegalStateException("no Mainframe at " + at);
            }
            final HardwareEra era = mainframe.mainframeEra();
            helper.assertTrue(mainframe.installEngine(PROPHET) == (age.version() != null),
                    "Prophet YourIQL on a " + era + " Mainframe");
            if (age.version() != null) {
                helper.assertValueEqual(mainframe.installedEngines().get(PROPHET), age.version(),
                        "the version a " + era + " Mainframe installs");
            }
            helper.setBlock(at, Blocks.AIR);
        }
        helper.succeed();
    }

    /** A state kept is made up once, counting what is on its way, and then held. */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void keep_makesUpWhatItLacksOnceAndHolds(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net))
                .thenExecute(() -> {
                    final IqlEngine.Outcome kept = query(helper, net, "KEEP oak_planks >= 16");
                    helper.assertTrue(kept.ok() && kept.message().equals("Oak Planks kept at 16 or more"),
                            "KEEP answers; said " + kept.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(held(helper, net, Items.OAK_PLANKS) >= 16,
                        "the planks are made up to the level; held " + held(helper, net, Items.OAK_PLANKS)))
                .thenIdle(60)
                .thenExecute(() -> {
                    final List<ProphetEngine.StateRow> states = engine().states(net.mainframe());
                    helper.assertTrue(states.size() == 1 && GameText.resolve(states.get(0).status()).equals("Holding"),
                            "the state holds; " + states);
                    final List<ProphetEngine.ReactionRow> reactions = engine().reactions(net.mainframe());
                    helper.assertTrue(reactions.size() == 1, "it asked once, counting what was on its way; did "
                            + reactions.stream().map(r -> GameText.resolve(r.what())).toList());
                    helper.assertTrue(held(helper, net, Items.OAK_PLANKS) == 16L,
                            "and made no more than the level; held " + held(helper, net, Items.OAK_PLANKS));
                })
                .thenSucceed();
    }

    /** A state nothing on the network can make says it cannot hold, and why. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void keep_saysWhenNothingCanMakeIt(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net))
                .thenExecute(() -> helper.assertTrue(query(helper, net, "KEEP diamond >= 4").ok(), "kept"))
                .thenWaitUntil(() -> {
                    final List<ProphetEngine.StateRow> states = engine().states(net.mainframe());
                    helper.assertTrue(states.size() == 1
                                    && GameText.resolve(states.get(0).status()).equals("Cannot hold"),
                            "the state cannot hold; " + states);
                })
                .thenExecute(() -> helper.assertTrue(engine().reactions(net.mainframe()).stream()
                                .anyMatch(r -> GameText.resolve(r.what()).equals(
                                        "nothing on the network makes Diamond")),
                        "and says why"))
                .thenSucceed();
    }

    /** A watch fires once when its condition comes true, makes up to its level, and is armed again after. */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void watch_firesOnceAndMakesUpToItsLevel(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net))
                .thenExecute(() -> {
                    final IqlEngine.Outcome set = query(helper, net, "WATCH oak_planks < 4 DO CRAFT oak_planks TO 8");
                    helper.assertTrue(set.ok() && set.message().startsWith("watch 1 set"),
                            "the watch is set; said " + set.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(held(helper, net, Items.OAK_PLANKS) >= 8,
                        "it fired and made the planks up to 8; held " + held(helper, net, Items.OAK_PLANKS)))
                .thenWaitUntil(() -> {
                    final List<ProphetEngine.WatchRow> watches = engine().watches(net.mainframe());
                    helper.assertTrue(watches.size() == 1 && watches.get(0).times() == 1 && watches.get(0).armed(),
                            "fired once, and armed again once the planks were back over 4; " + watches);
                })
                .thenSucceed();
    }

    /** States and watches are listed, and let go. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void forget_letsGoAndShowStatesLists(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net))
                .thenExecute(() -> {
                    engine().configure(net.mainframe(), new ProphetEngine.Settings(20, 1024, false));
                    query(helper, net, "KEEP oak_planks BETWEEN 8 AND 64");
                    query(helper, net, "WATCH oak_log < 2 DO QUERY items");
                    final IqlEngine.Outcome listed = query(helper, net, "SHOW STATES");
                    helper.assertTrue(listed.ok() && listed.table().rows().size() == 2,
                            "one state and one watch are listed; said " + listed.message());
                    helper.assertTrue(GameText.resolve(listed.table().rows().get(0).get(0))
                                    .equals("KEEP oak_planks BETWEEN 8 AND 64"),
                            "the state as written; " + listed.table().rows().get(0));
                    helper.assertTrue(query(helper, net, "FORGET oak_planks").ok()
                            && query(helper, net, "FORGET WATCH 1").ok(), "both let go");
                    helper.assertTrue(engine().states(net.mainframe()).isEmpty()
                            && engine().watches(net.mainframe()).isEmpty(), "and gone");
                    helper.assertFalse(query(helper, net, "FORGET oak_planks").ok(), "a state not kept cannot go");
                })
                .thenSucceed();
    }

    /** A statement of YourIQL written wrong is answered with how it is written; the core still answers. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void malformed_isAnsweredWithItsSyntax(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net))
                .thenExecute(() -> {
                    expectRefused(helper, query(helper, net, "KEEP coal >"), ProphetTexts.USAGE_KEEP.text());
                    expectRefused(helper, query(helper, net, "WATCH coal < 4 DO"), ProphetTexts.USAGE_WATCH.text());
                    expectRefused(helper, query(helper, net, "KEEP nothing_at_all >= 4"),
                            ProphetTexts.UNKNOWN_ITEM.with("nothing_at_all"));
                    helper.assertTrue(query(helper, net, "QUERY items").ok(), "the language's core still answers");
                })
                .thenSucceed();
    }

    /** With reactions paused nothing is made; switched on again, the state is made up. */
    @GameTest(template = ARENA, timeoutTicks = 900)
    public static void paused_reactsToNothingUntilSwitchedOn(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net))
                .thenExecute(() -> {
                    engine().configure(net.mainframe(), new ProphetEngine.Settings(20, 1024, false));
                    query(helper, net, "KEEP oak_planks >= 8");
                })
                .thenIdle(80)
                .thenExecute(() -> helper.assertTrue(held(helper, net, Items.OAK_PLANKS) == 0L
                        && engine().reactions(net.mainframe()).isEmpty(), "paused, it made nothing"))
                .thenExecute(() -> engine().configure(net.mainframe(), new ProphetEngine.Settings(20, 1024, true)))
                .thenWaitUntil(() -> helper.assertTrue(held(helper, net, Items.OAK_PLANKS) >= 8,
                        "switched on, it makes the state up"))
                .thenSucceed();
    }

    /** The states, the watches and the settings go in the Mainframe's save, under the engine's package. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void states_goInTheMainframesSave(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net))
                .thenExecute(() -> {
                    engine().configure(net.mainframe(), new ProphetEngine.Settings(100, 256, false));
                    query(helper, net, "KEEP oak_planks >= 8");
                    query(helper, net, "WATCH oak_log < 2 DO QUERY items");
                    final CompoundTag saved = net.mainframe().saveWithoutMetadata(helper.getLevel().registryAccess())
                            .getCompound("EngineData").getCompound(PROPHET.toString());
                    helper.assertTrue(saved.getList("Keeps", Tag.TAG_COMPOUND).size() == 1
                                    && saved.getList("Watches", Tag.TAG_COMPOUND).size() == 1,
                            "the state and the watch are saved; " + saved);
                    helper.assertTrue(saved.getInt("Interval") == 100 && saved.getLong("MaxBatch") == 256L
                            && !saved.getBoolean("Reacting"), "and the settings");
                })
                .thenSucceed();
    }

    /** The Reactive Console is answered with the states, the watches and the settings; a check applies nothing. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void console_isAnsweredWithStatesWatchesAndSettings(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = planksNetwork(helper);
        helper.startSequence()
                .thenWaitUntil(() -> ready(helper, net))
                .thenExecute(() -> {
                    final ProphetConsolePayload checked = act(helper, net, ProphetActionPayload.CHECK,
                            "KEEP oak_planks >= 8");
                    helper.assertTrue(checked.ok() && checked.states().isEmpty(),
                            "a check reads the statement and applies nothing");
                    helper.assertFalse(act(helper, net, ProphetActionPayload.CHECK, "KEEP coal >").ok(),
                            "a check of a statement written wrong says so");
                    final ProphetConsolePayload applied = act(helper, net, ProphetActionPayload.APPLY,
                            "KEEP oak_planks >= 8");
                    helper.assertTrue(applied.ok() && applied.states().size() == 1
                                    && applied.states().get(0).written().equals("KEEP oak_planks >= 8"),
                            "applied, the state is listed; said " + GameText.resolve(applied.message()));
                    helper.assertTrue(GameText.resolve(applied.engine()).equals("Prophet YourIQL 5.6"),
                            "with the engine and its version; " + GameText.resolve(applied.engine()));
                    final ProphetConsolePayload configured = act(helper, net, ProphetActionPayload.SETTINGS,
                            "40;256;false");
                    helper.assertTrue(configured.settings().interval() == 40
                            && configured.settings().maxBatch() == 256L && !configured.settings().reacting(),
                            "the settings are set");
                    net.mainframe().activateEngine(NetworkEngines.MIDSOFT_IQL_SERVER.program());
                    final ProphetConsolePayload refused = act(helper, net, ProphetActionPayload.REFRESH, "");
                    helper.assertTrue(!refused.ok() && GameText.resolve(refused.message())
                                    .startsWith("No compatible Prophet YourIQL"),
                            "on another engine it finds no Prophet; " + GameText.resolve(refused.message()));
                    helper.assertFalse(query(helper, net, "KEEP oak_planks >= 8").ok(),
                            "and the Midsoft IQL Server does not speak YourIQL");
                })
                .thenSucceed();
    }

    /* A crafting network running Prophet YourIQL, with the planks recipe and eight logs in its server. */
    private static TestWorldBuilder.CraftingNetwork planksNetwork(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork net = TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        helper.runAfterDelay(SETTLE, () -> {
            net.mainframe().installEngine(PROPHET);
            net.mainframe().activateEngine(PROPHET);
            net.cc().loadPattern(CraftFiles.oakPlanks());
            net.seed(Items.OAK_LOG, 8);
        });
        return net;
    }

    private static void ready(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net) {
        helper.assertTrue(net.mainframe().runningEngine() instanceof ProphetEngine, "Prophet YourIQL runs");
        helper.assertFalse(net.mainframe().networkPatterns().isEmpty(), "the network knows the planks recipe");
        helper.assertTrue(net.mainframe().networkIndex().snapshot().getOrDefault(StorageKey.of(Items.OAK_LOG), 0L)
                == 8L, "the network indexes the logs");
    }

    private static IqlEngine.Outcome query(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net,
                                           final String statement) {
        return net.mainframe().networkOperations().query(
                IqlEngine.viewOf(new ServerCliComputer(net.cc(), helper.getLevel())), statement, ROWS);
    }

    private static ProphetConsolePayload act(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net,
                                             final int action, final String arg) {
        return ProphetPayloads.act(helper.getLevel(), net.cc(), null,
                new ProphetActionPayload(net.cc().getBlockPos(), 3, action, arg));
    }

    private static ProphetEngine engine() {
        final INetworkEngine engine = NetworkEngines.get(PROPHET);
        if (engine instanceof ProphetEngine prophet) {
            return prophet;
        }
        throw new IllegalStateException("Prophet YourIQL is not registered");
    }

    private static long held(final GameTestHelper helper, final TestWorldBuilder.CraftingNetwork net,
                             final Item item) {
        return net.storage(helper.getLevel()).count(StorageKey.of(item));
    }

    private static void expectRefused(final GameTestHelper helper, final IqlEngine.Outcome outcome,
                                      final Text expected) {
        helper.assertTrue(!outcome.ok() && outcome.message().equals(expected.english()),
                "refused with \"" + expected.english() + "\"; got " + outcome.ok() + " \"" + outcome.message() + "\"");
    }

    /** An age's Mainframe, and the version of Prophet YourIQL it installs, or null when it installs none. */
    private record Age(Block block, @Nullable String version) {
    }
}
