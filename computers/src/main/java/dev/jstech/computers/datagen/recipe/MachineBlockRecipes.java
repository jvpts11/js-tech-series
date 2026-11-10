/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import static dev.jstech.computers.datagen.recipe.BenchItems.aluminiumPlate;
import static dev.jstech.computers.datagen.recipe.BenchItems.board;
import static dev.jstech.computers.datagen.recipe.BenchItems.controller;
import static dev.jstech.computers.datagen.recipe.BenchItems.grid;
import static dev.jstech.computers.datagen.recipe.BenchItems.item;
import static dev.jstech.computers.datagen.recipe.BenchItems.made;
import static dev.jstech.computers.datagen.recipe.BenchItems.pvc;
import static dev.jstech.computers.datagen.recipe.BenchItems.rows;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelPlate;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.ingot;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.of;

import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The blocks of the chain's machines and the benches (computers/docs/design/recipes.md, "The benches" and "The blocks
 * of the machines"): the five casings by duty, the entries and the energy, the terminal and the peripheral port,
 * and each machine's working blocks, made of what the real one has. What is mechanical is made at the Crafting Table,
 * what is electronic at the bench of its era, by hand and by that era's Electronics Assembly Line.
 */
final class MachineBlockRecipes {

    private static final int CASINGS = 4;

    private MachineBlockRecipes() {
    }

    static void write(final RecipeOutput output) {
        benches(output);
        casings(output);
        entries(output);
        steps(output);
        chipFactory(output);
        assemblyLines(output);
    }

    /* The Vintage bench at the Crafting Table, and each next bench at the bench of the era before. */
    private static void benches(final RecipeOutput output) {
        grid(output, ChainBenches.VINTAGE.get(), 1, rows("OOO", "ITP", "ICI"), 'O', Items.OAK_PLANKS,
                'I', Items.IRON_INGOT, 'T', Items.CRAFTING_TABLE, 'P', Items.GLASS_PANE, 'C', Items.COPPER_INGOT);
        made(output, HardwareEra.VINTAGE, ChainBenches.LEGACY.get(), ChainBenches.VINTAGE.get(), 1, steelPlate(), 4,
                board(HardwareEra.LEGACY), 1, controller(HardwareEra.LEGACY), 1, ChainParts.TRANSFORMER.get(), 1,
                pvc(), 1);
        made(output, HardwareEra.LEGACY, ChainBenches.TRANSITION.get(), ChainBenches.LEGACY.get(), 1, steelPlate(),
                4, board(HardwareEra.TRANSITION), 1, controller(HardwareEra.TRANSITION), 1,
                ChainParts.ELECTRIC_MOTOR.get(), 1, ChainParts.LCD_SCREEN.get(), 1, pvc(), 1);
        made(output, HardwareEra.TRANSITION, ChainBenches.STANDARD.get(), ChainBenches.TRANSITION.get(), 1,
                aluminiumPlate(), 4, board(HardwareEra.STANDARD), 1, controller(HardwareEra.STANDARD), 1,
                of(SeriesCompounds.CALCIUM_FLUORIDE, Forms.LENS), 2, pvc(), 1);
        made(output, HardwareEra.STANDARD, ChainBenches.ADVANCED.get(), ChainBenches.STANDARD.get(), 1,
                aluminiumPlate(), 4, board(HardwareEra.ADVANCED), 1, controller(HardwareEra.ADVANCED), 1,
                ChainParts.LED.get(), 8, ChainParts.SELF_LIT_PANEL.get(), 1);
    }

    /* The five casings, each for the work it stands, four a recipe. */
    private static void casings(final RecipeOutput output) {
        grid(output, block("machine_casing"), CASINGS, rows("ZIZ", "I I", "ZIZ"), 'Z', ingot(Element.ZINC),
                'I', Items.IRON_INGOT);
        grid(output, block("refractory_casing"), CASINGS, rows("BIB", "IBI", "BIB"), 'B', Items.BRICK,
                'I', Items.IRON_INGOT);
        grid(output, block("lined_casing"), CASINGS, rows("IGI", "G G", "IGI"), 'I', Items.IRON_INGOT,
                'G', Items.GLASS);
        grid(output, block("vacuum_casing"), CASINGS, rows("CSC", "S S", "CSC"), 'C', Items.COPPER_INGOT,
                'S', of(SeriesAlloys.STAINLESS_STEEL, Forms.PLATE));
        grid(output, block("clean_room_casing"), CASINGS, rows("AFA", "F F", "AFA"), 'A', aluminiumPlate(),
                'F', ChainParts.GLASS_FIBRE.get());
    }

