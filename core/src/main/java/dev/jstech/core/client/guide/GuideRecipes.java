/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.guide.IRecipeWords;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.MachineTexts;
import dev.jstech.core.machine.ProcessingViewLayout;
import dev.jstech.core.progression.IKnowledgeGated;
import dev.jstech.core.progression.KnowledgeGate;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * The recipes a manual's page shows, read from the world the player is in: a recipe type's recipes, or those making
 * one item, each as what goes in, what comes out, and the time and energy over its arrow. Read once for each manual
 * opened, since a world's recipes do not change while it is played.
 */
public final class GuideRecipes {

    private final Map<String, List<View>> read = new HashMap<>();

    /** The most inputs a row shows; a recipe of more shows its first ones. */
    public static final int MOST_INPUTS = 5;

    /* The blocks of the game's own that work its recipe types, named as what works their recipes. */
    private static final Map<RecipeType<?>, Item> STATIONS = Map.of(RecipeType.CRAFTING, Items.CRAFTING_TABLE,
            RecipeType.SMELTING, Items.FURNACE, RecipeType.BLASTING, Items.BLAST_FURNACE, RecipeType.SMOKING,
            Items.SMOKER, RecipeType.CAMPFIRE_COOKING, Items.CAMPFIRE, RecipeType.STONECUTTING, Items.STONECUTTER,
            RecipeType.SMITHING, Items.SMITHING_TABLE);

    /** The recipes of that type, making that item when one is named, in the order of their ids. */
    public List<View> of(final String type, final String output) {
        return this.read.computeIfAbsent(type + "|" + output, key -> load(type, output));
    }

    /** The recipes as a search reads them: the names of what each makes and takes, and of what works it. */
    public IRecipeWords words() {
        return (type, output) -> this.of(type, output).stream().map(View::words).toList();
    }

    private static List<View> load(final String type, final String output) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return List.of();
        }
        final RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.parse(type));
        if (recipeType == null) {
            return List.of();
        }
        final HolderLookup.Provider registries = minecraft.level.registryAccess();
        final List<RecipeHolder<?>> holders = new ArrayList<>();
        for (final RecipeHolder<?> holder : minecraft.level.getRecipeManager().getRecipes()) {
            if (holder.value().getType() == recipeType) {
                holders.add(holder);
            }
        }
        holders.sort(Comparator.comparing(holder -> holder.id().toString()));
        final Component kind = kindOf(recipeType);
        final List<View> views = new ArrayList<>();
        for (final RecipeHolder<?> holder : holders) {
            final View view = view(holder.value(), registries, kind);
            if (output.isEmpty() || view.makes(output)) {
                views.add(view);
            }
        }
        return views;
    }

    /*
     * What works recipes of a type, by name: a processing machine's kind, or the block of the game's own that works
     * them; nothing for a type neither names.
     */
    private static Component kindOf(final RecipeType<?> type) {
        for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
            if (kind.type() == type) {
                return GameText.component(kind.title());
            }
        }
        final Item station = STATIONS.get(type);
        return station == null ? Component.empty() : new ItemStack(station).getHoverName();
    }

    private static View view(final Recipe<?> recipe, final HolderLookup.Provider registries, final Component kind) {
        if (recipe instanceof ProcessingRecipe processing) {
            final List<List<ItemStack>> inputs = new ArrayList<>();
            for (final SizedIngredient input : processing.inputs()) {
                if (inputs.size() >= MOST_INPUTS) {
                    break;
                }
                inputs.add(Arrays.asList(input.getItems()));
            }
            final Component energy = processing.energyPerTick() <= 0 ? Component.empty()
                    : GameText.component(processing.kind().energy().rate(processing.energyPerTick()));
            return new View(inputs, List.copyOf(processing.outputs()), GameText.component(
                    MachineTexts.RECIPE_TIME.with(ProcessingViewLayout.seconds(processing.ticks()))), energy,
                    ProcessingRecipeViews.locked(processing), kind);
        }
        final List<List<ItemStack>> inputs = new ArrayList<>();
        for (final Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty() && inputs.size() < MOST_INPUTS) {
                inputs.add(Arrays.asList(ingredient.getItems()));
            }
        }
        final Component time = recipe instanceof AbstractCookingRecipe cooking
                ? GameText.component(MachineTexts.RECIPE_TIME.with(
                        ProcessingViewLayout.seconds(cooking.getCookingTime())))
                : Component.empty();
        final boolean locked = recipe instanceof IKnowledgeGated gated && !KnowledgeGate.openHere(gated.knowledge());
        return new View(inputs, List.of(recipe.getResultItem(registries)), time, Component.empty(), locked, kind);
    }

    /**
     * One recipe as a row shows it.
     *
     * @param inputs  each input's choices, the first one drawn and the others in turn
     * @param outputs what it makes
     * @param time    how long it takes, written over its arrow, or nothing
     * @param energy  the energy it spends a tick, written under its arrow, or nothing
     * @param locked  whether it needs knowledge the reader's team does not have: a padlock on its arrow
     * @param kind    what works it, by name, or nothing
     */
    public record View(List<List<ItemStack>> inputs, List<ItemStack> outputs, Component time, Component energy,
                       boolean locked, Component kind) {

        public View {
            inputs = inputs.stream().map(List::copyOf).toList();
            outputs = List.copyOf(outputs);
        }

        /** The recipe as a search reads it. */
        public IRecipeWords.RecipeWords words() {
            final List<String> takes = new ArrayList<>();
            for (final List<ItemStack> choices : this.inputs) {
                for (final ItemStack choice : choices) {
                    takes.add(choice.getHoverName().getString());
                }
            }
            return new IRecipeWords.RecipeWords(this.outputs.stream().map(stack -> stack.getHoverName().getString())
                    .toList(), takes, this.kind.getString(), this.outputs.isEmpty() ? ""
                    : BuiltInRegistries.ITEM.getKey(this.outputs.getFirst().getItem()).toString());
        }

        /** Whether one of its outputs is that item. */
        public boolean makes(final String item) {
            for (final ItemStack stack : this.outputs) {
                if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item)) {
                    return true;
                }
            }
            return false;
        }
    }
}
