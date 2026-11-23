/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import com.google.gson.JsonPrimitive;
import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.condition.SettingCondition;
import dev.jstech.core.config.CoreConfigKeys;
import dev.jstech.core.item.SwitchedShapedRecipe;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.jstechseries.energy.EnergyConverters;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.lines.SharedPipes;
import dev.jstech.core.jstechseries.tools.PhysicsTool;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.core.jstechseries.upgrade.SharedUpgrades;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.multipart.MicroblockRecipes;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.AndCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import org.jetbrains.annotations.Nullable;

/**
 * The recipes of the series' shared content, of the game's own materials, so a mod used alone can make it. The basic
 * cable, pipes and battery are J's Industrial's first cable, pipes and energy bank when it is installed, and it may
 * give them its own industrial recipes: these stand unless J's Industrial is installed with its setting that hands
 * them to its own recipes on, which is off as it comes. The wrench's and the converters' stand always.
 */
public final class SeriesRecipes extends RecipeProvider {

    /** The mod whose own recipes may replace those of the basic cable and battery. */
    public static final String INDUSTRIAL = "jsindustrial";
    /* J's Industrial's settings file, and its switch that hands the basic lines to its own recipes. */
    private static final String INDUSTRIAL_SETTINGS = "jsindustrial-server";
    private static final String INDUSTRIAL_BASIC_LINES = "recipes.industrial_basic_lines";
    private static final int CABLES_A_CRAFT = 6;
    /* The steel plates any mod files under the common tag, which decide which of the cylinder's recipes stands. */
    private static final TagKey<Item> STEEL_PLATES = Forms.PLATE.itemTag(SeriesAlloys.STEEL);
    /* How long the furnace and the blast furnace take, and the experience they give, as for the game's raw iron. */
    private static final int FURNACE_TICKS = 200;
    private static final int BLAST_TICKS = 100;
    private static final float SMELTING_XP = 0.7F;

    public SeriesRecipes(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(final RecipeOutput output) {
        // Gone only where J's Industrial is installed and hands them to its own recipes; the setting is read only then.
        final RecipeOutput alone = output.withConditions(new NotCondition(new AndCondition(List.of(
                new ModLoadedCondition(INDUSTRIAL), new SettingCondition(INDUSTRIAL_SETTINGS, INDUSTRIAL_BASIC_LINES,
                        new JsonPrimitive(true))))));
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
        // An iron box with the gas pipe's inlet, and a grate on top where the gas goes out.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, SharedPipes.GAS_VENT.get())
                .pattern("IBI").pattern("IPI").pattern("III")
                .define('I', Items.IRON_INGOT).define('B', Items.IRON_BARS).define('P', SharedPipes.IRON_GAS_PIPE)
                .unlockedBy("has_iron_gas_pipe", has(SharedPipes.IRON_GAS_PIPE)).save(output, id("gas_vent"));
        tinAndBronze(output);
        upgrades(output);
        // A cylinder of steel plates, its bronze valve on top; of iron ingots where no mod makes steel plates.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GasCylinders.CYLINDER.get())
                .pattern("PBP").pattern("P P").pattern("PPP")
                .define('P', STEEL_PLATES).define('B', Forms.INGOT.itemTag(SeriesMaterials.BRONZE))
                .unlockedBy("has_steel_plate", has(STEEL_PLATES))
                .save(output.withConditions(new NotCondition(new TagEmptyCondition(STEEL_PLATES.location()))),
                        id("gas_cylinder"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, GasCylinders.CYLINDER.get())
                .pattern("PBP").pattern("P P").pattern("PPP")
                .define('P', Items.IRON_INGOT).define('B', Forms.INGOT.itemTag(SeriesMaterials.BRONZE))
                .unlockedBy("has_bronze_ingot", has(SeriesBronze.BRONZE_INGOT.get()))
                .save(output.withConditions(new TagEmptyCondition(STEEL_PLATES.location())),
                        id("gas_cylinder_from_iron"));
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
        // An iron body round a lodestone, the field it holds with, an amethyst at its muzzle and redstone in its grip;
        // made only while its setting says so.
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, PhysicsTool.ITEM.get())
                .pattern(" IA").pattern("ILI").pattern("RI ")
                .define('I', Items.IRON_INGOT).define('L', Items.LODESTONE).define('A', Items.AMETHYST_SHARD)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_lodestone", has(Items.LODESTONE))
                .save(switched(output, CoreConfigKeys.FILE.name(), CoreConfigKeys.MOVING_PHYSICS_TOOL_RECIPE
                        .dottedPath()), id("physics_tool"));
        converters(output);
    }

    /* Where a shaped recipe goes made one a setting turns on and off. */
    /* Tin's raw lumps and ore smelted as the game's raw iron is, and bronze alloyed by hand: 3 copper and 1 tin. */
    private static void tinAndBronze(final RecipeOutput output) {
        final Item tin = SeriesBronze.TIN_INGOT.get();
        for (final Form from : List.of(Forms.RAW, Forms.ORE)) {
            final Ingredient ingredient = Ingredient.of(from.itemTag(SeriesMaterials.TIN));
            final String name = "tin_ingot_from_" + from.id().getPath();
            SimpleCookingRecipeBuilder.smelting(ingredient, RecipeCategory.MISC, tin, SMELTING_XP, FURNACE_TICKS)
                    .unlockedBy("has_it", has(from.itemTag(SeriesMaterials.TIN)))
                    .save(output, id("smelting/" + name));
            SimpleCookingRecipeBuilder.blasting(ingredient, RecipeCategory.MISC, tin, SMELTING_XP, BLAST_TICKS)
                    .unlockedBy("has_it", has(from.itemTag(SeriesMaterials.TIN)))
                    .save(output, id("blasting/" + name));
        }
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SeriesBronze.BRONZE_INGOT.get(), SeriesBronze.ALLOYED)
                .requires(Ingredient.of(Tags.Items.INGOTS_COPPER), SeriesBronze.COPPER_INGOTS)
                .requires(Forms.INGOT.itemTag(SeriesMaterials.TIN))
                .unlockedBy("has_tin_ingot", has(tin)).save(output, id("bronze_ingot"));
    }

