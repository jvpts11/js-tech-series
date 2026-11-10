/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import static dev.jstech.computers.datagen.recipe.BenchItems.board;
import static dev.jstech.computers.datagen.recipe.BenchItems.chip;
import static dev.jstech.computers.datagen.recipe.BenchItems.controller;
import static dev.jstech.computers.datagen.recipe.BenchItems.copperFoil;
import static dev.jstech.computers.datagen.recipe.BenchItems.copperWire;
import static dev.jstech.computers.datagen.recipe.BenchItems.item;
import static dev.jstech.computers.datagen.recipe.BenchItems.join;
import static dev.jstech.computers.datagen.recipe.BenchItems.made;
import static dev.jstech.computers.datagen.recipe.BenchItems.memory;
import static dev.jstech.computers.datagen.recipe.BenchItems.plating;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelPlate;

import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The function cards (computers/docs/design/recipes.md, "Function cards"): each the spec's expansion card, its etched
 * board, four ceramic capacitors, its edge's copper foils and their plating, with its chips on top, by the design's
 * table: the Crafting Cards, the personal-use cards, the cluster's interface cards, the Vaultis controllers, the Phi
 * coprocessors, the WAN interface cards and the Network Anchor Cards.
 */
final class CardRecipes {

    private CardRecipes() {
    }

    static void write(final RecipeOutput output) {
        final Era v = HardwareEra.VINTAGE;
        final Era l = HardwareEra.LEGACY;
        final Era t = HardwareEra.TRANSITION;
        final Era s = HardwareEra.STANDARD;
        final Era a = HardwareEra.ADVANCED;
        card(output, v, "crafting_card_isa", 4, controller(v), 1, memory(v), 1);
        card(output, v, "serial_console_card", 5, controller(v), 1, copperWire(),1);
        card(output, v, "vaultis_array_isa", 4, controller(v), 2);
        card(output, v, "vaultis_cache_isa", 4, controller(v), 1, memory(v), 2);
        card(output, v, "modem_card", 4, controller(v), 1, ChainParts.TRANSFORMER.get(), 1);
        card(output, v, "network_anchor_card_vintage", 4, controller(v), 1, memory(v), 3);
        card(output, l, "crafting_card_pci", 5, controller(l), 2, memory(l), 1);
        card(output, l, "crafting_table_card", 2, controller(l), 1, memory(l), 2);
        card(output, l, "furnace_card", 2, controller(l), 1, ChainParts.COPPER_COIL.get(), 1);
        card(output, l, "enchanting_card", 2, controller(l), 1, Items.LAPIS_LAZULI, 1);
        card(output, l, "anvil_card", 2, controller(l), 1, steelPlate(), 1);
        card(output, l, "management_nic", 2, controller(l), 2, copperWire(),1);
        card(output, l, "vaultis_array_pci", 5, controller(l), 2);
        card(output, l, "vaultis_cache_pci", 5, controller(l), 1, memory(l), 2);
        card(output, l, "t1_card", 5, controller(l), 1, ChainParts.TRANSFORMER.get(), 2);
        card(output, l, "network_anchor_card_legacy", 5, controller(l), 1, memory(l), 3);
        card(output, t, "crafting_card_t2", 2, controller(t), 2, memory(t), 3);
        card(output, t, "crafting_card_t3", 2, controller(t), 4, memory(t), 4);
        card(output, t, "vaultis_array_pcie2", 2, controller(t), 2);
        card(output, t, "vaultis_cache_pcie2", 2, controller(t), 1, memory(t), 2);
        card(output, t, "t3_card", 2, controller(t), 2, ChainParts.TRANSFORMER.get(), 1, copperWire(),1);
        card(output, t, "network_anchor_card_transition", 2, controller(t), 1, memory(t), 3);
        card(output, s, "crafting_card_pcie3", 2, controller(s), 6, memory(s), 5);
        card(output, s, "fabric_host_adapter", 2, controller(s), 4, copperWire(),1);
        card(output, s, "vaultis_array_pcie3", 2, controller(s), 2);
        card(output, s, "vaultis_cache_pcie3", 2, controller(s), 1, memory(s), 4);
        card(output, s, "optical_network_card", 2, controller(s), 1, ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(),
                2, ChainParts.OPTICAL_FIBRE.get(), 2);
        final Item mic = chip(ChainChips.PROCESSORS, s, "mic");
        card(output, s, "phi_5100", 2, mic, 8, memory(s), 8);
        card(output, s, "phi_7120", 2, mic, 9, memory(s), 8);
        card(output, s, "phi_7290", 2, mic, 10, memory(s), 8);
        card(output, s, "phi_9000", 2, mic, 12, memory(s), 8);
        card(output, s, "vldc_card", 2, controller(s), 2, ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(), 2,
                ChainParts.OPTICAL_FIBRE.get(), 2);
        card(output, s, "network_anchor_card_standard", 2, controller(s), 1, memory(s), 3);
        card(output, a, "crafting_card_t4", 2, controller(a), 8, memory(a), 5);
        card(output, a, "fabric_dpu", 2, controller(a), 8, copperWire(),1);
        card(output, a, "vaultis_array_pcie4", 2, controller(a), 2);
        card(output, a, "vaultis_cache_pcie4", 2, controller(a), 1, memory(a), 4);
        card(output, a, "optical_transport_card", 2, controller(a), 2, ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(),
                4, ChainParts.OPTICAL_FIBRE.get(), 4);
        card(output, a, "network_anchor_card_advanced", 2, controller(a), 1, memory(a), 3);
    }

    /* A card of {@code era}: its etched board, four ceramic capacitors, {@code foils} copper foils and its plating. */
    private static void card(final RecipeOutput output, final Era era, final String id, final int foils,
                             final Object... chips) {
        made(output, era, item(id), join(new Object[] {board(era), 1, ChainParts.CERAMIC_CAPACITOR.get(), 4,
            copperFoil(), foils, plating(era), 1}, chips));
    }
}
