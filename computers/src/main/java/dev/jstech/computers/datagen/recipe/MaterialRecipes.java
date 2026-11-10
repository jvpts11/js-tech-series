/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.dust;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.ingot;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.kind;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.liquid;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.nugget;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.of;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.recipe;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUse;
import dev.jstech.core.material.MaterialUses;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;

/**
 * The chain's materials, as computers/docs/design/survival.md ("The chain's numbers") makes them: ore crushed into two
 * crushed ores with its byproducts' chances, crushed ore and byproduct dust smelted (in the furnace the metals it
 * melts, in the Induction Furnace the refractory ones), the forms rolled, drawn and ground from any metal or alloy the
 * chain uses, the alloys and furnace work in their real proportions, the air separated, the electrolysis, the coke and
 * the chemistry. Every number is a first estimate.
 */
final class MaterialRecipes {

    /* Each byproduct dust a crushed ore may come with, and its chance. */
    private static final Map<Element, List<Byproduct>> BYPRODUCTS = Map.of(
            Element.ALUMINIUM, List.of(new Byproduct(Element.GALLIUM, 0.25F)),
            Element.ZINC, List.of(new Byproduct(Element.INDIUM, 0.15F), new Byproduct(Element.GERMANIUM, 0.10F),
                    new Byproduct(Element.SULFUR, 0.50F)),
            Element.ZIRCONIUM, List.of(new Byproduct(Element.HAFNIUM, 0.25F)),
            Element.COPPER, List.of(new Byproduct(Element.ARSENIC, 0.15F)),
            Element.GOLD, List.of(new Byproduct(Element.ARSENIC, 0.15F)),
            Element.NICKEL, List.of(new Byproduct(Element.RUTHENIUM, 0.05F)),
            Element.LEAD, List.of(new Byproduct(Element.SULFUR, 0.50F)));
    /* The metals the vanilla furnace melts from their crushed ore or their dust; the refractory ones need the IF. */
    private static final List<Element> FURNACE_METALS = List.of(Element.IRON, Element.COPPER, Element.GOLD,
            Element.TIN, Element.LEAD, Element.ZINC, Element.SILVER, Element.NICKEL, Element.COBALT,
            Element.MANGANESE, Element.GALLIUM, Element.INDIUM);
    private static final List<Element> REFRACTORY_METALS = List.of(Element.TANTALUM, Element.CHROMIUM,
            Element.TUNGSTEN, Element.MOLYBDENUM, Element.ZIRCONIUM, Element.HAFNIUM, Element.RUTHENIUM);
    /* The foils the chain only ever uses as foil, rolled from the ingot straight. */
    private static final List<Material> FOIL_ONLY = List.of(Elements.of(Element.NICKEL), Elements.of(Element.SILVER),
            SeriesAlloys.PERMALLOY, SeriesAlloys.INDIUM_TIN_OXIDE);
    private static final int FURNACE_TICKS = 200;
    private static final float FURNACE_XP = 0.7F;
    private static final int TICKS = 20;
    private static final int BUCKET = 1_000;

    private MaterialRecipes() {
    }

    static void write(final RecipeOutput output) {
        ore(output);
        smelting(output);
        forms(output);
        nuggets(output);
        alloys(output);
        electrolysis(output);
        coke(output);
        chemistry(output);
        components(output);
        silicon(output);
    }

    /* 1 ore, the block or its raw form, into 2 crushed ores, with each byproduct's dust by its chance. */
    private static void ore(final RecipeOutput output) {
        final ProcessingKind crusher = kind("crusher");
        for (final Material material : used(Forms.CRUSHED)) {
            for (final Form from : List.of(Forms.ORE, Forms.RAW)) {
                final ChainRecipeItems.Saved crushed = recipe(crusher, material.id().getPath() + "_from_"
                        + from.id().getPath());
                crushed.builder().input(from.itemTag(material), 1).output(of(material, Forms.CRUSHED), 2);
                if (Elements.element(material).isPresent()) {
                    for (final Byproduct byproduct : BYPRODUCTS.getOrDefault(Elements.element(material).get(),
                            List.of())) {
                        crushed.builder().byproduct(new ItemStack(dust(byproduct.element())), byproduct.chance());
                    }
                }
                crushed.save(output);
            }
        }
    }

