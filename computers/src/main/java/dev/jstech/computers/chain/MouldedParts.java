/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import static dev.jstech.computers.chain.ChainParts.englishName;
import static dev.jstech.computers.chain.ChainParts.part;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The plastic parts the Injection Moulder moulds besides the board components' bodies (computers/docs/design/
 * recipes.md): each era's fan frame, the frame and rotor its case fan is built on, and each case's, screen's and
 * machine's bezel, the moulded front it takes, with the liquid coolers' pump covers and the robot arm's cover. Each is
 * moulded in its own mould, cut on a Mould Blank at the moulder by the computer linked to it and lent to every job;
 * so no product's lot makes another, the products of a family differing by their own bezel.
 */
public final class MouldedParts {

    /** Each era's fan frame, in the order of the eras. */
    public static final List<Part> FAN_FRAMES = List.of(
            frame(HardwareEra.VINTAGE), frame(HardwareEra.LEGACY), frame(HardwareEra.TRANSITION),
            frame(HardwareEra.STANDARD), frame(HardwareEra.ADVANCED));
    /**
     * The bezels and covers, family by family: the server cases, the Personal Computers, the monitors, the mainframes,
     * the server racks, the liquid coolers and the two left, the floppy drive and the robot arm. A product named with
     * its own parentheses has them after a comma in its part's name.
     */
    public static final List<Part> BEZELS = List.of(
            bezel(HardwareEra.VINTAGE, "vintage_server_case").named("Bezel (Vintage Server Case)"),
            bezel(HardwareEra.VINTAGE, "vintage_compute_server_case").named("Bezel (Vintage Compute Server Case)"),
            bezel(HardwareEra.VINTAGE, "vintage_storage_server_case").named("Bezel (Vintage Storage Server Case)"),
            bezel(HardwareEra.LEGACY, "legacy_server_case").named("Bezel (Legacy Server Case)"),
            bezel(HardwareEra.LEGACY, "legacy_compute_server_case").named("Bezel (Legacy Compute Server Case)"),
            bezel(HardwareEra.LEGACY, "legacy_storage_server_case").named("Bezel (Legacy Storage Server Case)"),
            bezel(HardwareEra.TRANSITION, "transition_server_case").named("Bezel (Transition Server Case)"),
            bezel(HardwareEra.TRANSITION, "transition_compute_server_case")
                    .named("Bezel (Transition Compute Server Case)"),
            bezel(HardwareEra.TRANSITION, "transition_storage_server_case")
                    .named("Bezel (Transition Storage Server Case)"),
            bezel(HardwareEra.STANDARD, "server_case").named("Bezel (Server Case)"),
            bezel(HardwareEra.STANDARD, "compute_server_case").named("Bezel (Compute Server Case)"),
            bezel(HardwareEra.STANDARD, "storage_server_case").named("Bezel (Storage Server Case)"),
            bezel(HardwareEra.ADVANCED, "advanced_server_case").named("Bezel (Advanced Server Case)"),
            bezel(HardwareEra.ADVANCED, "advanced_compute_server_case").named("Bezel (Advanced Compute Server Case)"),
            bezel(HardwareEra.ADVANCED, "advanced_storage_server_case").named("Bezel (Advanced Storage Server Case)"),
            bezel(HardwareEra.VINTAGE, "vintage_personal_computer").named("Bezel (Vintage Personal Computer)"),
            bezel(HardwareEra.LEGACY, "legacy_personal_computer").named("Bezel (Legacy Personal Computer)"),
            bezel(HardwareEra.TRANSITION, "transition_personal_computer").named("Bezel (Transition Personal Computer)"),
            bezel(HardwareEra.STANDARD, "personal_computer").named("Bezel (Personal Computer)"),
            bezel(HardwareEra.ADVANCED, "advanced_personal_computer").named("Bezel (Advanced Personal Computer)"),
            bezel(HardwareEra.STANDARD, "aesthetic_personal_computer").named("Bezel (Aesthetic Personal Computer)"),
            bezel(HardwareEra.ADVANCED, "advanced_aesthetic_personal_computer")
                    .named("Bezel (Advanced Aesthetic Personal Computer)"),
            bezel(HardwareEra.STANDARD, "high_performance_personal_computer")
                    .named("Bezel (High Performance Personal Computer)"),
            bezel(HardwareEra.ADVANCED, "advanced_high_performance_personal_computer")
                    .named("Bezel (Advanced High Performance Personal Computer)"),
            bezel(HardwareEra.VINTAGE, "vintage_monitor").named("Bezel (Mono II Monitor)"),
            bezel(HardwareEra.VINTAGE, "mono_i_monitor").named("Bezel (Mono I Monitor)"),
            bezel(HardwareEra.VINTAGE, "amber_monitor").named("Bezel (Amber Monitor)"),
            bezel(HardwareEra.VINTAGE, "cga_monitor").named("Bezel (CGA Monitor)"),
            bezel(HardwareEra.LEGACY, "legacy_monitor").named("Bezel (Legacy Monitor)"),
            bezel(HardwareEra.TRANSITION, "transition_monitor").named("Bezel (Transition Monitor)"),
            bezel(HardwareEra.STANDARD, "monitor").named("Bezel (Monitor)"),
            bezel(HardwareEra.ADVANCED, "color_monitor").named("Bezel (Color Monitor)"),
            bezel(HardwareEra.VINTAGE, "vintage_mainframe").named("Bezel (Vintage Mainframe)"),
            bezel(HardwareEra.LEGACY, "legacy_mainframe").named("Bezel (Legacy Mainframe)"),
            bezel(HardwareEra.TRANSITION, "transition_mainframe").named("Bezel (Transition Mainframe)"),
            bezel(HardwareEra.STANDARD, "mainframe").named("Bezel (Mainframe)"),
            bezel(HardwareEra.ADVANCED, "advanced_mainframe").named("Bezel (Advanced Mainframe)"),
            bezel(HardwareEra.TRANSITION, "quince_mainframe_transition").named("Bezel (Quince Mainframe, Transition)"),
            bezel(HardwareEra.STANDARD, "quince_mainframe_standard").named("Bezel (Quince Mainframe, Standard)"),
            bezel(HardwareEra.ADVANCED, "quince_mainframe_advanced").named("Bezel (Quince Mainframe, Advanced)"),
            bezel(HardwareEra.VINTAGE, "vintage_server_rack").named("Bezel (Vintage Server Rack)"),
            bezel(HardwareEra.LEGACY, "legacy_server_rack").named("Bezel (Legacy Server Rack)"),
            bezel(HardwareEra.TRANSITION, "transition_server_rack").named("Bezel (Transition Server Rack)"),
            bezel(HardwareEra.STANDARD, "server_rack").named("Bezel (Server Rack)"),
            bezel(HardwareEra.ADVANCED, "advanced_server_rack").named("Bezel (Advanced Server Rack)"),
            pumpCover(HardwareEra.STANDARD, "aqualoop_240_standard").named("Pump Cover (MF AquaLoop 240, Standard)"),
            pumpCover(HardwareEra.STANDARD, "aqualoop_360_standard").named("Pump Cover (MF AquaLoop 360, Standard)"),
            pumpCover(HardwareEra.ADVANCED, "aqualoop_240_advanced").named("Pump Cover (MF AquaLoop 240, Advanced)"),
            pumpCover(HardwareEra.ADVANCED, "aqualoop_360_advanced").named("Pump Cover (MF AquaLoop 360, Advanced)"),
            bezel(HardwareEra.VINTAGE, "floppy_drive").named("Bezel (Floppy Drive)"),
            armCover(HardwareEra.VINTAGE, "prebuilder_robot_arm").named("Arm Cover (Robot Arm)"));

