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
import static dev.jstech.computers.datagen.recipe.BenchItems.index;
import static dev.jstech.computers.datagen.recipe.BenchItems.item;
import static dev.jstech.computers.datagen.recipe.BenchItems.join;
import static dev.jstech.computers.datagen.recipe.BenchItems.made;
import static dev.jstech.computers.datagen.recipe.BenchItems.ofEra;
import static dev.jstech.computers.datagen.recipe.BenchItems.part;
import static dev.jstech.computers.datagen.recipe.BenchItems.polycarbonate;
import static dev.jstech.computers.datagen.recipe.BenchItems.populated;
import static dev.jstech.computers.datagen.recipe.BenchItems.pvc;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelPlate;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelRod;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelSheet;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.registry.EmptyComputerIngredient;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.List;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The computers (computers/docs/design/recipes.md, "Computers"): each desk case, empty, with its metals and its front;
 * the Crafting Computer and the Cluster Management Computer as the empty Personal Computer of the same case with its
 * era's card in it for good; Quince's logic boards and desk machines by the design's own tables; and the server room:
 * the chassis and the servers, the supercomputer's nodes and racks, the rack units, the machines placed whole and the
 * HBW Interface.
 */
final class ComputerRecipes {

    /* Each era's Crafting Card and the card that makes a Cluster Management Computer of a Personal Computer. */
    private static final List<String> CRAFTING_CARDS = List.of("crafting_card_isa", "crafting_card_pci",
            "crafting_card_t2", "crafting_card_pcie3", "crafting_card_t4");
    private static final List<String> CLUSTER_CARDS = List.of("serial_console_card", "management_nic",
            "management_nic", "fabric_host_adapter", "fabric_dpu");
    /* The drives each chassis takes from the rack's bays, by era from the Vintage, and how many U it stands. */
    private static final List<Chassis> CHASSIS = List.of(new Chassis("server", 1, new int[] {1, 2, 4, 3, 4}),
            new Chassis("storage_server", 2, new int[] {4, 4, 6, 8, 10}),
            new Chassis("compute_server", 2, new int[] {1, 1, 1, 1, 1}));

    private ComputerRecipes() {
    }

    static void write(final RecipeOutput output) {
        deskComputers(output);
        quinceBoards(output);
        quinceMachines(output);
        serverRoom(output);
    }

    /*
     * Each case gives the Personal Computer of its look: its metals and its front, the era's etched board (the panel
     * with the power switch and the lights), two copper wires and the bezel, of PVC to the Legacy and polycarbonate
     * after. Of the same case, the Crafting and the Cluster Management Computers take the empty one and their card.
     */
    private static void deskComputers(final RecipeOutput output) {
        computer(output, HardwareEra.VINTAGE, "vintage_", steelPlate(), 6);
        computer(output, HardwareEra.LEGACY, "legacy_", steelPlate(), 6, aluminiumPlate(), 2);
        computer(output, HardwareEra.TRANSITION, "transition_", steelPlate(), 4, aluminiumPlate(), 4);
        computer(output, HardwareEra.STANDARD, "", steelPlate(), 4, aluminiumPlate(), 4);
        computer(output, HardwareEra.STANDARD, "high_performance_", steelPlate(), 4, aluminiumPlate(), 4,
                steelSheet(), 2);
        computer(output, HardwareEra.STANDARD, "aesthetic_", steelPlate(), 4, aluminiumPlate(), 4, Items.GLASS_PANE, 2);
        computer(output, HardwareEra.ADVANCED, "advanced_", steelPlate(), 4, aluminiumPlate(), 4);
        computer(output, HardwareEra.ADVANCED, "advanced_high_performance_", steelPlate(), 4, aluminiumPlate(), 4,
                steelSheet(), 2);
        computer(output, HardwareEra.ADVANCED, "advanced_aesthetic_", steelPlate(), 4, aluminiumPlate(), 4,
                Items.GLASS_PANE, 2);
    }

