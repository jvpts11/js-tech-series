/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import dev.jstech.core.JsCore;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.content.ScriptBinding;
import dev.jstech.core.event.IOperationLifecycleEvent;
import dev.jstech.core.integration.ftbquests.FtbQuestsIntegration;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.progression.QuestGoals;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.script.CoreScripts;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestProgression;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a modpack's scripts and quests reach of the Core: the calls handed to KubeJS as {@code JsCore} and to
 * CraftTweaker as {@code mods.jscore.Core}, the goals FTB Quests' tasks of the Core ask, and, in runs that have them
 * ({@code -PwithKubeJs}, {@code -PwithCraftTweaker}, {@code -PwithFtbQuests}), the test scripts in
 * {@code tests/src/kubejs} and {@code tests/src/crafttweaker} hearing the Core's events and writing recipes of a
 * processing machine, and FTB Quests holding the Core's kinds of task. Each test's team is its own, so what a script
 * does to it reaches no other test.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ScriptReachGameTests {

    private static final String ARENA = "empty";
    private static final String TEAM = "jstests_scripts";
    private static final String KUBEJS_TEAM = "jstests_kubejs";
    private static final String CRAFTTWEAKER_TEAM = "jstests_crafttweaker";
    private static final String FUSION = "jstests:test_fusion";
    private static final String STAGES = "jstests:test_stages";
    private static final UUID QUESTER = UUID.nameUUIDFromBytes("jstests:quester".getBytes());
    private static final double SOME = 0.4;
    private static final int SCRIPT_TICKS = 40;
    private static final int SCRIPT_ENERGY = 5;
    /** The kind of Operation the test scripts answer. */
    private static final String SCRIPT_CHECK = "jstests_script_check";
    private static final double STONE_DENSITY = 2700.0;
    private static final int TIN_ATOMIC_NUMBER = 50;

    private ScriptReachGameTests() {
    }

    @GameTest(template = ARENA)
    public static void coreScripts_areBoundForScriptsAsJsCore(final GameTestHelper helper) {
        final List<ScriptBinding> bindings = ModContent.of(JsCore.MODID).declaredScriptBindings();
        helper.assertTrue(bindings.stream().anyMatch(binding -> binding.name().equals(CoreScripts.NAME)
                && binding.statics() == CoreScripts.class), "the Core declares JsCore; got " + bindings);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreScripts_reachTeamsAndTheirProgress(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        CoreScripts.forget(server, TEAM, FUSION);
        helper.assertFalse(CoreScripts.knows(server, TEAM, FUSION), "the team does not know Test Fusion yet");
        CoreScripts.grant(server, TEAM, FUSION);
        helper.assertTrue(CoreScripts.knows(server, TEAM, FUSION) && CoreScripts.share(server, TEAM, FUSION) == 1.0,
                "granted, it knows it whole");
        CoreScripts.forget(server, TEAM, FUSION);
        helper.assertFalse(CoreScripts.knows(server, TEAM, FUSION), "and forgets it");
        helper.assertTrue(CoreScripts.knowledge().contains(FUSION), "the knowledge declared is listed");
        CoreScripts.reach(server, TEAM, "jstests:test_suborbital");
        helper.assertTrue(CoreScripts.reached(server, TEAM, "jstests:test_suborbital"), "a milestone passed");
        helper.assertTrue(CoreScripts.milestones().contains("jstests:test_orbit"), "the milestones are listed");
        final List<String> steps = CoreScripts.steps(STAGES);
        helper.assertTrue(steps.size() == TestProgression.Stage.values().length, "the axis's steps; got " + steps);
        CoreScripts.reachStep(server, TEAM, STAGES, steps.get(1));
        helper.assertTrue(!CoreScripts.step(server, TEAM, STAGES).equals(steps.getFirst()),
                "the team moved along the axis; it is at " + CoreScripts.step(server, TEAM, STAGES));
        helper.assertTrue(refused(() -> CoreScripts.reachStep(server, TEAM, STAGES, "no_such_step")),
                "a step the axis does not have is refused");
        helper.assertTrue(refused(() -> CoreScripts.knows(server, TEAM, "jstests:no_such_knowledge")),
                "knowledge nothing declared is refused");
        helper.assertTrue(CoreScripts.teamOf(server, QUESTER).equals(CoreTeams.teamOf(server, QUESTER)),
                "a player's team is the Core's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreScripts_reachTheMaterialsAndTheUnification(final GameTestHelper helper) {
        final ItemStack ingot = CoreScripts.material("jscore:tin", "ingot");
        helper.assertTrue(ingot.is(MaterialUses.item(SeriesMaterials.TIN, Forms.INGOT).get()),
                "tin's ingot; got " + ingot);
        helper.assertTrue(CoreScripts.materials().contains("jscore:tin"), "the catalogue's materials are listed");
        helper.assertTrue(CoreScripts.unified("jstests:foreign_tin_ingot").equals(CoreScripts.unified(
                "jscore:tin_ingot")), "the copy and the Core's ingot stand as the same item");
        helper.assertTrue(CoreScripts.unified("minecraft:stone").equals("minecraft:stone"),
                "an item nothing stands for is itself");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void coreScripts_reachEverythingTheCoreKeepsAsData(final GameTestHelper helper) {
        helper.assertTrue(CoreScripts.property("jscore:tin", "jscore:atomic_number").equals(
                String.valueOf(TIN_ATOMIC_NUMBER)), "tin's atomic number; got " + CoreScripts.property("jscore:tin",
                "jscore:atomic_number"));
        helper.assertTrue(CoreScripts.property("jscore:tin", "jscore:symbol").equals("\"Sn\""), "tin's symbol");
        helper.assertTrue(CoreScripts.properties().contains("jscore:formula"), "the properties are listed");
        helper.assertTrue(CoreScripts.dataKinds().contains("jscore:deposits"), "the kinds of data are listed; got "
                + CoreScripts.dataKinds());
        helper.assertTrue(CoreScripts.dataIds("jscore:deposits").contains("jscore:copper"),
                "the copper deposit's file is loaded");
        helper.assertTrue(CoreScripts.data("jscore:deposits", "jscore:copper").startsWith("{"),
                "and written as JSON; got " + CoreScripts.data("jscore:deposits", "jscore:copper"));
        helper.assertTrue(CoreScripts.data("jscore:deposits", "jscore:no_such_deposit").isEmpty(),
                "a file the world lacks is nothing");
        helper.assertTrue(CoreScripts.dataMaps().contains("jscore:seals"), "the data maps are listed");
        helper.assertTrue(Double.parseDouble(CoreScripts.dataMap("jscore:block_density", "minecraft:stone"))
                == STONE_DENSITY, "stone's density, by the tag its file names");
        helper.assertTrue(CoreScripts.dataMap("jscore:seals", "minecraft:glass").contains("rating"),
                "how glass seals; got " + CoreScripts.dataMap("jscore:seals", "minecraft:glass"));
        helper.assertTrue(CoreScripts.dataMap("jscore:seals", "minecraft:dirt").isEmpty(),
                "what a map says nothing of is nothing");
        helper.assertTrue(refused(() -> CoreScripts.dataMap("jstests:no_such_map", "minecraft:stone")),
                "a map nothing declared is refused");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void questGoals_followTheTeamsProgress(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String team = CoreTeams.teamOf(server, FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(QUESTER, "jstests_quester")).getUUID());
        TeamProgress.forget(server, team, TestProgression.FUSION.id());
        TeamProgress.forget(server, team, TestProgression.COMPREHENSION.id());
        try {
            helper.assertTrue(QuestGoals.knowledge(server, QUESTER, FUSION) == 0.0, "nothing known yet");
            TeamProgress.grant(server, team, TestProgression.FUSION, 1.0);
            helper.assertTrue(QuestGoals.knowledge(server, QUESTER, FUSION) == 1.0, "known whole");
            TeamProgress.grant(server, team, TestProgression.COMPREHENSION, SOME);
            helper.assertTrue(QuestGoals.knowledge(server, QUESTER, "jstests:test_comprehension") == SOME,
                    "known in part");
            helper.assertTrue(QuestGoals.knowledge(server, QUESTER, "jstests:no_such_knowledge") == 0.0,
                    "knowledge nothing declared is never met");
            helper.assertTrue(QuestGoals.step(server, QUESTER, STAGES, CoreScripts.steps(STAGES).getFirst()),
                    "the first step of an axis is reached by every team");
            helper.assertFalse(QuestGoals.step(server, QUESTER, "jstests:no_such_axis", "first"),
                    "an axis nothing declared is never met");
            helper.assertFalse(QuestGoals.milestone(server, QUESTER, "jstests:no_such_milestone"),
                    "a milestone nothing declared is never met");
        } finally {
            TeamProgress.forget(server, team, TestProgression.FUSION.id());
            TeamProgress.forget(server, team, TestProgression.COMPREHENSION.id());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void kubeJsScript_hearsTheCoresEventsAndWritesAMachinesRecipe(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final Optional<RecipeHolder<?>> recipe = server.getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "kubejs_pressing"));
        if (!asked("kubejs")) {
            helper.assertTrue(recipe.isEmpty(), "without KubeJS no script wrote a recipe");
            helper.succeed();
            return;
        }
        pressedByScript(helper, recipe, Items.DIAMOND, 2);
        TeamProgress.grant(server, KUBEJS_TEAM, TestProgression.COMPREHENSION, 1.0);
        helper.assertTrue(TeamProgress.reached(server, KUBEJS_TEAM, TestProgression.FIRSTS.get(1)),
                "the script heard the team learn Test Comprehension and had it pass Test Orbit");
        completeAnOperation();
        helper.assertTrue(TeamProgress.reached(server, KUBEJS_TEAM, TestProgression.FIRSTS.getFirst()),
                "the script heard an Operation complete and had the team pass Test Suborbital");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void craftTweakerScript_hearsTheCoresEventsAndWritesAMachinesRecipe(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final Optional<RecipeHolder<?>> recipe = server.getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath("crafttweaker", "jstests_pressing"));
        if (!asked("crafttweaker")) {
            helper.assertTrue(recipe.isEmpty(), "without CraftTweaker no script wrote a recipe");
            helper.succeed();
            return;
        }
        pressedByScript(helper, recipe, Items.EMERALD, 3);
        TeamProgress.grant(server, CRAFTTWEAKER_TEAM, TestProgression.FUSION, 1.0);
        helper.assertTrue(TeamProgress.reached(server, CRAFTTWEAKER_TEAM, TestProgression.FIRSTS.getFirst()),
                "the script heard the team learn Test Fusion and had it pass Test Suborbital");
        completeAnOperation();
        helper.assertTrue(TeamProgress.reached(server, CRAFTTWEAKER_TEAM, TestProgression.FIRSTS.get(1)),
                "the script heard an Operation complete and had the team pass Test Orbit");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ftbQuests_holdsTheCoresKindsOfTask(final GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded(FtbQuestsIntegration.MOD_ID) == asked("ftbquests"),
                "FTB Quests is loaded exactly when the run asked for it");
        if (asked("ftbquests")) {
            for (final ResourceLocation kind : FtbQuestsIntegration.taskKinds()) {
                helper.assertTrue(TaskTypes.TYPES.containsKey(kind), "FTB Quests holds the task " + kind);
            }
        }
        helper.succeed();
    }

    private static void pressedByScript(final GameTestHelper helper, final Optional<RecipeHolder<?>> recipe,
                                        final Item made, final int count) {
        helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof ProcessingRecipe,
                "the script's recipe of the test press exists; got " + recipe);
        final ProcessingRecipe pressing = (ProcessingRecipe) recipe.get().value();
        helper.assertTrue(pressing.outputs().size() == 1 && pressing.outputs().getFirst().is(made)
                        && pressing.outputs().getFirst().getCount() == count,
                "it makes what the script wrote; got " + pressing.outputs());
        helper.assertTrue(pressing.ticks() == SCRIPT_TICKS && pressing.energyPerTick() == SCRIPT_ENERGY,
                "in the time and at the energy the script wrote; got " + pressing.ticks() + " ticks, "
                        + pressing.energyPerTick() + " a tick");
    }

    /* An Operation of the test's own kind completes, as an orchestrator tells the Core's bus of one. */
    private static void completeAnOperation() {
        JsCore.events().post(new IOperationLifecycleEvent.Completed(new NetworkUuid(UUID.randomUUID()),
                UUID.randomUUID(), SCRIPT_CHECK, 1L));
    }

    private static boolean refused(final Runnable call) {
        try {
            call.run();
            return false;
        } catch (final IllegalArgumentException expected) {
            return true;
        }
    }

    private static boolean asked(final String mod) {
        return "present".equals(System.getProperty("jsc.gametests." + mod));
    }
}
