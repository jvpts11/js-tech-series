/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.multipart.MicroblockShowcase;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * One microblock rule in JEI's crafting category, one entry for every block and piece: its grid's places and what it
 * makes go through the rule's frames together ({@link MicroblockShowcase}), and looking up a block or a microblock
 * shows only the frames it is in.
 *
 * @param <R> the rule's recipe
 */
final class MicroblockCraftingExtension<R extends CraftingRecipe> implements ICraftingCategoryExtension<R> {

    private final MicroblockShowcase.Rule rule;

    private static final int SIDE = 3;

    MicroblockCraftingExtension(final MicroblockShowcase.Rule rule) {
        this.rule = rule;
    }

    @Override
    public int getWidth(final RecipeHolder<R> recipeHolder) {
        return SIDE;
    }

    @Override
    public int getHeight(final RecipeHolder<R> recipeHolder) {
        return SIDE;
    }

    @Override
    public void setRecipe(final RecipeHolder<R> recipeHolder, final IRecipeLayoutBuilder builder,
                          final ICraftingGridHelper craftingGridHelper, final IFocusGroup focuses) {
        final List<MicroblockShowcase.Frame> frames = MicroblockShowcase.frames(this.rule);
        final List<@Nullable List<@Nullable ItemStack>> inputs = new ArrayList<>(SIDE * SIDE);
        for (int place = 0; place < SIDE * SIDE; place++) {
            final List<@Nullable ItemStack> through = new ArrayList<>(frames.size());
            for (final MicroblockShowcase.Frame frame : frames) {
                final ItemStack held = frame.grid().get(place);
                through.add(held.isEmpty() ? null : held);
            }
            inputs.add(through);
        }
        final List<@Nullable ItemStack> outputs = new ArrayList<>(frames.size());
        for (final MicroblockShowcase.Frame frame : frames) {
            outputs.add(frame.output());
        }
        final List<IRecipeSlotBuilder> slots = new ArrayList<>(craftingGridHelper.createAndSetInputs(builder, inputs,
                SIDE, SIDE));
        slots.add(craftingGridHelper.createAndSetOutputs(builder, outputs));
        // Every place shows the same frame at once, and a looked-up block keeps only the frames it is in.
        builder.createFocusLink(slots.toArray(new IRecipeSlotBuilder[0]));
    }
}
