/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import com.mojang.serialization.JsonOps;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * No recipe of a kind that takes exactly its ingredients fits in the lot of another of its kind
 * (computers/docs/design/recipes.md). A recipe fits in another when every item it takes, its tools among them, every
 * liquid and every matter is in the other's, as much or more; a lot of the bigger one, arriving at a machine a part at
 * a time, would then start the smaller one. The chips' classes, the moulded parts, the jigs, the phototools and the
 * parts' own materials keep every pair apart, and this holds them so for every recipe yet to come.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RecipeAntichainGameTests {

    private static final String ARENA = "empty";
    /* How many of the pairs found the failure names. */
    private static final int NAMED = 20;

    private RecipeAntichainGameTests() {
    }

    @GameTest(template = ARENA)
    public static void exactKinds_noRecipeFitsInAnothersLot(final GameTestHelper helper) {
        // Grouped by the machine, not the kind: a later era's bench works every earlier era's bench recipes too.
        final Map<Item, List<Lot>> byMachine = new LinkedHashMap<>();
        for (final RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            // The test mod's own fixture recipes are not the game's.
            if (!holder.id().getNamespace().equals(JsTests.MODID) && holder.value() instanceof ProcessingRecipe recipe
                    && recipe.kind().exact()) {
                final Lot lot = new Lot(holder.id(), lot(recipe), made(recipe));
                for (final ItemLike machine : recipe.kind().machines()) {
                    byMachine.computeIfAbsent(machine.asItem(), item -> new ArrayList<>()).add(lot);
                }
            }
        }
        final Set<String> fits = new TreeSet<>();
        for (final List<Lot> machine : byMachine.values()) {
            for (final Lot big : machine) {
                for (final Lot small : machine) {
                    if (big != small && !big.made().equals(small.made()) && fitsIn(small.lot(), big.lot())) {
                        fits.add(small.id() + " in " + big.id());
                    }
                }
            }
        }
        helper.assertTrue(fits.isEmpty(), fits.size() + " recipes fit in another's lot: "
                + fits.stream().limit(NAMED).collect(Collectors.joining("; ")));
        helper.succeed();
    }

    /* Whether every part of {@code small} is in {@code big}, as much or more. */
    private static boolean fitsIn(final Map<String, Long> small, final Map<String, Long> big) {
        for (final Map.Entry<String, Long> part : small.entrySet()) {
            if (big.getOrDefault(part.getKey(), 0L) < part.getValue()) {
                return false;
            }
        }
        return true;
    }

    /*
     * What a lot of the recipe holds in a machine: each item it takes and each tool it keeps there, by the item when it
     * is one plain item and by the whole ingredient otherwise (a chip of a class), each liquid and each matter, with
     * how much of each.
     */
    private static Map<String, Long> lot(final ProcessingRecipe recipe) {
        final Map<String, Long> lot = new HashMap<>();
        final List<SizedIngredient> items = new ArrayList<>(recipe.inputs());
        items.addAll(recipe.catalysts());
        for (final SizedIngredient item : items) {
            lot.merge(key(item.ingredient()), (long) item.count(), Long::sum);
        }
        for (final SizedFluidIngredient fluid : recipe.fluidInputs()) {
            lot.merge("fluid:" + FluidIngredient.CODEC.encodeStart(JsonOps.INSTANCE, fluid.ingredient()).getOrThrow(),
                    (long) fluid.amount(), Long::sum);
        }
        for (final MatterStack matter : recipe.matterInputs()) {
            lot.merge("matter:" + matter.material().id(), matter.amount(), Long::sum);
        }
        return lot;
    }

    private static String key(final Ingredient ingredient) {
        return ingredient.isSimple() && ingredient.getItems().length == 1
                ? BuiltInRegistries.ITEM.getKey(ingredient.getItems()[0].getItem()).toString()
                : Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow().toString();
    }

    /* What the recipe makes, by its items' ids, or by its liquids' for one that makes none. */
    private static String made(final ProcessingRecipe recipe) {
        if (!recipe.outputs().isEmpty()) {
            return recipe.outputs().stream().map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                    .collect(Collectors.joining(","));
        }
        return recipe.fluidOutputs().stream().map(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid())
                .toString()).collect(Collectors.joining(","));
    }

    /* One recipe's lot: its id, what it holds and what it makes. */
    private record Lot(ResourceLocation id, Map<String, Long> lot, String made) {
    }
}
