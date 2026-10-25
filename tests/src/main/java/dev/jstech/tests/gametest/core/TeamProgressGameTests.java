/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.jstechseries.tier.Eras;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.Milestones;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionTriggers;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.progression.TeamProgressEvent;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.TestPressBlockEntity;
import dev.jstech.tests.TestProgression;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Progression kept per team and by every mod: the eras as a registry an add-on's era goes into the middle of; what a
 * team reaches, its alone and joined whole when two teams merge; milestones reached once, each knowledge too;
 * knowledge whole or by shares; the axes moved by making and placing things; and a machine that will not run a recipe
 * whose knowledge its owner's team lacks, until the team learns it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TeamProgressGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PRESS = new BlockPos(2, 2, 2);
    private static final double EPSILON = 1.0e-9;

    private TeamProgressGameTests() {
    }

    @GameTest(template = ARENA)
    public static void eras_areARegistryAnAddonsEraGoesIntoTheMiddleOf(final GameTestHelper helper) {
        final List<Era> order = Eras.ordered();
        final int advanced = order.indexOf(HardwareEra.ADVANCED);
        helper.assertTrue(order.get(advanced + 1) == TestProgression.QUANTUM, "the add-on's era right after Advanced");
        helper.assertTrue(order.get(advanced + 2) == HardwareEra.EXA, "and Exa after it; got " + order);
        helper.assertTrue(order.size() == HardwareEra.SERIES.size() + 1, "the series' seven and one more");
        helper.assertTrue(HardwareEra.EXA.isAtLeast(TestProgression.QUANTUM), "Exa is the add-on's era or later");
        helper.assertTrue(!HardwareEra.STANDARD.isAtLeast(TestProgression.QUANTUM), "Standard is not");
        helper.assertTrue(TestProgression.QUANTUM.level() == advanced + 1 && Eras.settled(),
                "its place is settled once the game has loaded");
        final Era back = EraCodecs.CODEC.parse(JsonOps.INSTANCE, EraCodecs.CODEC.encodeStart(JsonOps.INSTANCE,
                TestProgression.QUANTUM).getOrThrow()).getOrThrow();
        helper.assertTrue(back == TestProgression.QUANTUM, "an era is saved by its id and read back");
        helper.assertTrue(EraCodecs.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("jstests:no_such_era")).isError(),
                "an id no era has is refused");
        helper.assertTrue(EraCodecs.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("legacy")).getOrThrow()
                == HardwareEra.LEGACY, "a data file may name one of the series' seven bare");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void knowledge_comesWholeOrByShares_andEachGainIsToldOnce(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String team = team();
        final List<TeamProgressEvent.KnowledgeGained> heard = new ArrayList<>();
        final Consumer<TeamProgressEvent.KnowledgeGained> listener = event -> {
            if (event.team().equals(team)) {
                heard.add(event);
            }
        };
        NeoForge.EVENT_BUS.addListener(TeamProgressEvent.KnowledgeGained.class, listener);
        try {
            final ResourceLocation comprehension = TestProgression.COMPREHENSION.id();
            helper.assertTrue(TeamProgress.grant(server, team, TestProgression.COMPREHENSION, 0.4), "some");
            helper.assertTrue(!TeamProgress.grant(server, team, TestProgression.COMPREHENSION, 0.3),
                    "less than it knows changes nothing");
            helper.assertTrue(Math.abs(TeamProgress.share(server, team, comprehension) - 0.4) < EPSILON,
                    "it knows the share given");
            helper.assertTrue(!TeamProgress.knows(server, team, comprehension), "and not the whole");
            TeamProgress.grant(server, team, TestProgression.COMPREHENSION, 1.0);
            helper.assertTrue(TeamProgress.knows(server, team, comprehension), "known whole");
            TeamProgress.grant(server, team, TestProgression.FUSION, 0.1);
            helper.assertTrue(TeamProgress.knows(server, team, TestProgression.FUSION.id()),
                    "knowledge that comes whole is known whole by any share");
            helper.assertTrue(heard.size() == 3 && heard.get(1).learnt() && heard.get(2).learnt()
                    && !heard.getFirst().learnt(), "each gain told once; heard " + heard.size());
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void milestones_areReachedOnce_andKnownAsKnowledge(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String team = team();
        helper.assertTrue(Milestones.of(TestProgression.FIRSTS.getFirst().id()).isEmpty()
                        && Milestones.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_firsts"))
                        .equals(TestProgression.FIRSTS), "the set is read back in its order");
        helper.assertTrue(Knowledges.byId(TestProgression.FIRSTS.get(1).id()).isPresent(),
                "each milestone is declared as knowledge too");
        helper.assertTrue(TeamProgress.reach(server, team, TestProgression.FIRSTS.get(1)), "the orbit reached");
        helper.assertTrue(!TeamProgress.reach(server, team, TestProgression.FIRSTS.get(1)), "only once");
        helper.assertTrue(TeamProgress.reached(server, team, TestProgression.FIRSTS.get(1)), "and kept");
        helper.assertTrue(!TeamProgress.reached(server, team, TestProgression.FIRSTS.getFirst()),
                "each milestone on its own");
        helper.assertTrue(TeamProgress.knows(server, team, TestProgression.FIRSTS.get(1).id()),
                "a recipe can ask for the orbit, now known");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void teams_thatMerge_keepEverythingEitherHad(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final String first = team();
        final String second = team();
        TeamProgress.grant(server, first, TestProgression.FUSION, 1.0);
        TeamProgress.reach(server, first, TestProgression.FIRSTS.getFirst());
        TeamProgress.reach(server, second, TestProgression.STAGES, TestProgression.Stage.LAST);
        TeamProgress.grant(server, second, TestProgression.COMPREHENSION, 0.5);
        TeamProgress.grant(server, first, TestProgression.COMPREHENSION, 0.2);
        TeamProgress.merge(server, first, second);
        helper.assertTrue(TeamProgress.knows(server, second, TestProgression.FUSION.id()), "the first's knowledge");
        helper.assertTrue(TeamProgress.reached(server, second, TestProgression.FIRSTS.getFirst()),
                "and its milestone");
        helper.assertTrue(TeamProgress.reached(server, second, TestProgression.STAGES) == TestProgression.Stage.LAST,
                "the second keeps its own");
        helper.assertTrue(Math.abs(TeamProgress.share(server, second, TestProgression.COMPREHENSION.id()) - 0.5)
                < EPSILON, "the more of each share");
        helper.assertTrue(TeamProgress.reached(server, first, TestProgression.STAGES) == TestProgression.Stage.FIRST,
                "the first is not given the second's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void axes_moveByMakingAndPlacing_forThePlayerAndTheirTeam(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID player = UUID.randomUUID();
        ProgressionTriggers.made(server, player, new ItemStack(Items.STONE));
        helper.assertTrue(PlayerProgress.reached(server, player, TestProgression.STAGES)
                == TestProgression.Stage.FIRST, "making what moves nothing moves nothing");
        ProgressionTriggers.made(server, player, new ItemStack(Items.GOLD_BLOCK));
        helper.assertTrue(PlayerProgress.reached(server, player, TestProgression.STAGES)
                == TestProgression.Stage.MIDDLE, "making a gold block moves the player on");
        ProgressionTriggers.placed(server, player, Blocks.LODESTONE.defaultBlockState());
        helper.assertTrue(PlayerProgress.reached(server, player, TestProgression.STAGES)
                == TestProgression.Stage.LAST, "placing a lodestone moves them to the last stage");
        helper.assertTrue(TeamProgress.reached(server, CoreTeams.teamOf(server, player), TestProgression.STAGES)
                == TestProgression.Stage.LAST, "and their team with them");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void recipes_readTheKnowledgeTheyAskForAndWriteItBack(final GameTestHelper helper) {
        final ProcessingRecipe built = ProcessingRecipeBuilder.of(TestPress.PRESSING).input(Items.GOLD_INGOT, 2)
                .output(Items.GOLD_BLOCK, 1).needs(TestProgression.FUSION.id()).build();
        final Codec<ProcessingRecipe> codec = TestPress.PRESSING.serializer().codec().codec();
        final ProcessingRecipe back = codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, built)
                .getOrThrow()).getOrThrow();
        helper.assertTrue(back.knowledge().orElse(null) != null
                && back.knowledge().get().equals(TestProgression.FUSION.id()), "the knowledge comes back");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void machines_runALockedRecipeOnlyOnceTheirOwnersTeamKnowsIt(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID owner = UUID.randomUUID();
        helper.setBlock(PRESS, TestPress.BLOCK.get());
        final TestPressBlockEntity press = helper.getBlockEntity(PRESS);
        press.own(Ownership.of(owner));
        press.getInventory().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 2));
        press.getInventory().setStackInSlot(1, new ItemStack(Items.REDSTONE, 1));
        press.getEnergy().setEnergyStored(50_000);
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(press.status() == MachineStatus.LOCKED,
                            "locked while the owner's team lacks the knowledge; it is " + press.status());
                    helper.assertTrue(press.getInventory().getStackInSlot(0).getCount() == 2, "and it took nothing");
                    TeamProgress.grant(server, CoreTeams.teamOf(server, owner), TestProgression.FUSION, 1.0);
                })
                .thenWaitUntil(() -> helper.assertTrue(made(press), "the gold block made once the team knew"))
                .thenExecute(() -> helper.assertTrue(PlayerProgress.reached(server, owner, TestProgression.STAGES)
                        == TestProgression.Stage.MIDDLE, "and what the machine made moved its owner on"))
                .thenSucceed();
    }

    /* A team of its own, by an id no source gives. */
    private static String team() {
        return "jstests:" + UUID.randomUUID();
    }

    private static boolean made(final TestPressBlockEntity press) {
        for (int slot = 0; slot < press.getInventory().getSlots(); slot++) {
            if (press.getInventory().getStackInSlot(slot).is(Items.GOLD_BLOCK)) {
                return true;
            }
        }
        return false;
    }
}
