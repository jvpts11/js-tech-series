/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import static dev.jstech.computers.datagen.recipe.BenchItems.aluminiumPlate;
import static dev.jstech.computers.datagen.recipe.BenchItems.aluminiumSheet;
import static dev.jstech.computers.datagen.recipe.BenchItems.board;
import static dev.jstech.computers.datagen.recipe.BenchItems.caseFan;
import static dev.jstech.computers.datagen.recipe.BenchItems.controller;
import static dev.jstech.computers.datagen.recipe.BenchItems.copperWire;
import static dev.jstech.computers.datagen.recipe.BenchItems.coreItem;
import static dev.jstech.computers.datagen.recipe.BenchItems.grid;
import static dev.jstech.computers.datagen.recipe.BenchItems.item;
import static dev.jstech.computers.datagen.recipe.BenchItems.made;
import static dev.jstech.computers.datagen.recipe.BenchItems.ofEra;
import static dev.jstech.computers.datagen.recipe.BenchItems.polycarbonate;
import static dev.jstech.computers.datagen.recipe.BenchItems.pvc;
import static dev.jstech.computers.datagen.recipe.BenchItems.rows;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelPlate;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelRod;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelSheet;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.of;

import dev.jstech.computers.CoolingItems;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/**
 * Power and cooling (computers/docs/design/recipes.md, "Power and cooling"), each at the bench of its era: the case
 * fans and the coolers, the UPSes, and the power and cooling rack units. The Vent Panel, with no electronics in it, and
 * the Guide to Operations, a binder, are made at the Crafting Table.
 */
final class PowerRecipes {

    private PowerRecipes() {
    }

    static void write(final RecipeOutput output) {
        cooling(output);
        upses(output);
        rackUnits(output);
        table(output);
    }

