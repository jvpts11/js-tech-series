/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.integration.rei.CoreReiPlugin;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.TestRecipeViews;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.neoforged.fml.ModList;

/**
 * The Core's bridge to REI as a player meets it, with REI installed (-PwithRei): the test press's two recipes under
 * their own kind, the game's stonecutting and the test mod's fuels declared for the viewers, each shown in REI from
 * the same declarations JEI and EMI show; and the press's page drawn. Without REI the test passes empty.
 */
public final class ReiRecipeViewClientTests {

    private static final String REI = "roughlyenoughitems";
    private static final int SETTLE = 4;
    private static final int VIEWER_WAIT = 400;
    private static final int PRESS_RECIPES = 2;
    private static final int FUELS = 3;
    /* The saw's cuts, the joins and the facades. */
    private static final int MICROBLOCK_RULES = 3;

    private ReiRecipeViewClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void rei_showsTheKitsKindsAsJeiAndEmiDo(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded(REI)) {
            ctx.then(0, () -> { });
            return;
        }
        ctx.thenWaitUntil(() -> CoreReiPlugin.recipesShown(TestPress.PRESSING.id()) == PRESS_RECIPES, VIEWER_WAIT,
                        "REI showing the test press's two recipes")
                .thenAssert(0, () -> CoreReiPlugin.recipesShown(TestRecipeViews.STONECUTTING.id()) > 0,
                        "the game's stonecutting, declared for the viewers")
                .thenAssert(0, () -> CoreReiPlugin.recipesShown(TestRecipeViews.FUELS.id()) == FUELS,
                        "and the test mod's fuels, each one")
                .then(0, () -> ctx.assertTrue(CoreReiPlugin.showRecipes(TestPress.PRESSING.id()),
                        "REI opens on the test press's recipes"))
                .thenScreenshot(SETTLE, "rei-test-pressing")
                .then(0, () -> ctx.assertTrue(CoreReiPlugin.showRecipes(TestRecipeViews.FUELS.id()),
                        "and on the fuels"))
                .thenScreenshot(SETTLE, "rei-test-fuels")
                .thenAssert(0, () -> CoreReiPlugin.recipesShown(CoreReiPlugin.multiblocks()) > 0,
                        "the multiblocks are shown")
                .then(0, () -> ctx.assertTrue(CoreReiPlugin.showRecipes(CoreReiPlugin.multiblocks()),
                        "REI opens on them, each with its preview"))
                .thenScreenshot(SETTLE, "rei-multiblocks")
                .then(0, () -> ctx.assertTrue(CoreReiPlugin.microblockRulesShown() == MICROBLOCK_RULES,
                        "and the microblocks' three rules are among its crafting recipes; there are "
                                + CoreReiPlugin.microblockRulesShown()))
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }
}
