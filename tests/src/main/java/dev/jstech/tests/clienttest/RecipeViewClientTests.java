/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.core.integration.emi.CoreEmiPlugin;
import dev.jstech.core.integration.jei.CoreJeiPlugin;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.tests.TestPress;
import net.neoforged.fml.ModList;

/**
 * The Core's bridges to the recipe viewers as a player meets them: every mod's processing machines have their recipes
 * shown under their own name, the Industrial mod's and the test mod's alike, with nothing written for the viewer by
 * either mod; and the test press's recipe, which takes items and a fluid and makes items and a fluid, drawn whole.
 * It runs against EMI when EMI is installed (-PwithEmi), which then stands in for JEI, and against JEI otherwise.
 */
public final class RecipeViewClientTests {

    private static final int SETTLE = 4;
    private static final int VIEWER_WAIT = 400;
    /* The grinding recipes the Industrial mod ships: an ore, a raw ore and an ingot into dust. */
    private static final int MACERATING_RECIPES = 3;

    private RecipeViewClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void recipeViewer_showsEveryModsMachineRecipes(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded("jei") && !emi()) {
            // Without a viewer there is nothing to show: the test passes empty, as the viewer steps elsewhere do.
            ctx.then(0, () -> { });
            return;
        }
        ctx.thenWaitUntil(() -> shown(IndustrialModule.MACERATING) > 0, VIEWER_WAIT,
                        "the viewer running with the Core's categories")
                .thenAssert(0, () -> shown(IndustrialModule.MACERATING) >= MACERATING_RECIPES,
                        "the Industrial mod's grinding recipes are all shown")
                .thenAssert(0, () -> shown(IndustrialModule.COMPRESSING) > 0, "and its pressing ones")
                .thenAssert(0, () -> shown(TestPress.PRESSING) == 1,
                        "the test mod's one recipe is shown under its own kind")
                .then(0, () -> ctx.assertTrue(show(TestPress.PRESSING),
                        "the viewer opens on the test press's recipes"))
                .thenScreenshot(SETTLE, "test-pressing")
                .then(0, () -> ctx.assertTrue(show(IndustrialModule.MACERATING), "and on the macerator's"))
                .thenScreenshot(SETTLE, "macerating")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    private static boolean emi() {
        return ModList.get().isLoaded("emi");
    }

    private static long shown(final ProcessingKind kind) {
        return emi() ? CoreEmiPlugin.recipesShown(kind) : CoreJeiPlugin.recipesShown(kind);
    }

    private static boolean show(final ProcessingKind kind) {
        return emi() ? CoreEmiPlugin.showRecipes(kind) : CoreJeiPlugin.showRecipes(kind);
    }
}
