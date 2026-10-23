/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen;

import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUse;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.material.IndustrialMaterials;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The furnace's recipes of the metals J's Industrial uses: every metal's dust melts back into its ingot when a
 * furnace reaches its melting point, so grinding an ore and smelting the dust gives two ingots; and a scrap melts into
 * a nugget, a loss, half of the metal it holds, so nobody keeps a chest of scraps for the electric arc furnace, which
 * melts them whole.
 */
public final class MetalRecipes {

    private static final float DUST_EXPERIENCE = 0.7F;
    private static final float SCRAP_EXPERIENCE = 0.1F;
    private static final int SMELTING_TICKS = 200;
    private static final int BLASTING_TICKS = 100;

    private MetalRecipes() {
    }

    /** Generates the furnace's recipes of every metal the mod uses. */
    public static void generateAll(final RecipeOutput out) {
        final Set<Material> metals = new LinkedHashSet<>();
        for (final MaterialUse use : MaterialUses.all()) {
            if (use.modid().equals(JsIndustrial.MODID)) {
                metals.add(use.material());
            }
        }
        for (final Material metal : metals) {
            final String path = metal.id().getPath();
            if (IndustrialMaterials.meltsInAFurnace(metal) && MaterialUses.used(metal, Forms.DUST)
                    && MaterialUses.used(metal, Forms.INGOT)) {
                cook(out, Forms.DUST.itemTag(metal), MaterialUses.item(metal, Forms.INGOT).get(), DUST_EXPERIENCE,
                        path + "_dust");
            }
            if (MaterialUses.used(metal, IndustrialMaterials.SCRAP) && MaterialUses.used(metal, Forms.NUGGET)) {
                cook(out, IndustrialMaterials.SCRAP.itemTag(metal), MaterialUses.item(metal, Forms.NUGGET).get(),
                        SCRAP_EXPERIENCE, path + "_scrap");
            }
        }
    }

    private static void cook(final RecipeOutput out, final TagKey<Item> input, final Item result,
                             final float experience, final String name) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, experience,
                        SMELTING_TICKS)
                .unlockedBy("has_" + name, MaterialFormRecipes.has(input))
                .save(out, MaterialFormRecipes.rl("smelting/" + name));
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, experience,
                        BLASTING_TICKS)
                .unlockedBy("has_" + name, MaterialFormRecipes.has(input))
                .save(out, MaterialFormRecipes.rl("blasting/" + name));
    }
}
