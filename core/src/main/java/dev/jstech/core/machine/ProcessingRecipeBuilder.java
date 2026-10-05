/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Writes a processing recipe from a mod's recipe provider:
 *
 * <pre>{@code
 * ProcessingRecipeBuilder.of(MACERATING).input(Tags.Items.ORES_IRON, 1).output(IRON_DUST, 2).ticks(200)
 *         .energyPerTick(40).save(output, id("iron_dust_from_ore"));
 * }</pre>
 */
public final class ProcessingRecipeBuilder {

    private final ProcessingKind kind;
    private final List<SizedIngredient> inputs = new ArrayList<>();
    private final List<SizedFluidIngredient> fluidInputs = new ArrayList<>();
    private final List<ItemStack> outputs = new ArrayList<>();
    private final List<FluidStack> fluidOutputs = new ArrayList<>();
    private int ticks = ProcessingRecipe.DEFAULT_TICKS;
    private int energyPerTick;

    private ProcessingRecipeBuilder(final ProcessingKind kind) {
        this.kind = kind;
    }

    /** Starts a recipe of {@code kind}. */
    public static ProcessingRecipeBuilder of(final ProcessingKind kind) {
        return new ProcessingRecipeBuilder(kind);
    }

    public ProcessingRecipeBuilder input(final Ingredient ingredient, final int count) {
        inputs.add(new SizedIngredient(ingredient, count));
        return this;
    }

    public ProcessingRecipeBuilder input(final ItemLike item, final int count) {
        inputs.add(SizedIngredient.of(item, count));
        return this;
    }

    public ProcessingRecipeBuilder input(final TagKey<Item> tag, final int count) {
        inputs.add(SizedIngredient.of(tag, count));
        return this;
    }

    public ProcessingRecipeBuilder fluidInput(final Fluid fluid, final int millibuckets) {
        fluidInputs.add(SizedFluidIngredient.of(fluid, millibuckets));
        return this;
    }

    public ProcessingRecipeBuilder fluidInput(final TagKey<Fluid> tag, final int millibuckets) {
        fluidInputs.add(SizedFluidIngredient.of(tag, millibuckets));
        return this;
    }

    public ProcessingRecipeBuilder output(final ItemStack stack) {
        outputs.add(stack.copy());
        return this;
    }

    public ProcessingRecipeBuilder output(final ItemLike item, final int count) {
        outputs.add(new ItemStack(item, count));
        return this;
    }

    public ProcessingRecipeBuilder fluidOutput(final Fluid fluid, final int millibuckets) {
        fluidOutputs.add(new FluidStack(fluid, millibuckets));
        return this;
    }

    public ProcessingRecipeBuilder ticks(final int value) {
        this.ticks = value;
        return this;
    }

    public ProcessingRecipeBuilder energyPerTick(final int value) {
        this.energyPerTick = value;
        return this;
    }

    /** The recipe as it stands. */
    public ProcessingRecipe build() {
        return new ProcessingRecipe(kind, inputs, fluidInputs, outputs, fluidOutputs, ticks, energyPerTick);
    }

    /**
     * Hands the recipe to the provider's output under {@code id}.
     *
     * @throws IllegalStateException when it takes nothing or makes nothing
     */
    public void save(final RecipeOutput output, final ResourceLocation id) {
        if (inputs.isEmpty() && fluidInputs.isEmpty() || outputs.isEmpty() && fluidOutputs.isEmpty()) {
            throw new IllegalStateException("the processing recipe " + id + " takes something and makes something");
        }
        output.accept(id, build(), null);
    }
}
