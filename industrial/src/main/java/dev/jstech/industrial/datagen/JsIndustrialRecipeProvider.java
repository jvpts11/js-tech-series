/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen;

import dev.jstech.industrial.tool.HandTools;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

/**
 * Generates the module's recipes, every one from data: the forms of its metals from their routes, by machine and by
 * hand; its ores and minerals by the levels of processing; the furnace's dusts and scraps; and the hand tools. Every
 * ingredient is a {@code c:} common tag, so another mod's ingot or ore works as the mod's own.
 */
public class JsIndustrialRecipeProvider extends RecipeProvider {

    public JsIndustrialRecipeProvider(final PackOutput output,
                                      final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(final RecipeOutput recipeOutput) {
        MaterialFormRecipes.generateAll(recipeOutput);
        OreRecipes.generateAll(recipeOutput);
        MetalRecipes.generateAll(recipeOutput);
        tools(recipeOutput);
    }

    /*
     * The hand tools: a hammer's head of four iron on a stick, not an axe's three; a plate of iron bored with flint,
     * the holes the wire is pulled through; a stone bowl with its flint pestle.
     */
    private void tools(final RecipeOutput out) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, HandTools.FORGE_HAMMER.get())
                .pattern("II").pattern("II").pattern(" S")
                .define('I', Items.IRON_INGOT).define('S', Items.STICK)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(out, MaterialFormRecipes.rl("forge_hammer"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, HandTools.DRAW_PLATE.get())
                .pattern(" F ").pattern("III")
                .define('I', Items.IRON_INGOT).define('F', Items.FLINT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(out, MaterialFormRecipes.rl("draw_plate"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, HandTools.MORTAR.get())
                .pattern("  F").pattern("S S").pattern(" S ")
                .define('S', Items.STONE).define('F', Items.FLINT)
                .unlockedBy("has_flint", has(Items.FLINT))
                .save(out, MaterialFormRecipes.rl("mortar"));
    }
}