    /*
     * A crushed ore, or a byproduct's dust, gives one ingot: in the vanilla furnace for the metals it melts, in the
     * Induction Furnace for the refractory ones. Crushed mercury ore, roasted, gives its mercury.
     */
    private static void smelting(final RecipeOutput output) {
        for (final Element element : FURNACE_METALS) {
            for (final Form from : List.of(Forms.CRUSHED, Forms.DUST)) {
                if (used(Elements.of(element), from) && used(Elements.of(element), Forms.INGOT)) {
                    SimpleCookingRecipeBuilder.smelting(Ingredient.of(of(element, from)), RecipeCategory.MISC,
                                    ingot(element), FURNACE_XP, FURNACE_TICKS)
                            .unlockedBy("has_it", has(of(element, from)))
                            .save(output, id("smelting/" + element.name().toLowerCase() + "_from_"
                                    + from.id().getPath()));
                }
            }
        }
        final ProcessingKind furnace = kind("induction_furnace");
        for (final Element element : REFRACTORY_METALS) {
            for (final Form from : List.of(Forms.CRUSHED, Forms.DUST)) {
                if (used(Elements.of(element), from) && used(Elements.of(element), Forms.INGOT)) {
                    final ChainRecipeItems.Saved melted = recipe(furnace, element.name().toLowerCase() + "_from_"
                            + from.id().getPath());
                    melted.builder().input(of(element, from), 1).output(ingot(element), 1);
                    melted.save(output);
                }
            }
        }
        final ChainRecipeItems.Saved mercury = recipe(furnace, "mercury");
        mercury.builder().input(of(Element.MERCURY, Forms.CRUSHED), 1).fluidOutput(liquid(Elements.of(
                Element.MERCURY)), 100);
        mercury.save(output);
    }

    /*
     * The forms of any metal or alloy the chain uses: the Rolling Mill turns 1 ingot into 1 plate, 1 plate into 2
     * sheets and 1 sheet into 4 foils, those only used as foil 1 ingot into 8; the Wire Drawer 1 ingot into 2 rods and
     * 1 rod into 2 wires; the Grinder 1 ingot or crystal into 1 dust, and 1 log into 4 wood pulp, which the Rolling
     * Mill presses into 2 paper.
     */
    private static void forms(final RecipeOutput output) {
        final ProcessingKind mill = kind("rolling_mill");
        final ProcessingKind drawer = kind("wire_drawer");
        final ProcessingKind grinder = kind("grinder");
        for (final Material material : usedMaterials()) {
            final String name = material.id().getPath();
            if (FOIL_ONLY.contains(material)) {
                step(output, mill, name + "_foil", material, Forms.INGOT, 1, Forms.FOIL, 8);
            } else {
                step(output, mill, name + "_plate", material, Forms.INGOT, 1, Forms.PLATE, 1);
                step(output, mill, name + "_sheet", material, Forms.PLATE, 1, Forms.SHEET, 2);
                step(output, mill, name + "_foil", material, Forms.SHEET, 1, Forms.FOIL, 4);
            }
            step(output, drawer, name + "_rod", material, Forms.INGOT, 1, Forms.ROD, 2);
            step(output, drawer, name + "_wire", material, Forms.ROD, 1, Forms.WIRE, 2);
            step(output, grinder, name + "_dust", material, Forms.INGOT, 1, Forms.DUST, 1);
            step(output, grinder, name + "_dust_from_crystal", material, Forms.CRYSTAL, 1, Forms.DUST, 1);
        }
        for (final Element bonding : List.of(Element.GOLD, Element.ALUMINIUM)) {
            final ChainRecipeItems.Saved wire = recipe(drawer, bonding.name().toLowerCase() + "_bonding_wire");
            wire.builder().input(ingot(bonding), 1).output(bonding == Element.GOLD
                    ? ChainParts.GOLD_BONDING_WIRE.get() : ChainParts.ALUMINIUM_BONDING_WIRE.get(), 16);
            wire.save(output);
        }
        final ChainRecipeItems.Saved pulp = recipe(grinder, "wood_pulp");
        pulp.builder().input(ItemTags.LOGS, 1).output(of(SeriesCompounds.WOOD_PULP, Forms.DUST), 4);
        pulp.save(output);
        final ChainRecipeItems.Saved paper = recipe(mill, "paper");
        paper.builder().input(of(SeriesCompounds.WOOD_PULP, Forms.DUST), 1).output(Items.PAPER, 2);
        paper.save(output);
    }

