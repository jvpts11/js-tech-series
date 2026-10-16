/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.jstechseries.energy.EnergyConverters;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.core.multipart.MicroblockRecipes;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;

/**
 * The recipes of the series' shared content, of the game's own materials, so a mod used alone can make it. The basic
 * cable, pipes and battery are J's Industrial's first cable, pipes and energy bank when it is installed, and it gives
 * them its own industrial recipes, so these stand only without it; the wrench's and the converters' stand always.
 */
public final class SeriesRecipes extends RecipeProvider {

    /** The mod whose own recipes replace those of the basic cable and battery. */
    public static final String INDUSTRIAL = "jsindustrial";
    private static final int CABLES_A_CRAFT = 6;

    public SeriesRecipes(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(final RecipeOutput output) {
        final RecipeOutput alone = output.withConditions(new NotCondition(new ModLoadedCondition(INDUSTRIAL)));
        // Copper wire between two layers of kelp dried into a rubbery insulation.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, SharedEnergy.INSULATED_COPPER_CABLE, CABLES_A_CRAFT)
                .pattern("KKK").pattern("CCC").pattern("KKK")
                .define('K', Items.DRIED_KELP).define('C', Items.COPPER_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(alone, id("insulated_copper_cable"));
        // Plates of metal in water, in an iron case, with its copper terminals.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, SharedEnergy.BATTERY.get())
                .pattern("ICI").pattern("RWR").pattern("ICI")
                .define('I', Items.IRON_INGOT).define('C', Items.COPPER_INGOT).define('R', Items.REDSTONE)
                .define('W', Items.WATER_BUCKET)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
                .save(alone, id("lead_acid_battery_bank"));
        // Each pipe: its metal round a row of glass panes, the sight glasses.
        pipe(alone, SharedPipes.COPPER_PIPE, Items.COPPER_INGOT, Items.GLASS_PANE, "copper_pipe");
        pipe(alone, SharedPipes.IRON_GAS_PIPE, Items.IRON_INGOT, Items.GLASS_PANE, "iron_gas_pipe");
        // The cryogenic pipe's two walls hold a vacuum, sealed with obsidian.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, SharedPipes.VACUUM_JACKETED_PIPE, 4)
                .pattern("III").pattern("GOG").pattern("III")
                .define('I', Items.IRON_INGOT).define('G', Items.GLASS_PANE).define('O', Items.OBSIDIAN)
                .unlockedBy("has_obsidian", has(Items.OBSIDIAN)).save(alone, id("vacuum_jacketed_pipe"));
        // An open-ended spanner of iron, its jaws apart at the top.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, SharedTools.WRENCH.get())
                .pattern("I I").pattern(" I ").pattern(" I ")
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(output, id("basic_wrench"));
        // A card of paper with a gold contact and the redstone that remembers what it was shown.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, SharedTools.CARD.get())
                .requires(Items.PAPER).requires(Items.REDSTONE).requires(Items.GOLD_NUGGET)
                .unlockedBy("has_redstone", has(Items.REDSTONE)).save(output, id("configuration_card"));
        // A blade of iron on a wooden handle.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, SharedTools.SAW.get())
                .pattern("SII").define('S', Items.STICK).define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT)).save(output, id("saw"));
        // The saw's cuts, two pieces joined, and a cover joined with a cable: rules for every block of every mod.
        SpecialRecipeBuilder.special(MicroblockRecipes.SawCut::new).save(output, id("saw_cut"));
        SpecialRecipeBuilder.special(MicroblockRecipes.Join::new).save(output, id("microblock_join"));
        SpecialRecipeBuilder.special(MicroblockRecipes.Facade::new).save(output, id("facade"));
        converters(output);
    }

    /*
     * The converters, each class made from the one below it and dearer things, from the low class's iron, copper and
     * gold to the ultra-high class's netherite and end crystals; the configurable one from the ultra-high one, as it
     * can be set to any class.
     */
    private void converters(final RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EnergyConverters.LOW.get())
                .pattern("ICI").pattern("RGR").pattern("ICI")
                .define('I', Items.IRON_INGOT).define('C', Items.COPPER_INGOT).define('R', Items.REDSTONE)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT)).save(output, id("fe_converter_lv"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EnergyConverters.MEDIUM.get())
                .pattern("GQG").pattern("QXQ").pattern("GQG")
                .define('G', Items.GOLD_INGOT).define('Q', Items.QUARTZ).define('X', EnergyConverters.LOW.get())
                .unlockedBy("has_converter", has(EnergyConverters.LOW.get())).save(output, id("fe_converter_mv"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EnergyConverters.HIGH.get())
                .pattern("DBD").pattern("BXB").pattern("DBD")
                .define('D', Items.DIAMOND).define('B', Items.REDSTONE_BLOCK).define('X', EnergyConverters.MEDIUM.get())
                .unlockedBy("has_converter", has(EnergyConverters.MEDIUM.get())).save(output, id("fe_converter_hv"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EnergyConverters.EXTRA_HIGH.get())
                .pattern("EOE").pattern("OXO").pattern("EOE")
                .define('E', Items.EMERALD).define('O', Items.OBSIDIAN).define('X', EnergyConverters.HIGH.get())
                .unlockedBy("has_converter", has(EnergyConverters.HIGH.get())).save(output, id("fe_converter_ehv"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EnergyConverters.ULTRA_HIGH.get())
                .pattern(" N ").pattern("CXC").pattern(" N ")
                .define('N', Items.NETHERITE_INGOT).define('C', Items.END_CRYSTAL)
                .define('X', EnergyConverters.EXTRA_HIGH.get())
                .unlockedBy("has_converter", has(EnergyConverters.EXTRA_HIGH.get()))
                .save(output, id("fe_converter_uhv"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, EnergyConverters.CONFIGURABLE.get())
                .pattern(" C ").pattern("RXR").pattern(" C ")
                .define('C', Items.COMPARATOR).define('R', Items.REPEATER)
                .define('X', EnergyConverters.ULTRA_HIGH.get())
                .unlockedBy("has_converter", has(EnergyConverters.ULTRA_HIGH.get()))
                .save(output, id("configurable_energy_converter"));
    }

    private void pipe(final RecipeOutput output, final CableEntry pipe, final Item metal, final Item glass,
                      final String path) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, pipe, CABLES_A_CRAFT)
                .pattern("MMM").pattern("GGG").pattern("MMM")
                .define('M', metal).define('G', glass)
                .unlockedBy("has_metal", has(metal)).save(output, id(path));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