    /* The entries and the energy, the same for every machine: a mouth up takes in, a mouth down gives out. */
    private static void entries(final RecipeOutput output) {
        grid(output, block("item_inlet"), 2, rows("I I", "I I", " I "), 'I', Items.IRON_INGOT);
        grid(output, block("item_outlet"), 2, rows(" I ", "I I", "I I"), 'I', Items.IRON_INGOT);
        grid(output, block("liquid_inlet"), 2, rows("C C", "C C", " C "), 'C', Items.COPPER_INGOT);
        grid(output, block("liquid_outlet"), 2, rows(" C ", "C C", "C C"), 'C', Items.COPPER_INGOT);
        grid(output, block("gas_inlet"), 1, rows("I", "L"), 'I', Items.IRON_INGOT, 'L', block("liquid_inlet"));
        grid(output, block("gas_outlet"), 1, rows("I", "L"), 'I', Items.IRON_INGOT, 'L', block("liquid_outlet"));
        grid(output, block("power_entry"), 1, rows(" C ", "CIC", " C "), 'C', Items.COPPER_INGOT,
                'I', Items.IRON_INGOT);
        made(output, HardwareEra.VINTAGE, ChainMachineBlocks.TERMINAL.get(), Items.IRON_INGOT, 2,
                Items.COPPER_INGOT, 2, Items.REDSTONE, 2, Items.GLASS, 1);
        made(output, HardwareEra.VINTAGE, block("peripheral_port"), Items.IRON_INGOT, 2,
                item("peripheral_cable"), 1, board(HardwareEra.VINTAGE), 1, controller(HardwareEra.VINTAGE), 1);
    }