    /* An ingot gives 9 nuggets and 9 nuggets an ingot, at the crafting table, for the metals the game has none of. */
    private static void nuggets(final RecipeOutput output) {
        for (final Material material : usedMaterials()) {
            if (!used(material, Forms.NUGGET) || !used(material, Forms.INGOT)
                    || material == Elements.of(Element.IRON) || material == Elements.of(Element.GOLD)) {
                continue;
            }
            final String name = material.id().getPath();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, of(material, Forms.NUGGET), 9)
                    .requires(of(material, Forms.INGOT)).unlockedBy("has_it", has(of(material, Forms.INGOT)))
                    .save(output, id("crafting/" + name + "_nuggets"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, of(material, Forms.INGOT))
                    .pattern("NNN").pattern("NNN").pattern("NNN").define('N', of(material, Forms.NUGGET))
                    .unlockedBy("has_it", has(of(material, Forms.NUGGET)))
                    .save(output, id("crafting/" + name + "_ingot_from_nuggets"));
        }
    }

    /* The Induction Furnace's alloys and its furnace work, in their real proportions. */
    private static void alloys(final RecipeOutput output) {
        final ProcessingKind furnace = kind("induction_furnace");
        made(output, furnace, "solder", List.of(ingot(Element.TIN), ingot(Element.LEAD)), List.of(3, 2),
                of(SeriesAlloys.SOLDER, Forms.WIRE), 5);
        made(output, furnace, "lead_free_solder", List.of(ingot(Element.TIN), ingot(Element.SILVER),
                nugget(Element.COPPER)), List.of(9, 1, 1), of(SeriesAlloys.LEAD_FREE_SOLDER, Forms.WIRE), 10);
        made(output, furnace, "permalloy", List.of(ingot(Element.NICKEL), ingot(Element.IRON)), List.of(4, 1),
                of(SeriesAlloys.PERMALLOY, Forms.INGOT), 5);
        made(output, furnace, "indium_tin_oxide", List.of(ingot(Element.INDIUM), ingot(Element.TIN)), List.of(9, 1),
                of(SeriesAlloys.INDIUM_TIN_OXIDE, Forms.INGOT), 10);
        made(output, furnace, "steel", List.of(ingot(Element.IRON), of(SeriesCompounds.COKE, Forms.LUMP)),
                List.of(2, 1), of(SeriesAlloys.STEEL, Forms.INGOT), 2);
        made(output, furnace, "stainless_steel", List.of(ingot(Element.IRON), ingot(Element.CHROMIUM),
                ingot(Element.NICKEL)), List.of(7, 2, 1), of(SeriesAlloys.STAINLESS_STEEL, Forms.INGOT), 10);
        made(output, furnace, "metallurgical_silicon", List.of(Items.SAND, of(SeriesCompounds.COKE, Forms.LUMP)),
                List.of(1, 1), of(SeriesCompounds.METALLURGICAL_SILICON, Forms.LUMP), 1);
        made(output, furnace, "calcium_carbide", List.of(Items.CALCITE, of(SeriesCompounds.COKE, Forms.LUMP)),
                List.of(1, 1), of(SeriesCompounds.CALCIUM_CARBIDE, Forms.LUMP), 1);
    }

    /* Water split, the chlor-alkali cell, aluminium from alumina and coke, and copper refined. */
    private static void electrolysis(final RecipeOutput output) {
        final ProcessingKind cell = kind("electrolytic_cell");
        final ChainRecipeItems.Saved water = recipe(cell, "water");
        water.builder().fluidInput(Fluids.WATER, BUCKET)
                .matterOutput(Elements.of(Element.HYDROGEN), BUCKET).matterOutput(Elements.of(Element.OXYGEN), 500);
        water.save(output);
        final ChainRecipeItems.Saved brine = recipe(cell, "chlor_alkali");
        brine.builder().input(of(SeriesCompounds.SALT, Forms.DUST), 1).fluidInput(Fluids.WATER, BUCKET)
                .matterOutput(Elements.of(Element.CHLORINE), 500).matterOutput(Elements.of(Element.HYDROGEN), 500)
                .output(of(SeriesCompounds.CAUSTIC_SODA, Forms.DUST), 1);
        brine.save(output);
        made(output, cell, "aluminium", List.of(of(SeriesCompounds.ALUMINA, Forms.DUST),
                of(SeriesCompounds.COKE, Forms.LUMP)), List.of(1, 1), ingot(Element.ALUMINIUM), 1);
        made(output, cell, "refined_copper", List.of(ingot(Element.COPPER)), List.of(1),
                of(SeriesAlloys.REFINED_COPPER, Forms.INGOT), 1);
    }

    /* The Coke Oven turns 1 coal into 1 coke and 250 mB of coal tar, burning part of its own coal. */
    private static void coke(final RecipeOutput output) {
        final ChainRecipeItems.Saved coke = recipe(kind("coke_oven"), "coke");
        coke.builder().input(Items.COAL, 1).output(of(SeriesCompounds.COKE, Forms.LUMP), 1)
                .fluidOutput(liquid(SeriesChemicals.COAL_TAR), 250);
        coke.save(output);
    }

