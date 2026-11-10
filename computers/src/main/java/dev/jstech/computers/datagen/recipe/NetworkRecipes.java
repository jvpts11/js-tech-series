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
import static dev.jstech.computers.datagen.recipe.BenchItems.copperFoil;
import static dev.jstech.computers.datagen.recipe.BenchItems.copperWire;
import static dev.jstech.computers.datagen.recipe.BenchItems.index;
import static dev.jstech.computers.datagen.recipe.BenchItems.item;
import static dev.jstech.computers.datagen.recipe.BenchItems.join;
import static dev.jstech.computers.datagen.recipe.BenchItems.made;
import static dev.jstech.computers.datagen.recipe.BenchItems.memory;
import static dev.jstech.computers.datagen.recipe.BenchItems.ofEra;
import static dev.jstech.computers.datagen.recipe.BenchItems.part;
import static dev.jstech.computers.datagen.recipe.BenchItems.plastic;
import static dev.jstech.computers.datagen.recipe.BenchItems.pvc;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelPlate;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelRod;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.nugget;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.List;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The network (computers/docs/design/recipes.md, "The network"), each at the bench of its era: the cables, eight to a
 * recipe; the routers, the repeaters and the Server Routers; and the buses and blocks of storage and the crafting
 * network. The function cards are {@link CardRecipes}'.
 */
final class NetworkRecipes {

    private static final int CABLES = 8;
    /* Each era's router; the Legacy's is the Personal Router. */
    private static final List<String> ROUTERS = List.of("vintage_router", "personal_router", "transition_router",
            "standard_router", "advanced_router");
    /* The memory chips a Crafting Interface holds, one for every 3 patterns it keeps, by era from the Vintage. */
    private static final int[] INTERFACE_MEMORY = {1, 2, 3, 3, 4};

    private NetworkRecipes() {
    }

    static void write(final RecipeOutput output) {
        cables(output);
        routers(output);
        buses(output);
    }

    /*
     * Each cable is what the real one has: the coaxials' braid (copper foil), more copper in the later twisted pairs
     * (their twists are tighter, more copper to the metre), the shielded ones' foil, the long lines' armour (a steel
     * rod), the direct-attach copper of the OSFP and the gold of its plugs. The crafting cable carries redstone, the
     * line's signal; a peripheral cable takes its era's plugs, tin in the Vintage and gold after.
     */
    private static void cables(final RecipeOutput output) {
        final Era v = HardwareEra.VINTAGE;
        final Era l = HardwareEra.LEGACY;
        final Era t = HardwareEra.TRANSITION;
        final Era s = HardwareEra.STANDARD;
        final Era a = HardwareEra.ADVANCED;
        final Item wire = copperWire();
        final Item foil = copperFoil();
        final Item fibre = ChainParts.OPTICAL_FIBRE.get();
        final Item gold = Items.GOLD_NUGGET;
        cable(output, v, "thin_coax_cable", wire, 2, foil, 2, pvc(), 1);
        cable(output, v, "thick_coax_cable", wire, 4, foil, 4, pvc(), 2);
        cable(output, v, "telephone_line", wire, 2, pvc(), 1);
        cable(output, v, "crafting_cable", wire, 3, pvc(), 1, Items.REDSTONE, 1);
        cable(output, v, "vintage_peripheral_cable", wire, 4, pvc(), 1, nugget(Element.TIN), 1);
        cable(output, l, "ethernet_cable", wire, 4, pvc(), 1);
        cable(output, l, "hbw_cable", wire, 10, foil, 2, pvc(), 2);
        cable(output, l, "leased_line", wire, 4, pvc(), 1, foil, 1);
        cable(output, l, "legacy_peripheral_cable", wire, 4, pvc(), 1, gold, 1);
        cable(output, t, "cat5e_cable", wire, 5, pvc(), 1);
        cable(output, t, "cx4_cable", wire, 8, foil, 2, pvc(), 1);
        cable(output, t, "t3_line", wire, 1, foil, 2, pvc(), 1);
        cable(output, t, "infiniband_cable", wire, 8, foil, 4, pvc(), 1);
        cable(output, t, "transition_peripheral_cable", wire, 5, pvc(), 1, gold, 1);
        cable(output, s, "gigabit_cable", wire, 6, pvc(), 1);
        cable(output, s, "fibre_cable", fibre, 4, pvc(), 1);
        cable(output, s, "vldc_cable", fibre, 4, pvc(), 1, steelRod(), 1);
        cable(output, s, "hpc_cable", fibre, 8, pvc(), 1);
        cable(output, s, "peripheral_cable", wire, 6, pvc(), 1, gold, 1, foil, 1);
        cable(output, a, "cat6a_cable", wire, 6, pvc(), 1, foil, 1);
        cable(output, a, "om5_cable", fibre, 6, pvc(), 1);
        cable(output, a, "dark_fibre_cable", fibre, 8, pvc(), 1, steelRod(), 1);
        cable(output, a, "osfp_cable", wire, 16, foil, 8, pvc(), 2, gold, 1);
        cable(output, a, "advanced_peripheral_cable", wire, 6, pvc(), 1, gold, 2);
    }