    private MouldedParts() {
    }

    /** Declares the parts, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every part, family by family. */
    public static List<Part> all() {
        return Stream.concat(FAN_FRAMES.stream(), BEZELS.stream()).toList();
    }

    /** Era {@code era}'s fan frame. */
    public static Part fanFrame(final Era era) {
        return FAN_FRAMES.stream().filter(frame -> frame.era() == era).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no fan frame of " + era.id()));
    }

    /** The part moulded for the product whose item is {@code product}: "vintage_server_case". */
    public static Part of(final String product) {
        return all().stream().filter(part -> part.product().equals(product)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no moulded part for " + product));
    }

    /* Era {@code era}'s fan frame, "Fan Frame (Vintage)", and its mould, "Mould (Fan Frame, Vintage)". */
    private static Part frame(final Era era) {
        return new Declaration(Piece.FAN_FRAME, era, "case_fan_" + era.path())
                .named("Fan Frame (" + englishName(era) + ")");
    }

    private static Declaration bezel(final Era era, final String product) {
        return new Declaration(Piece.BEZEL, era, product);
    }

    private static Declaration pumpCover(final Era era, final String product) {
        return new Declaration(Piece.PUMP_COVER, era, product);
    }

    private static Declaration armCover(final Era era, final String product) {
        return new Declaration(Piece.ARM_COVER, era, product);
    }

    /** What a part is, the list of the moulds the lithography program offers it in. */
    public enum Family {
        FANS,
        BEZELS
    }

    /** What a moulded part is to its product. */
    public enum Piece {
        FAN_FRAME(Family.FANS),
        BEZEL(Family.BEZELS),
        PUMP_COVER(Family.BEZELS),
        ARM_COVER(Family.BEZELS);

        private final Family family;

        Piece(final Family family) {
            this.family = family;
        }

        /** The list its moulds are offered in. */
        public Family family() {
            return family;
        }
    }

    /**
     * One part.
     *
     * @param piece   what it is to its product
     * @param era     the era it is first made in
     * @param product the item of the product it is moulded for, "vintage_server_case"
     * @param item    its item
     * @param mould   the steel mould it is moulded in, cut from a Mould Blank
     */
    public record Part(Piece piece, Era era, String product, ItemEntry<Item> item, ItemEntry<Item> mould) {

        /** The list its mould is offered in. */
        public Family family() {
            return piece.family();
        }

        /** The product's item. */
        public Item productItem() {
            return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, product));
        }
    }

    /*
     * A part declared by its piece, era and product, named last, which registers it, its item the piece's and the
     * product's, "bezel_server_case", and its mould, named after it: "Bezel (Server Case)" gives "Mould (Bezel, Server
     * Case)". A fan frame's item is the era's, "fan_frame_vintage".
     */
    private record Declaration(Piece piece, Era era, String product) {

        Part named(final String english) {
            final String id = piece == Piece.FAN_FRAME ? "fan_frame_" + era.path()
                    : piece.name().toLowerCase(Locale.ROOT) + "_" + product;
            return new Part(piece, era, product, part(id, era).named(english).register(),
                    part("mould_" + id, era).named("Mould (" + english.replaceFirst(" \\(", ", ")).register());
        }
    }
}