    /* The working blocks of the Vintage machines' first two steps, from vanilla things and from the first metals. */
    private static void steps(final RecipeOutput output) {
        final Item mc = block("machine_casing").asItem();
        final Item rc = block("refractory_casing").asItem();
        final Item lc = block("lined_casing").asItem();
        final Item motor = ChainParts.ELECTRIC_MOTOR.get();
        grid(output, block("crusher_roll"), 1, rows("NIN", "ICI", "NIN"), 'N', ingot(Element.MANGANESE),
                'I', Items.IRON_INGOT, 'C', mc);
        grid(output, block("crusher_drive"), 1, rows(" C ", "BMB"), 'C', mc, 'B', Items.IRON_BLOCK, 'M', motor);
        grid(output, block("grinder_drum"), 1, rows("INI", "NCN", "INI"), 'I', Items.IRON_INGOT,
                'N', Items.IRON_NUGGET, 'C', mc);
        grid(output, block("grinder_drive"), 1, rows(" C ", " M ", "III"), 'C', mc, 'M', motor, 'I', Items.IRON_INGOT);
        grid(output, block("induction_furnace_coil"), 1, rows("UUU", "URU", "UUU"), 'U', Items.COPPER_INGOT,
                'R', rc);
        made(output, HardwareEra.VINTAGE, block("induction_furnace_capacitor_bank"), mc, 1, Items.COPPER_INGOT, 4,
                ingot(Element.TIN), 4);
        grid(output, block("electrolytic_cell_cell_stack"), 1, rows("UPU", "PLP", "UPU"), 'U', Items.COPPER_INGOT,
                'P', ingot(Element.LEAD), 'L', lc);
        grid(output, block("rolling_mill_mill_housing"), 1, rows("III", "ICI", " M "), 'I', Items.IRON_INGOT,
                'C', mc, 'M', motor);
        grid(output, block("rolling_mill_work_roll"), 1, rows("NIN", "ICI", "NIN"), 'N', ingot(Element.NICKEL),
                'I', Items.IRON_INGOT, 'C', mc);
        grid(output, block("rolling_mill_roller_table"), 1, rows("III", " C ", "III"), 'I', Items.IRON_INGOT,
                'C', mc);
        grid(output, block("wire_drawer_capstan"), 1, rows("D", "C", "M"), 'D', Items.DIAMOND, 'C', mc, 'M', motor);
        grid(output, block("coke_oven_oven_chamber"), 1, rows("BBB", "BRB", "BBB"), 'B', Items.BRICK, 'R', rc);
        grid(output, block("coke_oven_oven_door"), 1, rows(" I ", "IRI", " I "), 'I', Items.IRON_INGOT, 'R', rc);
        grid(output, block("coke_oven_standpipe"), 1, rows("I", "I", "R"), 'I', Items.IRON_INGOT, 'R', rc);
        grid(output, block("chemical_reactor_vessel"), 1, rows("IGI", "GLG", "IGI"), 'I', Items.IRON_INGOT,
                'G', Items.GLASS, 'L', lc);
        grid(output, block("chemical_reactor_agitator"), 1, rows("M", "L", "I"), 'M', motor, 'L', lc,
                'I', Items.IRON_INGOT);
        final Item rod = of(SeriesAlloys.STEEL, Forms.ROD);
        final Item wire = of(Element.COPPER, Forms.WIRE);
        final Item plate = steelPlate();
        grid(output, block("coil_winder_winding_head"), 1, rows("MCR"), 'M', motor, 'C', mc, 'R', rod);
        grid(output, block("coil_winder_supply_spool"), 1, rows("WRW", "WCW", " R "), 'W', wire, 'R', rod, 'C', mc);
        grid(output, block("ceramic_kiln_kiln_chamber"), 1, rows("AWA", "WRW", "AWA"),
                'A', of(SeriesCompounds.ALUMINA, Forms.DUST), 'W', wire, 'R', rc);
        grid(output, block("ceramic_kiln_kiln_car"), 1, rows(" R ", "III", "I I"), 'R', rc, 'I', Items.IRON_INGOT);
        grid(output, block("laminator_hot_platen"), 1, rows("PPP", "WCW", " P "), 'P', plate, 'W', wire, 'C', mc);
        grid(output, block("laminator_press_crown"), 1, rows("PMP", "PCP"), 'P', plate, 'M', motor, 'C', mc);
        grid(output, block("board_etcher_spray_tunnel"), 1, rows("VVV", " L ", " M "), 'V', pvc(), 'L', lc,
                'M', motor);
        grid(output, block("board_etcher_etchant_sump"), 1, rows(" L ", "V V", "VVV"), 'L', lc, 'V', pvc());
        grid(output, block("siemens_reactor_bell"), 1, rows("PGP", "PRP"), 'P', plate, 'G', Items.GLASS, 'R', rc);
        grid(output, block("siemens_reactor_base_plate"), 1, rows("WTW", "WRW"), 'W', wire,
                'T', ChainParts.TRANSFORMER.get(), 'R', rc);
        grid(output, block("crystal_puller_hot_zone"), 1, rows("KGK", "KRK"), 'K', of(SeriesCompounds.COKE,
                Forms.LUMP), 'G', Items.GLASS, 'R', rc);
        grid(output, block("crystal_puller_pull_chamber"), 1, rows("M", "S", "R"), 'M', motor, 'S', rod, 'R', rc);
        grid(output, block("wafer_saw_wire_spool"), 1, rows("SMS", " C "), 'S', rod, 'M', motor, 'C', mc);
        grid(output, block("wafer_saw_wire_web"), 1, rows("SDS", " C "), 'S', rod, 'D', Items.DIAMOND, 'C', mc);
    }

