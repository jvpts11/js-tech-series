/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.chemistry;

import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.machine.RecipeConditions;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A reaction as a chemical machine's recipe: its gases taken from and made into the machine's tanks of gas, its
 * solids as dusts, each in the amount its coefficient gives ({@link Reaction#amount}); the temperature and the
 * pressure it runs between; its catalyst in a slot of its own, which wears as the reaction runs. A reaction that does
 * not balance never becomes one.
 */
public final class ReactionRecipes {

    private ReactionRecipes() {
    }

    /**
     * The recipe of {@code kind} that runs {@code reaction}, {@code batches} batches at a time, at
     * {@code conditions}, over {@code catalyst}; its time and its energy are the builder's to give.
     *
     * @throws IllegalArgumentException when a substance is no material, or is a liquid, which no machine of the
     *                                  series' chemistry takes in its tanks yet
     */
    public static ProcessingRecipeBuilder of(final ProcessingKind kind, final Reaction reaction, final int batches,
                                             final Reactions.Conditions conditions, final Ingredient catalyst) {
        final ProcessingRecipeBuilder builder = ProcessingRecipeBuilder.of(kind)
                .conditions(new RecipeConditions(conditions.leastKelvin(), conditions.mostKelvin(),
                        conditions.leastKpa(), conditions.mostKpa()))
                .catalyst(catalyst, 1);
        for (final Reaction.Term term : reaction.reactants()) {
            final Material material = material(term);
            final long amount = reaction.amount(term, batches);
            if (term.phase() == Reaction.Phase.SOLID) {
                builder.input(Forms.DUST.itemTag(material), Math.toIntExact(amount));
            } else {
                builder.matterInput(gas(material, term), amount);
            }
        }
        for (final Reaction.Term term : reaction.products()) {
            final Material material = material(term);
            final long amount = reaction.amount(term, batches);
            if (term.phase() == Reaction.Phase.SOLID) {
                builder.output(MaterialUses.item(material, Forms.DUST).get(), Math.toIntExact(amount));
            } else {
                builder.matterOutput(gas(material, term), amount);
            }
        }
        return builder;
    }

    private static Material material(final Reaction.Term term) {
        return Materials.byId(ResourceLocation.parse(term.material())).orElseThrow(() ->
                new IllegalArgumentException("the reaction names " + term.material() + ", which is no material"));
    }

    private static Material gas(final Material material, final Reaction.Term term) {
        if (!material.takes(MatterState.GAS)) {
            throw new IllegalArgumentException("the reaction takes " + term.material() + " as a fluid, and only gases"
                    + " are taken in a chemical machine's tanks yet");
        }
        return material;
    }
}
