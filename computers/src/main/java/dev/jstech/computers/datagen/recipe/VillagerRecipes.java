/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.villager.ComputerVillagers;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.material.Forms;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The two job sites, made at the Crafting Table as vanilla's are (computers/docs/design/recipes.md): two things of the
 * trade on four planks of any wood. The Programmer's Desk is the floppy disk and the sheet of code on it; the Repair
 * Bench the solder and the iron of its tools.
 */
final class VillagerRecipes {

    private VillagerRecipes() {
    }

    static void write(final RecipeOutput output) {
        jobSite(output, ComputerVillagers.PROGRAMMERS_DESK, ComputingModule.FLOPPY_DISK, Items.PAPER);
        jobSite(output, ComputerVillagers.REPAIR_BENCH, ChainRecipeItems.of(SeriesAlloys.SOLDER, Forms.WIRE),
                Items.IRON_INGOT);
    }

    /* {@code site} from {@code left} and {@code right} over two rows of planks. */
    private static void jobSite(final RecipeOutput output, final ItemLike site, final ItemLike left,
                                final ItemLike right) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, site)
                .pattern("AB").pattern("PP").pattern("PP")
                .define('A', left).define('B', right).define('P', ItemTags.PLANKS)
                .unlockedBy("has_it", InventoryChangeTrigger.TriggerInstance.hasItems(left))
                .save(output, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "crafting/"
                        + BuiltInRegistries.ITEM.getKey(site.asItem()).getPath()));
    }
}
