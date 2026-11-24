/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import com.mojang.serialization.JsonOps;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.ChipClasses;
import dev.jstech.computers.chain.ChipItem;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.matter.MatterStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.Nullable;

/**
 * Gives each model of a family its own class of the chip that makes it (computers/docs/design/survival.md, "How a
 * chip is made"), so no processing recipe's lot holds another's whole: fed an NVMe 8T's nine memory chips, a line
 * must not make a 4T from eight of them. It holds every processing recipe the mod writes until all are written, then
 * finds each pair of the same kind where one's inputs all fit in the other's (more of each, or the same) and both are
 * made by the same chip: the die for a processor or a graphics card, the memory for a disk or a module, the controller
 * for the rest. The models of each chip are given classes so no two that fit take the same one, the model taking the
 * most of the chip first, class A; a model that fits no other keeps the plain chip. A chip needing more classes than
 * the Packaging Line sorts into stops the run.
 */
final class ChipClassing implements RecipeOutput {

    private final RecipeOutput out;
    private final List<Held> held = new ArrayList<>();

    ChipClassing(final RecipeOutput out) {
        this.out = out;
    }

    @Override
    public void accept(final ResourceLocation id, final Recipe<?> recipe, @Nullable final AdvancementHolder advancement,
                       final ICondition... conditions) {
        if (recipe instanceof ProcessingRecipe processing) {
            held.add(new Held(id, processing, advancement, conditions, lot(processing), made(processing),
                    mainChip(processing)));
        } else {
            out.accept(id, recipe, advancement, conditions);
        }
    }

    @Override
    public Advancement.Builder advancement() {
        return out.advancement();
    }

    /** Writes every recipe held, each model's chip of its class. */
    void finish() {
        final Map<Held, Integer> classes = classes();
        for (final Held recipe : held) {
            final Integer grade = classes.get(recipe);
            out.accept(recipe.id(), grade == null ? recipe.recipe() : classed(recipe, grade), recipe.advancement(),
                    recipe.conditions());
        }
    }

    /* The class each recipe in a pair that fits takes of its chip, 1 for A; none for the rest. */
    private Map<Held, Integer> classes() {
        final Map<Item, Map<Held, Set<Held>>> fits = new LinkedHashMap<>();
        final Map<ResourceLocation, List<Held>> byKind = held.stream().collect(Collectors.groupingBy(
                recipe -> recipe.recipe().kind().id(), LinkedHashMap::new, Collectors.toList()));
        for (final List<Held> kind : byKind.values()) {
            for (final Held big : kind) {
                for (final Held small : kind) {
                    if (big != small && big.chip() != null && big.chip() == small.chip()
                            && !big.made().equals(small.made()) && fitsIn(small.lot(), big.lot())) {
                        final Map<Held, Set<Held>> graph = fits.computeIfAbsent(big.chip(), chip -> new HashMap<>());
                        graph.computeIfAbsent(big, recipe -> new HashSet<>()).add(small);
                        graph.computeIfAbsent(small, recipe -> new HashSet<>()).add(big);
                    }
                }
            }
        }
        final Map<Held, Integer> classes = new HashMap<>();
        for (final Map.Entry<Item, Map<Held, Set<Held>>> chip : fits.entrySet()) {
            final Map<Held, Set<Held>> graph = chip.getValue();
            final String key = key(chip.getKey());
            final List<Held> order = new ArrayList<>(graph.keySet());
            order.sort(Comparator.<Held>comparingLong(recipe -> -recipe.lot().get(key))
                    .thenComparingInt(recipe -> -graph.get(recipe).size())
                    .thenComparing(recipe -> recipe.id().toString()));
            for (final Held recipe : order) {
                final Set<Integer> taken = new HashSet<>();
                for (final Held other : graph.get(recipe)) {
                    taken.add(classes.get(other));
                }
                int grade = 1;
                while (taken.contains(grade)) {
                    grade++;
                }
                if (grade > ChipClasses.MOST) {
                    throw new IllegalStateException(chip.getKey() + " would need more than " + ChipClasses.MOST
                            + " classes for " + recipe.id() + "; give some of its models another ingredient");
                }
                classes.put(recipe, grade);
            }
        }
        return classes;
    }

