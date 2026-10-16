/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.integration.emi.CoreEmiPlugin;
import dev.jstech.core.integration.jei.CoreJeiPlugin;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.TestProgression;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.UUID;
import net.neoforged.fml.ModList;

/**
 * The knowledge gate as a player meets it, to set beside what it should look like: the test press's recipe that asks
 * for knowledge, shown in the recipe viewer with a padlock and its line in the locked colour while the player's team
 * lacks it, and as any other recipe once the team knows it.
 */
public final class ProgressionClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 200;

    private ProgressionClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void lockedRecipes_showAPadlockUntilTheTeamKnowsWhatTheyNeed(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded("jei") && !ModList.get().isLoaded("emi")) {
            // Without a viewer there is nothing to show: the test passes empty, as the viewer steps elsewhere do.
            ctx.then(0, () -> { });
            return;
        }
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        final UUID[] player = new UUID[1];
        ctx.then(0, () -> player[0] = ctx.mc().player.getUUID())
                .thenServer(0, level -> TeamProgress.forget(level.getServer(), CoreTeams.teamOf(level.getServer(),
                        player[0]), TestProgression.FUSION.id()))
                .thenWaitUntil(() -> !TeamProgress.knowsHere(TestProgression.FUSION.id()), WAIT,
                        "the team's knowledge to reach the player")
                .then(SETTLE, () -> ctx.assertTrue(show(), "the viewer opens on the test press's recipes"))
                .thenScreenshot(20, "recipe-locked")
                .then(SETTLE, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> TeamProgress.grant(level.getServer(), CoreTeams.teamOf(level.getServer(),
                        player[0]), TestProgression.FUSION, 1.0))
                .thenWaitUntil(() -> TeamProgress.knowsHere(TestProgression.FUSION.id()), WAIT,
                        "the team's new knowledge to reach the player")
                .then(SETTLE, () -> ctx.assertTrue(show(), "the viewer opens again"))
                .thenScreenshot(20, "recipe-unlocked")
                .then(SETTLE, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> TeamProgress.forget(level.getServer(), CoreTeams.teamOf(level.getServer(),
                        player[0]), TestProgression.FUSION.id()));
    }

    private static boolean show() {
        return ModList.get().isLoaded("emi") ? CoreEmiPlugin.showRecipesOf(TestPress.PRESSING)
                : CoreJeiPlugin.showRecipesOf(TestPress.PRESSING);
    }
}