    /*
     * The case fan of each era (80, 80, 92, 120 and 120 mm) is a motor and its blades, PVC in the Vintage and
     * polycarbonate after; the bigger fan takes more plastic, and the Advanced's carries its light ring (an LED). The
     * stock cooler is an aluminium plate in the Vintage, with a fan and thermal paste from the Legacy and a copper rod
     * from the Transition; the tower cooler 4 aluminium sheets, 2 copper rods (the heat pipes), a fan and thermal
     * paste; the all-in-one MF AquaLoop a copper plate, the pump (a motor), the radiator, 2 or 3 fans, the hose (PVC),
     * its water (a water bottle, about the 250 mB it holds) and thermal paste.
     */
    private static void cooling(final RecipeOutput output) {
        final Item motor = ChainParts.ELECTRIC_MOTOR.get();
        final Item paste = ChainParts.THERMAL_PASTE.get();
        final Item copperRod = of(Element.COPPER, Forms.ROD);
        made(output, HardwareEra.VINTAGE, caseFan(HardwareEra.VINTAGE), motor, 1, pvc(), 1);
        made(output, HardwareEra.LEGACY, caseFan(HardwareEra.LEGACY), motor, 1, polycarbonate(), 1);
        made(output, HardwareEra.TRANSITION, caseFan(HardwareEra.TRANSITION), motor, 1, polycarbonate(), 2);
        made(output, HardwareEra.STANDARD, caseFan(HardwareEra.STANDARD), motor, 1, polycarbonate(), 3);
        made(output, HardwareEra.ADVANCED, caseFan(HardwareEra.ADVANCED), motor, 1, polycarbonate(), 3,
                ChainParts.LED.get(), 1);
        made(output, HardwareEra.VINTAGE, CoolingItems.VINTAGE_STOCK_COOLER.get(), aluminiumPlate(), 1);
        made(output, HardwareEra.LEGACY, CoolingItems.LEGACY_STOCK_COOLER.get(), aluminiumPlate(), 1,
                caseFan(HardwareEra.LEGACY), 1, paste, 1);
        made(output, HardwareEra.TRANSITION, CoolingItems.TRANSITION_STOCK_COOLER.get(), aluminiumPlate(), 1,
                caseFan(HardwareEra.TRANSITION), 1, paste, 1, copperRod, 1);
        made(output, HardwareEra.STANDARD, CoolingItems.STANDARD_STOCK_COOLER.get(), aluminiumPlate(), 1,
                caseFan(HardwareEra.STANDARD), 1, paste, 1, copperRod, 1);
        made(output, HardwareEra.ADVANCED, CoolingItems.ADVANCED_STOCK_COOLER.get(), aluminiumPlate(), 1,
                caseFan(HardwareEra.ADVANCED), 1, paste, 1, copperRod, 1);
        final Ingredient water = DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS,
                new PotionContents(Potions.WATER), Items.POTION);
        final Item copperPlate = of(Element.COPPER, Forms.PLATE);
        for (final Era era : List.of(HardwareEra.STANDARD, HardwareEra.ADVANCED)) {
            final boolean standard = era == HardwareEra.STANDARD;
            made(output, era, (standard ? CoolingItems.STANDARD_TOWER_COOLER : CoolingItems.ADVANCED_TOWER_COOLER)
                    .get(), aluminiumSheet(), 4, copperRod, 2, caseFan(era), 1, paste, 1);
            made(output, era, (standard ? CoolingItems.STANDARD_AQUALOOP_240 : CoolingItems.ADVANCED_AQUALOOP_240)
                    .get(), copperPlate, 1, motor, 1, aluminiumSheet(), 4, caseFan(era), 2, pvc(), 1, water, 1, paste,
                    1);
            made(output, era, (standard ? CoolingItems.STANDARD_AQUALOOP_360 : CoolingItems.ADVANCED_AQUALOOP_360)
                    .get(), copperPlate, 1, motor, 1, aluminiumSheet(), 6, caseFan(era), 3, pvc(), 1, water, 1, paste,
                    1);
        }
    }

    /*
     * A UPS is what a real one is: its steel box (3 plates for the 1500), its board and controller (the charger, the
     * screen and the button), its inverter, built as a power supply's (a transformer for every 300 W it carries), its
     * lead-acid batteries (one 12 V, 7 Ah block, about 300 kJ, for every 300 kJ it holds) and its outlets and inlet.
     */
    private static void upses(final RecipeOutput output) {
        ups(output, HardwareEra.VINTAGE, 2, 1, 1);
        ups(output, HardwareEra.LEGACY, 2, 1, 1);
        ups(output, HardwareEra.TRANSITION, 2, 2, 1);
        ups(output, HardwareEra.STANDARD, 2, 3, 2);
        ups(output, HardwareEra.ADVANCED, 3, 5, 5);
    }

    private static void ups(final RecipeOutput output, final Era era, final int plates, final int transformers,
                            final int batteries) {
        made(output, era, item(era.path() + "_ups"), steelPlate(), plates, board(era), 1, controller(era), 1,
                ChainParts.TRANSFORMER.get(), transformers, ChainParts.LEAD_ACID_BATTERY.get(), batteries,
                copperWire(), 4);
    }

    /*
     * Each is 2 steel plates for its 1U and what it needs: the Rack UPS a UPS's parts for 3 kW and 2.7 MJ; a UPS
     * Battery Expansion its 9 batteries, the controller that watches them and its link to the UPS; the PDU its metering
     * board, a current transformer and 8 outlets; the Cooling Unit an air conditioner, its compressor (a motor), its
     * loop (2 copper pipes) and 2 fans.
     */
    private static void rackUnits(final RecipeOutput output) {
        final Item transformer = ChainParts.TRANSFORMER.get();
        final Item battery = ChainParts.LEAD_ACID_BATTERY.get();
        for (final Era era : ChainParts.ERAS) {
            made(output, era, ofEra(era, "rack_ups"), steelPlate(), 2, board(era), 1, controller(era), 1, transformer,
                    10, battery, 9, copperWire(), 4);
            made(output, era, ofEra(era, "ups_battery_expansion"), steelPlate(), 2, battery, 9, controller(era), 1,
                    copperWire(), 2);
            made(output, era, ofEra(era, "pdu"), steelPlate(), 2, board(era), 1, controller(era), 1, transformer, 1,
                    copperWire(), 8);
            made(output, era, ofEra(era, "cooling_unit"), steelPlate(), 2, board(era), 1, controller(era), 1,
                    caseFan(era), 2, ChainParts.ELECTRIC_MOTOR.get(), 1, coreItem("copper_pipe"), 2);
        }
    }

    /*
     * The Vent Panel, one for every era, taking the colour of the rack it is in: the 1U's ears (steel plates) and the
     * perforated face (a steel sheet). The Guide to Operations is a binder, as the first computers' manuals were: the
     * PVC covers, the rings (a steel rod) and the pages.
     */
    private static void table(final RecipeOutput output) {
        grid(output, item("vent_panel"), 1, rows("PSP"), 'P', steelPlate(), 'S', steelSheet());
        grid(output, item("guide_to_operations"), 1, rows("VP", "RP", "VP"), 'V', pvc(), 'P', Items.PAPER,
                'R', steelRod());
    }
}
