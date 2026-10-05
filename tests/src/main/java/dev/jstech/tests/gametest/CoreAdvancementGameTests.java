/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.advancement.Advancements;
import dev.jstech.core.advancement.AxisStepTrigger;
import dev.jstech.core.gametest.GameTestPlayers;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's advancement triggers, through two advancements of the test mod written by hand: one earned by an event a
 * mod reports under its own namespace, and one by reaching the Transition era, or any era past it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CoreAdvancementGameTests {

    private static final String ARENA = "empty";
    private static final ResourceLocation POKED = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "poked");

    private CoreAdvancementGameTests() {
    }

    @GameTest(template = ARENA)
    public static void event_earnsTheAdvancementThatWaitsForIt(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            Advancements.award(player, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "prodded"));
            helper.assertFalse(done(player, "poked"), "another event earns nothing");
            Advancements.award(FakePlayerFactory.getMinecraft(helper.getLevel()), POKED);
            helper.assertFalse(done(player, "poked"), "nor the event credited to a machine acting as a player");
            Advancements.award(player, POKED, "any detail");
            helper.assertTrue(done(player, "poked"), "the event it waits for earns it, whatever its detail");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void axisStep_isEarnedAtItsStepOrAnyPastIt(final GameTestHelper helper) {
        final ServerPlayer player = join(helper);
        try {
            PlayerProgress.reach(player.server, player.getUUID(), ProgressionAxes.HARDWARE_ERA, HardwareEra.LEGACY);
            helper.assertFalse(done(player, "reached_transition"), "a step short of it earns nothing");
            // Straight from Legacy to Standard: the Transition step is skipped, and still counted as passed.
            PlayerProgress.reach(player.server, player.getUUID(), ProgressionAxes.HARDWARE_ERA, HardwareEra.STANDARD);
            helper.assertTrue(done(player, "reached_transition"), "a step past it earns it");
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void axisStep_neverMeetsAnAxisOrStepTheGameDoesNotKnow(final GameTestHelper helper) {
        final ResourceLocation era = ProgressionAxes.HARDWARE_ERA.id();
        helper.assertFalse(new AxisStepTrigger.Instance(Optional.empty(), era, "steam_age").matches(era, 9),
                "a step the axis does not have is never reached");
        final ResourceLocation gone = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "gone_axis");
        helper.assertFalse(new AxisStepTrigger.Instance(Optional.empty(), gone, "first").matches(gone, 9),
                "nor a step of an axis no mod registered");
        helper.assertTrue(new AxisStepTrigger.Instance(Optional.empty(), era, "legacy").matches(era, 1),
                "while the step itself is met");
        helper.succeed();
    }

    private static boolean done(final ServerPlayer player, final String path) {
        final AdvancementHolder holder = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path));
        if (holder == null) {
            throw new IllegalStateException("no advancement " + path);
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /* The Core's test player: online where the server looks a player up by id, never logged in. */
    private static ServerPlayer join(final GameTestHelper helper) {
        return GameTestPlayers.join(helper, "achiever");
    }

    private static void leave(final ServerPlayer player) {
        GameTestPlayers.leave(player);
    }
}
