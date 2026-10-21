/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.kubejs;

import dev.jstech.core.machine.ProcessingRecipe;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.FluidStackComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemStackComponent;
import dev.latvian.mods.kubejs.recipe.component.ListRecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.SizedFluidIngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.SizedIngredientComponent;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.util.IntBounds;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * How a script writes a recipe of any processing machine, the Core's or any mod's, under the machine kind's id: what it
 * makes and what it takes, then the rest by name, as the recipe's file has them.
 *
 * <pre>{@code
 * ServerEvents.recipes(event => {
 *   event.recipes.jsindustrial.macerating(['2x jscore:iron_dust'], ['#c:ores/iron']).ticks(200).energyPerTick(40)
 *   event.recipes.mymod.fusing(['mymod:alloy'], ['mymod:a', 'mymod:b']).knowledge('mymod:fusion')
 * })
 * }</pre>
 *
 * The matter of other states, the byproducts and the temperature and pressure a recipe runs between have no function of
 * their own: a script writes them with {@code event.custom}, as the recipe's file does.
 */
final class ProcessingKubeSchema {

    // A recipe may make only fluids or take only fluids, so every list may be empty.
    static final RecipeKey<List<ItemStack>> OUTPUTS = listOf(ItemStackComponent.ITEM_STACK.instance())
            .outputKey("outputs");
    static final RecipeKey<List<SizedIngredient>> INPUTS = listOf(SizedIngredientComponent.FLAT.instance())
            .inputKey("inputs").optional(List.of());
    static final RecipeKey<List<FluidStack>> FLUID_OUTPUTS = listOf(FluidStackComponent.FLUID_STACK.instance())
            .outputKey("fluid_outputs").optional(List.of()).functionNames("fluidOutputs");
    static final RecipeKey<List<SizedFluidIngredient>> FLUID_INPUTS = listOf(SizedFluidIngredientComponent.FLAT
            .instance()).inputKey("fluid_inputs").optional(List.of()).functionNames("fluidInputs");
    static final RecipeKey<Integer> TICKS = NumberComponent.POSITIVE_INT.instance().otherKey("ticks")
            .optional(ProcessingRecipe.DEFAULT_TICKS);
    static final RecipeKey<Integer> ENERGY_PER_TICK = NumberComponent.NON_NEGATIVE_INT.instance()
            .otherKey("energy_per_tick").optional(0).functionNames("energyPerTick", "energy");
    static final RecipeKey<List<SizedIngredient>> CATALYSTS = listOf(SizedIngredientComponent.FLAT.instance())
            .inputKey("catalysts").optional(List.of());
    static final RecipeKey<String> KNOWLEDGE = StringComponent.ID.instance().otherKey("knowledge").defaultOptional();
    /** Every processing machine's recipes, written as above. */
    static final RecipeSchema SCHEMA = new RecipeSchema(OUTPUTS, INPUTS, FLUID_OUTPUTS, FLUID_INPUTS, TICKS,
            ENERGY_PER_TICK, CATALYSTS, KNOWLEDGE).constructor(OUTPUTS, INPUTS);

    private ProcessingKubeSchema() {
    }

    private static <T> ListRecipeComponent<T> listOf(final RecipeComponent<T> component) {
        return ListRecipeComponent.create(component, false, false, IntBounds.OPTIONAL, Optional.empty());
    }
}
