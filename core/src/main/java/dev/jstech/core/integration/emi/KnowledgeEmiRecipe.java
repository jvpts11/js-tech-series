/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.progression.IKnowledgeGated;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * A crafting recipe that asks for knowledge among EMI's crafting recipes: its grid and what it makes as EMI shows any
 * crafting recipe, and, while the player's team does not know what it needs, a padlock in the corner under what it
 * makes, which names the knowledge when pointed at.
 */
final class KnowledgeEmiRecipe extends EmiCraftingRecipe {

    private static final int GRID = 3;
    private final IKnowledgeGated gated;

    KnowledgeEmiRecipe(final RecipeHolder<? extends CraftingRecipe> holder, final RegistryAccess registries) {
        super(inputsOf(holder.value()), EmiStack.of(holder.value().getResultItem(registries)), idOf(holder.id()),
                !(holder.value() instanceof ShapedRecipe));
        this.gated = (IKnowledgeGated) holder.value();
    }

    @Override
    public void addWidgets(final WidgetHolder widgets) {
        super.addWidgets(widgets);
        if (!ProcessingRecipeViews.locked(this.gated)) {
            return;
        }
        final int x = getDisplayWidth() - ProcessingRecipeViews.LOCK_WIDTH;
        final int y = getDisplayHeight() - ProcessingRecipeViews.LOCK_HEIGHT;
        widgets.addDrawable(x, y, ProcessingRecipeViews.LOCK_WIDTH, ProcessingRecipeViews.LOCK_HEIGHT,
                (graphics, mouseX, mouseY, delta) -> graphics.blitSprite(ProcessingRecipeViews.LOCK, 0, 0,
                        ProcessingRecipeViews.LOCK_WIDTH, ProcessingRecipeViews.LOCK_HEIGHT));
        ProcessingRecipeViews.knowledgeLine(this.gated).ifPresent(needs -> widgets.addTooltipText(List.of(needs), x,
                y, ProcessingRecipeViews.LOCK_WIDTH, ProcessingRecipeViews.LOCK_HEIGHT));
    }

    /* The recipe's id in EMI, beside the one EMI may give the game's own form of it, which the plugin takes out. */
    static ResourceLocation idOf(final ResourceLocation recipe) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "/knowledge/" + recipe.getNamespace() + "/"
                + recipe.getPath());
    }

    /* A shaped recipe's ingredients laid on the full grid, each in its place; a shapeless one's as they come. */
    private static List<EmiIngredient> inputsOf(final CraftingRecipe recipe) {
        final List<Ingredient> ingredients = recipe.getIngredients();
        final List<EmiIngredient> out = new ArrayList<>();
        if (!(recipe instanceof ShapedRecipe shaped)) {
            ingredients.forEach(ingredient -> out.add(EmiIngredient.of(ingredient)));
            return out;
        }
        for (int place = 0; place < GRID * GRID; place++) {
            final int column = place % GRID;
            final int row = place / GRID;
            out.add(column < shaped.getWidth() && row < shaped.getHeight()
                    ? EmiIngredient.of(ingredients.get(row * shaped.getWidth() + column)) : EmiStack.EMPTY);
        }
        return out;
    }
}