    /* The Chemical Reactor's chemistry, each product from what the design's table says. */
    private static void chemistry(final RecipeOutput output) {
        final ProcessingKind reactor = kind("chemical_reactor");
        final ChainRecipeItems.Saved salt = recipe(reactor, "salt");
        salt.builder().fluidInput(Fluids.WATER, 4 * BUCKET).output(of(SeriesCompounds.SALT,
                Forms.DUST), 1).countedByproduct(new ItemStack(dust(Element.BORON)), 1F / 16).ticks(60 * TICKS);
        salt.save(output);
        final ChainRecipeItems.Saved alumina = recipe(reactor, "alumina");
        alumina.builder().input(of(Element.ALUMINIUM, Forms.CRUSHED), 1).input(of(SeriesCompounds.CAUSTIC_SODA,
                Forms.DUST), 1).output(of(SeriesCompounds.ALUMINA, Forms.DUST), 1);
        alumina.save(output);
        final ChainRecipeItems.Saved chloride = recipe(reactor, "hydrogen_chloride");
        chloride.builder().matterInput(Elements.of(Element.HYDROGEN), 500).matterInput(Elements.of(Element.CHLORINE),
                500).matterOutput(SeriesChemicals.HYDROGEN_CHLORIDE, BUCKET);
        chloride.save(output);
        final ChainRecipeItems.Saved sulfuric = recipe(reactor, "sulfuric_acid");
        sulfuric.builder().input(dust(Element.SULFUR), 1).matterInput(Elements.of(Element.OXYGEN), 500)
                .fluidInput(Fluids.WATER, BUCKET).fluidOutput(liquid(SeriesChemicals.SULFURIC_ACID),
                        BUCKET);
        sulfuric.save(output);
        final ChainRecipeItems.Saved hydrofluoric = recipe(reactor, "hydrofluoric_acid");
        hydrofluoric.builder().input(of(SeriesMinerals.FLUORSPAR, Forms.DUST), 1)
                .fluidInput(liquid(SeriesChemicals.SULFURIC_ACID), BUCKET)
                .fluidOutput(liquid(SeriesChemicals.HYDROFLUORIC_ACID), BUCKET);
        hydrofluoric.save(output);
        final ChainRecipeItems.Saved ferric = recipe(reactor, "ferric_chloride");
        ferric.builder().input(dust(Element.IRON), 1).matterInput(SeriesChemicals.HYDROGEN_CHLORIDE, BUCKET)
                .fluidOutput(liquid(SeriesChemicals.FERRIC_CHLORIDE), BUCKET);
        ferric.save(output);
        final ChainRecipeItems.Saved phenol = recipe(reactor, "phenol");
        phenol.builder().fluidInput(liquid(SeriesChemicals.COAL_TAR), BUCKET)
                .fluidOutput(liquid(SeriesChemicals.PHENOL), 500);
        phenol.save(output);
        final ChainRecipeItems.Saved methanol = recipe(reactor, "methanol");
        methanol.builder().input(of(SeriesCompounds.COKE, Forms.LUMP), 1).fluidInput(Fluids.WATER, BUCKET)
                .fluidOutput(liquid(SeriesChemicals.METHANOL), 500);
        methanol.save(output);
        final ChainRecipeItems.Saved phenolic = recipe(reactor, "phenolic_resin");
        phenolic.builder().fluidInput(liquid(SeriesChemicals.PHENOL), 500)
                .fluidInput(liquid(SeriesChemicals.METHANOL), 500).output(of(SeriesCompounds.PHENOLIC_RESIN,
                        Forms.PLATE), 1);
        phenolic.save(output);
        final ChainRecipeItems.Saved epoxy = recipe(reactor, "epoxy_resin");
        epoxy.builder().fluidInput(liquid(SeriesChemicals.PHENOL), 500).matterInput(Elements.of(Element.CHLORINE), 250)
                .output(of(SeriesCompounds.EPOXY_RESIN, Forms.PLATE), 1);
        epoxy.save(output);
        final ChainRecipeItems.Saved polycarbonate = recipe(reactor, "polycarbonate");
        polycarbonate.builder().fluidInput(liquid(SeriesChemicals.PHENOL), 500)
                .matterInput(Elements.of(Element.CHLORINE), 250).input(of(SeriesCompounds.COKE, Forms.LUMP), 1)
                .output(of(SeriesCompounds.POLYCARBONATE, Forms.SHEET), 1);
        polycarbonate.save(output);
        final ChainRecipeItems.Saved acetylene = recipe(reactor, "acetylene");
        acetylene.builder().input(of(SeriesCompounds.CALCIUM_CARBIDE, Forms.LUMP), 1).fluidInput(Fluids.WATER, BUCKET)
                .matterOutput(SeriesChemicals.ACETYLENE, BUCKET);
        acetylene.save(output);
        final ChainRecipeItems.Saved pvc = recipe(reactor, "pvc");
        pvc.builder().matterInput(SeriesChemicals.ACETYLENE, BUCKET).matterInput(SeriesChemicals.HYDROGEN_CHLORIDE,
                BUCKET).output(of(SeriesCompounds.PVC, Forms.SHEET), 1);
        pvc.save(output);
        final ChainRecipeItems.Saved photoresist = recipe(reactor, "photoresist");
        photoresist.builder().input(of(SeriesCompounds.PHENOLIC_RESIN, Forms.PLATE), 1)
                .fluidInput(liquid(SeriesChemicals.METHANOL), 500)
                .fluidOutput(liquid(SeriesChemicals.PHOTORESIST), BUCKET);
        photoresist.save(output);
        final ChainRecipeItems.Saved quartz = recipe(reactor, "cultured_quartz");
        quartz.builder().input(Items.SAND, 4).input(of(SeriesCompounds.CAUSTIC_SODA, Forms.DUST), 1)
                .fluidInput(Fluids.WATER, BUCKET).output(of(SeriesCompounds.CULTURED_QUARTZ, Forms.CRYSTAL), 1)
                .ticks(60 * TICKS);
        quartz.save(output);
        made(output, reactor, "phosphorus", List.of(Items.BONE_MEAL, of(SeriesCompounds.COKE, Forms.LUMP),
                Items.SAND), List.of(4, 1, 1), dust(Element.PHOSPHORUS), 1);
        final ChainRecipeItems.Saved ultrapure = recipe(reactor, "ultrapure_water");
        ultrapure.builder().fluidInput(Fluids.WATER, BUCKET).fluidOutput(liquid(SeriesChemicals.ULTRAPURE_WATER), 500);
        ultrapure.save(output);
        made(output, reactor, "zinc_sulfide", List.of(dust(Element.ZINC), dust(Element.SULFUR)), List.of(1, 1),
                of(SeriesCompounds.ZINC_SULFIDE, Forms.DUST), 1);
        final ChainRecipeItems.Saved crystal = recipe(reactor, "liquid_crystal");
        crystal.builder().fluidInput(liquid(SeriesChemicals.PHENOL), 500).input(of(SeriesMinerals.FLUORSPAR,
                Forms.DUST), 1).fluidOutput(liquid(SeriesChemicals.LIQUID_CRYSTAL), 250);
        crystal.save(output);
        made(output, reactor, "thermal_paste", List.of(dust(Element.SILVER), dust(Element.ZINC)), List.of(1, 1),
                ChainParts.THERMAL_PASTE.get(), 4);
        final ChainRecipeItems.Saved lithium = recipe(reactor, "lithium_hydroxide");
        lithium.builder().input(of(Element.LITHIUM, Forms.CRUSHED), 1).input(of(SeriesCompounds.CAUSTIC_SODA,
                Forms.DUST), 1).fluidInput(liquid(SeriesChemicals.SULFURIC_ACID), BUCKET)
                .output(of(SeriesCompounds.LITHIUM_HYDROXIDE, Forms.DUST), 1);
        lithium.save(output);
        final ChainRecipeItems.Saved electrolyte = recipe(reactor, "battery_electrolyte");
        electrolyte.builder().input(of(SeriesCompounds.LITHIUM_HYDROXIDE, Forms.DUST), 1)
                .input(dust(Element.PHOSPHORUS), 1).fluidInput(liquid(SeriesChemicals.HYDROFLUORIC_ACID), 500)
                .fluidOutput(liquid(SeriesChemicals.BATTERY_ELECTROLYTE), BUCKET);
        electrolyte.save(output);
    }