    private static void computer(final RecipeOutput output, final Era era, final String look,
                                 final Object... metals) {
        final Item pc = item(look + "personal_computer");
        made(output, era, pc, join(metals, front(era)));
        made(output, era, item(look + "crafting_computer"), empty(pc), 1, item(CRAFTING_CARDS.get(index(era))), 1);
        // The Advanced High Performance case has no Cluster Management Computer.
        if (!look.equals("advanced_high_performance_")) {
            made(output, era, item(look + "cluster_management_computer"), empty(pc), 1,
                    item(CLUSTER_CARDS.get(index(era))), 1);
        }
    }

    /* Quince's logic boards, by the motherboards' rule with the numbers of the real machines of each year. */
    private static void quinceBoards(final RecipeOutput output) {
        final Era v = HardwareEra.VINTAGE;
        final Era l = HardwareEra.LEGACY;
        final Era t = HardwareEra.TRANSITION;
        final Era s = HardwareEra.STANDARD;
        final Era a = HardwareEra.ADVANCED;
        made(output, v, item("quince_board_russet_128k"), populated(v), 1, part("socket_quince_dip64"), 1,
                part("memory_slot_simm"), 4, part("storage_ide"), 1, part("power_at"), 1);
        made(output, l, item("quince_board_qrusset_g3"), populated(l), 1, part("socket_quince_card"), 1,
                part("memory_slot_sdram"), 2, part("storage_ide"), 1, part("power_atx20"), 1);
        made(output, l, item("quince_board_power_g4"), populated(l), 2, part("socket_quince_card"), 2,
                part("memory_slot_ddr"), 4, part("storage_ide"), 2, part("power_atx20"), 1, part("power_eps"), 2,
                part("expansion_slot_agp"), 1, part("expansion_slot_pci"), 4);
        made(output, l, item("quince_board_qserve_legacy"), populated(l), 2, part("socket_quince_card"), 2,
                part("memory_slot_ddr"), 4, part("expansion_slot_pci"), 2, part("storage_ide"), 2,
                part("power_atx20"), 1, part("power_eps"), 2);
        made(output, t, item("quince_board_pro_2006"), populated(t), 2, part("socket_lga_771"), 2,
                part("memory_slot_ddr2"), 8, part("expansion_slot_pcie"), 4, part("storage_sata"), 4,
                part("power_atx24"), 1, part("power_eps"), 2);
        made(output, t, item("quince_board_qrusset_white"), populated(t), 1, part("socket_lga_775"), 1,
                part("memory_slot_ddr2"), 2, part("storage_sata"), 1, part("power_atx24"), 1);
        made(output, t, item("quince_board_qserve_transition"), populated(t), 2, part("socket_lga_771"), 2,
                part("memory_slot_ddr2"), 8, part("expansion_slot_pcie"), 2, part("storage_sata"), 3,
                part("power_atx24"), 1, part("power_eps"), 2);
        made(output, t, item("quince_board_mainframe_t"), populated(t), 2, part("socket_lga_771"), 4,
                part("memory_slot_ddr2"), 16, part("expansion_slot_pcie"), 6, part("storage_sata"), 4,
                part("power_atx24"), 1, part("power_eps"), 4);
        made(output, s, item("quince_board_qrusset_thin"), populated(s), 1, part("socket_lga_1155"), 1,
                part("memory_slot_ddr3"), 4, part("storage_sata"), 1, part("power_atx24"), 1);
        made(output, s, item("quince_board_pro_2019"), populated(s), 2, part("socket_lga_2011"), 1,
                part("memory_slot_ddr3"), 4, part("expansion_slot_pcie"), 2, part("storage_sata"), 1,
                part("power_atx24"), 1);
        made(output, s, item("quince_board_qserve_standard"), populated(s), 2, part("socket_lga_2011"), 2,
                part("memory_slot_ddr3"), 12, part("expansion_slot_pcie"), 2, part("storage_sata"), 3,
                part("power_atx24"), 1, part("power_eps"), 2);
        made(output, s, item("quince_board_mainframe_s"), populated(s), 2, part("socket_lga_2011"), 4,
                part("memory_slot_ddr3"), 32, part("expansion_slot_pcie"), 6, part("storage_sata"), 4,
                part("power_atx24"), 1, part("power_eps"), 4);
        made(output, a, item("quince_board_russet_studio"), populated(a), 1, part("socket_quince_q"), 1,
                part("memory_slot_ddr5"), 2, part("storage_m2"), 1, part("power_atx24"), 1);
        made(output, a, item("quince_board_qrusset_colour"), populated(a), 1, part("socket_quince_q"), 1,
                part("storage_m2"), 1, part("power_atx24"), 1);
        made(output, a, item("quince_board_qserve_advanced"), populated(a), 2, part("socket_quince_q"), 1,
                part("memory_slot_ddr5"), 8, part("expansion_slot_pcie"), 2, part("storage_m2"), 4,
                part("power_atx24"), 1);
        made(output, a, item("quince_board_mainframe_a"), populated(a), 2, part("socket_quince_q"), 4,
                part("memory_slot_ddr5"), 32, part("expansion_slot_pcie"), 6, part("storage_m2"), 4,
                part("power_atx24"), 1, part("power_eps"), 4);
    }

