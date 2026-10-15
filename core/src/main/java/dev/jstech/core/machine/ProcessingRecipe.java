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
import dev.jstech.core.matter.MatterStack;
import java.util.ArrayList;
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
 * A recipe of a processing machine: items taken by the count, fluids by the millibucket, matter of every other state by
 * the millibucket at the open air's pressure, and what comes out, in so many ticks at so much energy a tick; catalysts
 * that must be in the machine but are not used up; byproducts that come by chance; and the temperature and pressure it
 * runs between. Every mod's processing machines share it; what tells a grinder's recipes from a press's is their
 * {@link ProcessingKind}, the recipe type each mod declares.
 *
 * <pre>{@code
 * {"type": "jsindustrial:macerating",
 *  "inputs": [{"tag": "c:ores/iron", "count": 1}],
 *  "outputs": [{"id": "jscore:iron_dust", "count": 2}],
 *  "byproducts": [{"item": {"id": "jscore:gold_dust"}, "chance": 0.1}],
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
 * @param catalysts     the items that must be in the machine, each in a slot of its own, and are not used up
 * @param matterInputs  the matter it takes from the machine's tanks of gas and the other states
 * @param matterOutputs the matter it makes
 * @param byproducts    what it may make besides, by chance
 * @param conditions    the temperature and the pressure it runs between
 */
public record ProcessingRecipe(ProcessingKind kind, List<SizedIngredient> inputs,
                               List<SizedFluidIngredient> fluidInputs, List<ItemStack> outputs,
                               List<FluidStack> fluidOutputs, int ticks, int energyPerTick,
                               List<SizedIngredient> catalysts, List<MatterStack> matterInputs,
                               List<MatterStack> matterOutputs, List<Byproduct> byproducts,
                               RecipeConditions conditions)
        implements Recipe<ProcessingInput> {

    /** How long a recipe takes when its file does not say. */
    public static final int DEFAULT_TICKS = 200;
    private static final StreamCodec<RegistryFriendlyByteBuf, MatterStack> MATTER_STREAM =
            ByteBufCodecs.fromCodecWithRegistries(MatterStack.CODEC);

    public ProcessingRecipe {
        inputs = List.copyOf(inputs);
        fluidInputs = List.copyOf(fluidInputs);
        outputs = List.copyOf(outputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        catalysts = List.copyOf(catalysts);
        matterInputs = List.copyOf(matterInputs);
        matterOutputs = List.copyOf(matterOutputs);
        byproducts = List.copyOf(byproducts);
    }

    /** A recipe of items and liquids only, with no catalyst, byproduct or condition. */
    public ProcessingRecipe(final ProcessingKind kind, final List<SizedIngredient> inputs,
                            final List<SizedFluidIngredient> fluidInputs, final List<ItemStack> outputs,
                            final List<FluidStack> fluidOutputs, final int ticks, final int energyPerTick) {
        this(kind, inputs, fluidInputs, outputs, fluidOutputs, ticks, energyPerTick, List.of(), List.of(), List.of(),
                List.of(), RecipeConditions.ANY);
    }

    /**
     * Whether the machine holds everything the recipe takes, each item input and each catalyst in a slot of its own,
     * each fluid and each matter in a tank of its own, with at least its amount; and whether the temperature and the
     * pressure inside the machine are within the recipe's.
     */
    @Override
    public boolean matches(final ProcessingInput input, final Level level) {
        return slotsFor(input) != null && tanksFor(input) != null && matterTanksFor(input) != null
                && conditions.holds(input.temperature(), input.pressure());
    }

    /**
     * The input slot each item input is taken from, in the order of {@link #inputs()}, or null when the slots do not
     * hold them (and the catalysts beside them): what a machine takes from when it finishes the recipe.
     */
    @Nullable
    public int[] slotsFor(final ProcessingInput input) {
        final List<SizedIngredient> wanted = new ArrayList<>(inputs);
        wanted.addAll(catalysts);
        final int[] all = assignAll(wanted, input.items(), SizedIngredient::test);
        if (all == null) {
            return null;
        }
        final int[] taken = new int[inputs.size()];
        System.arraycopy(all, 0, taken, 0, taken.length);
        return taken;
    }

    /** The input tank each fluid input is taken from, or null when the tanks do not hold them. */
    @Nullable
    public int[] tanksFor(final ProcessingInput input) {
        return assignAll(fluidInputs, input.fluids(), SizedFluidIngredient::test);
    }

    /** The input tank of matter each matter input is taken from, or null when the tanks do not hold them. */
    @Nullable
    public int[] matterTanksFor(final ProcessingInput input) {
        return assignAll(matterInputs, input.matter(), (wanted, held) -> held.isPresent()
                && held.get().material() == wanted.material() && held.get().amount() >= wanted.amount());
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

    /** The item inputs and the catalysts, each once whatever its count, for the recipe viewers and planners. */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        final NonNullList<Ingredient> list = NonNullList.create();
        for (final SizedIngredient input : inputs) {
            list.add(input.ingredient());
        }
        for (final SizedIngredient catalyst : catalysts) {
            list.add(catalyst.ingredient());
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
                                .forGetter(ProcessingRecipe::energyPerTick),
                        SizedIngredient.FLAT_CODEC.listOf().optionalFieldOf("catalysts", List.of())
                                .forGetter(ProcessingRecipe::catalysts),
                        MatterStack.CODEC.listOf().optionalFieldOf("matter_inputs", List.of())
                                .forGetter(ProcessingRecipe::matterInputs),
                        MatterStack.CODEC.listOf().optionalFieldOf("matter_outputs", List.of())
                                .forGetter(ProcessingRecipe::matterOutputs),
                        Byproduct.CODEC.listOf().optionalFieldOf("byproducts", List.of())
                                .forGetter(ProcessingRecipe::byproducts),
                        RecipeConditions.CODEC.optionalFieldOf("conditions", RecipeConditions.ANY)
                                .forGetter(ProcessingRecipe::conditions))
                .apply(instance, (in, fluidIn, out, fluidOut, ticks, energy, catalysts, matterIn, matterOut,
                                  byproducts, conditions) -> new ProcessingRecipe(kind, in, fluidIn, out, fluidOut,
                        ticks, energy, catalysts, matterIn, matterOut, byproducts, conditions)))
                .validate(recipe -> recipe.inputs().isEmpty() && recipe.fluidInputs().isEmpty()
                        && recipe.matterInputs().isEmpty()
                        ? DataResult.error(() -> "a processing recipe takes something")
                        : recipe.outputs().isEmpty() && recipe.fluidOutputs().isEmpty()
                        && recipe.matterOutputs().isEmpty() && recipe.byproducts().isEmpty()
                        ? DataResult.error(() -> "a processing recipe makes something")
                        : DataResult.success(recipe));
    }

    /** How a recipe of {@code kind} is sent to the players. */
    static StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec(final ProcessingKind kind) {
        final StreamCodec<RegistryFriendlyByteBuf, List<SizedIngredient>> ingredients =
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list());
        final StreamCodec<RegistryFriendlyByteBuf, List<SizedFluidIngredient>> fluidIngredients =
                SizedFluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list());
        final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> stacks =
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list());
        final StreamCodec<RegistryFriendlyByteBuf, List<FluidStack>> fluids =
                FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list());
        final StreamCodec<RegistryFriendlyByteBuf, List<MatterStack>> matter = MATTER_STREAM.apply(
                ByteBufCodecs.list());
        final StreamCodec<RegistryFriendlyByteBuf, List<Byproduct>> byproducts = Byproduct.STREAM_CODEC.apply(
                ByteBufCodecs.list());
        return StreamCodec.of((buf, recipe) -> {
            ingredients.encode(buf, recipe.inputs());
            fluidIngredients.encode(buf, recipe.fluidInputs());
            stacks.encode(buf, recipe.outputs());
            fluids.encode(buf, recipe.fluidOutputs());
            ByteBufCodecs.VAR_INT.encode(buf, recipe.ticks());
            ByteBufCodecs.VAR_INT.encode(buf, recipe.energyPerTick());
            ingredients.encode(buf, recipe.catalysts());
            matter.encode(buf, recipe.matterInputs());
            matter.encode(buf, recipe.matterOutputs());
            byproducts.encode(buf, recipe.byproducts());
            RecipeConditions.STREAM_CODEC.encode(buf, recipe.conditions());
        }, buf -> new ProcessingRecipe(kind, ingredients.decode(buf), fluidIngredients.decode(buf),
                stacks.decode(buf), fluids.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ingredients.decode(buf), matter.decode(buf), matter.decode(buf),
                byproducts.decode(buf), RecipeConditions.STREAM_CODEC.decode(buf)));
    }

    /*
     * Gives each wanted thing a slot or tank of its own that satisfies it, trying them in order and each place not yet
     * taken; a later one that finds no place sends the search back to try the earlier ones elsewhere. A recipe has a
     * handful of inputs, so the search stays small.
     */
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
