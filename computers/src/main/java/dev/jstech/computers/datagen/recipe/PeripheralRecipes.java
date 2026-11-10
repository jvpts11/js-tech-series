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
import static dev.jstech.computers.datagen.recipe.BenchItems.glassLens;
import static dev.jstech.computers.datagen.recipe.BenchItems.item;
import static dev.jstech.computers.datagen.recipe.BenchItems.made;
import static dev.jstech.computers.datagen.recipe.BenchItems.memory;
import static dev.jstech.computers.datagen.recipe.BenchItems.ofEra;
import static dev.jstech.computers.datagen.recipe.BenchItems.part;
import static dev.jstech.computers.datagen.recipe.BenchItems.plastic;
import static dev.jstech.computers.datagen.recipe.BenchItems.polycarbonate;
import static dev.jstech.computers.datagen.recipe.BenchItems.pvc;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelPlate;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelRod;
import static dev.jstech.computers.datagen.recipe.BenchItems.steelSheet;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.dust;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.nugget;
import static dev.jstech.computers.datagen.recipe.ChainRecipeItems.of;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import java.util.Map;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The peripherals and the media (computers/docs/design/recipes.md, "Peripherals and media"), each at the bench of its
 * era: the monitors, the printers, the drives and the Dock Station, the speakers, the Redstone Interfaces, the hubs,
 * the Pattern Encoders and the Network Gateway, and the writable media. The media pressed with their content are
 * never made: they are found, bought and copied onto the blank ones.
 */
final class PeripheralRecipes {

    /* The drive each era's Pattern Encoder writes its era's medium with; the Standard's writes a USB stick. */
    private static final Map<Era, String> ENCODER_DRIVES = Map.of(HardwareEra.VINTAGE, "floppy_drive",
            HardwareEra.LEGACY, "internal_cd_drive", HardwareEra.TRANSITION, "internal_dvd_drive",
            HardwareEra.ADVANCED, "internal_blu_ray_drive");

    private PeripheralRecipes() {
    }

    static void write(final RecipeOutput output) {
        monitors(output);
        printers(output);
        drives(output);
        speakers(output);
        devices(output);
        media(output);
    }

    /*
     * The Display Line's screen of its era, an etched board and a case of 2 PVC sheets (Vintage) or 2 polycarbonate.
     * The Vintage tubes differ by their phosphor: white (the tube's own zinc sulfide), green (doped with copper), amber
     * (with manganese), and the CGA's colours (both, behind a steel shadow mask), which the Legacy's colour tube has.
     */
    private static void monitors(final RecipeOutput output) {
        final Era v = HardwareEra.VINTAGE;
        final Era l = HardwareEra.LEGACY;
        final Item tube = ChainParts.TUBE_SCREEN.get();
        final Item copper = nugget(Element.COPPER);
        final Item manganese = dust(Element.MANGANESE);
        made(output, v, item("mono_i_monitor"), tube, 1, board(v), 1, pvc(), 2);
        made(output, v, item("vintage_monitor"), tube, 1, board(v), 1, pvc(), 2, copper, 1);
        made(output, v, item("amber_monitor"), tube, 1, board(v), 1, pvc(), 2, manganese, 1);
        made(output, v, item("cga_monitor"), tube, 1, board(v), 1, pvc(), 2, steelSheet(), 1, copper, 1, manganese,
                1);
        made(output, l, item("legacy_monitor"), tube, 1, board(l), 1, polycarbonate(), 2, steelSheet(), 1, copper, 1,
                manganese, 1);
        made(output, HardwareEra.TRANSITION, item("transition_monitor"), ChainParts.LCD_SCREEN.get(), 1,
                board(HardwareEra.TRANSITION), 1, polycarbonate(), 2);
        made(output, HardwareEra.STANDARD, item("monitor"), ChainParts.LED_SCREEN.get(), 1,
                board(HardwareEra.STANDARD), 1, polycarbonate(), 2);
        made(output, HardwareEra.ADVANCED, item("color_monitor"), ChainParts.SELF_LIT_PANEL.get(), 1,
                board(HardwareEra.ADVANCED), 1, polycarbonate(), 2);
    }