    /*
     * Quince's desk machines by the cases' rule, with the real machine's materials and, in an all-in-one, the era's
     * screen. A machine sold in several colours is made in its first.
     */
    private static void quinceMachines(final RecipeOutput output) {
        final Item wire = copperWire();
        made(output, HardwareEra.VINTAGE, item("russet_128k"), pvc(), 5, ChainParts.TUBE_SCREEN.get(), 1,
                board(HardwareEra.VINTAGE), 1, wire, 2);
        made(output, HardwareEra.LEGACY, item("qrusset_g3_bondi"), polycarbonate(), 4, ChainParts.TUBE_SCREEN.get(),
                1, board(HardwareEra.LEGACY), 1, wire, 2, pvc(), 1);
        made(output, HardwareEra.LEGACY, item("russet_power_g4"), steelPlate(), 4, polycarbonate(), 2,
                board(HardwareEra.LEGACY), 1, wire, 2, pvc(), 1);
        made(output, HardwareEra.TRANSITION, item("russet_pro_t"), aluminiumPlate(), 6, aluminiumSheet(), 2,
                board(HardwareEra.TRANSITION), 1, wire, 2, polycarbonate(), 1);
        made(output, HardwareEra.TRANSITION, item("qrusset_white"), polycarbonate(), 5, ChainParts.LCD_SCREEN.get(),
                1, board(HardwareEra.TRANSITION), 1, wire, 2);
        made(output, HardwareEra.STANDARD, item("qrusset_thin"), aluminiumPlate(), 4, ChainParts.LED_SCREEN.get(), 1,
                Items.GLASS_PANE, 1, board(HardwareEra.STANDARD), 1, wire, 2, polycarbonate(), 1);
        made(output, HardwareEra.STANDARD, item("russet_pro_s"), aluminiumPlate(), 4, steelRod(), 2,
                board(HardwareEra.STANDARD), 1, wire, 2, polycarbonate(), 1);
        made(output, HardwareEra.ADVANCED, item("russet_studio"), aluminiumPlate(), 6, board(HardwareEra.ADVANCED), 1,
                wire, 2, polycarbonate(), 1);
        made(output, HardwareEra.ADVANCED, item("qrusset_colour_blue"), aluminiumPlate(), 4,
                ChainParts.SELF_LIT_PANEL.get(), 1, Items.GLASS_PANE, 1, board(HardwareEra.ADVANCED), 1, wire, 2,
                polycarbonate(), 1);
    }

