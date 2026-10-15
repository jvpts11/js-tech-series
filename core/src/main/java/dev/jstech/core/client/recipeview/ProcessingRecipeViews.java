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
import dev.jstech.core.machine.RecipeConditions;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
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

    /**
     * Where the parts of {@code recipe} go: its item and fluid inputs and its catalysts on the left, its item and
     * fluid outputs and its byproducts on the right, and under them the line of time and energy and every line
     * {@link #lines} says besides.
     */
    public static ProcessingViewLayout layoutOf(final ProcessingRecipe recipe) {
        return new ProcessingViewLayout(recipe.inputs().size() + recipe.fluidInputs().size()
                + recipe.catalysts().size(), recipe.outputs().size() + recipe.fluidOutputs().size()
                + recipe.byproducts().size(), 1 + lines(recipe).size());
    }

    /**
     * What a recipe says under its line of time and energy: each matter it takes and makes, and the temperature and the
     * pressure it runs between.
     */
    public static List<Component> lines(final ProcessingRecipe recipe) {
        final List<Component> lines = new ArrayList<>();
        for (final MatterStack taken : recipe.matterInputs()) {
            lines.add(GameText.component(MachineTexts.RECIPE_MATTER_IN.with(Text.literal(String.valueOf(
                    taken.amount())), taken.material().name())));
        }
        for (final MatterStack made : recipe.matterOutputs()) {
            lines.add(GameText.component(MachineTexts.RECIPE_MATTER_OUT.with(Text.literal(String.valueOf(
                    made.amount())), made.material().name())));
        }
        final RecipeConditions conditions = recipe.conditions();
        range(lines, conditions.leastKelvin(), conditions.mostKelvin(), MachineTexts.RECIPE_TEMPERATURE,
                MachineTexts.RECIPE_TEMPERATURE_ABOVE, MachineTexts.RECIPE_TEMPERATURE_BELOW);
        range(lines, conditions.leastKpa(), conditions.mostKpa(), MachineTexts.RECIPE_PRESSURE,
                MachineTexts.RECIPE_PRESSURE_ABOVE, MachineTexts.RECIPE_PRESSURE_BELOW);
        return lines;
    }

    /** What a byproduct's slot says of how likely it comes: "10% chance". */
    public static Component chance(final float chance) {
        final double percent = Math.round(chance * 1_000.0) / 10.0;
        return GameText.component(MachineTexts.RECIPE_CHANCE.with(Text.literal(number(percent))));
    }

    /** What a catalyst's slot says: it is not used up. */
    public static Component catalyst() {
        return GameText.component(MachineTexts.RECIPE_CATALYST.text());
    }


    /**
     * A layout as large as the largest of {@code recipes}, which every one of them fits in: a layout only grows with
     * its inputs and outputs. A kind with no recipes yet gets the room of one in and one out.
     */
    public static ProcessingViewLayout largest(final Collection<ProcessingRecipe> recipes) {
        int inputs = 1;
        int outputs = 1;
        int lines = 1;
        for (final ProcessingRecipe recipe : recipes) {
            final ProcessingViewLayout layout = layoutOf(recipe);
            inputs = Math.max(inputs, layout.inputs());
            outputs = Math.max(outputs, layout.outputs());
            lines = Math.max(lines, layout.lines());
        }
        return new ProcessingViewLayout(inputs, outputs, lines);
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

    /* A line for a span from {@code least} to {@code most}, when it says anything at all. */
    private static void range(final List<Component> lines, final double least, final double most,
                              final TextKey between, final TextKey above, final TextKey below) {
        final boolean low = least > 0.0;
        final boolean high = most < Double.MAX_VALUE;
        if (low && high) {
            lines.add(GameText.component(between.with(Text.literal(number(least)), Text.literal(number(most)))));
        } else if (low) {
            lines.add(GameText.component(above.with(Text.literal(number(least)))));
        } else if (high) {
            lines.add(GameText.component(below.with(Text.literal(number(most)))));
        }
    }

    /* A number as few digits as say it, to a tenth. */
    private static String number(final double value) {
        final double tenth = Math.round(value * 10.0) / 10.0;
        return tenth == Math.rint(tenth) ? String.valueOf((long) tenth) : String.valueOf(tenth);
    }
}
