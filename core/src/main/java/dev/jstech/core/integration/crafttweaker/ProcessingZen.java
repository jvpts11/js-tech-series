/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionAddRecipe;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredientWithAmount;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.RecipeConditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.openzen.zencode.java.ZenCodeType;

/**
 * A recipe of any processing machine, the Core's or any mod's, from a CraftTweaker script, by the machine kind's id:
 *
 * <pre>{@code
 * import mods.jscore.Processing;
 * Processing.addRecipe("jsindustrial:macerating", "iron_dust", [<item:jscore:iron_dust> * 2],
 *     [<tag:items:c:ores/iron> * 1], 200, 40);
 * Processing.addRecipe("mymod:fusing", "alloy", [<item:mymod:alloy>], [<item:mymod:a>, <item:mymod:b>], 100, 20,
 *     "mymod:fusion");
 * }</pre>
 *
 * Everything else a recipe of the kind can hold, fluids, matter, byproducts and the temperature and pressure it runs
 * between, a script writes with {@code <recipetype:mymod:fusing>.addJsonRecipe(name, {...})}, as the recipe's file
 * does; recipes are taken out with the same recipe type's {@code remove} and {@code removeByName}.
 */
@ZenRegister
@ZenCodeType.Name("mods.jscore.Processing")
public final class ProcessingZen {

    private ProcessingZen() {
    }

    /**
     * Adds a recipe of the processing machines of {@code kind}.
     *
     * @param kind          the machine kind's id
     * @param name          the recipe's name, made into a CraftTweaker id
     * @param outputs       what it makes
     * @param inputs        what it takes, each with its count
     * @param ticks         how long it takes
     * @param energyPerTick what it spends each tick
     * @param knowledge     the knowledge the machine's owner's team must have, or nothing
     */
    @ZenCodeType.Method
    public static void addRecipe(final String kind, final String name, final IItemStack[] outputs,
                                 final IIngredientWithAmount[] inputs,
                                 @ZenCodeType.OptionalInt(ProcessingRecipe.DEFAULT_TICKS) final int ticks,
                                 @ZenCodeType.OptionalInt final int energyPerTick,
                                 @ZenCodeType.OptionalString final String knowledge) {
        final ProcessingKind found = kindOf(kind);
        final List<ItemStack> made = new ArrayList<>(outputs.length);
        for (final IItemStack output : outputs) {
            made.add(output.getInternal().copy());
        }
        final List<SizedIngredient> taken = new ArrayList<>(inputs.length);
        for (final IIngredientWithAmount input : inputs) {
            taken.add(new SizedIngredient(input.ingredient().asVanillaIngredient(), input.amount()));
        }
        final Optional<ResourceLocation> needs = knowledge == null || knowledge.isEmpty() ? Optional.empty()
                : Optional.of(ResourceLocation.parse(knowledge));
        final ProcessingRecipe recipe = new ProcessingRecipe(found, taken, List.of(), made, List.of(), ticks,
                energyPerTick, List.of(), List.of(), List.of(), List.of(), RecipeConditions.ANY, needs);
        final Manager manager = new Manager(found);
        CraftTweakerAPI.apply(new ActionAddRecipe<>(manager, manager.createHolder(manager.fixRecipeId(name), recipe))
                .outputDescriber(holder -> holder.value().outputs().toString()));
    }

    private static ProcessingKind kindOf(final String id) {
        final ResourceLocation wanted = ResourceLocation.parse(id);
        final List<String> known = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            for (final ProcessingKind kind : content.declaredProcessing()) {
                if (kind.id().equals(wanted)) {
                    return kind;
                }
                known.add(kind.id().toString());
            }
        }
        throw new IllegalArgumentException("no processing machine kind " + id + " is declared; the kinds are "
                + known);
    }

    /* The recipes of one kind, as CraftTweaker adds them. */
    private record Manager(ProcessingKind kind) implements IRecipeManager<ProcessingRecipe> {

        @Override
        public RecipeType<ProcessingRecipe> getRecipeType() {
            return this.kind.type();
        }
    }
}
