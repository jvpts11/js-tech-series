/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

/**
 * What the chain's recipes are written with (computers/docs/design/recipes.md, survival.md): the material forms of J's
 * Core by element or material, their liquids, and each chain machine's kind of recipe, each recipe saved under the
 * machine that works it.
 */
final class ChainRecipeItems {

    private ChainRecipeItems() {
    }

    /** {@code element} in {@code form}: an iron ingot, a tin nugget, copper foil. */
    static Item of(final Element element, final Form form) {
        return of(Elements.of(element), form);
    }

    /** {@code material} in {@code form}. */
    static Item of(final Material material, final Form form) {
        return MaterialUses.item(material, form).get();
    }

    static Item ingot(final Element element) {
        return of(element, Forms.INGOT);
    }

    static Item nugget(final Element element) {
        return of(element, Forms.NUGGET);
    }

    static Item dust(final Element element) {
        return of(element, Forms.DUST);
    }

    /** The id {@code item} is registered under. */
    static ResourceLocation idOf(final ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem());
    }

    /** The liquid of {@code material}, in a tank. */
    static Fluid liquid(final Material material) {
        return MaterialLiquids.fluid(material).get();
    }

    /** The kind of recipe the chain machine {@code id} works; a station's of {@code era}. */
    static ProcessingKind kind(final String id, final Era era) {
        for (final ChainMachines.Worked worked : ChainMachines.workedBy(machine(id))) {
            if (worked.era() == era || worked.era() == null) {
                return worked.kind();
            }
        }
        throw new IllegalArgumentException("the machine " + id + " works no recipes of " + era);
    }

    /** The kind of recipe of the chain machine {@code id}, which serves no single era. */
    static ProcessingKind kind(final String id) {
        return ChainMachines.workedBy(machine(id)).getFirst().kind();
    }

    /** A recipe of {@code kind}, to be saved as {@code name} under the machine that works it. */
    static Saved recipe(final ProcessingKind kind, final String name) {
        return new Saved(ProcessingRecipeBuilder.of(kind), ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                kind.id().getPath() + "/" + name));
    }

    private static ChainMachine machine(final String id) {
        return ChainMachines.byId(id).orElseThrow(() -> new IllegalArgumentException("no chain machine " + id));
    }

    /**
     * A recipe being written, and the id it is saved under.
     *
     * @param builder what it takes and makes
     * @param id      its id
     */
    record Saved(ProcessingRecipeBuilder builder, ResourceLocation id) {

        void save(final RecipeOutput output) {
            builder.save(output, id);
        }
    }
}
