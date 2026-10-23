/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen;

import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUse;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.blockentity.MaceratorBlockEntity;
import java.util.Map;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Emits the machine recipe of every material form J's Industrial declares it uses, from the material's ingot.
 *
 * <p>Adding a production route for a new form is a single entry in {@link #PRODUCTIONS}; making a form of a material
 * is declaring its use in {@link IndustrialModule}. No other datagen files need to change.
 *
 * <p>Special-case recipes (ore doubling, dust to ingot smelting) live in {@link JsIndustrialRecipeProvider} because
 * they use fixed input tags or vanilla item outputs that are not derivable from the material and the form alone.
 */
public final class MaterialFormRecipes {

    @FunctionalInterface
    private interface IFormRecipe {
        void generate(RecipeOutput out, Material material, Form form);
    }

    /** How long pressing and grinding (ingots and ores alike) take at Tier 1, in ticks. */
    private static final int COMPRESSING_TICKS = 120;
    static final int MACERATING_TICKS = 200;

    private static final Map<Form, IFormRecipe> PRODUCTIONS = Map.of(
            // Compress one ingot into one plate.
            Forms.PLATE, (out, material, form) -> ProcessingRecipeBuilder.of(IndustrialModule.COMPRESSING)
                    .input(ingot(material), 1)
                    .output(MaterialUses.item(material, form).get(), 1)
                    .ticks(COMPRESSING_TICKS).energyPerTick(CompressorBlockEntity.FE_PER_TICK)
                    .save(out, rl("compressing/" + id(material, form))),
            // Macerate one ingot into one dust.
            Forms.DUST, (out, material, form) -> macerating(out, ingot(material),
                    new ItemStack(MaterialUses.item(material, form).get(), 1), id(material, form)));

    private MaterialFormRecipes() {
    }

    /** Generates the machine recipe of every form the mod uses that has a production route. */
    public static void generateAll(final RecipeOutput out) {
        for (final MaterialUse use : MaterialUses.all()) {
            final IFormRecipe recipe = PRODUCTIONS.get(use.form());
            if (use.modid().equals(JsIndustrial.MODID) && recipe != null) {
                recipe.generate(out, use.material(), use.form());
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

    /* The recipe's name: its material, from its ingot, to its form, "iron_ingot_to_plate". */
    private static String id(final Material material, final Form form) {
        return material.id().getPath() + "_" + Forms.INGOT.id().getPath() + "_to_" + form.id().getPath();
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }

    /* Any ingot of the material, by its common tag. */
    private static Ingredient ingot(final Material material) {
        return Ingredient.of(Forms.INGOT.itemTag(material));
    }
}
