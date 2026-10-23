/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.Ownership;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.api.Consequences;
import dev.jstech.industrial.api.IndustrialAdvancementTab;
import dev.jstech.industrial.api.IndustrialSoundGroup;
import dev.jstech.industrial.blockentity.MaceratorBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestProgression;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What J's Industrial's content will stand on: a consequence's switch, on unless a server turns it off and saying
 * what happens instead; the four tabs of its advancements, each with its root; the groups of its sounds, each mixed
 * in a channel J's Core knows; and its machines, which run what is locked only once their owner's team knows it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialFoundationsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACERATOR = new BlockPos(2, 2, 2);
    /* The Macerator's output slot. */
    private static final int OUT = 1;

    private IndustrialFoundationsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void consequence_isOnUnlessTurnedOffAndSaysWhatHappensWhenOff(final GameTestHelper helper) {
        final ConfigKey<Boolean> boilers = Consequences.of("fluids.boilers_explode", "Boilers explode",
                "A boiler held past its pressure bursts, as a real one does.", "a blocked boiler just stops.");
        helper.assertTrue(boilers.defaultValue(), "a consequence is on unless a server turns it off");
        helper.assertTrue(boilers.comment().equals(List.of("A boiler held past its pressure bursts, as a real one "
                + "does.", "Off, a blocked boiler just stops.")), "it says what it is and what happens when off: "
                + boilers.comment());
        helper.assertTrue(boilers.title().equals("Boilers explode")
                && boilers.dottedPath().equals("fluids.boilers_explode"), "named for the settings screen");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void advancementTabs_eachHaveTheirRoot(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        for (final IndustrialAdvancementTab tab : IndustrialAdvancementTab.values()) {
            final AdvancementHolder root = server.getAdvancements().get(tab.root());
            helper.assertTrue(root != null, "the tab " + tab.path() + " has its root, " + tab.root());
            helper.assertTrue(root.value().parent().isEmpty() && root.value().display().isPresent(),
                    "which starts a tab of its own: " + tab.root());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void soundGroups_mixInChannelsTheCoreKnows(final GameTestHelper helper) {
        for (final IndustrialSoundGroup group : IndustrialSoundGroup.values()) {
            helper.assertTrue(AudioChannels.find(group.channel().id()) == group.channel(),
                    group + " is mixed in a channel the Core declared");
        }
        helper.assertTrue(IndustrialSoundGroup.SAFETY.carriesFar(), "alarms carry far");
        helper.assertTrue(IndustrialSoundGroup.MACHINES.followsPower(), "a machine drawing more sounds louder");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void machine_runsALockedRecipeOnlyOnceItsOwnersTeamKnowsIt(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID owner = UUID.randomUUID();
        helper.setBlock(MACERATOR, IndustrialModule.MACERATOR.get());
        final MaceratorBlockEntity macerator = helper.getBlockEntity(MACERATOR);
        macerator.own(Ownership.of(owner));
        macerator.getInventory().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        macerator.getEnergy().setEnergyStored(16_000);
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    helper.assertTrue(macerator.status() == MachineStatus.LOCKED,
                            "locked while the owner's team lacks the knowledge; it is " + macerator.status());
                    helper.assertTrue(macerator.getInventory().getStackInSlot(0).is(Items.GOLD_INGOT),
                            "and it took nothing");
                    TeamProgress.grant(server, CoreTeams.teamOf(server, owner), TestProgression.FUSION, 1.0);
                })
                .thenWaitUntil(() -> helper.assertTrue(macerator.getInventory().getStackInSlot(OUT)
                        .is(Items.GOLD_NUGGET), "ground once the team knew"))
                .thenSucceed();
    }
}