    private static void cable(final RecipeOutput output, final Era era, final String id,
                              final Object... ingredients) {
        made(output, era, id, item(id), CABLES, ingredients);
    }

    /*
     * A router is its box, its board, 2 controller chips, a memory chip (the routing table) and its ports (copper
     * wires); an optical router is of aluminium, with the fibre's lasers and the fibre; a repeater one controller and
     * two ports. A Server Router is a steel box with its board, the table's memory, its six faces' ports and one
     * controller for every 4 racks it serves (4 in the Legacy, 8 in the Transition, 16 in the Standard, 32 in the
     * Advanced).
     */
    private static void routers(final RecipeOutput output) {
        for (final Era era : ChainParts.ERAS) {
            made(output, era, item(ROUTERS.get(index(era))), plastic(era), 1, board(era), 1, controller(era), 2,
                    memory(era), 1, copperWire(), 4);
            made(output, era, item(era.path() + "_repeater"), plastic(era), 1, board(era), 1, controller(era), 1,
                    copperWire(), 2);
        }
        for (final Era era : List.of(HardwareEra.STANDARD, HardwareEra.ADVANCED)) {
            made(output, era, ofEra(era, "optical_router"), aluminiumPlate(), 1, board(era), 1, controller(era), 2,
                    memory(era), 1, ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(), 2, ChainParts.OPTICAL_FIBRE.get(),
                    2);
        }
        serverRouter(output, HardwareEra.LEGACY, item("legacy_server_router"), 1);
        serverRouter(output, HardwareEra.TRANSITION, item("transition_server_router"), 2);
        serverRouter(output, HardwareEra.STANDARD, item("server_router"), 4);
        serverRouter(output, HardwareEra.ADVANCED, item("advanced_server_router"), 8);
    }

    private static void serverRouter(final RecipeOutput output, final Era era, final Item router,
                                     final int controllers) {
        made(output, era, router, steelPlate(), 2, board(era), 1, controller(era), controllers, memory(era), 1,
                copperWire(), 6);
    }

    /*
     * Each with its box, its board and its controller: the Import Bus pulls with a motor; the Export Bus pushes with a
     * motor and its ejector (a steel rod); the External Storage Bus reads an inventory as a disk (the era's drive
     * connector); the Crafting Interface keeps its machine's patterns, a memory chip for every 3 it holds. The Crafting
     * Input Router and the Receiving Bus serve every era and are made once, in the Vintage.
     */
    private static void buses(final RecipeOutput output) {
        final Item motor = ChainParts.ELECTRIC_MOTOR.get();
        for (final Era era : ChainParts.ERAS) {
            bus(output, era, ofEra(era, "import_bus"), motor, 1);
            bus(output, era, ofEra(era, "export_bus"), motor, 1, steelRod(), 1);
            bus(output, era, ofEra(era, "external_storage_bus"),
                    part(era.isAtMost(HardwareEra.LEGACY) ? "storage_ide" : "storage_sata"), 1);
            bus(output, era, ofEra(era, "crafting_interface"), memory(era), INTERFACE_MEMORY[index(era)]);
        }
        final Era v = HardwareEra.VINTAGE;
        bus(output, v, item("crafting_input_router"), steelRod(), 1, Items.REDSTONE, 1);
        bus(output, v, item("receiving_bus"), motor, 1, Items.REDSTONE, 1);
    }

    /* A bus of {@code era}: its box, its board and its controller, and what does its work. */
    private static void bus(final RecipeOutput output, final Era era, final Item bus, final Object... work) {
        made(output, era, bus, join(new Object[] {plastic(era), 1, board(era), 1, controller(era), 1}, work));
    }
}
