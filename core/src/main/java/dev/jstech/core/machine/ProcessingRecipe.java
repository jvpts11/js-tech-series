/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

/**
 * A recipe of a processing machine: items taken by the count, fluids by the millibucket, and what comes out, in so
 * many ticks at so much energy a tick. Every mod's processing machines share it; what tells a grinder's recipes from a
 * press's is their {@link ProcessingKind}, the recipe type each mod declares.
 *
 * <pre>{@code
 * {"type": "jsindustrial:macerating",
 *  "inputs": [{"tag": "c:ores/iron", "count": 1}],
 *  "outputs": [{"id": "jscore:iron_dust", "count": 2}],
 *  "ticks": 200, "energy_per_tick": 40}
 * }</pre>
 *
 * @param kind          the machine kind the recipe is for
 * @param inputs        the items it takes, each with its count
 * @param fluidInputs   the fluids it takes, each with its amount
 * @param outputs       what it makes
 * @param fluidOutputs  the fluids it makes
 * @param ticks         how long it takes, before upgrades
 * @param energyPerTick what it spends each tick, before upgrades
 */
public record ProcessingRecipe(ProcessingKind kind, List<SizedIngredient> inputs,
                               List<SizedFluidIngredient> fluidInputs, List<ItemStack> outputs,
                               List<FluidStack> fluidOutputs, int ticks, int energyPerTick)
        implements Recipe<ProcessingInput> {

    /** How long a recipe takes when its file does not say. */
    public static final int DEFAULT_TICKS = 200;

    public ProcessingRecipe {
        inputs = List.copyOf(inputs);
        fluidInputs = List.copyOf(fluidInputs);
        outputs = List.copyOf(outputs);
        fluidOutputs = List.copyOf(fluidOutputs);
    }

    /**
     * Whether the machine's inputs hold everything the recipe takes: each item input in a slot of its own, with at
     * least its count, and each fluid input in a tank of its own, with at least its amount.
     */
    @Override
    public boolean matches(final ProcessingInput input, final Level level) {
        return assign(inputs, input.items()) != null && assignFluids(fluidInputs, input.fluids()) != null;
    }

    /**
     * The input slot each item input is taken from, in the order of {@link #inputs()}, or null when the slots do not
     * hold them: what a machine takes from when it finishes the recipe.
     */
    @Nullable
    public int[] slotsFor(final ProcessingInput input) {
        return assign(inputs, input.items());
    }

    /** The input tank each fluid input is taken from, or null when the tanks do not hold them. */
    @Nullable
    public int[] tanksFor(final ProcessingInput input) {
        return assignFluids(fluidInputs, input.fluids());
    }

    @Override
    public ItemStack assemble(final ProcessingInput input, final HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(final int width, final int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(final HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst();
    }

    /** The item inputs, each once whatever its count, for the recipe viewers and the autocraft planners. */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        final NonNullList<Ingredient> list = NonNullList.create();
        for (final SizedIngredient input : inputs) {
            list.add(input.ingredient());
        }
        return list;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return kind.type();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /** How a recipe of {@code kind} is read from its file, and checked: it takes something and makes something. */
    static MapCodec<ProcessingRecipe> codec(final ProcessingKind kind) {
        return RecordCodecBuilder.<ProcessingRecipe>mapCodec(instance -> instance.group(
                        SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("inputs", List.of())
                                .forGetter(ProcessingRecipe::inputs),
                        SizedFluidIngredient.FLAT_CODEC.listOf().optionalFieldOf("fluid_inputs", List.of())
                                .forGetter(ProcessingRecipe::fluidInputs),
                        ItemStack.CODEC.listOf().optionalFieldOf("outputs", List.of())
                                .forGetter(ProcessingRecipe::outputs),
                        FluidStack.CODEC.listOf().optionalFieldOf("fluid_outputs", List.of())
                                .forGetter(ProcessingRecipe::fluidOutputs),
                        Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("ticks", DEFAULT_TICKS)
                                .forGetter(ProcessingRecipe::ticks),
                        Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy_per_tick", 0)
                                .forGetter(ProcessingRecipe::energyPerTick))
                .apply(instance, (in, fluidIn, out, fluidOut, ticks, energy) ->
                        new ProcessingRecipe(kind, in, fluidIn, out, fluidOut, ticks, energy)))
                .validate(recipe -> recipe.inputs().isEmpty() && recipe.fluidInputs().isEmpty()
                        ? DataResult.error(() -> "a processing recipe takes something")
                        : recipe.outputs().isEmpty() && recipe.fluidOutputs().isEmpty()
                        ? DataResult.error(() -> "a processing recipe makes something")
                        : DataResult.success(recipe));
    }

    /** How a recipe of {@code kind} is sent to the players. */
    static StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec(final ProcessingKind kind) {
        return StreamCodec.composite(
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ProcessingRecipe::inputs,
                SizedFluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ProcessingRecipe::fluidInputs,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ProcessingRecipe::outputs,
                FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()), ProcessingRecipe::fluidOutputs,
                ByteBufCodecs.VAR_INT, ProcessingRecipe::ticks,
                ByteBufCodecs.VAR_INT, ProcessingRecipe::energyPerTick,
                (in, fluidIn, out, fluidOut, ticks, energy) ->
                        new ProcessingRecipe(kind, in, fluidIn, out, fluidOut, ticks, energy));
    }

    /*
     * Gives each input a slot of its own that satisfies it, trying the inputs in order and each slot not yet taken;
     * a later input that finds no slot sends the search back to try the earlier ones elsewhere. A recipe has a handful
     * of inputs, so the search stays small.
     */
    @Nullable
    private static int[] assign(final List<SizedIngredient> wanted, final List<ItemStack> slots) {
        return assignAll(wanted, slots, SizedIngredient::test);
    }

    /* The same search for the fluids: an input that accepts two tanks must not claim the one a later input needs. */
    @Nullable
    private static int[] assignFluids(final List<SizedFluidIngredient> wanted, final List<FluidStack> tanks) {
        return assignAll(wanted, tanks, SizedFluidIngredient::test);
    }

    @Nullable
    private static <W, C> int[] assignAll(final List<W> wanted, final List<C> contents,
                                          final BiPredicate<W, C> accepts) {
        final int[] chosen = new int[wanted.size()];
        return place(wanted, contents, accepts, chosen, new boolean[contents.size()], 0) ? chosen : null;
    }

    private static <W, C> boolean place(final List<W> wanted, final List<C> contents, final BiPredicate<W, C> accepts,
                                        final int[] chosen, final boolean[] taken, final int next) {
        if (next == wanted.size()) {
            return true;
        }
        for (int slot = 0; slot < contents.size(); slot++) {
            if (!taken[slot] && accepts.test(wanted.get(next), contents.get(slot))) {
                taken[slot] = true;
                chosen[next] = slot;
                if (place(wanted, contents, accepts, chosen, taken, next + 1)) {
                    return true;
                }
                taken[slot] = false;
            }
        }
        return false;
    }
}
