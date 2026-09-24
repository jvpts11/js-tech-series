/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.MachineCategory;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import java.util.ArrayList;
import java.util.List;

/**
 * What makes a thing and what that thing goes into, as a person reads it.
 *
 * <p>The two questions a player asks of an item they are looking at, answered from the recipes the network
 * knows. The graphical Network Interactor asks them of its details panel and a terminal asks them of
 * {@code info}, and both get the same words, because they are the same question about the same network.
 */
@TextHolder
public final class ItemRecipes {

    private static final TextKey PROCESSING = TextKey.of("jsc.service.recipes.processing", "%s · processing · %s");
    private static final TextKey MULTI_STAGE = TextKey.of("jsc.service.recipes.multi_stage", "%s · multi-stage · %s");
    private static final TextKey AT_THE_BENCH = TextKey.of("jsc.service.recipes.at_the_bench", "%s · bench");
    private static final TextKey BENCH_STAGE = TextKey.of("jsc.service.recipes.bench_stage", "Bench");

    private ItemRecipes() {
    }

    /** The ways the network can make that thing, one line each, newest knowledge first. */
    public static List<Text> madeBy(final MainframeBlockEntity mainframe, final StorageKey key, final int most) {
        final List<Text> lines = new ArrayList<>();
        if (mainframe == null) {
            return lines;
        }
        for (final NetworkRecipe recipe : mainframe.recipesFor(key)) {
            if (lines.size() >= most) {
                break;
            }
            lines.add(line(recipe));
        }
        return lines;
    }

    /** The things the network's recipes take that thing for, by name, each named once. */
    public static List<Text> usedIn(final MainframeBlockEntity mainframe, final StorageKey key, final int most) {
        final List<Text> names = new ArrayList<>();
        if (mainframe == null) {
            return names;
        }
        for (final CraftingPattern pattern : mainframe.networkPatterns()) {
            if (pattern.ingredientTotals().containsKey(key)) {
                add(names, GameText.of(pattern.result().getHoverName()), most);
            }
        }
        for (final NetworkRecipe recipe : mainframe.networkMachineRecipes()) {
            if (consumes(recipe, key)) {
                final StorageKey made = recipe.resultKey();
                add(names, made == null ? recipe.displayText() : GameText.of(made.displayName()), most);
            }
        }
        return names;
    }

    /**
     * "Blast &#183; processing &#183; Blast Furnace": one way of making a thing, in one line, each name in the
     * language of whoever reads it.
     */
    public static Text line(final NetworkRecipe recipe) {
        if (recipe.proc().isPresent()) {
            return PROCESSING.with(recipe.displayText(), MachineCategory.text(recipe.proc().get().machineType()));
        }
        if (recipe.multi().isPresent()) {
            final List<Text> machines = new ArrayList<>();
            for (final var stage : recipe.multi().get().stages()) {
                machines.add(stage.proc().isPresent()
                        ? MachineCategory.text(stage.proc().get().machineType())
                        : BENCH_STAGE.text());
            }
            return MULTI_STAGE.with(recipe.displayText(), TextLists.join(" -> ", machines));
        }
        return AT_THE_BENCH.with(recipe.displayText());
    }

    /** Whether a recipe takes that thing in, at any of its stages. */
    public static boolean consumes(final NetworkRecipe recipe, final StorageKey key) {
        if (recipe.proc().isPresent()) {
            return recipe.proc().get().ingredientTotals().containsKey(key);
        }
        if (recipe.multi().isPresent()) {
            for (final var stage : recipe.multi().get().stages()) {
                if (stage.proc().isPresent() && stage.proc().get().ingredientTotals().containsKey(key)) {
                    return true;
                }
                if (stage.bench().isPresent() && stage.bench().get().ingredientTotals().containsKey(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void add(final List<Text> names, final Text name, final int most) {
        if (names.size() < most && !names.contains(name)) {
            names.add(name);
        }
    }
}
