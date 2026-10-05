/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.jstech.core.client.recipeview.IKeepsViewersClear;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The Core's bridge to EMI, which finds it by its annotation and only when EMI is installed: the same as the bridge to
 * JEI, a category for every mod's processing machines with the machines as its workstations, and the areas screens
 * name kept clear of EMI's lists.
 */
@EmiEntrypoint
public final class CoreEmiPlugin implements EmiPlugin {

    @Override
    public void register(final EmiRegistry registry) {
        for (final ProcessingKind kind : ProcessingRecipeViews.kinds()) {
            final List<ItemStack> machines = ProcessingRecipeViews.machineStacks(kind);
            final EmiRenderable icon = machines.isEmpty() ? EmiStack.EMPTY : EmiStack.of(machines.getFirst());
            final EmiRecipeCategory category = new KindCategory(kind, icon);
            registry.addCategory(category);
            for (final ItemStack machine : machines) {
                registry.addWorkstation(category, EmiStack.of(machine));
            }
            for (final RecipeHolder<ProcessingRecipe> holder
                    : registry.getRecipeManager().getAllRecipesFor(kind.type())) {
                registry.addRecipe(new ProcessingEmiRecipe(category, holder));
            }
        }
        registry.addGenericExclusionArea((screen, areas) -> {
            if (screen instanceof IKeepsViewersClear keeps) {
                for (final Rect2i area : keeps.areasKeptClear()) {
                    areas.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                }
            }
        });
    }

    /** How many recipes EMI shows under that kind of machine; 0 before EMI has read them. */
    public static int recipesShown(final ProcessingKind kind) {
        final EmiRecipeCategory category = categoryOf(kind);
        return category == null ? 0 : EmiApi.getRecipeManager().getRecipes(category).size();
    }

    /** Opens EMI's recipe screen on the recipes of that kind of machine; false before EMI has read them. */
    public static boolean showRecipes(final ProcessingKind kind) {
        final EmiRecipeCategory category = categoryOf(kind);
        if (category == null) {
            return false;
        }
        EmiApi.displayRecipeCategory(category);
        return true;
    }

    @Nullable
    private static EmiRecipeCategory categoryOf(final ProcessingKind kind) {
        for (final EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            if (category.getId().equals(kind.id())) {
                return category;
            }
        }
        return null;
    }

    /* A kind's category, named as the kind is rather than by EMI's own key for it. */
    private static final class KindCategory extends EmiRecipeCategory {

        private final ProcessingKind kind;

        private KindCategory(final ProcessingKind kind, final EmiRenderable icon) {
            super(kind.id(), icon);
            this.kind = kind;
        }

        @Override
        public Component getName() {
            return GameText.component(kind.title());
        }
    }
}