    /*
     * Each the printer of its kind: the dot matrix's 9-pin head (solenoids, copper coils) on its carriage rails; the
     * inkjets' heads (a controller chip each) and the photo printer's little screen; the laser printer's laser, its
     * drum (an aluminium plate) and its fuser's heater (a copper coil); the ink tank printer's tanks.
     */
    private static void printers(final RecipeOutput output) {
        final Item motor = ChainParts.ELECTRIC_MOTOR.get();
        final Item coil = ChainParts.COPPER_COIL.get();
        made(output, HardwareEra.VINTAGE, item("vintage_printer"), pvc(), 2, motor, 2, coil, 2, steelRod(), 2,
                board(HardwareEra.VINTAGE), 1);
        made(output, HardwareEra.LEGACY, item("legacy_printer"), polycarbonate(), 2, motor, 2,
                controller(HardwareEra.LEGACY), 1, steelRod(), 1, board(HardwareEra.LEGACY), 1);
        made(output, HardwareEra.TRANSITION, item("transition_printer"), polycarbonate(), 2, motor, 2,
                controller(HardwareEra.TRANSITION), 2, ChainParts.LCD_SCREEN.get(), 1, board(HardwareEra.TRANSITION),
                1);
        made(output, HardwareEra.STANDARD, item("printer"), polycarbonate(), 2, motor, 2,
                ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(), 1, aluminiumPlate(), 1, coil, 1,
                board(HardwareEra.STANDARD), 1);
        made(output, HardwareEra.ADVANCED, item("advanced_printer"), polycarbonate(), 3, motor, 2,
                controller(HardwareEra.ADVANCED), 1, ChainParts.LED.get(), 1, board(HardwareEra.ADVANCED), 1);
    }

    /*
     * A drive in a case of its own: an optical one its motor, a lens, the laser diode of its era and an etched board;
     * the floppy its motor, a magnetic head and an etched board. The Dock Station's trays are a SATA port for each disk
     * size and an M.2 slot, with its USB bridge.
     */
    private static void drives(final RecipeOutput output) {
        final Item motor = ChainParts.ELECTRIC_MOTOR.get();
        made(output, HardwareEra.VINTAGE, item("floppy_drive"), motor, 1, ChainParts.MAGNETIC_HEAD.get(), 1,
                board(HardwareEra.VINTAGE), 1, pvc(), 1);
        made(output, HardwareEra.LEGACY, item("cd_drive"), motor, 1, glassLens(), 1,
                ChainParts.GALLIUM_ARSENIDE_LASER_DIODE.get(), 1, board(HardwareEra.LEGACY), 1, polycarbonate(), 1);
        made(output, HardwareEra.TRANSITION, item("dvd_drive"), motor, 1, glassLens(), 1,
                ChainParts.ALGAINP_LASER_DIODE.get(), 1, board(HardwareEra.TRANSITION), 1, polycarbonate(), 1);
        made(output, HardwareEra.ADVANCED, item("blu_ray_drive"), motor, 1, glassLens(), 1,
                ChainParts.GALLIUM_NITRIDE_LASER_DIODE.get(), 1, board(HardwareEra.ADVANCED), 1, polycarbonate(), 1);
        made(output, HardwareEra.STANDARD, item("dock_station"), polycarbonate(), 2, board(HardwareEra.STANDARD), 1,
                controller(HardwareEra.STANDARD), 1, part("storage_sata"), 2, part("storage_m2"), 1);
    }

    /*
     * A speaker: its voice coil (a copper coil), its magnet (ferrite, as real speaker magnets are), its paper cone and
     * its box; a powered one its amplifier (the era's etched board). The Inspira's satellites take the cable to their
     * subwoofer, which holds the amplifier and two drivers.
     */
    private static void speakers(final RecipeOutput output) {
        final Item coil = ChainParts.COPPER_COIL.get();
        final Item ferrite = ChainParts.FERRITE.get();
        made(output, HardwareEra.LEGACY, item("legacy_speaker"), coil, 1, ferrite, 1, Items.PAPER, 1, polycarbonate(),
                1);
        made(output, HardwareEra.TRANSITION, item("transition_speaker"), coil, 1, ferrite, 1, Items.PAPER, 1,
                polycarbonate(), 1, copperWire(), 2);
        made(output, HardwareEra.TRANSITION, item("transition_subwoofer"), coil, 2, ferrite, 2, Items.PAPER, 2,
                polycarbonate(), 2, board(HardwareEra.TRANSITION), 1);
        made(output, HardwareEra.STANDARD, item("speaker"), coil, 1, ferrite, 1, Items.PAPER, 1, aluminiumPlate(), 1,
                board(HardwareEra.STANDARD), 1);
        made(output, HardwareEra.ADVANCED, item("advanced_speaker"), coil, 1, ferrite, 1, Items.PAPER, 1,
                polycarbonate(), 1, board(HardwareEra.ADVANCED), 1);
    }

