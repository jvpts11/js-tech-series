/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.rei;

import dev.jstech.core.client.recipeview.ViewKinds;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One recipe of a kind from the Core's kit, in REI: what its page's slots take and make, as REI looks recipes up, and
 * the page itself, which its category draws.
 *
 * @param <T> what one recipe is
 */
final class DeclaredReiDisplay<T> implements Display {

    private final RecipeViewKind<T> kind;
    private final T recipe;
    private final RecipeView page;
    private final List<EntryIngredient> inputs = new ArrayList<>();
    private final List<EntryIngredient> outputs = new ArrayList<>();

    DeclaredReiDisplay(final RecipeViewKind<T> kind, final T recipe) {
        this.kind = kind;
        this.recipe = recipe;
        this.page = kind.view(recipe);
        for (final RecipeView.Slot slot : this.page.slots()) {
            (slot.role() == RecipeView.Role.OUTPUT ? this.outputs : this.inputs).add(ingredientOf(slot));
        }
    }

    /** The kind it is of. */
    RecipeViewKind<T> kind() {
        return this.kind;
    }

    /** The recipe it shows. */
    T recipe() {
        return this.recipe;
    }

    /** Its page, as the kind laid it. */
    RecipeView page() {
        return this.page;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return this.inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return this.outputs;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIdentifier.of(this.kind.id());
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.ofNullable(this.kind.idOf(this.recipe));
    }

    /** What a slot holds as REI holds it: any of its items or fluids, each with the lines its tooltip adds. */
    static EntryIngredient ingredientOf(final RecipeView.Slot slot) {
        final List<Component> lines = ViewKinds.tooltipOf(slot);
        final List<EntryStack<?>> stacks = new ArrayList<>();
        for (final ItemStack item : slot.items()) {
            stacks.add(lines.isEmpty() ? EntryStacks.of(item) : EntryStacks.of(item).tooltip(lines));
        }
        for (final FluidStack fluid : slot.fluids()) {
            final EntryStack<?> stack = EntryStacks.of(fluid.getFluid(), fluid.getAmount());
            stacks.add(lines.isEmpty() ? stack : stack.tooltip(lines));
        }
        return EntryIngredient.of(stacks);
    }
}