    /* The working blocks of the chip factory, the Vintage machines' third step. */
    private static void chipFactory(final RecipeOutput output) {
        final Item mc = block("machine_casing").asItem();
        final Item vc = block("vacuum_casing").asItem();
        final Item cc = block("clean_room_casing").asItem();
        final Item motor = ChainParts.ELECTRIC_MOTOR.get();
        final Item coil = ChainParts.COPPER_COIL.get();
        final Item transformer = ChainParts.TRANSFORMER.get();
        final Item alumina = of(SeriesCompounds.ALUMINA, Forms.DUST);
        final Item plate = steelPlate();
        final Item stainless = of(SeriesAlloys.STAINLESS_STEEL, Forms.PLATE);
        final Item glassLens = of(SeriesCompounds.GLASS, Forms.LENS);
        final Item fluorideLens = of(SeriesCompounds.CALCIUM_FLUORIDE, Forms.LENS);
        final Item molybdenum = of(Element.MOLYBDENUM, Forms.PLATE);
        final Item siliconWafer = of(Element.SILICON, Forms.WAFER);
        grid(output, block("optical_polisher_polishing_lap"), 1, rows("A A", " C ", " M "), 'A', alumina, 'C', mc,
                'M', motor);
        grid(output, block("optical_polisher_draw_tower"), 1, rows("O", "C", "M"), 'O', coil, 'C', mc, 'M', motor);
        grid(output, block("air_separation_unit_compressor"), 1, rows("PMP", "PVP", " M "), 'P', plate, 'M', motor,
                'V', vc);
        grid(output, block("air_separation_unit_adsorber"), 1, rows("PAP", " V ", " A "), 'P', plate, 'A', alumina,
                'V', vc);
        grid(output, block("air_separation_unit_column"), 1, rows(" A ", "AVA", " A "), 'A', aluminiumPlate(),
                'V', vc);
        grid(output, block("air_separation_unit_vaporizer"), 1, rows("S S", "SVS"), 'S', of(Element.ALUMINIUM,
                Forms.SHEET), 'V', vc);
        grid(output, block("display_line_fill_chamber"), 1, rows("GGG", "GV ", " M "), 'G', Items.GLASS, 'V', vc,
                'M', motor);
        grid(output, block("display_line_gantry"), 1, rows("MSM", " V "), 'M', motor, 'S',
                of(SeriesAlloys.STEEL, Forms.ROD), 'V', vc);
        made(output, HardwareEra.VINTAGE, block("display_line_test_stand"), vc, 1, board(HardwareEra.VINTAGE), 1,
                ChainParts.TUBE_SCREEN.get(), 1);
        grid(output, block("lithography_wafer_stage"), 1, rows(" M ", "GCG", "G G"), 'M', motor,
                'G', Items.POLISHED_GRANITE, 'C', cc);
        grid(output, block("lithography_glass_optics"), 1, rows(" L ", "LCL", " L "), 'L', glassLens, 'C', cc);
        grid(output, block("lithography_fluoride_optics"), 1, rows(" L ", "LCL", " L "), 'L', fluorideLens,
                'C', cc);
        grid(output, block("lithography_mirror_optics"), 1, rows("OSO", "OCO", " S "), 'O', molybdenum,
                'S', siliconWafer, 'C', cc);
        grid(output, block("lithography_mercury_lamp"), 1, rows("GHG", " C ", " T "), 'G', Items.GLASS,
                'H', mercuryBucket(), 'C', cc, 'T', transformer);
        made(output, HardwareEra.LEGACY, block("lithography_krf_laser"), cc, 1, transformer, 2, glassLens, 2,
                board(HardwareEra.VINTAGE), 1);
        made(output, HardwareEra.TRANSITION, block("lithography_arf_laser"), cc, 1, transformer, 2, fluorideLens, 2,
                board(HardwareEra.LEGACY), 1);
        made(output, HardwareEra.ADVANCED, block("lithography_euv_source"), cc, 1, transformer, 2, molybdenum, 2,
                ingot(Element.TIN), 1, board(HardwareEra.STANDARD), 1);
        grid(output, block("plasma_etcher_etch_chamber"), 1, rows("GOG", " V ", " T "), 'G', Items.GLASS, 'O', coil,
                'V', vc, 'T', transformer);
        grid(output, block("ion_implanter_analyzer_magnet"), 1, rows("IOI", "OVO", "IOI"), 'I', Items.IRON_INGOT,
                'O', coil, 'V', vc);
        grid(output, block("ion_implanter_end_station"), 1, rows("SMS", " V "), 'S', stainless, 'M', motor, 'V', vc);
        grid(output, block("deposition_chamber_sputter_chamber"), 1, rows("SOS", " V ", " T "), 'S', stainless,
                'O', coil, 'V', vc, 'T', transformer);
        made(output, HardwareEra.VINTAGE, block("packaging_line_wire_bonder"), mc, 1, motor, 1,
                board(HardwareEra.VINTAGE), 1, ChainParts.GOLD_BONDING_WIRE.get(), 1);
        grid(output, block("packaging_line_mould_press"), 1, rows(" M ", "PCP", "PPP"), 'M', motor, 'P', plate,
                'C', mc);
        grid(output, block("prebuilder_case_lift"), 1, rows("R R", " C ", " M "), 'R', of(SeriesAlloys.STEEL,
                Forms.ROD), 'C', mc, 'M', motor);
        grid(output, block("prebuilder_parts_tray"), 1, rows("PPP", " C "), 'P', plate, 'C', mc);
        made(output, HardwareEra.VINTAGE, block("prebuilder_robot_arm"), mc, 1, motor, 2,
                board(HardwareEra.VINTAGE), 1);
        grid(output, block("preinstaller_cradle"), 1, rows("P P", " C "), 'P', plate, 'C', mc);
        grid(output, block("preinstaller_board_fixture"), 1, rows("G G", "PCP"), 'G', Items.GOLD_NUGGET, 'P', plate,
                'C', mc);
        made(output, HardwareEra.VINTAGE, block("preinstaller_console"), mc, 1, ChainParts.TUBE_SCREEN.get(), 1,
                board(HardwareEra.VINTAGE), 1);
        made(output, HardwareEra.VINTAGE, block("preinstaller_disk_dock"), mc, 1, board(HardwareEra.VINTAGE), 1,
                of(Element.COPPER, Forms.WIRE), 4);
    }

