/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViews;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestRecipeViews;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's kit of recipe views: the test mod declares the game's stonecutting and a list of fuels, kinds that are
 * no machine's; each lists its recipes from the world, lays each page out as it says, and a kind declared twice stops
 * the game with a paragraph saying so.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeViewGameTests {

    private static final String ARENA = "empty";
    private static final int FUELS = 3;

    private RecipeViewGameTests() {
    }

    @GameTest(template = ARENA)
    public static void kinds_listTheirRecipesAndLayEachPageOut(final GameTestHelper helper) {
        helper.assertTrue(RecipeViews.declared().containsAll(List.of(TestRecipeViews.STONECUTTING,
                TestRecipeViews.FUELS)), "both kinds are declared");
        final List<RecipeHolder<StonecutterRecipe>> cut = TestRecipeViews.STONECUTTING.recipes(helper.getLevel());
        helper.assertTrue(!cut.isEmpty() && cut.size() == helper.getLevel().getRecipeManager()
                .getAllRecipesFor(RecipeType.STONECUTTING).size(), "every stonecutting recipe is listed");
        final RecipeHolder<StonecutterRecipe> first = cut.getFirst();
        final RecipeView page = TestRecipeViews.STONECUTTING.view(first);
        helper.assertTrue(page.slots(RecipeView.Role.INPUT).size() == 1
                && page.slots(RecipeView.Role.INPUT).getFirst().x() == TestRecipeViews.INPUT_X,
                "its input is laid where the kind says");
        final RecipeView.Slot output = page.slots(RecipeView.Role.OUTPUT).getFirst();
        helper.assertTrue(output.x() == TestRecipeViews.OUTPUT_X && output.items().getFirst().is(first.value()
                .getResultItem(RegistryAccess.EMPTY).getItem()), "and its output, what the recipe makes");
        helper.assertTrue(page.arrow().isPresent() && page.arrow().get().x() == TestRecipeViews.ARROW_X,
                "with its arrow between");
        helper.assertTrue(first.id().equals(TestRecipeViews.STONECUTTING.idOf(first)), "each page has its id");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void anyValue_isAKindOfRecipe(final GameTestHelper helper) {
        final List<TestRecipeViews.Fuel> fuels = TestRecipeViews.FUELS.recipes(helper.getLevel());
        helper.assertTrue(fuels.size() == FUELS, "the test mod's fuels are listed; got " + fuels.size());
        final RecipeView page = TestRecipeViews.FUELS.view(fuels.getFirst());
        final String line = page.lines().getFirst().text().getString();
        helper.assertTrue(line.equals("Burns 1600 ticks"), "its line says how long it burns; got " + line);
        helper.assertTrue(TestRecipeViews.FUELS.icon().getCount() == 1, "and it is shown by its icon");
        try {
            RecipeViews.of(JsTests.MODID, "test_fuels", TestRecipeViews.FUELS_TITLE, TestRecipeViews.Fuel.class)
                    .layout((fuel, laid) -> { }).declare();
            helper.fail("a kind declared twice is refused");
        } catch (final DeclarationError refused) {
            helper.assertTrue(refused.getMessage().contains("test_fuels"), "saying which kind");
        }
        helper.succeed();
    }
}
