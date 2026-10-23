/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen;

import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Mineral;
import dev.jstech.industrial.material.IndustrialMaterials;
import dev.jstech.industrial.metallurgy.FamilyRoute;
import dev.jstech.industrial.metallurgy.OreLevel;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The recipes of the ores J's Industrial mines, by the three levels of processing: the furnace smelts an "X ore" and
 * the simple minerals, a native metal, an oxide or a carbonate, into one ingot, as the game does; the Macerator
 * grinds any of them, ore or raw, into two dusts of what it is mined for and a little of its byproducts, counted by the
 * machine, never by chance ({@link OreLevel}). A sulfide or an arsenide does not go in a furnace raw. Every mineral of
 * the catalogue the mod uses gets its recipes from its own data: its family, its main element, its byproducts.
 */
public final class OreRecipes {

    private static final float ORE_EXPERIENCE = 0.7F;
    private static final int SMELTING_TICKS = 200;
    private static final int BLASTING_TICKS = 100;

    private OreRecipes() {
    }

    /** Generates the recipes of every ore and mineral the mod uses. */
    public static void generateAll(final RecipeOutput out) {
        for (final Material metal : IndustrialMaterials.MINED) {
            final Item dust = MaterialUses.item(metal, Forms.DUST).get();
            final String path = metal.id().getPath();
            MaterialFormRecipes.macerating(Ingredient.of(Forms.ORE.itemTag(metal)), new ItemStack(dust,
                    OreLevel.MACERATOR.mainOut())).save(out, MaterialFormRecipes.rl("macerating/" + path + "_ore"));
            MaterialFormRecipes.macerating(Ingredient.of(Forms.RAW.itemTag(metal)), new ItemStack(dust,
                    OreLevel.MACERATOR.mainOut())).save(out, MaterialFormRecipes.rl("macerating/raw_" + path));
            // The game smelts its own raw iron, copper and gold; the Core's metals are smelted here.
            if (metal.provided(Forms.RAW).isEmpty()) {
                smelt(out, Ingredient.of(Forms.RAW.itemTag(metal)), metal, "raw_" + path);
                smelt(out, Ingredient.of(Forms.ORE.itemTag(metal)), metal, path + "_ore");
            }
        }
        for (final Material material : SeriesMinerals.all()) {
            material.mineral().ifPresent(mineral -> mineral(out, material, mineral));
        }
    }

    /* A mineral's recipes: ground at the second level, and smelted raw at the first when its family allows. */
    private static void mineral(final RecipeOutput out, final Material material, final Mineral mineral) {
        final Mineral.Content main = mineral.main();
        final String path = material.id().getPath();
        final ProcessingRecipeBuilder ore = ground(Ingredient.of(Forms.ORE.itemTag(material)), mineral);
        final ProcessingRecipeBuilder raw = ground(Ingredient.of(Forms.RAW.itemTag(material)), mineral);
        ore.save(out, MaterialFormRecipes.rl("macerating/" + path + "_ore"));
        raw.save(out, MaterialFormRecipes.rl("macerating/raw_" + path));
        if (FamilyRoute.smeltsRaw(mineral.family().id().getPath())
                && MaterialUses.used(main.material(), Forms.INGOT)) {
            smelt(out, Ingredient.of(Forms.RAW.itemTag(material)), main.material(), "raw_" + path);
            smelt(out, Ingredient.of(Forms.ORE.itemTag(material)), main.material(), path + "_ore");
        }
    }

    /* Two dusts of the main element, and each byproduct's share of the level's, counted. */
    private static ProcessingRecipeBuilder ground(final Ingredient input, final Mineral mineral) {
        final Mineral.Content main = mineral.main();
        final int mainOut = OreLevel.MACERATOR.mainOut();
        final ProcessingRecipeBuilder builder = MaterialFormRecipes.macerating(input,
                new ItemStack(MaterialUses.item(main.material(), Forms.DUST).get(), mainOut));
        for (final Mineral.Content byproduct : mineral.byproducts()) {
            if (!MaterialUses.used(byproduct.material(), Forms.DUST)) {
                continue;
            }
            final double share = OreLevel.MACERATOR.byproduct(main.share(), byproduct.share(), mainOut);
            builder.countedByproduct(new ItemStack(MaterialUses.item(byproduct.material(), Forms.DUST).get()),
                    (float) share);
        }
        return builder;
    }

    /* Smelting and blasting {@code input} into one ingot of {@code metal}, the first level. */
    private static void smelt(final RecipeOutput out, final Ingredient input, final Material metal,
                              final String name) {
        final Item ingot = MaterialUses.item(metal, Forms.INGOT).get();
        SimpleCookingRecipeBuilder.smelting(input, RecipeCategory.MISC, ingot, ORE_EXPERIENCE, SMELTING_TICKS)
                .unlockedBy("has_ore", MaterialFormRecipes.has(Forms.INGOT.itemTag(metal)))
                .save(out, MaterialFormRecipes.rl("smelting/" + name));
        SimpleCookingRecipeBuilder.blasting(input, RecipeCategory.MISC, ingot, ORE_EXPERIENCE, BLASTING_TICKS)
                .unlockedBy("has_ore", MaterialFormRecipes.has(Forms.INGOT.itemTag(metal)))
                .save(out, MaterialFormRecipes.rl("blasting/" + name));
    }
}