    /*
     * The server room. A chassis is two steel plates for every U, an aluminium front, the desk cases' front and the
     * connectors of the drives it takes from the rack's bays; a server, its chassis with two of its era's case fans
     * for every U and its rails, two steel rods. The rest each at its era's bench.
     */
    private static void serverRoom(final RecipeOutput output) {
        final Item rod = steelRod();
        final Item wire = copperWire();
        for (final Era era : ChainParts.ERAS) {
            for (final Chassis chassis : CHASSIS) {
                final Item made = ofEra(era, chassis.id() + "_case");
                made(output, era, made, join(new Object[] {steelPlate(), 2 * chassis.units(), aluminiumPlate(), 1},
                        front(era), bays(era, chassis.drives()[index(era)])));
                made(output, era, ofEra(era, chassis.id()), made, 1, caseFan(era), 2 * chassis.units(), rod, 2);
            }
            made(output, era, ofEra(era, "server_rack"), steelPlate(), 12, rod, 4, caseFan(era), 2, board(era), 1,
                    bay(era), 8);
            made(output, era, ofEra(era, "kvm_switch"), steelPlate(), 2, board(era), 1, controller(era), 1, wire, 4);
            made(output, era, ofEra(era, "mainframe"), steelPlate(), 12, aluminiumPlate(), 4, caseFan(era), 4,
                    board(era), 2, wire, 8);
            made(output, era, ofEra(era, "wan_gateway"), steelPlate(), 16, aluminiumPlate(), 4, caseFan(era), 4,
                    board(era), 2, wire, 8, controller(era), 4);
        }
        final Era s = HardwareEra.STANDARD;
        final Era a = HardwareEra.ADVANCED;
        made(output, s, item("supercomputer_node"), join(new Object[] {steelPlate(), 4, aluminiumPlate(), 1},
                front(s), new Object[] {part("storage_sata"), 1, caseFan(s), 4, rod, 2}));
        made(output, a, item("advanced_supercomputer_node"), join(new Object[] {steelPlate(), 8, aluminiumPlate(), 1},
                front(a), new Object[] {part("storage_sata"), 2, caseFan(a), 4, rod, 2}));
        for (final Era era : List.of(s, a)) {
            made(output, era, ofEra(era, "supercomputer_rack"), steelPlate(), 12, rod, 4, caseFan(era), 4,
                    board(era), 1, bay(era), 8, ChainParts.LED.get(), 4, coreItem("copper_pipe"), 2);
            made(output, era, ofEra(era, "fibre_optic_interface"), steelPlate(), 2, board(era), 1, controller(era),
                    1, ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(), 2, ChainParts.OPTICAL_FIBRE.get(),
                    era == s ? 2 : 4);
            made(output, era, ofEra(era, "hbw_interface"), steelPlate(), 2, board(era), 1, controller(era), 2,
                    part("expansion_slot_pcie"), 6, ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(), 2,
                    ChainParts.OPTICAL_FIBRE.get(), 2);
        }
        made(output, s, item("drive_shelf"), steelPlate(), 4, board(s), 1, part("storage_sata"), 10, caseFan(s), 2);
        for (final Era era : List.of(HardwareEra.TRANSITION, s, a)) {
            made(output, era, item("quince_mainframe_" + era.path()), aluminiumPlate(), 16, caseFan(era), 4,
                    board(era), 2, wire, 8, Items.GLASS_PANE, 2);
        }
        // A qServe is a 1U of aluminium: its chassis, then the chassis with its fans and its rails, as a server is.
        final int[] qserveDrives = {0, 4, 3, 3, 4};
        for (final Era era : List.of(HardwareEra.LEGACY, HardwareEra.TRANSITION, s, a)) {
            final Item chassis = item("qserve_" + era.path() + "_case");
            made(output, era, chassis, join(new Object[] {aluminiumPlate(), 3}, front(era),
                    bays(era, qserveDrives[index(era)])));
            made(output, era, item("qserve_" + era.path()), chassis, 1, caseFan(era), 2, rod, 2);
        }
    }

    /* The desk cases' front: the era's etched board, two copper wires and the bezel. */
    private static Object[] front(final Era era) {
        return new Object[] {board(era), 1, copperWire(), 2, era.isAtMost(HardwareEra.LEGACY) ? pvc() : polycarbonate(),
            1};
    }

    /* The connectors of {@code drives} bays: an IDE header for every two to the Legacy, a SATA port for each after. */
    private static Object[] bays(final Era era, final int drives) {
        return new Object[] {bay(era), era.isAtMost(HardwareEra.LEGACY) ? (drives + 1) / 2 : drives};
    }

    private static Item bay(final Era era) {
        return part(era.isAtMost(HardwareEra.LEGACY) ? "storage_ide" : "storage_sata");
    }

    private static Ingredient empty(final Item computer) {
        return new EmptyComputerIngredient(computer).toVanilla();
    }

    /* A server chassis: its id, the U it stands and the drives it takes by era. */
    private record Chassis(String id, int units, int[] drives) {
    }
}
