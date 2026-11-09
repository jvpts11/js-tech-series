/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;
import static dev.jstech.computers.registry.ComputingContent.shelf;

import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.world.item.Item;

/**
 * The parts the mod's own chain makes on the way to a computer's (computers/docs/design/survival.md), each a plain item
 * on the chain's shelf of the era it first appears in: the memory and controller chips and their masks for each era,
 * the quartz plate a mask is made on, each era's finished wafer, the passive components, the laser diodes and the
 * screens (their lights animated, as the game animates an item), the laminates and each era's etched board, the fibres,
 * the bonding wires, the discs and platters, and the thermal paste. The processor and graphics chips and their masks
 * are each microarchitecture's own, and come with them.
 */
public final class ChainParts {

    /** The eras that have a bench and their own chips, from the oldest. */
    public static final List<Era> ERAS = List.of(HardwareEra.VINTAGE, HardwareEra.LEGACY, HardwareEra.TRANSITION,
            HardwareEra.STANDARD, HardwareEra.ADVANCED);

    public static final List<ItemEntry<Item>> MEMORY_CHIPS = perEra(era ->
            part("memory_chip_" + era.path(), era).named("Memory Chip (" + englishName(era) + ")").register());
    public static final List<ItemEntry<Item>> CONTROLLER_CHIPS = perEra(era ->
            part("controller_chip_" + era.path(), era).named("Controller Chip (" + englishName(era) + ")").register());
    public static final ItemEntry<Item> MASK_PLATE = part("mask_plate", HardwareEra.VINTAGE).named("Mask Plate")
            .register();
    public static final List<ItemEntry<Item>> MEMORY_MASKS = perEra(era ->
            part("mask_memory_" + era.path(), era).named("Memory Mask (" + englishName(era) + ")").register());
    public static final List<ItemEntry<Item>> CONTROLLER_MASKS = perEra(era ->
            part("mask_controller_" + era.path(), era).named("Controller Mask (" + englishName(era) + ")").register());
    public static final List<ItemEntry<Item>> FINISHED_WAFERS = perEra(era ->
            part("finished_wafer_" + era.path(), era).named("Finished Wafer (" + englishName(era) + ")").register());

    public static final ItemEntry<Item> CERAMIC_PACKAGE = part("ceramic_package", HardwareEra.VINTAGE)
            .named("Ceramic Package").register();
    public static final ItemEntry<Item> CERAMIC_CAPACITOR = part("ceramic_capacitor", HardwareEra.VINTAGE)
            .named("Ceramic Capacitor").register();
    public static final ItemEntry<Item> TANTALUM_CAPACITOR = part("tantalum_capacitor", HardwareEra.VINTAGE)
            .named("Tantalum Capacitor").register();
    public static final ItemEntry<Item> FERRITE = part("ferrite", HardwareEra.VINTAGE).named("Ferrite").register();
    public static final ItemEntry<Item> INDUCTOR = part("inductor", HardwareEra.VINTAGE).named("Inductor")
            .register();
    public static final ItemEntry<Item> TRANSFORMER = part("transformer", HardwareEra.VINTAGE).named("Transformer")
            .register();
    /* The lasers of the CD, the DVD and the Blu-ray, in the eras of their drives. */
    public static final ItemEntry<Item> GALLIUM_ARSENIDE_LASER_DIODE = part("laser_diode_gallium_arsenide",
            HardwareEra.LEGACY).named("Gallium Arsenide Laser Diode").register();
    public static final ItemEntry<Item> ALGAINP_LASER_DIODE = part("laser_diode_algainp", HardwareEra.TRANSITION)
            .named("AlGaInP Laser Diode").register();
    public static final ItemEntry<Item> GALLIUM_NITRIDE_LASER_DIODE = part("laser_diode_gallium_nitride",
            HardwareEra.STANDARD).named("Gallium Nitride Laser Diode").register();
    public static final ItemEntry<Item> COLD_CATHODE_LAMP = part("cold_cathode_lamp", HardwareEra.LEGACY)
            .named("Cold Cathode Lamp").register();
    /* Each screen in the era its kind of monitor first sits on a desk. */
    public static final ItemEntry<Item> TUBE_SCREEN = part("tube_screen", HardwareEra.VINTAGE).named("Tube Screen")
            .register();
    public static final ItemEntry<Item> LCD_SCREEN = part("lcd_screen", HardwareEra.LEGACY).named("LCD Screen")
            .register();
    public static final ItemEntry<Item> LED_SCREEN = part("led_screen", HardwareEra.STANDARD).named("LED Screen")
            .register();
    public static final ItemEntry<Item> SELF_LIT_PANEL = part("self_lit_panel", HardwareEra.ADVANCED)
            .named("Self-Lit Panel").register();

    public static final ItemEntry<Item> FR2_LAMINATE = part("fr2_laminate", HardwareEra.VINTAGE)
            .named("FR-2 Laminate").register();
    public static final ItemEntry<Item> FR4_LAMINATE = part("fr4_laminate", HardwareEra.LEGACY)
            .named("FR-4 Laminate").register();
    public static final List<ItemEntry<Item>> ETCHED_BOARDS = perEra(era ->
            part("etched_board_" + era.path(), era).named("Etched Board (" + englishName(era) + ")").register());
    public static final ItemEntry<Item> GLASS_FIBRE = part("glass_fibre", HardwareEra.LEGACY).named("Glass Fibre")
            .register();
    public static final ItemEntry<Item> OPTICAL_FIBRE = part("optical_fibre", HardwareEra.STANDARD)
            .named("Optical Fibre").register();
    public static final ItemEntry<Item> ALUMINIUM_BONDING_WIRE = part("aluminium_bonding_wire", HardwareEra.VINTAGE)
            .named("Aluminium Bonding Wire").register();
    public static final ItemEntry<Item> GOLD_BONDING_WIRE = part("gold_bonding_wire", HardwareEra.LEGACY)
            .named("Gold Bonding Wire").register();
    public static final ItemEntry<Item> FLOPPY_FILM = part("floppy_film", HardwareEra.VINTAGE).named("Floppy Film")
            .register();
    public static final ItemEntry<Item> PLATTER_VINTAGE = part("platter_vintage", HardwareEra.VINTAGE)
            .named("Platter (Vintage)").register();
    public static final ItemEntry<Item> PLATTER = part("platter", HardwareEra.LEGACY).named("Platter").register();
    public static final ItemEntry<Item> BLANK_DISC = part("blank_disc", HardwareEra.LEGACY).named("Blank Disc")
            .register();
    public static final ItemEntry<Item> THERMAL_PASTE = part("thermal_paste", HardwareEra.LEGACY)
            .named("Thermal Paste").register();

    private ChainParts() {
    }

    /** Declares the parts, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The English name of {@code era} as the chain's things carry it in their own names: "Vintage". */
    static String englishName(final Era era) {
        final String path = era.path();
        return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
    }

    /* The part {@code declared} declares for each era, from the oldest. */
    private static List<ItemEntry<Item>> perEra(final Function<Era, ItemEntry<Item>> declared) {
        final List<ItemEntry<Item>> out = new ArrayList<>();
        for (final Era era : ERAS) {
            out.add(declared.apply(era));
        }
        return List.copyOf(out);
    }

    /* A plain part on the chain's shelf of {@code era}, to be named and registered. */
    private static ItemBuilder<Item> part(final String id, final Era era) {
        return CONTENT.item(id, Item::new).look(IItemLook.FLAT).tab(shelf(Shelf.CHAIN, era));
    }
}
