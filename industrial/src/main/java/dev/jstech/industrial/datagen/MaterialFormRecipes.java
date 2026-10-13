/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen;

import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.material.MaterialForm;
import dev.jstech.core.material.MaterialItems;
import dev.jstech.core.material.ModMaterial;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.blockentity.MaceratorBlockEntity;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Locale;

/**
 * Loops over every active (material, form) pair and emits the appropriate machine recipe.
 *
 * <p>Adding a production route for a new form is a single entry in {@link #PRODUCTIONS}.
 * Activating that form for a material is a single addition to {@link ModMaterial#activeModForms()}.
 * No other datagen files need to change.
 *
 * <p>Special-case recipes (ore doubling, dust→ingot smelting) live in
 * {@link JsIndustrialRecipeProvider} because they use fixed input tags or vanilla item outputs
 * that are not derivable from the material/form pair alone.
 */
public final class MaterialFormRecipes {

    @FunctionalInterface
    private interface IFormRecipe {
        void generate(RecipeOutput out, ModMaterial mat, MaterialForm form);
    }

    private static final EnumMap<MaterialForm, IFormRecipe> PRODUCTIONS = new EnumMap<>(MaterialForm.class);
    /** How long pressing and grinding (ingots and ores alike) take at Tier 1, in ticks. */
    private static final int COMPRESSING_TICKS = 120;
    static final int MACERATING_TICKS = 200;

    static {
        // Compress one ingot into one plate.
        PRODUCTIONS.put(MaterialForm.PLATE, (out, mat, form) ->
                ProcessingRecipeBuilder.of(IndustrialModule.COMPRESSING)
                        .input(cTag(mat, MaterialForm.INGOT), 1)
                        .output(MaterialItems.get(mat, form).get(), 1)
                        .ticks(COMPRESSING_TICKS).energyPerTick(CompressorBlockEntity.FE_PER_TICK)
                        .save(out, rl("compressing/" + id(mat, MaterialForm.INGOT, form))));

        // Macerate one ingot into one dust.
        PRODUCTIONS.put(MaterialForm.DUST, (out, mat, form) ->
                macerating(out, cTag(mat, MaterialForm.INGOT),
                        new ItemStack(MaterialItems.get(mat, form).get(), 1), id(mat, MaterialForm.INGOT, form)));

        /*
         * BOLT, ROD, GEAR: production routes not yet established.
         * Add one entry here per form when input, machine and ratio are defined.
         */
    }

    private MaterialFormRecipes() {}

    /** Generates all machine recipes that have a defined production route. */
    public static void generateAll(final RecipeOutput out) {
        for (final ModMaterial mat : ModMaterial.values()) {
            for (final MaterialForm form : mat.activeModForms()) {
                final IFormRecipe recipe = PRODUCTIONS.get(form);
                if (recipe != null) {
                    recipe.generate(out, mat, form);
                }
            }
        }
    }

    /** Adds a macerating recipe under the {@code macerating/} prefix; the one place the grinding cost is set. */
    static void macerating(final RecipeOutput out, final Ingredient ingredient, final ItemStack result,
                           final String name) {
        ProcessingRecipeBuilder.of(IndustrialModule.MACERATING).input(ingredient, 1).output(result)
                .ticks(MACERATING_TICKS).energyPerTick(MaceratorBlockEntity.FE_PER_TICK)
                .save(out, rl("macerating/" + name));
    }

    /** The {@code c:} common item tag at {@code path}. */
    static TagKey<Item> cTag(final String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static String id(final ModMaterial mat, final MaterialForm inputForm, final MaterialForm outputForm) {
        return mat.materialName() + "_" + inputForm.name().toLowerCase(Locale.ROOT)
                + "_to_" + outputForm.name().toLowerCase(Locale.ROOT);
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }

    private static Ingredient cTag(final ModMaterial mat, final MaterialForm inputForm) {
        return Ingredient.of(cTag(inputForm.tagPath(mat.materialName())));
    }
}
