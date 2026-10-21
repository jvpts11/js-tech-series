/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.progression.IKnowledgeGated;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

/**
 * A crafting recipe that asks for knowledge in JEI's crafting category: its grid and what it makes as any crafting
 * recipe shows them, and, while the player's team does not know what it needs, a padlock in the corner under what it
 * makes, which names the knowledge when pointed at.
 *
 * @param <R> the recipe
 */
final class KnowledgeCraftingExtension<R extends CraftingRecipe & IKnowledgeGated>
        implements ICraftingCategoryExtension<R> {

    /* The size of JEI's crafting category, which the padlock sits in the bottom corner of. */
    private static final int CRAFTING_WIDTH = 116;
    private static final int CRAFTING_HEIGHT = 54;

    @Override
    public int getWidth(final RecipeHolder<R> recipeHolder) {
        return recipeHolder.value() instanceof ShapedRecipe shaped ? shaped.getWidth() : 0;
    }

    @Override
    public int getHeight(final RecipeHolder<R> recipeHolder) {
        return recipeHolder.value() instanceof ShapedRecipe shaped ? shaped.getHeight() : 0;
    }

    @Override
    public void setRecipe(final RecipeHolder<R> recipeHolder, final IRecipeLayoutBuilder builder,
                          final ICraftingGridHelper craftingGridHelper, final IFocusGroup focuses) {
        final List<@Nullable List<@Nullable ItemStack>> inputs = new ArrayList<>();
        for (final Ingredient ingredient : recipeHolder.value().getIngredients()) {
            inputs.add(ingredient.isEmpty() ? null : Arrays.asList(ingredient.getItems()));
        }
        craftingGridHelper.createAndSetInputs(builder, inputs, getWidth(recipeHolder), getHeight(recipeHolder));
        final RegistryAccess registries = Minecraft.getInstance().level == null ? RegistryAccess.EMPTY
                : Minecraft.getInstance().level.registryAccess();
        craftingGridHelper.createAndSetOutputs(builder, List.of(recipeHolder.value().getResultItem(registries)));
    }

    @Override
    public void drawInfo(final RecipeHolder<R> recipe, final int recipeWidth, final int recipeHeight,
                         final GuiGraphics guiGraphics, final double mouseX, final double mouseY) {
        if (ProcessingRecipeViews.locked(recipe.value())) {
            guiGraphics.blitSprite(ProcessingRecipeViews.LOCK, CRAFTING_WIDTH - ProcessingRecipeViews.LOCK_WIDTH,
                    CRAFTING_HEIGHT - ProcessingRecipeViews.LOCK_HEIGHT, ProcessingRecipeViews.LOCK_WIDTH,
                    ProcessingRecipeViews.LOCK_HEIGHT);
        }
    }

    @Override
    public void getTooltip(final ITooltipBuilder tooltip, final RecipeHolder<R> recipe, final double mouseX,
                           final double mouseY) {
        if (ProcessingRecipeViews.locked(recipe.value())
                && mouseX >= CRAFTING_WIDTH - ProcessingRecipeViews.LOCK_WIDTH && mouseX < CRAFTING_WIDTH
                && mouseY >= CRAFTING_HEIGHT - ProcessingRecipeViews.LOCK_HEIGHT && mouseY < CRAFTING_HEIGHT) {
            ProcessingRecipeViews.knowledgeLine(recipe.value()).ifPresent(tooltip::add);
        }
    }
}
