/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static java.util.Map.entry;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The kits of the small starship's parts, where each block goes in the ship's frame ({@link ShipKit}). Every section
 * of the spine is a ring of the hull four blocks wide and three high: thermal plating under its sides and the deck
 * between, the sides' plating with an access panel every third block, frames and plating on the top's corners with the
 * ship's lines and a light along its middle, and the walkway in the middle row, a pressure door in its bulkhead where a
 * section begins. The cockpits taper to their noses with the console in front of the seat and the overhead panel above
 * it; the airlock opens its door on the left with the suit locker and the oxygen panel across; the docking module
 * carries its collar on top; the holds lay their tie-down decks, racks and doors; the reactors keep their core in the
 * middle within its shield; the tail engines close the spine; the nacelles sit on the stub wings, the side intakes
 * beside the cockpit's joint, the fins on the tail, the thrusters at the cockpit's back and the tail's shoulders, the
 * gear under it all.
 */
public final class SmallShipKits {

    private SmallShipKits() {
    }

    /** Declares the kits, once, from the mod's constructor. */
    public static void declare() {
        final Map<String, ResourceLocation> hull = keys(entry("T", ShipKitBlocks.THERMAL_PLATING),
                entry("D", ShipKitBlocks.DECK_PLATE), entry("G", ShipKitBlocks.DECK_GRATING),
                entry("A", ShipKitBlocks.ACCESS_PANEL), entry("L", ShipKitBlocks.LIVERY_PLATING),
                entry("P", ShipKitBlocks.PRESSURE_DOOR), entry("B", ShipKitBlocks.BULKHEAD),
                entry("F", ShipKitBlocks.HULL_FRAME), entry("H", ShipKitBlocks.HULL_PLATING),
                entry("C", ShipKitBlocks.CONDUIT), entry("Y", ShipKitBlocks.CABLE_TRAY),
                entry("I", ShipKitBlocks.LIGHT_PANEL), entry("V", ShipKitBlocks.VIEWPORT));
        final Map<String, ResourceLocation> cockpit = with(hull, entry("N", ShipKitBlocks.NOSE_SENSOR),
                entry("E", ShipKitBlocks.CARBON_EDGE), entry("K", ShipKitBlocks.CANOPY_FRAME),
                entry("W", ShipKitBlocks.CANOPY_GLASS), entry("O", ShipKitBlocks.FLIGHT_CONSOLE),
                entry("S", ShipKitBlocks.PILOT_SEAT), entry("U", ShipKitBlocks.OVERHEAD_PANEL));
        section("orbiter_cockpit", cockpit, List.of("    ", " TT ", "TDDT", "TDDT", "TDDT"),
                List.of(" NN ", " EE ", "KWWK", "LOOL", "LS L"),
                List.of("    ", "    ", "    ", "KWWK", "KUUK"));
        section("cab_cockpit", cockpit, List.of(" TT ", "TDDT", "TDDT", "TDDT", "TDDT"),
                List.of(" NN ", "EEEE", "L  L", "LOOL", "LS L"),
                List.of("    ", "KWWK", "KWWK", "KWWK", "KUUK"));
        section("greenhouse_cockpit", cockpit, List.of(" WW ", "TWWT", "TDDT", "TDDT", "TDDT"),
                List.of(" WW ", "KWWK", "KWWK", "LOOL", "LS L"),
                List.of("    ", "    ", "KWWK", "KWWK", "KUUK"));
        final Map<String, ResourceLocation> airlock = with(hull, entry("Q", ShipKitBlocks.AIRLOCK_CYCLER),
                entry("O", ShipKitBlocks.AIRLOCK_DOOR), entry("K", ShipKitBlocks.SUIT_LOCKER),
                entry("X", ShipKitBlocks.OXYGEN_PANEL), entry("N", ShipKitBlocks.DOCKING_COLLAR),
                entry("U", ShipKitBlocks.DOCKING_GUIDE));
        section("airlock", airlock, List.of("TDDT", "TGGT", "TDDT"), List.of("QPBA", "O  K", "O  X"),
                List.of("FCYF", "VIIH", "FCYF"));
        section("docking", airlock, List.of("TDDT", "TGGT", "TDDT"), List.of("QPBA", "V  V", "V  V"),
                List.of("FCYF", "HNNH", "FUUF"));
        section("corridor", hull, List.of("TDDT", "TGGT"), List.of("APBA", "V  V"), List.of("FCYF", "HIIH"));
        // The holds keep their cargo in vanilla barrels, strapped down in the middle, working as they are.
        final Map<String, ResourceLocation> hold = plus(with(hull, entry("W", ShipKitBlocks.TIE_DOWN_DECK),
                entry("J", ShipKitBlocks.RAMP_HINGE), entry("M", ShipKitBlocks.RAMP_PLATE)),
                entry("b", vanilla(Blocks.BARREL)));
        section("bay", hold, List.of("TWWT", "TWWT", "TWWT", "TWWT", "TWWT"),
                List.of("APBA", "L  L", "L  L", "AbbA", "LbbL"),
                List.of("FHHF", "HHHH", "FHHF", "HHHH", "FHHF"));
        section("ramp", hold, List.of("TWWT", "TWWT", "TWWT", "TWWT", "TJJT"),
                List.of("APBA", "L  L", "LbbL", "AbbA", "LMML"),
                List.of("FCYF", "HIIH", "FCYF", "HCYH", "FMMF"));
        section("side_door", hold, List.of("TWWT", "TWWT", "TWWT", "TWWT", "TWWT"),
                List.of("APBA", "Lb M", "Lb M", "Ab M", "L  L"),
                List.of("FCYF", "HIIH", "FCYF", "HCYH", "FIIF"));
        // The small functional modules: the workshop's vanilla crafting table, smithing table, furnace and barrel in
        // its middle, working as they are; the lab's panels and sample racks along its walls; the quarters' bunk; the
        // computing module's server panels and its antenna feed; small cargo's racks and a barrel.
        final List<String> floor = List.of("TDDT", "TGGT");
        final List<String> ceiling = List.of("FCYF", "HIIH");
        section("workshop", plus(hull, entry("c", vanilla(Blocks.CRAFTING_TABLE)),
                entry("s", vanilla(Blocks.SMITHING_TABLE)), entry("f", vanilla(Blocks.FURNACE)),
                entry("b", vanilla(Blocks.BARREL))), floor, List.of("AcsA", "LfbL"), ceiling);
        section("lab", with(hull, entry("N", ShipKitBlocks.LAB_PANEL), entry("S", ShipKitBlocks.SAMPLE_RACK)), floor,
                List.of("NPBS", "N  S"), ceiling);
        section("quarters", with(hull, entry("U", ShipKitBlocks.BUNK_FRAME)), floor, List.of("UPBA", "U  L"),
                ceiling);
        section("computing", with(hull, entry("V", ShipKitBlocks.SERVER_PANEL), entry("W", ShipKitBlocks.ANTENNA_FEED)),
                floor, List.of("VPBV", "V  V"), List.of("FCYW", "HIIW"));
        section("small_cargo", plus(with(hull, entry("R", ShipKitBlocks.CARGO_RACK)), entry("b",
                vanilla(Blocks.BARREL))), floor, List.of("RPBR", "Rb R"), ceiling);
        final Map<String, ResourceLocation> reactor = with(hull, entry("E", ShipKitBlocks.FUSION_CORE),
                entry("Z", ShipKitBlocks.MAGNETIC_COIL), entry("S", ShipKitBlocks.REACTOR_SHIELD),
                entry("Q", ShipKitBlocks.COOLANT_PUMP), entry("R", ShipKitBlocks.RADIATOR_PANEL));
        section("reactor_flush", reactor, List.of("TDDT", "TQQT"), List.of("SEES", "SZZS"), List.of("FRRF", "HRRH"));
        section("reactor_fins", reactor, List.of("TDDT", "TQQT"), List.of("SEES", "SZZS"), List.of("RRRR", "RRRR"));
        final Map<String, ResourceLocation> tail = keys(entry("T", ShipKitBlocks.THERMAL_PLATING),
                entry("M", ShipKitBlocks.ENGINE_MOUNT), entry("F", ShipKitBlocks.PROPELLANT_FEED),
                entry("U", ShipKitBlocks.FUEL_TANK), entry("V", ShipKitBlocks.VALVE_PACK),
                entry("C", ShipKitBlocks.THRUST_CHAMBER), entry("B", ShipKitBlocks.BELL_SEGMENT));
        hung("twin_bells", tail, piece(ShipKit.Anchor.SPINE_END, -2, 0, 0, false, List.of("TTTT", "TTTT"),
                List.of("FMMF", "BCCB"), List.of("VUUV", "BUUB")));
        hung("cluster", tail, piece(ShipKit.Anchor.SPINE_END, -2, 0, 0, false, List.of("TTTT", "BCCB"),
                List.of("FMMF", "BCCB"), List.of("VUUV", "BUUB")));
        final Map<String, ResourceLocation> nozzle = keys(entry("M", ShipKitBlocks.ENGINE_MOUNT),
                entry("U", ShipKitBlocks.FUEL_TANK), entry("P", ShipKitBlocks.PROPELLANT_FEED),
                entry("C", ShipKitBlocks.THRUST_CHAMBER), entry("B", ShipKitBlocks.BELL_SEGMENT));
        hung("magnetic_nozzle", nozzle, piece(ShipKit.Anchor.WING_ROOT, 3, 0, 3, true,
                List.of("MM", "UU", "CC", "BB"), List.of("MM", "PP", "CC", "BB")));
        final Map<String, ResourceLocation> air = keys(entry("L", ShipKitBlocks.INTAKE_LIP),
                entry("F", ShipKitBlocks.INTAKE_FAN), entry("C", ShipKitBlocks.COMPRESSOR_STAGE),
                entry("S", ShipKitBlocks.SWIVEL_DUCT), entry("V", ShipKitBlocks.NOZZLE_VANE),
                entry("P", ShipKitBlocks.LIVERY_PLATING));
        hung("nacelle_intake", air, piece(ShipKit.Anchor.WING_ROOT, 3, 0, -1, true,
                List.of("LL", "SS", "SS", "VV"), List.of("LL", "FF", "CC", "CC")));
        hung("side_intakes", air, piece(ShipKit.Anchor.COCKPIT_JOINT, -3, 0, -2, true,
                List.of(" ", "S", "S", "V"), List.of("L", "F", "C", "P")));
        final Map<String, ResourceLocation> wing = keys(entry("E", ShipKitBlocks.CARBON_EDGE),
                entry("P", ShipKitBlocks.WING_SPAR), entry("K", ShipKitBlocks.WING_SKIN),
                entry("A", ShipKitBlocks.FLAP_ACTUATOR), entry("T", ShipKitBlocks.WING_TIP_LIGHT),
                entry("S", ShipKitBlocks.CONTROL_SURFACE));
        hung("blended_delta", wing, piece(ShipKit.Anchor.WING_ROOT, 2, 0, -1, true, List.of("E    ", "P    ",
                "PE   ", "PKE  ", "PKK  ", "PKKE ", "PKKKE", "PKAAA", "AA   ")));
        hung("stub_wings", wing, piece(ShipKit.Anchor.WING_ROOT, 2, 1, 0, true,
                List.of("E    ", "P  E ", "P  KE", "P  AT", "A    ")));
        hung("swept_wings", wing, piece(ShipKit.Anchor.WING_ROOT, 2, 1, 1, true,
                List.of("EE   ", "PKE T", "PKKET", "AAA T")));
        hung("twin_canted_fins", wing, piece(ShipKit.Anchor.SPINE_END, -2, 3, -3, true,
                List.of("E", "S", "S", "S"), List.of(" ", "E", "S", "S")));
        hung("single_fin", wing, piece(ShipKit.Anchor.SPINE_END, -1, 3, -4, false,
                List.of("EE", "SS", "SS", "SS", "  "), List.of("  ", "EE", "SS", "SS", "  "),
                List.of("  ", "  ", "EE", "SS", "SS")));
        final Map<String, ResourceLocation> thrusters = keys(entry("R", ShipKitBlocks.THRUSTER_BLOCK),
                entry("V", ShipKitBlocks.VALVE_PACK));
        hung("blisters", thrusters, piece(ShipKit.Anchor.COCKPIT_JOINT, -3, 2, -1, true, List.of("V", "R")),
                piece(ShipKit.Anchor.SPINE_END, -3, 2, -2, true, List.of("R", "V")));
        hung("quads", thrusters, piece(ShipKit.Anchor.COCKPIT_JOINT, -3, 2, -1, true, List.of("R")),
                piece(ShipKit.Anchor.SPINE_END, -3, 2, -1, true, List.of("R")));
        final Map<String, ResourceLocation> gear = keys(entry("G", ShipKitBlocks.GEAR_BAY_DOOR),
                entry("S", ShipKitBlocks.LANDING_STRUT), entry("W", ShipKitBlocks.WHEEL),
                entry("F", ShipKitBlocks.LANDING_FOOT));
        hung("wheels", gear, piece(ShipKit.Anchor.COCKPIT_JOINT, -1, -2, -3, false, List.of("WW"), List.of("GS")),
                piece(ShipKit.Anchor.WING_ROOT, -2, -2, 2, true, List.of("W"), List.of("S")));
        hung("legs", gear, piece(ShipKit.Anchor.COCKPIT_JOINT, -1, -2, -3, false, List.of("FF"), List.of("SS")),
                piece(ShipKit.Anchor.SPINE_END, -2, -2, -2, true, List.of("F"), List.of("S")));
    }