    /*
     * The Electronics Assembly Lines: the feeder bank of every era and the Vintage's and Legacy's wave solder at the
     * Crafting Table; each era's placement head at its bench, the Vintage's with its lens and each later one with the
     * era before's etched board and controller chip, its camera; each reflow oven from the Transition at its era's
     * bench with the era before's etched board.
     */
    private static void assemblyLines(final RecipeOutput output) {
        final Item mc = block("machine_casing").asItem();
        final Item motor = ChainParts.ELECTRIC_MOTOR.get();
        grid(output, block("assembly_feeder_bank"), 1, rows("PPP", "MCM"), 'P', steelPlate(), 'M', motor, 'C', mc);
        grid(output, block("assembly_wave_solder"), 1, rows("TTT", " C ", "TMT"), 'T', ingot(Element.TIN), 'C', mc,
                'M', motor);
        made(output, HardwareEra.VINTAGE, block("assembly_vintage_placement_head"), mc, 1, motor, 2,
                board(HardwareEra.VINTAGE), 1, of(SeriesCompounds.GLASS, Forms.LENS), 1);
        for (int i = 1; i < ChainParts.ERAS.size(); i++) {
            final Era era = ChainParts.ERAS.get(i);
            final Era before = ChainParts.ERAS.get(i - 1);
            made(output, era, block("assembly_" + era.path() + "_placement_head"), mc, 1, motor, 2, board(before), 1,
                    controller(before), 1);
            if (!era.isAtMost(HardwareEra.LEGACY)) {
                made(output, era, block("assembly_" + era.path() + "_reflow_oven"), mc, 1,
                        of(Element.COPPER, Forms.WIRE), 4, motor, 1, board(before), 1);
            }
        }
    }

    private static ItemLike block(final String id) {
        return ChainMachineBlocks.byId(id).get();
    }

    private static Item mercuryBucket() {
        return ChainRecipeItems.liquid(Elements.of(Element.MERCURY)).getBucket();
    }
}
