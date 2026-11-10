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
import static dev.jstech.computers.datagen.recipe.BenchItems.chip;
import static dev.jstech.computers.datagen.recipe.BenchItems.controller;
import static dev.jstech.computers.datagen.recipe.BenchItems.copperFoil;
import static dev.jstech.computers.datagen.recipe.BenchItems.copperWire;
import static dev.jstech.computers.datagen.recipe.BenchItems.glassLens;
import static dev.jstech.computers.datagen.recipe.BenchItems.item;
import static dev.jstech.computers.datagen.recipe.BenchItems.join;
import static dev.jstech.computers.datagen.recipe.BenchItems.made;
import static dev.jstech.computers.datagen.recipe.BenchItems.memory;
import static dev.jstech.computers.datagen.recipe.BenchItems.plating;
import static dev.jstech.computers.datagen.recipe.BenchItems.polycarbonate;
import static dev.jstech.computers.datagen.recipe.BenchItems.pvc;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelPlate;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.of;

import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.jstechseries.SeriesAlloys;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;

/**
 * Wireless and the portables (computers/docs/design/recipes.md, "Wireless and the portables"), each at the bench of
 * its era: the access points, the Wi-Fi cards and the charging docks, and each portable made of the real device's
 * parts.
 */
final class WirelessRecipes {

    /* The copper foils of a Wi-Fi card's edge: the Legacy's a PCI card's, the later ones a PCI Express card's. */
    private static final int PCI_FOILS = 5;
    private static final int PCIE_FOILS = 2;

    private WirelessRecipes() {
    }

    static void write(final RecipeOutput output) {
        accessPoints(output);
        wifiCards(output);
        docks(output);
        portables(output);
    }

    /*
     * An access point is its board, its radio (a controller; two on the dual-band ones), its antennas (two and three
     * outside on the desk ones, four and eight inside the ceiling ones, as 802.11ac and Wi-Fi 6 had), its case, its
     * light, the ceiling ones' mounting plate, and its two lines, data and energy.
     */
    private static void accessPoints(final RecipeOutput output) {
        final Item antenna = ChainParts.ANTENNA.get();
        final Item led = ChainParts.LED.get();
        accessPoint(output, HardwareEra.LEGACY, 1, antenna, 2, pvc(), 1);
        accessPoint(output, HardwareEra.TRANSITION, 1, antenna, 3, polycarbonate(), 1);
        accessPoint(output, HardwareEra.STANDARD, 2, antenna, 4, polycarbonate(), 1, led, 1, steelPlate(), 1);
        accessPoint(output, HardwareEra.ADVANCED, 2, antenna, 8, polycarbonate(), 1, led, 2, steelPlate(), 1);
    }

    private static void accessPoint(final RecipeOutput output, final Era era, final int radios,
                                    final Object... body) {
        made(output, era, item(era.path() + "_access_point"), join(new Object[] {board(era), 1, controller(era),
            radios}, body, new Object[] {copperWire(), 2}));
    }

    /*
     * A Wi-Fi card is the card base, PCI in the Legacy and PCI Express x1 after, with its radio and antennas; the
     * Advanced's carries Bluetooth, a second controller.
     */
    private static void wifiCards(final RecipeOutput output) {
        wifiCard(output, HardwareEra.LEGACY, PCI_FOILS, 1, 1);
        wifiCard(output, HardwareEra.TRANSITION, PCIE_FOILS, 1, 3);
        wifiCard(output, HardwareEra.STANDARD, PCIE_FOILS, 1, 3);
        wifiCard(output, HardwareEra.ADVANCED, PCIE_FOILS, 2, 2);
    }

    private static void wifiCard(final RecipeOutput output, final Era era, final int foils, final int radios,
                                 final int antennas) {
        made(output, era, item("wifi_card_" + era.path()), board(era), 1, ChainParts.CERAMIC_CAPACITOR.get(), 4,
                copperFoil(), foils, plating(era), 1, controller(era), radios, ChainParts.ANTENNA.get(), antennas);
    }

    /*
     * A dock is its board, its charger (a controller and a transformer), its lines and its body: a PVC cradle, a
     * polycarbonate base, an aluminium stand with its lamp, a wireless stand whose charging coil is a copper coil.
     */
    private static void docks(final RecipeOutput output) {
        final Item led = ChainParts.LED.get();
        dock(output, HardwareEra.LEGACY, pvc(), 1);
        dock(output, HardwareEra.TRANSITION, polycarbonate(), 1);
        dock(output, HardwareEra.STANDARD, aluminiumPlate(), 1, led, 1);
        dock(output, HardwareEra.ADVANCED, polycarbonate(), 1, led, 1, ChainParts.COPPER_COIL.get(), 1);
    }

