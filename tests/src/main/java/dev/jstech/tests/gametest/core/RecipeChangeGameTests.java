/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.tests.JsTests;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Recipes changed as data: the test mod's data pack takes one recipe away by its id and another by what it makes,
 * swaps the knowledge book for a jigsaw in every recipe, puts a recipe in another's place and adds one the same way;
 * a file that asks for a mod that is not installed is passed over, and the recipe it would take away stays.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeChangeGameTests {

    private static final String ARENA = "empty";

    private RecipeChangeGameTests() {
    }

    @GameTest(template = ARENA)
    public static void recipeChanges_takeRecipesAwayByIdAndByWhatTheyMake(final GameTestHelper helper) {
        helper.assertTrue(recipe(helper, "change/removed").isEmpty(), "the recipe named by its id is gone");
        helper.assertTrue(recipe(helper, "change/by_output").isEmpty(),
                "the recipe making the item the change names is gone");
        helper.assertTrue(recipe(helper, "change/kept").isPresent(),
                "the recipe only a passed-over file would take away stays");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void recipeChanges_swapItemsAndReplaceRecipes(final GameTestHelper helper) {
        final Recipe<?> swapped = recipe(helper, "change/swapped").orElseThrow().value();
        boolean jigsaw = false;
        boolean book = false;
        for (final Ingredient ingredient : swapped.getIngredients()) {
            jigsaw |= ingredient.test(new ItemStack(Items.JIGSAW));
            book |= ingredient.test(new ItemStack(Items.KNOWLEDGE_BOOK));
        }
        helper.assertTrue(jigsaw && !book, "the knowledge book is swapped for a jigsaw");
        final ItemStack replaced = recipe(helper, "change/replaced").orElseThrow().value()
                .getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(replaced.is(Items.RED_SAND), "the recipe put in the other's place makes red sand; got "
                + replaced);
        final ItemStack added = recipe(helper, "change/added").orElseThrow().value()
                .getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(added.is(Items.CLAY), "and a recipe put in no other's place is added; got " + added);
        helper.succeed();
    }

    private static Optional<RecipeHolder<?>> recipe(final GameTestHelper helper, final String path) {
        return helper.getLevel().getServer().getRecipeManager()
                .byKey(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path));
    }
}
