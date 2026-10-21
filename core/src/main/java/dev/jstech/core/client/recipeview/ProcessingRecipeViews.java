/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.machine.Byproduct;
import dev.jstech.core.machine.MachineTexts;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.ProcessingViewLayout;
import dev.jstech.core.machine.RecipeConditions;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.progression.IKnowledgeGated;
import dev.jstech.core.progression.KnowledgeGate;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import dev.jstech.core.recipeview.RecipeViews;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * What both recipe viewers show of the processing machines every mod declared: the kinds, the layout each recipe is
 * drawn in, and the line of time and energy under it. The bridges to JEI and to EMI read only this, so the two show
 * the same recipes the same way.
 */
public final class ProcessingRecipeViews {

    /** The padlock drawn beside the knowledge line of a locked recipe, a sprite of the game's GUI atlas. */
    public static final ResourceLocation LOCK = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "recipe/lock");
    /** The padlock's size, in pixels. */
    public static final int LOCK_WIDTH = 7;
    public static final int LOCK_HEIGHT = 9;
    private static final int LOCK_GAP = 2;

    private ProcessingRecipeViews() {
    }

    /** Every kind of processing machine any mod declared, in the order the mods declared them. */
    public static List<ProcessingKind> kinds() {
        return ModContent.all().stream().flatMap(content -> content.declaredProcessing().stream()).toList();
    }

    /**
     * {@code kind} as a kind of recipe view, the same pages in JEI, EMI and REI: its machines beside it, each page as
     * large as its largest recipe's, laid out as {@link #lay} lays it.
     */
    public static RecipeViewKind<RecipeHolder<ProcessingRecipe>> viewKind(final ProcessingKind kind) {
        return RecipeViews.recipes(kind.id().getNamespace(), kind.id().getPath(), kind.title(), kind::type)
                .workstations(() -> machineStacks(kind))
                .sizedBy(recipes -> {
                    final ProcessingViewLayout largest = largest(recipes.stream().map(RecipeHolder::value).toList());
                    return new RecipeViewKind.Size(largest.width(), largest.height());
                })
                .layout((holder, page) -> lay(holder.value(), page))
                .build();
    }

    /**
     * Lays {@code recipe} out: its item and fluid inputs and its catalysts on the left, its outputs and byproducts on
     * the right, the arrow between, and under them its line of time and energy, what else it says, and the knowledge
     * it needs, beside a padlock while the player's team lacks it.
     */
    public static void lay(final ProcessingRecipe recipe, final RecipeView.Builder page) {
        final ProcessingViewLayout layout = layoutOf(recipe);
        final int colour = RecipeViewPalette.get().text();
        int index = 0;
        for (final SizedIngredient input : recipe.inputs()) {
            final ProcessingViewLayout.Point at = layout.input(index++);
            page.input(at.x(), at.y(), List.of(input.getItems()));
        }
        for (final SizedFluidIngredient input : recipe.fluidInputs()) {
            final ProcessingViewLayout.Point at = layout.input(index++);
            page.fluidInput(at.x(), at.y(), List.of(input.getFluids()), input.amount());
        }
        for (final SizedIngredient catalyst : recipe.catalysts()) {
            final ProcessingViewLayout.Point at = layout.input(index++);
            page.catalyst(at.x(), at.y(), List.of(catalyst.getItems())).tooltip(catalyst());
        }
        index = 0;
        for (final ItemStack output : recipe.outputs()) {
            final ProcessingViewLayout.Point at = layout.output(index++);
            page.output(at.x(), at.y(), output);
        }
        for (final FluidStack output : recipe.fluidOutputs()) {
            final ProcessingViewLayout.Point at = layout.output(index++);
            page.fluidOutput(at.x(), at.y(), output, output.getAmount());
        }
        for (final Byproduct byproduct : recipe.byproducts()) {
            final ProcessingViewLayout.Point at = layout.output(index++);
            page.output(at.x(), at.y(), byproduct.stack()).chance(byproduct.chance());
        }
        page.arrow(layout.arrow().x(), layout.arrow().y(), recipe.ticks());
        page.line(layout.textLine().x(), layout.textLine().y(), layout.width(), workLine(recipe), colour);
        final List<Component> lines = lines(recipe);
        for (int line = 0; line < lines.size(); line++) {
            final ProcessingViewLayout.Point at = layout.textLine(line + 1);
            page.line(at.x(), at.y(), layout.width(), lines.get(line), colour);
        }
        knowledgeLine(recipe).ifPresent(needs -> {
            final ProcessingViewLayout.Point at = layout.textLine(lines.size() + 1);
            if (locked(recipe)) {
                page.sprite(LOCK, at.x(), at.y(), LOCK_WIDTH, LOCK_HEIGHT);
            }
            final int indent = knowledgeIndent(recipe);
            page.line(at.x() + indent, at.y(), layout.width() - indent, needs, colour);
        });
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
                + recipe.byproducts().size(), 1 + lines(recipe).size() + (recipe.knowledge().isPresent() ? 1 : 0));
    }

    /**
     * The line naming the knowledge {@code recipe} needs, last under it, if it needs any: in the viewer's own colour
     * when the player's team has it, and the palette's locked colour beside a padlock when it does not.
     */
    public static Optional<Component> knowledgeLine(final IKnowledgeGated recipe) {
        return recipe.knowledge().map(id -> {
            final Text name = Knowledges.byId(id).map(known -> known.name().text())
                    .orElseGet(() -> Text.literal(id.toString()));
            final MutableComponent line = GameText.component(MachineTexts.RECIPE_NEEDS.with(name));
            return KnowledgeGate.openHere(recipe.knowledge()) ? line
                    : line.withColor(RecipeViewPalette.get().locked());
        });
    }

    /** Whether {@code recipe} is locked for this client's player's team: drawn with a padlock. */
    public static boolean locked(final IKnowledgeGated recipe) {
        return !KnowledgeGate.openHere(recipe.knowledge());
    }

    /** How far the knowledge line of {@code recipe} stands in from the left, past its padlock when it has one. */
    public static int knowledgeIndent(final IKnowledgeGated recipe) {
        return locked(recipe) ? LOCK_WIDTH + LOCK_GAP : 0;
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
