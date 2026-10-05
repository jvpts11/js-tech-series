/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import dev.jstech.core.content.ModContent;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.machine.MachineTexts;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.ProcessingViewLayout;
import dev.jstech.core.text.GameText;
import java.util.Collection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * What both recipe viewers show of the processing machines every mod declared: the kinds, the layout each recipe is
 * drawn in, and the line of time and energy under it. The bridges to JEI and to EMI read only this, so the two show
 * the same recipes the same way.
 */
public final class ProcessingRecipeViews {

    private ProcessingRecipeViews() {
    }

    /** Every kind of processing machine any mod declared, in the order the mods declared them. */
    public static List<ProcessingKind> kinds() {
        return ModContent.all().stream().flatMap(content -> content.declaredProcessing().stream()).toList();
    }

    /**
     * The machines of {@code kind} that have an item to show, as one of each. A machine with no item of its own, a
     * block only ever placed by other means, is left out: a viewer shows no empty stack, and refuses the whole kind
     * when handed one.
     */
    public static List<ItemStack> machineStacks(final ProcessingKind kind) {
        return kind.machines().stream().map(ItemStack::new).filter(stack -> !stack.isEmpty()).toList();
    }

    /** Where the parts of {@code recipe} go. */
    public static ProcessingViewLayout layoutOf(final ProcessingRecipe recipe) {
        return new ProcessingViewLayout(recipe.inputs().size() + recipe.fluidInputs().size(),
                recipe.outputs().size() + recipe.fluidOutputs().size());
    }

    /**
     * A layout as large as the largest of {@code recipes}, which every one of them fits in: a layout only grows with
     * its inputs and outputs. A kind with no recipes yet gets the room of one in and one out.
     */
    public static ProcessingViewLayout largest(final Collection<ProcessingRecipe> recipes) {
        int inputs = 1;
        int outputs = 1;
        for (final ProcessingRecipe recipe : recipes) {
            final ProcessingViewLayout layout = layoutOf(recipe);
            inputs = Math.max(inputs, layout.inputs());
            outputs = Math.max(outputs, layout.outputs());
        }
        return new ProcessingViewLayout(inputs, outputs);
    }

    /** The line under a recipe: how long it takes, and what it spends a tick when it names that. */
    public static Component workLine(final ProcessingRecipe recipe) {
        final String seconds = ProcessingViewLayout.seconds(recipe.ticks());
        if (recipe.energyPerTick() <= 0) {
            return GameText.component(MachineTexts.RECIPE_TIME.with(seconds));
        }
        return GameText.component(MachineTexts.RECIPE_TIME_AND_ENERGY.with(seconds,
                UnitFormatter.forCurrentLocale().compact(recipe.energyPerTick(), Unit.FE_PER_TICK)));
    }
}