    /* The components the chain's machines make, by the design's table of components. */
    private static void components(final RecipeOutput output) {
        final ProcessingKind kiln = kind("ceramic_kiln");
        made(output, kiln, "ceramic_package", List.of(of(SeriesCompounds.ALUMINA, Forms.DUST)), List.of(1),
                ChainParts.CERAMIC_PACKAGE.get(), 4);
        made(output, kiln, "ceramic_capacitor", List.of(of(SeriesCompounds.ALUMINA, Forms.DUST),
                of(Element.NICKEL, Forms.FOIL)), List.of(1, 1), ChainParts.CERAMIC_CAPACITOR.get(), 8);
        made(output, kiln, "tantalum_capacitor", List.of(dust(Element.TANTALUM), dust(Element.MANGANESE)),
                List.of(1, 1), ChainParts.TANTALUM_CAPACITOR.get(), 8);
        made(output, kiln, "lithium_cobalt_oxide", List.of(of(SeriesCompounds.LITHIUM_HYDROXIDE, Forms.DUST),
                dust(Element.COBALT)), List.of(1, 1), of(SeriesCompounds.LITHIUM_COBALT_OXIDE, Forms.DUST), 1);
        made(output, kiln, "ferrite", List.of(dust(Element.IRON), dust(Element.MANGANESE), dust(Element.ZINC)),
                List.of(2, 1, 1), ChainParts.FERRITE.get(), 4);
        final ProcessingKind winder = kind("coil_winder");
        final Item copperWire = of(Element.COPPER, Forms.WIRE);
        made(output, winder, "inductor", List.of(ChainParts.FERRITE.get(), copperWire), List.of(1, 2),
                ChainParts.INDUCTOR.get(), 2);
        made(output, winder, "transformer", List.of(ChainParts.FERRITE.get(), copperWire), List.of(2, 4),
                ChainParts.TRANSFORMER.get(), 1);
        made(output, winder, "electric_motor", List.of(of(SeriesAlloys.STEEL, Forms.ROD), copperWire,
                ChainParts.FERRITE.get()), List.of(1, 4, 1), ChainParts.ELECTRIC_MOTOR.get(), 1);
        made(output, winder, "magnetic_head", List.of(of(SeriesAlloys.PERMALLOY, Forms.FOIL), copperWire),
                List.of(1, 1), ChainParts.MAGNETIC_HEAD.get(), 2);
        made(output, winder, "copper_coil", List.of(copperWire), List.of(4), ChainParts.COPPER_COIL.get(), 1);
        final ProcessingKind polisher = kind("optical_polisher");
        made(output, polisher, "mask_plate", List.of(of(SeriesCompounds.CULTURED_QUARTZ, Forms.CRYSTAL)), List.of(1),
                ChainParts.MASK_PLATE.get(), 1);
        made(output, polisher, "glass_lens", List.of(Items.GLASS), List.of(1), of(SeriesCompounds.GLASS, Forms.LENS),
                4);
        made(output, polisher, "calcium_fluoride_lens", List.of(of(SeriesCompounds.CALCIUM_FLUORIDE, Forms.BOULE)),
                List.of(1), of(SeriesCompounds.CALCIUM_FLUORIDE, Forms.LENS), 4);
        made(output, polisher, "blank_disc", List.of(of(SeriesCompounds.POLYCARBONATE, Forms.SHEET)), List.of(1),
                ChainParts.BLANK_DISC.get(), 4);
        made(output, polisher, "glass_fibre", List.of(Items.GLASS, of(SeriesCompounds.ALUMINA, Forms.DUST)),
                List.of(1, 1), ChainParts.GLASS_FIBRE.get(), 8);
        made(output, polisher, "optical_fibre", List.of(Items.GLASS, dust(Element.GERMANIUM)), List.of(1, 1),
                ChainParts.OPTICAL_FIBRE.get(), 8);
        final ProcessingKind deposition = kind("deposition_chamber");
        made(output, deposition, "mask_blank", List.of(ChainParts.MASK_PLATE.get(), nugget(Element.CHROMIUM)),
                List.of(1, 1), ChainChips.MASK_BLANK.get(), 1);
        made(output, deposition, "oscillator", List.of(of(SeriesCompounds.CULTURED_QUARTZ, Forms.CRYSTAL),
                Items.GOLD_NUGGET), List.of(1, 1), ChainParts.OSCILLATOR.get(), 4);
        made(output, deposition, "platter_vintage", List.of(of(Element.ALUMINIUM, Forms.SHEET), dust(Element.IRON)),
                List.of(1, 1), ChainParts.PLATTER_VINTAGE.get(), 1);
        made(output, deposition, "platter", List.of(of(Element.ALUMINIUM, Forms.SHEET), nugget(Element.COBALT)),
                List.of(1, 1), ChainParts.PLATTER.get(), 1);
        made(output, deposition, "floppy_film", List.of(of(SeriesCompounds.PVC, Forms.SHEET), dust(Element.IRON)),
                List.of(1, 1), ChainParts.FLOPPY_FILM.get(), 4);
        final ProcessingKind laminator = kind("laminator");
        made(output, laminator, "fr2_laminate", List.of(Items.PAPER, of(SeriesCompounds.PHENOLIC_RESIN, Forms.PLATE),
                of(Element.COPPER, Forms.FOIL)), List.of(2, 1, 1), ChainParts.FR2_LAMINATE.get(), 1);
        made(output, laminator, "fr4_laminate", List.of(ChainParts.GLASS_FIBRE.get(), of(SeriesCompounds.EPOXY_RESIN,
                Forms.PLATE), of(Element.COPPER, Forms.FOIL)), List.of(2, 1, 1), ChainParts.FR4_LAMINATE.get(), 1);
        // A board gained layers with the eras: one laminate in the Vintage and the Legacy, then two, three, four.
        final int[] laminates = {1, 1, 2, 3, 4};
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            final ChainRecipeItems.Saved etched = recipe(kind("board_etcher"), "etched_board_"
                    + ChainParts.ERAS.get(i).path());
            etched.builder().input(i == 0 ? ChainParts.FR2_LAMINATE.get() : ChainParts.FR4_LAMINATE.get(),
                    laminates[i]).fluidInput(liquid(SeriesChemicals.FERRIC_CHLORIDE), 250)
                    .output(ChainParts.ETCHED_BOARDS.get(i).get(), 1);
            etched.save(output);
        }
        final ProcessingKind display = kind("display_line");
        made(output, display, "tube_screen", List.of(Items.GLASS, ingot(Element.LEAD),
                of(SeriesCompounds.ZINC_SULFIDE, Forms.DUST), ChainParts.COPPER_COIL.get()), List.of(4, 1, 1, 2),
                ChainParts.TUBE_SCREEN.get(), 1);
        final ChainRecipeItems.Saved lamp = recipe(display, "cold_cathode_lamp");
        lamp.builder().input(Items.GLASS, 1).fluidInput(liquid(Elements.of(Element.MERCURY)), 100)
                .output(ChainParts.COLD_CATHODE_LAMP.get(), 1);
        lamp.save(output);
        final Item ito = of(SeriesAlloys.INDIUM_TIN_OXIDE, Forms.FOIL);
        final ChainRecipeItems.Saved lcd = recipe(display, "lcd_screen");
        lcd.builder().input(Items.GLASS, 2).input(ito, 1).input(ChainParts.COLD_CATHODE_LAMP.get(), 1)
                .fluidInput(liquid(SeriesChemicals.LIQUID_CRYSTAL), 250).output(ChainParts.LCD_SCREEN.get(), 1);
        lcd.save(output);
        final ChainRecipeItems.Saved led = recipe(display, "led_screen");
        led.builder().input(Items.GLASS, 2).input(ito, 1).input(ChainParts.LED.get(), 8)
                .fluidInput(liquid(SeriesChemicals.LIQUID_CRYSTAL), 250).output(ChainParts.LED_SCREEN.get(), 1);
        led.save(output);
        made(output, display, "self_lit_panel", List.of(Items.GLASS, ito, ChainParts.LED.get()), List.of(2, 1, 16),
                ChainParts.SELF_LIT_PANEL.get(), 1);
        final ProcessingKind cell = kind("electrolytic_cell");
        final ChainRecipeItems.Saved leadAcid = recipe(cell, "lead_acid_battery");
        leadAcid.builder().input(ingot(Element.LEAD), 2).input(of(SeriesCompounds.PVC, Forms.SHEET), 1)
                .fluidInput(liquid(SeriesChemicals.SULFURIC_ACID), 250).output(ChainParts.LEAD_ACID_BATTERY.get(), 1);
        leadAcid.save(output);
        final ChainRecipeItems.Saved lithiumIon = recipe(cell, "lithium_ion_cell");
        lithiumIon.builder().input(of(SeriesCompounds.LITHIUM_COBALT_OXIDE, Forms.DUST), 1)
                .input(of(SeriesCompounds.COKE, Forms.LUMP), 1).input(of(Element.ALUMINIUM, Forms.FOIL), 2)
                .input(of(Element.COPPER, Forms.FOIL), 1).fluidInput(liquid(SeriesChemicals.BATTERY_ELECTROLYTE), 250)
                .output(ChainParts.LITHIUM_ION_CELL.get(), 1);
        lithiumIon.save(output);
    }

    /*
     * Silicon to wafers, and the crystals of light: polysilicon in the Siemens Reactor, a boule pulled from it with a
     * dopant, the light's boules and calcium fluoride's, each boule sawn into eight wafers.
     */
    private static void silicon(final RecipeOutput output) {
        final ChainRecipeItems.Saved poly = recipe(kind("siemens_reactor"), "polysilicon");
        poly.builder().input(of(SeriesCompounds.METALLURGICAL_SILICON, Forms.LUMP), 1)
                .matterInput(SeriesChemicals.HYDROGEN_CHLORIDE, BUCKET).matterInput(Elements.of(Element.HYDROGEN),
                        500).output(of(SeriesCompounds.POLYSILICON, Forms.ROD), 1);
        poly.save(output);
        final ProcessingKind puller = kind("crystal_puller");
        final Material silicon = Elements.of(Element.SILICON);
        for (final Element dopant : List.of(Element.PHOSPHORUS, Element.BORON)) {
            made(output, puller, "silicon_boule_" + dopant.name().toLowerCase(),
                    List.of(of(SeriesCompounds.POLYSILICON, Forms.ROD), dust(dopant)), List.of(4, 1),
                    of(silicon, Forms.BOULE), 1);
        }
        made(output, puller, "gallium_arsenide_boule", List.of(ingot(Element.GALLIUM), dust(Element.ARSENIC)),
                List.of(2, 2), of(SeriesCompounds.GALLIUM_ARSENIDE, Forms.BOULE), 1);
        made(output, puller, "algainp_boule", List.of(ingot(Element.ALUMINIUM), ingot(Element.GALLIUM),
                ingot(Element.INDIUM), dust(Element.PHOSPHORUS)), List.of(1, 1, 1, 1),
                of(SeriesCompounds.ALGAINP, Forms.BOULE), 1);
        final ChainRecipeItems.Saved nitride = recipe(puller, "gallium_nitride_boule");
        nitride.builder().input(ingot(Element.GALLIUM), 2).matterInput(Elements.of(Element.NITROGEN), BUCKET)
                .output(of(SeriesCompounds.GALLIUM_NITRIDE, Forms.BOULE), 1);
        nitride.save(output);
        made(output, puller, "calcium_fluoride_boule", List.of(of(SeriesMinerals.FLUORSPAR, Forms.DUST)),
                List.of(4), of(SeriesCompounds.CALCIUM_FLUORIDE, Forms.BOULE), 1);
        final ProcessingKind saw = kind("wafer_saw");
        for (final Material crystal : List.of(silicon, SeriesCompounds.GALLIUM_ARSENIDE, SeriesCompounds.ALGAINP,
                SeriesCompounds.GALLIUM_NITRIDE)) {
            made(output, saw, crystal.id().getPath() + "_wafer", List.of(of(crystal, Forms.BOULE)), List.of(1),
                    of(crystal, Forms.WAFER), 8);
        }
    }

    /* A machine's recipe of items in, one item out. */
    private static void made(final RecipeOutput output, final ProcessingKind kind, final String name,
                             final List<? extends ItemLike> inputs, final List<Integer> counts, final ItemLike result,
                             final int count) {
        final ChainRecipeItems.Saved made = recipe(kind, name);
        for (int i = 0; i < inputs.size(); i++) {
            made.builder().input(inputs.get(i), counts.get(i));
        }
        made.builder().output(result, count);
        made.save(output);
    }

    /* One form into another, when the chain uses both of {@code material}. */
    private static void step(final RecipeOutput output, final ProcessingKind kind, final String name,
                             final Material material, final Form from, final int taken, final Form into,
                             final int given) {
        if (!used(material, from) || !used(material, into)) {
            return;
        }
        final ChainRecipeItems.Saved step = recipe(kind, name);
        step.builder().input(of(material, from), taken).output(of(material, into), given);
        step.save(output);
    }

    /* The materials the chain uses in {@code form}, in the order they were declared. */
    private static Set<Material> used(final Form form) {
        final Set<Material> out = new LinkedHashSet<>();
        for (final MaterialUse use : MaterialUses.all()) {
            if (use.modid().equals(JsComputers.MODID) && use.form() == form) {
                out.add(use.material());
            }
        }
        return out;
    }

    /* Every material the chain uses in any form. */
    private static Set<Material> usedMaterials() {
        final Set<Material> out = new LinkedHashSet<>();
        for (final MaterialUse use : MaterialUses.all()) {
            if (use.modid().equals(JsComputers.MODID)) {
                out.add(use.material());
            }
        }
        return out;
    }

    private static boolean used(final Material material, final Form form) {
        return MaterialUses.used(material, form);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static Criterion<?> has(final ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    /* A byproduct's dust and its chance. */
    private record Byproduct(Element element, float chance) {
    }
}