    private static void dock(final RecipeOutput output, final Era era, final Object... body) {
        made(output, era, item(era.path() + "_mobile_device_charging_dock"), join(new Object[] {board(era), 1,
            controller(era), 1, ChainParts.TRANSFORMER.get(), 1, copperWire(), 2}, body));
    }

    /*
     * A portable is the real device's parts: its chip, its maker's die of its era (a faster model of the same die one
     * chip more); its memory, the memory chips its era's density holds, and its flash, one memory chip and one more for
     * every bigger size of its era; the etched board; the screen of its era (two on a tablet) under its touch layer, an
     * indium tin oxide foil; its lithium-ion cells (two on a tablet); its antennas; the camera's lens on the phones and
     * tablets; and its case.
     */
    private static void portables(final RecipeOutput output) {
        final Era l = HardwareEra.LEGACY;
        final Era t = HardwareEra.TRANSITION;
        final Era s = HardwareEra.STANDARD;
        final Era a = HardwareEra.ADVANCED;
        final Item lcd = ChainParts.LCD_SCREEN.get();
        final Item led = ChainParts.LED_SCREEN.get();
        final Item panel = ChainParts.SELF_LIT_PANEL.get();
        final Item cell = ChainParts.LITHIUM_ION_CELL.get();
        final Item antenna = ChainParts.ANTENNA.get();
        final Item lens = glassLens();
        portable(output, l, "hightide_pocket", mobile(l, "ht"), 1, memory(l), 3, lcd, 1, cell, 1, antenna, 1, pvc(),
                1);
        portable(output, t, "hightide_pocket_pro", mobile(t, "ht"), 1, memory(t), 2, lcd, 1, cell, 1, antenna, 1,
                polycarbonate(), 1);
        portable(output, t, "qphone", mobile(t, "qa"), 1, memory(t), 3, lcd, 1, cell, 1, antenna, 3, lens, 1,
                aluminiumPlate(), 1);
        portable(output, t, "motoroller_landmark", mobile(t, "mx"), 1, memory(t), 5, lcd, 1, cell, 1, antenna, 3,
                lens, 1, polycarbonate(), 1);
        portable(output, s, "qphone_5", mobile(s, "qa"), 1, memory(s), 3, led, 1, cell, 1, antenna, 3, lens, 1,
                aluminiumPlate(), 1);
        portable(output, s, "hightide_wave", mobile(s, "ht"), 1, memory(s), 4, led, 1, cell, 1, antenna, 3, lens, 1,
                aluminiumPlate(), 1);
        portable(output, s, "qpad_air", mobile(s, "qa"), 2, memory(s), 5, led, 2, cell, 2, antenna, 2, lens, 1,
                aluminiumPlate(), 2);
        portable(output, s, "sunsung_nebula_tab", mobile(s, "ex"), 1, memory(s), 2, led, 2, cell, 2, antenna, 2,
                lens, 1, polycarbonate(), 2);
        portable(output, a, "qphone_x", mobile(a, "qa"), 1, memory(a), 2, panel, 1, cell, 1, antenna, 3, lens, 1,
                steelPlate(), 1);
        portable(output, a, "sunsung_nebula_s10", mobile(a, "ex"), 1, memory(a), 4, panel, 1, cell, 1, antenna, 3,
                lens, 1, aluminiumPlate(), 1);
        portable(output, a, "qpad_pro", chip(ChainChips.PROCESSORS, a, "q1"), 1, memory(a), 5, panel, 2, cell, 2,
                antenna, 2, lens, 1, aluminiumPlate(), 2);
        portable(output, a, "sunsung_nebula_tab_s7", mobile(a, "ex"), 2, memory(a), 4, panel, 2, cell, 2, antenna,
                2, lens, 1, aluminiumPlate(), 2);
    }

    /* A portable of {@code era}: its parts, and the etched board and touch layer every one has. */
    private static void portable(final RecipeOutput output, final Era era, final String id, final Object... parts) {
        made(output, era, item(id), join(parts, new Object[] {board(era), 1, of(SeriesAlloys.INDIUM_TIN_OXIDE,
            Forms.FOIL), 1}));
    }

    private static Item mobile(final Era era, final String path) {
        return chip(ChainChips.MOBILE, era, path);
    }
}
