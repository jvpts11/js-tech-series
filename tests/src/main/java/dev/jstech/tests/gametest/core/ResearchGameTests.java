/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.progression.KnowledgeGate;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.research.DataKind;
import dev.jstech.core.research.DataKinds;
import dev.jstech.core.research.Discoveries;
import dev.jstech.core.research.Research;
import dev.jstech.core.research.ResearchCost;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestResearch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's research: a discovery goes on by the points worked and spends its data a part of the way at a time, waits
 * for what it comes after, is not there when it asks for what no mod declares, costs what a data pack says, and a
 * mod's setting turning research off opens what its discoveries lock without granting them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_research";
    /* The gate is the test mod's own, which turning off opens for every test at once: it runs alone. */
    private static final String GATE_BATCH = "jscore_research_gate";
    private static final double CLOSE = 1e-9;

    private ResearchGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void work_goesByThePointsAndSpendsTheDataOfEachPart(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String team = CoreTeams.teamOf(server, UUID.randomUUID());
        final Store store = new Store(4);
        helper.assertTrue(Research.work(server, team, TestResearch.FIRST, 10, store) == Research.Outcome.WORKED
                && near(Research.share(server, team, TestResearch.FIRST), 0.1) && store.held == 3,
                "ten points of a hundred: a tenth of the way, the first sample spent; held " + store.held);
        Research.work(server, team, TestResearch.FIRST, 40, store);
        helper.assertTrue(near(Research.share(server, team, TestResearch.FIRST), 0.5) && store.held == 2,
                "half the way, two samples spent; held " + store.held);
        store.held = 0;
        helper.assertTrue(Research.work(server, team, TestResearch.FIRST, 10, store) == Research.Outcome.NEEDS_DATA
                && near(Research.share(server, team, TestResearch.FIRST), 0.5),
                "with no sample for the third quarter it stays at half");
        store.held = 2;
        helper.assertTrue(Research.work(server, team, TestResearch.FIRST, 1_000, store) == Research.Outcome.MADE
                && Research.made(server, team, TestResearch.FIRST) && store.held == 0,
                "with the two samples left it is made whole, and goes no further; held " + store.held);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void work_waitsForWhatItComesAfter(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String team = CoreTeams.teamOf(server, UUID.randomUUID());
        final Store none = new Store(0);
        helper.assertTrue(Research.work(server, team, TestResearch.SECOND, 10, none) == Research.Outcome.LOCKED
                && Research.missing(server, team, TestResearch.SECOND).equals(List.of(TestResearch.FIRST.id())),
                "the second waits for the first");
        Research.work(server, team, TestResearch.FIRST, 100, new Store(4));
        helper.assertTrue(Research.work(server, team, TestResearch.SECOND, 10, none) == Research.Outcome.WORKED
                && near(Research.share(server, team, TestResearch.SECOND), 0.2), "once the first is made it starts");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void discovery_askingForWhatNoModDeclaresIsNotThere(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(!Discoveries.exists(helper.getLevel(), TestResearch.ABSENT)
                && Discoveries.exists(helper.getLevel(), TestResearch.FIRST), "a kind no mod declares leaves it out");
        helper.assertTrue(!Discoveries.existing(helper.getLevel()).contains(TestResearch.ABSENT)
                && Discoveries.existing(helper.getLevel()).contains(TestResearch.FIRST), "and from the list");
        helper.assertTrue(Research.work(server, CoreTeams.teamOf(server, UUID.randomUUID()), TestResearch.ABSENT, 10,
                new Store(0)) == Research.Outcome.ABSENT, "it cannot be researched");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void cost_isWhatADataPackSaysOverWhatWasDeclared(final GameTestHelper helper) {
        final ResearchCost overridden = Discoveries.cost(helper.getLevel(), TestResearch.OVERRIDDEN.id())
                .orElseThrow();
        helper.assertTrue(overridden.points() == 7, "the pack's file says seven points; got " + overridden);
        final ResearchCost first = Discoveries.cost(helper.getLevel(), TestResearch.FIRST.id()).orElseThrow();
        helper.assertTrue(first.points() == 100 && first.data(TestResearch.SAMPLES.id()) == 4,
                "the declared cost stands where no file says otherwise; got " + first);
        final ResearchCost read = ResearchCost.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"points\": 36000, \"data\": {\"jstests:test_samples\": 10}}")).getOrThrow();
        helper.assertTrue(read.points() == 36_000 && read.data(TestResearch.SAMPLES.id()) == 10, "a file reads");
        helper.assertTrue(ResearchCost.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"points\": 0}"))
                .error().isPresent(), "a cost of no points is refused");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void dataKinds_sayWhatKindAnItemIs(final GameTestHelper helper) {
        helper.assertTrue(DataKinds.of(new ItemStack(TestResearch.SAMPLE_DATA.get())).equals(
                Optional.of(TestResearch.SAMPLES)), "the sample data is of the samples");
        helper.assertTrue(DataKinds.of(new ItemStack(Items.PAPER)).isEmpty(), "paper is no data");
        helper.assertTrue(DataKinds.byId(TestResearch.SAMPLES.id()).isPresent()
                && DataKinds.byId(TestResearch.NOWHERE.id()).isEmpty(), "a kind is known only once declared");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = GATE_BATCH)
    public static void gate_offOpensWhatItsDiscoveriesLockAndGrantsNothing(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID owner = UUID.randomUUID();
        final Optional<ResourceLocation> first = Optional.of(TestResearch.FIRST.id());
        helper.assertTrue(!KnowledgeGate.allows(server, owner, first), "the first discovery locks what asks for it");
        try {
            TestResearch.require(false);
            helper.assertTrue(!Discoveries.required(server, JsTests.MODID)
                    && KnowledgeGate.allows(server, owner, first),
                    "with the test mod's research off, nothing it locks is locked");
            helper.assertTrue(!TeamProgress.knows(server, CoreTeams.teamOf(server, owner), TestResearch.FIRST.id()),
                    "and nothing is granted");
        } finally {
            TestResearch.require(true);
        }
        helper.assertTrue(!KnowledgeGate.allows(server, owner, first), "turned on again, it locks again");
        helper.succeed();
    }

    private static boolean near(final double share, final double expected) {
        return Math.abs(share - expected) < CLOSE;
    }

    /* A research computer's data slots, holding samples alone. */
    private static final class Store implements Research.IDataStore {

        private int held;

        private Store(final int held) {
            this.held = held;
        }

        @Override
        public int held(final DataKind kind) {
            return kind.equals(TestResearch.SAMPLES) ? this.held : 0;
        }

        @Override
        public void spend(final DataKind kind, final int count) {
            this.held -= Math.min(count, held(kind));
        }
    }
}
