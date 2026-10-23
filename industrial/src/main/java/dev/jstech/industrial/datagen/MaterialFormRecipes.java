/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.datagen;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUse;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.blockentity.MaceratorBlockEntity;
import dev.jstech.industrial.material.IndustrialMaterials;
import dev.jstech.industrial.metallurgy.FormRoute;
import dev.jstech.industrial.metallurgy.FormRoutes;
import dev.jstech.industrial.metallurgy.Yield;
import dev.jstech.industrial.tool.HandTool;
import dev.jstech.industrial.tool.HandTools;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Emits the recipes of every form of a metal J's Industrial uses, from its line of {@link FormRoutes}: each machine's
 * that is in the game, with the scraps rolling and drawing leave, counted; and the hand tool's, at the crafting table,
 * yielding less. Making a form of a metal is declaring its use; making a form by a new machine is the machine's line
 * in the routes and its kind here. No other datagen file changes.
 */
public final class MaterialFormRecipes {

    /** How long pressing and grinding (ingots and ores alike) take at Tier 1, in ticks. */
    static final int COMPRESSING_TICKS = 120;
    static final int MACERATING_TICKS = 200;

    /* The machines in the game, by their routes' names, with their recipe kinds, time and energy. */
    private static final Map<String, Machine> MACHINES = Map.of(
            "compressor", new Machine(IndustrialModule.COMPRESSING, COMPRESSING_TICKS,
                    CompressorBlockEntity.FE_PER_TICK),
            "macerator", new Machine(IndustrialModule.MACERATING, MACERATING_TICKS,
                    MaceratorBlockEntity.FE_PER_TICK));

    private MaterialFormRecipes() {
    }

    /** Generates the recipes of every form of a metal the mod uses that has a route. */
    public static void generateAll(final RecipeOutput out) {
        final Set<String> seen = new LinkedHashSet<>();
        for (final MaterialUse use : MaterialUses.all()) {
            if (!use.modid().equals(JsIndustrial.MODID) || use.rock() != null
                    || !seen.add(use.material().id() + "/" + use.form().id())) {
                continue;
            }
            final Optional<FormRoute> route = FormRoutes.of(use.form().id().getPath());
            final Optional<Form> from = route.flatMap(found -> Forms.all().stream()
                    .filter(form -> form.id().getPath().equals(found.from())).findFirst());
            if (route.isEmpty() || from.isEmpty() || !MaterialUses.used(use.material(), from.get())) {
                continue;
            }
            machines(out, use.material(), use.form(), from.get(), route.get());
            byHand(out, use.material(), use.form(), from.get(), route.get());
        }
    }

    /** Adds a macerating recipe under the {@code macerating/} prefix; the one place the grinding cost is set. */
    static ProcessingRecipeBuilder macerating(final Ingredient ingredient, final ItemStack result) {
        return ProcessingRecipeBuilder.of(IndustrialModule.MACERATING).input(ingredient, 1).output(result)
                .ticks(MACERATING_TICKS).energyPerTick(MaceratorBlockEntity.FE_PER_TICK);
    }

    /** The {@code c:} common item tag at {@code path}. */
    static TagKey<Item> cTag(final String path) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
    }

    static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }

    /* Each machine in the game that makes the form, with its yield, and the scraps of the work counted. */
    private static void machines(final RecipeOutput out, final Material material, final Form form, final Form from,
                                 final FormRoute route) {
        for (final FormRoute.MachineWay way : route.machines()) {
            final Machine machine = MACHINES.get(way.machine());
            if (machine == null) {
                continue;
            }
            final ProcessingRecipeBuilder builder = ProcessingRecipeBuilder.of(machine.kind())
                    .input(from.itemTag(material), way.yield().in())
                    .output(MaterialUses.item(material, form).get(), way.yield().out())
                    .ticks(machine.ticks()).energyPerTick(machine.energyPerTick());
            if (route.leavesScrap() && MaterialUses.used(material, IndustrialMaterials.SCRAP)) {
                builder.countedByproduct(new ItemStack(MaterialUses.item(material, IndustrialMaterials.SCRAP).get()),
                        (float) FormRoutes.SCRAP_SHARE);
            }
            builder.save(out, rl(way.machine() + "/" + name(material, from, form)));
        }
    }

    /* The form by hand: with its hand tool at the crafting table, or as nuggets and blocks are, nine for one. */
    private static void byHand(final RecipeOutput out, final Material material, final Form form, final Form from,
                               final FormRoute route) {
        if (route.handYield().isEmpty()) {
            return;
        }
        final Yield yield = route.handYield().get();
        final Item made = MaterialUses.item(material, form).get();
        final Item source = MaterialUses.item(material, from).get();
        if (route.hand() == FormRoute.ByHand.CRAFTING_TABLE) {
            // The game has its own for its nuggets and blocks of iron and gold; the Core's are the rest.
            if (material.provided(form).isPresent() && material.provided(from).isPresent()) {
                return;
            }
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, made, yield.out())
                    .requires(Ingredient.of(from.itemTag(material)), yield.in())
                    .unlockedBy("has_" + from.id().getPath(), has(from.itemTag(material)))
                    .save(out, rl("crafting/" + name(material, from, form)));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, source, yield.in())
                    .requires(Ingredient.of(form.itemTag(material)), yield.out())
                    .unlockedBy("has_" + form.id().getPath(), has(form.itemTag(material)))
                    .save(out, rl("crafting/" + name(material, form, from)));
            return;
        }
        final HandTool tool = tool(route.hand());
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, made, yield.out()).requires(tool)
                .requires(Ingredient.of(from.itemTag(material)), yield.in())
                .unlockedBy("has_" + from.id().getPath(), has(from.itemTag(material)))
                .save(out, rl("hand/" + name(material, from, form)));
    }

    /** What unlocks a recipe: having any item of {@code tag}. */
    static Criterion<InventoryChangeTrigger.TriggerInstance> has(final TagKey<Item> tag) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag).build());
    }

    private static HandTool tool(final FormRoute.ByHand hand) {
        for (final ItemEntry<HandTool> entry : HandTools.all()) {
            if (entry.get().work() == hand) {
                return entry.get();
            }
        }
        throw new IllegalStateException("no hand tool does " + hand);
    }

    /* A recipe's name: its material, from one form to another, "iron_ingot_to_plate". */
    private static String name(final Material material, final Form from, final Form to) {
        return material.id().getPath() + "_" + from.id().getPath() + "_to_" + to.id().getPath();
    }

    /* A machine in the game, as its recipes need it. */
    private record Machine(ProcessingKind kind, int ticks, int energyPerTick) {
    }
}