    /* The recipe with its chip of class {@code grade}, as many as before. */
    private static ProcessingRecipe classed(final Held held, final int grade) {
        final ProcessingRecipe recipe = held.recipe();
        final List<SizedIngredient> inputs = new ArrayList<>();
        for (final SizedIngredient input : recipe.inputs()) {
            final Item only = only(input.ingredient());
            inputs.add(only != null && only == held.chip() ? new SizedIngredient(DataComponentIngredient.of(false,
                    ComputingComponents.CHIP_CLASS.get(), grade, only), input.count()) : input);
        }
        return new ProcessingRecipe(recipe.kind(), inputs, recipe.fluidInputs(), recipe.outputs(),
                recipe.fluidOutputs(), recipe.ticks(), recipe.energyPerTick(), recipe.catalysts(),
                recipe.matterInputs(), recipe.matterOutputs(), recipe.byproducts(), recipe.conditions(),
                recipe.knowledge());
    }

    /* Whether every input of {@code small} is in {@code big}, as many or more. */
    private static boolean fitsIn(final Map<String, Long> small, final Map<String, Long> big) {
        for (final Map.Entry<String, Long> input : small.entrySet()) {
            if (big.getOrDefault(input.getKey(), 0L) < input.getValue()) {
                return false;
            }
        }
        return true;
    }

    /* What a lot of the recipe holds: each item input, liquid and matter by what it is, and how much of it. */
    private static Map<String, Long> lot(final ProcessingRecipe recipe) {
        final Map<String, Long> lot = new HashMap<>();
        for (final SizedIngredient input : recipe.inputs()) {
            final Item only = only(input.ingredient());
            lot.merge(only != null ? key(only) : Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, input.ingredient())
                    .getOrThrow().toString(), (long) input.count(), Long::sum);
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

    /* What the recipe makes, by the ids of its items, or of its liquids for one that makes none. */
    private static String made(final ProcessingRecipe recipe) {
        if (!recipe.outputs().isEmpty()) {
            return recipe.outputs().stream().map(stack -> key(stack.getItem())).collect(Collectors.joining(","));
        }
        return recipe.fluidOutputs().stream().map(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid())
                .toString()).collect(Collectors.joining(","));
    }

    /*
     * The chip that makes the recipe's model: its die, else its memory chip, else its controller chip, the one it
     * takes most of; none when it takes no chip.
     */
    private static @Nullable Item mainChip(final ProcessingRecipe recipe) {
        Item best = null;
        int bestRank = 0;
        int bestCount = 0;
        for (final SizedIngredient input : recipe.inputs()) {
            final Item only = only(input.ingredient());
            final int rank = only instanceof ChipItem ? rank(only) : 0;
            if (rank > bestRank || rank == bestRank && rank > 0 && input.count() > bestCount) {
                best = only;
                bestRank = rank;
                bestCount = input.count();
            }
        }
        return best;
    }

    /* A die first, then a memory chip, then a controller chip. */
    private static int rank(final Item chip) {
        if (ChainChips.dieOf(chip).isPresent()) {
            return 3;
        }
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            if (ChainParts.MEMORY_CHIPS.get(i).get() == chip) {
                return 2;
            }
            if (ChainParts.CONTROLLER_CHIPS.get(i).get() == chip) {
                return 1;
            }
        }
        return 0;
    }

    /* The one item an ingredient of a single item stands for, or none for a tag, a custom one or several. */
    private static @Nullable Item only(final Ingredient ingredient) {
        if (ingredient.isCustom() || ingredient.getValues().length != 1
                || !(ingredient.getValues()[0] instanceof Ingredient.ItemValue value)) {
            return null;
        }
        return value.item().getItem();
    }

    private static String key(final Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /* A processing recipe held until all are written, with its lot, what it makes and its main chip. */
    private record Held(ResourceLocation id, ProcessingRecipe recipe, @Nullable AdvancementHolder advancement,
                        ICondition[] conditions, Map<String, Long> lot, String made, @Nullable Item chip) {
    }
}
