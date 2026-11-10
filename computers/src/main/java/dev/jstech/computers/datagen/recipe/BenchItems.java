/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import static dev.jstech.computers.datagen.recipe.BenchRecipes.bench;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.idOf;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.nugget;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.of;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.recipe;

import dev.jstech.computers.CoolingItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.BoardParts;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.JsCore;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import java.util.List;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * What the benches' recipes are written with (computers/docs/design/recipes.md): each era's etched board, chips and
 * case fan, the board's components, the plates, sheets and wires every box is made of, and a bench recipe written
 * from its ingredients' counts.
 */
final class BenchItems {

    /* The prefix each era's items carry, the Standard's none. */
    static final List<String> PREFIX = List.of("vintage_", "legacy_", "transition_", "", "advanced_");

    private BenchItems() {
    }

    /* A bench recipe of {@code era} making one {@code result}, saved under its id: each ingredient, then its count. */
    static void made(final RecipeOutput output, final Era era, final ItemLike result, final Object... ingredients) {
        made(output, era, idOf(result).getPath(), result, 1, ingredients);
    }

    /* A bench recipe of {@code era} making {@code count} of {@code result}, saved as {@code name}. */
    static void made(final RecipeOutput output, final Era era, final String name, final ItemLike result,
                     final int count, final Object... ingredients) {
        final ChainRecipeItems.Saved made = recipe(bench(era), name);
        for (int i = 0; i < ingredients.length; i += 2) {
            final int n = (Integer) ingredients[i + 1];
            if (ingredients[i] instanceof Ingredient ingredient) {
                made.builder().input(ingredient, n);
            } else {
                made.builder().input((ItemLike) ingredients[i], n);
            }
        }
        made.builder().output(result, count);
        made.save(output);
    }

    /* A Crafting Table recipe making {@code count} of {@code result} from {@code rows}, each cell's item by its key. */
    static void grid(final RecipeOutput output, final ItemLike result, final int count, final String[] rows,
                     final Object... key) {
        final ShapedRecipeBuilder shaped = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, count);
        for (final String row : rows) {
            shaped.pattern(row);
        }
        for (int i = 0; i < key.length; i += 2) {
            shaped.define((Character) key[i], (ItemLike) key[i + 1]);
        }
        shaped.unlockedBy("has_it", InventoryChangeTrigger.TriggerInstance.hasItems((ItemLike) key[1]))
                .save(output, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "crafting/"
                        + idOf(result).getPath()));
    }

    static String[] rows(final String... rows) {
        return rows;
    }

    /* The ingredient lists {@code lists}, one after the other. */
    static Object[] join(final Object[]... lists) {
        int size = 0;
        for (final Object[] list : lists) {
            size += list.length;
        }
        final Object[] out = new Object[size];
        int at = 0;
        for (final Object[] list : lists) {
            System.arraycopy(list, 0, out, at, list.length);
            at += list.length;
        }
        return out;
    }

    /* J's Computers' item {@code path}; a missing one fails the data generation. */
    static Item item(final String path) {
        return registered(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
    }

    /* J's Core's item {@code path}. */
    static Item coreItem(final String path) {
        return registered(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path));
    }

    /* {@code era}'s item {@code path}, with the era's prefix: the Standard's bare. */
    static Item ofEra(final Era era, final String path) {
        return item(PREFIX.get(index(era)) + path);
    }

    /* The board component whose item is {@code path}: "storage_sata". */
    static Item part(final String path) {
        return BoardParts.byPath(path).item().get();
    }

    /* The chip of {@code era}'s die {@code path} among {@code dies}. */
    static Item chip(final List<ChainChips.Die> dies, final Era era, final String path) {
        return dies.stream().filter(die -> die.era() == era && die.path().equals(path)).findFirst()
                .orElseThrow(() -> new IllegalStateException("no die of " + path + " in the " + era.path()))
                .chip().get();
    }

    static Item board(final Era era) {
        return ChainParts.ETCHED_BOARDS.get(index(era)).get();
    }

    static Item populated(final Era era) {
        return ChainParts.POPULATED_BOARDS.get(index(era)).get();
    }

    static Item controller(final Era era) {
        return ChainParts.CONTROLLER_CHIPS.get(index(era)).get();
    }

    static Item memory(final Era era) {
        return ChainParts.MEMORY_CHIPS.get(index(era)).get();
    }

    static Item caseFan(final Era era) {
        return switch (era.path()) {
            case "vintage" -> CoolingItems.VINTAGE_CASE_FAN.get();
            case "legacy" -> CoolingItems.LEGACY_CASE_FAN.get();
            case "transition" -> CoolingItems.TRANSITION_CASE_FAN.get();
            case "standard" -> CoolingItems.STANDARD_CASE_FAN.get();
            default -> CoolingItems.ADVANCED_CASE_FAN.get();
        };
    }

    /* The plating of a contact: tin in the Vintage, gold after. */
    static Item plating(final Era era) {
        return era == HardwareEra.VINTAGE ? nugget(Element.TIN) : Items.GOLD_NUGGET;
    }

    /* The plastic of a device's box: PVC in the Vintage, polycarbonate after. */
    static Item plastic(final Era era) {
        return era == HardwareEra.VINTAGE ? pvc() : polycarbonate();
    }

    static Item steelPlate() {
        return of(SeriesAlloys.STEEL, Forms.PLATE);
    }

    static Item steelSheet() {
        return of(SeriesAlloys.STEEL, Forms.SHEET);
    }

    static Item steelRod() {
        return of(SeriesAlloys.STEEL, Forms.ROD);
    }

    static Item aluminiumPlate() {
        return of(Element.ALUMINIUM, Forms.PLATE);
    }

    static Item aluminiumSheet() {
        return of(Element.ALUMINIUM, Forms.SHEET);
    }

    static Item pvc() {
        return of(SeriesCompounds.PVC, Forms.SHEET);
    }

    static Item polycarbonate() {
        return of(SeriesCompounds.POLYCARBONATE, Forms.SHEET);
    }

    static Item copperWire() {
        return of(Element.COPPER, Forms.WIRE);
    }

    static Item copperFoil() {
        return of(Element.COPPER, Forms.FOIL);
    }

    static Item glassLens() {
        return of(SeriesCompounds.GLASS, Forms.LENS);
    }

    static int index(final Era era) {
        return ChainParts.ERAS.indexOf(era);
    }

    private static Item registered(final ResourceLocation id) {
        final Item found = BuiltInRegistries.ITEM.get(id);
        if (found == Items.AIR) {
            throw new IllegalStateException("no item " + id);
        }
        return found;
    }
}
