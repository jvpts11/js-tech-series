/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.recipeview.RecipeViewers;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.core.guide.GuideSearch;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.integration.emi.CoreEmiPlugin;
import dev.jstech.core.integration.jei.CoreJeiPlugin;
import dev.jstech.core.integration.rei.CoreReiPlugin;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

/**
 * The manual beside a recipe viewer, JEI or EMI, whichever the game runs: the search finds the recipes the manual
 * shows and those an item goes into, a recipe on a page opens in the viewer and closing the viewer comes back to the
 * page, and the manual key held over an item in the viewer's list opens its page.
 */
public final class ManualRecipeViewClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    /* Far enough into a hold for its bar to show, and early enough to see it before the page opens. */
    private static final float HALF_HELD = 0.35F;
    /* How far apart the points looked at for an item in the viewer's list are: less than half a slot. */
    private static final int SCAN_STEP = 6;

    private ManualRecipeViewClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void manual_searchFindsTheRecipesItShowsAndTheirUses(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, ""))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> manual().search("block of iron"))
                .thenAssert(SETTLE, () -> manual().found().stream().anyMatch(hit -> hit.found()
                        == GuideSearch.Found.RECIPE && hit.target().equals(TestGuide.PRESS)),
                        "the recipe making a block of iron is found on the press's page")
                .then(0, () -> manual().search("redstone"))
                .thenAssert(SETTLE, () -> manual().found().stream().anyMatch(hit -> hit.found()
                        == GuideSearch.Found.USE && hit.target().equals(TestGuide.PRESS)
                        && hit.line().startsWith("Block of Gold")),
                        "and redstone is found going into the press's recipe for a block of gold")
                .thenScreenshot(2, "manual_search_recipes")
                .then(0, ManualRecipeViewClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 400)
    public static void manual_aRecipeOnAPageOpensInTheViewerAndComesBack(final ClientTestContext ctx) {
        ctx.then(0, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.PRESS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, ManualRecipeViewClientTests::toRecipes)
                .thenAssert(SETTLE, () -> manual().recipePoint() != null, "a spread shows the press's recipes")
                .then(0, () -> {
                    final int[] point = manual().recipePoint();
                    ctx.pointAt(point[0] + 0.5, point[1] + 0.5);
                })
                .thenAssert(SETTLE, () -> manual().pointing(), "the hand shows over a recipe, which takes a click")
                .thenScreenshot(0, "manual_recipe_to_viewer")
                .then(0, () -> {
                    final int[] point = manual().recipePoint();
                    ctx.click(point[0] + 0.5, point[1] + 0.5);
                })
                .thenWaitUntil(() -> ctx.mc().screen != null && !(ctx.mc().screen instanceof ManualScreen), OPEN,
                        "the viewer opens on the recipe clicked")
                .thenScreenshot(SETTLE, "manual_recipe_in_viewer")
                .then(0, () -> ctx.mc().screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().recipePoint() != null,
                        "and closing the viewer comes back to the page the recipe is on")
                .then(0, ManualRecipeViewClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    @ClientTest(timeoutTicks = 400)
    public static void manualKey_heldOverAnItemInTheViewersListOpensItsPage(final ClientTestContext ctx) {
        final ItemStack upgrade = new ItemStack(TestPress.UPGRADE.get());
        ctx.afterTest(() -> {
            manualKey().setDown(false);
            search("");
        });
        // The test player is in creative, where the inventory opens as the creative one.
        ctx.then(0, () -> ctx.mc().setScreen(new InventoryScreen(ctx.player())))
                .thenAwaitScreen(AbstractContainerScreen.class, OPEN)
                .thenAssert(0, () -> search(upgrade.getHoverName().getString()), "a viewer runs to search")
                // A viewer still building its list as the test starts finds the upgrade once it is built.
                .thenWaitUntil(() -> pointAtListed(ctx, upgrade), OPEN, "the upgrade is found in the viewer's list")
                .then(2, () -> manualKey().setDown(true))
                .thenWaitUntil(() -> GuideClient.holdProgress() > HALF_HELD, OPEN,
                        "the bar fills while the key is held over it")
                .thenScreenshot(0, "manual_hold_bar_viewer")
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> shows(TestGuide.PRESS), "and the item's page opens when it is full")
                .then(0, () -> manualKey().setDown(false))
                .then(0, ManualRecipeViewClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
    }

    /* Shows the spread holding the first recipes of the press's entry. */
    private static void toRecipes() {
        final GuideBook book = manual().book();
        for (int page = book.pageOf(TestGuide.PRESS).orElseThrow(); page < book.indexAt(); page++) {
            if (book.pages().get(page).pieces().stream().anyMatch(piece -> piece instanceof GuidePiece.Recipes)) {
                manual().goToPage(page);
                return;
            }
        }
    }

    /** Whether the spread shown holds the page where that target starts. */
    private static boolean shows(final String target) {
        final ManualScreen screen = manual();
        final int page = screen.book().pageOf(target).orElse(-2);
        return page >= 0 && screen.spread() == page / 2;
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
    }

    private static KeyMapping manualKey() {
        return KeyActionsClient.mapping(CoreKeys.OPEN_IN_MANUAL);
    }

    /* Types into the search of the viewer the game runs; whether one runs. */
    private static boolean search(final String words) {
        // REI's list lies over the others where it runs beside them, so it is narrowed as well.
        final boolean rei = !ModList.get().isLoaded("roughlyenoughitems") || CoreReiPlugin.search(words);
        return rei && (ModList.get().isLoaded("emi") ? CoreEmiPlugin.search(words) : CoreJeiPlugin.search(words));
    }

    /*
     * Points at the stack in the viewer's list, wherever the viewer laid it: looks across the screen's right half,
     * where the viewers keep their lists, until the viewer says the pointer is over it.
     */
    private static boolean pointAtListed(final ClientTestContext ctx, final ItemStack stack) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        for (int x = width / 2; x < width; x += SCAN_STEP) {
            for (int y = 0; y < height; y += SCAN_STEP) {
                ctx.pointAt(x + 0.5, y + 0.5);
                if (ItemStack.isSameItem(RecipeViewers.hovered(), stack)) {
                    return true;
                }
            }
        }
        return false;
    }
}
