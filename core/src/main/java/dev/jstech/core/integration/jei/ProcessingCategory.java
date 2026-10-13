/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.client.recipeview.RecipeViewPalette;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.ProcessingViewLayout;
import dev.jstech.core.text.GameText;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** The recipes of one kind of processing machine, in JEI: under the kind's name, with its machines beside them. */
final class ProcessingCategory implements IRecipeCategory<RecipeHolder<ProcessingRecipe>> {

    private final ProcessingKind kind;
    private final RecipeType<RecipeHolder<ProcessingRecipe>> type;
    private final IDrawable icon;
    private final ProcessingViewLayout size;

    /* A slot's picture is drawn a pixel outside the 16 pixels of what it holds. */
    private static final int SLOT_INSET = 1;
    private static final int FLUID_SIZE = 16;

    ProcessingCategory(final ProcessingKind kind, final IGuiHelper gui, final ProcessingViewLayout size) {
        this.kind = kind;
        this.type = typeOf(kind);
        final List<ItemStack> machines = ProcessingRecipeViews.machineStacks(kind);
        this.icon = machines.isEmpty() ? gui.createBlankDrawable(FLUID_SIZE, FLUID_SIZE)
                : gui.createDrawableItemStack(machines.getFirst());
        this.size = size;
    }

    /** The JEI recipe type of {@code kind}: its own id, so a bookmark names the kind it came from. */
    static RecipeType<RecipeHolder<ProcessingRecipe>> typeOf(final ProcessingKind kind) {
        return RecipeType.createRecipeHolderType(kind.id());
    }

    @Override
    public RecipeType<RecipeHolder<ProcessingRecipe>> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return GameText.component(kind.title());
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return size.width();
    }

    @Override
    public int getHeight() {
        return size.height();
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder, final RecipeHolder<ProcessingRecipe> holder,
                          final IFocusGroup focuses) {
        final ProcessingRecipe recipe = holder.value();
        final ProcessingViewLayout layout = ProcessingRecipeViews.layoutOf(recipe);
        int index = 0;
        for (final SizedIngredient input : recipe.inputs()) {
            final ProcessingViewLayout.Point at = layout.input(index++);
            builder.addInputSlot(at.x() + SLOT_INSET, at.y() + SLOT_INSET).setStandardSlotBackground()
                    .addItemStacks(List.of(input.getItems()));
        }
        for (final SizedFluidIngredient input : recipe.fluidInputs()) {
            final ProcessingViewLayout.Point at = layout.input(index++);
            builder.addInputSlot(at.x() + SLOT_INSET, at.y() + SLOT_INSET).setStandardSlotBackground()
                    .setFluidRenderer(input.amount(), false, FLUID_SIZE, FLUID_SIZE)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, List.of(input.getFluids()));
        }
        /*
         * The outputs wear the same plain slot as the inputs: JEI's large output slot reaches five pixels past its
         * item, onto the next output and the line under the recipe.
         */
        index = 0;
        for (final ItemStack output : recipe.outputs()) {
            final ProcessingViewLayout.Point at = layout.output(index++);
            builder.addOutputSlot(at.x() + SLOT_INSET, at.y() + SLOT_INSET).setStandardSlotBackground()
                    .addItemStack(output);
        }
        for (final FluidStack output : recipe.fluidOutputs()) {
            final ProcessingViewLayout.Point at = layout.output(index++);
            builder.addOutputSlot(at.x() + SLOT_INSET, at.y() + SLOT_INSET).setStandardSlotBackground()
                    .setFluidRenderer(output.getAmount(), false, FLUID_SIZE, FLUID_SIZE)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, output);
        }
    }

    @Override
    public void createRecipeExtras(final IRecipeExtrasBuilder builder, final RecipeHolder<ProcessingRecipe> holder,
                                   final IFocusGroup focuses) {
        final ProcessingRecipe recipe = holder.value();
        final ProcessingViewLayout layout = ProcessingRecipeViews.layoutOf(recipe);
        builder.addAnimatedRecipeArrow(recipe.ticks()).setPosition(layout.arrow().x(), layout.arrow().y());
        builder.addText(ProcessingRecipeViews.workLine(recipe), size.width(), ProcessingViewLayout.TEXT_LINE)
                .setPosition(layout.textLine().x(), layout.textLine().y())
                .setColor(RecipeViewPalette.get().text());
    }

    @Override
    public ResourceLocation getRegistryName(final RecipeHolder<ProcessingRecipe> holder) {
        return holder.id();
    }
}