    /* A section of the spine: one piece from its own front, the hull's four blocks across. */
    @SafeVarargs
    private static void section(final String part, final Map<String, ResourceLocation> key,
                                final List<String>... layers) {
        hung(part, key, piece(ShipKit.Anchor.SECTION, -2, 0, 0, false, layers));
    }

    private static void hung(final String part, final Map<String, ResourceLocation> key,
                             final ShipKit.Piece... pieces) {
        ShipKits.declare(SmallShipParts.id("small_" + part), new ShipKit(List.of(pieces), key));
    }

    @SafeVarargs
    private static ShipKit.Piece piece(final ShipKit.Anchor anchor, final int x, final int y, final int z,
                                       final boolean mirrored, final List<String>... layers) {
        return new ShipKit.Piece(anchor, x, y, z, List.of(layers), mirrored);
    }

    @SafeVarargs
    private static Map<String, ResourceLocation> plus(final Map<String, ResourceLocation> base,
                                                      final Map.Entry<String, ResourceLocation>... entries) {
        final Map<String, ResourceLocation> key = new LinkedHashMap<>(base);
        for (final Map.Entry<String, ResourceLocation> each : entries) {
            key.put(each.getKey(), each.getValue());
        }
        return Map.copyOf(key);
    }

    private static ResourceLocation vanilla(final Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    @SafeVarargs
    private static Map<String, ResourceLocation> keys(final Map.Entry<String, BlockEntry<Block>>... entries) {
        return with(Map.of(), entries);
    }

    @SafeVarargs
    private static Map<String, ResourceLocation> with(final Map<String, ResourceLocation> base,
                                                      final Map.Entry<String, BlockEntry<Block>>... entries) {
        final Map<String, ResourceLocation> key = new LinkedHashMap<>(base);
        for (final Map.Entry<String, BlockEntry<Block>> each : entries) {
            key.put(each.getKey(), each.getValue().getId());
        }
        return Map.copyOf(key);
    }
}
