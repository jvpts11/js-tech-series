/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import dev.emi.emi.api.neoforge.NeoForgeEmiIngredient;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.client.recipeview.RecipeViewPalette;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.ProcessingViewLayout;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** One processing recipe in EMI, laid out as JEI lays it out. */
final class ProcessingEmiRecipe extends BasicEmiRecipe {

    private final ProcessingRecipe recipe;
    private final RecipeHolder<ProcessingRecipe> holder;
    private final ProcessingViewLayout layout;

    /* A recipe's arrow fills in its time: a tick is fifty milliseconds. */
    private static final int MILLIS_PER_TICK = 50;

    ProcessingEmiRecipe(final EmiRecipeCategory category, final RecipeHolder<ProcessingRecipe> holder) {
        this(category, holder, ProcessingRecipeViews.layoutOf(holder.value()));
    }

    private ProcessingEmiRecipe(final EmiRecipeCategory category, final RecipeHolder<ProcessingRecipe> holder,
                                final ProcessingViewLayout layout) {
        super(category, holder.id(), layout.width(), layout.height());
        this.recipe = holder.value();
        this.holder = holder;
        this.layout = layout;
        for (final SizedIngredient input : recipe.inputs()) {
            inputs.add(NeoForgeEmiIngredient.of(input));
        }
        for (final SizedFluidIngredient input : recipe.fluidInputs()) {
            inputs.add(NeoForgeEmiIngredient.of(input));
        }
        for (final ItemStack output : recipe.outputs()) {
            outputs.add(EmiStack.of(output));
        }
        for (final FluidStack output : recipe.fluidOutputs()) {
            outputs.add(NeoForgeEmiStack.of(output));
        }
    }

    @Override
    public void addWidgets(final WidgetHolder widgets) {
        int index = 0;
        for (final EmiIngredient input : inputs) {
            final ProcessingViewLayout.Point at = layout.input(index++);
            widgets.addSlot(input, at.x(), at.y());
        }
        index = 0;
        for (final EmiStack output : outputs) {
            final ProcessingViewLayout.Point at = layout.output(index++);
            widgets.addSlot(output, at.x(), at.y()).recipeContext(this);
        }
        widgets.addFillingArrow(layout.arrow().x(), layout.arrow().y(), recipe.ticks() * MILLIS_PER_TICK);
        widgets.addText(ProcessingRecipeViews.workLine(recipe), layout.textLine().x(), layout.textLine().y(),
                RecipeViewPalette.get().text(), false);
    }

    @Override
    public RecipeHolder<?> getBackingRecipe() {
        return holder;
    }
}