    /*
     * The machine upgrades, at the Crafting Table, of metals any mod's ingots give and the game's redstone, quartz,
     * glass and diamond, so whichever mod's machines take them can make them.
     */
    private static void upgrades(final RecipeOutput output) {
        final TagKey<Item> iron = Tags.Items.INGOTS_IRON;
        final TagKey<Item> copper = Tags.Items.INGOTS_COPPER;
        final TagKey<Item> gold = Tags.Items.INGOTS_GOLD;
        final TagKey<Item> bronze = Forms.INGOT.itemTag(SeriesMaterials.BRONZE);
        final TagKey<Item> redstone = Tags.Items.DUSTS_REDSTONE;
        final TagKey<Item> glass = Tags.Items.GLASS_PANES;
        // A steel can round copper windings and the redstone of its commutator.
        upgrade(SharedUpgrades.BRUSHED_MOTOR.get(), "ICI", "CRC", "ICI")
                .define('I', iron).define('C', copper).define('R', redstone).save(output, id("brushed_motor"));
        // Copper windings all round, no brushes.
        upgrade(SharedUpgrades.INDUCTION_MOTOR.get(), "ICI", "CCC", "ICI")
                .define('I', iron).define('C', copper).save(output, id("induction_motor"));
        // The windings and their controller, its bell of gold.
        upgrade(SharedUpgrades.BRUSHLESS_MOTOR.get(), "ICI", "CRC", "GGG")
                .define('I', iron).define('C', copper).define('R', redstone).define('G', gold)
                .save(output, id("brushless_motor"));
        // A box of power electronics: its gold contacts, its redstone, its quartz clock and its copper heat sink.
        upgrade(SharedUpgrades.VARIABLE_FREQUENCY_DRIVE.get(), "IGI", "RQR", "ICI")
                .define('I', iron).define('G', gold).define('R', redstone).define('Q', Tags.Items.GEMS_QUARTZ)
                .define('C', copper).save(output, id("variable_frequency_drive"));
        // A slat conveyor: iron slats over bronze rollers and its redstone drive, on an iron frame.
        upgrade(SharedUpgrades.PARALLEL_LINE.get(), "NNN", "BRB", "III")
                .define('N', Tags.Items.NUGGETS_IRON).define('B', bronze).define('R', redstone).define('I', iron)
                .save(output, id("parallel_line"));
        // Two lines on a frame of gold, with a diamond's precision.
        upgrade(SharedUpgrades.ADVANCED_PARALLEL_LINE.get(), "GGG", "LDL", "GGG")
                .define('G', gold).define('L', SharedUpgrades.PARALLEL_LINE.get()).define('D', Tags.Items.GEMS_DIAMOND)
                .save(output, id("advanced_parallel_line"));
        // A receiver of iron with its sight glass, over a compressor of copper and redstone.
        upgrade(SharedUpgrades.GAS_RECOVERY_UNIT.get(), "IPI", "CRC", "III")
                .define('I', iron).define('P', glass).define('C', copper).define('R', redstone)
                .save(output, id("gas_recovery_unit"));
        // A glass condenser over a copper still.
        upgrade(SharedUpgrades.SOLVENT_RECOVERY_UNIT.get(), "PPP", "PCP", "CCC")
                .define('P', glass).define('C', copper).save(output, id("solvent_recovery_unit"));
        // A diamond edge, its gold coating, on an iron holder.
        upgrade(SharedUpgrades.PRECISION_TOOLING.get(), " D ", " G ", "III")
                .define('D', Tags.Items.GEMS_DIAMOND).define('G', gold).define('I', iron)
                .save(output, id("precision_tooling"));
        // An iron shell packed round with glass wool.
        upgrade(SharedUpgrades.MUFFLER.get(), "IPI", "IPI", "IPI")
                .define('I', iron).define('P', glass).save(output, id("muffler"));
        // A cyclone of iron, its bronze valve below.
        upgrade(SharedUpgrades.BYPRODUCT_SEPARATOR.get(), "III", "I I", " B ")
                .define('I', iron).define('B', bronze).save(output, id("byproduct_separator"));
    }

    /* An upgrade's shaped recipe of {@code rows}, unlocked by holding iron. */
    private static ShapedRecipeBuilder upgrade(final Item upgrade, final String... rows) {
        final ShapedRecipeBuilder recipe = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, upgrade);
        for (final String row : rows) {
            recipe.pattern(row);
        }
        return recipe.unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON));
    }

    private static RecipeOutput switched(final RecipeOutput output, final String file, final String setting) {
        return new RecipeOutput() {
            @Override
            public void accept(final ResourceLocation id, final Recipe<?> recipe,
                               @Nullable final AdvancementHolder advancement, final ICondition... conditions) {
                output.accept(id, new SwitchedShapedRecipe(file, setting, (ShapedRecipe) recipe), advancement,
                        conditions);
            }

            @Override
            public Advancement.Builder advancement() {
                return output.advancement();
            }
        };
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
