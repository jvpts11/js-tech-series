/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.material.Material;
import dev.jstech.core.matter.MatterStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.jetbrains.annotations.Nullable;

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
    private final List<SizedIngredient> catalysts = new ArrayList<>();
    private final List<MatterStack> matterInputs = new ArrayList<>();
    private final List<MatterStack> matterOutputs = new ArrayList<>();
    private final List<Byproduct> byproducts = new ArrayList<>();
    private RecipeConditions conditions = RecipeConditions.ANY;
    private int ticks = ProcessingRecipe.DEFAULT_TICKS;
    private int energyPerTick;
    private @Nullable ResourceLocation knowledge;

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

    /** An item that must be in the machine, in a slot of its own, and is not used up. */
    public ProcessingRecipeBuilder catalyst(final Ingredient ingredient, final int count) {
        catalysts.add(new SizedIngredient(ingredient, count));
        return this;
    }

    /** So many millibuckets of {@code material}, at the open air's pressure, taken from a tank of matter. */
    public ProcessingRecipeBuilder matterInput(final Material material, final long millibuckets) {
        matterInputs.add(new MatterStack(material, millibuckets));
        return this;
    }

    /** So many millibuckets of {@code material} made into a tank of matter. */
    public ProcessingRecipeBuilder matterOutput(final Material material, final long millibuckets) {
        matterOutputs.add(new MatterStack(material, millibuckets));
        return this;
    }

    /** {@code stack} made besides, by {@code chance}, from above none to always. */
    public ProcessingRecipeBuilder byproduct(final ItemStack stack, final float chance) {
        byproducts.add(new Byproduct(stack, chance));
        return this;
    }

    /** {@code stack} made besides, {@code share} of it each time, counted by the machine: never by chance. */
    public ProcessingRecipeBuilder countedByproduct(final ItemStack stack, final float share) {
        byproducts.add(Byproduct.counted(stack, share));
        return this;
    }

    /** The temperature, in kelvin, and the pressure, in kilopascals, the recipe runs between. */
    public ProcessingRecipeBuilder conditions(final RecipeConditions value) {
        this.conditions = value;
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

    /** The knowledge the machine's owner's team must have before the machine runs it. */
    public ProcessingRecipeBuilder needs(final ResourceLocation id) {
        this.knowledge = Objects.requireNonNull(id, "id");
        return this;
    }

    /** The recipe as it stands. */
    public ProcessingRecipe build() {
        return new ProcessingRecipe(kind, inputs, fluidInputs, outputs, fluidOutputs, ticks, energyPerTick, catalysts,
                matterInputs, matterOutputs, byproducts, conditions, Optional.ofNullable(knowledge));
    }

    /**
     * Hands the recipe to the provider's output under {@code id}.
     *
     * @throws IllegalStateException when it takes nothing or makes nothing, or its time or energy is out of range
     */
    public void save(final RecipeOutput output, final ResourceLocation id) {
        if (inputs.isEmpty() && fluidInputs.isEmpty() && matterInputs.isEmpty()
                || outputs.isEmpty() && fluidOutputs.isEmpty() && matterOutputs.isEmpty() && byproducts.isEmpty()) {
            throw new IllegalStateException("the processing recipe " + id + " takes something and makes something");
        }
        if (ticks < 1 || energyPerTick < 0) {
            throw new IllegalStateException("the processing recipe " + id + " needs at least one tick and no negative"
                    + " energy, not " + ticks + " ticks at " + energyPerTick);
        }
        output.accept(id, build(), null);
    }
}
