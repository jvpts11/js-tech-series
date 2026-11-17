/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every ore of the series, and the raw form it breaks into, is good for something: a recipe takes it, so a recipe
 * viewer shows the player what to do with what they mined.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OreUsesGameTests {

    private static final String ARENA = "empty";
    /* The series' mods, whose ores are checked. */
    private static final Set<String> SERIES = Set.of("jscore", "jsc", "jsindustrial", "jsspace");
    /*
     * The Moon's and Mars' own minerals: what J's Space's own recipes will need, not yet built (its minerals page,
     * "To build"); mined and kept until then.
     */
    private static final List<String> AWAITING_SPACE = List.of("ilmenite", "armalcolite", "tranquillityite",
            "merrillite", "native_iron", "olivine", "jarosite", "gypsum");

    private OreUsesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void ores_everyOreAndItsRawFormHaveAUse(final GameTestHelper helper) {
        final List<RecipeHolder<?>> recipes = List.copyOf(helper.getLevel().getRecipeManager().getRecipes());
        final List<String> unused = new ArrayList<>();
        int checked = 0;
        for (final Item item : BuiltInRegistries.ITEM) {
            final String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            final String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            final ItemStack stack = new ItemStack(item);
            if (!SERIES.contains(namespace) || !stack.is(Tags.Items.ORES) && !stack.is(Tags.Items.RAW_MATERIALS)
                    || AWAITING_SPACE.stream().anyMatch(path::contains)) {
                continue;
            }
            checked++;
            if (recipes.stream().noneMatch(recipe -> takes(recipe, stack))) {
                unused.add(namespace + ":" + path);
            }
        }
        helper.assertTrue(checked > 0, "the series has ores");
        helper.assertTrue(unused.isEmpty(), unused.size() + " of " + checked + " ores and raw forms no recipe takes: "
                + unused);
        helper.succeed();
    }

    /* Whether {@code recipe} takes {@code stack} as one of its ingredients. */
    private static boolean takes(final RecipeHolder<?> recipe, final ItemStack stack) {
        for (final Ingredient ingredient : recipe.value().getIngredients()) {
            if (ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }
}