    /*
     * The Redstone Interface: redstone, the lens of the face it reads through, its board and box. A hub: the Vintage's
     * switch box, then the hub chip and a copper wire for each port it adds. A Pattern Encoder: its board and
     * controller and the drive of its era's writable medium; the Standard's the USB port, a copper foil and its
     * controller. The Network Gateway: its board, controller, box and the serial line.
     */
    private static void devices(final RecipeOutput output) {
        for (final Era era : ChainParts.ERAS) {
            made(output, era, item(era.path() + "_redstone_interface"), Items.REDSTONE, 1, glassLens(), 1, board(era),
                    1, plastic(era), 1);
        }
        made(output, HardwareEra.VINTAGE, item("vintage_hub"), steelPlate(), 1, steelRod(), 1, copperWire(), 2,
                board(HardwareEra.VINTAGE), 1);
        hub(output, HardwareEra.LEGACY, polycarbonate(), 4);
        hub(output, HardwareEra.TRANSITION, polycarbonate(), 4);
        hub(output, HardwareEra.STANDARD, polycarbonate(), 7);
        hub(output, HardwareEra.ADVANCED, aluminiumPlate(), 7);
        for (final Era era : ChainParts.ERAS) {
            if (era == HardwareEra.STANDARD) {
                made(output, era, ofEra(era, "pattern_encoder"), board(era), 1, controller(era), 2, plastic(era), 2,
                        copperFoil(), 1);
            } else {
                made(output, era, ofEra(era, "pattern_encoder"), board(era), 1, controller(era), 1, plastic(era), 2,
                        item(ENCODER_DRIVES.get(era)), 1);
            }
        }
        made(output, HardwareEra.VINTAGE, item("network_gateway"), board(HardwareEra.VINTAGE), 1,
                controller(HardwareEra.VINTAGE), 1, pvc(), 2, copperWire(), 2);
    }

    private static void hub(final RecipeOutput output, final Era era, final Item box, final int ports) {
        made(output, era, item(era.path() + "_hub"), box, 1, board(era), 1, controller(era), 1, copperWire(), ports);
    }

    /*
     * The floppy disk is its film in a PVC case. A rewritable disc is the blank disc and its reflector, with the
     * phase-change layer the real ones have (silver for the CD-RW's, silver and indium for the DVD-RW's, germanium for
     * the BD-RE's); a DVD is two discs bonded. A USB stick is memory chips by its size, its controller, board, case and
     * plug.
     */
    private static void media(final RecipeOutput output) {
        final Item blank = ChainParts.BLANK_DISC.get();
        made(output, HardwareEra.VINTAGE, item("floppy_disk"), ChainParts.FLOPPY_FILM.get(), 1, pvc(), 1);
        made(output, HardwareEra.LEGACY, item("cd_rw"), blank, 1, of(Element.ALUMINIUM, Forms.FOIL), 1,
                dust(Element.SILVER), 1);
        made(output, HardwareEra.TRANSITION, item("dvd_rw"), blank, 2, of(Element.ALUMINIUM, Forms.FOIL), 1,
                dust(Element.SILVER), 1, dust(Element.INDIUM), 1);
        made(output, HardwareEra.ADVANCED, item("bd_re"), blank, 1, of(Element.SILVER, Forms.FOIL), 1,
                dust(Element.GERMANIUM), 1);
        made(output, HardwareEra.STANDARD, item("usb_flash_drive"), memory(HardwareEra.STANDARD), 4,
                controller(HardwareEra.STANDARD), 1, board(HardwareEra.STANDARD), 1, polycarbonate(), 1, copperFoil(),
                2);
    }
}
